package com.ourhouse.missions.ui.screens.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourhouse.missions.data.repository.AuthRepository
import com.ourhouse.missions.data.repository.HouseholdRepository
import com.ourhouse.missions.data.local.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PairingState(
    val isLoading: Boolean = false,
    val inviteCode: String? = null,
    val errorMessage: String? = null,
    val paired: Boolean = false
)

class PairingViewModel(
    private val authRepository: AuthRepository,
    private val householdRepository: HouseholdRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(PairingState())
    val state: StateFlow<PairingState> = _state.asStateFlow()

    /** Called when the first partner taps "שיתוף הקוד" — creates the household and
     *  shows the 6-digit code so it can be sent (WhatsApp, SMS, in person) to the other phone. */
    fun createHousehold(displayName: String) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        runCatching {
            val userId = authRepository.ensureSignedIn()
            val household = householdRepository.createHousehold(userId, displayName)
            sessionManager.saveHousehold(household.id, displayName)
            household
        }.onSuccess { household ->
            // Not `paired` yet: stay on screen so the code can be read out/sent, then the
            // user taps Continue.
            _state.value = _state.value.copy(isLoading = false, inviteCode = household.inviteCode)
        }.onFailure { error ->
            _state.value = _state.value.copy(isLoading = false, errorMessage = error.message)
        }
    }

    /** Called when the second partner enters the code they were sent. */
    fun joinHousehold(code: String, displayName: String) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        runCatching {
            val userId = authRepository.ensureSignedIn()
            householdRepository.joinHousehold(code, userId, displayName).getOrThrow()
        }.onSuccess { household ->
            sessionManager.saveHousehold(household.id, displayName)
            _state.value = _state.value.copy(isLoading = false, paired = true)
        }.onFailure { error ->
            _state.value = _state.value.copy(isLoading = false, errorMessage = error.message ?: "החיבור נכשל")
        }
    }
}
