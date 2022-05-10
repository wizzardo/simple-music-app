package com.example.be.service

import com.example.be.db.dto.AlbumDto
import com.example.be.db.dto.ArtistDto
import com.example.be.db.dto.toArtistDto
import com.example.be.db.repository.ArtistRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArtistService(
    private val artistRepository: ArtistRepository,
    private val songService: SongService,
    private val objectMapper: ObjectMapper,
    private val storageService: StorageService,
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
                storageService.createFolder("${to.path}/${album.path}")

                if (album.coverPath != null) {
                    storageService.move("${from.path}/${album.path}/${album.coverPath}", "${to.path}/${album.path}/${album.coverPath}")
                }
                album.songs.forEach {
                    storageService.move("${from.path}/${album.path}/${it.path}", "${to.path}/${album.path}/${it.path}")
                }
            }
        } else {
            to.albums.forEach { album ->
                val fromAlbum = from.albums.find { it.id == album.id }!!
                if (album.name != fromAlbum.name) {
                    val albumPath = album.name.replace("/", " - ")
                    album.path = albumPath

                    storageService.createFolder("${to.path}/${album.path}")

                    if (fromAlbum.coverPath != null) {
                        storageService.move("${from.path}/${fromAlbum.path}/${album.coverPath}", "${to.path}/${album.path}/${album.coverPath}")
                    }
                    album.songs.forEach {
                        storageService.move("${from.path}/${fromAlbum.path}/${it.path}", "${to.path}/${album.path}/${it.path}")
                    }
                } else {
                    album.songs.forEach { song ->
                        val fromSong = fromAlbum.songs.find { it.id == song.id }!!
                        if (song.title != fromSong.title || song.track != fromSong.track) {
                            song.path = "${song.track} - ${song.title}.${fromSong.path.substringAfterLast(".")}"
                            storageService.move("${from.path}/${fromAlbum.path}/${fromSong.path}", "${to.path}/${album.path}/${song.path}")
                        }
                    }
                }
            }
        }
        artistRepository.update(id, to, objectMapper)
        return getArtist(id)!!
    }

    fun getArtistByPath(artistPath: String): ArtistDto? {
        return artistRepository.findByPath(artistPath)?.toArtistDto(objectMapper)
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
                    val toFile = "${item.path}/${album.path}/${it.path}"
                    val fromFile = "${item.path}/${fromAlbum.path}/${it.path}"

                    if (toFile == fromFile)
                        return@loop

                    storageService.move(fromFile, toFile)
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
        storageService.createFolder("${toArtist.path}/${album.path}")

        if (album.coverPath != null) {
            storageService.move("${fromArtist.path}/${album.path}/${album.coverPath}", "${toArtist.path}/${album.path}/${album.coverPath}")
        }
        album.songs.forEach loop@{
            val toFile = "${toArtist.path}/${album.path}/${it.path}"
            val fromFile = "${fromArtist.path}/${album.path}/${it.path}"

            if (toFile == fromFile)
                return@loop

            storageService.move(fromFile, toFile)
        }

        fromArtist.albums -= album
        toArtist.albums += album
        artistRepository.update(fromArtist.id, fromArtist, objectMapper)
        artistRepository.update(toArtist.id, toArtist, objectMapper)
    }

    fun delete(item: ArtistDto) {
        artistRepository.deleteById(item.id)
        storageService.delete(item.path)
    }

    fun delete(artist: ArtistDto, albumId: String) {
        val album = artist.albums.find { it.id == albumId }!!
        artist.albums -= album
        artistRepository.update(artist.id, artist, objectMapper)
        storageService.delete(artist.path + "/" + album.path)
    }

    fun delete(artist: ArtistDto, albumId: String, songId: String) {
        val album = artist.albums.find { it.id == albumId }!!
        val song = album.songs.find { it.id == songId }!!
        album.songs -= song
        artistRepository.update(artist.id, artist, objectMapper)
        storageService.delete("${artist.path}/${album.path}/${song.path}")
    }
}
