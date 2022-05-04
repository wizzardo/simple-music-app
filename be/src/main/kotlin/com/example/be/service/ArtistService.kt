package com.example.be.service

import com.example.be.db.dto.AlbumDto
import com.example.be.db.dto.ArtistDto
import com.example.be.db.dto.toArtistDto
import com.example.be.db.repository.ArtistRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.wizzardo.tools.io.FileTools
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File

@Service
class ArtistService(
    private val artistRepository: ArtistRepository,
    private val songService: SongService,
    private val objectMapper: ObjectMapper,

    @Value("\${storage.path}")
    private val storagePath: String,
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
                val albumPath = album.name.replace("/", " - ")
                val artistAlbumPath = "$artistPath/$albumPath"
                album.path = albumPath

                val albumFolder = File(storagePath, artistAlbumPath)
                albumFolder.mkdirs()

                if (album.coverPath != null) {
                    val coverNewPath = "$artistAlbumPath/cover.jpg"
                    val coverNewFile = File(storagePath, coverNewPath)
                    if (!coverNewFile.exists())
                        if (!File(album.coverPath!!).renameTo(coverNewFile))
                            throw IllegalStateException("file rename failed! ${album.coverPath} -> ${coverNewFile.canonicalPath}")

                    album.coverPath = "cover.jpg"
                }
                album.songs.forEach {
                    val target = File(albumFolder, it.path)
                    //todo: create a list of operations before executing them and check for conflicts
                    if (target.exists())
                        throw IllegalArgumentException("file already exists! ${target.canonicalPath}")

                    //todo: probably better to copy file and remove old one afterwards
                    if (!File(storagePath, "${from.path}/${album.path}/${it.path}").renameTo(target))
                        throw IllegalStateException("file rename failed! ${it.path} -> ${target.canonicalPath}")
                }
            }
        } else {
            to.albums.forEach { album ->
                val fromAlbum = from.albums.find { it.id == album.id }!!
                if (album.name != fromAlbum.name) {
                    val albumPath = album.name.replace("/", " - ")
                    val artistAlbumPath = "${from.path}/$albumPath"
                    val albumFolder = File(storagePath, artistAlbumPath)
                    albumFolder.mkdirs()

                    album.path = albumPath

                    if (fromAlbum.coverPath != null) {
                        val coverNewPath = "$artistAlbumPath/cover.jpg"
                        val coverNewFile = File(storagePath, coverNewPath)
                        if (!coverNewFile.exists())
                            if (!File("${from.path}/${fromAlbum.path}/${fromAlbum.coverPath}").renameTo(coverNewFile))
                                throw IllegalStateException("file rename failed! ${album.coverPath} -> ${coverNewFile.canonicalPath}")

                        album.coverPath = "cover.jpg"
                    }
                    album.songs.forEach {
                        val target = File(albumFolder, it.path)

                        //todo: create a list of operations before executing them and check for conflicts
                        if (target.exists())
                            throw IllegalArgumentException("file already exists! ${target.canonicalPath}")

                        //todo: probably better to copy file and remove old one afterwards
                        if (!File(storagePath, "${from.path}/${fromAlbum.path}/${it.path}").renameTo(target))
                            throw IllegalStateException("file rename failed! ${it.path} -> ${target.canonicalPath}")
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
        val artistAlbumPath = "${item.path}/${album.path}"
        val albumFolder = File(storagePath, artistAlbumPath)

        val mergedAlbums: MutableList<AlbumDto> = ArrayList(item.albums.size - albums.size)
        item.albums.forEach { fromAlbum ->
            if (fromAlbum.id in albums) {
                fromAlbum.songs.forEach loop@{
                    songs.add(it)
                    val toFile = File(albumFolder, it.path)
                    val fromFile = File(storagePath, "${item.path}/${fromAlbum.path}/${it.path}")

                    if (toFile.canonicalPath == fromFile.canonicalPath)
                        return@loop

                    //todo: create a list of operations before executing them and check for conflicts
                    if (toFile.exists())
                        throw IllegalArgumentException("file already exists! ${toFile.canonicalPath}")

                    //todo: probably better to copy file and remove old one afterwards
                    if (!fromFile.renameTo(toFile))
                        throw IllegalStateException("file rename failed! ${it.path} -> ${toFile.canonicalPath}")
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
        val albumPath = album.name.replace("/", " - ")
        val artistAlbumPath = "${toArtist.path}/$albumPath"
        album.path = albumPath

        val albumFolder = File(storagePath, artistAlbumPath)
        albumFolder.mkdirs()

        if (album.coverPath != null) {
            val coverNewPath = "$artistAlbumPath/cover.jpg"
            val coverNewFile = File(storagePath, coverNewPath)
            if (!coverNewFile.exists())
                if (!File(album.coverPath!!).renameTo(coverNewFile))
                    throw IllegalStateException("file rename failed! ${album.coverPath} -> ${coverNewFile.canonicalPath}")

            album.coverPath = "cover.jpg"
        }
        album.songs.forEach {
            val toFile = File(albumFolder, it.path)
            //todo: create a list of operations before executing them and check for conflicts
            if (toFile.exists())
                throw IllegalArgumentException("file already exists! ${toFile.canonicalPath}")

            //todo: probably better to copy file and remove old one afterwards
            if (!File(storagePath, "${fromArtist.path}/${album.path}/${it.path}").renameTo(toFile))
                throw IllegalStateException("file rename failed! ${it.path} -> ${toFile.canonicalPath}")
        }

        fromArtist.albums -= album
        toArtist.albums += album
        artistRepository.update(fromArtist.id, fromArtist, objectMapper)
        artistRepository.update(toArtist.id, toArtist, objectMapper)
    }

    fun delete(item: ArtistDto) {
        artistRepository.deleteById(item.id)
        FileTools.deleteRecursive(File(storagePath, item.path))
    }
}
