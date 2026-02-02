package io.github.couchtracker.ui.screens.watchedItem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.WatchableExternalId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import io.github.couchtracker.db.profile.model.watchedItem.localizedWatchAt
import io.github.couchtracker.db.profile.model.watchedItem.sortDescending
import io.github.couchtracker.db.profile.movie.TmdbExternalMovieId
import io.github.couchtracker.db.profile.movie.UnknownExternalMovieId
import io.github.couchtracker.db.profile.type
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MediaScreenScaffold
import io.github.couchtracker.ui.components.MessageComposable
import io.github.couchtracker.ui.components.WatchedItemDimensionSelections
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.str
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class WatchedItemsScreen(val itemId: String) : Screen() {
    @Composable
    override fun content() {
        val viewModel = when (val externalId = WatchableExternalId.parse(itemId)) {
            is WatchableExternalId.Movie -> {
                val movieId = when (externalId.movieId) {
                    is TmdbExternalMovieId -> externalId.movieId.id
                    is UnknownExternalMovieId -> TODO()
                }
                viewModel {
                    WatchedItemsScreenViewModel.Movie(
                        application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]),
                        watchableExternalMovieId = externalId,
                        movieId = movieId,
                    )
                }
            }

            is WatchableExternalId.Episode -> TODO()
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
    val coroutineScope = rememberCoroutineScope()
    LoadableScreen(
        data = viewModel.details,
        onError = { exception ->
            Surface {
                DefaultErrorScreen(
                    errorMessage = exception.title.string(),
                    errorDetails = exception.details?.string(),
                    retry = {
                        coroutineScope.launch { viewModel.retryAll() }
                    },
                )
            }
        },
    ) { details ->
        WatchedItemList(viewModel = viewModel, details = details)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchedItemList(viewModel: WatchedItemsScreenViewModel, details: WatchedItemsScreenViewModel.Details) {
    val fullProfileData = LocalFullProfileDataContext.current
    val watchedItems = remember(fullProfileData, viewModel.watchableExternalMovieId) {
        fullProfileData.watchedItems.filter { it.itemId == viewModel.watchableExternalMovieId }.sortDescending()
    }
    val scaffoldState = rememberWatchedItemSheetScaffoldState()
    var watchedItemForInfoDialog: WatchedItemWrapper? by remember { mutableStateOf(null) }
    var watchedItemForDeleteDialog: WatchedItemWrapper? by remember { mutableStateOf(null) }
    val colorScheme = viewModel.colorScheme.resultValueOrNull() ?: ColorSchemes.Movie
    val backgroundColor by animateColorAsState(colorScheme.background)
    MediaScreenScaffold(
        watchedItemSheetScaffoldState = scaffoldState,
        colorScheme = colorScheme,
        backgroundColor = { backgroundColor },
        watchedItemType = viewModel.watchableExternalMovieId.type(),
        mediaRuntime = { details.runtime },
        mediaLanguages = { listOfNotNull(details.originalLanguage) },
        title = R.string.viewing_history_for_x.str(details.title),
        backdrop = details.backdrop,
        floatingActionButton = {
            FloatingActionButton(onClick = { scaffoldState.open(WatchedItemSheetMode.New(viewModel.watchableExternalMovieId)) }) {
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
            LazyColumn(
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(watchedItems) { watchedItem ->
                    WatchedItemListItem(
                        watchedItem = watchedItem,
                        mediaRuntime = details.runtime,
                        onClick = { watchedItemForInfoDialog = watchedItem },
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

@Composable
private fun WatchedItemListItem(
    watchedItem: WatchedItemWrapper,
    mediaRuntime: Duration?,
    onClick: () -> Unit,
) {
    val progressState = rememberWatchedItemProgressState(watchedItem, mediaRuntime)
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = watchedItem.localizedWatchAt(includeTimeZone = false),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Column {
                WatchedItemDimensionSelections(watchedItem.dimensions)
                WatchedItemProgress(
                    state = progressState,
                    type = watchedItem.itemId.type(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
