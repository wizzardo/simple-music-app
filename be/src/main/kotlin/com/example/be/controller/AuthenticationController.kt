package com.example.be.controller

import com.example.be.misc.HttpDateFormatterHolder
import com.example.be.service.AuthenticationService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.*

@RestController
class AuthenticationController(
    val authenticationService: AuthenticationService
) {

    data class LoginRequest(
        var username: String = "",
        var password: String = "",
    )

    data class LoginResponse(
        var validUntil: Long = 0,
    )

    @PostMapping("/login")
    fun login(
        @RequestBody data: LoginRequest,
    ): ResponseEntity<LoginResponse> {
        if (!authenticationService.authenticate(data.username, data.password))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        val validUntil = Date.from(OffsetDateTime.now().plusYears(1).toInstant())
        val token = authenticationService.createAuthToken(validUntil.time)
        val headers = HttpHeaders()
        headers.add(
            HttpHeaders.SET_COOKIE, "token=${token}; Secure; HttpOnly; SameSite=None; Path=/; Expires=${
                HttpDateFormatterHolder.get().format(validUntil)
            }"
        )
        return ResponseEntity(LoginResponse(validUntil.time), headers, HttpStatus.OK)
    }


    data class LoginRequiredResponse(
        var required: Boolean = false
    )

    @GetMapping("/login/required")
    fun isLoginRequired(
    ): ResponseEntity<LoginRequiredResponse> {
        return ResponseEntity.ok(LoginRequiredResponse(authenticationService.isAuthenticationEnabled()))
    }

}
