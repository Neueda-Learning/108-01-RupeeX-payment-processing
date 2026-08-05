import amqp from 'amqplib';

const QUEUE = process.env.BOT_COMMAND_QUEUE || 'bot.commands';
let channel: amqp.Channel | null = null;

// In-memory fallback queue for prototype mode when RabbitMQ is not available.
const inMemoryQueue: any[] = [];
let inMemoryConsumers: Array<(msg: any) => Promise<void>> = [];

export async function connectRabbit(amqpUrl?: string) {
  const url = amqpUrl || process.env.AMQP_URL || 'amqp://localhost';
  try {
    const conn = await amqp.connect(url);
    channel = await conn.createChannel();
    await channel.assertQueue(QUEUE, { durable: true });
    return channel;
  } catch (err: any) {
    console.warn('AMQP connect failed, using in-memory queue for prototype:', err.message || err);
    // fall back to in-memory queue; no channel returned
    return null as any;
  }
}

export async function publishCommand(command: any) {
  if (!channel) {
    // fallback
    inMemoryQueue.push(command);
    return true;
  }
  const payload = Buffer.from(JSON.stringify(command));
  return channel.sendToQueue(QUEUE, payload, { persistent: true });
}

export async function consumeCommands(onMessage: (msg: any) => Promise<void>) {
  if (!channel) {
    // Register consumer for in-memory queue
    inMemoryConsumers.push(onMessage);
    // Start drain loop
    const drain = async () => {
      while (inMemoryQueue.length > 0) {
        const cmd = inMemoryQueue.shift();
        try {
          for (const c of inMemoryConsumers) await c(cmd);
        } catch (err) {
          console.error('In-memory consumer failed', err);
        }
      }
    };
    // run drain periodically
    setInterval(drain, 500);
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
