package io.github.couchtracker.ui.screens.watchedItem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalId
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.db.profile.externalids.TmdbExternalMovieId
import io.github.couchtracker.db.profile.externalids.UnknownExternalMovieId
import io.github.couchtracker.db.profile.externalids.WatchableExternalId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import io.github.couchtracker.db.profile.model.watchedItem.localizedWatchAt
import io.github.couchtracker.db.profile.model.watchedItem.sortDescending
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MessageComposable
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.components.WatchableMediaScreenScaffold
import io.github.couchtracker.ui.components.WatchedItemDimensionSelections
import io.github.couchtracker.ui.itemsWithPosition
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.viewModelApplication
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class WatchedItemsScreen(val itemId: String) : Screen() {
    @Composable
    override fun content() {
        val viewModel = when (val externalItemId = ExternalId.parse<WatchableExternalId>(itemId)) {
            is ExternalMovieId -> {
                val movieId = when (externalItemId) {
                    is TmdbExternalMovieId -> externalItemId.id
                    is UnknownExternalMovieId -> TODO()
                }
                viewModel {
                    WatchedItemsScreenViewModel.Movie(
                        application = viewModelApplication(),
                        movieId = movieId,
                    )
                }
            }

            is ExternalEpisodeId -> TODO()
        }
        Content(viewModel)
    }
}

fun NavController.navigateToWatchedItems(id: WatchableExternalId) {
    navigate(WatchedItemsScreen(id.serialize()))
}

@Composable
private fun Content(
    viewModel: WatchedItemsScreenViewModel,
) {
    LoadableScreen(
        data = viewModel.details,
        onError = { exception ->
            Surface {
                DefaultErrorScreen(
                    errorMessage = exception.title.string(),
                    errorDetails = exception.details?.string(),
                    retry = { viewModel.retryAll() },
                )
            }
        },
    ) { details ->
        WatchedItemList(viewModel = viewModel, details = details)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WatchedItemList(viewModel: WatchedItemsScreenViewModel, details: WatchedItemsScreenViewModel.Details) {
    val fullProfileData = LocalFullProfileDataContext.current
    val watchedItems = remember(fullProfileData, viewModel.externalId) {
        fullProfileData.watchedItems.filter { it.itemId == viewModel.externalId }.sortDescending()
    }
    val scaffoldState = rememberWatchedItemSheetScaffoldState()
    var watchedItemForInfoDialog: WatchedItemWrapper? by remember { mutableStateOf(null) }
    var watchedItemForDeleteDialog: WatchedItemWrapper? by remember { mutableStateOf(null) }
    val colorScheme = viewModel.colorScheme.resultValueOrNull() ?: ColorSchemes.Movie
    val backgroundColor by animateColorAsState(colorScheme.background)
    WatchableMediaScreenScaffold(
        watchedItemSheetScaffoldState = scaffoldState,
        colorScheme = colorScheme,
        backgroundColor = { backgroundColor },
        watchedItemType = viewModel.watchedItemType,
        mediaRuntime = { details.runtime },
        mediaLanguages = { listOfNotNull(details.originalLanguage) },
        title = R.string.viewing_history.str(),
        subtitle = details.title,
        backdrop = details.backdrop,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val sheetMode = when (viewModel) {
                        is WatchedItemsScreenViewModel.Movie -> WatchedItemSheetMode.New.Movie(viewModel.externalId)
                    }
                    scaffoldState.open(sheetMode)
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = R.string.add_viewing.str())
            }
        },
    ) { contentPadding ->
        if (watchedItems.isEmpty()) {
            MessageComposable(
                modifier = Modifier.fillMaxSize(),
                icon = Icons.Default.Inbox,
                message = R.string.no_viewings.str(),
            )
        } else {
            OverviewScreenComponents.ContentList(
                innerPadding = contentPadding + PaddingValues(horizontal = 8.dp),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsWithPosition(watchedItems) { position, watchedItem ->
                    WatchedItemListItem(
                        watchedItem = watchedItem,
                        mediaRuntime = details.runtime,
                        onClick = { watchedItemForInfoDialog = watchedItem },
                        shapes = ListItemShapes(position),
                    )
                }
            }
        }
        watchedItemForInfoDialog?.let { watchedItem ->
            WatchedItemInfoDialog(
                itemTitle = details.title,
                watchedItem = watchedItem,
                onDismissRequest = { watchedItemForInfoDialog = null },
                onEditRequest = { scaffoldState.open(WatchedItemSheetMode.Edit(watchedItem)) },
                onDeleteRequest = { watchedItemForDeleteDialog = watchedItem },
            )
        }

        DeleteWatchedItemConfirmDialog(
            watchedItem = watchedItemForDeleteDialog,
            onDismissRequest = { watchedItemForDeleteDialog = null },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WatchedItemListItem(
    watchedItem: WatchedItemWrapper,
    mediaRuntime: Duration?,
    onClick: () -> Unit,
    shapes: androidx.compose.material3.ListItemShapes,
) {
    val progressState = rememberWatchedItemProgressState(watchedItem, mediaRuntime)
    ListItem(
        onClick = onClick,
        content = {
            Text(watchedItem.localizedWatchAt(includeTimeZone = false))
        },
        supportingContent = {
            Column {
                WatchedItemDimensionSelections(watchedItem.dimensions)
                WatchedItemProgress(
                    state = progressState,
                    type = watchedItem.type(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                )
            }
        },
        shapes = shapes,
    )
}
