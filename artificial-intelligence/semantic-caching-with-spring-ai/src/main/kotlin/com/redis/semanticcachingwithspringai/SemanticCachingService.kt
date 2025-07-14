package com.redis.semanticcachingwithspringai

import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.redis.RedisVectorStore
import org.springframework.stereotype.Service

@Service
class SemanticCachingService(
    private val semanticCachingVectorStore: RedisVectorStore
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun storeInCache(prompt: String, answer: String) {
        semanticCachingVectorStore.add(listOf(Document(
            prompt,
            mapOf(
                "answer" to answer
            )
        )))
    }

    fun getFromCache(prompt: String, similarityThreshold: Double): String? {
        val results = semanticCachingVectorStore.similaritySearch(
            SearchRequest.builder()
                .query(prompt)
                .topK(1)
                .build()
        )

        if (results?.isNotEmpty() == true) {
            if (similarityThreshold < (results[0].score ?: 0.0)) {
                logger.info("Returning cached answer. Similarity score: ${results[0].score}")
                return results[0].metadata["answer"] as String
            }
        }

        return null
    }

}