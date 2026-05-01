package com.evandhardspace.movie.adminpanel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.evandhardspace.movie.adminpanel.data.ApiResult
import com.evandhardspace.movie.adminpanel.data.AuthRepository
import com.evandhardspace.movie.adminpanel.data.MovieRepository
import com.evandhardspace.movie.adminpanel.data.UserRepository
import com.evandhardspace.movie.adminpanel.domain.model.Movie
import com.evandhardspace.movie.adminpanel.domain.model.UserRole
import com.evandhardspace.movie.adminpanel.storage.TokenStorage

sealed class Screen {
    data object Login : Screen()
    data object MovieList : Screen()
    data class MovieForm(val movie: Movie? = null) : Screen()
    data object UserList : Screen()
}

class AppState(
    private val tokenStorage: TokenStorage,
    val userRepository: UserRepository,
    val authRepository: AuthRepository,
    val movieRepository: MovieRepository,
) {
    var currentScreen: Screen by mutableStateOf(
        if (tokenStorage.getAccessToken() != null) Screen.MovieList else Screen.Login
    )
        private set

    var userRole: UserRole? by mutableStateOf(null)
        private set

    suspend fun initialize() {
        if (tokenStorage.getAccessToken() == null) return
        when (val result = userRepository.getMe()) {
            is ApiResult.Success -> userRole = result.data.role
            else -> onUnauthorized()
        }
    }

    suspend fun loginUser(email: String, password: String): String? {
        return when (val loginResult = authRepository.login(email, password)) {
            is ApiResult.Success -> when (val meResult = userRepository.getMe()) {
                is ApiResult.Success -> { onLoggedIn(meResult.data.role); null }
                is ApiResult.Unauthorized -> { onUnauthorized(); "Session expired, please try again" }
                is ApiResult.Error -> meResult.message
            }
            is ApiResult.Unauthorized -> "Invalid email or password"
            is ApiResult.Error -> loginResult.message
        }
    }

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun onLoggedIn(role: UserRole) {
        userRole = role
        currentScreen = Screen.MovieList
    }

    fun onUnauthorized() {
        tokenStorage.clear()
        userRole = null
        currentScreen = Screen.Login
    }
}
