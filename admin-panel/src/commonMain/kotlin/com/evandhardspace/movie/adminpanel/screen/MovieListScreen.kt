package com.evandhardspace.movie.adminpanel.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evandhardspace.movie.adminpanel.AppState
import com.evandhardspace.movie.adminpanel.Screen
import com.evandhardspace.movie.adminpanel.data.ApiResult
import com.evandhardspace.movie.adminpanel.domain.model.Movie
import kotlinx.coroutines.launch

@Composable
fun MovieListScreen(appState: AppState) {
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun loadMovies() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = appState.movieRepository.getMovies()) {
                is ApiResult.Success -> movies = result.data
                is ApiResult.Unauthorized -> appState.onUnauthorized()
                is ApiResult.Error -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadMovies() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { appState.navigateTo(Screen.MovieForm()) }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                errorMessage != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                    Button(onClick = ::loadMovies) { Text("Retry") }
                }
                movies.isEmpty() -> Text("No movies yet", modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(movies) { movie ->
                        MovieListItem(
                            movie = movie,
                            onClick = { appState.navigateTo(Screen.MovieForm(movie)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieListItem(movie: Movie, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(movie.title, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(movie.genre.name, style = MaterialTheme.typography.bodySmall)
                if (movie.rating != null) {
                    Text("${movie.rating} / 10", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
