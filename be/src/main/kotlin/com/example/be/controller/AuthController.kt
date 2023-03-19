package com.example.be.controller

import com.example.be.service.AuthenticationService
import com.wizzardo.http.framework.Controller
import com.wizzardo.http.framework.RequestContext
import com.wizzardo.http.request.Header
import com.wizzardo.http.response.CookieBuilder
import com.wizzardo.http.response.Response
import com.wizzardo.http.response.Status
import com.wizzardo.tools.json.JsonTools
import java.time.OffsetDateTime
import java.util.*

class AuthController : Controller() {

    lateinit var authenticationService: AuthenticationService

    data class LoginRequest(
        var username: String = "",
        var password: String = "",
    )

    data class LoginResponse(
        var validUntil: Long = 0,
    )

    fun login(
        data: LoginRequest,
    ): Response {
        if (!authenticationService.authenticate(data.username, data.password))
            return response.status(Status._403)

        val validUntil = Date.from(OffsetDateTime.now().plusYears(1).toInstant())
        val token = authenticationService.createAuthToken(validUntil.time)

        response.setCookie(CookieBuilder("token", token).httpOnly().secure().sameSite(CookieBuilder.SameSite.None).path("/").expires(validUntil))
        return response.status(Status._200)
            .appendHeader(Header.KV_CONTENT_TYPE_APPLICATION_JSON)
            .body(JsonTools.serialize(LoginResponse(validUntil.time)))
    }


    data class LoginRequiredResponse(
        var required: Boolean = false,
        var tokenValid: Boolean = false,
    )

    fun isLoginRequired(): LoginRequiredResponse {
        val permissions = RequestContext.get().requestHolder.get<Set<AuthenticationService.Permission>?>("permissions")
        val authenticationEnabled = authenticationService.isAuthenticationEnabled()
        val tokenValid = permissions != null
        return LoginRequiredResponse(authenticationEnabled, tokenValid)
    }

}
