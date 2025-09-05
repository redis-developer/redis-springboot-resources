package com.redis.agentmemory.chat

import com.redis.agentmemory.memory.shortterm.ShortTermMemoryRepository
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.ChatMemoryRepository
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage

import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service

data class ChatResult(
    val response: ChatResponse,
)

@Service
class ChatService(
    private val chatClient: ChatClient,
    private val shortTermMemoryRepository: ShortTermMemoryRepository,
    private val travelAgentSystemPrompt: Message,
    private val chatMemoryRepository: ChatMemoryRepository
) {
    private val log = LoggerFactory.getLogger(ChatService::class.java)

    fun sendMessage(
        message: String,
        userId: String,
    ): ChatResult {
        // Use userId as the key for conversation history and long-term memory
        log.info("Processing message from user $userId: $message")
        val response = chatClient
            .prompt(
                Prompt(
                    travelAgentSystemPrompt,
                    UserMessage(message)
                )
            )
            .advisors { it
                .param(ChatMemory.CONVERSATION_ID, userId)
                .param("ltm_user_id", userId)
            }
            .call()

        return ChatResult(
            response = response.chatResponse()!!
        )
    }


    fun getConversationHistory(userId: String): List<Message?> {
        return chatMemoryRepository.findByConversationId(userId)
    }

    fun clearConversationHistory(userId: String) {
        shortTermMemoryRepository.deleteById(userId)
        log.info("Cleared conversation history for user $userId from Redis")
    }

}