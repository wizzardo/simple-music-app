package com.example.be.service

import org.springframework.stereotype.Service
import java.io.File

@Service
class FFmpegService {

    fun getMetaData(f: File): MetaData {
        val command = "./ffprobe -hide_banner -i \"" + f.canonicalPath + "\""
        println("executing command: $command")
        val process = Runtime.getRuntime().exec(arrayOf("./ffprobe", "-hide_banner", "-i", f.canonicalPath))
        process.waitFor()
        println("output:")
        println(String(process.inputStream.readAllBytes()))

        println("error:")
        var message = String(process.errorStream.readAllBytes())
        println(message)

        val start = message.indexOf("Metadata")
        if (start == -1)
            return MetaData()

        message = message.substring(start + 9).trim()
        val metadata: MutableMap<String, String> = HashMap()
        for (s in message.split("\n")) {
            val (key, value) = s.trim().split(Regex(":"), 2)
            if (value.isNotBlank()) {
                val normalizedKey = key.trim().lowercase()
                if (!metadata.containsKey(normalizedKey))
                    metadata[normalizedKey] = value.trim()
            }
        }

//        println(metadata)
        return MetaData(
            date = metadata["date"],
            album = metadata["album"],
            artist = metadata["artist"],
            title = metadata["title"],
            track = metadata["track"]?.toInt(),
            comment = metadata["comment"],
            duration = metadata["duration"],
            stream = metadata["stream #0"],
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
