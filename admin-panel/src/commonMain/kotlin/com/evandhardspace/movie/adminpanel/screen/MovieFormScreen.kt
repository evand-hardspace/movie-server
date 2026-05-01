package com.evandhardspace.movie.adminpanel.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evandhardspace.movie.adminpanel.AppState
import com.evandhardspace.movie.adminpanel.Screen
import com.evandhardspace.movie.adminpanel.data.ApiResult
import com.evandhardspace.movie.adminpanel.data.MovieRequest
import com.evandhardspace.movie.adminpanel.domain.model.Genre
import com.evandhardspace.movie.adminpanel.domain.model.Movie
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieFormScreen(appState: AppState, movie: Movie?) {
    val isEdit = movie != null
    var title by remember { mutableStateOf(movie?.title ?: "") }
    var description by remember { mutableStateOf(movie?.description ?: "") }
    var genre by remember { mutableStateOf(movie?.genre ?: Genre.ACTION) }
    var rating by remember { mutableStateOf(movie?.rating?.toString() ?: "") }
    var photoUrl by remember { mutableStateOf(movie?.photoUrl ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var genreExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Movie" else "Add Movie") },
                navigationIcon = {
                    IconButton(onClick = { appState.navigateTo(Screen.MovieList) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            ExposedDropdownMenuBox(
                expanded = genreExpanded,
                onExpandedChange = { genreExpanded = it },
            ) {
                OutlinedTextField(
                    value = genre.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Genre") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(genreExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = genreExpanded,
                    onDismissRequest = { genreExpanded = false },
                ) {
                    Genre.entries.forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g.name) },
                            onClick = { genre = g; genreExpanded = false },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = rating,
                onValueChange = { rating = it },
                label = { Text("Rating (0–10)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = photoUrl,
                onValueChange = { photoUrl = it },
                label = { Text("Photo URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        val request = MovieRequest(
                            title = title.trim(),
                            description = description.trim().takeIf { it.isNotBlank() },
                            genre = genre,
                            rating = rating.trim().toDoubleOrNull(),
                            photoUrl = photoUrl.trim().takeIf { it.isNotBlank() },
                        )
                        val result = if (isEdit)
                            appState.movieRepository.updateMovie(movie.id, request)
                        else
                            appState.movieRepository.createMovie(request)
                        when (result) {
                            is ApiResult.Success -> appState.navigateTo(Screen.MovieList)
                            is ApiResult.Unauthorized -> appState.onUnauthorized()
                            is ApiResult.Error -> errorMessage = result.message
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading && title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(if (isEdit) "Save Changes" else "Create Movie")
                }
            }
        }
    }
}
