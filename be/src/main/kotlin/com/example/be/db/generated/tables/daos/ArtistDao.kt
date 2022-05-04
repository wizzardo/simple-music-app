/*
 * This file is generated by jOOQ.
 */
package com.example.be.db.generated.tables.daos


import com.example.be.db.generated.tables.Artist
import com.example.be.db.generated.tables.records.ArtistRecord

import java.time.LocalDateTime

import javax.annotation.processing.Generated

import kotlin.collections.List

import org.jooq.Configuration
import org.jooq.JSONB
import org.jooq.impl.DAOImpl


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = [
        "https://www.jooq.org",
        "jOOQ version:3.16.5"
    ],
    comments = "This class is generated by jOOQ"
)
@Suppress("UNCHECKED_CAST")
open class ArtistDao(configuration: Configuration?) : DAOImpl<ArtistRecord, com.example.be.db.generated.tables.pojos.Artist, Long>(Artist.ARTIST, com.example.be.db.generated.tables.pojos.Artist::class.java, configuration) {

    /**
     * Create a new ArtistDao without any configuration
     */
    constructor(): this(null)

    override fun getId(o: com.example.be.db.generated.tables.pojos.Artist): Long? = o.id

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    fun fetchRangeOfId(lowerInclusive: Long?, upperInclusive: Long?): List<com.example.be.db.generated.tables.pojos.Artist> = fetchRange(Artist.ARTIST.ID, lowerInclusive, upperInclusive)

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    fun fetchById(vararg values: Long): List<com.example.be.db.generated.tables.pojos.Artist> = fetch(Artist.ARTIST.ID, *values.toTypedArray())

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    fun fetchOneById(value: Long): com.example.be.db.generated.tables.pojos.Artist? = fetchOne(Artist.ARTIST.ID, value)

    /**
     * Fetch records that have <code>created BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    fun fetchRangeOfCreated(lowerInclusive: LocalDateTime?, upperInclusive: LocalDateTime?): List<com.example.be.db.generated.tables.pojos.Artist> = fetchRange(Artist.ARTIST.CREATED, lowerInclusive, upperInclusive)

    /**
     * Fetch records that have <code>created IN (values)</code>
     */
    fun fetchByCreated(vararg values: LocalDateTime): List<com.example.be.db.generated.tables.pojos.Artist> = fetch(Artist.ARTIST.CREATED, *values)

    /**
     * Fetch records that have <code>updated BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    fun fetchRangeOfUpdated(lowerInclusive: LocalDateTime?, upperInclusive: LocalDateTime?): List<com.example.be.db.generated.tables.pojos.Artist> = fetchRange(Artist.ARTIST.UPDATED, lowerInclusive, upperInclusive)

    /**
     * Fetch records that have <code>updated IN (values)</code>
     */
    fun fetchByUpdated(vararg values: LocalDateTime): List<com.example.be.db.generated.tables.pojos.Artist> = fetch(Artist.ARTIST.UPDATED, *values)

    /**
     * Fetch records that have <code>name BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    fun fetchRangeOfName(lowerInclusive: String?, upperInclusive: String?): List<com.example.be.db.generated.tables.pojos.Artist> = fetchRange(Artist.ARTIST.NAME, lowerInclusive, upperInclusive)

    /**
     * Fetch records that have <code>name IN (values)</code>
     */
    fun fetchByName(vararg values: String): List<com.example.be.db.generated.tables.pojos.Artist> = fetch(Artist.ARTIST.NAME, *values)

    /**
     * Fetch records that have <code>albums BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    fun fetchRangeOfAlbums(lowerInclusive: JSONB?, upperInclusive: JSONB?): List<com.example.be.db.generated.tables.pojos.Artist> = fetchRange(Artist.ARTIST.ALBUMS, lowerInclusive, upperInclusive)

    /**
     * Fetch records that have <code>albums IN (values)</code>
     */
    fun fetchByAlbums(vararg values: JSONB): List<com.example.be.db.generated.tables.pojos.Artist> = fetch(Artist.ARTIST.ALBUMS, *values)

    /**
     * Fetch records that have <code>path BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    fun fetchRangeOfPath(lowerInclusive: String?, upperInclusive: String?): List<com.example.be.db.generated.tables.pojos.Artist> = fetchRange(Artist.ARTIST.PATH, lowerInclusive, upperInclusive)

    /**
     * Fetch records that have <code>path IN (values)</code>
     */
    fun fetchByPath(vararg values: String): List<com.example.be.db.generated.tables.pojos.Artist> = fetch(Artist.ARTIST.PATH, *values)
}
