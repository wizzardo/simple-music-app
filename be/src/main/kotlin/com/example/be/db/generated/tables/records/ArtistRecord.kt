/*
 * This file is generated by jOOQ.
 */
package com.example.be.db.generated.tables.records


import com.example.be.db.generated.tables.Artist

import java.time.LocalDateTime

import javax.annotation.processing.Generated

import org.jooq.Field
import org.jooq.JSONB
import org.jooq.Record1
import org.jooq.Record6
import org.jooq.Row6
import org.jooq.impl.UpdatableRecordImpl


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
open class ArtistRecord() : UpdatableRecordImpl<ArtistRecord>(Artist.ARTIST), Record6<Long?, LocalDateTime?, LocalDateTime?, String?, JSONB?, String?> {

    var id: Long?
        set(value): Unit = set(0, value)
        get(): Long? = get(0) as Long?

    var created: LocalDateTime?
        set(value): Unit = set(1, value)
        get(): LocalDateTime? = get(1) as LocalDateTime?

    var updated: LocalDateTime?
        set(value): Unit = set(2, value)
        get(): LocalDateTime? = get(2) as LocalDateTime?

    var name: String?
        set(value): Unit = set(3, value)
        get(): String? = get(3) as String?

    var albums: JSONB?
        set(value): Unit = set(4, value)
        get(): JSONB? = get(4) as JSONB?

    var path: String?
        set(value): Unit = set(5, value)
        get(): String? = get(5) as String?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Long?> = super.key() as Record1<Long?>

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row6<Long?, LocalDateTime?, LocalDateTime?, String?, JSONB?, String?> = super.fieldsRow() as Row6<Long?, LocalDateTime?, LocalDateTime?, String?, JSONB?, String?>
    override fun valuesRow(): Row6<Long?, LocalDateTime?, LocalDateTime?, String?, JSONB?, String?> = super.valuesRow() as Row6<Long?, LocalDateTime?, LocalDateTime?, String?, JSONB?, String?>
    override fun field1(): Field<Long?> = Artist.ARTIST.ID
    override fun field2(): Field<LocalDateTime?> = Artist.ARTIST.CREATED
    override fun field3(): Field<LocalDateTime?> = Artist.ARTIST.UPDATED
    override fun field4(): Field<String?> = Artist.ARTIST.NAME
    override fun field5(): Field<JSONB?> = Artist.ARTIST.ALBUMS
    override fun field6(): Field<String?> = Artist.ARTIST.PATH
    override fun component1(): Long? = id
    override fun component2(): LocalDateTime? = created
    override fun component3(): LocalDateTime? = updated
    override fun component4(): String? = name
    override fun component5(): JSONB? = albums
    override fun component6(): String? = path
    override fun value1(): Long? = id
    override fun value2(): LocalDateTime? = created
    override fun value3(): LocalDateTime? = updated
    override fun value4(): String? = name
    override fun value5(): JSONB? = albums
    override fun value6(): String? = path

    override fun value1(value: Long?): ArtistRecord {
        this.id = value
        return this
    }

    override fun value2(value: LocalDateTime?): ArtistRecord {
        this.created = value
        return this
    }

    override fun value3(value: LocalDateTime?): ArtistRecord {
        this.updated = value
        return this
    }

    override fun value4(value: String?): ArtistRecord {
        this.name = value
        return this
    }

    override fun value5(value: JSONB?): ArtistRecord {
        this.albums = value
        return this
    }

    override fun value6(value: String?): ArtistRecord {
        this.path = value
        return this
    }

    override fun values(value1: Long?, value2: LocalDateTime?, value3: LocalDateTime?, value4: String?, value5: JSONB?, value6: String?): ArtistRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        this.value4(value4)
        this.value5(value5)
        this.value6(value6)
        return this
    }

    /**
     * Create a detached, initialised ArtistRecord
     */
    constructor(id: Long? = null, created: LocalDateTime? = null, updated: LocalDateTime? = null, name: String? = null, albums: JSONB? = null, path: String? = null): this() {
        this.id = id
        this.created = created
        this.updated = updated
        this.name = name
        this.albums = albums
        this.path = path
    }

    /**
     * Create a detached, initialised ArtistRecord
     */
    constructor(value: com.example.be.db.generated.tables.pojos.Artist?): this() {
        if (value != null) {
            this.id = value.id
            this.created = value.created
            this.updated = value.updated
            this.name = value.name
            this.albums = value.albums
            this.path = value.path
        }
    }
}
