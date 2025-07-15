package com.redis.vectorsearchspringai

import org.slf4j.LoggerFactory
import org.springframework.ai.vectorstore.redis.RedisVectorStore
import org.springframework.stereotype.Service

@Service
class EmbeddingStatusService(
    private val movieVectorStore: RedisVectorStore
) {
    private val logger = LoggerFactory.getLogger(EmbeddingStatusService::class.java)
    
    /**
     * Checks if the embeddings are ready by verifying that there are at least 10,000 documents in the index.
     * @return true if the embeddings are ready, false otherwise
     */
    fun areEmbeddingsReady(): Boolean {
        try {
            val indexInfo = movieVectorStore.jedis.ftInfo("movieIdx")
            val numDocs = indexInfo["num_docs"] as Long
            logger.info("Number of documents in movieIdx: $numDocs")
            return numDocs >= 10000
        } catch (e: Exception) {
            logger.error("Error checking embedding status", e)
            return false
        }
    }
    
    /**
     * Gets the total number of documents in the index.
     * @return the number of documents
     */
    fun getTotalDocNum(): Long {
        try {
            val indexInfo = movieVectorStore.jedis.ftInfo("movieIdx")
            return indexInfo["num_docs"] as Long
        } catch (e: Exception) {
            logger.error("Error getting total document count", e)
            return 0
        }
    }
}