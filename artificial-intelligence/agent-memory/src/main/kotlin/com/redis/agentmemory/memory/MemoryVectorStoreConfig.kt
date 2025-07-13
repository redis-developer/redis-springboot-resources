package com.redis.agentmemory.memory

import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.redis.RedisVectorStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.search.Schema

@Configuration
class MemoryVectorStoreConfig {

    @Bean
    fun memoryVectorStore(
        embeddingModel: EmbeddingModel,
        jedisPooled: JedisPooled
    ): RedisVectorStore {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
            .indexName("memoryIdx")
            .contentFieldName("content")
            .embeddingFieldName("embedding")
            .metadataFields(
                RedisVectorStore.MetadataField("memoryType", Schema.FieldType.TAG),
                RedisVectorStore.MetadataField("metadata", Schema.FieldType.TEXT),
                RedisVectorStore.MetadataField("userId", Schema.FieldType.TAG),
                RedisVectorStore.MetadataField("createdAt", Schema.FieldType.TEXT)
            )
            .prefix("memory:")
            .initializeSchema(true)
            .vectorAlgorithm(RedisVectorStore.Algorithm.HSNW)
            .build()
    }
}