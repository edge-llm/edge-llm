package expo.modules.mediapipe

import android.content.Context
import android.util.Log
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions


import java.io.File

private const val TAG = "MediapipeModule"

class MediapipeModule : Module() {
    
    private var engine: LlmInference? = null
    private var embedder: TextEmbedder? = null
    private var ragDb: RagDatabase? = null
    private var ragManager: RagManager? = null
    private var embeddingModelPath: String? = null
    private var llmModelPath: String? = null
    
    private fun ctx(): Context {
        return appContext.reactContext
            ?: throw Exception("React context not available")
    }

    private fun initRag() {
    if (ragManager != null) return

    Log.d(TAG, "Initializing RAG system")

    val context = ctx()

    ragDb = RagDatabase(context)

    ragManager = RagManager(
        db = ragDb!!,
        embed = { text -> embedText(text) }
    )

    Log.d(TAG, "RAG system initialized")
}




    private fun copyAssetToInternal(assetPath: String): File {
    val context = ctx()
    val outDir = File(context.filesDir, "models")
    if (!outDir.exists()) outDir.mkdirs()

    val fileName = assetPath.substringAfterLast("/")
    val outFile = File(outDir, fileName)

    if (outFile.exists() && outFile.length() > 0) {
        return outFile
    }

    context.assets.open(assetPath).use { input ->
        outFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    return outFile
}




    private fun initEmbedder() {
    if (embedder != null) return

    val modelFile = if (embeddingModelPath != null) {
    copyAssetToInternal(embeddingModelPath!!)
} else {
    copyAssetToModels("universal_sentence_encoder.tflite")  // fallback
}

    val baseOptions = BaseOptions.builder()
        .setModelAssetPath(modelFile.absolutePath)
        .build()

    val options = TextEmbedderOptions.builder()
        .setBaseOptions(baseOptions)
        .build()

    embedder = TextEmbedder.createFromOptions(ctx(), options)
}


private fun embedText(text: String): FloatArray {
    initEmbedder()

    if (embedder == null) {
        throw Exception("TextEmbedder not initialized")
    }

    val result = embedder!!
        .embed(text)
        .embeddingResult()
        .embeddings()
        .first()

    return result.floatEmbedding()
}

    
    private fun copyAssetToModels(assetName: String): File {
        val context = ctx()
        val modelDir = File(context.filesDir, "models")
        if (!modelDir.exists()) modelDir.mkdirs()
        
        val outFile = File(modelDir, assetName)
        if (outFile.exists() && outFile.length() > 0L) {
            Log.d(TAG, "Model file already exists: ${outFile.absolutePath}")
            return outFile
        }
        
        try {
            Log.d(TAG, "Copying asset: $assetName")
            context.assets.open(assetName).use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            if (outFile.length() == 0L) {
                throw Exception("Model file copy resulted in 0 bytes")
            }
            
            Log.d(TAG, "Asset copied successfully. Size: ${outFile.length() / 1_000_000}MB")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy asset: ${e.message}", e)
            throw Exception("Failed to copy model: ${e.message}")
        }
        
        return outFile
    }
    
    private fun initializeEngine(modelName: String = "") {
        if (engine != null) {
            Log.d(TAG, "Engine already initialized, reusing instance")
            return
        }

        val actualModelName = if (llmModelPath != null) {
    llmModelPath!!
} else {
    modelName.ifEmpty { "gemma3n_e2b_int4.task" }  // fallback
}
        
        try {
            val context = ctx()
            val modelFile = copyAssetToInternal(actualModelName)
            
            val fileSize = modelFile.length()
            Log.d(TAG, "Model file size: ${fileSize / 1_000_000}MB")
            
            if (fileSize < 50_000_000) {
                throw Exception("Model file too small (${fileSize / 1_000_000}MB) - might be corrupted")
            }
            
            val options = LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(4096)
                .build()
            
            Log.d(TAG, "Creating LLM Inference engine with model: $modelName")
            engine = LlmInference.createFromOptions(context, options)
            Log.d(TAG, "Engine created successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Engine initialization failed: ${e.message}", e)
            throw Exception("Failed to initialize engine: ${e.message}")
        }
    }
    
    override fun definition() = ModuleDefinition {
        Name("Mediapipe")


        AsyncFunction("init") { options: Map<String, Any> ->
    embeddingModelPath = options["embeddingModel"] as? String
    llmModelPath = options["llmModel"] as? String
    
    Log.d(TAG, "Init: embedding=$embeddingModelPath, llm=$llmModelPath")
    
    mapOf("status" to "ok")
}
        
        AsyncFunction("mediapipeSmokeTest") { ->
            try {
                Log.d(TAG, "Running smoke test with model: gemma3n_e2b_int4.task")
                initializeEngine("gemma3n_e2b_int4.task")
                
                if (engine == null) {
                    throw Exception("Engine is null after initialization")
                }
                
                Log.d(TAG, "Smoke test passed")
                mapOf(
                    "status" to "ok",
                    "message" to "MediaPipe engine initialized successfully"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Smoke test failed: ${e.message}", e)
                mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Unknown error"),
                    "cause" to (e.cause?.message ?: "")
                )
            }
        }
        
        AsyncFunction("testModel") { ->
            try {
                Log.d(TAG, "Testing model")
                
                if (engine == null) {
                    Log.d(TAG, "Engine not initialized, initializing now...")
                    initializeEngine("gemma3n_e2b_int4.task")
                }
                
                if (engine == null) {
                    throw Exception("Failed to initialize engine")
                }
                
                val testPrompt = "Hello"
                Log.d(TAG, "Sending test prompt: $testPrompt")
                
                val startTime = System.currentTimeMillis()
                val response = engine!!.generateResponse(testPrompt)
                val duration = System.currentTimeMillis() - startTime
                
                Log.d(TAG, "Test completed in ${duration}ms")
                
                mapOf(
                    "status" to "success",
                    "testPrompt" to testPrompt,
                    "testResponse" to response,
                    "responseLength" to response.length,
                    "duration" to duration
                )
            } catch (e: Exception) {
                Log.e(TAG, "Model test failed: ${e.message}", e)
                mapOf(
                    "status" to "error",
                    "error" to (e.message ?: "Unknown error"),
                    "cause" to (e.cause?.message ?: "")
                )
            }
        }
        
        AsyncFunction("generateText") { prompt: String ->
            try {
                Log.d(TAG, "generateText called with prompt length: ${prompt.length}")
                
                if (engine == null) {
                    Log.d(TAG, "Engine not initialized, initializing now...")
                    initializeEngine("gemma3n_e2b_int4.task")
                }
                
                if (engine == null) {
                    throw Exception("Failed to initialize engine")
                }
                
                Log.d(TAG, "Generating response for: ${prompt.substring(0, Math.min(50, prompt.length))}")
                val startTime = System.currentTimeMillis()
                
                val response = engine!!.generateResponse(prompt)
                
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "Response generated in ${duration}ms. Length: ${response.length}")
                
                mapOf(
                    "status" to "success",
                    "response" to response,
                    "duration" to duration,
                    "promptLength" to prompt.length,
                    "responseLength" to response.length
                )
            } catch (e: Exception) {
                Log.e(TAG, "Text generation failed: ${e.message}", e)
                mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Unknown error"),
                    "cause" to (e.cause?.message ?: "")
                )
            }
        }

