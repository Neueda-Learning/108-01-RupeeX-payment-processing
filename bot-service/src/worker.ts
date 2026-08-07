import dotenv from 'dotenv';
import axios from 'axios';
import crypto from 'crypto';
import { consumeCommands, connectRabbit } from './rabbit';
import { AccessDeniedError, assertOwnAccount, assertOwnsPayment } from './access';

dotenv.config();

const PAYMENT_API = process.env.PAYMENT_API_URL || 'http://localhost:8082/api';
const BOT_API_KEY = process.env.BOT_API_KEY;
const DEFAULT_ORIGIN_COUNTRY = process.env.BOT_DEFAULT_ORIGIN_COUNTRY || 'IN';
const DEFAULT_DESTINATION_COUNTRY = process.env.BOT_DEFAULT_DESTINATION_COUNTRY || 'IN';

async function handleMessage(command: any) {
  console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('🔔 Worker received command from queue');
  console.log('Type:', command.type);
  console.log('Payload:', JSON.stringify(command.payload, null, 2));
  console.log('User:', command.__user ? {
    name: command.__user.name,
    accountNumber: command.__user.accountNumber,
    role: command.__user.role
  } : 'none');
  if (command.__confirmedBy) {
    console.log('Confirmed by:', command.__confirmedBy, 'at', command.__confirmedAt);
  }
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

  const { type, payload, __user: user } = command;
  try {
    // Defense-in-depth: re-validate ownership here too, in case a command
    // ever reaches the queue without having been checked by the API layer.
    if (user && user.role !== 'admin') {
      if (type === 'create_payment') {
        assertOwnAccount(user, payload.sourceAccount || payload.accounts?.[0]);
      } else if (type === 'retry_payment' || type === 'cancel_payment') {
        const headers: any = {};
        if (BOT_API_KEY) headers['Authorization'] = `Bearer ${BOT_API_KEY}`;
        const existing = await axios.get(`${PAYMENT_API}/payments/${payload.paymentId}`, { headers });
        assertOwnsPayment(user, existing.data);
      }
    }
    if (type === 'create_payment') {
      console.log('=== CREATE PAYMENT COMMAND ===');
      console.log('Raw command payload:', JSON.stringify(payload, null, 2));
      console.log('User context:', user ? { name: user.name, accountNumber: user.accountNumber, role: user.role } : 'none');

      const sourceAccount = payload.sourceAccount || payload.accounts?.[0];
      const destinationAccount = payload.destinationAccount || payload.accounts?.[1];

      if (!sourceAccount || !destinationAccount) {
        console.error('❌ Missing accounts - source:', sourceAccount, 'destination:', destinationAccount);
        throw new Error('sourceAccount and destinationAccount are both required');
      }
      if (!payload.amount || payload.amount <= 0) {
        console.error('❌ Invalid amount:', payload.amount);
        throw new Error('amount must be a positive number');
      }

      // Map payload to the backend's PaymentPlatformRequest shape. All of
      // amount/currency/sourceAccount/destinationAccount/idempotencyKey/
      // originCountry/destinationCountry are @NotBlank/@NotNull on the
      // backend, so they must all be present or the request 400s.
      const body = {
        amount: payload.amount,
        currency: payload.currency || 'INR',
        sourceAccount,
        destinationAccount,
        idempotencyKey: payload.idempotencyKey || crypto.randomUUID(),
        originCountry: payload.originCountry || DEFAULT_ORIGIN_COUNTRY,
        destinationCountry: payload.destinationCountry || DEFAULT_DESTINATION_COUNTRY,
      };

      console.log('📤 Sending payment request to backend:', PAYMENT_API);
      console.log('Request body:', JSON.stringify(body, null, 2));

      const headers: any = {};
      if (BOT_API_KEY) headers['Authorization'] = `Bearer ${BOT_API_KEY}`;

      const resp = await axios.post(`${PAYMENT_API}/payments`, body, { headers });

      console.log('✅ Payment API response - Status:', resp.status);
      console.log('Response headers:', JSON.stringify(resp.headers, null, 2));
      console.log('Response data:', JSON.stringify(resp.data, null, 2));
      console.log('=== END CREATE PAYMENT ===');
    } else if (type === 'retry_payment') {
      console.log('=== RETRY PAYMENT COMMAND ===');
      const id = payload.paymentId;
      if (!id) {
        console.error('❌ Missing paymentId');
        throw new Error('paymentId missing');
      }
      console.log('Payment ID:', id);
      console.log('📤 Sending retry request to backend:', `${PAYMENT_API}/payments/${id}/retry`);

      const headers: any = {};
      if (BOT_API_KEY) headers['Authorization'] = `Bearer ${BOT_API_KEY}`;
      const resp = await axios.post(`${PAYMENT_API}/payments/${id}/retry`, {}, { headers });

      console.log('✅ Retry response - Status:', resp.status);
      console.log('Response data:', JSON.stringify(resp.data, null, 2));
      console.log('=== END RETRY PAYMENT ===');
    } else if (type === 'cancel_payment') {
      console.log('=== CANCEL PAYMENT COMMAND ===');
      const id = payload.paymentId;
      if (!id) {
        console.error('❌ Missing paymentId');
        throw new Error('paymentId missing');
      }
      console.log('Payment ID:', id);
      console.log('📤 Sending cancel request to backend:', `${PAYMENT_API}/payments/${id}/cancel`);

      const headers: any = {};
      if (BOT_API_KEY) headers['Authorization'] = `Bearer ${BOT_API_KEY}`;
      const resp = await axios.post(`${PAYMENT_API}/payments/${id}/cancel`, {}, { headers });

      console.log('✅ Cancel response - Status:', resp.status);
      console.log('Response data:', JSON.stringify(resp.data, null, 2));
      console.log('=== END CANCEL PAYMENT ===');
    } else {
      console.warn('Unknown command type', type);
    }
  } catch (err: any) {
    console.error('❌ Error executing command:');
    console.error('Command type:', type);

    if (err?.response) {
      // Backend returned an error response
      console.error('Backend error response:');
      console.error('  Status:', err.response.status, err.response.statusText);
      console.error('  Headers:', JSON.stringify(err.response.headers, null, 2));
      console.error('  Data:', JSON.stringify(err.response.data, null, 2));
    } else if (err?.message) {
      console.error('Error message:', err.message);
    } else {
      console.error('Unknown error:', err);
    }

    throw err;
  }
}

export async function startWorker() {
  console.log('Bot worker starting...');

  // Retry connection with exponential backoff
  const maxRetries = 10;
  let retryCount = 0;
  let connected = false;

  while (!connected && retryCount < maxRetries) {
    try {
      await connectRabbit();
      console.log('Worker connected to AMQP successfully');
      connected = true;
    } catch (err: any) {
      retryCount++;
      const delayMs = Math.min(1000 * Math.pow(2, retryCount), 30000); // Max 30 seconds
      console.warn(`Worker AMQP connection attempt ${retryCount}/${maxRetries} failed: ${err.message}`);

      if (retryCount < maxRetries) {
        console.log(`Retrying in ${delayMs}ms...`);
        await new Promise(resolve => setTimeout(resolve, delayMs));
      }
    }
  }

  if (!connected) {
    console.error('Worker failed to connect to AMQP after maximum retries. Using in-memory fallback.');
  }

  try {
    await consumeCommands(handleMessage);
    console.log('Worker is now listening for commands');
  } catch (err) {
    console.error('Worker startup failed', err);
    process.exit(1);
  }
}

if (require.main === module) startWorker();
