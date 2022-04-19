package com.example.be.db.dto

import com.example.be.db.generated.tables.pojos.Artist
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime

class ArtistDto(
    val id: Long,
    val created: LocalDateTime,
    val updated: LocalDateTime,
    val name: String,
    val albums: List<AlbumDto>,
)

fun Artist.toArtistDto(objectMapper: ObjectMapper): ArtistDto = ArtistDto(
    id = id!!,
    created = created!!,
    updated = updated!!,
    name = name!!,
    albums = objectMapper.readValue(albums!!.data(), object : TypeReference<ArrayList<AlbumDto>>() {})
)
