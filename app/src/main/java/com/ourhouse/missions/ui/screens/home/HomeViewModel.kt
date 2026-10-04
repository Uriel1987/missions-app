package com.ourhouse.missions.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourhouse.missions.data.local.SessionManager
import com.ourhouse.missions.data.model.Task
import com.ourhouse.missions.data.repository.AuthRepository
import com.ourhouse.missions.data.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class HomeFilter { ALL, MINE, PARTNER, UNASSIGNED }

data class HomeState(
    val tasks: List<Task> = emptyList(),
    val filter: HomeFilter = HomeFilter.ALL,
    val currentUserId: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val hasUnreadHandoff: Boolean = false
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val filter = MutableStateFlow(HomeFilter.ALL)
    private val allTasks = MutableStateFlow<List<Task>>(emptyList())
    private val isLoading = MutableStateFlow(true)

    private val errorMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<HomeState> = combine(allTasks, filter, isLoading, errorMessage) { tasks, currentFilter, loading, error ->
        val userId = authRepository.currentUserId
        val visible = when (currentFilter) {
            HomeFilter.ALL -> tasks
            HomeFilter.MINE -> tasks.filter { it.assigneeId == userId }
            HomeFilter.PARTNER -> tasks.filter { it.assigneeId != null && it.assigneeId != userId }
            HomeFilter.UNASSIGNED -> tasks.filter { it.assigneeId == null }
        }
        // "Unread" here means: something was just handed off to me — a good-enough
        // signal without a dedicated read/unread tracking collection.
        val hasUnread = tasks.any { it.lastHandoffAt != null && it.assigneeId == userId }
        HomeState(
            tasks = visible, filter = currentFilter, currentUserId = userId,
            isLoading = loading, errorMessage = error, hasUnreadHandoff = hasUnread
        )
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), HomeState())

    init {
        viewModelScope.launch {
            val householdId = sessionManager.householdId.filterNotNull().first()
            runCatching {
                taskRepository.observeTasks(householdId).collect { tasks ->
                    allTasks.value = tasks
                    isLoading.value = false
                    errorMessage.value = null
                }
            }.onFailure { error ->
                isLoading.value = false
                errorMessage.value = error.message ?: "שגיאה בטעינת המשימות"
            }
        }
    }

    fun setFilter(newFilter: HomeFilter) {
        filter.value = newFilter
    }

    fun toggleCompleted(task: Task) = viewModelScope.launch {
        val householdId = sessionManager.householdId.first() ?: return@launch
        val userId = authRepository.currentUserId ?: return@launch
        taskRepository.setCompleted(householdId, task.id, userId, !task.completed)
    }
}
