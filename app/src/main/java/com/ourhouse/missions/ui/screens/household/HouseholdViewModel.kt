package com.ourhouse.missions.ui.screens.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourhouse.missions.data.local.SessionManager
import com.ourhouse.missions.data.model.Household
import com.ourhouse.missions.data.repository.AuthRepository
import com.ourhouse.missions.data.repository.HouseholdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HouseholdState(
    val household: Household? = null,
    val myUserId: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val left: Boolean = false
)

class HouseholdViewModel(
    private val householdRepository: HouseholdRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(HouseholdState())
    val state: StateFlow<HouseholdState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val householdId = sessionManager.householdId.filterNotNull().first()
            runCatching {
                householdRepository.observeHousehold(householdId).collect { household ->
                    _state.value = _state.value.copy(
                        household = household,
                        myUserId = authRepository.currentUserId,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _state.value = _state.value.copy(isLoading = false, errorMessage = error.message ?: "שגיאה בטעינת הבית המשותף")
            }
        }
    }

    /** Disconnects this device only — it forgets the local session so the pairing
     *  screen shows again, without touching the shared household data or the
     *  partner's device. */
    fun leaveOnThisDevice() = viewModelScope.launch {
        sessionManager.clear()
        _state.value = _state.value.copy(left = true)
    }
}
