package com.example.be.service

import com.example.be.db.model.Artist
import com.example.be.db.model.Artist.*
import com.wizzardo.tools.image.ImageTools
import com.wizzardo.tools.io.FileTools
import com.wizzardo.tools.io.IOTools
import com.wizzardo.tools.misc.DateIso8601
import com.wizzardo.tools.misc.Stopwatch
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.IllegalStateException
import java.lang.StringBuilder
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.collections.HashMap

class FFmpegService(
    private val songService: SongService,
) {
    val threadPool = Executors.newFixedThreadPool(8)
    val bitratePattern = Pattern.compile("([0-9]+) *kb/s")

    fun getMetaData(f: File): MetaData {
        val process = Runtime.getRuntime().exec(arrayOf("./ffmpeg", "-hide_banner", "-i", f.canonicalPath))
        process.waitFor()
//        println("output:")
//        println(String(process.inputStream.readAllBytes()))

//        println("error:")
        var message = String(process.errorStream.readAllBytes())
        println(message)

        val start = message.indexOf(':', message.indexOf("Input #0") + 1)
        if (start == -1)
            return MetaData()

        message = message.substring(start + 1).trim()
        if (message.startsWith("Metadata:"))
            message = message.substring(9).trim()

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

        println(metadata)
        val track = metadata["track"]?.let {
            it.substringBefore("/").toInt()
        }
        return MetaData(
            date = metadata["date"],
            album = metadata["album"],
            artist = metadata["artist"],
            title = metadata["title"],
            track = track,
            comment = metadata["comment"],
            duration = metadata["duration"],
            streams = metadata.keys.filter { it.startsWith("stream") }.map { metadata[it]!! },
        )
    }

    fun convert(artist: Artist, album: Album, song: Album.Song, format: AudioFormat, bitrate: Int): File {
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


    data class ConversionStreamResult(
        val data: InputStream,
        val format: AudioFormat,
    )

    fun convertAsStream(artist: Artist, album: Album, song: Album.Song, format: AudioFormat, bitrate: Int): ConversionStreamResult {
        val audio = song.streams.find { it.startsWith("Audio:") }!!
        if (format == AudioFormat.FLAC || format == AudioFormat.WAV)
            if (song.format == format)
                return ConversionStreamResult(
                    songService.copySongStream(artist, album, song),
                    format
                )
            else if (song.format != AudioFormat.FLAC && song.format != AudioFormat.WAV)
                return ConversionStreamResult(
                    songService.copySongStream(artist, album, song),
                    song.format
                )

        val matcher = bitratePattern.matcher(audio)

        val b = if (matcher.find())
            matcher.group(1).toInt()
        else
            320

        if (song.format == format) {
            if (b <= bitrate)
                return ConversionStreamResult(
                    songService.copySongStream(artist, album, song),
                    format
                )
        }

        val songStream = songService.copySongStream(artist, album, song)
        var toBitrate = bitrate
        if (b < bitrate) {
            toBitrate = b
            if ((format == AudioFormat.AAC || format == AudioFormat.OGG) && song.format == AudioFormat.MP3) {
                toBitrate = (toBitrate * 0.9).toInt()
            }
            if ((format == AudioFormat.OPUS) && song.format == AudioFormat.MP3) {
                toBitrate = (toBitrate * 0.8).toInt()
            }
        }
        return ConversionStreamResult(doConvertAsStream(songStream, song.format, format, toBitrate), format)
    }

    fun doConvert(artist: Artist, album: Album, song: Album.Song, format: AudioFormat, bitrate: Int): File {
        val songStream = songService.copySongStream(artist, album, song)
        return doConvert(songStream, song.format, format, bitrate)
    }

    fun doConvert(songStream: InputStream, fromFormat: AudioFormat, toFormat: AudioFormat, bitrate: Int): File {
        val tempOutFile = File.createTempFile("to_", "." + toFormat.extension)
        val stopwatch = Stopwatch("converting to " + toFormat)
        val command =
            arrayOf(
                "./ffmpeg",
                "-nostdin",
                "-y",
                "-hide_banner",
                "-f",
                fromFormat.extension,
                "-i",
                "pipe:0",
                "-map",
                "a",
                "-c:a",
                toFormat.codec,
                "-f",
                toFormat.extension,
                "-ab",
                bitrate.toString() + "k",
                "pipe:1"
            )
        println("executing command: ${Arrays.toString(command)}")
        val process = Runtime.getRuntime().exec(command)

        val latch = CountDownLatch(2)
        threadPool.execute({
            try {
                FileOutputStream(tempOutFile).use { outputStream ->
                    IOTools.copy(process.inputStream, outputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                latch.countDown()
            }
        })


        val messageFuture = threadPool.submit(Callable {
            val sb = StringBuilder()
            try {
                BufferedReader(InputStreamReader(process.errorStream)).use {
                    var line: String?
                    while (true) {
                        line = it.readLine()
                        if (line != null) {
//                            println(line)
                            sb.appendLine(line)
                        } else
                            break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                latch.countDown()
            }
            sb.toString()
        })

        try {
            IOTools.copy(songStream, process.outputStream)
            process.outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }


        val exited = process.waitFor(1, TimeUnit.SECONDS)
        println(stopwatch)
        if (!exited) {
            process.destroy()
        }

        val message = messageFuture.get()
        println("error output: ")
        println(message)
        if (message.contains("Error while decoding") || message.contains("corrupt input")) {
            throw IllegalStateException("Error while decoding")
        }


        if (!latch.await(4, TimeUnit.SECONDS)) {
            throw IllegalStateException("latch wasn't released")
        }
        return tempOutFile
    }

    fun doConvertAsStream(songStream: InputStream, fromFormat: AudioFormat, toFormat: AudioFormat, bitrate: Int): InputStream {
        val stopwatch = Stopwatch("converting to " + toFormat)
        val command =
            arrayOf(
                "./ffmpeg",
                "-nostdin",
                "-y",
                "-hide_banner",
                "-f",
                fromFormat.extension,
                "-i",
                "pipe:0",
                "-map",
                "a",
                "-c:a",
                toFormat.codec,
                "-f",
                toFormat.extension,
                if (toFormat == AudioFormat.WAV || toFormat == AudioFormat.FLAC) "" else "-ab",
                if (toFormat == AudioFormat.WAV || toFormat == AudioFormat.FLAC) "" else (bitrate.toString() + "k"),
                "pipe:1"
            ).filter { it.isNotBlank() }.toTypedArray()
        println("executing command: ${Arrays.toString(command)}")
        val process = Runtime.getRuntime().exec(command)


        val messageFuture = threadPool.submit(Callable {
            val sb = StringBuilder()
            try {
                BufferedReader(InputStreamReader(process.errorStream)).use {
                    var line: String?
                    while (true) {
                        line = it.readLine()
                        if (line != null) {
                            sb.appendLine(line)
                        } else
                            break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            sb.toString()
        })

        threadPool.execute({
            try {
                IOTools.copy(songStream, process.outputStream)
                process.outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val exited = process.waitFor(1, TimeUnit.SECONDS)
            println(stopwatch)
            if (!exited) {
                process.destroy()
            }
            val message = messageFuture.get()
            println("error output: ")
            println(message)
            if (message.contains("Error while decoding") || message.contains("corrupt input")) {
                throw IllegalStateException("Error while decoding")
            }
        })
        return process.inputStream
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
        var date: String? = null,
        var album: String? = null,
        var artist: String? = null,
        var title: String? = null,
        var track: Int? = null,
        var comment: String? = null,
        var duration: String? = null,
        var streams: List<String> = emptyList(),
    )

    enum class AudioFormat(val mimeType: String, val extension: String, val codec: String) {
        MP3("audio/mpeg", "mp3", "libmp3lame"),
        AAC("audio/aac", "adts", "aac"),
        OGG("audio/ogg", "ogg", "libvorbis"),
        OPUS("audio/webm; codecs=\"opus\"", "webm", "libopus"),
        FLAC("audio/x-flac", "flac", "flac"),
        WAV("audio/x-wav", "wav", "wav");

    }
}
