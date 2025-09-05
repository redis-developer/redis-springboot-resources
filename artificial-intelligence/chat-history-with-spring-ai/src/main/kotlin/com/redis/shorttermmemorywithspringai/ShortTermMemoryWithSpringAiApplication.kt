package com.redis.shorttermmemorywithspringai

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@EnableRedisDocumentRepositories
@SpringBootApplication
class ShortTermMemoryWithSpringAiApplication

fun main(args: Array<String>) {
    runApplication<ShortTermMemoryWithSpringAiApplication>(*args)
}
