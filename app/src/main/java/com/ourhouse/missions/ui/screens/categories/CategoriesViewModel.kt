package com.ourhouse.missions.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourhouse.missions.data.local.SessionManager
import com.ourhouse.missions.data.model.Category
import com.ourhouse.missions.data.model.Task
import com.ourhouse.missions.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** One row in the browse-by-category list: a built-in [Category], or null for tasks
 *  whose category is either unset or a free-text one the couple typed in themselves. */
data class CategoryGroup(
    val category: Category?,
    val customLabel: String?,
    val tasks: List<Task>
)

data class CategoriesState(
    val groups: List<CategoryGroup> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/** Backs the "Categories" tab — same task stream as Home, but grouped by category
 *  instead of filtered by assignee, so it doesn't share state with [HomeViewModel]. */
class CategoriesViewModel(
    private val taskRepository: TaskRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(CategoriesState())
    val state: StateFlow<CategoriesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val householdId = sessionManager.householdId.filterNotNull().first()
            runCatching {
                taskRepository.observeTasks(householdId).collect { tasks ->
                    _state.value = CategoriesState(groups = groupByCategory(tasks), isLoading = false)
                }
            }.onFailure { error ->
                _state.value = _state.value.copy(isLoading = false, errorMessage = error.message ?: "שגיאה בטעינת המשימות")
            }
        }
    }

    private fun groupByCategory(tasks: List<Task>): List<CategoryGroup> {
        val byKey = tasks.groupBy { it.category }

        val builtIn = Category.entries.mapNotNull { category ->
            byKey[category.key]?.let { CategoryGroup(category, null, it) }
        }

        // Free-text categories the couple typed in themselves (not one of the built-ins,
        // and not null/blank) each get their own group, sorted for a stable order.
        val custom = byKey
            .filterKeys { key -> !key.isNullOrBlank() && Category.fromKey(key) == null }
            .toSortedMap(compareBy { it })
            .map { (key, tasksInGroup) -> CategoryGroup(null, key, tasksInGroup) }

        val uncategorized = byKey[null]?.let { listOf(CategoryGroup(null, null, it)) }.orEmpty()

        return builtIn + custom + uncategorized
    }
}
