package io.github.couchtracker.ui.screens.show

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.externalids.TmdbExternalShowId
import io.github.couchtracker.db.profile.externalids.UnknownExternalShowId
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.intl.datetime.MonthSkeleton
import io.github.couchtracker.intl.datetime.YearSkeleton
import io.github.couchtracker.intl.datetime.localized
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.ListItemPosition
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.InfoFooter
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MediaScreenScaffold
import io.github.couchtracker.ui.components.MessageComposable
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.components.OverviewScreenComponents.section
import io.github.couchtracker.ui.components.OverviewScreenComponents.textBlock
import io.github.couchtracker.ui.components.WatchedItemDimensionSelections
import io.github.couchtracker.ui.itemsWithPosition
import io.github.couchtracker.utils.pluralStr
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.viewModelApplication
import kotlinx.serialization.Serializable

@Serializable
data class WatchedEpisodeSessionsScreen(val showId: String) : Screen() {
    @Composable
    override fun content() {
        val externalShowId = ExternalShowId.parse(this.showId)
        val showId = when (externalShowId) {
            is TmdbExternalShowId -> externalShowId.id
            is UnknownExternalShowId -> TODO()
        }
        Content(
            viewModel {
                WatchedEpisodeSessionsScreenViewModel(
                    application = viewModelApplication(),
                    showId = showId,
                    externalShowId = externalShowId,
                )
            },
        )
    }
}

fun NavController.navigateToEpisodeWatchSessions(id: ExternalShowId) {
    navigate(WatchedEpisodeSessionsScreen(ExternalShowId.serialize(id)))
}

@Composable
private fun Content(viewModel: WatchedEpisodeSessionsScreenViewModel) {
    LoadableScreen(
        data = viewModel.fullDetails,
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
        WatchedEpisodeSessionList(viewModel = viewModel, details = details)
    }
}

