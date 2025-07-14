package com.redis.ragwithspringai

import com.redis.om.spring.RedisOMProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import redis.clients.jedis.*

@Configuration
class RedisConfig {

    @Bean
    fun jedisPooled(
        jedisConnectionFactory: JedisConnectionFactory
    ): JedisPooled {
        val cc = jedisConnectionFactory.clientConfiguration
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