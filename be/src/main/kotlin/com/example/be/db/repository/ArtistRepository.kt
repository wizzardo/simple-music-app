package com.example.be.db.repository

import com.example.be.db.dto.AlbumDto
import com.example.be.db.dto.ArtistDto
import com.example.be.db.generated.tables.Artist.Companion.ARTIST
import com.example.be.db.generated.tables.daos.ArtistDao
import com.example.be.db.generated.tables.pojos.Artist
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.Configuration
import org.jooq.JSONB
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
open class ArtistRepository(configuration: Configuration) : ArtistDao(configuration) {

    fun findByName(name: String): Artist? {
        return ctx().selectFrom(ARTIST)
            .where(ARTIST.NAME.eq(name))
            .limit(1)
            .fetchOne(mapper())
    }

    fun findByPath(path: String): Artist? {
        return ctx().selectFrom(ARTIST)
            .where(ARTIST.PATH.eq(path))
            .limit(1)
            .fetchOne(mapper())
    }

    fun updateAlbums(artist: Artist, albums: List<AlbumDto>, objectMapper: ObjectMapper): Int {
//        println("albums: $albums")
        return ctx().update(ARTIST)
            .set(ARTIST.UPDATED, LocalDateTime.now())
            .set(ARTIST.ALBUMS, JSONB.valueOf(objectMapper.writeValueAsString(albums)))
            .where(ARTIST.ID.eq(artist.id))
            .and(ARTIST.UPDATED.eq(artist.updated))
            .execute()
    }

    fun update(id: Long, data: ArtistDto, objectMapper: ObjectMapper): Int {
        return ctx().update(ARTIST)
            .set(ARTIST.UPDATED, LocalDateTime.now())
            .set(ARTIST.NAME, data.name)
            .set(ARTIST.PATH, data.path)
            .set(ARTIST.ALBUMS, JSONB.valueOf(objectMapper.writeValueAsString(data.albums)))
            .where(ARTIST.ID.eq(id))
            .execute()
    }
}
