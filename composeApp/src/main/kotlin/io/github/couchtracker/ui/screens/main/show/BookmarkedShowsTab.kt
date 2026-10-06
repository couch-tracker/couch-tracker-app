package io.github.couchtracker.ui.screens.main.show

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.couchtracker.model.sort.SortableItemType
import io.github.couchtracker.model.sort.Sorter
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.settings.appSettings
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MessageComposable
import io.github.couchtracker.ui.components.PortraitComposableDefaults
import io.github.couchtracker.ui.components.ShowPortrait
import io.github.couchtracker.ui.components.ShowPortraitModel
import io.github.couchtracker.ui.components.SortActionButton
import io.github.couchtracker.ui.components.SortedGrid
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.settings.Setting
import kotlinx.coroutines.launch

@Composable
fun BookmarkedShowActions(
    sortSetting: AppSettings.() -> Setting<*, *, Sorter, Sorter>,
) {
    val cs = rememberCoroutineScope()
    val settings = appSettings()
    val current = settings.get { sortSetting() }.current
    SortActionButton(
        itemTypes = setOf(SortableItemType.SHOW),
        currentSorter = current,
        setCurrentSorter = { newSorter ->
            cs.launch {
                settings.getSetting { sortSetting() }.set(newSorter)
            }
        },
    )
}

@Composable
fun BookmarkedShowTab(
    shows: Loadable<ShowSectionViewModel.BookmarkedShowsModel>,
    emptyMessage: String,
    emptyDescription: String,
) {
    LoadableScreen(shows) { (sorter, shows) ->
        if (shows.isEmpty()) {
            MessageComposable(
                modifier = Modifier.fillMaxSize(),
                icon = Icons.Default.BookmarkBorder,
                message = emptyMessage,
                details = emptyDescription,
            )
        } else {
            SortedGrid(
                sorter = sorter,
                sortedItems = shows,
                columns = GridCells.Adaptive(minSize = PortraitComposableDefaults.SUGGESTED_WIDTH),
                itemKey = { it.itemKey },
                itemComposable = { show ->
                    val showPortraitModel = show.data.map { data ->
                        data.portraitModel.copy(
                            downloadState = when (val seasons = data.seasons) {
                                is Loadable.Loaded -> when (seasons.value) {
                                    is Result.Error -> ShowPortraitModel.DownloadState.Error
                                    is Result.Value -> ShowPortraitModel.DownloadState.Downloaded
                                }
                                Loadable.Loading -> ShowPortraitModel.DownloadState.Loading
                            },
                        )
                    }
                    ShowPortrait(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .animateItem(),
                        showId = show.showId,
                        showResult = showPortraitModel,
                    )
                },
            )
        }
    }
}
