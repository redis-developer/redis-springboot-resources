package com.redis.distributedsessionmanagement

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DistributedSessionManagementApplication

fun main(args: Array<String>) {
    runApplication<DistributedSessionManagementApplication>(*args)
}
