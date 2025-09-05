package com.redis.agentmemory.memory.longterm

import com.redis.agentmemory.memory.longterm.model.MemoryType
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.*
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.core.Ordered
import org.springframework.stereotype.Component

@Component
class LongTermMemoryRecorderAdvisor(
    private val memoryService: MemoryService,
    private val chatModel: ChatModel
) : CallAdvisor, Ordered {

    data class MemoryCandidate(val content: String, val type: MemoryType, val userId: String?)
    data class ExtractionResult(val memories: List<MemoryCandidate> = emptyList())

    private val extractorConverter = BeanOutputConverter(ExtractionResult::class.java)

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE + 60
    override fun getName(): String = "LongTermMemoryRecorderAdvisor"

    override fun adviseCall(req: ChatClientRequest, chain: CallAdvisorChain): ChatClientResponse {
        // 1) Proceed with the normal call (other advisors may have enriched the prompt)
        val res = chain.nextCall(req)

        // 2) Build extraction prompt (user + assistant text of *this* turn)
        val userText = req.prompt().userMessage.text
        val assistantText = res.chatResponse()?.result?.output?.text

        // 3) Ask the model to extract long-term memories as structured JSON
        val schemaHint = extractorConverter.jsonSchema // JSON schema string for the POJO
        val extractSystem = """
            You extract LONG-TERM MEMORIES from a dialogue turn.

            A memory is either:

            1. EPISODIC MEMORIES: Personal experiences and user-specific preferences
               Examples: "User prefers Delta airlines", "User visited Paris last year"

            2. SEMANTIC MEMORIES: General domain knowledge and facts
               Examples: "Singapore requires passport", "Tokyo has excellent public transit"

            Only extract clear, factual information. Do not make assumptions or infer information that isn't explicitly stated.
            If no memories can be extracted, return an empty array.
            
            The instance must conform to this JSON Schema (for validation, do not output it):
              $schemaHint

            Do not include code fences, schema, or properties. Output a single-line JSON object.
        """.trimIndent()

        val extractUser = """
            USER SAID:
            $userText

            ASSISTANT REPLIED:
            $assistantText

            Extract up to 5 memories with correct type; set userId if present/known.
        """.trimIndent()

        val options: ChatOptions = OpenAiChatOptions.builder()
            .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
            .build()

        val extraction = chatModel.call(
            Prompt(
                listOf(
                    UserMessage(extractUser),
                    SystemMessage(extractSystem)
                ),
                options
            ),
        )

        val parsed = extractorConverter.convert(extraction.result.output.text ?: "")
            ?: ExtractionResult()

        // 4) Persist memories (MemoryService handles dedupe/thresholding)
        val userId = (req.context["ltm_user_id"] as? String) // optional per-call param
        parsed.memories.forEach { m ->
            val owner = m.userId ?: userId
            memoryService.storeMemory(
                content = m.content,
                memoryType = m.type,
                userId = owner
            )
        }

        return res
    }
}