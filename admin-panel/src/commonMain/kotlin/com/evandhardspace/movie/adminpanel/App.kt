package com.evandhardspace.movie.adminpanel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.evandhardspace.movie.adminpanel.data.ApiClient
import com.evandhardspace.movie.adminpanel.data.AuthRepository
import com.evandhardspace.movie.adminpanel.data.MovieRepository
import com.evandhardspace.movie.adminpanel.data.UserRepository
import com.evandhardspace.movie.adminpanel.domain.model.UserRole
import com.evandhardspace.movie.adminpanel.screen.LoginScreen
import com.evandhardspace.movie.adminpanel.screen.MovieFormScreen
import com.evandhardspace.movie.adminpanel.screen.MovieListScreen
import com.evandhardspace.movie.adminpanel.screen.UserListScreen
import com.evandhardspace.movie.adminpanel.storage.TokenStorage

@Composable
fun App() {
    val tokenStorage = remember { TokenStorage() }
    val apiClient = remember { ApiClient(apiBaseUrl, tokenStorage) }
    val appState = remember {
        AppState(
            tokenStorage = tokenStorage,
            userRepository = UserRepository(apiClient),
            authRepository = AuthRepository(apiClient, tokenStorage),
            movieRepository = MovieRepository(apiClient),
        )
    }

    LaunchedEffect(Unit) { appState.initialize() }

    MaterialTheme {
        AppContent(appState)
    }
}

@Composable
private fun AppContent(appState: AppState) {
    when (appState.currentScreen) {
        is Screen.Login -> LoginScreen(appState)
        else -> AuthenticatedContent(appState)
    }
}

@Composable
private fun AuthenticatedContent(appState: AppState) {
    val currentScreen = appState.currentScreen
    val isSuperAdmin = appState.userRole == UserRole.SUPER_ADMIN

    if (currentScreen is Screen.MovieForm) {
        MovieFormScreen(appState, currentScreen.movie)
        return
    }

    val selectedTab = if (currentScreen is Screen.UserList) 1 else 0

    Column(androidx.compose.ui.Modifier.fillMaxSize()) {
        Box(androidx.compose.ui.Modifier.weight(1f).fillMaxSize()) {
            when (currentScreen) {
                is Screen.MovieList -> MovieListScreen(appState)
                is Screen.UserList -> UserListScreen(appState)
                else -> {}
            }
        }

        if (isSuperAdmin) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { appState.navigateTo(Screen.MovieList) },
                    text = { Text("Movies") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { appState.navigateTo(Screen.UserList) },
                    text = { Text("Users") },
                )
            }
        }
    }
}
