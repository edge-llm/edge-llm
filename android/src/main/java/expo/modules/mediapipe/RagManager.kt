package expo.modules.mediapipe

/**
 * RagManager
 *
 * This class CONNECTS:
 * - embeddings
 * - SQLite vector DB
 * - cosine similarity
 *
 * It does NOT know about:
 * - Expo
 * - MediaPipe
 * - LLMs
 *
 * That separation is VERY important.
 */
class RagManager(
    private val db: RagDatabase,
    private val embed: (String) -> FloatArray
) {




    fun normalize(v: FloatArray): FloatArray {
    var norm = 0f
    for (x in v) norm += x * x
    norm = kotlin.math.sqrt(norm)
    if (norm == 0f) return v
    return FloatArray(v.size) { i -> v[i] / norm }
}


    /**
     * Add a document to the RAG database
     *
     * Flow:
     * text → embedding → SQLite
     */
    fun addDocument(text: String) {
        val vector = normalize(embed(text))
        db.insert(text, vector)
    }

    /**
     * Retrieve top-K most relevant documents
     *
     * Flow:
     * query → embedding
     * queryEmbedding ↔ docEmbedding (cosine similarity)
     */
    fun retrieve(
        query: String,
        k: Int = 3
    ): List<String> {

        val queryVector = normalize(embed(query))

        return db.getAll()
    .map { (content, docVector) ->

        val cosine = RagMath.cosine(queryVector, docVector)

        val queryTokens = query
            .lowercase()
            .split(Regex("\\W+"))
            .filter { it.length >= 3 }   // ignore junk like "is", "??"

        val lexicalBoost = queryTokens.count { token ->
            content.lowercase().contains(token)
        } * 0.05f   // small boost per matched token

        val finalScore = cosine + lexicalBoost

        content to finalScore
    }
    .sortedByDescending { it.second }
    .take(k)
    .map { it.first }


    }
}
