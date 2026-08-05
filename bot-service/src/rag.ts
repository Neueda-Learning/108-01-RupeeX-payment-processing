import fs from 'fs';
import path from 'path';
import axios from 'axios';

/**
 * Small on-premise RAG (Retrieval-Augmented Generation) layer.
 *
 * - Knowledge base: short markdown files under ../knowledge, chunked by
 *   blank-line paragraphs.
 * - Embeddings: computed locally via Ollama's /api/embeddings endpoint
 *   (e.g. model "nomic-embed-text" or "all-minilm") — no data leaves
 *   the network.
 * - Index: a simple in-memory vector store with cosine-similarity search.
 *   Sufficient for a small, static knowledge base; swap for a real vector
 *   DB (pgvector, Qdrant, etc.) if the corpus grows.
 */

const RAG_ENABLED = process.env.RAG_ENABLED !== 'false';
const SLM_BASE_URL = process.env.SLM_BASE_URL || 'http://localhost:11434';
const EMBED_MODEL = process.env.EMBED_MODEL || 'nomic-embed-text';
const RAG_TOP_K = Number(process.env.RAG_TOP_K || 3);
const KB_DIR = process.env.RAG_KB_DIR || path.join(__dirname, '..', 'knowledge');

type Chunk = { id: string; text: string; source: string; embedding?: number[] };

let index: Chunk[] = [];
let indexReady = false;
let indexError: string | null = null;

function loadChunks(): Chunk[] {
  if (!fs.existsSync(KB_DIR)) return [];
  const files = fs.readdirSync(KB_DIR).filter((f) => f.endsWith('.md'));
  const chunks: Chunk[] = [];
  for (const file of files) {
    const full = path.join(KB_DIR, file);
    const raw = fs.readFileSync(full, 'utf-8');
    const paragraphs = raw
      .split(/\n\s*\n/)
      .map((p) => p.trim())
      .filter((p) => p.length > 20); // skip tiny/empty fragments
    paragraphs.forEach((p, i) => {
      chunks.push({ id: `${file}#${i}`, text: p, source: file });
    });
  }
  return chunks;
}

async function embed(text: string): Promise<number[] | null> {
  const url = `${SLM_BASE_URL}/api/embeddings`;
  // Try several payload shapes and parse multiple common response formats
  const attempts = [
    { model: EMBED_MODEL, input: text },
    { model: EMBED_MODEL, prompt: text },
    { model: EMBED_MODEL, inputs: [text] },
  ];

  for (const payload of attempts) {
    try {
      const resp = await axios.post(url, payload, { timeout: 8000 });
      const d = resp.data ?? {};

      // Common return shapes:
      // { embedding: [...] }
      if (Array.isArray(d.embedding) && typeof d.embedding[0] === 'number') return d.embedding as number[];

      // { embeddings: [[...]] }
      if (Array.isArray(d.embeddings) && Array.isArray(d.embeddings[0]) && typeof d.embeddings[0][0] === 'number') {
        return d.embeddings[0] as number[];
      }

      // { data: [{ embedding: [...] }, ...] }
      if (Array.isArray(d.data) && Array.isArray(d.data[0]?.embedding)) return d.data[0].embedding as number[];

      // Some runtimes return { results: [...] }
      if (Array.isArray(d.results) && Array.isArray(d.results[0]?.embedding)) return d.results[0].embedding as number[];

      // If response contained an embeddings-like field with a single numeric array, try to find it
      for (const v of Object.values(d)) {
        if (Array.isArray(v) && typeof v[0] === 'number') return v as number[];
      }
    } catch (err: any) {
      // Log but keep trying other payload shapes; most failures here are connection or 4xx/5xx responses
      const status = err?.response?.status ? ` status=${err.response.status}` : '';
      console.warn(`[rag] embedding request failed (${status}) for payload keys=${Object.keys(payload)}:`, err.message || err);
    }
  }

  return null;
}

function cosineSimilarity(a: number[], b: number[]): number {
  let dot = 0;
  let normA = 0;
  let normB = 0;
  for (let i = 0; i < a.length; i++) {
    dot += a[i] * b[i];
    normA += a[i] * a[i];
    normB += b[i] * b[i];
  }
  if (normA === 0 || normB === 0) return 0;
  return dot / (Math.sqrt(normA) * Math.sqrt(normB));
}

const RAG_RETRY_MS = Number(process.env.RAG_RETRY_MS || 15000);
const RAG_MAX_RETRIES = Number(process.env.RAG_MAX_RETRIES || 20); // ~5 min at default interval

/**
 * Attempts a single pass at building the in-memory embedding index.
 * Returns true on success, false if the embedding model is still
 * unreachable (e.g. `ollama-pull` hasn't finished downloading it yet).
 */
async function tryBuildIndex(): Promise<boolean> {
  const chunks = loadChunks();
  if (chunks.length === 0) {
    indexError = `no knowledge chunks found in ${KB_DIR}`;
    return true; // nothing to retry for — not a transient failure
  }
  const embedded: Chunk[] = [];
  for (const chunk of chunks) {
    const vec = await embed(chunk.text);
    if (vec) embedded.push({ ...chunk, embedding: vec });
  }
  if (embedded.length === 0) {
    indexError = 'embedding model unreachable (is Ollama running with EMBED_MODEL pulled?)';
    return false;
  }
  index = embedded;
  indexReady = true;
  indexError = null;
  console.log(`[rag] indexed ${index.length} knowledge chunks using model "${EMBED_MODEL}"`);
  return true;
}

/**
 * Builds the in-memory embedding index at startup. If the embedding model
 * isn't reachable yet (common when `ollama-pull` is still downloading it in
 * the background), retries in the background every RAG_RETRY_MS up to
 * RAG_MAX_RETRIES times instead of failing permanently. Never throws —
 * failures leave RAG disabled and callers fall back to no extra context.
 */
export async function initRag(): Promise<void> {
  if (!RAG_ENABLED) {
    indexError = 'disabled via RAG_ENABLED=false';
    return;
  }
  try {
    const ok = await tryBuildIndex();
    if (ok) return;
  } catch (err: any) {
    indexError = err.message || String(err);
    console.warn('[rag] index build failed:', indexError);
  }

  // Background retry loop — doesn't block the initial startup log line.
  let attempt = 0;
  const retry = async () => {
    attempt += 1;
    if (indexReady || attempt > RAG_MAX_RETRIES) return;
    console.log(`[rag] retrying index build (attempt ${attempt}/${RAG_MAX_RETRIES})...`);
    try {
      const ok = await tryBuildIndex();
      if (ok) return;
    } catch (err: any) {
      indexError = err.message || String(err);
    }
    setTimeout(retry, RAG_RETRY_MS);
  };
  setTimeout(retry, RAG_RETRY_MS);
}

/**
 * Retrieves the top-K most relevant knowledge chunks for a query.
 * Returns an empty array if RAG is disabled/unavailable.
 */
export async function retrieveContext(query: string, topK: number = RAG_TOP_K): Promise<string[]> {
  if (!RAG_ENABLED || !indexReady || index.length === 0) return [];
  const queryVec = await embed(query);
  if (!queryVec) return [];

  const scored = index
    .map((chunk) => ({ chunk, score: cosineSimilarity(queryVec, chunk.embedding!) }))
    .sort((a, b) => b.score - a.score)
    .slice(0, topK);

  return scored.map((s) => s.chunk.text);
}

export function getRagStatus() {
  return {
    enabled: RAG_ENABLED,
    ready: indexReady,
    chunks: index.length,
    embedModel: EMBED_MODEL,
    kbDir: KB_DIR,
    error: indexError,
  };
}
