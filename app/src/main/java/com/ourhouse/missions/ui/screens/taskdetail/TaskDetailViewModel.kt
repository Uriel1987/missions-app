package com.ourhouse.missions.ui.screens.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourhouse.missions.data.local.SessionManager
import com.ourhouse.missions.data.model.Task
import com.ourhouse.missions.data.repository.AuthRepository
import com.ourhouse.missions.data.repository.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TaskDetailState(
    val task: Task? = null,
    /** The task's photo (base64), fetched separately from the task itself. */
    val imageBase64: String? = null,
    val handoffMessage: String = "",
    val isSending: Boolean = false,
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val currentUserId: String? = null,
    val errorMessage: String? = null
)

class TaskDetailViewModel(
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(TaskDetailState(currentUserId = authRepository.currentUserId))
    val state: StateFlow<TaskDetailState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var loadedImageRev: Long? = null

    fun load(taskId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val householdId = sessionManager.householdId.filterNotNull().first()
            // Simple one-shot + live listener via the same collection the repository already watches.
            runCatching {
                taskRepository.observeTasks(householdId).collect { tasks ->
                    val task = tasks.firstOrNull { it.id == taskId }
                    // A task that disappears (deleted here or on the partner's phone) sends us back.
                    _state.value = _state.value.copy(task = task, deleted = _state.value.deleted || task == null, errorMessage = null)
                    when {
                        task == null || !task.hasImage -> {
                            loadedImageRev = null
                            _state.value = _state.value.copy(imageBase64 = null)
                        }
                        loadedImageRev != task.imageRev -> {
                            loadedImageRev = task.imageRev
                            _state.value = _state.value.copy(imageBase64 = taskRepository.loadImage(householdId, taskId))
                        }
                    }
                }
            }.onFailure { error ->
                _state.value = _state.value.copy(errorMessage = error.message ?: "שגיאה בטעינת המשימה")
            }
        }
    }

    fun onHandoffMessageChange(value: String) { _state.value = _state.value.copy(handoffMessage = value) }

    /** The core "pass responsibility" action: reassigns the task to [toUserId] and
     *  attaches the optional note — the partner's phone notices this write and shows
     *  a local notification (see notifications/TaskNotifier). */
    fun passResponsibility(toUserId: String, toUserName: String?) = viewModelScope.launch {
        val task = _state.value.task ?: return@launch
        val fromUserId = authRepository.currentUserId ?: return@launch
        val fromUserName = sessionManager.displayName.first()
        val householdId = sessionManager.householdId.first() ?: return@launch

        _state.value = _state.value.copy(isSending = true, errorMessage = null)
        taskRepository.passResponsibility(
            householdId, task.id, fromUserId, fromUserName, toUserId, toUserName,
            message = _state.value.handoffMessage.trim().ifBlank { null }
        ).onSuccess {
            _state.value = _state.value.copy(isSending = false, handoffMessage = "")
        }.onFailure { error ->
            _state.value = _state.value.copy(isSending = false, errorMessage = error.message ?: "שליחת ההעברה נכשלה")
        }
    }

    fun toggleCompleted() = viewModelScope.launch {
        val task = _state.value.task ?: return@launch
        val userId = authRepository.currentUserId ?: return@launch
        val householdId = sessionManager.householdId.first() ?: return@launch
        taskRepository.setCompleted(householdId, task.id, userId, !task.completed)
            .onFailure { _state.value = _state.value.copy(errorMessage = it.message ?: "העדכון נכשל") }
    }

    fun deleteTask() = viewModelScope.launch {
        val task = _state.value.task ?: return@launch
        val householdId = sessionManager.householdId.first() ?: return@launch
        _state.value = _state.value.copy(isDeleting = true, errorMessage = null)
        taskRepository.deleteTask(householdId, task.id)
            .onSuccess { _state.value = _state.value.copy(isDeleting = false, deleted = true) }
            .onFailure { _state.value = _state.value.copy(isDeleting = false, errorMessage = it.message ?: "המחיקה נכשלה") }
    }
}
