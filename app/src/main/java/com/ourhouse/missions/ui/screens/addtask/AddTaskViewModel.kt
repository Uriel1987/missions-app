package com.ourhouse.missions.ui.screens.addtask

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourhouse.missions.data.local.SessionManager
import com.ourhouse.missions.data.model.Task
import com.ourhouse.missions.data.model.Urgency
import com.ourhouse.missions.data.repository.AuthRepository
import com.ourhouse.missions.data.repository.TaskRepository
import com.ourhouse.missions.util.ImageCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

/** null [assigneeId] means "ללא שיוך"; null [dueAt] means no fixed date/time — both
 *  optional per the product spec. */
data class AddTaskState(
    val title: String = "",
    val note: String = "",
    val category: String? = "home",
    val urgency: Urgency = Urgency.MEDIUM,
    val assigneeId: String? = null,
    val assigneeName: String? = null,
    val dueAt: Date? = null,
    val hasDueDate: Boolean = false,
    val linkUrl: String? = null,
    /** Already-compressed photo, base64 — see [ImageCodec]. */
    val imageBase64: String? = null,
    val isProcessingImage: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null
)

class AddTaskViewModel(
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _state = MutableStateFlow(AddTaskState())
    val state: StateFlow<AddTaskState> = _state.asStateFlow()

    fun onTitleChange(value: String) { _state.value = _state.value.copy(title = value) }
    fun onNoteChange(value: String) { _state.value = _state.value.copy(note = value) }
    fun onCategoryChange(value: String?) { _state.value = _state.value.copy(category = value) }
    fun onUrgencyChange(value: Urgency) { _state.value = _state.value.copy(urgency = value) }
    fun onAssigneeChange(id: String?, name: String?) { _state.value = _state.value.copy(assigneeId = id, assigneeName = name) }
    fun onDueDateToggle(enabled: Boolean) { _state.value = _state.value.copy(hasDueDate = enabled) }
    fun onDueDateChange(date: Date?) { _state.value = _state.value.copy(dueAt = date) }
    fun onLinkChange(value: String?) { _state.value = _state.value.copy(linkUrl = value?.takeIf { it.isNotBlank() }) }

    fun onImageSelected(uri: Uri) = viewModelScope.launch {
        _state.value = _state.value.copy(isProcessingImage = true, errorMessage = null)
        runCatching {
            withContext(Dispatchers.Default) { ImageCodec.encode(contentResolver, uri) }
        }.onSuccess { encoded ->
            _state.value = _state.value.copy(isProcessingImage = false, imageBase64 = encoded)
        }.onFailure { error ->
            _state.value = _state.value.copy(isProcessingImage = false, errorMessage = error.message ?: "הוספת התמונה נכשלה")
        }
    }

    fun onImageRemoved() { _state.value = _state.value.copy(imageBase64 = null) }

    fun save() = viewModelScope.launch {
        val current = _state.value
        if (current.title.isBlank()) {
            _state.value = current.copy(errorMessage = "נא להזין כותרת למשימה")
            return@launch
        }
        _state.value = current.copy(isSaving = true, errorMessage = null)

        val householdId = sessionManager.householdId.first()
        val userId = authRepository.currentUserId
        val userName = sessionManager.displayName.first()
        if (householdId == null || userId == null) {
            _state.value = current.copy(isSaving = false, errorMessage = "לא מחובר לבית משותף")
            return@launch
        }

        val task = Task(
            title = current.title.trim(),
            note = current.note.trim(),
            category = current.category,
            urgency = current.urgency.name,
            assigneeId = current.assigneeId,
            assigneeName = current.assigneeName,
            dueAt = if (current.hasDueDate) current.dueAt else null,
            linkUrl = current.linkUrl,
            createdBy = userId,
            createdByName = userName ?: ""
        )

        taskRepository.addTask(householdId, task, current.imageBase64)
            .onSuccess { _state.value = current.copy(isSaving = false, saved = true) }
            .onFailure { _state.value = current.copy(isSaving = false, errorMessage = it.message) }
    }
}
