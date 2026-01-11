package com.example.conferenceapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "participants")
data class ParticipantEntity(
    @PrimaryKey val userId: Int,                 // Unique Integer
    val fullName: String,                        // EditText
    val title: String,                           // Spinner: Prof, Dr., Student
    val registrationType: Int,                   // 1-Full, 2-Student, 3-None
    val photoUri: String?                        // path (Uri string)
)