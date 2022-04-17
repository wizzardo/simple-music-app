package com.example.be.service

import org.springframework.stereotype.Service
import java.io.File

@Service
class FFmpegService {

    fun getMetaData(f: File): MetaData {
        val process = Runtime.getRuntime().exec("./ffprobe -hide_banner -i " + f.absolutePath)
        process.waitFor()
        println("output:")
        println(String(process.inputStream.readAllBytes()))

        println("error:")
        var message = String(process.errorStream.readAllBytes())
//        println(message)

        val start = message.indexOf("Metadata")
        if (start == -1)
            return MetaData()

        message = message.substring(start + 9).trim()
        val metadata: MutableMap<String, String> = HashMap()
        for (s in message.split("\n")) {
            val (key, value) = s.trim().split(Regex(":"), 2)
            if (value.isNotBlank())
                metadata[key.trim()] = value.trim()
        }

//        println(metadata)
        return MetaData(
            date = metadata["date"],
            album = metadata["album"],
            artist = metadata["artist"],
            title = metadata["title"],
            track = metadata["track"]?.toInt(),
            comment = metadata["comment"],
            duration = metadata["Duration"],
            stream = metadata["Stream #0"],
        )
    }

    class MetaData(
        val date: String? = null,
        val album: String? = null,
        val artist: String? = null,
        val title: String? = null,
        val track: Int? = null,
        val comment: String? = null,
        val duration: String? = null,
        val stream: String? = null,
    )
}
