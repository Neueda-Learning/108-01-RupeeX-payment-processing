import axios from 'axios';

/**
 * Thin client for read-only lookups against the Spring Boot payment
 * platform backend. Used for bot commands that only need to fetch data
 * (account balance, payment status, account listing) — these execute
 * synchronously and never touch the command queue since they have no
 * side effects and don't require confirmation.
 */

const PAYMENT_API = process.env.PAYMENT_API_URL || 'http://localhost:8082/api';
const BOT_API_KEY = process.env.BOT_API_KEY;

function authHeaders(): Record<string, string> {
  return BOT_API_KEY ? { Authorization: `Bearer ${BOT_API_KEY}` } : {};
}

export type AccountInfo = {
  accountNumber: string;
  accountHolder: string;
  accountType: string;
  currency: string;
  countryCode?: string;
  balance: number;
  status: string;
};

export type PaymentInfo = {
  paymentId: number;
  paymentReference?: string;
  amount: number;
  currency: string;
  sourceAccount: string;
  destinationAccount: string;
  status: string;
  createdAt?: string;
  errorMessage?: string;
};

export async function getAccountBalance(accountNumber: string): Promise<AccountInfo> {
  const resp = await axios.get(`${PAYMENT_API}/accounts/${encodeURIComponent(accountNumber)}`, {
    headers: authHeaders(),
    timeout: 8000,
  });
  return resp.data;
}

export async function listAccounts(): Promise<AccountInfo[]> {
  const resp = await axios.get(`${PAYMENT_API}/accounts`, {
    headers: authHeaders(),
    timeout: 8000,
  });
  return resp.data;
}

export async function getPaymentStatus(paymentId: string): Promise<PaymentInfo> {
  const resp = await axios.get(`${PAYMENT_API}/payments/${encodeURIComponent(paymentId)}`, {
    headers: authHeaders(),
    timeout: 8000,
  });
  return resp.data;
}
