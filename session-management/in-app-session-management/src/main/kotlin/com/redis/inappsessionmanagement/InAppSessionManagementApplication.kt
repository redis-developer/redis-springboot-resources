package com.redis.inappsessionmanagement

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean

@SpringBootApplication
class InAppSessionManagementApplication {

    private val logger = LoggerFactory.getLogger(InAppSessionManagementApplication::class.java)

    @Bean
    fun cmdLineRunner(): CommandLineRunner {
        return CommandLineRunner {
            logger.info("java.io.tmpdir: " + System.getProperty("java.io.tmpdir"))
        }
    }
}

fun main(args: Array<String>) {
    runApplication<InAppSessionManagementApplication>(*args)
}

