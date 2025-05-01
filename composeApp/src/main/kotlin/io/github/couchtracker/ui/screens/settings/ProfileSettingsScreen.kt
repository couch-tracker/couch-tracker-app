package io.github.couchtracker.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.LocalProfileManagerContext
import io.github.couchtracker.R
import io.github.couchtracker.db.app.AppData
import io.github.couchtracker.db.app.ProfileManager.ProfileInfo
import io.github.couchtracker.db.profile.ExternalProfileDb
import io.github.couchtracker.db.profile.ManagedProfileDb
import io.github.couchtracker.db.profile.ProfileDbResult
import io.github.couchtracker.db.profile.ProfileDbUtils
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.formattedLastModified
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.toJavaUri
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.saket.bytesize.BinaryByteSize
import me.zhanghai.compose.preference.Preference
import org.koin.compose.koinInject

@Serializable
data class ProfileSettingsScreen(val id: Long) : Screen {
    @Composable
    override fun content() {
        val navController = LocalNavController.current
        val profileManager = LocalProfileManagerContext.current
        val profileInfo = profileManager.profiles.singleOrNull { it.profile.id == id }

        if (profileInfo != null) {
            Content(profileInfo)
        } else {
            LaunchedEffect(Unit) {
                navController.navigateUp()
            }
        }
    }
}

@Composable
private fun Content(profileInfo: ProfileInfo) {
    val profileManager = LocalProfileManagerContext.current

    BaseSettings(R.string.profile.str()) {
        item("name") { ProfileNamePreference(profileInfo) }
        item("location") { ProfileLocationPreference(profileInfo) }
        item("file-name") { ProfileFileName(profileInfo) }
        item("last-used") { ProfileLastUsedPreference(profileInfo) }
        item("size") { ProfileSizePreference(profileInfo) }
        item("divider") { HorizontalDivider() }
        item("delete") { DeleteProfilePreference(profileInfo) }
    }
}

@Composable
private fun ProfileNamePreference(profileInfo: ProfileInfo) {
    val appDb = koinInject<AppData>()
    var dialogOpen by remember { mutableStateOf(false) }

    Preference(
        title = { Text(R.string.profile_name.str()) },
        icon = { Icon(Icons.Outlined.Badge, contentDescription = null) },
        summary = { Text(profileInfo.profile.name) },
        onClick = { dialogOpen = true },
    )

    if (dialogOpen) {
        ProfileNameDialog(
            initialName = profileInfo.profile.name,
            onOk = { appDb.profileQueries.setName(name = it, id = profileInfo.profile.id) },
            onDismissRequest = { dialogOpen = false },
        )
    }
}

private sealed interface LocationChangeDialogState {
    data object Idle : LocationChangeDialogState
    data object Open : LocationChangeDialogState
    data object Moving : LocationChangeDialogState

    sealed interface Complete : LocationChangeDialogState {
        data object Success : Complete
        data class Error(val moveStatus: ProfileDbResult.AnyError) : Complete

        companion object {
            fun from(result: ProfileDbResult<Unit>) = when (result) {
                is ProfileDbResult.Completed.Success -> Success
                is ProfileDbResult.AnyError -> Error(result)
            }
        }
    }
}

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun ProfileLocationPreference(profileInfo: ProfileInfo) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var state: LocationChangeDialogState by remember { mutableStateOf(LocationChangeDialogState.Idle) }

    val takeOwnershipWorkflow = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ProfileDbUtils.MIME_TYPE)) { uri ->
        if (uri != null && profileInfo.db is ManagedProfileDb) {
            state = LocationChangeDialogState.Moving
            coroutineScope.launch {
                state = LocationChangeDialogState.Complete.from(profileInfo.db.moveToExternalDb(context, uri.toJavaUri()))
            }
        }
    }

    Preference(
        title = { Text(R.string.profile_location.str()) },
        icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
        summary = {
            Text(
                text = when (profileInfo.db) {
                    is ManagedProfileDb -> R.string.profile_location_internal_app_storage.str()
                    is ExternalProfileDb -> profileInfo.db.externalDb.uri.toString()
                },
            )
        },
        onClick = { state = LocationChangeDialogState.Open },
    )

    if (state != LocationChangeDialogState.Idle) {
        val isMoving = state == LocationChangeDialogState.Moving
        AlertDialog(
            icon = {
                Icon(
                    imageVector = when (state) {
                        is LocationChangeDialogState.Complete.Success -> Icons.Default.Check
                        is LocationChangeDialogState.Complete.Error -> Icons.Default.Error
                        else -> Icons.AutoMirrored.Outlined.DriveFileMove
                    },
                    contentDescription = null,
                )
            },
            title = {
                Text(
                    text = when (state) {
                        is LocationChangeDialogState.Complete.Success -> R.string.move_profile_location_success
                        is LocationChangeDialogState.Complete.Error -> R.string.move_profile_location_error
                        else -> when (profileInfo.db) {
                            is ManagedProfileDb -> R.string.move_profile_location_out_of_app_storage
                            is ExternalProfileDb -> R.string.move_profile_location_to_app_storage
                        }
                    }.str(),
                )
            },
            text = {
                when (val state = state) {
                    LocationChangeDialogState.Idle -> {}
                    LocationChangeDialogState.Open -> {
                        val profile = profileInfo.profile
                        Text(
                            text = when (profileInfo.db) {
                                is ManagedProfileDb -> R.string.move_profile_location_out_of_app_storage_message.str(profile.name)
                                is ExternalProfileDb -> R.string.move_profile_location_to_app_storage_message.str(profile.name)
                            },
                        )
                    }

                    LocationChangeDialogState.Moving -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    is LocationChangeDialogState.Complete.Success -> {
                        Text(R.string.move_profile_location_success_message.str())
                    }

                    is LocationChangeDialogState.Complete.Error -> {
                        // TODO actual error message
                        Text(R.string.move_profile_location_error_message.str(state.moveStatus.toString()))
                    }
                }
            },
            onDismissRequest = {
                if (!isMoving) {
                    state = LocationChangeDialogState.Idle
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isMoving,
                    onClick = {
                        when (state) {
                            LocationChangeDialogState.Idle -> {}
                            LocationChangeDialogState.Open -> {
                                when (profileInfo.db) {
                                    is ManagedProfileDb -> takeOwnershipWorkflow.launch("couch-tracker.db")
                                    is ExternalProfileDb -> {
                                        state = LocationChangeDialogState.Moving
                                        coroutineScope.launch {
                                            state = LocationChangeDialogState.Complete.from(profileInfo.db.moveToManagedDb(context))
                                        }
                                    }
                                }
                            }

                            LocationChangeDialogState.Moving -> {}
                            is LocationChangeDialogState.Complete -> {
                                state = LocationChangeDialogState.Idle
                            }
                        }
                    },
                    content = { Text(android.R.string.ok.str()) },
                )
            },
            dismissButton = {
                if (state !is LocationChangeDialogState.Complete) {
                    TextButton(
                        enabled = !isMoving,
                        onClick = { state = LocationChangeDialogState.Idle },
                        content = { Text(android.R.string.cancel.str()) },
                    )
                }
            },
        )
    }
}

