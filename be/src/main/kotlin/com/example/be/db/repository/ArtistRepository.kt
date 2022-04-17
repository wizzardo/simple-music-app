package com.example.be.db.repository

import com.example.be.db.dto.AlbumDto
import com.example.be.db.generated.tables.Artist.Companion.ARTIST
import com.example.be.db.generated.tables.daos.ArtistDao
import com.example.be.db.generated.tables.pojos.Artist
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.Configuration
import org.jooq.JSONB
import org.springframework.stereotype.Repository

@Repository
class ArtistRepository(configuration: Configuration) : ArtistDao(configuration) {

    fun findByName(name: String): Artist? {
        return ctx().selectFrom(ARTIST)
            .where(ARTIST.NAME.eq(name))
            .fetchOne(mapper())
    }

    fun updateAlbums(artist: Artist, albums: List<AlbumDto>, objectMapper: ObjectMapper): Int {
//        println("albums: $albums")
        return ctx().update(ARTIST)
            .set(ARTIST.ALBUMS, JSONB.valueOf(objectMapper.writeValueAsString(albums)))
            .where(ARTIST.ID.eq(artist.id))
            .execute()
    }
}
