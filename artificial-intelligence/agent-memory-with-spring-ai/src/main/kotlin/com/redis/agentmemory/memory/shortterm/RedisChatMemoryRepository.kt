package com.redis.agentmemory.memory.shortterm

import com.redis.om.spring.search.stream.EntityStream
import org.springframework.ai.chat.memory.ChatMemoryRepository
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.MessageType
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.stereotype.Component
import java.util.stream.Collectors

@Component
class RedisChatMemoryRepository(
    private val shortTermMemoryRepository: ShortTermMemoryRepository,
    private val entityStream: EntityStream
) : ChatMemoryRepository {

    override fun findConversationIds(): List<String> {
        return entityStream.of(ChatHistory::class.java)
            .map(ChatHistory::id)
            .collect(Collectors.toList())
            .filterNotNull()
    }

    override fun findByConversationId(conversationId: String): List<Message> {
        val optHist = shortTermMemoryRepository.findById(conversationId)
        return if (optHist.isPresent) { optHist.get().messages.map { msg -> msg.toAi() } } else { emptyList() }
    }

    override fun saveAll(
        conversationId: String,
        messages: List<Message>
    ) {
        val storedMessages = messages.map { msg ->
            val storedMessage = when (msg) {
                is AssistantMessage -> {
                    StoredMessage(
                        text = msg.text ?: "",
                        metadata = msg.metadata,
                        media = msg.media,
                        toolCalls = msg.toolCalls,
                        messageType = MessageType.ASSISTANT
                    )
                }

                is UserMessage -> {
                    StoredMessage(
                        text = msg.text,
                        metadata = msg.metadata,
                        media = msg.media,
                        messageType = MessageType.USER
                    )
                }

                is SystemMessage -> {
                    StoredMessage(
                        text = msg.text,
                        metadata = msg.metadata,
                        messageType = MessageType.SYSTEM
                    )
                }

                is ToolResponseMessage -> {
                    StoredMessage(
                        toolResponses = msg.responses,
                        metadata = msg.metadata,
                        messageType = MessageType.TOOL
                    )
                }
                else -> error("Unknown message type ${msg.javaClass.canonicalName}")
            }
            storedMessage
        }

        val optHist = shortTermMemoryRepository.findById(conversationId)
        if (optHist.isPresent) {
            shortTermMemoryRepository.save(optHist.get().copy(messages = storedMessages))
        } else {
            shortTermMemoryRepository.save(ChatHistory(id = conversationId, messages = storedMessages))
        }
    }

    override fun deleteByConversationId(conversationId: String) {
        shortTermMemoryRepository.deleteById(conversationId)
    }
}