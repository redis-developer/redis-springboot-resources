package com.redis.distributedsessionmanagement

import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import java.security.Principal
import java.util.Date
import java.util.LinkedHashMap

@Controller
class WelcomeController {

    @GetMapping("/welcome")
    fun welcome(session: HttpSession, principal: Principal, model: Model): String {
        val sessionInfo = LinkedHashMap<String, Any>().apply {
            put("Session ID", session.id)
            put("Username", principal.name)
            put("Creation Time", Date(session.creationTime))
            put("Last Accessed Time", Date(session.lastAccessedTime))
            put("Max Inactive Interval", "${session.maxInactiveInterval} seconds")
        }

        model.addAttribute("sessionInfo", sessionInfo)
        return "welcome"
    }
}