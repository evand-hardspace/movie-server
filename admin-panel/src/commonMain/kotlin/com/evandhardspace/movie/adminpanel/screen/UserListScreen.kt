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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import com.evandhardspace.movie.adminpanel.data.ApiResult
import com.evandhardspace.movie.adminpanel.domain.model.User
import com.evandhardspace.movie.adminpanel.domain.model.UserRole
import kotlinx.coroutines.launch

@Composable
fun UserListScreen(appState: AppState) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun loadUsers() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = appState.userRepository.getUsers()) {
                is ApiResult.Success -> users = result.data
                is ApiResult.Unauthorized -> appState.onUnauthorized()
                is ApiResult.Error -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadUsers() }

    Box(Modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            errorMessage != null -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                Button(onClick = ::loadUsers) { Text("Retry") }
            }
            users.isEmpty() -> Text("No users found", modifier = Modifier.align(Alignment.Center))
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(users) { user ->
                    UserListItem(
                        user = user,
                        isSelf = user.id == appState.currentUserId,
                        onRoleChange = { newRole ->
                            coroutineScope.launch {
                                when (appState.userRepository.updateRole(user.id, newRole)) {
                                    is ApiResult.Success -> loadUsers()
                                    is ApiResult.Unauthorized -> appState.onUnauthorized()
                                    is ApiResult.Error -> {}
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun UserListItem(user: User, isSelf: Boolean, onRoleChange: (UserRole) -> Unit) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Box {
                AssistChip(
                    onClick = { if (!isSelf) dropdownExpanded = true },
                    label = { Text(user.role.name) },
                    enabled = !isSelf,
                )
                if (!isSelf) {
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                    ) {
                        UserRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.name) },
                                onClick = {
                                    dropdownExpanded = false
                                    if (role != user.role) onRoleChange(role)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
