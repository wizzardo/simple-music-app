package com.example.be.filter

import com.example.be.service.AuthenticationService
import com.wizzardo.http.Filter
import com.wizzardo.http.HttpConnection
import com.wizzardo.http.framework.RequestContext
import com.wizzardo.http.framework.di.Injectable
import com.wizzardo.http.request.Request
import com.wizzardo.http.response.Response

@Injectable
class AuthFilter : Filter {
    lateinit var authenticationService: AuthenticationService

    override fun filter(request: Request<out HttpConnection<*, *, *>, *>, response: Response): Boolean {
        if (authenticationService.isAuthenticationEnabled()) {
            val token = request.cookies().get("token")
            if (token != null) {
                val permissions = authenticationService.getPermissions(token)
                if (permissions.isNotEmpty()) {
                    RequestContext.get().requestHolder.put("permissions", permissions)
                }
            }
        } else {
            RequestContext.get().requestHolder.put("permissions", AuthenticationService.Permission.values().toSet())
        }

        return true
    }

}