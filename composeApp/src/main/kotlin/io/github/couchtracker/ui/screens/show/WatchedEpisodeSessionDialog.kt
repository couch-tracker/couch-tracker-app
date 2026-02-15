package io.github.couchtracker.ui.screens.show

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedEpisodeSessionWrapper
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionSelectionsMode
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionSelectionsState
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.db.profile.model.watchedItem.rememberWatchedItemDimensionSelectionsState
import io.github.couchtracker.ui.components.InfoFooter
import io.github.couchtracker.ui.components.ProfileDbActionDialog
import io.github.couchtracker.ui.components.WatchedItemDimensionSelections
import io.github.couchtracker.ui.screens.watchedItem.DimensionSection
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSelectionsScope
import io.github.couchtracker.utils.str

@Composable
fun WatchedEpisodeSessionDialog(
    state: WatchedEpisodeSessionDialogState?,
    onDismissRequest: () -> Unit,
    onSaved: () -> Unit = {},
) {
    var showDimensionSelectionsDialog by remember { mutableStateOf(false) }
    var deleteDialogState by remember { mutableStateOf<WatchedEpisodeSessionWrapper?>(null) }
    ProfileDbActionDialog(
        state = state,
        execute = { db, state -> state.save(db) },
        onDismissRequest = onDismissRequest,
        onSuccess = { onSaved() },
        confirmText = { state ->
            when (state.mode) {
                is WatchedEpisodeSessionDialogMode.New -> Text(R.string.create_action.str())
                is WatchedEpisodeSessionDialogMode.Edit -> Text(R.string.save_action.str())
            }
        },
        icon = null,
        title = { state ->
            when (state.mode) {
                is WatchedEpisodeSessionDialogMode.New -> Text(R.string.add_watch_session.str())
                is WatchedEpisodeSessionDialogMode.Edit -> Text(R.string.edit_watch_session.str())
            }
        },
        text = { state ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.name,
                    onValueChange = { state.name = it },
                    label = { Text(R.string.optional_field.str(R.string.watch_session_name.str())) },
                    placeholder = { Text(R.string.unnamed_watch_session.str()) },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.description,
                    onValueChange = { state.description = it },
                    label = { Text(R.string.optional_field.str(R.string.watch_session_description.str())) },
                )
                Row(verticalAlignment = CenterVertically) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                        Checkbox(
                            modifier = Modifier.padding(end = 12.dp),
                            checked = state.isActive,
                            onCheckedChange = { state.isActive = it },
                        )
                    }
                    Text(text = R.string.watch_session_active.str(), style = MaterialTheme.typography.bodyLarge)
                }
                Row(verticalAlignment = CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = R.string.watch_session_preset.str(), style = MaterialTheme.typography.bodyLarge)
                        WatchedItemDimensionSelections(
                            selections = state.defaultDimensionSelections.dimensions,
                            emptyPlaceholder = { Text(R.string.watch_session_empty_preset.str()) },
                        )
                    }
                    TextButton(
                        onClick = { showDimensionSelectionsDialog = true },
                        content = { Icon(Icons.Default.Edit, contentDescription = R.string.edit_action.str()) },
                    )
                }
                InfoFooter(R.string.watch_session_dialog_info.str())
            }
            if (showDimensionSelectionsDialog) {
                DimensionSelectionsDialog(
                    selectionsState = state.defaultDimensionSelections,
                    mediaLanguages = state.mode.mediaLanguages,
                    onDismissRequest = { showDimensionSelectionsDialog = false },
                )
            }
        },
        onError = { },
        additionalButtons = { state ->
            if (state.mode is WatchedEpisodeSessionDialogMode.Edit) {
                TextButton(
                    onClick = {
                        onDismissRequest()
                        deleteDialogState = state.mode.session
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(R.string.delete_action.str())
                }
            }
        },
    )
    DeleteWatchedEpisodeSessionConfirmDialog(
        watchedEpisodeSession = deleteDialogState,
        onDismissRequest = { deleteDialogState = null },
    )
}

