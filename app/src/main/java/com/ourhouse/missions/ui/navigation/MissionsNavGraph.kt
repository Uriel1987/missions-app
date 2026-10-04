package com.ourhouse.missions.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ourhouse.missions.AppContainer
import com.ourhouse.missions.data.model.Household
import com.ourhouse.missions.ui.ViewModelFactory
import com.ourhouse.missions.ui.components.NavDestination
import com.ourhouse.missions.ui.screens.addtask.AddTaskScreen
import com.ourhouse.missions.ui.screens.addtask.AddTaskViewModel
import com.ourhouse.missions.ui.screens.categories.CategoriesScreen
import com.ourhouse.missions.ui.screens.categories.CategoriesViewModel
import com.ourhouse.missions.ui.screens.home.HomeScreen
import com.ourhouse.missions.ui.screens.home.HomeViewModel
import com.ourhouse.missions.ui.screens.household.HouseholdScreen
import com.ourhouse.missions.ui.screens.household.HouseholdViewModel
import com.ourhouse.missions.ui.screens.notifications.NotificationsScreen
import com.ourhouse.missions.ui.screens.pairing.PairingScreen
import com.ourhouse.missions.ui.screens.pairing.PairingViewModel
import com.ourhouse.missions.ui.screens.taskdetail.TaskDetailScreen
import com.ourhouse.missions.ui.screens.taskdetail.TaskDetailViewModel
import kotlinx.coroutines.flow.combine

private object Routes {
    const val PAIRING = "pairing"
    const val HOME = "home"
    const val ADD_TASK = "add_task"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val NOTIFICATIONS = "notifications"
    const val HOUSEHOLD = "household"
    const val CATEGORIES = "categories"
    fun taskDetail(id: String) = "task_detail/$id"
}

private data class Session(val householdId: String?, val displayName: String?)

@Composable
fun MissionsNavGraph(container: AppContainer) {
    val navController = rememberNavController()
    val factory = remember { ViewModelFactory(container) }

    // `null` means "not read from disk yet" — distinct from a loaded session whose
    // fields are null (a fresh install), which must show the pairing screen, not a blank one.
    // Collected live so the household is picked up right after pairing, not only on restart.
    val session by remember {
        combine(container.sessionManager.householdId, container.sessionManager.displayName) { id, name ->
            Session(id, name)
        }
    }.collectAsState(initial = null)
    val loadedSession = session ?: return
    val householdId = loadedSession.householdId
    val displayName = loadedSession.displayName

    NavHost(
        navController = navController,
        startDestination = if (householdId == null) Routes.PAIRING else Routes.HOME
    ) {
        composable(Routes.PAIRING) {
            val vm: PairingViewModel = viewModel(factory = factory)
            PairingScreen(
                viewModel = vm,
                onPaired = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.PAIRING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = vm,
                onAddTask = { navController.navigate(Routes.ADD_TASK) },
                onOpenTask = { taskId -> navController.navigate(Routes.taskDetail(taskId)) },
                onNavigate = { dest -> navController.navigateToBottomTab(dest) }
            )
        }

        composable(Routes.ADD_TASK) {
            val vm: AddTaskViewModel = viewModel(factory = factory)
            val household by rememberHousehold(container, householdId)
            val myId = container.authRepository.currentUserId ?: ""
            val partner = partnerOf(household, myId)
            AddTaskScreen(
                viewModel = vm,
                myUserId = myId,
                myName = displayName ?: "אני",
                partnerId = partner?.first,
                partnerName = partner?.second,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            val vm: TaskDetailViewModel = viewModel(factory = factory)
            val household by rememberHousehold(container, householdId)
            val myId = container.authRepository.currentUserId ?: ""
            val partner = partnerOf(household, myId)
            TaskDetailScreen(
                viewModel = vm,
                taskId = taskId,
                partnerId = partner?.first ?: "",
                partnerName = partner?.second ?: "בן/בת הזוג",
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.NOTIFICATIONS) {
            val homeVm: HomeViewModel = viewModel(factory = factory)
            val state by homeVm.state.collectAsState()
            NotificationsScreen(
                tasks = state.tasks,
                onNavigate = { dest -> navController.navigateToBottomTab(dest) }
            )
        }

        composable(Routes.HOUSEHOLD) {
            val vm: HouseholdViewModel = viewModel(factory = factory)
            HouseholdScreen(
                viewModel = vm,
                onNavigate = { dest -> navController.navigateToBottomTab(dest) },
                onLeft = {
                    navController.navigate(Routes.PAIRING) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CATEGORIES) {
            val vm: CategoriesViewModel = viewModel(factory = factory)
            CategoriesScreen(
                viewModel = vm,
                onOpenTask = { taskId -> navController.navigate(Routes.taskDetail(taskId)) },
                onNavigate = { dest -> navController.navigateToBottomTab(dest) }
            )
        }
    }
}

/** Switches between the bottom-nav tabs without piling up back-stack entries —
 *  each tab keeps its own saved state (scroll position, etc.) across switches. */
private fun NavHostController.navigateToBottomTab(dest: NavDestination) {
    val route = when (dest) {
        NavDestination.HOME -> Routes.HOME
        NavDestination.NOTIFICATIONS -> Routes.NOTIFICATIONS
        NavDestination.HOUSEHOLD -> Routes.HOUSEHOLD
        NavDestination.CATEGORIES -> Routes.CATEGORIES
    }
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun rememberHousehold(container: AppContainer, householdId: String?) =
    produceState<Household?>(initialValue = null, householdId) {
        if (householdId != null) {
            runCatching {
                container.householdRepository.observeHousehold(householdId).collect { value = it }
            }
        }
    }

/** Returns (uid, displayName) for whichever member isn't [myId]. */
private fun partnerOf(household: Household?, myId: String): Pair<String, String>? {
    val partnerId = household?.memberIds?.firstOrNull { it != myId } ?: return null
    val partnerName = household.memberNames[partnerId] ?: "בן/בת הזוג"
    return partnerId to partnerName
}
