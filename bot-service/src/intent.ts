import { extractIntentWithSLM } from './slm';

export type BotCommand = {
  type: string;
  payload: any;
  confidence: number;
  summary?: string;
  requiresConfirmation?: boolean;
  // Read-only lookups (balance/status/list) execute immediately and never
  // touch the command queue — no side effects, so no confirmation needed.
  readOnly?: boolean;
  source?: 'slm' | 'rules';
};

const HIGH_THRESHOLD = Number(process.env.BOT_HIGH_VALUE_THRESHOLD || 100000);
const SLM_ENABLED = process.env.SLM_ENABLED === 'true';
const SLM_MIN_CONFIDENCE = Number(process.env.SLM_MIN_CONFIDENCE || 0.5);

/**
 * Primary entry point: tries the on-premise SLM first (if enabled), then
 * falls back to the deterministic rule-based parser below if the SLM is
 * unavailable, times out, returns low confidence, or returns "unknown".
 */
export async function parseIntent(text: string, userId?: string): Promise<BotCommand> {
  if (SLM_ENABLED) {
    const slmResult = await extractIntentWithSLM(text);
    if (slmResult && slmResult.type !== 'unknown' && (slmResult.confidence ?? 0) >= SLM_MIN_CONFIDENCE) {
      return mapSlmToCommand(slmResult, text);
    }
  }
  return parseIntentRules(text, userId);
}

function mapSlmToCommand(slm: Awaited<ReturnType<typeof extractIntentWithSLM>>, text: string): BotCommand {
  const result = slm!;
  const amount = result.amount;
  const requiresConfirmation = result.type === 'create_payment' && (amount || 0) >= HIGH_THRESHOLD;

  switch (result.type) {
    case 'create_payment':
      return {
        type: 'create_payment',
        payload: {
          amount,
          currency: result.currency || 'INR',
          accounts: [result.sourceAccount, result.destinationAccount].filter(Boolean),
          sourceAccount: result.sourceAccount,
          destinationAccount: result.destinationAccount,
          raw: text,
        },
        confidence: result.confidence ?? 0.7,
        summary: `Create payment ${amount ?? ''} ${result.currency ?? ''} ${result.sourceAccount ?? ''} -> ${result.destinationAccount ?? ''}`,
        requiresConfirmation,
        source: 'slm',
      };
    case 'retry_payment':
      return {
        type: 'retry_payment',
        payload: { paymentId: result.paymentId, raw: text },
        confidence: result.confidence ?? 0.7,
        summary: `Retry payment ${result.paymentId ?? ''}`,
        source: 'slm',
      };
    case 'cancel_payment':
      return {
        type: 'cancel_payment',
        payload: { paymentId: result.paymentId, raw: text },
        confidence: result.confidence ?? 0.7,
        summary: `Cancel payment ${result.paymentId ?? ''}`,
        source: 'slm',
      };
    case 'query_payments':
      return {
        type: 'query_payments',
        payload: { raw: text },
        confidence: result.confidence ?? 0.6,
        summary: 'Query payments',
        source: 'slm',
      };
    case 'check_balance':
      return {
        type: 'check_balance',
        payload: { accountNumber: result.sourceAccount, raw: text },
        confidence: result.confidence ?? 0.7,
        summary: `Check balance for ${result.sourceAccount ?? 'account'}`,
        readOnly: true,
        source: 'slm',
      };
    case 'list_accounts':
      return {
        type: 'list_accounts',
        payload: { raw: text },
        confidence: result.confidence ?? 0.7,
        summary: 'List accounts',
        readOnly: true,
        source: 'slm',
      };
    case 'payment_status':
      return {
        type: 'payment_status',
        payload: { paymentId: result.paymentId, raw: text },
        confidence: result.confidence ?? 0.7,
        summary: `Check status of payment ${result.paymentId ?? ''}`,
        readOnly: true,
        source: 'slm',
      };
    default:
      return { type: 'unknown', payload: { raw: text }, confidence: 0.2, summary: 'Could not parse intent', source: 'slm' };
  }
}

/**
 * Deterministic rule-based intent parser. Used as a fallback when the SLM
 * is disabled, unreachable, or not confident enough.
 */
