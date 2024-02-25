package com.example.be.controller

import com.example.be.db.model.Artist
import com.example.be.misc.TempFileInputStream
import com.example.be.service.*
import com.wizzardo.epoll.readable.ReadableByteArray
import com.wizzardo.http.ChunkedReadableData
import com.wizzardo.http.framework.Controller
import com.wizzardo.http.framework.RequestContext
import com.wizzardo.http.framework.template.ReadableDataRenderer
import com.wizzardo.http.framework.template.Renderer
import com.wizzardo.http.request.Header
import com.wizzardo.http.request.MultiPartFileEntry
import com.wizzardo.http.response.JsonResponseHelper
import com.wizzardo.http.response.Status
import com.wizzardo.tools.cache.Cache
import com.wizzardo.tools.io.IOTools
import com.wizzardo.tools.json.JsonTools
import com.wizzardo.tools.misc.Stopwatch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.zip.GZIPOutputStream


class ArtistController2 : Controller() {
    private lateinit var artistService: ArtistService
    private lateinit var songService: SongService
    private lateinit var ffmpegService: FFmpegService
    private lateinit var uploadService: UploadService
    private lateinit var streamHandlingService: StreamHandlingService

    companion object {
        val MAX_AGE_1_YEAR = "max-age=31556926"
    }

    val cache: Cache<ConversionTask, File> = Cache(-1, {
        processConvertionTask(it)
    })

    private fun processConvertionTask(it: ConversionTask): File {
        val artist: Artist = artistService.getArtist(it.artistId)
            ?: throw IllegalArgumentException("Artist not found. id: ${it.artistId}")
        val album: Artist.Album = artist.albums.find { album -> album.name == it.albumIdOrName || album.id == it.albumIdOrName || album.path == it.albumIdOrName }
            ?: throw IllegalArgumentException("Album not found. id: ${it.albumIdOrName}")

        val trackNumber: Int = it.songIdOrTrackNumber.toIntOrNull() ?: -1
        val song: Artist.Album.Song = album.songs.find { song -> song.id == it.songIdOrTrackNumber || song.track == trackNumber }
            ?: throw IllegalArgumentException("Song not found. id: ${it.albumIdOrName}")

        return ffmpegService.convert(artist, album, song, it.format, it.bitrate)
    }

    private fun startConversion(it: ConversionTask): FFmpegService.ConversionStreamResult {
        val artist: Artist = artistService.getArtist(it.artistId)
            ?: throw IllegalArgumentException("Artist not found. id: ${it.artistId}")
        val album: Artist.Album = artist.albums.find { album -> album.name == it.albumIdOrName || album.id == it.albumIdOrName || album.path == it.albumIdOrName }
            ?: throw IllegalArgumentException("Album not found. id: ${it.albumIdOrName}")

        val trackNumber: Int = it.songIdOrTrackNumber.toIntOrNull() ?: -1
        val song: Artist.Album.Song = album.songs.find { song -> song.id == it.songIdOrTrackNumber || song.track == trackNumber }
            ?: throw IllegalArgumentException("Song not found. id: ${it.albumIdOrName}")

        return ffmpegService.convertAsStream(artist, album, song, it.format, it.bitrate)
    }

    data class ConversionTask(
        val artistId: Long,
        val albumIdOrName: String,
        val songIdOrTrackNumber: String,
        val format: FFmpegService.AudioFormat,
        val bitrate: Int,
    )

    fun getArtists(): Renderer? {
        val permissions = permissions() ?: return render(Status._403)
        return renderJsonGzipped(artistService.getArtists())
    }

    fun renderJsonGzipped(o: Any?): Renderer? {
        if (o == null) {
            response.setStatus(Status._404)
            return null
        }
        response.appendHeader(Header.KV_CONTENT_TYPE_APPLICATION_JSON)

        val acceptEncoding = request.header(Header.KEY_ACCEPT_ENCODING)
        if (!(acceptEncoding?.contains("gzip") ?: false)) {
            return ReadableDataRenderer(JsonResponseHelper.renderJson(o))
        }

        class GZIPOutputStreamWithCompression(out: OutputStream, compression: Int) : GZIPOutputStream(out, 32 * 1024) {
            init {
                def.setLevel(compression)
            }
        }

        response.appendHeader(Header.KV_CONTENT_ENCODING_GZIP)

        val baos = ByteArrayOutputStream(32 * 1024)
        val gos = GZIPOutputStreamWithCompression(baos, 1)
        gos.use { gz -> JsonTools.serialize(o, gz) }
        val bytes = baos.toByteArray()
        return ReadableDataRenderer(ReadableByteArray(bytes))
    }

    fun getArtist(id: Long): Renderer {
        val permissions = permissions() ?: return render(Status._403)
        val item: Artist = artistService.getArtist(id) ?: return render(Status._404)
        return renderJson(item)
    }

