package com.redis.agentmemory.memory.shortterm

import com.redis.om.spring.annotations.Document
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.data.annotation.Id
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.MessageType
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.content.Media

@Document(value = "short-term-memory", indexName = "shortTermHistoryIdx")
data class ChatHistory(
    @Id
    val id: String? = null,
    val messages: List<StoredMessage>,
)

data class StoredMessage(
    val text: String = "",
    val metadata: Map<String, Any> = emptyMap(),
    val toolCalls: List<AssistantMessage.ToolCall>? = null,
    val toolResponses: List<ToolResponseMessage.ToolResponse>? = null,
    val media: List<Media>? = null,
    val messageType: MessageType
) {
    fun toAi(): Message = when (messageType) {
        MessageType.USER -> UserMessage.builder().text(text).metadata(metadata).media(media ?: emptyList()).build()
        MessageType.ASSISTANT -> AssistantMessage(text, metadata, toolCalls ?: emptyList(), media ?: emptyList())
        MessageType.SYSTEM -> SystemMessage.builder().text(text).metadata(metadata).build()
        MessageType.TOOL -> ToolResponseMessage(toolResponses ?: emptyList(), metadata)
    }
}

