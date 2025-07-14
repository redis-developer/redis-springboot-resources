package com.redis.semanticcachingwithspringai

import org.springframework.ai.transformers.TransformersEmbeddingModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EmbeddingModelConfiguration {
    @Bean
    fun transformersEmbeddingClient(): TransformersEmbeddingModel {
        return TransformersEmbeddingModel()
    }
}