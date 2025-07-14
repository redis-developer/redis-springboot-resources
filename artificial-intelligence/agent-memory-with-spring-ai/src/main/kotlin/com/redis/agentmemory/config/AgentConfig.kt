package com.redis.agentmemory.config

import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AgentConfig {

    @Bean
    fun travelAgentSystemPrompt(): Message {
        val promptText = """
            You are a travel assistant helping users plan their trips. You remember user preferences
            and provide personalized recommendations based on past interactions.

            You have access to the following types of memory:
            1. Short-term memory: The current conversation thread
            2. Long-term memory:
               - Episodic: User preferences and past trip experiences (e.g., "User prefers window seats")
               - Semantic: General knowledge about travel destinations and requirements

            Always be helpful, personal, and context-aware in your responses.
            
            Always answer in text format. No markdown or special formatting.
        """.trimIndent()

        return SystemMessage(promptText)
    }
}
