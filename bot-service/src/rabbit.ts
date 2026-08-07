import amqp from 'amqplib';

const QUEUE = process.env.BOT_COMMAND_QUEUE || 'bot.commands';
let channel: amqp.Channel | null = null;

// In-memory fallback queue for prototype mode when RabbitMQ is not available.
const inMemoryQueue: any[] = [];
let inMemoryConsumers: Array<(msg: any) => Promise<void>> = [];
let isProcessing = false;

export async function connectRabbit(amqpUrl?: string) {
  const url = amqpUrl || process.env.AMQP_URL || 'amqp://localhost';
  console.log(`Attempting to connect to RabbitMQ at: ${url.replace(/\/\/.*@/, '//***@')}`);

  try {
    const conn = await amqp.connect(url);

    conn.on('error', (err) => {
      console.error('RabbitMQ connection error:', err.message);
    });

    conn.on('close', () => {
      console.warn('RabbitMQ connection closed');
    });

    channel = await conn.createChannel();
    await channel.assertQueue(QUEUE, { durable: true });
    console.log(`Successfully connected to RabbitMQ, queue: ${QUEUE}`);
    return channel;
  } catch (err: any) {
    console.warn('AMQP connect failed, using in-memory queue for prototype:', err.message || err);
    if (err.code === 'ECONNREFUSED') {
      console.warn(`Connection refused - RabbitMQ may not be running or not yet ready at ${url}`);
    }
    // fall back to in-memory queue; no channel returned
    return null as any;
  }
}

// Process in-memory queue - drain all pending commands
async function drainInMemoryQueue() {
  if (isProcessing || inMemoryQueue.length === 0 || inMemoryConsumers.length === 0) {
    return;
  }

  isProcessing = true;
  try {
    while (inMemoryQueue.length > 0) {
      const cmd = inMemoryQueue.shift();
      try {
        for (const consumer of inMemoryConsumers) {
          await consumer(cmd);
        }
      } catch (err) {
        console.error('In-memory consumer failed', err);
      }
    }
  } finally {
    isProcessing = false;
  }
}

export async function publishCommand(command: any) {
  if (!channel) {
    // fallback to in-memory queue
    console.log('[queue] Using in-memory fallback queue');
    inMemoryQueue.push(command);
    // Trigger immediate processing
    setImmediate(() => drainInMemoryQueue());
    return true;
  }
  const payload = Buffer.from(JSON.stringify(command));
  return channel.sendToQueue(QUEUE, payload, { persistent: true });
}

export async function consumeCommands(onMessage: (msg: any) => Promise<void>) {
  if (!channel) {
    // Register consumer for in-memory queue
    console.log('[queue] Registering in-memory queue consumer');
    inMemoryConsumers.push(onMessage);
    // Start periodic drain loop as backup
    setInterval(drainInMemoryQueue, 500);
    // Drain immediately in case there are already queued commands
    setImmediate(() => drainInMemoryQueue());
    return;
  }

  await channel.consume(QUEUE, async (msg: amqp.ConsumeMessage | null) => {
    if (!msg) return;
    try {
      const body = JSON.parse(msg.content.toString());
      await onMessage(body);
      channel!.ack(msg);
    } catch (err) {
      console.error('Failed to process message', err);
      channel!.nack(msg, false, false);
    }
  });
}
