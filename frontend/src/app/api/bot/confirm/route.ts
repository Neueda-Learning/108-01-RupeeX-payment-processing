import { NextResponse } from 'next/server';

export async function POST(req: Request) {
  const body = await req.json();
  const BOT_URL = process.env.BOT_SERVICE_URL || 'http://localhost:4001';

  const resp = await fetch(`${BOT_URL}/confirm`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  const data = await resp.json();
  return NextResponse.json(data, { status: resp.status });
}