@Composable
private fun ProfileFileName(profileInfo: ProfileInfo) {
    when (val db = profileInfo.db) {
        is ManagedProfileDb -> {}
        is ExternalProfileDb -> {
            Preference(
                icon = { Icon(Icons.Default.Abc, contentDescription = null) },
                title = { Text(R.string.profile_file_name.str()) },
                summary = { Text(db.externalDb.name ?: R.string.profile_file_name_unknown.str()) },
                enabled = false,
            )
        }
    }
}

@Composable
private fun ProfileLastUsedPreference(profileInfo: ProfileInfo) {
    Preference(
        icon = { Icon(Icons.Default.History, contentDescription = null) },
        title = { Text(R.string.profile_last_used.str()) },
        summary = { Text(profileInfo.formattedLastModified()) },
        enabled = false,
    )
}

@Composable
private fun ProfileSizePreference(profileInfo: ProfileInfo) {
    val byteSize = profileInfo.db.size()?.let { BinaryByteSize(it) }
    Preference(
        icon = { Icon(Icons.Default.Straighten, contentDescription = null) },
        title = { Text(R.string.profile_database_size.str()) },
        summary = { Text(byteSize?.toString() ?: R.string.profile_database_size_unknown.str()) },
        enabled = false,
    )
}

@Composable
private fun DeleteProfilePreference(profileInfo: ProfileInfo) {
    val appDb = koinInject<AppData>()
    val cs = rememberCoroutineScope()

    var dialogOpen by remember { mutableStateOf(false) }

    val icon = when (profileInfo.db) {
        is ManagedProfileDb -> Icons.Default.Delete
        is ExternalProfileDb -> Icons.Default.LinkOff
    }
    val text = when (profileInfo.db) {
        is ManagedProfileDb -> R.string.delete_profile.str()
        is ExternalProfileDb -> R.string.unlink_profile.str()
    }
    Preference(
        title = { Text(text) },
        icon = { Icon(icon, contentDescription = null) },
        onClick = {
            dialogOpen = true
        },
    )

    if (dialogOpen) {
        AlertDialog(
            icon = { Icon(icon, contentDescription = null) },
            title = {
                Text(
                    text = when (profileInfo.db) {
                        is ManagedProfileDb -> R.string.delete_profile_question
                        is ExternalProfileDb -> R.string.unlink_profile_question
                    }.str(),
                )
            },
            text = {
                Text(
                    text = when (profileInfo.db) {
                        is ManagedProfileDb -> R.string.delete_profile_message
                        is ExternalProfileDb -> R.string.unlink_profile_message
                    }.str(),
                )
            },
            onDismissRequest = { dialogOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        cs.launch { profileInfo.delete(appDb) }
                    },
                    colors = when (profileInfo.db) {
                        is ManagedProfileDb -> ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        is ExternalProfileDb -> ButtonDefaults.textButtonColors()
                    },
                    content = { Text(text) },
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { dialogOpen = false },
                    content = { Text(android.R.string.cancel.str()) },
                )
            },
        )
    }
}

@Composable
fun ProfileNameDialog(
    initialName: String,
    onOk: (name: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        title = { Text(R.string.profile_name.str()) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardActions = KeyboardActions { onOk(name) },
                singleLine = true,
                placeholder = { Text(R.string.profile_name_placeholder.str()) },
            )
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onOk(name.trim())
                    onDismissRequest()
                },
                enabled = name.isNotBlank(),
                content = { Text(android.R.string.ok.str()) },
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                content = { Text(android.R.string.cancel.str()) },
            )
        },
    )
}
