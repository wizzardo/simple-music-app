package com.example.be.service

import com.example.be.db.dto.AlbumDto
import com.example.be.db.dto.ArtistDto
import com.wizzardo.tools.image.ImageTools
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
//        println("executing command: $command")
        val process = Runtime.getRuntime().exec(arrayOf("./ffprobe", "-hide_banner", "-i", f.canonicalPath))
        process.waitFor()
//        println("output:")
//        println(String(process.inputStream.readAllBytes()))

//        println("error:")
        var message = String(process.errorStream.readAllBytes())
//        println(message)

        val start = message.indexOf("Metadata")
        if (start == -1)
            return MetaData()

        message = message.substring(start + 9).trim()
        val metadata: MutableMap<String, String> = HashMap()
        for (s in message.split("\n")) {
            val arr = s.trim().split(Regex(": "), 2)
            if (arr.size != 2)
                continue

            val (key, value) = arr
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
            streams = metadata.keys.filter { it.startsWith("stream") }.map { metadata[it]!! },
        )
    }

    fun convert(artist: ArtistDto, album: AlbumDto, song: AlbumDto.Song, format: AudioFormat, bitrate: Int): ByteArray {
        val audio = song.streams.find { it.startsWith("Audio:") }!!
        if (format == AudioFormat.FLAC)
            if (audio.contains("flac"))
                return songService.getSongData(artist, album, song)
            else
                throw IllegalArgumentException("Upconvert is not preferred");

        val matcher = bitratePattern.matcher(audio)

        val b = if (matcher.find())
            matcher.group(1).toInt()
        else
            320

        if (audio.contains(format.name.lowercase())) {
            if (b <= bitrate)
                return songService.getSongData(artist, album, song)
        }

        return doConvert(artist, album, song, format, Math.min(bitrate, b))
    }

    protected fun doConvert(artist: ArtistDto, album: AlbumDto, song: AlbumDto.Song, format: AudioFormat, bitrate: Int): ByteArray {
        val tempFile = File.createTempFile("from_", "." + song.path.substringAfterLast('.'))
        val tempOutFile = File.createTempFile("to_", "." + format.extension)
        try {
            songService.copySongData(artist, album, song, tempFile)

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
                    "-c:a",
                    format.codec,
                    "-ab",
                    bitrate.toString() + "k",
                    tempOutFile.canonicalPath
                )
//            println("executing command: ${Arrays.toString(command)}")
            val process = Runtime.getRuntime().exec(command)
            val exited = process.waitFor(30, TimeUnit.SECONDS)
            if (!exited) {
                process.destroy()
            }

//            println(stopwatch)
//            println("output:")
//            println(String(process.inputStream.readAllBytes()))
//            println("error:")
//            val message = String(process.errorStream.readAllBytes())
//            println(message)

            return Files.readAllBytes(tempOutFile.toPath())
        } finally {
            tempFile.delete()
            tempOutFile.delete()
        }
    }

    fun extractCoverArt(audio: File, to: File) {
        val tempFile = File.createTempFile("cover", ".png")
        try {
            val command =
                arrayOf(
                    "./ffmpeg",
                    "-nostdin",
                    "-y",
                    "-hide_banner",
                    "-i",
                    audio.canonicalPath,
                    "-an",
                    tempFile.canonicalPath
                )
            println("executing command: ${Arrays.toString(command)}")
            val process = Runtime.getRuntime().exec(command)
            val exited = process.waitFor(30, TimeUnit.SECONDS)
            if (!exited) {
                process.destroy()
            }
            println("output:")
            println(String(process.inputStream.readAllBytes()))
            println("error:")
            val message = String(process.errorStream.readAllBytes())
            println(message)

            val image = ImageTools.read(tempFile)
            ImageTools.saveJPG(image, to, 90)
        } finally {
            tempFile.delete()
        }
    }

    fun extractCoverArt(audio: File): ByteArray {
        val tempFile = File.createTempFile("cover", ".png")
        try {
            val command =
                arrayOf(
                    "./ffmpeg",
                    "-nostdin",
                    "-y",
                    "-hide_banner",
                    "-i",
                    audio.canonicalPath,
                    "-an",
                    tempFile.canonicalPath
                )
//            println("executing command: ${Arrays.toString(command)}")
            val process = Runtime.getRuntime().exec(command)
            val exited = process.waitFor(30, TimeUnit.SECONDS)
            if (!exited) {
                process.destroy()
            }
//            println("output:")
//            println(String(process.inputStream.readAllBytes()))
//            println("error:")
//            val message = String(process.errorStream.readAllBytes())
//            println(message)

            val image = ImageTools.read(tempFile)
            return ImageTools.saveJPGtoBytes(image, 90)
        } finally {
            tempFile.delete()
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
        val streams: List<String> = emptyList(),
    )

    enum class AudioFormat(val mimeType: String, val extension: String, val codec: String) {
        MP3("audio/mpeg", "mp3", "libmp3lame"),
        AAC("audio/aac", "aac", "libfdk_aac"),
        OGG("audio/ogg", "ogg", "libvorbis"),
        OPUS("audio/opus", "opus", "libopus"),
        FLAC("audio/x-flac", "flac", "flac");

    }
}
