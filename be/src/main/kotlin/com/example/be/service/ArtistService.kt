package com.example.be.service

import com.example.be.db.dto.AlbumDto
import com.example.be.db.dto.ArtistDto
import com.example.be.db.dto.toArtistDto
import com.example.be.db.generated.tables.pojos.Artist
import com.example.be.db.repository.ArtistRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.JSONB
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ArtistService(
    private val artistRepository: ArtistRepository,
    private val objectMapper: ObjectMapper,
    private val songsStorageService: SongsStorageService,
    private val randomIdService: RandomIdService,
) {

    fun getArtists(): List<ArtistDto> {
        return artistRepository.findAll().map { artist -> artist.toArtistDto(objectMapper) }
    }

    fun getArtist(id: Long): ArtistDto? {
        return artistRepository.findById(id)?.toArtistDto(objectMapper)
    }

    fun getArtistByName(name: String): ArtistDto? {
        return artistRepository.findByName(name)?.toArtistDto(objectMapper)
    }

    fun update(id: Long, from: ArtistDto, to: ArtistDto): ArtistDto {
        if (from.name != to.name) {
            val artistPath = to.name.replace("/", " - ")
            to.path = artistPath;
            to.albums.forEach { album ->
                songsStorageService.createFolder(to, album)

                if (album.coverPath != null) {
                    songsStorageService.moveCover(from, to, album)
                }
                album.songs.forEach {
                    songsStorageService.move(from, to, album, it)
                }
            }
        } else {
            to.albums.forEach { album ->
                val fromAlbum = from.albums.find { it.id == album.id }!!
                if (album.name != fromAlbum.name) {
                    val albumPath = album.name.replace("/", " - ")
                    album.path = albumPath

                    songsStorageService.createFolder(to, album)

                    if (fromAlbum.coverPath != null) {
                        songsStorageService.moveCover(from, fromAlbum, to, album)
                    }
                    album.songs.forEach {
                        songsStorageService.move(from, fromAlbum, to, album, it)
                    }
                } else {
                    album.songs.forEach { song ->
                        val fromSong = fromAlbum.songs.find { it.id == song.id }!!
                        if (song.title != fromSong.title || song.track != fromSong.track) {
                            song.path = "${song.track} - ${song.title}.${fromSong.path.substringAfterLast(".")}"
                            songsStorageService.move(from, fromAlbum, fromSong, to, album, song)
                        }
                    }
                }
            }
        }
        artistRepository.update(id, to, objectMapper)
        return getArtist(id)!!
    }

    fun update(item: ArtistDto): ArtistDto {
        artistRepository.update(item.id, item, objectMapper)
        return getArtist(item.id)!!
    }

    fun getArtistByPath(artistPath: String): ArtistDto? {
        return artistRepository.findByPath(artistPath)?.toArtistDto(objectMapper)
    }

    fun getArtistByIdOrPath(idOrPath: String): ArtistDto? {
        return artistRepository.findByIdOrPath(idOrPath)?.toArtistDto(objectMapper)
    }

    fun mergeAlbums(item: ArtistDto, intoAlbumId: String, albums: List<String>): ArtistDto {
        val album = item.albums.find { it.id == intoAlbumId } ?: throw IllegalArgumentException("Cannot find album with id ${intoAlbumId}")
        val songs: MutableList<AlbumDto.Song> = ArrayList(album.songs)
        album.songs = songs

        val mergedAlbums: MutableList<AlbumDto> = ArrayList(item.albums.size - albums.size)
        item.albums.forEach { fromAlbum ->
            if (fromAlbum.id in albums) {
                fromAlbum.songs.forEach loop@{
                    songs.add(it)
                    songsStorageService.move(item, fromAlbum, it, item, album, it)
                }
            } else {
                mergedAlbums.add(fromAlbum)
            }
        }

        item.albums = mergedAlbums
        artistRepository.update(item.id, item, objectMapper)
        return getArtist(item.id)!!
    }

    @Transactional
    fun move(fromArtist: ArtistDto, toArtist: ArtistDto, album: AlbumDto) {
        songsStorageService.createFolder(toArtist, album)

        if (album.coverPath != null) {
            songsStorageService.moveCover(fromArtist, album, toArtist, album)
        }
        album.songs.forEach loop@{
            songsStorageService.move(fromArtist, album, it, toArtist, album, it)
        }

        fromArtist.albums -= album
        toArtist.albums += album
        artistRepository.update(fromArtist.id, fromArtist, objectMapper)
        artistRepository.update(toArtist.id, toArtist, objectMapper)
    }

    fun delete(item: ArtistDto) {
        artistRepository.deleteById(item.id)
        songsStorageService.delete(item)
    }

    fun delete(artist: ArtistDto, albumId: String) {
        val album = artist.albums.find { it.id == albumId }!!
        artist.albums -= album
        artistRepository.update(artist.id, artist, objectMapper)
        songsStorageService.delete(artist, album)
    }

    fun delete(artist: ArtistDto, albumId: String, songId: String) {
        val album = artist.albums.find { it.id == albumId }!!
        val song = album.songs.find { it.id == songId }!!
        album.songs -= song
        artistRepository.update(artist.id, artist, objectMapper)
        songsStorageService.delete(artist, album, song)
    }

    @Transactional
    fun getOrCreateArtist(name: String, path: String): ArtistDto {
        var artist: Artist? = artistRepository.findByName(name)
        if (artist == null) {
            artist = createArtistDto(name, path)
            artistRepository.insert(artist)
        }
        return artist.toArtistDto(objectMapper)
    }

    private fun createArtistDto(name: String, path: String): Artist = Artist().apply {
        created = LocalDateTime.now()
        updated = LocalDateTime.now()
        this.name = name
        this.path = path
        albums = JSONB.valueOf("[]")
    }

    fun createAlbum(name: String, path: String): AlbumDto = AlbumDto().apply {
        id = randomIdService.generateId()
        this.path = path
        this.name = name
        this.songs = emptyList()
        if (songsStorageService.encryption) {
            coverEncryptionKey = songsStorageService.createEncryptionKey()
        }
    }
}
