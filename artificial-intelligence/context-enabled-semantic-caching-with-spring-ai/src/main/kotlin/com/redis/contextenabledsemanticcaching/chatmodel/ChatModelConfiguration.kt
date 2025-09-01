package com.redis.contextenabledsemanticcaching.chatmodel

import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class ChatModelConfiguration {
    @Bean
    fun openAiExpensiveChatModel(): OpenAiChatModel {
        val modelName = "gpt-5-2025-08-07"
        return openAiChatModel(modelName)
    }

    @Bean
    fun openAiCheapChatModel(): OpenAiChatModel {
        val modelName = "gpt-5-nano-2025-08-07"
        return openAiChatModel(modelName)
    }

    private fun openAiChatModel(modelName: String): OpenAiChatModel {
        val openAiApi = OpenAiApi.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .build()
        val openAiChatOptions = OpenAiChatOptions.builder()
            .model(modelName)
            .temperature(0.4)
            .build()

        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(openAiChatOptions)
            .build()
    }
}