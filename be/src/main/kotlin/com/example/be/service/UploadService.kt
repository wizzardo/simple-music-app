package com.example.be.service

import com.example.be.db.dto.AlbumDto
import com.example.be.db.generated.tables.pojos.Artist
import com.example.be.db.repository.ArtistRepository
import com.example.be.service.FFmpegService.MetaData
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.wizzardo.tools.security.MD5
import org.jooq.JSONB
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.sql.SQLException
import java.time.LocalDateTime

@Service
class UploadService(
    @Value("\${storage.path}")
    private val storagePath: String,
    private val objectMapper: ObjectMapper,
    private val ffmpegService: FFmpegService,
    private val artistRepository: ArtistRepository,
) {

    fun upload(
        file: MultipartFile
    ): ResponseEntity<Any> {
        val tempFile = File("/tmp/", file.originalFilename ?: file.name)
        try {
            file.transferTo(tempFile)
            val metaData: MetaData = ffmpegService.getMetaData(tempFile)

            val pathArtist = metaData.artist
                ?.replace("/", " - ")
                ?: throw IllegalArgumentException("artist tag is empty!")
            val album = metaData.album
                ?.replace("/", " - ")
                ?: throw IllegalArgumentException("album tag is empty!")
            val title = metaData.title
                ?.replace("/", " - ")
                ?: throw IllegalArgumentException("title tag is empty!")
            val track = metaData.track

            val albumPath = "$pathArtist/$album"
            val fileName = "$track - $title.${tempFile.extension}"
            val albumFolder = File(storagePath, albumPath)
            albumFolder.mkdirs()
            val target = File(albumFolder, fileName)
            if (target.exists())
                throw IllegalArgumentException("file already exists! ${target.canonicalPath}")

            tempFile.copyTo(target)

            while (true) {
                try {
                    addSong(metaData, "$albumPath/$fileName", target)
                    break
                } catch (e: SQLException) {
                    e.printStackTrace()
                }
            }
        } finally {
            tempFile.delete()
        }

        return ResponseEntity.noContent().build()
    }

    @Transactional
    fun addSong(metaData: MetaData, relativePath: String, audio: File) {
        var artist: Artist? = artistRepository.findByName(metaData.artist ?: UNKNOWN_ARTIST)
        if (artist == null) {
            artist = createArtistDto(metaData)
            artistRepository.insert(artist)
        }

        val albums: MutableList<AlbumDto> = objectMapper.readValue(artist.albums?.data(), object : TypeReference<ArrayList<AlbumDto>>() {})
        val album = albums.find { it.name == metaData.album }
            ?: createAlbum(metaData).also { albums.add(it) }
        album.songs += createSong(metaData, relativePath)

        if (album.coverPath == null && metaData.streams.any { it.startsWith("Video:") }) {
            val cover = relativePath.substringBeforeLast("/") + "/cover.jpg"
            try {
                val coverFile = File(storagePath, cover)
                ffmpegService.extractCoverArt(audio, coverFile)
                album.coverPath = cover
                album.coverHash = MD5.create().update(coverFile.readBytes()).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        artistRepository.updateAlbums(artist, albums, objectMapper)
    }

    private fun createArtistDto(metaData: MetaData): Artist = Artist().apply {
        created = LocalDateTime.now()
        updated = LocalDateTime.now()
        name = metaData.artist ?: UNKNOWN_ARTIST
        albums = JSONB.valueOf("[]")
    }

    private fun createAlbum(metaData: MetaData): AlbumDto = AlbumDto().apply {
        metaData.date?.let { this.date = it }
        metaData.album?.let { this.name = it }
        this.songs = emptyList()
    }

    private fun createSong(metaData: MetaData, relativePath: String): AlbumDto.Song = AlbumDto.Song().apply {
        metaData.track?.let { this.track = it }
        metaData.title?.let { this.title = it }
        metaData.comment?.let { this.comment = it }
        metaData.duration?.let { this.duration = getMillis(it) }
        metaData.streams.let { this.streams = it }
        path = relativePath
    }

    private fun getMillis(time: String): Int {
        val duration = time.split(",".toRegex(), 2)
        val (hMs, ms) = duration[0].split('.')
        val (h, m, s) = hMs.split(':').map { it.toInt() }

        return (ms.toInt() + ((h * 60 + m) * 60 + s) * 1000)
    }

    companion object {
        private const val UNKNOWN_ARTIST = "unknownArtist"
    }
}
