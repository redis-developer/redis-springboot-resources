package com.redis.agentmemory.chat

import com.redis.agentmemory.memory.MemoryService
import com.redis.agentmemory.memory.model.Memory
import com.redis.agentmemory.memory.model.MemoryType
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service
import redis.clients.jedis.JedisPooled
import java.util.concurrent.ConcurrentHashMap

@Service
class ChatService(
    private val chatModel: ChatModel,
    private val memoryService: MemoryService,
    private val travelAgentSystemPrompt: Message,
    private val jedisPooled: JedisPooled
) {
    private val log = LoggerFactory.getLogger(ChatService::class.java)
    private val systemUserId = "system"
    private val conversationKeyPrefix = "conversation:"

    // In-memory cache for conversation history
    // Also stored in Redis for persistence
    private val conversationHistory = ConcurrentHashMap<String, MutableList<Message>>()

    fun sendMessage(
        message: String,
        userId: String,
    ): ChatResponse {
        // Use userId as the key for conversation history
        log.info("Processing message from user $userId: $message")

        // Get or create conversation history (try to load from Redis first)
        val history = conversationHistory.computeIfAbsent(userId) {
            // Try to load from Redis first
            val redisHistory = loadConversationHistoryFromRedis(userId)
            if (redisHistory.isNotEmpty()) {
                redisHistory.toMutableList()
            } else {
                mutableListOf(travelAgentSystemPrompt)
            }
        }

        // Retrieve relevant memories
        val relevantMemories = retrieveRelevantMemories(message, userId)

        // Add memory context if available
        if (relevantMemories.isNotEmpty()) {
            val memoryContext = formatMemoriesAsContext(relevantMemories)

            // Add memory context as a system message
            history.add(SystemMessage(memoryContext))
            log.info("Added memory context to conversation: $memoryContext")
        }

        // Add user's message to history
        val userMessage = UserMessage(message)
        history.add(userMessage)

        // Create prompt with conversation history
        val prompt = Prompt(history)

        // Generate response
        val response = chatModel.call(prompt)

        // Add assistant response to history
        history.add(AssistantMessage(response.result.output.text ?: ""))

        // Save conversation history to Redis
        saveConversationHistoryToRedis(userId, history)

        // Extract and store memories from the conversation
        extractAndStoreMemories(message, response.result.output.text ?: "", userId)

        // Summarize conversation if it's getting too long
        if (history.size > 10) {
            summarizeConversation(history, userId)
            // Save the summarized history to Redis
            saveConversationHistoryToRedis(userId, history)
        }

        return response
    }

    private fun retrieveRelevantMemories(
        query: String,
        userId: String
    ): List<Memory> {
        return memoryService.retrieveMemories(
            query = query,
            userId = userId,
            distanceThreshold = 0.3f
        ).map { it.memory }
    }

    private fun formatMemoriesAsContext(memories: List<Memory>): String {
        val formattedMemories = memories.joinToString("\n") {
            "- [${it.memoryType}] ${it.content}"
        }

        return """
            I have access to the following relevant memories about this user or topic:

            $formattedMemories

            Use this information to personalize your response, but don't explicitly mention 
            that you're using stored memories unless directly asked about your memory capabilities.
        """.trimIndent()
    }

    private fun extractAndStoreMemories(
        userMessage: String,
        assistantResponse: String,
        userId: String
    ) {
        log.info("Extracting memories from conversation")

        val extractionPrompt = """
            Analyze the following conversation and extract potential memories.

            USER MESSAGE:
            $userMessage

            ASSISTANT RESPONSE:
            $assistantResponse

            Extract two types of memories:

            1. EPISODIC MEMORIES: Personal experiences and user-specific preferences
               Examples: "User prefers Delta airlines", "User visited Paris last year"

            2. SEMANTIC MEMORIES: General domain knowledge and facts
               Examples: "Singapore requires passport", "Tokyo has excellent public transit"

            Format your response as a JSON array with objects containing:
            - "type": Either "EPISODIC" or "SEMANTIC"
            - "content": The memory content

            Only extract clear, factual information. Do not make assumptions or infer information that isn't explicitly stated.
            If no memories can be extracted, return an empty array.

            Response format example:
            [
              {"type": "EPISODIC", "content": "User prefers window seats on flights"},
              {"type": "SEMANTIC", "content": "Paris is known for the Eiffel Tower"}
            ]
        """.trimIndent()

        try {
            // Call the LLM to extract memories
            val extractionResponse = chatModel.call(
                Prompt(listOf(SystemMessage(extractionPrompt)))
            )

            val responseText = extractionResponse.result.output.text ?: ""
            log.debug("LLM memory extraction response: $responseText")

            // Simple JSON parsing - in production, use a proper JSON parser
            val jsonText = responseText.trim().let {
                when {
                    it.startsWith("```json") && it.endsWith("```") ->
                        it.removePrefix("```json").removeSuffix("```").trim()
                    it.startsWith("```") && it.endsWith("```") ->
                        it.removePrefix("```").removeSuffix("```").trim()
                    it.startsWith("[") && it.endsWith("]") -> it
                    else -> "[]" // Default to empty array if format is unexpected
                }
            }

            // Very simple JSON array parsing - in production, use a proper JSON library
            if (jsonText.startsWith("[") && jsonText.endsWith("]")) {
                val items = jsonText.removeSurrounding("[", "]")
                    .split("},")
                    .filter { it.isNotBlank() }
                    .map { it.trim() + if (!it.endsWith("}")) "}" else "" }

                for (item in items) {
                    val typeMatch = Regex("\"type\"\\s*:\\s*\"(EPISODIC|SEMANTIC)\"").find(item)
                    // Updated regex to handle escaped quotes in content
                    val contentMatch = Regex("\"content\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"").find(item)

                    if (typeMatch != null && contentMatch != null) {
                        val type = typeMatch.groupValues[1]
                        // Unescape the content to handle special characters properly
                        val content = unescapeJson(contentMatch.groupValues[1])

                        if (content.isNotBlank()) {
                            try {
                                val memoryType = MemoryType.valueOf(type)
                                val memoryUserId = if (memoryType == MemoryType.EPISODIC) userId else systemUserId

                                memoryService.storeMemory(
                                    content = content,
                                    memoryType = memoryType,
                                    userId = memoryUserId,
                                    metadata = "{}"  // Explicitly provide empty JSON object as metadata
                                )
                                log.info("Stored ${memoryType.name.lowercase()} memory: $content")
                            } catch (e: Exception) {
                                log.error("Failed to store memory: ${e.message}", e)
                            }
                        }
                    }
                }
            } else {
                log.warn("LLM response was not in expected JSON format: $responseText")
            }
        } catch (e: Exception) {
            log.error("Error extracting memories: ${e.message}", e)
        }
    }

    /**
     * Summarizes the conversation history to prevent it from getting too long.
     */
    private fun summarizeConversation(
        history: MutableList<Message>,
        userId: String
    ) {
        log.info("Summarizing conversation for user $userId")

        // Keep the system prompt and the last 4 messages
        val systemPrompt = history.first()
        val recentMessages = history.takeLast(4)

        // Create a summary prompt
        val summaryPrompt = """
            Summarize the key points of this conversation, including:
            1. User preferences and important details
            2. Topics discussed
            3. Any decisions or conclusions reached

            Keep the summary concise but informative.
        """.trimIndent()

        val summaryRequest = Prompt(
            listOf(
                SystemMessage(summaryPrompt),
                SystemMessage(history.joinToString("\n") {
                    when (it) {
                        is UserMessage -> "User: ${it.text}"
                        is AssistantMessage -> "Assistant: ${it.text}"
                        else -> ""
                    }
                })
            )
        )

        try {
            // Generate summary
            val summaryResponse = chatModel.call(summaryRequest)
            val summary = summaryResponse.result.output.text

            // Replace history with summary and recent messages
            history.clear()
            history.add(systemPrompt)
            history.add(SystemMessage("Conversation summary: $summary"))
            history.addAll(recentMessages)

            // Note: The updated history will be saved to Redis by the calling method
            log.info("Conversation summarized successfully")
        } catch (e: Exception) {
            log.error("Failed to summarize conversation: ${e.message}")
        }
    }

    /**
     * Gets the conversation history for a user.
     */
    fun getConversationHistory(userId: String): List<Message> {
        // Try to get from in-memory cache first
        val cachedHistory = conversationHistory[userId]
        if (cachedHistory != null) {
            return cachedHistory
        }

        // If not in cache, try to load from Redis
        return loadConversationHistoryFromRedis(userId)
    }

    /**
     * Clears the conversation history and short-term memory for a user.
     */
    fun clearConversationHistory(userId: String) {
        // Remove from in-memory cache
        conversationHistory.remove(userId)

        // Remove from Redis
        val redisKey = "$conversationKeyPrefix$userId"
        jedisPooled.del(redisKey)

        log.info("Cleared conversation history for user $userId from Redis")
    }

    /**
     * Saves the conversation history to Redis with a TTL of one hour.
     */
    private fun saveConversationHistoryToRedis(userId: String, history: List<Message>) {
        val redisKey = "$conversationKeyPrefix$userId"

        try {
            // Delete existing key if it exists
            jedisPooled.del(redisKey)

            // Serialize each message and add to Redis list
            for (message in history) {
                val serializedMessage = serializeMessage(message)
                jedisPooled.rpush(redisKey, serializedMessage)
            }

            // Set TTL of one hour (3600 seconds)
            jedisPooled.expire(redisKey, 3600)

            log.debug("Saved conversation history for user $userId to Redis with TTL of 1 hour")
        } catch (e: Exception) {
            log.error("Error saving conversation history to Redis: ${e.message}", e)
        }
    }

    /**
     * Loads the conversation history from Redis.
     */
    private fun loadConversationHistoryFromRedis(userId: String): List<Message> {
        val redisKey = "$conversationKeyPrefix$userId"

        try {
            // Check if key exists
            if (!jedisPooled.exists(redisKey)) {
                return emptyList()
            }

            // Get all messages from Redis list
            val serializedMessages = jedisPooled.lrange(redisKey, 0, -1)

            // Deserialize messages
            val history = serializedMessages.mapNotNull { deserializeMessage(it) }.toMutableList()

            // Cache the loaded history
            if (history.isNotEmpty()) {
                conversationHistory[userId] = history
            }

            log.debug("Loaded conversation history for user $userId from Redis: ${history.size} messages")
            return history
        } catch (e: Exception) {
            log.error("Error loading conversation history from Redis: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Serializes a Message object to a JSON string.
     */
    private fun serializeMessage(message: Message): String {
        return when (message) {
            is UserMessage -> """{"type":"user","content":"${escapeJson(message.text)}"}"""
            is AssistantMessage -> """{"type":"assistant","content":"${escapeJson(message.text ?: "")}"}"""
            is SystemMessage -> """{"type":"system","content":"${escapeJson(message.text)}"}"""
            else -> """{"type":"unknown","content":"${escapeJson(message.text ?: "")}"}"""
        }
    }

    /**
     * Deserializes a JSON string to a Message object.
     */
    private fun deserializeMessage(json: String): Message? {
        try {
            val typeMatch = Regex("\"type\"\\s*:\\s*\"(user|assistant|system|unknown)\"").find(json)
            val contentMatch = Regex("\"content\"\\s*:\\s*\"(.*?)\"(?=,|\\s*})").find(json)

            if (typeMatch != null && contentMatch != null) {
                val type = typeMatch.groupValues[1]
                val content = unescapeJson(contentMatch.groupValues[1])

                return when (type) {
                    "user" -> UserMessage(content)
                    "assistant" -> AssistantMessage(content)
                    "system" -> SystemMessage(content)
                    else -> null
                }
            }
        } catch (e: Exception) {
            log.error("Error deserializing message: ${e.message}", e)
        }

        return null
    }

    /**
     * Escapes special characters in a string for JSON.
     */
    private fun escapeJson(text: String): String {
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t")
    }

    /**
     * Unescapes special characters in a JSON string.
     */
    private fun unescapeJson(text: String): String {
        return text.replace("\\\"", "\"")
                  .replace("\\\\", "\\")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t")
    }
}