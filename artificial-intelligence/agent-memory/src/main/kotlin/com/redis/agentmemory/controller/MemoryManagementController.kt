package com.redis.agentmemory.controller

import com.redis.agentmemory.memory.MemoryService
import com.redis.agentmemory.memory.model.Memory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/memory")
class MemoryManagementController(
    private val memoryService: MemoryService
) {
    @GetMapping("/retrieve")
    fun retrieveMemories(
        @RequestParam(required = false) userId: String?
    ): ResponseEntity<List<Memory>> {
        val memories = memoryService.retrieveMemories(
            "",
            null,
            userId,
            50,
            0.1f
        ).map { it.memory }
        return ResponseEntity.ok(memories)
    }
}
