package com.example.be.service

import com.wizzardo.tools.io.BytesTools
import com.wizzardo.tools.security.Base64
import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class RandomIdService(
    val random: SecureRandom = SecureRandom.getInstanceStrong()
) {

    fun generateId(): String {
        return Base64.encodeToString(BytesTools.toBytes(random.nextLong()), false, true).replace("=", "")
    }
}