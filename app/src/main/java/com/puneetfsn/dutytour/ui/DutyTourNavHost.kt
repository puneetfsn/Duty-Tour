package com.puneetfsn.dutytour.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.puneetfsn.dutytour.DutyTourApp
import com.puneetfsn.dutytour.ui.detail.TourDetailScreen
import com.puneetfsn.dutytour.ui.detail.TourDetailViewModel
import com.puneetfsn.dutytour.ui.edit.TourEditScreen
import com.puneetfsn.dutytour.ui.edit.TourEditViewModel
import com.puneetfsn.dutytour.ui.list.TourListScreen
import com.puneetfsn.dutytour.ui.list.TourListViewModel
import com.puneetfsn.dutytour.ui.settings.SettingsScreen
import com.puneetfsn.dutytour.ui.settings.SettingsViewModel

object Routes {
    const val LIST = "tours"
    const val DETAIL = "tour/{tourId}"
    const val EDIT = "tour/edit?tourId={tourId}"
    const val SETTINGS = "settings"

    fun detail(tourId: Long) = "tour/$tourId"
    fun edit(tourId: Long?) = "tour/edit?tourId=${tourId ?: -1L}"
}

@Composable
fun DutyTourNavHost() {
    val app = LocalContext.current.applicationContext as DutyTourApp
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LIST) {

        composable(Routes.LIST) {
            val vm: TourListViewModel = viewModel(factory = TourListViewModel.factory(app.tourRepository))
            TourListScreen(
                viewModel = vm,
                onAddTour = { navController.navigate(Routes.edit(null)) },
                onOpenTour = { id -> navController.navigate(Routes.detail(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument("tourId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { entry ->
            val tourId = entry.arguments?.getLong("tourId") ?: -1L
            val vm: TourEditViewModel = viewModel(
                key = "edit-$tourId",
                factory = TourEditViewModel.factory(
                    repository = app.tourRepository,
                    settingsRepository = app.settingsRepository,
                    tourId = tourId
                )
            )
            TourEditScreen(
                viewModel = vm,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("tourId") { type = NavType.LongType })
        ) { entry ->
            val tourId = entry.arguments?.getLong("tourId") ?: -1L
            val vm: TourDetailViewModel = viewModel(
                key = "detail-$tourId",
                factory = TourDetailViewModel.factory(app.tourRepository, tourId)
            )
            TourDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.edit(tourId)) }
            )
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app.settingsRepository))
            SettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
}
