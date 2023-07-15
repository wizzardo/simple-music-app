package com.example.be.service

import com.example.be.db.DBService
import com.example.be.db.model.Artist
import com.example.be.db.model.Artist.Album
import com.example.be.service.FFmpegService.MetaData
import com.wizzardo.tools.security.MD5
import com.wizzardo.tools.sql.query.QueryBuilder
import java.io.File
import java.sql.SQLException
import java.util.regex.Pattern

class UploadService(
    private val songsStorageService: SongsStorageService,
    private val ffmpegService: FFmpegService,
    private val randomIdService: RandomIdService,
    private val artistService: ArtistService,
    private val dbService: DBService,
) {

    companion object {
        private const val UNKNOWN_ARTIST = "unknownArtist"
    }

    fun interface TempFileLoader {
        fun transferTo(dest: File)
    }

    fun upload(
        loader: TempFileLoader,
        fileName: String?,
        artistId: Long?,
        albumId: String?
    ): Artist {
        val ext = fileName?.substringAfterLast('.') ?: "bin"
        val tempFile = File.createTempFile("upload", ".$ext")
//        val tempFile = File("upload.$ext")
        try {
            loader.transferTo(tempFile)
            val metaData: MetaData = ffmpegService.getMetaData(tempFile)
            var artist: Artist? = artistId?.let { artistService.getArtist(it) }

            val artistPath = artist?.path
                ?: metaData.artist?.replace("/", " - ")
                ?: throw IllegalArgumentException("artist tag is empty!")

            val albumPath = albumId?.let { artist?.albums?.find { it.id == albumId } }?.path
                ?: metaData.album?.replace("/", " - ")
                ?: throw IllegalArgumentException("album tag is empty!")

            var title = metaData.title?.replace("/", " - ")
                ?: fileName?.substringBeforeLast('.')
                ?: throw IllegalArgumentException("title tag is empty!")
            var track = metaData.track
            if (track == null) {
                val matcher = Pattern.compile("^([0-9]+)").matcher(title)
                if (matcher.find()) {
                    track = matcher.group(1).toInt()
                    title = title.substring(matcher.end(1)).trim()
                    metaData.track = track
                }
            }

            if (metaData.title == null)
                metaData.title = title

            val fileName = if (track == null)
                "$title.${tempFile.extension}"
            else
                "$track - $title.${tempFile.extension}"

            var tries = 0;
            while (true) {
                try {
                    tries++;
                    if (!dbService.transaction { db ->
                            if (artist == null)
                                artist = artistService.getOrCreateArtist(db, metaData.artist ?: UNKNOWN_ARTIST, artistPath)
                            val added = addSong(db, artist!!, metaData, albumPath, fileName, tempFile)
                            if (added == null)
                                return@transaction false

                            songsStorageService.put(artist!!, added.album, added.song, tempFile)
                            true
                        })
                        continue
                    break
                } catch (e: SQLException) {
                    e.printStackTrace()
                    if (tries >= 5)
                        throw IllegalStateException(e)
                }
            }

            val album = artist?.albums?.find { it.path == albumPath }!!
            if (album.coverPath == null && metaData.streams.any { it.startsWith("Video:") }) {
                try {
                    val imageBytes = ffmpegService.extractCoverArt(tempFile)
                    while (true) {
                        try {
                            val artist = artistService.getArtist(artist!!.id)!!
                            val album = artist?.albums?.find { it.path == albumPath }!!
                            if (album.coverPath != null)
                                break

                            tries++;
                            album.coverPath = "cover.jpg"
                            album.coverHash = MD5.create().update(imageBytes).toString()
                            val updated = artistService.update(artist.id, artist)
                            if (updated != 1)
                                continue

                            songsStorageService.putCover(artist, album, imageBytes)
                            break
                        } catch (e: SQLException) {
                            e.printStackTrace()
                            if (tries >= 5)
                                throw IllegalStateException(e)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return artist!!
        } finally {
            tempFile.delete()
        }
    }

    data class AddResult(val artist: Artist, val album: Album, val song: Album.Song)

    fun addSong(db: QueryBuilder.WrapConnectionStep, artist: Artist, metaData: MetaData, albumPath: String, fileName: String, audio: File): AddResult? {
        val album = artist.albums.find { it.path == albumPath }
            ?: createAlbum(metaData, albumPath).also { artist.albums.add(it) }
        val song = createSong(metaData, fileName)
        album.songs.add(song)

        val updated = artistService.update(db, artist.id, artist)
        val added = updated == 1
        if (added) {
            println("added song ${song.track} ${song.title} to ${album.name}:")
            album.songs.forEach { println("  ${it.track} ${it.title}") }
        } else {
            println("retrying update")
        }

        return if (added) AddResult(artist, album, song) else null
    }

    private fun createAlbum(metaData: MetaData, relativePath: String): Album = Album().apply {
        id = randomIdService.generateId()
        path = relativePath
        metaData.date?.let { this.date = it }
        metaData.album?.let { this.name = it }
        this.songs = ArrayList()
        if (songsStorageService.encryption) {
            coverEncryptionKey = songsStorageService.createEncryptionKey()
        }
    }

    private fun createSong(metaData: MetaData, relativePath: String): Album.Song = Album.Song().apply {
        id = randomIdService.generateId()
        metaData.track?.let { this.track = it }
        metaData.title?.let { this.title = it }
        metaData.comment?.let { this.comment = it }
        metaData.duration?.let { this.duration = getMillis(it) }
        metaData.streams.let { this.streams = it }
        format = FFmpegService.AudioFormat.valueOf(relativePath.substringAfterLast('.').uppercase())
        path = relativePath
        if (songsStorageService.encryption) {
            encryptionKey = songsStorageService.createEncryptionKey()
        }
    }

    private fun getMillis(time: String): Int {
        val duration = time.split(",".toRegex(), 2)
        val (hMs, ms) = duration[0].split('.')
        val (h, m, s) = hMs.split(':').map { it.toInt() }

        return (ms.toInt() + ((h * 60 + m) * 60 + s) * 1000)
    }

    fun uploadCoverArt(artist: Artist, album: Album, file: File): Artist {
        val imageBytes = file.readBytes()
        file.delete()
        album.coverPath = "cover.jpg"
        album.coverHash = MD5.create().update(imageBytes).toString()
        songsStorageService.putCover(artist, album, imageBytes)
        return artistService.update(artist.id, artist, artist)
    }
}
