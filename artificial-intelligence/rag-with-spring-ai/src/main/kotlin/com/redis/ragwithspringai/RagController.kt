package com.redis.ragwithspringai

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.util.*

@Controller
class RagController(
    private val ragService: RagService,
    private val embeddingStatusService: EmbeddingStatusService
) {

    @PostMapping("/chat/startChat")
    @ResponseBody
    fun startChat(): ResponseEntity<Any> {
        val embeddedDocs = embeddingStatusService.getTotalDocNum()
        if (embeddedDocs < 20000) {
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse("Embeddings are still being created ($embeddedDocs of 20000 already created). This operation takes around three minutes to complete. Please try again later."))
        }
        return ResponseEntity.ok(Message(UUID.randomUUID().toString()))
    }

    @PostMapping("/chat/{chatId}")
    @ResponseBody
    fun chatMessage(@PathVariable chatId: String, @RequestBody prompt: Prompt): ResponseEntity<Any> {
        val embeddedDocs = embeddingStatusService.getTotalDocNum()
        if (embeddedDocs < 20000) {
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse("Embeddings are still being created ($embeddedDocs of 20000 already created). This operation takes around three minutes to complete. Please try again later."))
        }

        val result = ragService.retrieve(prompt.prompt)
        return ResponseEntity.ok(ChatResponse(
            message = result.generation.output.text.toString(),
            metrics = MetricsResponse(
                embeddingTimeMs = result.metrics.embeddingTimeMs,
                searchTimeMs = result.metrics.searchTimeMs,
                llmTimeMs = result.metrics.llmTimeMs
            )
        ))
    }
}

data class Message(val message: String = "")

data class Prompt(val prompt: String = "")

data class MetricsResponse(
    val embeddingTimeMs: Long,
    val searchTimeMs: Long,
    val llmTimeMs: Long
)

data class ChatResponse(
    val message: String,
    val metrics: MetricsResponse
)

data class ErrorResponse(
    val error: String
)
