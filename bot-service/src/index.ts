import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import { parseIntent } from './intent';
import { publishCommand, connectRabbit } from './rabbit';
import { startWorker } from './worker';
import { isSlmAvailable, generateChatResponse } from './slm';
import { initRag, getRagStatus } from './rag';
import { getAccountBalance, listAccounts, getPaymentStatus } from './backendClient';
import { AccessDeniedError, assertOwnAccount, assertOwnsPayment, filterAccountsForUser, type BotUser } from './access';

dotenv.config();

const app = express();
app.use(cors());
app.use(express.json());

app.post('/nl', async (req, res) => {
  try {
    const { text, user } = req.body as { text?: string; user?: BotUser };
    if (!text) return res.status(400).json({ error: 'text required' });
    const intent = await parseIntent(text, user);

    if (intent.type === 'unknown') {
      if (process.env.SLM_ENABLED === 'true' || await isSlmAvailable()) {
        const reply = await generateChatResponse(text);
        if (reply) {
          return res.json({ intent, reply });
        }
      }
    }

    // Read-only lookups have no side effects and don't need confirmation or
    // queueing — resolve them immediately and return the data inline.
    if (intent.readOnly) {
      const result = await resolveReadOnlyIntent(intent, user);
      return res.json({ intent, ...result });
    }

    if (!isCommandOwnedByUser(intent, user)) {
      return res.json({ intent, error: "That's not your account, so I can't help with that request." });
    }

    return res.json({ intent });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'internal' });
  }
});

/**
 * For state-changing commands (create/retry/cancel payment), checks that the
 * command only involves the current user's own account before it's even
 * shown to them for confirmation. Retry/cancel are checked again against the
 * real payment record in resolveOwnershipForQueue before queueing.
 */
function isCommandOwnedByUser(intent: { type: string; payload: any }, user?: BotUser): boolean {
  if (!user || user.role === 'admin') return true;
  if (intent.type === 'create_payment') {
    try {
      assertOwnAccount(user, intent.payload?.sourceAccount);
      return true;
    } catch {
      return false;
    }
  }
  return true;
}

async function resolveReadOnlyIntent(
  intent: { type: string; payload: any },
  user?: BotUser
): Promise<{ result?: unknown; error?: string }> {
  try {
    if (intent.type === 'check_balance') {
      if (!intent.payload?.accountNumber) return { error: 'No account number found in the request.' };
      assertOwnAccount(user, intent.payload.accountNumber);
      const account = await getAccountBalance(intent.payload.accountNumber);
      return { result: account };
    }
    if (intent.type === 'list_accounts') {
      const accounts = await listAccounts();
      return { result: filterAccountsForUser(user, accounts) };
    }
    if (intent.type === 'payment_status') {
      if (!intent.payload?.paymentId) return { error: 'No payment id found in the request.' };
      const payment = await getPaymentStatus(intent.payload.paymentId);
      assertOwnsPayment(user, payment);
      return { result: payment };
    }
    return { error: 'Unsupported lookup.' };
  } catch (err: any) {
    if (err instanceof AccessDeniedError) return { error: err.message };
    const status = err?.response?.status;
    if (status === 404) return { error: 'Not found.' };
    return { error: err?.response?.data?.message || err.message || 'Lookup failed.' };
  }
}

/**
 * Ownership check for state-changing commands right before they're queued.
 * create_payment is checked against the source account directly; retry/cancel
 * fetch the real payment record so a member can't spoof the payload.
 */
async function assertCommandOwnership(command: any, user?: BotUser): Promise<void> {
  if (!user || user.role === 'admin') return;
  if (command.type === 'create_payment') {
    assertOwnAccount(user, command.payload?.sourceAccount);
    return;
  }
  if (command.type === 'retry_payment' || command.type === 'cancel_payment') {
    const paymentId = command.payload?.paymentId;
    if (!paymentId) throw new AccessDeniedError('No payment id found in the request.');
    const payment = await getPaymentStatus(paymentId);
    assertOwnsPayment(user, payment);
  }
}

app.get('/slm/status', async (_req, res) => {
  const available = await isSlmAvailable();
  return res.json({
    enabled: process.env.SLM_ENABLED === 'true',
    available,
    model: process.env.SLM_MODEL || 'qwen2.5:0.5b',
    baseUrl: process.env.SLM_BASE_URL || 'http://localhost:11434',
  });
});

app.get('/rag/status', (_req, res) => {
  return res.json(getRagStatus());
});

app.post('/execute', async (req, res) => {
  try {
    const { command, user } = req.body as { command?: any; user?: BotUser };
    if (!command) return res.status(400).json({ error: 'command required' });
    if (command.requiresConfirmation) {
      return res.status(400).json({ error: 'confirmation required', message: 'This command requires explicit confirmation via /confirm' });
    }
    try {
      await assertCommandOwnership(command, user);
    } catch (err) {
      if (err instanceof AccessDeniedError) return res.status(403).json({ error: err.message });
      throw err;
    }
    command.__user = user;
    await publishCommand(command);
    return res.json({ status: 'queued' });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'failed to queue' });
  }
});

app.post('/confirm', async (req, res) => {
  try {
    const { command, approver, user } = req.body as { command?: any; approver?: string; user?: BotUser };
    if (!command) return res.status(400).json({ error: 'command required' });
    try {
      await assertCommandOwnership(command, user);
    } catch (err) {
      if (err instanceof AccessDeniedError) return res.status(403).json({ error: err.message });
      throw err;
    }
    // Simple audit note: attach approver metadata
    command.__confirmedBy = approver || 'unknown';
    command.__confirmedAt = new Date().toISOString();
    command.__user = user;
    await publishCommand(command);
    return res.json({ status: 'queued', confirmedBy: command.__confirmedBy });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'failed to queue' });
  }
});

const port = Number(process.env.PORT || 4001);
app.listen(port, async () => {
  console.log(`RupeeX Bot Service listening on ${port}`);
  try {
    await connectRabbit();
    console.log('Connected to AMQP');
  } catch (err: any) {
    console.warn('AMQP not available at startup:', err.message || err);
  }
  if (process.env.START_WORKER === 'true') {
    console.log('Starting embedded worker (prototype mode)');
    startWorker().catch((e) => console.error('embedded worker failed', e));
  }
  initRag()
    .then(() => console.log('[rag]', getRagStatus()))
    .catch((e) => console.warn('[rag] init failed', e));
});
