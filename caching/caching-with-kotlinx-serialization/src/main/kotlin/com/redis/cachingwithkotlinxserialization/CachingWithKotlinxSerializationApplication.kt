package com.redis.cachingwithkotlinxserialization

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@EnableCaching
@SpringBootApplication
class CachingWithKotlinxSerializationApplication

fun main(args: Array<String>) {
    runApplication<CachingWithKotlinxSerializationApplication>(*args)
}
