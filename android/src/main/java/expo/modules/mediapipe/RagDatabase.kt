package expo.modules.mediapipe

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.nio.ByteBuffer

/**
 * RagDatabase
 *
 * This class is a VERY SIMPLE vector database.
 *
 * What it does:
 * - Stores documents
 * - Stores their embeddings (vectors)
 * - Uses SQLite (offline, fast, reliable)
 *
 * This is the foundation of RAG.
 */
class RagDatabase(context: Context) :
    SQLiteOpenHelper(
        context,
        "rag.db",   // Database name (stored in app internal storage)
        null,
        1           // Database version
    ) {

    /**
     * Called automatically when the database is created for the first time
     */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE documents (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                content TEXT NOT NULL,
                embedding BLOB NOT NULL
            )
            """.trimIndent()
        )
    }

    /**
     * Called when database version changes
     * (not needed now, but required by SQLiteOpenHelper)
     */
    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // No migrations yet
    }

    /**
     * Insert a document + its embedding into the database
     *
     * @param content   Original text
     * @param embedding Vector representation of the text
     */
    fun insert(content: String, embedding: FloatArray) {
        val values = ContentValues()

        // Store raw text
        values.put("content", content)

        // Store vector as bytes (SQLite does NOT support FloatArray)
        values.put("embedding", floatArrayToBytes(embedding))

        writableDatabase.insert(
            "documents",
            null,
            values
        )
    }

    /**
     * Read all stored documents and embeddings
     *
     * @return List of (content, embedding)
     */
    fun getAll(): List<Pair<String, FloatArray>> {
        val results = mutableListOf<Pair<String, FloatArray>>()

        val cursor = readableDatabase.rawQuery(
            "SELECT content, embedding FROM documents",
            null
        )

        while (cursor.moveToNext()) {
            val content = cursor.getString(0)
            val embeddingBlob = cursor.getBlob(1)

            results.add(
                content to bytesToFloatArray(embeddingBlob)
            )
        }

        cursor.close()
        return results
    }



    // Add this to RagDatabase.kt
fun clearAll() {
    writableDatabase.execSQL("DELETE FROM documents")
}

fun getCount(): Int {
    val cursor = readableDatabase.rawQuery(
        "SELECT COUNT(*) FROM documents",
        null
    )
    cursor.moveToFirst()
    val count = cursor.getInt(0)
    cursor.close()
    return count
}

    /**
     * Converts FloatArray → ByteArray
     *
     * Why?
     * - SQLite cannot store FloatArray
     * - Each Float = 4 bytes
     */
    private fun floatArrayToBytes(arr: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(arr.size * 4)
        for (value in arr) {
            buffer.putFloat(value)
        }
        return buffer.array()
    }

    /**
     * Converts ByteArray → FloatArray
     *
     * This restores the original embedding vector
     */
    private fun bytesToFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes)
        val floatArray = FloatArray(bytes.size / 4)

        for (i in floatArray.indices) {
            floatArray[i] = buffer.float
        }

        return floatArray
    }
}
