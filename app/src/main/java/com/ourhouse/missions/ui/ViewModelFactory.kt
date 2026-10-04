package com.ourhouse.missions.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.ourhouse.missions.AppContainer
import com.ourhouse.missions.ui.screens.addtask.AddTaskViewModel
import com.ourhouse.missions.ui.screens.categories.CategoriesViewModel
import com.ourhouse.missions.ui.screens.home.HomeViewModel
import com.ourhouse.missions.ui.screens.household.HouseholdViewModel
import com.ourhouse.missions.ui.screens.pairing.PairingViewModel
import com.ourhouse.missions.ui.screens.taskdetail.TaskDetailViewModel

/** Tiny factory so screens can request a ViewModel wired to the shared [AppContainer]
 *  without a full DI framework. */
class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return when (modelClass) {
            PairingViewModel::class.java -> PairingViewModel(
                container.authRepository, container.householdRepository, container.sessionManager
            ) as T
            HomeViewModel::class.java -> HomeViewModel(
                container.authRepository, container.taskRepository, container.sessionManager
            ) as T
            AddTaskViewModel::class.java -> AddTaskViewModel(
                container.taskRepository, container.authRepository, container.sessionManager, container.contentResolver
            ) as T
            TaskDetailViewModel::class.java -> TaskDetailViewModel(
                container.taskRepository, container.authRepository, container.sessionManager
            ) as T
            HouseholdViewModel::class.java -> HouseholdViewModel(
                container.householdRepository, container.authRepository, container.sessionManager
            ) as T
            CategoriesViewModel::class.java -> CategoriesViewModel(
                container.taskRepository, container.sessionManager
            ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
