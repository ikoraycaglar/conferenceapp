package com.example.conferenceapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conferenceapp.data.ParticipantEntity
import com.example.conferenceapp.data.ParticipantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistrationUiState(
    val userIdText: String = "",
    val fullName: String = "",
    val title: String = "Prof",
    val regType: Int = 1,
    val photoUri: String? = null,
    val message: String? = null
)

class RegistrationViewModel(
    private val repo: ParticipantRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegistrationUiState())
    val state: StateFlow<RegistrationUiState> = _state

    fun setUserIdText(v: String) = _state.update { it.copy(userIdText = v) }
    fun setFullName(v: String) = _state.update { it.copy(fullName = v) }
    fun setTitle(v: String) = _state.update { it.copy(title = v) }
    fun setRegType(v: Int) = _state.update { it.copy(regType = v) }
    fun setPhotoUri(v: String?) = _state.update { it.copy(photoUri = v) }
    fun clearMessage() = _state.update { it.copy(message = null) }
    fun showMessage(msg: String) = _state.update { it.copy(message = msg) }

    fun register() {
        val s = _state.value

        val id = s.userIdText.toIntOrNull()
        if (id == null) {
            _state.update { it.copy(message = "User ID must be an integer (example: 101).") }
            return
        }
        if (s.fullName.isBlank()) {
            _state.update { it.copy(message = "Full Name cannot be empty.") }
            return
        }

        viewModelScope.launch {
            try {
                repo.registerParticipant(
                    ParticipantEntity(
                        userId = id,
                        fullName = s.fullName.trim(),
                        title = s.title,
                        registrationType = s.regType,
                        photoUri = s.photoUri
                    )
                )
                _state.update {
                    it.copy(
                        userIdText = "",
                        fullName = "",
                        title = "Prof",
                        regType = 1,
                        photoUri = null,
                        message = "Registered Successfully ✅"
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(message = "Register Failed: ID exists or DB malfunction.")
                }
            }
        }
    }
}