# RupeeX Bot Service

This service provides a small HTTP API to parse natural-language commands and queue them for execution against the payment microservice.

Environment
- `PORT` (default 4001)
- `AMQP_URL` (default amqp://localhost)
- `BOT_COMMAND_QUEUE` (default bot.commands)
- `PAYMENT_API_URL` (default http://localhost:8082 — the host-mapped backend port; note 8080 is typically reserved for Jenkins on this host)
- `BOT_API_KEY` (optional, forwarded to payment API)

Run locally (development):

```bash
cd bot-service
npm install
npm run dev
```

Prototype quick start (no RabbitMQ required):

1. Copy `.env.example` to `.env` and keep `START_WORKER=true`.
2. Run the service which will also start an embedded worker and use an in-memory queue:

```bash
cd bot-service
npm install
npm run dev
```

This mode uses an in-memory queue to process commands and is suitable for quick prototypes only.

## On-premise Small Language Model (SLM) + RAG

Natural-language intent extraction is powered by a small language model, grounded
with a small on-premise RAG (retrieval-augmented generation) layer — both run
entirely on-premise via [Ollama](https://ollama.com). No data leaves the network.

### Option A: Docker (recommended — single compose file)

Everything (db, backend, onboarding, frontend, nginx, rabbitmq, ollama, bot-service,
bot-worker) runs from the single root-level compose file:

```bash
docker compose up -d --build
```

This starts, among the existing app services:
- `ollama` — the model runtime (container, port 11434, persisted volume `ollama_data`)
- `ollama-pull` — a one-shot job that pulls `qwen2.5:0.5b` (chat) and `nomic-embed-text` (embeddings) into that volume, then exits
- `rabbitmq` — command queue for the bot (ports 5672/15672)
- `bot-service` / `bot-worker` — already configured with `SLM_BASE_URL=http://ollama:11434` and `PAYMENT_API_URL=http://app:8080`

Check progress:

```bash
docker compose logs -f ollama-pull
curl http://localhost:4001/slm/status
curl http://localhost:4001/rag/status
```

Through nginx (port 80), the frontend's chat UI talks to the bot service via
`/api/bot/nl`, which nginx routes directly to the `bot-service` container.

### Option B: Ollama installed on the host

1. Install Ollama: https://ollama.com/download
2. Pull the models:

```bash
ollama pull qwen2.5:0.5b
ollama pull nomic-embed-text
```

3. Set `SLM_ENABLED=true` in `.env` (see `.env.example`); `SLM_BASE_URL` defaults to `http://localhost:11434`.

### RAG knowledge base

Short domain docs live in `bot-service/knowledge/*.md` (payment lifecycle,
fraud rule types, RBAC rules). On startup the service embeds each paragraph
locally via Ollama and builds an in-memory cosine-similarity index. Every SLM
request retrieves the top `RAG_TOP_K` relevant chunks and injects them into
the prompt so answers stay grounded in actual system behavior. Add more `.md`
files to the `knowledge/` folder to extend it — no code changes required.

If the SLM or RAG index is disabled, unreachable, or returns a low-confidence/
`unknown` result, the service automatically falls back to the deterministic
rule-based parser in `src/intent.ts` (`parseIntentRules`), so the bot keeps
working even without Ollama running.


Build & run:

```bash
cd bot-service
npm install
npm run build
npm start
```

Worker

To run the background worker that consumes queued bot commands:

```bash
node dist/worker.js
```

Notes
- This is an initial scaffold. Replace the rule-based `parseIntent` with an LLM + RAG pipeline and add robust validation, RBAC, and tests before use in production.
- High-value commands (threshold controlled by `BOT_HIGH_VALUE_THRESHOLD`) will require explicit confirmation via the `/confirm` endpoint. This is a basic safeguard — integrate RBAC and approval workflows in production.

