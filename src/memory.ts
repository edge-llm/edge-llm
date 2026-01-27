/**
 * memory.ts
 *
 * Flexible memory system for AI apps with developer control.
 *
 * - Short-term memory → JS (conversation context)
 * - Long-term memory → Native RAG (SQLite + embeddings)
 *
 * Developer chooses:
 * ✅ askWithMemory() - both memories
 * ✅ askWithShortTermOnly() - chat history only
 * ✅ askWithLongTermOnly() - RAG only
 * ✅ Automatic token limiting to prevent prompt bloat
 */

import Mediapipe from "./index";

/* ---------------------------------------------
 * CONFIG
 * --------------------------------------------- */

const MAX_SHORT_TERM_ITEMS = 20;
const MAX_TOKENS_SHORT_TERM = 1000; // Limit conversation context
const MAX_TOKENS_LONG_TERM = 300;   // Limit RAG context

/* Rough estimate: 1 token ≈ 4 chars */
function estimateTokens(text: string): number {
  return Math.ceil(text.length / 4);
}

/* ---------------------------------------------
 * 1️⃣ SHORT-TERM MEMORY (SESSION)
 * --------------------------------------------- */

type MemoryItem = {
  role: "user" | "assistant" | "system";
  content: string;
};

const shortTermMemory: MemoryItem[] = [];

export function addShortTermMemory(
  role: MemoryItem["role"],
  content: string
) {
  shortTermMemory.push({ role, content });

  // Cap by item count
  if (shortTermMemory.length > MAX_SHORT_TERM_ITEMS) {
    shortTermMemory.shift();
  }
}

export function clearShortTermMemory() {
  shortTermMemory.length = 0;
}

/**
 * Get short-term context with token limiting.
 * Returns only as much history as fits within the token budget.
 */
export function getShortTermContext(maxTokens: number = MAX_TOKENS_SHORT_TERM): string {
  let context = "";
  let tokenCount = 0;

  // Iterate backwards (most recent first) to keep latest context
  for (let i = shortTermMemory.length - 1; i >= 0; i--) {
    const item = shortTermMemory[i];
    const line = `${item.role.toUpperCase()}: ${item.content}`;
    const tokens = estimateTokens(line);

    if (tokenCount + tokens > maxTokens) {
      break; // Stop if we exceed budget
    }

    context = line + "\n" + context;
    tokenCount += tokens;
  }

  return context.trim();
}

/**
 * Peek at full short-term memory (for debugging)
 */
export function getAllShortTermMemory(): MemoryItem[] {
  return [...shortTermMemory];
}

export function getShortTermLength(): number {
  return shortTermMemory.length;
}

/* ---------------------------------------------
 * 2️⃣ LONG-TERM MEMORY (PERSISTENT RAG)
 * --------------------------------------------- */

/**
 * Store something permanently in the RAG database.
 * Developer decides WHAT is important to persist.
 */
export async function storeLongTermMemory(text: string): Promise<void> {
  await Mediapipe.addRagDocument(text);
}

/**
 * Clear entire long-term memory
 */
export async function clearLongTermMemory(): Promise<void> {
  await Mediapipe.clearDB("");
}

/**
 * Get stats about stored memory
 */
export async function getLongTermStats(): Promise<{ documentCount: number }> {
  return await Mediapipe.getRagStats();
}

/* ---------------------------------------------
 * 3️⃣ ASK WITH MEMORY (3 MODES)
 * --------------------------------------------- */

type AskMode = "both" | "short-term-only" | "long-term-only";

interface AskOptions {
  maxShortTermTokens?: number;
  maxLongTermTokens?: number;
  systemPrompt?: string;
}

/**
 * MODE 1: Ask with BOTH short-term + long-term memory
 * 
 * ✅ Uses conversation history
 * ✅ Uses RAG for knowledge
 * ✅ Best for: General chat with context
 */
export async function askWithMemory(
  question: string,
  options: AskOptions = {}
): Promise<string> {
  const {
    maxShortTermTokens = MAX_TOKENS_SHORT_TERM,
    maxLongTermTokens = MAX_TOKENS_LONG_TERM,
    systemPrompt = "You are a helpful assistant with memory.",
  } = options;

  const shortContext = getShortTermContext(maxShortTermTokens);
  const shortTermSection = shortContext
    ? `\nRecent conversation:\n${shortContext}`
    : "";

  // RAG will automatically find relevant long-term memory
  const finalPrompt = `${systemPrompt}${shortTermSection}\n\nQuestion: ${question}`.trim();

  const response = await Mediapipe.generateWithRag(finalPrompt);

  // Auto-append to short-term memory
  addShortTermMemory("user", question);
  addShortTermMemory("assistant", response);

  return response;
}

/**
 * MODE 2: Ask with ONLY short-term memory (chat history)
 * 
 * ✅ Fast, uses only conversation context
 * ✅ No RAG lookup overhead
 * ✅ Best for: Quick replies, chat continuity, no knowledge needed
 */
export async function askWithShortTermOnly(
  question: string,
  options: AskOptions = {}
): Promise<string> {
  const {
    maxShortTermTokens = MAX_TOKENS_SHORT_TERM,
    systemPrompt = "You are a helpful assistant.",
  } = options;

  const shortContext = getShortTermContext(maxShortTermTokens);
  const shortTermSection = shortContext
    ? `\nConversation so far:\n${shortContext}`
    : "";

  const finalPrompt = `${systemPrompt}${shortTermSection}\n\nUser: ${question}`.trim();

  // Use generateText instead of generateWithRag (faster, no RAG)
  const response = await Mediapipe.generateText(finalPrompt);

  // Auto-append to short-term memory
  addShortTermMemory("user", question);
  addShortTermMemory("assistant", response);

  return response;
}

/**
 * MODE 3: Ask with ONLY long-term memory (RAG)
 * 
 * ✅ Uses knowledge base only
 * ✅ Ignores conversation history
 * ✅ Best for: Knowledge queries, document Q&A, fresh lookups
 */
export async function askWithLongTermOnly(
  question: string,
  options: AskOptions = {}
): Promise<string> {
  const {
    maxLongTermTokens = MAX_TOKENS_LONG_TERM,
    systemPrompt = "You are a knowledgeable assistant.",
  } = options;

  const finalPrompt = `${systemPrompt}\n\nQuestion: ${question}`.trim();

  // RAG lookup without short-term context
  const response = await Mediapipe.generateWithRag(finalPrompt);

  // DON'T add to short-term memory (intentional isolation)
  // This keeps knowledge queries separate from chat

  return response;
}

/* ---------------------------------------------
 * 4️⃣ UTILITY: EXPORT ALL MEMORY (FOR DEBUGGING)
 * --------------------------------------------- */

export interface MemoryExport {
  shortTerm: MemoryItem[];
  shortTermTokenCount: number;
  longTermDocCount: number;
}

export async function exportMemory(): Promise<MemoryExport> {
  const stats = await Mediapipe.getRagStats();
  
  return {
    shortTerm: [...shortTermMemory],
    shortTermTokenCount: estimateTokens(getShortTermContext()),
    longTermDocCount: stats.documentCount,
  };
}

/* ---------------------------------------------
 * 5️⃣ MEMORY MANAGEMENT
 * --------------------------------------------- */

export async function resetAllMemory(): Promise<void> {
  clearShortTermMemory();
  await clearLongTermMemory();
}