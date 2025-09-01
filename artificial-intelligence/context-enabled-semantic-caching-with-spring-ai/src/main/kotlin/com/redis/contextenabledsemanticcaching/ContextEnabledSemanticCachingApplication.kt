package com.redis.contextenabledsemanticcaching

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ContextEnabledSemanticCachingApplication

fun main(args: Array<String>) {
    runApplication<ContextEnabledSemanticCachingApplication>(*args)
}
