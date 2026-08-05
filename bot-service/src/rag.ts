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
  try {
    const resp = await axios.post(
      `${SLM_BASE_URL}/api/embeddings`,
      { model: EMBED_MODEL, prompt: text },
      { timeout: 8000 }
    );
    return resp.data?.embedding ?? null;
  } catch (err: any) {
    console.warn('[rag] embedding request failed:', err.message || err);
    return null;
  }
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

/**
 * Builds the in-memory embedding index once at startup. Safe to call
 * multiple times (no-op after the first successful build). Never throws —
 * failures leave RAG disabled and callers fall back to no extra context.
 */
export async function initRag(): Promise<void> {
  if (!RAG_ENABLED) {
    indexError = 'disabled via RAG_ENABLED=false';
    return;
  }
  try {
    const chunks = loadChunks();
    if (chunks.length === 0) {
      indexError = `no knowledge chunks found in ${KB_DIR}`;
      return;
    }
    const embedded: Chunk[] = [];
    for (const chunk of chunks) {
      const vec = await embed(chunk.text);
      if (vec) embedded.push({ ...chunk, embedding: vec });
    }
    if (embedded.length === 0) {
      indexError = 'embedding model unreachable (is Ollama running with EMBED_MODEL pulled?)';
      return;
    }
    index = embedded;
    indexReady = true;
    indexError = null;
    console.log(`[rag] indexed ${index.length} knowledge chunks using model "${EMBED_MODEL}"`);
  } catch (err: any) {
    indexError = err.message || String(err);
    console.warn('[rag] index build failed:', indexError);
  }
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
