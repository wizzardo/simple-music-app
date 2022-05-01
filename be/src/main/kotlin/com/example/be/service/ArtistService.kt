package com.example.be.service

import com.example.be.db.dto.ArtistDto
import com.example.be.db.dto.toArtistDto
import com.example.be.db.repository.ArtistRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
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
            val pathArtist = to.name.replace("/", " - ")
            to.albums.forEach { album ->
                val albumName = album.name.replace("/", " - ")
                val albumPath = "$pathArtist/$albumName"
                if (album.coverPath != null) {
                    val coverNewPath = "$albumPath/cover.jpg"
                    val coverNewFile = File(storagePath, coverNewPath)
                    if (!coverNewFile.exists())
                        if (!File(album.coverPath!!).renameTo(coverNewFile))
                            throw IllegalStateException("file rename failed! ${album.coverPath} -> ${coverNewFile.canonicalPath}")

                   album.coverPath = coverNewPath
                }
                album.songs.forEach {
                    //todo: update path
                    val title = it.title.replace("/", " - ")
                    val track = it.track

                    val fileName = "$track - $title.${it.path.substringAfterLast('.')}"
                    val albumFolder = File(storagePath, albumPath)
                    albumFolder.mkdirs()
                    val target = File(albumFolder, fileName)
                    //todo: create a list of operations before executing them and check for conflicts
                    if (target.exists())
                        throw IllegalArgumentException("file already exists! ${target.canonicalPath}")

                    //todo: probably better to copy file and remove old one afterwards
                    if (!File(storagePath, it.path).renameTo(target))
                        throw IllegalStateException("file rename failed! ${it.path} -> ${target.canonicalPath}")

                    it.path = "$albumPath/$fileName"
                }

            }
        }
        artistRepository.update(id, to, objectMapper)
        return getArtist(id)!!
    }
}
