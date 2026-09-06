package com.pratone.app.ui.navigation

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pratone.app.domain.model.Song
import com.pratone.app.ui.AppViewModel
import com.pratone.app.ui.home.HomeScreen
import com.pratone.app.ui.library.LibraryScreen
import com.pratone.app.ui.nowplaying.NowPlayingScreen
import com.pratone.app.ui.settings.SettingsScreen
import com.pratone.app.ui.theme.Accent

private sealed class Destination(val route: String, val label: String) {
    data object Home : Destination("home", "Home")
    data object Library : Destination("library", "Library")
    data object Settings : Destination("settings", "Settings")
    data object NowPlaying : Destination("now_playing", "Now Playing")
}

private val bottomBarDestinations = listOf(Destination.Home, Destination.Library, Destination.Settings)

@Composable
fun PratoneNavGraph(viewModel: AppViewModel, onNeedsMicRationale: () -> Unit) {
    val navController = rememberNavController()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { PratoneBottomBar(navController) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenLibrary = { navController.navigate(Destination.Library.route) },
                    modifier = Modifier.padding(padding),
                )
            }
            composable(Destination.Library.route) {
                LibraryScreen(
                    viewModel = viewModel,
                    onSongSelected = { song, queue ->
                        viewModel.playSong(song, queue)
                        navController.navigate(Destination.NowPlaying.route)
                    },
                    modifier = Modifier.padding(padding),
                )
            }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNeedsMicRationale = onNeedsMicRationale,
                    modifier = Modifier.padding(padding),
                )
            }
            composable(
                Destination.NowPlaying.route,
                enterTransition = { slideInVertically(initialOffsetY = { it }) },
                exitTransition = { slideOutVertically(targetOffsetY = { it }) },
            ) {
                NowPlayingScreen(viewModel = viewModel, onClose = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun PratoneBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        bottomBarDestinations.forEach { destination ->
            val selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        when (destination) {
                            Destination.Home -> Icons.Filled.Home
                            Destination.Library -> Icons.Filled.LibraryMusic
                            else -> Icons.Filled.Settings
                        },
                        contentDescription = destination.label,
                    )
                },
                label = { Text(destination.label) },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = Accent,
                    selectedTextColor = Accent,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}
