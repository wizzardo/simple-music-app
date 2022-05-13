package com.example.be.service

import com.example.be.db.dto.AlbumDto
import com.example.be.db.dto.ArtistDto
import com.example.be.db.dto.toArtistDto
import com.example.be.db.generated.tables.pojos.Artist
import com.example.be.db.repository.ArtistRepository
import com.example.be.service.FFmpegService.MetaData
import com.fasterxml.jackson.databind.ObjectMapper
import com.wizzardo.tools.image.ImageTools
import com.wizzardo.tools.security.MD5
import org.jooq.JSONB
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.sql.SQLException
import java.time.LocalDateTime

@Service
class UploadService(
    private val songsStorageService: SongsStorageService,
    private val objectMapper: ObjectMapper,
    private val ffmpegService: FFmpegService,
    private val artistRepository: ArtistRepository,
    private val randomIdService: RandomIdService,
    private val artistService: ArtistService,
) {

    companion object {
        private const val UNKNOWN_ARTIST = "unknownArtist"
    }

    fun upload(
        file: MultipartFile,
        artistId: Long?,
        albumId: String?
    ): ResponseEntity<Any> {
        val ext = file.originalFilename?.substringAfterLast('.') ?: "bin"
        val tempFile = File.createTempFile("upload", ".$ext")
        try {
            file.transferTo(tempFile)
            val metaData: MetaData = ffmpegService.getMetaData(tempFile)
            var artist: ArtistDto? = artistId?.let { artistService.getArtist(it) }

            val artistPath = artist?.path
                ?: metaData.artist?.replace("/", " - ")
                ?: throw IllegalArgumentException("artist tag is empty!")

            val albumPath = albumId?.let { artist?.albums?.find { it.id == albumId } }?.path
                ?: metaData.album?.replace("/", " - ")
                ?: throw IllegalArgumentException("album tag is empty!")

            val title = metaData.title
                ?.replace("/", " - ")
                ?: throw IllegalArgumentException("title tag is empty!")
            val track = metaData.track

            val fileName = "$track - $title.${tempFile.extension}"

            var tries = 0;
            while (true) {
                try {
                    tries++;
                    if (artist == null)
                        artist = getOrCreateArtist(metaData, artistPath)
                    val added = addSong(artist, metaData, albumPath, fileName, tempFile)
                    if (added == null)
                        continue

                    songsStorageService.put(artist, added.album, added.song, tempFile)

                    break
                } catch (e: SQLException) {
                    e.printStackTrace()
                    if (tries >= 5)
                        throw IllegalStateException(e)
                }
            }

            val album = artist?.albums?.find { it.path == albumPath }!!
            if (album.coverPath == null && metaData.streams.any { it.startsWith("Video:") }) {
                try {
                    val imageBytes = ffmpegService.extractCoverArt(tempFile)
                    while (true) {
                        try {
                            val artist = artistService.getArtist(artist!!.id)!!
                            val album = artist?.albums?.find { it.path == albumPath }!!
                            if (album.coverPath != null)
                                break

                            tries++;
                            album.coverPath = "cover.jpg"
                            album.coverHash = MD5.create().update(imageBytes).toString()
                            val updated = artistRepository.update(artist.id, artist, objectMapper)
                            if (updated != 1)
                                continue

                            songsStorageService.putCover(artist, album, imageBytes)
                            break
                        } catch (e: SQLException) {
                            e.printStackTrace()
                            if (tries >= 5)
                                throw IllegalStateException(e)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } finally {
            tempFile.delete()
        }

        return ResponseEntity.noContent().build()
    }

    data class AddResult(val artist: ArtistDto, val album: AlbumDto, val song: AlbumDto.Song)

    @Transactional
    fun addSong(artist: ArtistDto, metaData: MetaData, albumPath: String, fileName: String, audio: File): AddResult? {
        val album = artist.albums.find { it.path == albumPath }
            ?: createAlbum(metaData, albumPath).also { artist.albums += it }
        val song = createSong(metaData, fileName)
        album.songs += song

        val updated = artistRepository.update(artist.id, artist, objectMapper)
        val added = updated == 1
        if (added) {
            println("added song ${song.track} ${song.title} to ${album.name}:")
            album.songs.forEach { println("  ${it.track} ${it.title}") }
        } else {
            println("retrying update")
        }

        return if (added) AddResult(artist, album, song) else null
    }

    @Transactional
    fun getOrCreateArtist(metaData: MetaData, path: String): ArtistDto {
        var artist: Artist? = artistRepository.findByName(metaData.artist ?: UNKNOWN_ARTIST)
        if (artist == null) {
            artist = createArtistDto(metaData, path)
            artistRepository.insert(artist)
        }
        return artist.toArtistDto(objectMapper)
    }

    private fun createArtistDto(metaData: MetaData, relativePath: String): Artist = Artist().apply {
        created = LocalDateTime.now()
        updated = LocalDateTime.now()
        name = metaData.artist ?: UNKNOWN_ARTIST
        path = relativePath
        albums = JSONB.valueOf("[]")
    }

    private fun createAlbum(metaData: MetaData, relativePath: String): AlbumDto = AlbumDto().apply {
        id = randomIdService.generateId()
        path = relativePath
        metaData.date?.let { this.date = it }
        metaData.album?.let { this.name = it }
        this.songs = emptyList()
        if (songsStorageService.encryption) {
            coverEncryptionKey = songsStorageService.createEncryptionKey()
        }
    }

    private fun createSong(metaData: MetaData, relativePath: String): AlbumDto.Song = AlbumDto.Song().apply {
        id = randomIdService.generateId()
        metaData.track?.let { this.track = it }
        metaData.title?.let { this.title = it }
        metaData.comment?.let { this.comment = it }
        metaData.duration?.let { this.duration = getMillis(it) }
        metaData.streams.let { this.streams = it }
        format = FFmpegService.AudioFormat.valueOf(relativePath.substringAfterLast('.').uppercase())
        path = relativePath
        if (songsStorageService.encryption) {
            encryptionKey = songsStorageService.createEncryptionKey()
        }
    }

    private fun getMillis(time: String): Int {
        val duration = time.split(",".toRegex(), 2)
        val (hMs, ms) = duration[0].split('.')
        val (h, m, s) = hMs.split(':').map { it.toInt() }

        return (ms.toInt() + ((h * 60 + m) * 60 + s) * 1000)
    }

    fun uploadCoverArt(artist: ArtistDto, album: AlbumDto, file: MultipartFile): ArtistDto {
        val imageBytes = ImageTools.saveJPGtoBytes(ImageTools.read(file.bytes), 90)
        album.coverPath = "cover.jpg"
        album.coverHash = MD5.create().update(imageBytes).toString()
        songsStorageService.putCover(artist, album, imageBytes)
        return artistService.update(artist.id, artist, artist)
    }
}
