package expo.modules.mediapipe

import kotlin.math.sqrt

/**
 * RagMath
 *
 * This file contains ONLY math utilities used by RAG.
 *
 * Right now:
 * - Cosine similarity
 *
 * Why cosine similarity?
 * - It compares vector DIRECTION, not magnitude
 * - Standard for embeddings
 * - Used by OpenAI, Google, Meta, etc.
 */
object RagMath {

    /**
     * Computes cosine similarity between two vectors.
     *
     * Formula:
     *
     *        A · B
     * -----------------
     * ||A|| * ||B||
     *
     * Result range:
     * -1.0 → opposite meaning
     *  0.0 → unrelated
     *  1.0 → same meaning
     *
     * @param a Query embedding
     * @param b Document embedding
     */
    fun cosine(a: FloatArray, b: FloatArray): Float {

        // Safety check (should NEVER fail in correct usage)
        if (a.size != b.size) {
            throw IllegalArgumentException("Embedding size mismatch")
        }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        // Single loop = fast
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        // Prevent division by zero
        if (normA == 0f || normB == 0f) return 0f

        return dotProduct / (sqrt(normA) * sqrt(normB))
    }
}
