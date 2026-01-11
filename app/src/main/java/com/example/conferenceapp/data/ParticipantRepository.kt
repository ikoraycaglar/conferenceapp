package com.example.conferenceapp.data

class ParticipantRepository(
    private val dao: ParticipantDao
) {
    suspend fun registerParticipant(p: ParticipantEntity) = dao.insertParticipant(p)
    suspend fun findById(id: Int): ParticipantEntity? = dao.getParticipantById(id)
}