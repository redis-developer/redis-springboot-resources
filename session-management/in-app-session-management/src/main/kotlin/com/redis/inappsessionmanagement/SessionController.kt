package com.redis.inappsessionmanagement

import jakarta.servlet.http.HttpSession
import org.springframework.web.bind.annotation.*
import java.security.Principal


@RestController
@RequestMapping("/api")
class SessionController {
    @GetMapping("/session-info")
    fun getSessionInfo(session: HttpSession, principal: Principal): MutableMap<String?, Any?> {
        val sessionInfo: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        sessionInfo.put("sessionId", session.id)
        sessionInfo.put("creationTime", session.creationTime)
        sessionInfo.put("lastAccessedTime", session.lastAccessedTime)
        sessionInfo.put("maxInactiveInterval", session.maxInactiveInterval)
        sessionInfo.put("username", principal.name)
        return sessionInfo
    }

    @PostMapping("/session-attribute")
    fun setSessionAttribute(
        @RequestParam key: String?,
        @RequestParam value: String?,
        session: HttpSession
    ) {
        session.setAttribute(key, value)
    }

    @GetMapping("/session-attribute")
    fun getSessionAttribute(
        @RequestParam key: String?,
        session: HttpSession
    ): Any? {
        return session.getAttribute(key)
    }
}