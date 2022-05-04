package com.example.be.db.dto

import com.example.be.db.generated.tables.pojos.Artist
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime

class ArtistDto(
    val id: Long,
    val created: LocalDateTime,
    val updated: LocalDateTime,
    var name: String,
    var path: String,
    var albums: List<AlbumDto>,
)

fun Artist.toArtistDto(objectMapper: ObjectMapper): ArtistDto = ArtistDto(
    id = id!!,
    created = created!!,
    updated = updated!!,
    name = name!!,
    path = path!!,
    albums = objectMapper.readValue(albums!!.data(), object : TypeReference<ArrayList<AlbumDto>>() {})
)
