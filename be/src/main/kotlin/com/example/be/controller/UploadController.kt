package com.example.be.controller

import com.example.be.db.dto.ArtistDto
import com.example.be.service.ArtistService
import com.example.be.service.SongService
import com.example.be.service.UploadService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
class UploadController(
    private val uploadService: UploadService,
    private val artistService: ArtistService,
    private val songService: SongService,
) {

    @PostMapping("/upload")
    fun upload(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<Any> = uploadService.upload(file)

    @GetMapping("/artists")
    fun getArtists(): List<ArtistDto> = artistService.getArtists()

    @GetMapping("/artists/{artistId}/{albumName}/{trackNumber}")
    fun getSong(
        @PathVariable artistId: Long,
        @PathVariable albumName: String,
        @PathVariable trackNumber: Int,
    ): ResponseEntity<ByteArray> {
        val songData = songService.getSongData(artistId, albumName, trackNumber)
        val headers = HttpHeaders().apply { this.contentType = MediaType.parseMediaType("audio/mpeg") }
        return ResponseEntity(songData, headers, HttpStatus.OK)
    }
}
