# edge-llm - On-Device AI for React Native

**edge-llm** is an Expo native module that brings powerful on-device AI capabilities to React Native applications using Google's MediaPipe framework. Run LLMs, embeddings, and RAG (Retrieval-Augmented Generation) entirely offline on mobile devices.

---

## 🚀 Features

- ✅ **On-Device LLM Inference** - Run Gemma 2B models directly on mobile
- ✅ **Text Embeddings** - Generate 384-dimensional vectors using MediaPipe BERT
- ✅ **RAG (Retrieval-Augmented Generation)** - Built-in vector database with cosine similarity search
- ✅ **100% Offline** - No internet connection required
- ✅ **Expo Compatible** - Works with Expo development builds
- ✅ **TypeScript Support** - Full type definitions included

---

## 📦 Installation

### 1. Install the Module

```bash
npm install edge-llm
# or
yarn add edge-llm
```

### 2. Add Model Files

Download the required model files and place them in your project:

```
your-app/
├── android/app/src/main/assets/
│   ├── gemma3n_e2b_int4.task     # LLM model (required)
│   └── bert_embedder.tflite      # Embedding model (required)
```

**Model Downloads:**
- **Gemma 2B INT4** - [Download from MediaPipe](https://developers.google.com/mediapipe/solutions/genai/llm_inference)
- **BERT Embedder** - [Download from MediaPipe](https://developers.google.com/mediapipe/solutions/text/text_embedder)

### 3. Rebuild Your App

```bash
npx expo prebuild --clean
npx expo run:android
```

---

## 🎯 Quick Start

```typescript
import Mediapipe from 'edge-llm';
import { useEffect, useState } from 'react';

export default function App() {
  const [response, setResponse] = useState('');

  useEffect(() => {
    const initializeAI = async () => {
      try {
        // Test the model
        const result = await Mediapipe.testModel();
        console.log('Model loaded:', result.status);

        // Generate text
        const output = await Mediapipe.generateText('Hello, how are you?');
        setResponse(output.response);
      } catch (error) {
        console.error('AI Error:', error);
      }
    };

    initializeAI();
  }, []);

  return <Text>{response}</Text>;
}
```

---

## 📚 API Reference

### Core Functions

#### `mediapipeSmokeTest()`
Test if the MediaPipe engine initializes correctly.

```typescript
const result = await Mediapipe.mediapipeSmokeTest();
// Returns: { status: 'ok', message: 'MediaPipe engine initialized successfully' }
```

#### `testModel()`
Run a simple test prompt through the model.

```typescript
const result = await Mediapipe.testModel();
// Returns: {
//   status: 'success',
//   testPrompt: 'Hello',
//   testResponse: '...',
//   responseLength: 50,
//   duration: 1234
// }
```

#### `generateText(prompt: string)`
Generate text from a prompt.

```typescript
const result = await Mediapipe.generateText('Explain quantum computing');
// Returns: {
//   status: 'success',
//   response: '...',
//   duration: 2000,
//   promptLength: 25,
//   responseLength: 150
// }
```

---

### Embedding Functions

#### `embedTest(text: string)`
Generate embeddings for text (384-dimensional vector).

```typescript
const result = await Mediapipe.embedTest('Hello world');
// Returns: {
//   length: 384,
//   sample: [18.3, -0.16, 14.69, -12.54, -23.61]
// }
```

---

### RAG (Retrieval-Augmented Generation) Functions

#### `clearRagDatabase()`
Clear all documents from the RAG database.

```typescript
const result = await Mediapipe.clearRagDatabase();
// Returns: { status: 'success', message: 'RAG database cleared' }
```

#### `getRagStats()`
Get statistics about the RAG database.

```typescript
const result = await Mediapipe.getRagStats();
// Returns: { status: 'success', documentCount: 4 }
```

#### `addRagDocument(text: string)`
Add a document to the RAG database.

```typescript
const result = await Mediapipe.addRagDocument(`
  Project Name: NEBULA-7
  Lead Engineer: Tirth Parmar
  Status: Active
`);
// Returns: { status: 'stored', length: 78, totalDocuments: 1 }
```

**Validation:**
- Empty documents are rejected
- Error JSON objects are automatically rejected
- Returns current document count

#### `generateWithRag(prompt: string)`
Generate text using RAG (retrieves relevant context first).

```typescript
const result = await Mediapipe.generateWithRag('Who is the lead engineer?');
// Returns: {
//   status: 'success',
//   response: 'Tirth Parmar',
//   contextUsed: '...',
//   promptLength: 152,
//   bestScore: 0.856
// }
```

**How it works:**
1. Embeds your query
2. Searches database for most similar document (cosine similarity)
3. Uses top match as context
4. Generates response using LLM

---

### Memory System

EdgeLLM includes a powerful **dual-memory system** that gives you complete control over conversation context and knowledge storage.

#### Architecture

```
┌─────────────────────────────────────────┐
│         SHORT-TERM MEMORY               │
│  (JavaScript Array - Session Only)      │
│  • Conversation history                 │
│  • Recent context (20 messages max)     │
│  • Auto token limiting (1000 tokens)    │
└─────────────────────────────────────────┘
                    ↕
         Memory System API
                    ↕
┌─────────────────────────────────────────┐
│         LONG-TERM MEMORY                │
│  (SQLite + Embeddings - Persistent)     │
│  • Knowledge base                       │
│  • Vector search retrieval              │
│  • Survives app restarts                │
└─────────────────────────────────────────┘
```

#### Import Memory System

```typescript
import {
  // Memory functions
  askWithMemory,
  askWithShortTermOnly,
  askWithLongTermOnly,
  
  // Short-term management
  addShortTermMemory,
  clearShortTermMemory,
  getShortTermContext,
  getAllShortTermMemory,
  getShortTermLength,
  
  // Long-term management
  storeLongTermMemory,
  clearLongTermMemory,
  getLongTermStats,
  
  // Utilities
  exportMemory,
  resetAllMemory
} from 'edge-llm/memory';
```

#### Three Ways to Ask Questions

##### 1️⃣ `askWithMemory()` - Both Memories
Uses **both** conversation history AND knowledge base.

```typescript
// Best for: General chat with full context
const answer = await askWithMemory('What did we discuss earlier?');
```

**Features:**
- ✅ Accesses conversation history
- ✅ Retrieves relevant knowledge from RAG
- ✅ Auto-appends to short-term memory
- ✅ Token-limited (prevents bloat)

##### 2️⃣ `askWithShortTermOnly()` - Chat History Only
Uses **only** conversation history (no RAG lookup).

```typescript
// Best for: Quick replies, chat continuity
const answer = await askWithShortTermOnly('What was my last question?');
```

**Features:**
- ✅ Fast (no vector search)
- ✅ Access to conversation context
- ✅ Auto-appends to short-term memory
- ❌ No knowledge base access

##### 3️⃣ `askWithLongTermOnly()` - Knowledge Base Only
Uses **only** RAG knowledge base (ignores chat history).

```typescript
// Best for: Knowledge queries, document Q&A
const answer = await askWithLongTermOnly('What is our refund policy?');
```

**Features:**
- ✅ Pure knowledge lookup
- ✅ Fresh context (no chat history bias)
- ❌ Doesn't remember conversation
- ❌ Doesn't auto-save to short-term

#### Configuration Options

All `ask*()` functions accept optional configuration:

```typescript
const answer = await askWithMemory('Hello!', {
  maxShortTermTokens: 1500,  // Default: 1000
  maxLongTermTokens: 500,    // Default: 300
  systemPrompt: 'You are a friendly assistant.'
});
```

#### Manual Memory Management

##### Short-Term Memory (Session)

```typescript
// Add messages manually
addShortTermMemory('user', 'What is AI?');
addShortTermMemory('assistant', 'AI is...');

// Get current context (token-limited)
const context = getShortTermContext(1000); // Max 1000 tokens
console.log(context);
// Output:
// USER: What is AI?
// ASSISTANT: AI is...

// Get all messages (for debugging)
const allMessages = getAllShortTermMemory();
console.log(allMessages);
// [{ role: 'user', content: 'What is AI?' }, ...]

// Get message count
const count = getShortTermLength(); // Returns: 2

// Clear session memory
clearShortTermMemory();
```

##### Long-Term Memory (Persistent)

```typescript
// Store important information permanently
await storeLongTermMemory(`
  User Preference: Dark mode enabled
  Notification: 9 AM daily
  Language: English
`);

// Get statistics
const stats = await getLongTermStats();
console.log(`Stored: ${stats.documentCount} documents`);

// Clear all long-term memory
await clearLongTermMemory();
```

#### Complete Example: Chatbot with Memory

```typescript
import { useState, useEffect } from 'react';
import {
  askWithMemory,
  storeLongTermMemory,
  getLongTermStats,
  getShortTermLength,
  resetAllMemory
} from 'edge-llm/memory';

export default function SmartChatbot() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [stats, setStats] = useState({ shortTerm: 0, longTerm: 0 });

  useEffect(() => {
    const initKnowledge = async () => {
      // Seed knowledge base
      await storeLongTermMemory(`
        Company: TechCorp
        Support Email: support@techcorp.com
        Office Hours: 9 AM - 5 PM EST
      `);

      await storeLongTermMemory(`
        Product: CloudAI Pro
        Price: $99/month
        Features: Auto-scaling, 99.9% uptime, API access
      `);

      updateStats();
    };

    initKnowledge();
  }, []);

  const updateStats = async () => {
    const longTerm = await getLongTermStats();
    const shortTerm = getShortTermLength();
    setStats({ shortTerm, longTerm: longTerm.documentCount });
  };

  const sendMessage = async () => {
    if (!input.trim()) return;

    // Add user message to UI
    setMessages(prev => [...prev, { role: 'user', text: input }]);
    setInput('');

    try {
      // Ask with BOTH memories
      const response = await askWithMemory(input, {
        systemPrompt: 'You are TechCorp support assistant.',
        maxShortTermTokens: 1500,
        maxLongTermTokens: 400
      });

      // Add AI response to UI
      setMessages(prev => [...prev, { role: 'assistant', text: response }]);
      updateStats();
    } catch (error) {
      console.error('Chat error:', error);
    }
  };

  const handleReset = async () => {
    await resetAllMemory();
    setMessages([]);
    updateStats();
    alert('Memory reset!');
  };

  return (
    <View style={{ flex: 1 }}>
      {/* Stats Bar */}
      <View style={{ padding: 10, backgroundColor: '#f0f0f0' }}>
        <Text>📝 Chat: {stats.shortTerm} messages | 📚 Knowledge: {stats.longTerm} docs</Text>
      </View>

      {/* Messages */}
      <FlatList
        data={messages}
        keyExtractor={(_, i) => i.toString()}
        renderItem={({ item }) => (
          <View style={{
            padding: 12,
            margin: 8,
            backgroundColor: item.role === 'user' ? '#DCF8C6' : '#FFF',
            borderRadius: 8,
            alignSelf: item.role === 'user' ? 'flex-end' : 'flex-start'
          }}>
            <Text>{item.text}</Text>
          </View>
        )}
      />

      {/* Input */}
      <View style={{ flexDirection: 'row', padding: 12 }}>
        <TextInput
          value={input}
          onChangeText={setInput}
          placeholder="Ask anything..."
          style={{ flex: 1, padding: 12, backgroundColor: '#F0F0F0', borderRadius: 8 }}
        />
        <TouchableOpacity onPress={sendMessage} style={{ marginLeft: 8, padding: 12 }}>
          <Text>Send</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={handleReset} style={{ marginLeft: 8, padding: 12 }}>
          <Text>Reset</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}
```

#### Advanced: Selective Memory Storage

Decide **what** gets stored in long-term memory:

```typescript
const response = await askWithShortTermOnly(userQuestion);

// Only store important facts
if (userQuestion.includes('remember') || userQuestion.includes('save')) {
  await storeLongTermMemory(`User noted: ${userQuestion}`);
}
```

#### Memory Export (Debugging)

```typescript
import { exportMemory } from 'edge-llm/memory';

const memoryDump = await exportMemory();
console.log('Short-term:', memoryDump.shortTerm);
console.log('Tokens used:', memoryDump.shortTermTokenCount);
console.log('Long-term docs:', memoryDump.longTermDocCount);

// Output:
// Short-term: [
//   { role: 'user', content: 'Hello' },
//   { role: 'assistant', content: 'Hi there!' }
// ]
// Tokens used: 25
// Long-term docs: 5
```

#### Token Management

The memory system automatically limits tokens to prevent prompt bloat:

| Memory Type | Default Limit | Purpose |
|-------------|---------------|---------|
| Short-term | 1000 tokens | Conversation context |
| Long-term | 300 tokens | RAG retrieval context |

**Token estimation:** ~4 characters ≈ 1 token

```typescript
// Example: 1000 tokens ≈ 4000 characters ≈ ~10 messages
```

#### Memory Strategies

##### Strategy 1: Chat-Heavy Application
```typescript
// Use short-term memory for natural conversation
const response = await askWithShortTermOnly(userInput);

// Only store critical facts
if (isImportantFact(userInput)) {
  await storeLongTermMemory(userInput);
}
```

##### Strategy 2: Knowledge-Heavy Application
```typescript
// Pre-load knowledge base
await storeLongTermMemory(companyPolicies);
await storeLongTermMemory(productDocs);

// Use long-term memory for queries
const response = await askWithLongTermOnly('What is the refund policy?');
```

##### Strategy 3: Hybrid (Recommended)
```typescript
// Default: Use both memories
const response = await askWithMemory(userInput);

// Override when needed
if (isKnowledgeQuery(userInput)) {
  const response = await askWithLongTermOnly(userInput);
}
```

---

### Utility Functions

#### `closeEngine()`
Manually close the LLM engine (frees memory).

```typescript
const result = await Mediapipe.closeEngine();
// Returns: 'Engine closed'
```

#### `getEngineStatus()`
Check if the engine is initialized.

```typescript
const status = await Mediapipe.getEngineStatus();
// Returns: { initialized: true, ready: true }
```

---

## 🏗️ Complete RAG Example

```typescript
import Mediapipe from 'edge-llm';
import { useEffect, useRef, useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, FlatList } from 'react-native';

export default function RAGChatApp() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const booted = useRef(false);

  useEffect(() => {
    if (booted.current) return;
    booted.current = true;

    const initializeRAG = async () => {
      try {
        // Clear old data
        await Mediapipe.clearRagDatabase();
        console.log('✅ Database cleared');

        // Add knowledge documents
        await Mediapipe.addRagDocument(`
          Company: TechCorp
          Founded: 2020
          CEO: Jane Smith
          Products: AI Solutions, Cloud Services
        `);

        await Mediapipe.addRagDocument(`
          Product: CloudAI Pro
          Price: $99/month
          Features: Auto-scaling, 99.9% uptime, 24/7 support
        `);

        // Verify
        const stats = await Mediapipe.getRagStats();
        console.log(`✅ RAG ready with ${stats.documentCount} documents`);
      } catch (error) {
        console.error('❌ RAG init failed:', error);
      }
    };

    initializeRAG();
  }, []);

  const sendMessage = async () => {
    if (!input.trim() || loading) return;

    const userMsg = { id: Date.now(), role: 'user', text: input };
    setMessages(prev => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      const result = await Mediapipe.generateWithRag(input);

      if (result.status === 'success') {
        const aiMsg = {
          id: Date.now() + 1,
          role: 'assistant',
          text: result.response
        };
        setMessages(prev => [...prev, aiMsg]);
      }
    } catch (error) {
      console.error('❌ Generation failed:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={{ flex: 1 }}>
      <FlatList
        data={messages}
        keyExtractor={item => item.id.toString()}
        renderItem={({ item }) => (
          <View style={{
            padding: 12,
            margin: 8,
            backgroundColor: item.role === 'user' ? '#DCF8C6' : '#FFF',
            borderRadius: 8,
            alignSelf: item.role === 'user' ? 'flex-end' : 'flex-start',
            maxWidth: '80%'
          }}>
            <Text>{item.text}</Text>
          </View>
        )}
      />

      <View style={{ flexDirection: 'row', padding: 12, borderTopWidth: 1, borderColor: '#DDD' }}>
        <TextInput
          value={input}
          onChangeText={setInput}
          placeholder="Ask anything..."
          style={{
            flex: 1,
            padding: 12,
            backgroundColor: '#F0F0F0',
            borderRadius: 8,
            marginRight: 8
          }}
        />
        <TouchableOpacity
          onPress={sendMessage}
          disabled={loading}
          style={{
            backgroundColor: loading ? '#CCC' : '#4CAF50',
            paddingHorizontal: 20,
            paddingVertical: 12,
            borderRadius: 8,
            justifyContent: 'center'
          }}
        >
          <Text style={{ color: '#FFF', fontWeight: '600' }}>
            {loading ? 'Thinking...' : 'Send'}
          </Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}
```

---

## 🔧 Configuration

### Customize Token Limit

The default maximum token limit is **4096**. To change it, modify `MediapipeModule.kt`:

```kotlin
val options = LlmInferenceOptions.builder()
    .setModelPath(modelFile.absolutePath)
    .setMaxTokens(8192)  // Increase token limit
    .build()
```

### Using Different Models

Replace model files in `android/app/src/main/assets/` and update the model name in code. it should be mediapipe compatible.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────┐
│           React Native Layer                │
│  (TypeScript/JavaScript)                    │
└──────────────────┬──────────────────────────┘
                   │ Expo Module Bridge
┌──────────────────▼──────────────────────────┐
│        MediapipeModule (Kotlin)             │
│  - LLM Inference Engine                     │
│  - Text Embedder                            │
│  - RAG Manager                              │
└──────────────────┬──────────────────────────┘
                   │
      ┌────────────┼────────────┐
      │            │            │
┌─────▼────┐ ┌────▼─────┐ ┌───▼──────┐
│ RagDB    │ │ RagMath  │ │ MediaPipe│
│ (SQLite) │ │ (Cosine) │ │ Framework│
└──────────┘ └──────────┘ └──────────┘
```

---

## 🤝 Contributing

Found a bug or want to contribute? Here's how:

1. **Report Issues:** Check similarity scores in logs and share full error traces
2. **Test Changes:** Always rebuild with `npx expo prebuild --clean`
3. **Document Changes:** Update this README with any API changes

---

## 📄 License

MIT License - See LICENSE file for details

---

## 🙏 Acknowledgments

- **MediaPipe Team** - For the incredible on-device ML framework
- **Google** - For Gemma models
- **Anthropic** - For Claude AI assistance in development

---

## 📞 Support

- **Issues:** [GitHub Issues](https://github.com/your-repo/edge-llm/issues)
- **Discussions:** [GitHub Discussions](https://github.com/your-repo/edge-llm/discussions)
- **Email:** tirthparmarx@gmail.com

---

## 🗺️ Roadmap

- [ ] iOS Support
- [ ] Streaming responses
- [ ] Memory management improvements
- [ ] Conversation history management
- [ ] Voice input/output
- [ ] Model quantization utilities

---

**Built with ❤️ by Tirth Parmar**

*Last Updated: January 24, 2026*
