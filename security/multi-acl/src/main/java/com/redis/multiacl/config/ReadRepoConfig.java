package com.redis.multiacl.config;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRedisDocumentRepositories(
        basePackages = {
                "com.redis.multiacl.repository.read",
                "com.redis.multiacl.model"
        },
        keyValueTemplateRef = "readKeyValueTemplate",
        redisTemplateRef = "readRedisOperations"
)
class ReadRepoConfig { }