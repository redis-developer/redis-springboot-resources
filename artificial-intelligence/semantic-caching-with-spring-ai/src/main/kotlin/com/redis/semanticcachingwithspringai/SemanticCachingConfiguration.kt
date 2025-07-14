package com.redis.semanticcachingwithspringai

import org.springframework.ai.transformers.TransformersEmbeddingModel
import org.springframework.ai.vectorstore.redis.RedisVectorStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.search.Schema

@Configuration
class SemanticCachingConfiguration {
    @Bean
    fun semanticCachingVectorStore(
        embeddingModel: TransformersEmbeddingModel,
        jedisPooled: JedisPooled
    ): RedisVectorStore {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
            .indexName("semanticCachingIdx")
            .contentFieldName("content")
            .embeddingFieldName("embedding")
            .metadataFields(
                RedisVectorStore.MetadataField("answer", Schema.FieldType.TEXT),
                )
            .prefix("semantic-caching:")
            .initializeSchema(true)
            .vectorAlgorithm(RedisVectorStore.Algorithm.HSNW)
            .build()
    }
}