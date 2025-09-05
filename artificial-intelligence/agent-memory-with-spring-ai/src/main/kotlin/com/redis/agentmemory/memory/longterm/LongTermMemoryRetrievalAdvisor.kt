package com.redis.agentmemory.memory.longterm

import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.*
import org.springframework.core.Ordered
import org.springframework.stereotype.Component

@Component
class LongTermMemoryRetrievalAdvisor(
  private val memoryService: MemoryService,
) : CallAdvisor, Ordered {

  companion object {
    const val USER_ID = "ltm_user_id"   // pass per-call
    const val TOP_K = "ltm_top_k"       // pass per-call (default 5)
  }

  override fun getOrder() = Ordered.HIGHEST_PRECEDENCE + 40
  override fun getName() = "LongTermMemoryRetrievalAdvisor"

  override fun adviseCall(req: ChatClientRequest, chain: CallAdvisorChain): ChatClientResponse {
    val userId = (req.context()[USER_ID] as? String) ?: "system"
    val k = (req.context()[TOP_K] as? Int) ?: 5

    val query = req.prompt().userMessage.text
    val memories = memoryService.retrieveRelevantMemories(query, userId = userId)
      .take(k)

    val memoryBlock = buildString {
      appendLine("Use the MEMORY below if relevant. Keep answers factual and concise.")
      appendLine("----- MEMORY -----")
      memories.forEachIndexed { i, m -> appendLine("${i+1}. ${m.memory.content}") }
      appendLine("------------------")
    }

    val enrichedPrompt = req.prompt().augmentSystemMessage { sys ->
      val existing = sys.text
      sys.mutate()
        .text(
          buildString {
            appendLine(memoryBlock)
            if (existing.isNotBlank()) {
              appendLine()
              append(existing)
            }
          }
        ).build()
    }

    val enrichedReq = req.mutate()
      .prompt(enrichedPrompt)
      .build()

    return chain.nextCall(enrichedReq)
  }
}