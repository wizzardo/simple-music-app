package com.example.be.controller

import com.example.be.db.dto.AlbumDto
import com.example.be.db.dto.ArtistDto
import com.example.be.service.ArtistService
import com.example.be.service.FFmpegService
import com.example.be.service.SongService
import com.example.be.service.UploadService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
class ArtistController(
    private val artistService: ArtistService,
    private val songService: SongService,
    private val ffmpegService: FFmpegService,
    private val uploadService: UploadService,
) {
    companion object {
        val MAX_AGE_1_YEAR = "max-age=31556926"
    }

    @GetMapping("/artists")
    fun getArtists(): List<ArtistDto> = artistService.getArtists()

    @GetMapping("/artists/{id}")
    fun getArtist(
        @PathVariable id: Long
    ): ResponseEntity<ArtistDto> {
        val item: ArtistDto = artistService.getArtist(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(item)
    }

    @PostMapping("/artists/{id}")
    fun updateArtist(
        @PathVariable id: Long,
        @RequestBody data: ArtistDto,
    ): ResponseEntity<ArtistDto> {
        val item: ArtistDto = artistService.getArtist(id) ?: return ResponseEntity.notFound().build()
        val updated = artistService.update(id, item, data)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/artists/{id}")
    fun deleteArtist(
        @PathVariable id: Long,
    ): ResponseEntity<ArtistDto> {
        val item: ArtistDto = artistService.getArtist(id) ?: return ResponseEntity.notFound().build()
        artistService.delete(item)
        return ResponseEntity.noContent().build()
    }

    data class MergeAlbumsRequest(
        val artistId: Long,
        val intoAlbumId: String,
        val albums: List<String>,
    )

    @PostMapping("/artists/{artistId}/{intoAlbumId}")
    fun mergeAlbums(
        @PathVariable artistId: Long,
        @PathVariable intoAlbumId: String,
        @RequestBody data: MergeAlbumsRequest,
    ): ResponseEntity<ArtistDto> {
        val item: ArtistDto = artistService.getArtist(artistId) ?: return ResponseEntity.notFound().build()
        val updated = artistService.mergeAlbums(item, intoAlbumId, data.albums)
        return ResponseEntity.ok(updated)
    }

    @PostMapping("/artists/{artistId}/{albumId}/{toArtistId}")
    fun moveAlbum(
        @PathVariable artistId: Long,
        @PathVariable albumId: String,
        @PathVariable toArtistId: Long,
    ): ResponseEntity<Unit> {
        val fromArtist: ArtistDto = artistService.getArtist(artistId) ?: return ResponseEntity.notFound().build()
        val toArtist: ArtistDto = artistService.getArtist(toArtistId) ?: return ResponseEntity.notFound().build()
        val album: AlbumDto = fromArtist.albums.find({ it.id == albumId }) ?: return ResponseEntity.notFound().build()
        artistService.move(fromArtist, toArtist, album)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/artists/{artistId}/{albumId}/cover")
    fun uploadCoverArt(
        @PathVariable artistId: Long,
        @PathVariable albumId: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ArtistDto> {
        val item: ArtistDto = artistService.getArtist(artistId) ?: return ResponseEntity.notFound().build()
        val album = item.albums.find { it.id == albumId } ?: return ResponseEntity.notFound().build()

        val updated = uploadService.uploadCoverArt(item, album, file)

        return ResponseEntity.ok(updated)
    }

    @GetMapping("/artists/{artistId}/{albumName}/{trackNumber}")
    fun getSong(
        @PathVariable artistId: Long,
        @PathVariable albumName: String,
        @PathVariable trackNumber: Int,
    ): ResponseEntity<ByteArray> {
        val song = songService.getSong(artistId, albumName, trackNumber)
        val songData = songService.getSongData(song)
        val type = AudioFormat.values().find { song.path.endsWith(it.name, true) }?.mimeType
        val headers = HttpHeaders().apply { this.contentType = MediaType.parseMediaType(type ?: "application/octet-stream") }
        return ResponseEntity(songData, headers, HttpStatus.OK)
    }

    enum class AudioFormat(val mimeType: String) {
        MP3("audio/mpeg"),
        AAC("audio/aac"),
        OGG("audio/ogg"),
        OPUS("audio/opus"),
        FLAC("audio/x-flac");
    }

    @GetMapping("/artists/{artistId}/{albumName}/{trackNumber}/{format}/{bitrate}")
    fun getSongConverted(
        @PathVariable artistId: Long,
        @PathVariable albumName: String,
        @PathVariable trackNumber: Int,
        @PathVariable format: AudioFormat,
        @PathVariable bitrate: Int,
    ): ResponseEntity<ByteArray> {
        val song = songService.getSong(artistId, albumName, trackNumber)
        val data = ffmpegService.convert(song, format, bitrate)
        val headers = HttpHeaders().apply { this.contentType = MediaType.parseMediaType(format.mimeType) }
        return ResponseEntity(data, headers, HttpStatus.OK)
    }

    @GetMapping("/artists/{artistPath}/{albumPath}/cover.jpg")
    fun getAlbumCover(
        @PathVariable artistPath: String,
        @PathVariable albumPath: String,
        @RequestHeader(value = "If-None-Match", required = false) ifNoneMatch: String?,
    ): ResponseEntity<ByteArray> {
        val artist = artistService.getArtistByPath(artistPath) ?: return ResponseEntity.notFound().build()
        val album = songService.getAlbum(artist, albumPath)
        if (album.coverPath == null)
            return ResponseEntity.notFound().build()

        if (ifNoneMatch != null && ifNoneMatch == "\"" + album.coverHash + "\"")
            return ResponseEntity<ByteArray>(HttpStatus.NOT_MODIFIED)

        val data = songService.getAlbumCoverData(artistPath, albumPath)

        val headers = HttpHeaders().apply {
            this.contentType = MediaType.parseMediaType("image/jpeg")
            this.cacheControl = MAX_AGE_1_YEAR
            this.eTag = "\"" + album.coverHash + "\""
        }
        return ResponseEntity(data, headers, HttpStatus.OK)
    }
}