        AsyncFunction("embedTest") { text: String ->
    val vector = embedText(text)
    mapOf(
        "length" to vector.size,
        "sample" to vector.take(5)
    )
}

AsyncFunction("clearDB") { text: String ->
    initRag()
    ragDb?.clearAll()

    mapOf(
        "status" to "success",
    )
}
AsyncFunction("getRagStats") {
        initRag()
        val count = ragDb?.getCount() ?: 0
        
        mapOf(
            "status" to "success",
            "documentCount" to count
        )
}

AsyncFunction("addRagDocument") { text: String ->
    initRag()

    ragManager!!.addDocument(text)

    mapOf(
        "status" to "stored",
        "length" to text.length
    )
}

    
AsyncFunction("generateWithRag") { prompt: String ->
    try {
        initRag()
        initializeEngine("gemma3n_e2b_int4.task")

        Log.d(TAG, "Running RAG for prompt: ${prompt.take(50)}")

        // Check document count
        val docCount = ragDb?.getCount() ?: 0
        Log.d(TAG, "Database has $docCount documents")
        
        if (docCount == 0) {
            return@AsyncFunction mapOf(
                "status" to "error",
                "message" to "No documents in RAG database"
            )
        }

        // ✅ GET ALL DOCS AND CALCULATE SIMILARITIES
        val queryVector = embedText(prompt)
        val allDocs = ragDb!!.getAll()
        
        Log.d(TAG, "Query: '$prompt'")
        Log.d(TAG, "Query embedding sample: ${queryVector.take(5).joinToString()}")
        
        // Calculate and LOG all similarities
        val rankedDocs = allDocs.mapIndexed { index, (content, docVector) ->
            val similarity = RagMath.cosine(queryVector, docVector)
            Log.d(TAG, "Doc $index similarity: $similarity")
            Log.d(TAG, "Doc $index preview: ${content.take(50)}")
            Log.d(TAG, "Doc $index embedding sample: ${docVector.take(5).joinToString()}")
            
            content to similarity
        }
        .sortedByDescending { it.second }
        
        if (rankedDocs.isEmpty()) {
            return@AsyncFunction mapOf(
                "status" to "error",
                "message" to "No documents found"
            )
        }
        
        val (bestDoc, bestScore) = rankedDocs.first()
        Log.d(TAG, "✅ BEST MATCH - Score: $bestScore")
        Log.d(TAG, "✅ BEST MATCH - Content: ${bestDoc.take(100)}")
        
        // Build minimal prompt
        val shortContext = bestDoc.take(300)
        val finalPrompt = "Context: $shortContext\n\nQuestion: $prompt\n\nAnswer:"
        
        Log.d(TAG, "Final prompt length: ${finalPrompt.length} chars")

        // Generate
        val response = engine!!.generateResponse(finalPrompt)

        mapOf(
            "status" to "success",
            "response" to response,
            "contextUsed" to shortContext,
            "promptLength" to finalPrompt.length,
            "bestScore" to bestScore
        )
        
    } catch (e: Exception) {
        Log.e(TAG, "RAG failed: ${e.message}", e)
        
        try {
            engine?.close()
            engine = null
        } catch (ex: Exception) {
            Log.e(TAG, "Engine close failed: ${ex.message}")
        }
        
        mapOf(
            "status" to "error",
            "message" to (e.message ?: "Unknown error")
        )
    }
}
        
        Function("closeEngine") {
            try {
                if (engine != null) {
                    engine!!.close()
                    engine = null
                    Log.d(TAG, "Engine closed successfully")
                    "Engine closed"
                } else {
                    Log.d(TAG, "Engine was already null")
                    "Engine was not initialized"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error closing engine: ${e.message}", e)
                "Error: ${e.message}"
            }
        }
        
        Function("getEngineStatus") {
            mapOf(
                "initialized" to (engine != null),
                "ready" to (engine != null)
            )
        }
    }
}