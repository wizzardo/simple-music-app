package com.example.be.service

import com.example.be.db.dto.ArtistDto
import com.example.be.db.dto.toArtistDto
import com.example.be.db.repository.ArtistRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class ArtistService(
    private val artistRepository: ArtistRepository,
    private val objectMapper: ObjectMapper,
) {

    fun getArtists(): List<ArtistDto> {
        return artistRepository.findAll().map { artist -> artist.toArtistDto(objectMapper) }
    }
}
