package com.redis.vectorsearchspringai

import com.redis.om.spring.RedisOMProperties
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.redis.RedisVectorStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import redis.clients.jedis.*
import redis.clients.jedis.search.Schema

@Configuration
class RedisConfig {

    @Bean
    fun jedisPooled(
        jedisConnectionFactory: JedisConnectionFactory): JedisPooled {
        val cc = jedisConnectionFactory.getClientConfiguration()
        val hostAndPort = HostAndPort(jedisConnectionFactory.hostName, jedisConnectionFactory.port)
        val standaloneConfig = jedisConnectionFactory.standaloneConfiguration
        val username = standaloneConfig?.username
        val password = standaloneConfig?.password
        val jedisClientConfig = createClientConfig(jedisConnectionFactory.database, username, password!!, cc)

        return JedisPooled(
            jedisConnectionFactory.getPoolConfig(),
            hostAndPort,
            jedisClientConfig
        )
    }

    @Bean
    fun movieVectorStore(
        embeddingModel: EmbeddingModel,
        jedisPooled: JedisPooled
    ): RedisVectorStore {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
            .indexName("movieIdx")
            .contentFieldName("extract")
            .embeddingFieldName("extractEmbedding")
            .metadataFields(
                RedisVectorStore.MetadataField("title", Schema.FieldType.TEXT),
                RedisVectorStore.MetadataField("year", Schema.FieldType.NUMERIC),
                RedisVectorStore.MetadataField("cast", Schema.FieldType.TAG),
                RedisVectorStore.MetadataField("genres", Schema.FieldType.TAG),
                RedisVectorStore.MetadataField("thumbnail", Schema.FieldType.TEXT),
            )
            .prefix("movies:")
            .initializeSchema(true)
            .vectorAlgorithm(RedisVectorStore.Algorithm.HSNW)
            .build()
    }

    private fun createClientConfig(
        database: Int,
        username: String?,
        password: RedisPassword,
        clientConfiguration: JedisClientConfiguration
    ): JedisClientConfig {
        val jedisConfigBuilder = DefaultJedisClientConfig.builder()

        clientConfiguration.clientName.ifPresent { jedisConfigBuilder.clientName(it) }
        jedisConfigBuilder.connectionTimeoutMillis(clientConfiguration.connectTimeout.toMillis().toInt())
        jedisConfigBuilder.socketTimeoutMillis(clientConfiguration.readTimeout.toMillis().toInt())
        jedisConfigBuilder.database(database)

        jedisConfigBuilder.clientSetInfoConfig(
            ClientSetInfoConfig.withLibNameSuffix("redis-om-spring_v${RedisOMProperties.ROMS_VERSION}")
        )

        if (!username.isNullOrEmpty()) {
            jedisConfigBuilder.user(username)
        }

        password.toOptional().map { it.toString() }.ifPresent { jedisConfigBuilder.password(it) }

        if (clientConfiguration.isUseSsl) {
            jedisConfigBuilder.ssl(true)

            clientConfiguration.sslSocketFactory.ifPresent { jedisConfigBuilder.sslSocketFactory(it) }
            clientConfiguration.hostnameVerifier.ifPresent { jedisConfigBuilder.hostnameVerifier(it) }
            clientConfiguration.sslParameters.ifPresent { jedisConfigBuilder.sslParameters(it) }
        }

        return jedisConfigBuilder.build()
    }
}