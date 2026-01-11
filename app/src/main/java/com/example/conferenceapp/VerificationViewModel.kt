package com.example.conferenceapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conferenceapp.data.ParticipantEntity
import com.example.conferenceapp.data.ParticipantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerificationUiState(
    val searchIdText: String = "",
    val result: ParticipantEntity? = null,
    val notFound: Boolean = false,
    val message: String? = null
)

class VerificationViewModel(
    private val repo: ParticipantRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VerificationUiState())
    val state: StateFlow<VerificationUiState> = _state

    fun setSearchIdText(v: String) = _state.update { it.copy(searchIdText = v) }
    fun clearMessage() = _state.update { it.copy(message = null) }

    fun verify() {
        val id = _state.value.searchIdText.toIntOrNull()
        if (id == null) {
            _state.update { it.copy(message = "User ID must be a number.", result = null, notFound = true) }
            return
        }

        viewModelScope.launch {
            val p = repo.findById(id)
            if (p == null) {
                _state.update {
                    it.copy(result = null, notFound = true, message = "User Not Found ❌")
                }
            } else {
                _state.update {
                    it.copy(result = p, notFound = false, message = "User Found ✅")
                }
            }
        }
    }
}