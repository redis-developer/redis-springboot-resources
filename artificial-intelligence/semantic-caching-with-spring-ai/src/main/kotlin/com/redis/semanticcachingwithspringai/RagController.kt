package com.redis.semanticcachingwithspringai

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.util.*

@Controller
class RagController(
    private val ragService: RagService
) {

    @PostMapping("/chat/startChat")
    @ResponseBody
    fun startChat(): Message {
        return Message(UUID.randomUUID().toString())
    }

    @PostMapping("/chat/{chatId}")
    @ResponseBody
    fun chatMessage(@PathVariable chatId: String, @RequestBody prompt: Prompt): ChatResponse {
        val result = ragService.retrieve(prompt.prompt)
        return ChatResponse(
            message = result.generation.output.text.toString(),
            metrics = MetricsResponse(
                embeddingTimeMs = result.metrics.embeddingTimeMs,
                searchTimeMs = result.metrics.searchTimeMs,
                llmTimeMs = result.metrics.llmTimeMs,
                cachingTimeMs = result.metrics.cachingTimeMs
            )
        )
    }
}

data class Message(val message: String = "")

data class Prompt(val prompt: String = "")

data class MetricsResponse(
    val embeddingTimeMs: Long,
    val searchTimeMs: Long,
    val llmTimeMs: Long,
    val cachingTimeMs: Long = 0
)

data class ChatResponse(
    val message: String,
    val metrics: MetricsResponse
)