sealed interface WatchedEpisodeSessionDialogMode {

    val mediaLanguages: List<Bcp47Language>

    data class New(
        val show: ExternalShowId,
        override val mediaLanguages: List<Bcp47Language>,
    ) : WatchedEpisodeSessionDialogMode

    data class Edit(
        val session: WatchedEpisodeSessionWrapper,
        override val mediaLanguages: List<Bcp47Language>,
    ) : WatchedEpisodeSessionDialogMode
}

class WatchedEpisodeSessionDialogState(
    val mode: WatchedEpisodeSessionDialogMode,
    name: String,
    description: String,
    isActive: Boolean,
    val defaultDimensionSelections: WatchedItemDimensionSelectionsState,
) {
    var name by mutableStateOf(name)
    var description by mutableStateOf(description)
    var isActive by mutableStateOf(isActive)

    fun save(db: ProfileData) {
        db.transaction {
            val selections = defaultDimensionSelections.save(db)

            val name = name.takeIf { it.isNotBlank() }?.trim()
            val description = description.takeIf { it.isNotBlank() }?.trim()
            when (mode) {
                is WatchedEpisodeSessionDialogMode.New -> db.watchedEpisodeSessionQueries.insert(
                    showId = mode.show,
                    name = name,
                    description = description,
                    isActive = isActive,
                    defaultDimensionSelections = selections.id,
                ).executeAsOne()
                is WatchedEpisodeSessionDialogMode.Edit -> db.watchedEpisodeSessionQueries.update(
                    id = mode.session.id,
                    name = name,
                    description = description,
                    isActive = isActive,
                ).executeAsOne()
            }
        }
    }
}

@Composable
fun rememberWatchedEpisodeSessionDialogState(mode: WatchedEpisodeSessionDialogMode): WatchedEpisodeSessionDialogState {
    val profileData = LocalFullProfileDataContext.current

    val defaultDimensionSelectionsState = rememberWatchedItemDimensionSelectionsState(
        watchedItemType = WatchedItemType.EPISODE,
        mode = when (mode) {
            is WatchedEpisodeSessionDialogMode.New -> WatchedItemDimensionSelectionsMode.New
            is WatchedEpisodeSessionDialogMode.Edit -> WatchedItemDimensionSelectionsMode.Edit(mode.session.defaultDimensionSelections)
        },
    )

    // TODO make this savable
    return remember(profileData, defaultDimensionSelectionsState, mode) {
        when (mode) {
            is WatchedEpisodeSessionDialogMode.New -> WatchedEpisodeSessionDialogState(
                mode = mode,
                name = "",
                description = "",
                isActive = true,
                defaultDimensionSelections = defaultDimensionSelectionsState,
            )

            is WatchedEpisodeSessionDialogMode.Edit -> WatchedEpisodeSessionDialogState(
                mode = mode,
                name = mode.session.name.orEmpty(),
                description = mode.session.description.orEmpty(),
                isActive = mode.session.isActive,
                defaultDimensionSelections = defaultDimensionSelectionsState,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DimensionSelectionsDialog(
    mediaLanguages: List<Bcp47Language>,
    selectionsState: WatchedItemDimensionSelectionsState,
    onDismissRequest: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        content = {
            Surface(
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = R.string.watch_session_preset.str(),
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(top = 8.dp, bottom = 16.dp),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Column(modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false)) {
                        for (selection in selectionsState.dimensions) {
                            AnimatedVisibility(visible = selection.dimension.isEnabled(selectionsState.dimensions)) {
                                WatchedItemSelectionsScope.DimensionSection(
                                    enabled = true,
                                    selection = selection,
                                    mediaLanguages = { mediaLanguages },
                                    onSelectionChange = selectionsState::update,
                                )
                            }
                        }
                    }
                    FlowRow(
                        Modifier.align(Alignment.End).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        TextButton(
                            onClick = { onDismissRequest() },
                        ) {
                            Text(android.R.string.ok.str())
                        }
                    }
                }
            }
        },
    )
}
