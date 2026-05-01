package com.evandhardspace.movie.adminpanel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.evandhardspace.movie.adminpanel.data.ApiClient
import com.evandhardspace.movie.adminpanel.data.AuthRepository
import com.evandhardspace.movie.adminpanel.data.MovieRepository
import com.evandhardspace.movie.adminpanel.data.UserRepository
import com.evandhardspace.movie.adminpanel.domain.model.UserRole
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
        is Screen.Login -> LoginScreenPlaceholder()
        else -> AuthenticatedContent(appState)
    }
}

@Composable
private fun AuthenticatedContent(appState: AppState) {
    val currentScreen = appState.currentScreen
    val isSuperAdmin = appState.userRole == UserRole.SUPER_ADMIN

    if (currentScreen is Screen.MovieForm) {
        // AP13: replace with MovieFormScreen(appState, currentScreen.movieId)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Movie Form — id: ${currentScreen.movieId ?: "new"}")
        }
        return
    }

    val selectedTab = if (currentScreen is Screen.UserList) 1 else 0

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxSize()) {
            when (currentScreen) {
                is Screen.MovieList -> {
                    // AP12: replace with MovieListScreen(appState)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Movie List")
                    }
                }
                is Screen.UserList -> {
                    // AP14: replace with UserListScreen(appState)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("User List")
                    }
                }
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

@Composable
private fun LoginScreenPlaceholder() {
    // AP11: replace with LoginScreen(appState)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Login Screen")
    }
}
