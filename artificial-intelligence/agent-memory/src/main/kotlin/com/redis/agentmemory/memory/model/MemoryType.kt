package com.redis.agentmemory.memory.model

/**
 * Defines the type of long-term memory for categorization and retrieval.
 *
 * EPISODIC: Personal experiences and user-specific preferences
 *           (e.g., "User prefers Delta airlines", "User visited Paris last year")
 *
 * SEMANTIC: General domain knowledge and facts
 *           (e.g., "Singapore requires passport", "Tokyo has excellent public transit")
 */
enum class MemoryType {
    EPISODIC,
    SEMANTIC
}