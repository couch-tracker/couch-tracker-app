package io.github.couchtracker.ui.screens.watchedItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalId
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.db.profile.externalids.TmdbExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.TmdbExternalMovieId
import io.github.couchtracker.db.profile.externalids.UnknownExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.UnknownExternalMovieId
import io.github.couchtracker.db.profile.externalids.WatchableExternalId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import io.github.couchtracker.db.profile.model.watchedItem.localizedWatchAt
import io.github.couchtracker.db.profile.model.watchedItem.sortDescending
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.LocalWatchedItemSheetScaffoldState
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.actions.ActionFloatingActionButton
import io.github.couchtracker.ui.components.CouchTrackerScreenScaffold
import io.github.couchtracker.ui.components.MessageComposable
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.components.WatchedItemDimensionSelections
import io.github.couchtracker.ui.itemsWithPosition
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.viewModelApplication
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class WatchedItemsScreen(val itemId: String) : Screen() {
    @Composable
    override fun Content() {
        val viewModel = when (val externalItemId = ExternalId.parse<WatchableExternalId>(itemId)) {
            is ExternalMovieId -> when (externalItemId) {
                is TmdbExternalMovieId -> viewModel {
                    WatchedItemsScreenViewModel.Movie.Tmdb(
                        application = viewModelApplication(),
                        movieId = externalItemId.id,
                    )
                }
                is UnknownExternalMovieId -> viewModel {
                    WatchedItemsScreenViewModel.Movie.Unknown(
                        application = viewModelApplication(),
                        externalId = externalItemId,
                    )
                }
            }

            is ExternalEpisodeId -> when (externalItemId) {
                is TmdbExternalEpisodeId -> viewModel {
                    WatchedItemsScreenViewModel.Episode.Tmdb(
                        application = viewModelApplication(),
                        externalId = externalItemId,
                    )
                }
                is UnknownExternalEpisodeId -> viewModel {
                    WatchedItemsScreenViewModel.Episode.Unknown(
                        application = viewModelApplication(),
                        externalId = externalItemId,
                    )
                }
            }
        }
        ScreenContainer(viewModel.colorScheme) {
            Content(viewModel)
        }
    }
}

fun NavController.navigateToWatchedItems(id: WatchableExternalId) {
    navigate(WatchedItemsScreen(id.serialize()))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Content(viewModel: WatchedItemsScreenViewModel) {
    val fullProfileData = LocalFullProfileDataContext.current
    val details = viewModel.details

    val watchedItems = remember(fullProfileData, viewModel.externalId) {
        fullProfileData.watchedItems.filter { it.itemId == viewModel.externalId }.sortDescending()
    }
    var watchedItemForInfoDialog: WatchedItemWrapper? by remember { mutableStateOf(null) }
    var watchedItemForDeleteDialog: WatchedItemWrapper? by remember { mutableStateOf(null) }

    val title = R.string.viewing_history.str()
    CouchTrackerScreenScaffold(
        title = { title },
        subtitle = { details.subtitle },
        backdrop = { details.backdrop },
        floatingActionButton = {
            viewModel.markAsWatchedAction()?.let { ActionFloatingActionButton(it) }
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
            val state = LocalWatchedItemSheetScaffoldState.current
            WatchedItemInfoDialog(
                itemTitle = details.subtitle,
                watchedItem = watchedItem,
                onDismissRequest = { watchedItemForInfoDialog = null },
                onEditRequest = {
                    state.open(
                        WatchedItemSheetMode.Edit(
                            watchedItem = watchedItem,
                            watchedItemType = viewModel.watchedItemType,
                            mediaRuntime = details.runtime,
                            mediaLanguages = listOfNotNull(details.originalLanguage),
                        ),
                    )
                },
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
    shapes: ListItemShapes,
) {
    val progressState = rememberWatchedItemProgressState(watchedItem, mediaRuntime)
    ListItem(
        onClick = onClick,
        content = {
            Text(watchedItem.localizedWatchAt(includeTimeZone = false))
        },
        supportingContent = {
            Column {
                WatchedItemDimensionSelections(
                    selections = watchedItem.dimensions,
                    emptyPlaceholder = { Text(R.string.watched_item_no_additional_information.str(), fontStyle = FontStyle.Italic) },
                )
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
