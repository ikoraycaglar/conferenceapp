package com.example.conferenceapp

import android.content.Context
import com.example.conferenceapp.data.AppDatabase
import com.example.conferenceapp.data.ParticipantRepository

class AppContainer(context: Context) {
    private val db = AppDatabase.build(context)
    val participantRepository = ParticipantRepository(db.participantDao())
}