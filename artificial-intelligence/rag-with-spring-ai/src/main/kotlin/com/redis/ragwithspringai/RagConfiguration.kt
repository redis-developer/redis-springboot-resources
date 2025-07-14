package com.redis.ragwithspringai

import org.springframework.ai.transformers.TransformersEmbeddingModel
import org.springframework.ai.vectorstore.redis.RedisVectorStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.JedisPooled

@Configuration
class RagConfiguration {

    @Bean
    fun transformersEmbeddingClient(): TransformersEmbeddingModel {
        return TransformersEmbeddingModel()
    }

    @Bean
    fun memoryVectorStore(
        embeddingModel: TransformersEmbeddingModel,
        jedisPooled: JedisPooled
    ): RedisVectorStore {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
            .indexName("beerIdx")
            .contentFieldName("content")
            .embeddingFieldName("embedding")
            .prefix("beer:")
            .initializeSchema(true)
            .vectorAlgorithm(RedisVectorStore.Algorithm.HSNW)
            .build()
    }
}