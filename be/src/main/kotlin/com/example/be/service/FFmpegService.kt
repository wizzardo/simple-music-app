package com.example.be.service

import com.example.be.controller.UploadController
import com.example.be.db.dto.AlbumDto
import com.wizzardo.tools.misc.Stopwatch
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.collections.HashMap

@Service
class FFmpegService(
    private val songService: SongService
) {
    val bitratePattern = Pattern.compile("([0-9])+ *kb/s")

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

    fun convert(song: AlbumDto.Song, format: UploadController.AudioFormat, bitrate: Int): ByteArray {
        if (format == UploadController.AudioFormat.FLAC)
            if (song.stream.contains("flac"))
                return songService.getSongData(song)
            else
                throw IllegalArgumentException("Upconvert is not preferred");

        val matcher = bitratePattern.matcher(song.stream)

        val b = if (matcher.find())
            matcher.group(1).toInt()
        else
            1024

        if (song.stream.contains(format.name.lowercase())) {
            if (b <= bitrate)
                return songService.getSongData(song)
        }

        return doConvert(song, format, Math.min(bitrate, b))
    }

    protected fun doConvert(song: AlbumDto.Song, format: UploadController.AudioFormat, bitrate: Int): ByteArray {
        val tempFile = File.createTempFile("from_", "." + song.path.substringAfterLast('.'))
        val tempOutFile = File.createTempFile("to_", "." + format.name.lowercase())
        try {
            songService.copySongData(song, tempFile)

            val stopwatch = Stopwatch("converting to " + format)
            val command =
                arrayOf(
                    "./ffmpeg",
                    "-nostdin",
                    "-y",
                    "-hide_banner",
                    "-i",
                    tempFile.canonicalPath,
                    "-map",
                    "a",
                    "-ab",
                    bitrate.toString() + "k",
                    tempOutFile.canonicalPath
                )
            println("executing command: ${Arrays.toString(command)}")
            val process = Runtime.getRuntime().exec(command)
            val exited = process.waitFor(30, TimeUnit.SECONDS)
            if (!exited) {
                process.destroy()
            }

            println(stopwatch)
            println("output:")
            println(String(process.inputStream.readAllBytes()))
            println("error:")
            val message = String(process.errorStream.readAllBytes())
            println(message)

            return Files.readAllBytes(tempOutFile.toPath())
        } finally {
            tempFile.delete()
            tempOutFile.delete()
        }
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
