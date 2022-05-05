package com.example.be.service

import com.example.be.db.dto.AlbumDto
import com.example.be.db.dto.ArtistDto
import com.example.be.db.dto.toArtistDto
import com.example.be.db.repository.ArtistRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.wizzardo.tools.io.IOTools
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Service
class SongService(
    private val artistRepository: ArtistRepository,
    private val objectMapper: ObjectMapper,
    private val storageService: StorageService,
) {

    fun getSongData(artistId: Long, albumName: String, trackNumber: Int): ByteArray {
        val artist: ArtistDto = artistRepository.findById(artistId)?.toArtistDto(objectMapper) ?: throw IllegalArgumentException("can't find artist with id: $artistId")
        val album: AlbumDto = artist.albums.find { album -> album.name == albumName } ?: throw IllegalArgumentException("can't find album with name: $albumName")
        val song: AlbumDto.Song = album.songs.find { song -> song.track == trackNumber } ?: throw IllegalArgumentException("can't find song with trackNumber: $trackNumber")
        return getSongData(artist, album, song)
    }

    fun getSongData(artist: ArtistDto, album: AlbumDto, song: AlbumDto.Song): ByteArray {
        return storageService.getData("${artist.path}/${album.path}/${song.path}")
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
        storageService.getStream("${artist.path}/${album.path}/${song.path}").use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                IOTools.copy(inputStream, outputStream)
            }
        }
    }

    fun getAlbumCoverData(artistPath: String, albumPath: String): ByteArray {
        return storageService.getData("${artistPath}/${albumPath}/cover.jpg")
    }
}
