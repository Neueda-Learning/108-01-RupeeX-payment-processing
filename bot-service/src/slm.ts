import axios from 'axios';
import { retrieveContext } from './rag';
import type { BotUser } from './access';

/**
 * On-premise Small Language Model (SLM) integration.
 *
 * Uses Ollama (https://ollama.com) running locally/on-prem to host small
 * open-weight models (e.g. qwen2.5:0.5b, phi3:mini, llama3.2:1b). No data
 * leaves the network — Ollama serves the model over a local HTTP API.
 *
 * A small on-premise RAG layer (see ./rag.ts) retrieves relevant domain
 * knowledge (payment statuses, fraud rule types, RBAC rules) and injects it
 * into the prompt so the SLM stays grounded in the actual system behavior.
 *
 * Docker: both the SLM and the embedding model run inside the `ollama`
 * service defined in docker-compose.bot.yml — nothing needs to be installed
 * on the host.
 */

const SLM_BASE_URL = process.env.SLM_BASE_URL || 'http://localhost:11434';
const SLM_MODEL = process.env.SLM_MODEL || 'qwen2.5:0.5b';
const SLM_TIMEOUT_MS = Number(process.env.SLM_TIMEOUT_MS || 8000);

export type SlmIntent = {
  type:
    | 'create_payment'
    | 'retry_payment'
    | 'cancel_payment'
    | 'query_payments'
    | 'check_balance'
    | 'list_accounts'
    | 'payment_status'
    | 'unknown';
  amount?: number;
  currency?: string;
  sourceAccount?: string;
  destinationAccount?: string;
  paymentId?: string;
  confidence?: number;
  reply?: string;
};

const SYSTEM_PROMPT = `You are an intent extraction and conversational engine for a payment processing system.
Given a user's natural language request, respond with ONLY a single-line JSON object (no markdown, no explanation) matching this schema:
{"type":"create_payment|retry_payment|cancel_payment|query_payments|check_balance|list_accounts|payment_status|unknown","amount":number|null,"currency":string|null,"sourceAccount":string|null,"destinationAccount":string|null,"paymentId":string|null,"confidence":number,"reply":string|null}
Rules:
- "type" must be one of the enumerated values.
- For check_balance requests, put the account number to look up in "sourceAccount". If a specific currency is mentioned, put it in "currency".
- For list_accounts requests, all fields except type/confidence should be null.
- For payment_status requests, put the payment id in "paymentId".
- Use null for fields that are not present in the request.
- If the request is a general greeting (e.g., "hi", "good morning"), a pleasantry ("how are you", "thanks"), or a question about capabilities ("what can you do?"), set "type" to "unknown" and provide a polite, concise conversational response in the "reply" field.
- "confidence" is a number between 0 and 1 reflecting how sure you are.
- Do not include any text outside the JSON object.

Examples:
User: hi there
{"type":"unknown","amount":null,"currency":null,"sourceAccount":null,"destinationAccount":null,"paymentId":null,"confidence":1,"reply":"Hello! How can I help you with your payments or balance today?"}
User: what can you do?
{"type":"unknown","amount":null,"currency":null,"sourceAccount":null,"destinationAccount":null,"paymentId":null,"confidence":1,"reply":"I can help you check your account balances, list your accounts, check payment statuses, and create or cancel payments."}
User: thank you!
{"type":"unknown","amount":null,"currency":null,"sourceAccount":null,"destinationAccount":null,"paymentId":null,"confidence":1,"reply":"You're very welcome! Let me know if you need anything else."}
User: how are you doing?
{"type":"unknown","amount":null,"currency":null,"sourceAccount":null,"destinationAccount":null,"paymentId":null,"confidence":1,"reply":"I'm doing great, thank you! Ready to assist you with your RupeeX account."}
User: what is my balance?
{"type":"check_balance","amount":null,"currency":null,"sourceAccount":null,"destinationAccount":null,"paymentId":null,"confidence":0.9,"reply":null}
User: what is my balance in USD?
{"type":"check_balance","amount":null,"currency":"USD","sourceAccount":null,"destinationAccount":null,"paymentId":null,"confidence":0.9,"reply":null}
User: send 100 EUR to ACC-123
{"type":"create_payment","amount":100,"currency":"EUR","sourceAccount":null,"destinationAccount":"ACC-123","paymentId":null,"confidence":0.9,"reply":null}`;

/**
 * Calls the local Ollama server to extract a structured intent from free text.
 * Retrieves grounding context from the local RAG index first (if available)
 * and appends it to the system prompt. Returns null if the SLM is
 * unreachable or the response cannot be parsed, so the caller can fall back
 * to the rule-based parser.
 */
export async function extractIntentWithSLM(text: string, user?: any): Promise<SlmIntent | null> {
  try {
    const contextChunks = await retrieveContext(text);
    let systemPrompt = contextChunks.length
      ? `${SYSTEM_PROMPT}\n\nRelevant domain context:\n${contextChunks.map((c) => `- ${c}`).join('\n')}`
      : SYSTEM_PROMPT;

    if (user?.name) {
      systemPrompt += `\nThe current user is ${user.name}.`;
      if (user.accountNumber) {
        systemPrompt += ` Their primary account number is ${user.accountNumber}.`;
      }
    }

    const resp = await axios.post(
      `${SLM_BASE_URL}/api/generate`,
      {
        model: SLM_MODEL,
        system: systemPrompt,
        prompt: text,
        stream: false,
        format: 'json',
        options: { temperature: 0 },
      },
      { timeout: SLM_TIMEOUT_MS }
    );

    const resultText = resp.data?.response;
    console.log('[slm] raw response:', resultText);
    if (!resultText) return null;

    return parseModelJson(resultText);
  } catch (err: any) {
    console.warn('[slm] extraction failed, will fall back to rule-based parser:', err.message || err);
    return null;
  }
}

function parseModelJson(raw: string): SlmIntent | null {
  if (!raw) return null;
  // Models sometimes wrap JSON in code fences despite instructions; strip them defensively.
  const cleaned = raw.trim().replace(/^```(json)?/i, '').replace(/```$/, '').trim();
  const jsonMatch = cleaned.match(/\{[\s\S]*\}/);
  const candidate = jsonMatch ? jsonMatch[0] : cleaned;

  try {
    const parsed = JSON.parse(candidate);
    const type = parsed.type;
    const allowed = [
      'create_payment',
      'retry_payment',
      'cancel_payment',
      'query_payments',
      'check_balance',
      'list_accounts',
      'payment_status',
      'unknown',
    ];
    if (!allowed.includes(type)) return null;
    return {
      type,
      amount: typeof parsed.amount === 'number' ? parsed.amount : undefined,
      currency: parsed.currency || undefined,
      sourceAccount: parsed.sourceAccount || undefined,
      destinationAccount: parsed.destinationAccount || undefined,
      paymentId: parsed.paymentId ? String(parsed.paymentId) : undefined,
      confidence: typeof parsed.confidence === 'number' ? parsed.confidence : 0.5,
      reply: parsed.reply || undefined,
    };
  } catch (err) {
    console.warn('[slm] could not parse model output as JSON:', raw);
    return null;
  }
}

export async function isSlmAvailable(): Promise<boolean> {
  try {
    await axios.get(`${SLM_BASE_URL}/api/tags`, { timeout: 2000 });
    return true;
  } catch {
    return false;
  }
}

