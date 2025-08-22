package io.github.couchtracker.ui.screens.watchedItem

import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import io.github.couchtracker.settings.appSettings
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MediaScreenScaffold
import io.github.couchtracker.ui.components.MessageComposable
import io.github.couchtracker.ui.components.WatchedItemDimensionSelections
import io.github.couchtracker.utils.ApiLoadable
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.str
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import kotlin.time.Duration

@Serializable
data class WatchedItemsScreen(val itemId: String) : Screen() {
    @Composable
    override fun content() {
        when (val itemId = WatchableExternalId.parse(itemId)) {
            is WatchableExternalId.Movie -> when (itemId.movieId) {
                is TmdbExternalMovieId -> {
                    val tmdbLanguages = appSettings().get { Tmdb.Languages }.current
                    Content(movie = TmdbMovie(itemId.movieId.id, tmdbLanguages))
                }
                is UnknownExternalMovieId -> TODO()
            }

            is WatchableExternalId.Episode -> TODO()
        }
    }
}

fun NavController.navigateToWatchedItems(id: WatchableExternalId) {
    navigate(WatchedItemsScreen(id.serialize()))
}

@Composable
private fun Content(movie: TmdbMovie) {
    val coroutineScope = rememberCoroutineScope()
    val context = koinInject<Context>()
    var screenModel by remember { mutableStateOf<ApiLoadable<WatchedItemsScreenModel>>(Loadable.Loading) }
    suspend fun load() {
        screenModel = Loadable.Loading
        screenModel = Loadable.Loaded(
            WatchedItemsScreenModel.loadTmdbMovie(
                context = context,
                movie = movie,
            ),
        )
    }
    LaunchedEffect(movie) {
        load()
    }

    LoadableScreen(
        data = screenModel,
        onError = { exception ->
            Surface {
                DefaultErrorScreen(
                    errorMessage = exception.title.string(),
                    errorDetails = exception.details?.string(),
                    retry = {
                        coroutineScope.launch { load() }
                    },
                )
            }
        },
    ) { model ->
        WatchedItemList(model = model)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchedItemList(model: WatchedItemsScreenModel) {
    val fullProfileData = LocalFullProfileDataContext.current
    val watchedItems = remember(fullProfileData, model.id) {
        fullProfileData.watchedItems.filter { it.itemId == model.id }.sortDescending()
    }
    val scaffoldState = rememberWatchedItemSheetScaffoldState()
    var watchedItemForInfoDialog: WatchedItemWrapper? by remember { mutableStateOf(null) }
    var watchedItemForDeleteDialog: WatchedItemWrapper? by remember { mutableStateOf(null) }
    val backgroundColor by animateColorAsState(model.colorScheme.background)

    MediaScreenScaffold(
        watchedItemSheetScaffoldState = scaffoldState,
        colorScheme = model.colorScheme,
        watchedItemType = model.itemType,
        mediaRuntime = { model.runtime },
        mediaLanguages = { listOf(model.originalLanguage) },
        backgroundColor = { backgroundColor },
        title = R.string.viewing_history_for_x.str(model.title),
        backdrop = model.backdrop,
        floatingActionButton = {
            FloatingActionButton(onClick = { scaffoldState.open(WatchedItemSheetMode.New(model.id)) }) {
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
                        mediaRuntime = model.runtime,
                        onClick = { watchedItemForInfoDialog = watchedItem },
                    )
                }
            }
        }
        watchedItemForInfoDialog?.let { watchedItem ->
            WatchedItemInfoDialog(
                itemTitle = model.title,
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
