package com.example.be.controller

import com.example.be.db.dto.AlbumDto
import com.example.be.db.dto.ArtistDto
import com.example.be.service.*
import com.wizzardo.tools.cache.Cache
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream

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

    val cache: Cache<ConvertionTask, File> = Cache(-1, {

        val artist: ArtistDto = artistService.getArtist(it.artistId)
            ?: throw IllegalArgumentException("Artist not found. id: ${it.artistId}")
        val album: AlbumDto = artist.albums.find { album -> album.name == it.albumIdOrName || album.id == it.albumIdOrName || album.path == it.albumIdOrName }
            ?: throw IllegalArgumentException("Album not found. id: ${it.albumIdOrName}")

        val trackNumber: Int = it.songIdOrTrackNumber.toIntOrNull() ?: -1
        val song: AlbumDto.Song = album.songs.find { song -> song.id == it.songIdOrTrackNumber || song.track == trackNumber }
            ?: throw IllegalArgumentException("Song not found. id: ${it.albumIdOrName}")

        ffmpegService.convert(artist, album, song, it.format, it.bitrate)
    })

    data class ConvertionTask(
        val artistId: Long,
        val albumIdOrName: String,
        val songIdOrTrackNumber: String,
        val format: FFmpegService.AudioFormat,
        val bitrate: Int
    )

    @GetMapping("/artists")
    fun getArtists(
        @RequestAttribute("permissions") permissions: Set<AuthenticationService.Permission>
    ): List<ArtistDto> = artistService.getArtists()

    @GetMapping("/artists/{id}")
    fun getArtist(
        @PathVariable id: Long,
        @RequestAttribute("permissions") permissions: Set<AuthenticationService.Permission>
    ): ResponseEntity<ArtistDto> {
        val item: ArtistDto = artistService.getArtist(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(item)
    }

    @PostMapping("/artists/{id}")
    fun updateArtist(
        @PathVariable id: Long,
        @RequestBody data: ArtistDto,
        @RequestAttribute("permissions") permissions: Set<AuthenticationService.Permission>
    ): ResponseEntity<ArtistDto> {
        val item: ArtistDto = artistService.getArtist(id) ?: return ResponseEntity.notFound().build()
        val updated = artistService.update(id, item, data)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/artists/{id}")
    fun deleteArtist(
        @PathVariable id: Long,
        @RequestAttribute("permissions") permissions: Set<AuthenticationService.Permission>
    ): ResponseEntity<ArtistDto> {
        val item: ArtistDto = artistService.getArtist(id) ?: return ResponseEntity.notFound().build()
        artistService.delete(item)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/artists/{artistId}/{albumId}")
    fun deleteAlbum(
        @PathVariable artistId: Long,
        @PathVariable albumId: String,
        @RequestAttribute("permissions") permissions: Set<AuthenticationService.Permission>
    ): ResponseEntity<ArtistDto> {
        val item: ArtistDto = artistService.getArtist(artistId) ?: return ResponseEntity.notFound().build()
        artistService.delete(item, albumId)
        return ResponseEntity.ok(artistService.getArtist(artistId))
    }

    data class CreateArtistRequest(var name: String = "")

    @PostMapping("/artists/")
    fun createArtist(
        @RequestBody data: CreateArtistRequest,
        @RequestAttribute("permissions") permissions: Set<AuthenticationService.Permission>
    ): ResponseEntity<ArtistDto> {
        val artist = artistService.getOrCreateArtist(data.name, data.name.replace("/", " - "))
        return ResponseEntity.ok(artist)
    }

    data class CreateAlbumRequest(
        var artistId: Long = 0,
        var name: String = "",
    )

    @PostMapping("/artists/{artistId}/album")
    fun createAlbum(
        @PathVariable artistId: Long,
        @RequestBody data: CreateAlbumRequest,
        @RequestAttribute("permissions") permissions: Set<AuthenticationService.Permission>
    ): ResponseEntity<ArtistDto> {
        val artist: ArtistDto = artistService.getArtist(artistId) ?: return ResponseEntity.notFound().build()
        val albumPath = data.name.replace("/", " - ")
        val album = artist.albums.find { it.path == albumPath }
        if (album == null) {
            artistService.createAlbum(data.name, albumPath).also { artist.albums += it }
            return ResponseEntity.ok(artistService.update(artist))
        } else
            return ResponseEntity.accepted().body(artist)
    }

    @DeleteMapping("/artists/{artistId}/{albumId}/{songId}")
    fun deleteSong(
        @PathVariable artistId: Long,
        @PathVariable albumId: String,
        @PathVariable songId: String,
        @RequestAttribute("permissions") permissions: Set<AuthenticationService.Permission>
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
        @RequestAttribute("permissions") permissions: Set<AuthenticationService.Permission>
    ): ResponseEntity<ArtistDto> {
        val item: ArtistDto = artistService.getArtist(artistId) ?: return ResponseEntity.notFound().build()
        val updated = artistService.mergeAlbums(item, intoAlbumId, data.albums)
        data.albums.forEach {
            artistService.delete(updated, it)
        }
        return ResponseEntity.ok(artistService.getArtist(artistId))
    }

    @PostMapping("/artists/{artistId}/{albumId}/{toArtistId}")
    fun moveAlbum(
        @PathVariable artistId: Long,
        @PathVariable albumId: String,
        @PathVariable toArtistId: Long,
        @RequestAttribute("permissions") permissions: Set<AuthenticationService.Permission>
    ): ResponseEntity<Unit> {
        val fromArtist: ArtistDto = artistService.getArtist(artistId) ?: return ResponseEntity.notFound().build()
        val toArtist: ArtistDto = artistService.getArtist(toArtistId) ?: return ResponseEntity.notFound().build()
        val album: AlbumDto = fromArtist.albums.find({ it.id == albumId }) ?: return ResponseEntity.notFound().build()
        artistService.move(fromArtist, toArtist, album)
        if (fromArtist.albums.size == 1) {
            artistService.delete(fromArtist)
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/artists/{artistId}/{albumId}/cover")
    fun uploadCoverArt(
        @PathVariable artistId: Long,
        @PathVariable albumId: String,
        @RequestParam("file") file: MultipartFile,
        @RequestAttribute("permissions") permissions: Set<AuthenticationService.Permission>
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
        @RequestAttribute("permissions") permissions: Set<AuthenticationService.Permission>
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
        return ResponseEntity(InputStreamResource(FileInputStream(data)), headers, HttpStatus.OK)
    }

    @GetMapping(value = ["/artists/{artistId}/{albumIdOrName}/{songIdOrTrackNumber}/{format}/{bitrate}"], produces = ["*/*"])
    fun getSongConverted(
        @PathVariable artistId: Long,
        @PathVariable albumIdOrName: String,
        @PathVariable songIdOrTrackNumber: String,
        @PathVariable format: FFmpegService.AudioFormat,
        @PathVariable bitrate: Int,
        @RequestAttribute("permissions") permissions: Set<AuthenticationService.Permission>
    ): ResponseEntity<InputStreamResource> {
        val artist: ArtistDto = artistService.getArtist(artistId)
            ?: return ResponseEntity.notFound().build()
        val album: AlbumDto = artist.albums.find { album -> album.name == albumIdOrName || album.id == albumIdOrName || album.path == albumIdOrName }
            ?: return ResponseEntity.notFound().build()

        val trackNumber: Int = songIdOrTrackNumber.toIntOrNull() ?: -1
        val song: AlbumDto.Song = album.songs.find { song -> song.id == songIdOrTrackNumber || song.track == trackNumber }
            ?: return ResponseEntity.notFound().build()

        val data = cache.get(ConvertionTask(artistId, albumIdOrName, songIdOrTrackNumber, format, bitrate))
        val headers = HttpHeaders().apply {
            this.contentLength = data.length()
            this.contentType = MediaType.parseMediaType(format.mimeType)
        }
        return ResponseEntity(InputStreamResource(FileInputStream(data)), headers, HttpStatus.OK)
    }

    @GetMapping(value = ["/artists/{artistIdOrPath}/{albumIdOrPath}/cover.jpg"], produces = ["image/jpeg"])
    fun getAlbumCover(
        @PathVariable artistIdOrPath: String,
        @PathVariable albumIdOrPath: String,
        @RequestHeader(value = "If-None-Match", required = false) ifNoneMatch: String?,
        @RequestAttribute("permissions") permissions: Set<AuthenticationService.Permission>
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
