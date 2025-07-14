package com.redis.agentmemory.controller

import com.redis.agentmemory.chat.ChatService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class MetricsResponse(
    val embeddingTimeMs: Long,
    val memoryRetrievalTimeMs: Long,
    val memoryExtractionTimeMs: Long,
    val memoryStorageTimeMs: Long,
    val llmTimeMs: Long
)

data class ChatResponseWithMetrics(
    val message: String,
    val metrics: MetricsResponse
)

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatService: ChatService
) {

    @PostMapping("/send")
    fun sendMessage(
        @RequestParam message: String,
        @RequestParam userId: String,
    ): ResponseEntity<ChatResponseWithMetrics> {
        val result = chatService.sendMessage(message, userId)
        return ResponseEntity.ok(
            ChatResponseWithMetrics(
                message = result.response.result.output.text ?: "",
                metrics = MetricsResponse(
                    embeddingTimeMs = result.metrics.embeddingTimeMs,
                    memoryRetrievalTimeMs = result.metrics.memoryRetrievalTimeMs,
                    memoryExtractionTimeMs = result.metrics.memoryExtractionTimeMs,
                    memoryStorageTimeMs = result.metrics.memoryStorageTimeMs,
                    llmTimeMs = result.metrics.llmTimeMs
                )
            )
        )
    }

    @GetMapping("/history")
    fun getConversationHistory(
        @RequestParam userId: String
    ): ResponseEntity<List<Map<String, String>>> {
        val history = chatService.getConversationHistory(userId)

        // Convert Message objects to a simpler format for the frontend
        val formattedHistory = history.map { message ->
            mapOf(
                "role" to message.javaClass.simpleName.replace("Message", "").lowercase(),
                "content" to message.text
            )
        }

        return ResponseEntity.ok(formattedHistory)
    }

    @DeleteMapping("/history")
    fun clearConversationHistory(
        @RequestParam userId: String
    ): ResponseEntity<Map<String, String>> {
        chatService.clearConversationHistory(userId)
        return ResponseEntity.ok(mapOf("status" to "success"))
    }
}