export function parseIntentRules(text: string, userId?: string): BotCommand {
  const t = text.trim().toLowerCase();

  if (/create .*payment|make .*payment|send .*payment/.test(t)) {
    // naive extraction: find numbers and account words
    const amountMatch = t.match(/(\d+[\,\d]*(?:\.\d+)?)/);
    const amount = amountMatch ? parseFloat(amountMatch[1].replace(/,/g, '')) : undefined;
    const currencyMatch = t.match(/\b(inr|usd|eur|gbp)\b/);
    const currency = currencyMatch ? currencyMatch[1].toUpperCase() : 'INR';

    // extract account identifiers, e.g. "from ACC-10001 to ACC-10002"
    const fromToMatch = t.match(/from\s+([a-z0-9\-]+)\s+to\s+([a-z0-9\-]+)/i);
    let sourceAccount: string | undefined;
    let destinationAccount: string | undefined;
    if (fromToMatch) {
      sourceAccount = fromToMatch[1].toUpperCase();
      destinationAccount = fromToMatch[2].toUpperCase();
    } else {
      // fallback: generic "account <id>" mentions, in order of appearance
      const genericMatches = [...t.matchAll(/account\s*([a-z0-9\-]+)/g)].map((m) => m[1].toUpperCase());
      sourceAccount = genericMatches[0];
      destinationAccount = genericMatches[1];
    }
    const accounts = [sourceAccount, destinationAccount].filter(Boolean) as string[];

    const requiresConfirmation = (amount || 0) >= HIGH_THRESHOLD;
    return {
      type: 'create_payment',
      payload: {
        amount,
        currency,
        accounts,
        sourceAccount,
        destinationAccount,
        raw: text
      },
      confidence: 0.6,
      summary: `Create payment ${amount || ''} ${currency} ${sourceAccount ?? ''} -> ${destinationAccount ?? ''}`,
      requiresConfirmation,
      source: 'rules',
    };
  }

  if (/retry .*payment|retry payment/.test(t)) {
    const idMatch = t.match(/#?(\d+)/);
    return {
      type: 'retry_payment',
      payload: { paymentId: idMatch ? idMatch[1] : undefined, raw: text },
      confidence: 0.9,
      summary: `Retry payment ${idMatch ? idMatch[1] : ''}`,
      source: 'rules',
    };
  }

  if (/cancel .*payment|cancel payment/.test(t)) {
    const idMatch = t.match(/#?(\d+)/);
    return {
      type: 'cancel_payment',
      payload: { paymentId: idMatch ? idMatch[1] : undefined, raw: text },
      confidence: 0.9,
      summary: `Cancel payment ${idMatch ? idMatch[1] : ''}`,
      source: 'rules',
    };
  }

  if (/\bbalance\b/.test(t)) {
    const accountNumber = extractAccountNumber(t);
    return {
      type: 'check_balance',
      payload: { accountNumber, raw: text },
      confidence: accountNumber ? 0.8 : 0.4,
      summary: `Check balance for ${accountNumber ?? 'account'}`,
      readOnly: true,
      source: 'rules',
    };
  }

  if (/(list|show|all)\b.*\baccounts?\b/.test(t)) {
    return {
      type: 'list_accounts',
      payload: { raw: text },
      confidence: 0.8,
      summary: 'List accounts',
      readOnly: true,
      source: 'rules',
    };
  }

  if (/status\b.*\bpayment\b|\bpayment\b.*\bstatus\b/.test(t)) {
    const idMatch = t.match(/#?(\d+)/);
    return {
      type: 'payment_status',
      payload: { paymentId: idMatch ? idMatch[1] : undefined, raw: text },
      confidence: idMatch ? 0.85 : 0.4,
      summary: `Check status of payment ${idMatch ? idMatch[1] : ''}`,
      readOnly: true,
      source: 'rules',
    };
  }

  return {
    type: 'unknown',
    payload: { raw: text },
    confidence: 0.2,
    summary: 'Could not parse intent',
    source: 'rules',
  };
}

/**
 * Best-effort account number extraction for single-account queries (e.g.
 * balance checks): tries "account <id>" phrasing first, then falls back to
 * common account-number shapes like "ACC-10001" or a bare numeric id.
 */
function extractAccountNumber(t: string): string | undefined {
  const explicit = t.match(/account\s*(?:number\s*)?([a-z0-9\-]+)/i);
  if (explicit) return explicit[1].toUpperCase();
  const shaped = t.match(/\b([a-z]{2,6}-\d{3,10})\b/i);
  if (shaped) return shaped[1].toUpperCase();
  const bare = t.match(/\b(\d{4,})\b/);
  return bare ? bare[1] : undefined;
}
