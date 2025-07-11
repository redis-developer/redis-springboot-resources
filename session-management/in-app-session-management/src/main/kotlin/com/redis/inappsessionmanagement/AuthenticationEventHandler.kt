package com.redis.inappsessionmanagement

import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class AuthenticationEventHandler : AuthenticationSuccessHandler, AuthenticationFailureHandler, LogoutSuccessHandler {

    private val logger = LoggerFactory.getLogger(AuthenticationEventHandler::class.java)
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val username = authentication.name
        val timestamp = LocalDateTime.now().format(formatter)
        val sessionId = request.session.id

        logger.info("User '{}' successfully logged in at {} (Session ID: {})", username, timestamp, sessionId)
        response.sendRedirect("/welcome")
    }

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        val username = request.getParameter("username")
        val timestamp = LocalDateTime.now().format(formatter)

        logger.warn("Failed login attempt for user '{}' at {} - Reason: {}", username, timestamp, exception.message)
        response.sendRedirect("/login?error")
    }

    @Throws(IOException::class, ServletException::class)
    override fun onLogoutSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?
    ) {
        val username = authentication?.name ?: "unknown"
        val timestamp = LocalDateTime.now().format(formatter)

        logger.info("User '{}' successfully logged out at {}", username, timestamp)
        response.sendRedirect("/login?logout")
    }
}