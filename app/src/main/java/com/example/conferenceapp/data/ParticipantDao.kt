package com.example.conferenceapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ParticipantDao {


    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertParticipant(p: ParticipantEntity)

    @Query("SELECT * FROM participants WHERE userId = :id LIMIT 1")
    suspend fun getParticipantById(id: Int): ParticipantEntity?
}