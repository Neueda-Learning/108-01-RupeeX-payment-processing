import dotenv from 'dotenv';
import axios from 'axios';
import crypto from 'crypto';
import { consumeCommands, connectRabbit } from './rabbit';

dotenv.config();

const PAYMENT_API = process.env.PAYMENT_API_URL || 'http://localhost:8082/api';
const BOT_API_KEY = process.env.BOT_API_KEY;
const DEFAULT_ORIGIN_COUNTRY = process.env.BOT_DEFAULT_ORIGIN_COUNTRY || 'IN';
const DEFAULT_DESTINATION_COUNTRY = process.env.BOT_DEFAULT_DESTINATION_COUNTRY || 'IN';

async function handleMessage(command: any) {
  console.log('Worker received command', command);
  const { type, payload } = command;
  try {
    if (type === 'create_payment') {
      const sourceAccount = payload.sourceAccount || payload.accounts?.[0];
      const destinationAccount = payload.destinationAccount || payload.accounts?.[1];
      if (!sourceAccount || !destinationAccount) {
        throw new Error('sourceAccount and destinationAccount are both required');
      }
      if (!payload.amount || payload.amount <= 0) {
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
      const headers: any = {};
      if (BOT_API_KEY) headers['Authorization'] = `Bearer ${BOT_API_KEY}`;
      const resp = await axios.post(`${PAYMENT_API}/payments`, body, { headers });
      console.log('Payment API response', resp.status, resp.data);
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
  } catch (err: any) {
    const detail = err?.response?.data ? JSON.stringify(err.response.data) : err.message || err;
    console.error('Error executing command:', detail);
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
