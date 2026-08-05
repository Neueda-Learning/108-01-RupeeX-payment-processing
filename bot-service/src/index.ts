import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import { parseIntent } from './intent';
import { publishCommand, connectRabbit } from './rabbit';
import { startWorker } from './worker';
import { isSlmAvailable } from './slm';
import { initRag, getRagStatus } from './rag';
import { getAccountBalance, listAccounts, getPaymentStatus } from './backendClient';

dotenv.config();

const app = express();
app.use(cors());
app.use(express.json());

app.post('/nl', async (req, res) => {
  try {
    const { text, userId } = req.body;
    if (!text) return res.status(400).json({ error: 'text required' });
    const intent = await parseIntent(text, userId);

    // Read-only lookups have no side effects and don't need confirmation or
    // queueing — resolve them immediately and return the data inline.
    if (intent.readOnly) {
      const result = await resolveReadOnlyIntent(intent);
      return res.json({ intent, ...result });
    }

    return res.json({ intent });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'internal' });
  }
});

async function resolveReadOnlyIntent(intent: { type: string; payload: any }): Promise<{ result?: unknown; error?: string }> {
  try {
    if (intent.type === 'check_balance') {
      if (!intent.payload?.accountNumber) return { error: 'No account number found in the request.' };
      const account = await getAccountBalance(intent.payload.accountNumber);
      return { result: account };
    }
    if (intent.type === 'list_accounts') {
      const accounts = await listAccounts();
      return { result: accounts };
    }
    if (intent.type === 'payment_status') {
      if (!intent.payload?.paymentId) return { error: 'No payment id found in the request.' };
      const payment = await getPaymentStatus(intent.payload.paymentId);
      return { result: payment };
    }
    return { error: 'Unsupported lookup.' };
  } catch (err: any) {
    const status = err?.response?.status;
    if (status === 404) return { error: 'Not found.' };
    return { error: err?.response?.data?.message || err.message || 'Lookup failed.' };
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
    const { command } = req.body;
    if (!command) return res.status(400).json({ error: 'command required' });
    if (command.requiresConfirmation) {
      return res.status(400).json({ error: 'confirmation required', message: 'This command requires explicit confirmation via /confirm' });
    }
    await publishCommand(command);
    return res.json({ status: 'queued' });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'failed to queue' });
  }
});

app.post('/confirm', async (req, res) => {
  try {
    const { command, approver } = req.body;
    if (!command) return res.status(400).json({ error: 'command required' });
    // Simple audit note: attach approver metadata
    command.__confirmedBy = approver || 'unknown';
    command.__confirmedAt = new Date().toISOString();
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
