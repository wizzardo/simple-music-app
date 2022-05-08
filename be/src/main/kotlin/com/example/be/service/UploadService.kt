package com.example.be.service

import com.example.be.db.dto.AlbumDto
import com.example.be.db.dto.ArtistDto
import com.example.be.db.generated.tables.pojos.Artist
import com.example.be.db.repository.ArtistRepository
import com.example.be.service.FFmpegService.MetaData
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.wizzardo.tools.image.ImageTools
import com.wizzardo.tools.io.FileTools
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
    private val storageService: StorageService,
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
        file: MultipartFile
    ): ResponseEntity<Any> {
        val tempFile = File("/tmp/", file.originalFilename ?: file.name)
        try {
            file.transferTo(tempFile)
            val metaData: MetaData = ffmpegService.getMetaData(tempFile)

            val artistPath = metaData.artist
                ?.replace("/", " - ")
                ?: throw IllegalArgumentException("artist tag is empty!")
            val album = metaData.album
                ?.replace("/", " - ")
                ?: throw IllegalArgumentException("album tag is empty!")
            val title = metaData.title
                ?.replace("/", " - ")
                ?: throw IllegalArgumentException("title tag is empty!")
            val track = metaData.track

            val fileName = "$track - $title.${tempFile.extension}"
            storageService.createFolder("$artistPath/$album")
            storageService.put("$artistPath/$album/${fileName}", tempFile)

            var tries = 0;
            while (true) {
                try {
                    tries++;
                    val artist = getOrCreateArtist(metaData, artistPath)
                    if (!addSong(artist, metaData, album, fileName, tempFile))
                        continue
                    break
                } catch (e: SQLException) {
                    e.printStackTrace()
                    if (tries >= 5)
                        throw IllegalStateException(e)
                }
            }
        } finally {
            tempFile.delete()
        }

        return ResponseEntity.noContent().build()
    }

    @Transactional
    fun addSong(artist: Artist, metaData: MetaData, albumPath: String, fileName: String, audio: File): Boolean {
        val albums: MutableList<AlbumDto> = objectMapper.readValue(artist.albums?.data(), object : TypeReference<ArrayList<AlbumDto>>() {})
        val album = albums.find { it.name == metaData.album }
            ?: createAlbum(metaData, albumPath).also { albums.add(it) }
        val song = createSong(metaData, fileName)
        album.songs += song

        if (album.coverPath == null && metaData.streams.any { it.startsWith("Video:") }) {
            try {
                val imageBytes = ffmpegService.extractCoverArt(audio)
                storageService.put("${artist.path}/${album.path}/cover.jpg", imageBytes)
                album.coverPath = "cover.jpg"
                album.coverHash = MD5.create().update(imageBytes).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val updated = artistRepository.updateAlbums(artist, albums, objectMapper)
        if (updated == 1) {
            println("added song ${song.track} ${song.title} to ${album.name}:")
            album.songs.forEach { println("  ${it.track} ${it.title}") }
        }else{
            println("retrying update")
        }
        return updated == 1
    }

    @Transactional
    fun getOrCreateArtist(metaData: MetaData, path: String): Artist {
        var artist: Artist? = artistRepository.findByName(metaData.artist ?: UNKNOWN_ARTIST)
        if (artist == null) {
            artist = createArtistDto(metaData, path)
            artistRepository.insert(artist)
        }
        return artist
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
    }

    private fun createSong(metaData: MetaData, relativePath: String): AlbumDto.Song = AlbumDto.Song().apply {
        id = randomIdService.generateId()
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

    fun uploadCoverArt(item: ArtistDto, album: AlbumDto, file: MultipartFile): ArtistDto {
        val cover = "cover.jpg"
        val imageBytes = ImageTools.saveJPGtoBytes(ImageTools.read(file.bytes), 90)
        storageService.put("${item.path}/${album.path}/cover.jpg", imageBytes)
        album.coverPath = cover
        album.coverHash = MD5.create().update(imageBytes).toString()
        return artistService.update(item.id, item, item)
    }
}
