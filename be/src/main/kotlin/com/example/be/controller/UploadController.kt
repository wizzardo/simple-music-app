package com.example.be.controller

import com.example.be.db.dto.ArtistDto
import com.example.be.service.ArtistService
import com.example.be.service.UploadService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class UploadController(
    private val uploadService: UploadService,
    private val artistService: ArtistService,
) {

    @PostMapping("/upload")
    fun upload(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<Any> = uploadService.upload(file)

    @GetMapping("/artists")
    fun getArtists(): List<ArtistDto> = artistService.getArtists()
}