    fun updateArtist(id: Long, data: Artist): Renderer {
        val permissions = permissions() ?: return render(Status._403)
        val item: Artist = artistService.getArtist(id) ?: return render(Status._404)
        val updated = artistService.update(id, item, data)
        return renderJson(updated)
    }

    fun deleteArtist(id: Long): Renderer {
        val permissions = permissions() ?: return render(Status._403)
        val item: Artist = artistService.getArtist(id) ?: return render(Status._404)
        artistService.delete(item)
        return render(Status._204)
    }

    fun deleteAlbum(
        artistId: Long,
        albumId: String,
    ): Renderer {
        val permissions = permissions() ?: return render(Status._403)
        val item: Artist = artistService.getArtist(artistId) ?: return render(Status._404)
        artistService.delete(item, albumId)
        return renderJson(artistService.getArtist(artistId))
    }

    data class CreateArtistRequest(var name: String = "")

    fun createArtist(data: CreateArtistRequest): Renderer {
        val permissions = permissions() ?: return render(Status._403)
        val artist = artistService.getOrCreateArtist(data.name, data.name.replace("/", " - "))
        return renderJson(artist)
    }


    data class CreateAlbumRequest(
        var artistId: Long = 0,
        var name: String = "",
    )

    fun createAlbum(
        artistId: Long,
        data: CreateAlbumRequest,
    ): Renderer {
        val permissions = permissions() ?: return render(Status._403)
        val artist: Artist = artistService.getArtist(artistId) ?: return render(Status._404)
        val albumPath = data.name.replace("/", " - ")
        val album = artist.albums.find { it.path == albumPath }
        return if (album == null) {
            artistService.createAlbum(data.name, albumPath).also { artist.albums.add(it) }
            renderJson(artistService.update(artist))
        } else {
            response.status(Status._202)
            renderJson(artist)
        }
    }


    fun deleteSong(
        artistId: Long,
        albumId: String,
        songId: String,
    ): Renderer {
        val permissions = permissions() ?: return render(Status._403)
        val item: Artist = artistService.getArtist(artistId) ?: return render(Status._404)
        artistService.delete(item, albumId, songId)
        return renderJson(artistService.getArtist(artistId))
    }

    data class MergeAlbumsRequest(
        var artistId: Long = 0,
        var intoAlbumId: String = "",
        var albums: List<String> = Collections.emptyList(),
    )

    fun mergeAlbums(
        artistId: Long,
        albumId: String,
        data: MergeAlbumsRequest,
    ): Renderer {
        val permissions = permissions() ?: return render(Status._403)
        val item: Artist = artistService.getArtist(artistId) ?: return render(Status._404)
        val updated = artistService.mergeAlbums(item, albumId, data.albums)
//        data.albums.forEach {
//            artistService.delete(updated, it)
//        }
//        return renderJson(artistService.getArtist(artistId))
        return renderJson(updated)
    }

    fun moveAlbum(
        artistId: Long,
        albumId: String,
        toArtistId: Long,
    ): Renderer {
        val permissions = permissions() ?: return render(Status._403)
        val fromArtist: Artist = artistService.getArtist(artistId) ?: return render(Status._404)
        val toArtist: Artist = artistService.getArtist(toArtistId) ?: return render(Status._404)
        val album: Artist.Album = fromArtist.albums.find({ it.id == albumId }) ?: return render(Status._404)
        artistService.move(fromArtist, toArtist, album)
        if (fromArtist.albums.size == 1) {
            artistService.delete(fromArtist)
        }
        return render(Status._204)
    }


    fun uploadCoverArt(
        artistId: Long,
        albumId: String,
        file: File,
    ): Renderer {
        val permissions = permissions() ?: return render(Status._403)
        val item: Artist = artistService.getArtist(artistId) ?: return render(Status._404)
        val album = item.albums.find { it.id == albumId } ?: return render(Status._404)

        val updated = uploadService.uploadCoverArt(item, album, file)

        return renderJson(updated)
    }

    fun getSong(
        artistId: Long,
        albumId: String,
        songId: String,
    ): Renderer {
        val permissions = permissions() ?: return render(Status._403)
        val artist: Artist = artistService.getArtist(artistId) ?: return render(Status._404)
        val album: Artist.Album = artist.albums.find { album -> album.id == albumId } ?: return render(Status._404)
        val song: Artist.Album.Song = album.songs.find { song -> song.id == songId } ?: return render(Status._404)
        val data = songService.getSongData(artist, album, song)
        val type = FFmpegService.AudioFormat.values().find { song.path.endsWith(it.name, true) }?.mimeType
        response.appendHeader(Header.KEY_CONTENT_TYPE, type ?: "application/octet-stream")
            .appendHeader(Header.KEY_CONTENT_LENGTH, data.length().toString())
        return renderChunked(TempFileInputStream(data))
    }

