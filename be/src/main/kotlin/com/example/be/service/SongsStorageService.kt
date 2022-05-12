package com.example.be.service

import com.example.be.db.dto.AlbumDto
import com.example.be.db.dto.ArtistDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.io.InputStream

@Component
class SongsStorageService(
    @Value("\${storage.useIdAsName:false}")
    private val useIdAsName: Boolean,
    val storageService: StorageService,
) {

    private fun ArtistDto.path(): String {
        if (useIdAsName)
            return this.id.toString()
        return this.path
    }

    private fun AlbumDto.path(): String {
        if (useIdAsName)
            return this.id
        return this.path
    }

    private fun AlbumDto.coverPath(): String? {
        if (useIdAsName)
            return this.id
        return this.coverPath
    }

    private fun AlbumDto.Song.path(): String {
        if (useIdAsName)
            return this.id
        return this.path
    }

    fun createFolder(artist: ArtistDto, album: AlbumDto) = storageService.createFolder("${artist.path()}/${album.path()}")

    fun createFolder(artist: ArtistDto) = storageService.createFolder(artist.path())

    fun move(from: ArtistDto, to: ArtistDto, album: AlbumDto, song: AlbumDto.Song) {
        storageService.move("${from.path()}/${album.path()}/${song.path()}", "${to.path()}/${album.path()}/${song.path()}")
    }

    fun move(from: ArtistDto, albumFrom: AlbumDto, to: ArtistDto, albumTo: AlbumDto, song: AlbumDto.Song) {
        storageService.move("${from.path()}/${albumFrom.path()}/${song.path()}", "${to.path()}/${albumTo.path()}/${song.path()}")
    }

    fun move(from: ArtistDto, albumFrom: AlbumDto, songFrom: AlbumDto.Song, to: ArtistDto, albumTo: AlbumDto, songTo: AlbumDto.Song) {
        storageService.move("${from.path()}/${albumFrom.path()}/${songFrom.path()}", "${to.path()}/${albumTo.path()}/${songTo.path()}")
    }

    fun moveCover(from: ArtistDto, to: ArtistDto, album: AlbumDto) {
        storageService.move("${from.path()}/${album.path()}/${album.coverPath()}", "${to.path()}/${album.path()}/${album.coverPath()}")
    }

    fun moveCover(from: ArtistDto, albumFrom: AlbumDto, to: ArtistDto, albumTo: AlbumDto) {
        storageService.move("${from.path()}/${albumFrom.path()}/${albumFrom.coverPath()}", "${to.path()}/${albumTo.path()}/${albumTo.coverPath()}")
    }

    fun delete(artist: ArtistDto) {
        storageService.delete(artist.path())
    }

    fun delete(artist: ArtistDto, album: AlbumDto) {
        storageService.delete("${artist.path()}/${album.path()}")
    }

    fun delete(artist: ArtistDto, album: AlbumDto, song: AlbumDto.Song) {
        storageService.delete("${artist.path()}/${album.path()}/${song.path()}")
    }

    fun getStream(artist: ArtistDto, album: AlbumDto, song: AlbumDto.Song): InputStream {
        return storageService.getStream("${artist.path()}/${album.path()}/${song.path()}")
    }

    fun getCoverAsStream(artist: ArtistDto, album: AlbumDto): InputStream {
        return storageService.getStream("${artist.path()}/${album.path()}/${album.coverPath()}")
    }

    fun put(artist: ArtistDto, album: AlbumDto, song: AlbumDto.Song, file: File) {
        storageService.put("${artist.path()}/${album.path()}/${song.path()}", file)
    }

    fun putCover(artist: ArtistDto, album: AlbumDto, bytes: ByteArray) {
        storageService.put("${artist.path()}/${album.path()}/${album.coverPath()}", bytes)
    }
}