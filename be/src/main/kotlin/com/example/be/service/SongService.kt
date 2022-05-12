package com.example.be.service

import com.example.be.db.dto.AlbumDto
import com.example.be.db.dto.ArtistDto
import com.example.be.db.dto.toArtistDto
import com.example.be.db.repository.ArtistRepository
import com.example.be.misc.TempFileInputStream
import com.fasterxml.jackson.databind.ObjectMapper
import com.wizzardo.tools.io.IOTools
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class SongService(
    private val artistRepository: ArtistRepository,
    private val objectMapper: ObjectMapper,
    private val songsStorageService: SongsStorageService,
) {

    fun getSongData(artist: ArtistDto, album: AlbumDto, song: AlbumDto.Song): TempFileInputStream {
        val tempFile = File.createTempFile("song", song.path)
        var delete = true
        try {
            songsStorageService.getStream(artist, album, song).use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    IOTools.copy(inputStream, outputStream)
                }
            }
            delete = false
            return TempFileInputStream(tempFile)
        } finally {
            if (delete)
                tempFile.delete()
        }
//        return storageService.getData("${artist.path}/${album.path}/${song.path}")
    }

    fun getSong(artistId: Long, albumName: String, trackNumber: Int): AlbumDto.Song {
        val artist: ArtistDto = artistRepository.findById(artistId)?.toArtistDto(objectMapper) ?: throw IllegalArgumentException("can't find artist with id: $artistId")
        val album: AlbumDto = artist.albums.find { album -> album.name == albumName } ?: throw IllegalArgumentException("can't find album with name: $albumName")
        val song: AlbumDto.Song = album.songs.find { song -> song.track == trackNumber } ?: throw IllegalArgumentException("can't find song with trackNumber: $trackNumber")
        return song
    }

    fun getAlbum(artistId: Long, albumName: String): AlbumDto {
        val artist: ArtistDto = artistRepository.findById(artistId)?.toArtistDto(objectMapper) ?: throw IllegalArgumentException("can't find artist with id: $artistId")
        val album: AlbumDto = artist.albums.find { album -> album.name == albumName } ?: throw IllegalArgumentException("can't find album with name: $albumName")
        return album
    }

    fun getAlbum(artist: ArtistDto, albumPath: String): AlbumDto {
        val album: AlbumDto = artist.albums.find { album -> album.path == albumPath } ?: throw IllegalArgumentException("can't find album with name: $albumPath")
        return album
    }

    fun copySongData(artist: ArtistDto, album: AlbumDto, song: AlbumDto.Song, tempFile: File) {
        songsStorageService.getStream(artist, album, song).use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                IOTools.copy(inputStream, outputStream)
            }
        }
    }

    fun getAlbumCoverData(artist: ArtistDto, album: AlbumDto): TempFileInputStream {
        val tempFile = File.createTempFile("cover", ".jpg")
        var delete = true
        songsStorageService.getCoverAsStream(artist, album).use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                IOTools.copy(inputStream, outputStream)
            }
        }

        try {
            val command =
                arrayOf(
                    "mogrify", "-resize", "512x512", tempFile.canonicalPath
                )
            println("executing command: ${Arrays.toString(command)}")
            val process = Runtime.getRuntime().exec(command)
            val exited = process.waitFor(30, TimeUnit.SECONDS)
            if (!exited) {
                process.destroy()
            }

            val output = String(process.inputStream.readAllBytes())
            if (output.isNotEmpty()) {
                println("output:")
                println(output)
            }
            val error = String(process.errorStream.readAllBytes())
            if (error.isNotEmpty()) {
                println("error:")
                println(error)
            }
            delete = false
            return TempFileInputStream(tempFile)
//            return FileTools.bytes(tempFile)
        } finally {
            if (delete)
                tempFile.delete()
        }

//        return storageService.getData("${artistPath}/${albumPath}/cover.jpg")
    }
}