    fun getSongConverted(
        artistId: Long,
        albumIdOrName: String,
        songIdOrTrackNumber: String,
        format: FFmpegService.AudioFormat,
        bitrate: Int,
    ): Renderer {
        val permissions = permissions() ?: return render(Status._403)

        val artist: Artist = artistService.getArtist(artistId)
            ?: return render(Status._404)
        val album: Artist.Album = artist.albums.find { album -> album.name == albumIdOrName || album.id == albumIdOrName || album.path == albumIdOrName }
            ?: return render(Status._404)

        val trackNumber: Int = songIdOrTrackNumber.toIntOrNull() ?: -1
        val song: Artist.Album.Song = album.songs.find { song -> song.id == songIdOrTrackNumber || song.track == trackNumber }
            ?: return render(Status._404)

        val data = cache.get(ConversionTask(artistId, albumIdOrName, songIdOrTrackNumber, format, bitrate))
        response.appendHeader(Header.KEY_CONTENT_TYPE, format.mimeType)
            .appendHeader(Header.KEY_CONTENT_LENGTH, data.length().toString())
        return renderChunked(FileInputStream(data))
    }

    fun getSongConvertedStreamed(
        artistId: Long,
        albumIdOrName: String,
        songIdOrTrackNumber: String,
        format: FFmpegService.AudioFormat,
        bitrate: Int,
    ): Renderer {
        val permissions = permissions() ?: return render(Status._403)

        val artist: Artist = artistService.getArtist(artistId)
            ?: return render(Status._404)
        val album: Artist.Album = artist.albums.find { album -> album.name == albumIdOrName || album.id == albumIdOrName || album.path == albumIdOrName }
            ?: return render(Status._404)

        val trackNumber: Int = songIdOrTrackNumber.toIntOrNull() ?: -1
        val song: Artist.Album.Song = album.songs.find { song -> song.id == songIdOrTrackNumber || song.track == trackNumber }
            ?: return render(Status._404)

        val result = startConversion(ConversionTask(artistId, albumIdOrName, songIdOrTrackNumber, format, bitrate))
        response.appendHeader(Header.KEY_CONTENT_TYPE, result.format.mimeType)
        return renderChunked(result.data)
    }

    fun getAlbumCover(
        artistIdOrPath: String,
        albumIdOrPath: String,
    ): Renderer {
        val permissions = permissions() ?: return render(Status._403)

        val artist = artistService.getArtistByIdOrPath(artistIdOrPath) ?: return render(Status._404)
        val album = songService.getAlbum(artist, albumIdOrPath) ?: return render(Status._404)
        if (album.coverPath == null)
            return render(Status._404)

        val ifNoneMatch = request.header(Header.KEY_IF_NONE_MATCH)
        if (ifNoneMatch != null && ifNoneMatch == "\"" + album.coverHash + "\"")
            return render(Status._304)

        val stopwatch = Stopwatch("getAlbumCoverData")
        val data = songService.getAlbumCoverData(artist, album)
        println(stopwatch)

        response.appendHeader(Header.KEY_CONTENT_TYPE, "image/jpeg")
            .appendHeader(Header.KEY_CONTENT_LENGTH, data.length().toString())
            .appendHeader(Header.KEY_CACHE_CONTROL, MAX_AGE_1_YEAR)
            .appendHeader(Header.KEY_ETAG, "\"" + album.coverHash + "\"")

        return renderChunked(data)
    }

    fun upload(
        file: MultiPartFileEntry,
        artistId: Long?,
        albumId: String?,
    ): Renderer {
        val permissions = permissions() ?: return render(Status._403)
        return renderJson(uploadService.upload({ file.save(it) }, file.fileName(), artistId, albumId))
    }


    private fun permissions(): Set<AuthenticationService.Permission>? =
        RequestContext.get().requestHolder.get<Set<AuthenticationService.Permission>?>("permissions")

    private fun render(status: Status, body: String = ""): Renderer {
        response.status(status)
        return renderString(body)
    }

    private fun renderChunked(inputStream: InputStream): Renderer {
        response.appendHeader(Header.KEY_TRANSFER_ENCODING, Header.VALUE_CHUNKED)

        response.commit(request.connection())
        request.connection().flush()
        response.async()

        streamHandlingService.sendInputStream(request.connection(), inputStream)
//        request.connection().send(createChunkedReadable(inputStream))

        return render(Status._200)
    }

    private fun createChunkedReadable(data: InputStream): ChunkedReadableData {
        val buf = ByteArray(1024 * 16)

        val chunkedReadableData = ChunkedReadableData(
            { to ->
                val r = data.read(buf, 0, buf.size)
                to.consume(buf, 0, r)
                if (r == -1) {
                    IOTools.close(data)
                }
            },
            request.connection()
        )
        return chunkedReadableData
    }
}