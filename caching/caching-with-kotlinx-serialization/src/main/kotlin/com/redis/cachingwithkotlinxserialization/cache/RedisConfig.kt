package com.redis.cachingwithkotlinxserialization.cache

import com.redis.cachingwithkotlinxserialization.model.TrainTicket
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.RedisSerializationContext

@Configuration
class RedisConfig {

    @Bean
    fun redisCacheManager(factory: RedisConnectionFactory): RedisCacheManager {
        val serializer = kotlinxRedisSerializer<TrainTicket>()

        val config = RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build()
    }
}