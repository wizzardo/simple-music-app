package com.example.be.service

import com.example.be.db.DBService
import com.example.be.db.generated.Tables
import com.example.be.db.model.Artist
import com.example.be.db.model.Artist.Album
import com.wizzardo.tools.sql.query.QueryBuilder
import org.springframework.stereotype.Service
import java.util.Date

@Service
class ArtistService(
    private val songsStorageService: SongsStorageService,
    private val randomIdService: RandomIdService,
    private val dbService: DBService,
) {

    fun findByName(name: String): Artist? = dbService.withBuilder { db -> findByName(db, name) }
    fun findByName(db: QueryBuilder.WrapConnectionStep, name: String): Artist? {
        return db.select(Tables.ARTIST.FIELDS)
            .from(Tables.ARTIST)
            .where(Tables.ARTIST.NAME.eq(name))
            .limit(1)
            .fetchOneInto(Artist::class.java)
    }

    fun findById(id: Long): Artist? = dbService.withBuilder { db -> findById(db, id) }
    fun findById(db: QueryBuilder.WrapConnectionStep, id: Long): Artist? {
        return db.select(Tables.ARTIST.FIELDS)
            .from(Tables.ARTIST)
            .where(Tables.ARTIST.ID.eq(id))
            .limit(1)
            .fetchOneInto(Artist::class.java)
    }

    fun findByPath(path: String): Artist? {
        return dbService.withBuilder { db ->
            db.select(Tables.ARTIST.FIELDS)
                .from(Tables.ARTIST)
                .where(Tables.ARTIST.PATH.eq(path))
                .limit(1)
                .fetchOneInto(Artist::class.java)
        }
    }

    fun findByIdOrPath(path: String): Artist? {
        return dbService.withBuilder { db ->
            db.select(Tables.ARTIST.FIELDS)
                .from(Tables.ARTIST)
                .where(Tables.ARTIST.PATH.eq(path).or(Tables.ARTIST.ID.eq(path.toLongOrNull())))
                .limit(1)
                .fetchOneInto(Artist::class.java)
        }
    }

    fun updateAlbums(artist: Artist, albums: List<Album>): Int {
//        println("albums: $albums")
        return dbService.withBuilder { db ->
            db.update(Tables.ARTIST)
                .set(Tables.ARTIST.UPDATED.eq(Date()))
                .set(Tables.ARTIST.ALBUMS.eq(artist.albums))
                .where(Tables.ARTIST.ID.eq(artist.id).and(Tables.ARTIST.UPDATED.eq(artist.updated)))
                .executeUpdate()
        }
    }

    fun update(id: Long, data: Artist): Int = dbService.withBuilder { db -> update(db, id, data) }
    fun update(db: QueryBuilder.WrapConnectionStep, id: Long, data: Artist): Int {
        return db.update(Tables.ARTIST)
            .set(Tables.ARTIST.UPDATED.eq(Date()))
            .set(Tables.ARTIST.ALBUMS.eq(data.albums))
            .set(Tables.ARTIST.NAME.eq(data.name))
            .set(Tables.ARTIST.PATH.eq(data.path))
            .where(Tables.ARTIST.ID.eq(id))
            .executeUpdate()
    }

    fun getArtists(): List<Artist> {
        return dbService.withBuilder { db ->
            db.select(Tables.ARTIST.FIELDS)
                .from(Tables.ARTIST)
                .fetchInto(Artist::class.java)
        }
    }

    fun getArtist(id: Long): Artist? {
        return findById(id)
    }

    fun getArtistByName(name: String): Artist? {
        return findByName(name)
    }

    fun update(id: Long, from: Artist, to: Artist): Artist {
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
        update(id, to)
        return getArtist(id)!!
    }

    fun update(item: Artist): Artist {
        update(item.id, item)
        return getArtist(item.id)!!
    }

    fun getArtistByPath(artistPath: String): Artist? {
        return findByPath(artistPath)
    }

    fun getArtistByIdOrPath(idOrPath: String): Artist? {
        return findByIdOrPath(idOrPath)
    }

    fun mergeAlbums(item: Artist, intoAlbumId: String, albums: List<String>): Artist {
        val album = item.albums.find { it.id == intoAlbumId } ?: throw IllegalArgumentException("Cannot find album with id ${intoAlbumId}")
        val songs: MutableList<Album.Song> = ArrayList(album.songs)
        album.songs = songs

        val mergedAlbums: MutableList<Album> = ArrayList(item.albums.size - albums.size)
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
        update(item.id, item)
        return getArtist(item.id)!!
    }

    fun move(fromArtist: Artist, toArtist: Artist, album: Album) {
        songsStorageService.createFolder(toArtist, album)

        if (album.coverPath != null) {
            songsStorageService.moveCover(fromArtist, album, toArtist, album)
        }
        album.songs.forEach loop@{
            songsStorageService.move(fromArtist, album, it, toArtist, album, it)
        }

        fromArtist.albums.remove(album)
        toArtist.albums.add(album)
        dbService.transaction { db ->
            update(db, fromArtist.id, fromArtist)
            update(db, toArtist.id, toArtist)
        }
    }

    fun delete(item: Artist) {
        dbService.withBuilder { db ->
            db.deleteFrom(Tables.ARTIST)
                .where(Tables.ARTIST.ID.eq(item.id))
                .executeUpdate()
        }
        songsStorageService.delete(item)
    }

    fun delete(artist: Artist, albumId: String) {
        val album = artist.albums.find { it.id == albumId }!!
        artist.albums.remove(album)
        update(artist.id, artist)
        songsStorageService.delete(artist, album)
    }

    fun delete(artist: Artist, albumId: String, songId: String) {
        val album = artist.albums.find { it.id == albumId }!!
        val song = album.songs.find { it.id == songId }!!
        album.songs.remove(song)
        update(artist.id, artist)
        songsStorageService.delete(artist, album, song)
    }

    fun getOrCreateArtist(name: String, path: String): Artist = dbService.withBuilder { db-> getOrCreateArtist(db, name, path) }
    fun getOrCreateArtist(db: QueryBuilder.WrapConnectionStep, name: String, path: String): Artist {
        var artist: Artist? = findByName(db, name)
        if (artist == null) {
            artist = createArtist(name, path)
            dbService.insertInto(db, artist, Tables.ARTIST)
        }
        return artist
    }

    private fun createArtist(name: String, path: String): Artist = Artist().apply {
        created = Date()
        updated = Date()
        this.name = name
        this.path = path
        albums = emptyList()
    }

    fun createAlbum(name: String, path: String): Album = Album().apply {
        id = randomIdService.generateId()
        this.path = path
        this.name = name
        this.songs = emptyList()
        if (songsStorageService.encryption) {
            coverEncryptionKey = songsStorageService.createEncryptionKey()
        }
    }
}
