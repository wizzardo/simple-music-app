package com.example.be.controller

import com.example.be.db.dto.AlbumDto
import com.example.be.db.dto.ArtistDto
import com.example.be.service.ArtistService
import com.example.be.service.FFmpegService
import com.example.be.service.SongService
import com.example.be.service.UploadService
import org.springframework.core.io.InputStreamResource
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

    @DeleteMapping("/artists/{artistId}/{albumId}")
    fun deleteAlbum(
        @PathVariable artistId: Long,
        @PathVariable albumId: String,
    ): ResponseEntity<ArtistDto> {
        val item: ArtistDto = artistService.getArtist(artistId) ?: return ResponseEntity.notFound().build()
        artistService.delete(item, albumId)
        return ResponseEntity.ok(artistService.getArtist(artistId))
    }

    @DeleteMapping("/artists/{artistId}/{albumId}/{songId}")
    fun deleteSong(
        @PathVariable artistId: Long,
        @PathVariable albumId: String,
        @PathVariable songId: String,
    ): ResponseEntity<ArtistDto> {
        val item: ArtistDto = artistService.getArtist(artistId) ?: return ResponseEntity.notFound().build()
        artistService.delete(item, albumId, songId)
        return ResponseEntity.ok(artistService.getArtist(artistId))
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

    @GetMapping(value = ["/artists/{artistId}/{albumName}/{trackNumber}"], produces = ["*/*"])
    fun getSong(
        @PathVariable artistId: Long,
        @PathVariable albumName: String,
        @PathVariable trackNumber: Int,
    ): ResponseEntity<InputStreamResource> {
        val artist: ArtistDto = artistService.getArtist(artistId) ?: return ResponseEntity.notFound().build()
        val album: AlbumDto = artist.albums.find { album -> album.name == albumName } ?: return ResponseEntity.notFound().build()
        val song: AlbumDto.Song = album.songs.find { song -> song.track == trackNumber } ?: return ResponseEntity.notFound().build()
        val data = songService.getSongData(artist, album, song)
        val type = FFmpegService.AudioFormat.values().find { song.path.endsWith(it.name, true) }?.mimeType
        val headers = HttpHeaders().apply {
            this.contentLength = data.length()
            this.contentType = MediaType.parseMediaType(type ?: "application/octet-stream")
        }
        return ResponseEntity(InputStreamResource(data), headers, HttpStatus.OK)
    }

    @GetMapping(value = ["/artists/{artistId}/{albumIdOrName}/{songIdOrTrackNumber}/{format}/{bitrate}"], produces = ["*/*"])
    fun getSongConverted(
        @PathVariable artistId: Long,
        @PathVariable albumIdOrName: String,
        @PathVariable songIdOrTrackNumber: String,
        @PathVariable format: FFmpegService.AudioFormat,
        @PathVariable bitrate: Int,
    ): ResponseEntity<InputStreamResource> {
        val artist: ArtistDto = artistService.getArtist(artistId)
            ?: return ResponseEntity.notFound().build()
        val album: AlbumDto = artist.albums.find { album -> album.name == albumIdOrName || album.id == albumIdOrName || album.path == albumIdOrName }
            ?: return ResponseEntity.notFound().build()

        val trackNumber: Int = songIdOrTrackNumber.toIntOrNull() ?: -1
        val song: AlbumDto.Song = album.songs.find { song -> song.id == songIdOrTrackNumber || song.track == trackNumber }
            ?: return ResponseEntity.notFound().build()

        val data = ffmpegService.convert(artist, album, song, format, bitrate)
        val headers = HttpHeaders().apply {
            this.contentLength = data.length()
            this.contentType = MediaType.parseMediaType(format.mimeType)
        }
        return ResponseEntity(InputStreamResource(data), headers, HttpStatus.OK)
    }

    @GetMapping(value = ["/artists/{artistIdOrPath}/{albumIdOrPath}/cover.jpg"], produces = ["image/jpeg"])
    fun getAlbumCover(
        @PathVariable artistIdOrPath: String,
        @PathVariable albumIdOrPath: String,
        @RequestHeader(value = "If-None-Match", required = false) ifNoneMatch: String?,
    ): ResponseEntity<InputStreamResource> {
        val artist = artistService.getArtistByIdOrPath(artistIdOrPath) ?: return ResponseEntity.notFound().build()
        val album = songService.getAlbum(artist, albumIdOrPath) ?: return ResponseEntity.notFound().build()
        if (album.coverPath == null)
            return ResponseEntity.notFound().build()

        if (ifNoneMatch != null && ifNoneMatch == "\"" + album.coverHash + "\"")
            return ResponseEntity<InputStreamResource>(HttpStatus.NOT_MODIFIED)

        val data = songService.getAlbumCoverData(artist, album)

        val headers = HttpHeaders().apply {
            this.contentLength = data.length()
            this.contentType = MediaType.parseMediaType("image/jpeg")
            this.cacheControl = MAX_AGE_1_YEAR
            this.eTag = "\"" + album.coverHash + "\""
        }
        return ResponseEntity(InputStreamResource(data), headers, HttpStatus.OK)
    }
}
