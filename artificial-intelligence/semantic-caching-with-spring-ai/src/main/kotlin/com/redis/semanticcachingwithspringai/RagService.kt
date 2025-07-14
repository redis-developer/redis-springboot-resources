package com.redis.semanticcachingwithspringai

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.SystemPromptTemplate
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class RagService(
    private val chatModel: ChatModel,
    private val beerVectorStore: VectorStore,
    private val semanticCachingService: SemanticCachingService
) {

    private val logger = LoggerFactory.getLogger(RagService::class.java)

    private val systemBeerPrompt = """
        You're assisting with questions about products in a beer catalog.
        Use the information from the DOCUMENTS section to provide accurate answers.
        The answer involves referring to the ABV or IBU of the beer, include the beer name in the response.
        If unsure, simply state that you don't know.

        DOCUMENTS:
        {documents}
    """.trimIndent()

    @Value("\${topk:10}")
    private var topK: Int = 10

    data class RagMetrics(
        val embeddingTimeMs: Long,
        val searchTimeMs: Long,
        val llmTimeMs: Long
    )

    data class RagResult(
        val generation: Generation,
        val metrics: RagMetrics
    )

    fun retrieve(message: String): RagResult {
        val startCachingTime = System.currentTimeMillis()
        val cachedAnswer = semanticCachingService.getFromCache(message, 0.8)
        val cachingTimeMs = System.currentTimeMillis() - startCachingTime

        if (cachedAnswer != null) {
            return RagResult(
                generation = Generation(AssistantMessage(cachedAnswer)),
                metrics = RagMetrics(
                    embeddingTimeMs = 0,
                    searchTimeMs = 0,
                    llmTimeMs = 0
                )
            )
        }


        // Measure time for augmentation (embedding + search)
        var embeddingTimeMs: Long = 0
        var searchTimeMs: Long = 0

        // Get documents with timing metrics
        val (embTime, searchTime, docs) = measureAugmentTime(message)
        embeddingTimeMs = embTime
        searchTimeMs = searchTime

        val systemMessage = getSystemMessage(docs)
        val userMessage = UserMessage(message)
        val prompt = Prompt(listOf(systemMessage, userMessage))

        // Measure time for LLM processing
        val startLlmTime = System.currentTimeMillis()
        val response: ChatResponse = chatModel.call(prompt)
        val llmTimeMs = System.currentTimeMillis() - startLlmTime

        semanticCachingService.storeInCache(message, response.result.output.text.toString())

        return RagResult(
            generation = response.result,
            metrics = RagMetrics(
                embeddingTimeMs = embeddingTimeMs,
                searchTimeMs = searchTimeMs,
                llmTimeMs = llmTimeMs
            )
        )
    }

    private fun measureAugmentTime(message: String): Triple<Long, Long, List<Document>> {
        val request = SearchRequest
            .builder()
            .query(message)
            .topK(topK)
            .build()

        // Measure total search time (includes embedding)
        val startSearchTime = System.currentTimeMillis()
        val documents = beerVectorStore.similaritySearch(request) ?: emptyList()
        val totalSearchTime = System.currentTimeMillis() - startSearchTime

        // Estimate embedding time as a portion of search time
        // In a real implementation, you might want to measure this directly if possible
        val embeddingTimeMs = (totalSearchTime * 0.7).toLong() // Assuming embedding is ~70% of search time
        val searchTimeMs = totalSearchTime - embeddingTimeMs

        return Triple(embeddingTimeMs, searchTimeMs, documents)
    }

    private fun getSystemMessage(docsForAugmentation: List<Document>): Message {
        val documents = docsForAugmentation.joinToString("\n") { it.text.toString() }

        logger.info("Retrieved documents: {}", documents)

        val systemPromptTemplate = SystemPromptTemplate(systemBeerPrompt)
        return systemPromptTemplate.createMessage(mapOf("documents" to documents))
    }
}
