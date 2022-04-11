package com.example.be.service

import org.springframework.stereotype.Service
import java.io.File

@Service
class FFmpegService {

    fun getMetaData(f: File): Map<String, String> {
        val process = Runtime.getRuntime().exec("./ffprobe -hide_banner -i " + f.absolutePath)
        process.waitFor()
        println("output:")
        println(String(process.inputStream.readAllBytes()))

        println("error:")
        var message = String(process.errorStream.readAllBytes())
        println(message)

        val start = message.indexOf("Metadata")
        if (start == -1)
            return emptyMap()

        message = message.substring(start + 9).trim()
        val metadata: MutableMap<String, String> = HashMap()
        for (s in message.split("\n")) {
            val (key, value) = s.trim().split(Regex(":"), 2)
            if (value.isNotBlank())
                metadata[key.trim()] = value.trim()
        }
        return metadata
    }
}
