package com.example.be.controller

import com.example.be.service.FFmpegService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File

@RestController
class UploadController(
    @Value("\${storage.path}")
    private val storagePath: String,
    val ffmpegService: FFmpegService,
) {

    @PostMapping("/upload")
    fun upload(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<Any> {

        val destination = File("/tmp/", file.originalFilename ?: file.name)
        try {
            file.transferTo(destination)
            val metaData: Map<String, String> = ffmpegService.getMetaData(destination)
            val artist = metaData["artist"]
                ?.replace("/", " - ")
                ?: throw IllegalArgumentException("artist tag is empty!")
            val album = metaData["album"]
                ?.replace("/", " - ")
                ?: throw IllegalArgumentException("album tag is empty!")
            val title = metaData["title"]
                ?.replace("/", " - ")
                ?: throw IllegalArgumentException("title tag is empty!")
            val track = metaData["track"]
                ?.replace("/", " - ")
                ?: throw IllegalArgumentException("track tag is empty!")

            val albumFolder = File(storagePath, "${artist}/${album}")
            albumFolder.mkdirs()
            val target = File(albumFolder, "${track} - ${title}.${destination.extension}")
            if (target.exists())
                throw IllegalArgumentException("file already exists! ${target.canonicalPath}")

            destination.copyTo(target)
        } catch (e: Exception) {
            destination.delete()
        }
        return ResponseEntity.noContent().build()
    }
}
