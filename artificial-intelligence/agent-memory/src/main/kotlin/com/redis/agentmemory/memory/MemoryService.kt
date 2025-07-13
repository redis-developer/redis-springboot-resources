package com.redis.agentmemory.memory

import com.redis.agentmemory.memory.model.Memory
import com.redis.agentmemory.memory.model.MemoryType
import com.redis.agentmemory.memory.model.StoredMemory
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder
import org.springframework.ai.vectorstore.redis.RedisVectorStore
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Implementation of the MemoryService interface using RedisVectorStore.
 */
@Service
class MemoryService(
    private val memoryVectorStore: RedisVectorStore
) {

    private val log = LoggerFactory.getLogger(MemoryService::class.java)
    private val systemUserId = "system"

    fun storeMemory(
        content: String,
        memoryType: MemoryType,
        userId: String? = null,
        metadata: String = "{}"
    ): StoredMemory {
        log.info("Preparing to store memory: $content")

        // Validate metadata is a valid JSON object
        val validatedMetadata = try {
            // Simple validation - just check if it starts with { and ends with }
            if (!metadata.trim().startsWith("{") || !metadata.trim().endsWith("}")) {
                log.warn("Invalid metadata format, using empty JSON object instead: $metadata")
                "{}"
            } else {
                metadata
            }
        } catch (e: Exception) {
            log.warn("Error validating metadata, using empty JSON object instead: ${e.message}")
            "{}"
        }

        // Check if a similar memory already exists
        if (similarMemoryExists(content, memoryType, userId)) {
            log.info("Similar memory found, skipping storage")
            // Create a memory object to return, but don't store it
            val memory = Memory(
                content = content,
                memoryType = memoryType,
                userId = userId ?: systemUserId,
                metadata = validatedMetadata,
                createdAt = LocalDateTime.now()
            )
            return StoredMemory(memory)
        }

        // Create a memory object
        val memory = Memory(
            content = content,
            memoryType = memoryType,
            userId = userId ?: systemUserId,
            metadata = validatedMetadata,
            createdAt = LocalDateTime.now()
        )

        // Create a document for the vector store
        val document = Document(
            content,
            mapOf(
                "memoryType" to memoryType.name,
                "metadata" to validatedMetadata,
                "userId" to (userId ?: systemUserId),
                "createdAt" to memory.createdAt.toString()
            )
        )

        // Store the document in the vector store
        try {
            memoryVectorStore.add(listOf(document))
            log.info("Stored ${memoryType.name} memory: $content")
        } catch (e: Exception) {
            log.error("Error storing memory: ${e.message}", e)
            throw e
        }

        return StoredMemory(memory)
    }

    fun retrieveMemories(
        query: String,
        memoryType: MemoryType? = null,
        userId: String? = null,
        limit: Int = 5,
        distanceThreshold: Float = 0.9f
    ): List<StoredMemory> {
        log.debug("Retrieving memories for query: $query")

        // Build filter expression
        val b = FilterExpressionBuilder()
        val filterList = mutableListOf<FilterExpressionBuilder.Op>()

        // Add user filter
        val effectiveUserId = userId ?: systemUserId
        filterList.add(b.or(b.eq("userId", effectiveUserId), b.eq("userId", systemUserId)))

        // Add memory type filter if specified
        if (memoryType != null) {
            filterList.add(b.eq("memoryType", memoryType.name))
        }

        // Combine filters
        val filterExpression = when (filterList.size) {
            0 -> null
            1 -> filterList[0]
            else -> filterList.reduce { acc, expr -> b.and(acc, expr) }
        }?.build()

        // Execute search
        val start = System.currentTimeMillis()
        val searchResults = memoryVectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(limit)
                .filterExpression(filterExpression)
                .build()
        ) ?: emptyList()

        // Transform results to StoredMemory objects
        val memories = searchResults.mapNotNull { result ->
            if (distanceThreshold < (result.score ?: 1.0)) {
                val metadata = result.metadata
                val memoryObj = Memory(
                    id = result.id,
                    content = result.text ?: "",
                    memoryType = MemoryType.valueOf(metadata["memoryType"] as String? ?: MemoryType.SEMANTIC.name),
                    metadata = metadata["metadata"] as String? ?: "{}",
                    userId = metadata["userId"] as String? ?: systemUserId,
                    createdAt = try {
                        LocalDateTime.parse(metadata["createdAt"] as String?)
                    } catch (_: Exception) {
                        LocalDateTime.now()
                    }
                )
                StoredMemory(memoryObj, null)
            } else {
                null
            }
        }

        val elapsed = System.currentTimeMillis() - start
        log.info("Retrieved ${memories.size} memories in $elapsed ms")

        return memories
    }

    fun similarMemoryExists(
        content: String,
        memoryType: MemoryType,
        userId: String? = null,
        distanceThreshold: Float = 0.9f
    ): Boolean {
        // Build filter expression
        val b = FilterExpressionBuilder()
        val filterList = mutableListOf<FilterExpressionBuilder.Op>()

        // Add user filter
        val effectiveUserId = userId ?: systemUserId
        filterList.add(b.eq("userId", effectiveUserId))

        // Add memory type filter
        filterList.add(b.eq("memoryType", memoryType.name))

        // Combine filters
        val filterExpression = when (filterList.size) {
            0 -> null
            1 -> filterList[0]
            else -> filterList.reduce { acc, expr -> b.and(acc, expr) }
        }?.build()

        // Search for similar memories
        val searchResults = memoryVectorStore.similaritySearch(
            SearchRequest.builder()
                .query(content)
                .topK(1)
                .filterExpression(filterExpression)
                .build()
        ) ?: emptyList()

        // Check if any results were found with a score below the threshold
        return searchResults.isNotEmpty() && distanceThreshold < (searchResults[0].score ?: 1.0)
    }
}
