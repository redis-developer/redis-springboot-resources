package com.redis.agentmemory.controller

import com.redis.agentmemory.chat.ChatService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class ChatResponse(
    val message: String
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
    ): ResponseEntity<ChatResponse> {
        val result = chatService.sendMessage(message, userId)
        return ResponseEntity.ok(
            ChatResponse(
                message = result.response.result.output.text ?: "",
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
                "role" to message?.messageType.toString(),
                "content" to (message?.text ?: "")
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
