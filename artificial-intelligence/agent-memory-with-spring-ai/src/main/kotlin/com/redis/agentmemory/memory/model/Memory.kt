package com.redis.agentmemory.memory.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a single memory entry that can be stored in the agent's long-term memory.
 *
 * @property id Unique identifier for the memory
 * @property content The actual content of the memory
 * @property memoryType The type of memory (EPISODIC or SEMANTIC)
 * @property metadata Additional context or information about the memory
 * @property userId The ID of the user associated with this memory (if applicable)
 * @property createdAt The timestamp when this memory was created
 */
data class Memory(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val memoryType: MemoryType,
    val metadata: String = "{}",
    val userId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Represents a memory that has been stored in Redis with vector embeddings.
 *
 * @property embedding The vector embedding of the memory content
 */
data class StoredMemory(
    val memory: Memory,
    val embedding: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StoredMemory

        if (memory != other.memory) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = memory.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        return result
    }
}