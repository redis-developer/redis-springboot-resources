package com.redis.ragwithspringai

import org.slf4j.LoggerFactory
import org.springframework.ai.vectorstore.redis.RedisVectorStore
import org.springframework.stereotype.Service

@Service
class EmbeddingStatusService(
    private val vectorStore: RedisVectorStore
) {
    private val logger = LoggerFactory.getLogger(EmbeddingStatusService::class.java)

    fun getTotalDocNum(): Long {
        try {
            val indexInfo = vectorStore.jedis.ftInfo("beerIdx")
            val numTerms = indexInfo["num_docs"] as Long
            logger.info("Number of terms in beerIdx: $numTerms")
            return numTerms
        } catch (e: Exception) {
            logger.error("Error checking embedding status", e)
            return 0
        }
    }
}