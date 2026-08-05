import dotenv from 'dotenv';
import axios from 'axios';
import { consumeCommands, connectRabbit } from './rabbit';

dotenv.config();

const PAYMENT_API = process.env.PAYMENT_API_URL || 'http://localhost:8082';
const BOT_API_KEY = process.env.BOT_API_KEY;

async function handleMessage(command: any) {
  console.log('Worker received command', command);
  const { type, payload } = command;
  try {
    if (type === 'create_payment') {
      // Map payload to payment API expected shape (best-effort)
      const body = {
        amount: payload.amount || 0,
        currency: payload.currency || 'INR',
        sourceAccount: payload.accounts?.[0] || 'unknown',
        destinationAccount: payload.accounts?.[1] || 'unknown',
        metadata: { via: 'bot', raw: payload.raw }
      };
      const headers: any = {};
      if (BOT_API_KEY) headers['Authorization'] = `Bearer ${BOT_API_KEY}`;
      const resp = await axios.post(`${PAYMENT_API}/payments`, body, { headers });
      console.log('Payment API response', resp.status);
    } else if (type === 'retry_payment') {
      const id = payload.paymentId;
      if (!id) throw new Error('paymentId missing');
      const headers: any = {};
      if (BOT_API_KEY) headers['Authorization'] = `Bearer ${BOT_API_KEY}`;
      await axios.post(`${PAYMENT_API}/payments/${id}/retry`, {}, { headers });
    } else if (type === 'cancel_payment') {
      const id = payload.paymentId;
      if (!id) throw new Error('paymentId missing');
      const headers: any = {};
      if (BOT_API_KEY) headers['Authorization'] = `Bearer ${BOT_API_KEY}`;
      await axios.post(`${PAYMENT_API}/payments/${id}/cancel`, {}, { headers });
    } else {
      console.warn('Unknown command type', type);
    }
  } catch (err) {
    console.error('Error executing command', err);
    throw err;
  }
}

export async function startWorker() {
  try {
    await connectRabbit();
    console.log('Worker connected to AMQP (or using in-memory)');
    await consumeCommands(handleMessage);
  } catch (err) {
    console.error('Worker startup failed', err);
    process.exit(1);
  }
}

if (require.main === module) startWorker();