@Composable
private fun WatchedEpisodeSessionList(
    viewModel: WatchedEpisodeSessionsScreenViewModel,
    details: ShowScreenViewModelHelper.FullDetails,
) {
    val colorScheme = viewModel.colorScheme.resultValueOrNull() ?: ColorSchemes.Show
    val backgroundColor by animateColorAsState(colorScheme.background)
    val fullProfileData = LocalFullProfileDataContext.current
    var dialogMode by remember { mutableStateOf<WatchedEpisodeSessionDialogMode?>(null) }

    MediaScreenScaffold(
        colorScheme = colorScheme,
        backgroundColor = { backgroundColor },
        title = R.string.watch_sessions.str(),
        subtitle = details.baseDetails.name.orEmpty(),
        backdrop = details.baseDetails.backdrop,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    dialogMode = WatchedEpisodeSessionDialogMode.New(
                        show = viewModel.externalShowId,
                        mediaLanguages = listOfNotNull(details.originalLanguage),
                    )
                },
                content = {
                    Icon(Icons.Default.Add, contentDescription = R.string.add_watch_session.str())
                },
            )
        },
        content = { innerPadding ->
            WatchedEpisodeSessionDialog(
                state = dialogMode?.let { rememberWatchedEpisodeSessionDialogState(mode = it) },
                onDismissRequest = { dialogMode = null },
            )
            OverviewScreenComponents.ContentList(
                innerPadding = innerPadding + PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                fun watchedEpisodeSessions(sessionInfos: List<WatchedEpisodeSessionInfo>) {
                    itemsWithPosition(sessionInfos, key = { _, info -> info.watchedEpisodeSession.id }) { position, sessionInfo ->
                        WatchedEpisodeSessionListItem(
                            modifier = Modifier.animateItem(),
                            watchedEpisodeSessionInfo = sessionInfo,
                            onClick = {
                                dialogMode = WatchedEpisodeSessionDialogMode.Edit(
                                    session = sessionInfo.watchedEpisodeSession,
                                    mediaLanguages = listOfNotNull(details.originalLanguage),
                                )
                            },
                            position = position,
                        )
                    }
                }

                val watchedEpisodeSessions = fullProfileData.watchedEpisodeSessions[viewModel.externalShowId]
                    .orEmpty()
                    .map { fullProfileData.getWatchedEpisodeSessionInfo(it) }
                    .sorted()
                val (activeSessions, inactiveSessions) = watchedEpisodeSessions.partition { it.watchedEpisodeSession.isActive }

                if (watchedEpisodeSessions.isEmpty()) {
                    item {
                        MessageComposable(
                            modifier = Modifier.fillMaxWidth().height(240.dp),
                            icon = Icons.Outlined.Layers,
                            message = R.string.no_watch_sessions.str(),
                        )
                    }
                } else {
                    section({ textBlock("active-sessions", R.string.active_watch_sessions) }) {
                        if (activeSessions.isNotEmpty()) {
                            watchedEpisodeSessions(activeSessions)
                        } else {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(Icons.Outlined.LayersClear, contentDescription = null)
                                    Text(R.string.no_active_watch_sessions.str())
                                }
                            }
                        }
                    }

                    if (inactiveSessions.isNotEmpty()) {
                        section({ textBlock("completed-sessions", R.string.inactive_watch_sessions) }) {
                            watchedEpisodeSessions(inactiveSessions)
                        }
                    }
                }
                item(key = "info-footer") {
                    InfoFooter(R.string.watch_session_info.str(), modifier = Modifier.animateItem().padding(horizontal = 8.dp))
                }
            }
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun WatchedEpisodeSessionListItem(
    watchedEpisodeSessionInfo: WatchedEpisodeSessionInfo,
    onClick: () -> Unit = {},
    position: ListItemPosition,
    modifier: Modifier = Modifier,
) {
    val watchedEpisodeSession = watchedEpisodeSessionInfo.watchedEpisodeSession

    fun PartialDateTime.yearMonthPrecision(): PartialDateTime.Local {
        return when (val local = local) {
            is PartialDateTime.Local.Year -> local
            is PartialDateTime.Local.WithYearMonth -> PartialDateTime.Local.YearMonth(local.yearMonth)
        }
    }

    @Composable
    fun PartialDateTime.Local.monthString(): String {
        return when (this) {
            is PartialDateTime.Local.Year -> localized(YearSkeleton.NUMERIC)
            is PartialDateTime.Local.WithYearMonth -> localized(YearSkeleton.NUMERIC, MonthSkeleton.ABBREVIATED)
        }.string()
    }

    ListItem(
        modifier = modifier,
        onClick = onClick,
        content = {
            Text(watchedEpisodeSession.name ?: R.string.unnamed_watch_session.str(), style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = {
            Column {
                WatchedItemDimensionSelections(
                    selections = watchedEpisodeSession.defaultDimensionSelections.dimensions,
                    emptyPlaceholder = { },
                )
                if (watchedEpisodeSessionInfo.watchedEpisodes.isEmpty()) {
                    Text(R.string.no_watched_episodes_in_this_session.str())
                } else {
                    Text(
                        R.plurals.x_watched_episodes_in_this_session.pluralStr(
                            watchedEpisodeSessionInfo.watchedEpisodes.size,
                            watchedEpisodeSessionInfo.watchedEpisodes.size,
                        ),
                    )
                }
                watchedEpisodeSession.description?.let {
                    Text(it)
                }
            }
        },
        trailingContent = {
            val firstYearMonthWatchedEpisode = watchedEpisodeSessionInfo.firstWatchedEpisodeAt?.yearMonthPrecision()
            val lastYearMonthWatchedEpisode = watchedEpisodeSessionInfo.lastWatchedEpisodeAt?.yearMonthPrecision()
            if (firstYearMonthWatchedEpisode != null && lastYearMonthWatchedEpisode != null) {
                if (firstYearMonthWatchedEpisode == lastYearMonthWatchedEpisode) {
                    Text(firstYearMonthWatchedEpisode.monthString())
                } else {
                    Text(
                        R.string.date_and_time_range_format.str(
                            firstYearMonthWatchedEpisode.monthString(),
                            lastYearMonthWatchedEpisode.monthString(),
                        ),
                    )
                }
            }
        },
        shapes = ListItemShapes(position),
    )
}
