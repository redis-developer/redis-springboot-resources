package com.redis.agentmemory

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@EnableRedisDocumentRepositories
@SpringBootApplication
class AgentMemoryApplication

fun main(args: Array<String>) {
    runApplication<AgentMemoryApplication>(*args)
}