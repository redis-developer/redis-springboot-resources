package com.redis.vectorsearchspringai

import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.transformers.TransformersEmbeddingModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EmbeddingModelConfig {

    @Bean
    fun embeddingModel(): EmbeddingModel {
        return TransformersEmbeddingModel()
    }
}