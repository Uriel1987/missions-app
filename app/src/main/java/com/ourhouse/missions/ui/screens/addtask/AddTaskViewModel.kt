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
    /** True when this screen is editing an existing task instead of creating one. */
    val isEditing: Boolean = false,
    val isLoadingTask: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null
)

/** Backs both "new task" and "edit task" — editing is just the same form pre-filled. */
class AddTaskViewModel(
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _state = MutableStateFlow(AddTaskState())
    val state: StateFlow<AddTaskState> = _state.asStateFlow()

    private var editingTaskId: String? = null
    private var originalAssigneeId: String? = null
    private var originalHadImage = false
    private var imageTouched = false

    fun onTitleChange(value: String) { _state.value = _state.value.copy(title = value) }
    fun onNoteChange(value: String) { _state.value = _state.value.copy(note = value) }
    fun onCategoryChange(value: String?) { _state.value = _state.value.copy(category = value) }
    fun onUrgencyChange(value: Urgency) { _state.value = _state.value.copy(urgency = value) }
    fun onAssigneeChange(id: String?, name: String?) { _state.value = _state.value.copy(assigneeId = id, assigneeName = name) }
    fun onDueDateToggle(enabled: Boolean) { _state.value = _state.value.copy(hasDueDate = enabled) }
    fun onDueDateChange(date: Date?) { _state.value = _state.value.copy(dueAt = date) }
    fun onLinkChange(value: String?) { _state.value = _state.value.copy(linkUrl = value?.takeIf { it.isNotBlank() }) }

    fun loadForEdit(taskId: String) {
        if (editingTaskId == taskId) return
        editingTaskId = taskId
        _state.value = _state.value.copy(isEditing = true, isLoadingTask = true)
        viewModelScope.launch {
            val householdId = sessionManager.householdId.first()
            val task = householdId?.let { taskRepository.getTask(it, taskId) }
            if (householdId == null || task == null) {
                _state.value = _state.value.copy(isLoadingTask = false, errorMessage = "לא ניתן לטעון את המשימה")
                return@launch
            }
            originalAssigneeId = task.assigneeId
            originalHadImage = task.hasImage
            _state.value = AddTaskState(
                title = task.title,
                note = task.note,
                category = task.category,
                urgency = task.urgencyEnum,
                assigneeId = task.assigneeId,
                assigneeName = task.assigneeName,
                dueAt = task.dueAt,
                hasDueDate = task.dueAt != null,
                linkUrl = task.linkUrl,
                imageBase64 = if (task.hasImage) taskRepository.loadImage(householdId, taskId) else null,
                isEditing = true
            )
        }
    }

    fun onImageSelected(uri: Uri) = viewModelScope.launch {
        _state.value = _state.value.copy(isProcessingImage = true, errorMessage = null)
        runCatching {
            withContext(Dispatchers.Default) { ImageCodec.encode(contentResolver, uri) }
        }.onSuccess { encoded ->
            imageTouched = true
            _state.value = _state.value.copy(isProcessingImage = false, imageBase64 = encoded)
        }.onFailure { error ->
            _state.value = _state.value.copy(isProcessingImage = false, errorMessage = error.message ?: "הוספת התמונה נכשלה")
        }
    }

    fun onImageRemoved() {
        imageTouched = true
        _state.value = _state.value.copy(imageBase64 = null)
    }

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

        val taskId = editingTaskId
        val result = if (taskId != null) {
            saveEdit(householdId, taskId, current, userId, userName)
        } else {
            saveNew(householdId, current, userId, userName)
        }
        result
            .onSuccess { _state.value = current.copy(isSaving = false, saved = true) }
            .onFailure { _state.value = current.copy(isSaving = false, errorMessage = it.message) }
    }

    private suspend fun saveNew(householdId: String, current: AddTaskState, userId: String, userName: String?): Result<*> {
        val assigned = current.assigneeId != null
        val task = Task(
            title = current.title.trim(),
            note = current.note.trim(),
            category = current.category,
            urgency = current.urgency.name,
            assigneeId = current.assigneeId,
            assigneeName = current.assigneeName,
            assignedBy = if (assigned) userId else null,
            assignedByName = if (assigned) userName else null,
            assignedAt = if (assigned) Date() else null,
            dueAt = if (current.hasDueDate) current.dueAt else null,
            linkUrl = current.linkUrl,
            createdBy = userId,
            createdByName = userName ?: ""
        )
        return taskRepository.addTask(householdId, task, current.imageBase64)
    }

    private suspend fun saveEdit(householdId: String, taskId: String, current: AddTaskState, userId: String, userName: String?): Result<*> {
        val updates = mutableMapOf<String, Any?>(
            "title" to current.title.trim(),
            "note" to current.note.trim(),
            "category" to current.category,
            "urgency" to current.urgency.name,
            "assigneeId" to current.assigneeId,
            "assigneeName" to current.assigneeName,
            "dueAt" to if (current.hasDueDate) current.dueAt else null,
            "linkUrl" to current.linkUrl
        )
        // Changing who it's assigned to is what makes the new assignee's phone notify.
        if (current.assigneeId != originalAssigneeId) {
            val assigned = current.assigneeId != null
            updates["assignedBy"] = if (assigned) userId else null
            updates["assignedByName"] = if (assigned) userName else null
            updates["assignedAt"] = if (assigned) Date() else null
        }
        val imageChange = when {
            !imageTouched -> TaskRepository.ImageChange.Keep
            current.imageBase64 != null -> TaskRepository.ImageChange.Replace(current.imageBase64)
            originalHadImage -> TaskRepository.ImageChange.Remove
            else -> TaskRepository.ImageChange.Keep
        }
        return taskRepository.editTask(householdId, taskId, updates, imageChange)
    }
}
