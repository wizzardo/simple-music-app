package com.example.be.service

import com.example.be.db.dto.AlbumDto
import com.example.be.db.dto.ArtistDto
import com.example.be.db.dto.toArtistDto
import com.example.be.db.repository.ArtistRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.wizzardo.tools.io.IOTools
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Service
class SongService(
    private val artistRepository: ArtistRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${storage.path}")
    private val storagePath: String
) {

    fun getSongData(artistId: Long, albumName: String, trackNumber: Int): ByteArray {
        return getSongData(getSong(artistId, albumName, trackNumber))
    }

    fun getSongData(song: AlbumDto.Song): ByteArray {
        val path: String = song.path
        val songFile = File(storagePath, path)
        return songFile.readBytes()
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
    fun getAlbum(artist: ArtistDto, albumName: String): AlbumDto {
        val album: AlbumDto = artist.albums.find { album -> album.name == albumName } ?: throw IllegalArgumentException("can't find album with name: $albumName")
        return album
    }

    fun copySongData(song: AlbumDto.Song, tempFile: File) {
        FileInputStream(File(storagePath, song.path)).use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                IOTools.copy(inputStream, outputStream)
            }
        }
    }

    fun getAlbumCoverData(album: AlbumDto): ByteArray {
        return File(storagePath, album.coverPath!!).readBytes()
    }
}
