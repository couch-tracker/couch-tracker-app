package io.github.couchtracker.ui.screens.watchedItem

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.navigation.NavController
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.WatchableExternalId
import io.github.couchtracker.db.profile.asWatchable
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import io.github.couchtracker.db.profile.model.watchedItem.localizedWatchAt
import io.github.couchtracker.db.profile.model.watchedItem.sortDescending
import io.github.couchtracker.db.profile.movie.TmdbExternalMovieId
import io.github.couchtracker.db.profile.movie.UnknownExternalMovieId
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MediaScreenScaffold
import io.github.couchtracker.ui.components.MessageComposable
import io.github.couchtracker.ui.components.WatchedItemDimensionSelections
import io.github.couchtracker.utils.ApiException
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.str
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data class WatchedItemsScreen(val itemId: String, val language: String?) : Screen() {
    @Composable
    override fun content() {
        val language = language?.let { TmdbLanguage.parse(it) } ?: TmdbLanguage.ENGLISH
        when (val itemId = WatchableExternalId.parse(itemId)) {
            is WatchableExternalId.Movie -> when (itemId.movieId) {
                is TmdbExternalMovieId -> Content(
                    movie = TmdbMovie(itemId.movieId.id, language),
                )

                is UnknownExternalMovieId -> TODO()
            }

            is WatchableExternalId.Episode -> TODO()
        }
    }
}

fun NavController.navigateToWatchedItems(movie: TmdbMovie) {
    navigate(WatchedItemsScreen(movie.id.toExternalId().asWatchable().serialize(), movie.language.serialize()))
}

@Composable
private fun Content(movie: TmdbMovie) {
    val coroutineScope = rememberCoroutineScope()
    val context = koinInject<Context>()
    val tmdbCache = koinInject<TmdbCache>()
    var screenModel by remember { mutableStateOf<Loadable<WatchedItemsScreenModel, ApiException>>(Loadable.Loading) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        suspend fun load() {
            screenModel = Loadable.Loading
            screenModel = WatchedItemsScreenModel.loadTmdbMovie(
                context = context,
                tmdbCache = tmdbCache,
                movie = movie,
                width = this.constraints.maxWidth,
                height = this.constraints.maxHeight,
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

    MediaScreenScaffold(
        watchedItemSheetScaffoldState = scaffoldState,
        colorScheme = model.colorScheme,
        watchedItemType = model.itemType,
        mediaRuntime = model.runtime,
        mediaLanguages = listOf(model.originalLanguage),
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
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = watchedItem.localizedWatchAt(includeTimeZone = false),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = { WatchedItemDimensionSelections(watchedItem.dimensions) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
