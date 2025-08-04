package com.redis.cachingwithkotlinxserialization.cache
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.SerializationException
import java.nio.charset.StandardCharsets

class KotlinxRedisSerializer<T : Any>(
    private val serializer: KSerializer<T>,
    private val json: Json = Json
) : RedisSerializer<T> {

    override fun serialize(t: T?): ByteArray? {
        return try {
            if (t == null) return null
            json.encodeToString(serializer, t).toByteArray(StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw SerializationException("Could not serialize: $t", e)
        }
    }

    override fun deserialize(bytes: ByteArray?): T? {
        return try {
            if (bytes == null || bytes.isEmpty()) return null
            val str = String(bytes, StandardCharsets.UTF_8)
            json.decodeFromString(serializer, str)
        } catch (e: Exception) {
            throw SerializationException("Could not deserialize", e)
        }
    }
}

inline fun <reified T : Any> kotlinxRedisSerializer(
    json: Json = Json
): RedisSerializer<T> {
    return KotlinxRedisSerializer(serializer = json.serializersModule.serializer(), json = json)
}