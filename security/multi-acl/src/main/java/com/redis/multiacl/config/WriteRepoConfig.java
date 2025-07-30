package com.redis.multiacl.config;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRedisDocumentRepositories(
        basePackages = {
                "com.redis.multiacl.repository.write",
                "com.redis.multiacl.model"
        },
        keyValueTemplateRef = "writeKeyValueTemplate",
        redisTemplateRef = "writeRedisOperations"
)
class WriteRepoConfig { }