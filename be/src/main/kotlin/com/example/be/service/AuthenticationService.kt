package com.example.be.service

import com.wizzardo.tools.security.Base64
import com.wizzardo.tools.security.SHA256
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class AuthenticationService(
    @Value("\${auth.username:}")
    private val username: String?,
    @Value("\${auth.password:}")
    private val password: String?,
) {
    private val secret: String

    init {
        secret = sign(username ?: "", password ?: "")
    }

    enum class Permission(
        val parent: Permission? = null,
    ) {
        ADMIN,
        READ(ADMIN),
        WRITE(ADMIN),
    }

    fun isAuthenticationEnabled(): Boolean {
        return !username.isNullOrBlank() && !password.isNullOrBlank()
    }

    fun isTokenValid(decodedToken: DecodedToken): Boolean {
        return isTokenValid(decodedToken, secret)
    }

    fun isTokenValid(decodedToken: DecodedToken, secret: String): Boolean {
        if (decodedToken.validUntil == -1L)
            return sign(decodedToken.data + "|" + decodedToken.action, secret) == decodedToken.sign
        else
            return System.currentTimeMillis() < decodedToken.validUntil && decodedToken.sign == sign(
                decodedToken.data + "|" + decodedToken.action + "|" + decodedToken.validUntil, secret
            )
    }

    fun sign(data: String, secret: String): String {
        return SHA256.create().update(data).update(secret).asString()
    }

    fun getPermissions(token: String): Set<Permission> {
        val decodeToken = decodeToken(token)
        if (decodeToken == null || !isTokenValid(decodeToken))
            return Collections.emptySet()

        return setOf(Permission.ADMIN)
    }

    enum class TokenAction {
        AUTH
    }

    data class DecodedToken(
        val data: String,
        val action: TokenAction,
        val validUntil: Long,
        val sign: String,
    )

    fun decodeToken(src: String?): DecodedToken? {
        if (src == null || src.isEmpty())
            return null
        val decode = Base64.decode(src.replace('*', '=')) ?: return null

        val decoded = String(decode, StandardCharsets.UTF_8).split(':')
        val data = decoded[0].split('|')
        if (data.size == 2)
            return DecodedToken(data[0], TokenAction.valueOf(data[1]), -1, decoded[1])
        else
            return DecodedToken(data[0], TokenAction.valueOf(data[1]), data[2].toLong(), decoded[1])
    }

    fun authenticate(username: String, password: String): Boolean {
        return username.equals(this.username) && password.equals(this.password)
    }

    fun createAuthToken(validUntil: Long):String {
        val data = username + "|" + TokenAction.AUTH + "|" + validUntil
        val token = data + ":" + sign(data, secret)
        return Base64.encodeToString(token.toByteArray(StandardCharsets.UTF_8), false).replace('=', '*')
    }
}