package com.redis.agentmemory

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AgentMemoryApplication

fun main(args: Array<String>) {
    runApplication<AgentMemoryApplication>(*args)
}