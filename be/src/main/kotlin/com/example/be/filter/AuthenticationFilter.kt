package com.example.be.filter

import com.example.be.service.AuthenticationService
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

@Component
@Order(1)
class AuthenticationFilter(
    val authenticationService: AuthenticationService,
) : Filter {
    override fun doFilter(servletRequest: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        val request = servletRequest as HttpServletRequest
        if (authenticationService.isAuthenticationEnabled()) {
            val token = request.cookies?.find { it.name == "token" }?.value
            if (token != null) {
                val permissions = authenticationService.getPermissions(token)
                if (permissions.isNotEmpty()) {
                    request.setAttribute("permissions", permissions)
                }
            }
        } else {
            request.setAttribute("permissions", AuthenticationService.Permission.values().toSet())
        }

        chain!!.doFilter(request, response);
    }

}