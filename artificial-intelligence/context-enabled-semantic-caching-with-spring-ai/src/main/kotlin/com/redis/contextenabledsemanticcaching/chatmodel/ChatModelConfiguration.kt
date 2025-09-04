package com.redis.contextenabledsemanticcaching.chatmodel

import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration
import kotlin.time.Duration.Companion.seconds


@Configuration
class ChatModelConfiguration {
    @Bean
    fun openAiExpensiveChatModel(): OpenAiChatModel {
        val modelName = "gpt-5-2025-08-07"
        return openAiChatModel(modelName)
    }

    @Bean
    fun openAiCheapChatModel(): OpenAiChatModel {
        val modelName = "gpt-4.1-nano"
        return openAiChatModel(modelName)
    }

    private fun openAiChatModel(modelName: String): OpenAiChatModel {
        val factory = SimpleClientHttpRequestFactory()
        factory.setReadTimeout(Duration.ofSeconds(120000))

        val openAiApi = OpenAiApi.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .restClientBuilder(
                RestClient
                    .builder().requestFactory(factory)
            )
            .build()
        val openAiChatOptions = OpenAiChatOptions.builder()
            .model(modelName)
            .temperature(1.0)
            .build()

        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(openAiChatOptions)
            .build()
    }
}