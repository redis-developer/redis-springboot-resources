package com.redis.agentmemory.memory.shortterm

import com.redis.om.spring.repository.RedisDocumentRepository

interface ShortTermMemoryRepository : RedisDocumentRepository<ChatHistory, String>