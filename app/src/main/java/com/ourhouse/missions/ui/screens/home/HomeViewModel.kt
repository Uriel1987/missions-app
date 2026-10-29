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

/** DATE: soonest due date first (no date last). URGENCY: most urgent first, then soonest due. */
enum class HomeSort { DATE, URGENCY }

private data class ViewOptions(
    val filter: HomeFilter = HomeFilter.ALL,
    val sort: HomeSort = HomeSort.DATE,
    val showCompleted: Boolean = false
)

data class HomeState(
    val tasks: List<Task> = emptyList(),
    val filter: HomeFilter = HomeFilter.ALL,
    val sort: HomeSort = HomeSort.DATE,
    /** false = the active list, true = the list of tasks already marked done. */
    val showCompleted: Boolean = false,
    val completedCount: Int = 0,
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

    private val options = MutableStateFlow(ViewOptions())
    private val allTasks = MutableStateFlow<List<Task>>(emptyList())
    private val isLoading = MutableStateFlow(true)

    private val errorMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<HomeState> = combine(allTasks, options, isLoading, errorMessage) { tasks, view, loading, error ->
        val userId = authRepository.currentUserId
        val byCompletion = tasks.filter { it.completed == view.showCompleted }
        val visible = when (view.filter) {
            HomeFilter.ALL -> byCompletion
            HomeFilter.MINE -> byCompletion.filter { it.assigneeId == userId }
            HomeFilter.PARTNER -> byCompletion.filter { it.assigneeId != null && it.assigneeId != userId }
            HomeFilter.UNASSIGNED -> byCompletion.filter { it.assigneeId == null }
        }
        // "Unread" here means: something was just handed off to me — a good-enough
        // signal without a dedicated read/unread tracking collection.
        val hasUnread = tasks.any { !it.completed && it.lastHandoffAt != null && it.assigneeId == userId }
        HomeState(
            tasks = visible.sortedWith(comparatorFor(view.sort)),
            filter = view.filter, sort = view.sort, showCompleted = view.showCompleted,
            completedCount = tasks.count { it.completed },
            currentUserId = userId,
            isLoading = loading, errorMessage = error, hasUnreadHandoff = hasUnread
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

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

    fun setFilter(newFilter: HomeFilter) { options.update { it.copy(filter = newFilter) } }
    fun setSort(newSort: HomeSort) { options.update { it.copy(sort = newSort) } }
    fun setShowCompleted(show: Boolean) { options.update { it.copy(showCompleted = show) } }

    fun toggleCompleted(task: Task) = viewModelScope.launch {
        val householdId = sessionManager.householdId.first() ?: return@launch
        val userId = authRepository.currentUserId ?: return@launch
        taskRepository.setCompleted(householdId, task.id, userId, !task.completed)
    }
}

internal fun comparatorFor(sort: HomeSort): Comparator<Task> {
    // Tasks without a due date always sort after those with one.
    val byDueDate = compareBy<Task> { it.dueAt == null }.thenBy { it.dueAt }
    val newestFirst = compareByDescending<Task> { it.createdAt }
    return when (sort) {
        HomeSort.DATE -> byDueDate.then(newestFirst)
        HomeSort.URGENCY -> compareBy<Task> { it.urgencyEnum.ordinal }.then(byDueDate).then(newestFirst)
    }
}
