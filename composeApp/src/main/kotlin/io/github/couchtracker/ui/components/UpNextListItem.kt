package io.github.couchtracker.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.mmauro.datetimepolyglot.TickingValue
import dev.mmauro.datetimepolyglot.localizers.dynamic.DynamicLocalDateLocalizer
import dev.mmauro.datetimepolyglot.localizers.localize
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalSeasonId
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.externalids.TmdbExternalEpisodeId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedEpisodeSessionWrapper
import io.github.couchtracker.intl.datetime.EPISODE_FIRST_AIRDATE_LOCALIZER_OPTIONS
import io.github.couchtracker.intl.datetime.rememberLocalizer
import io.github.couchtracker.tmdb.BaseTmdbShow
import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ItemPosition
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.actions.markEpisodeAsWatchedAction
import io.github.couchtracker.ui.rememberPlaceholderPainter
import io.github.couchtracker.ui.screens.episodes.navigateToEpisode
import io.github.couchtracker.ui.screens.main.ShowSectionViewModel
import io.github.couchtracker.ui.screens.show.navigateToShow
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode
import io.github.couchtracker.ui.seasonEpisodeNumberToString
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.rememberTickingValue
import kotlinx.datetime.LocalDate
import kotlin.time.Duration

private val POSTER_HEIGHT = 64.dp
private val POSTER_WIDTH = POSTER_HEIGHT * 2 / 3

@Composable
fun UpNextListItem(
    upNext: UpNextListItemModel,
    position: ItemPosition,
    modifier: Modifier = Modifier,
) {
    val navController = LocalNavController.current

    val dateTimeLocalizer = rememberLocalizer(EPISODE_FIRST_AIRDATE_LOCALIZER_OPTIONS, ::DynamicLocalDateLocalizer)
    val dateTimeText = rememberTickingValue(dateTimeLocalizer, upNext.episodeAirDate) {
        if (upNext.episodeAirDate == null) {
            TickingValue(null, null)
        } else {
            dateTimeLocalizer.localize(upNext.episodeAirDate)
        }
    }

    val markEpisodeAsWatchedAction = markEpisodeAsWatchedAction(upNext.showId, upNext.episodeId) { watchedSession ->
        WatchedItemSheetMode.New.Episode(
            itemId = upNext.episodeId,
            watchedSession = watchedSession,
            mediaRuntime = upNext.episodeRuntime,
            mediaLanguages = listOfNotNull(upNext.showOriginalLanguage),
        )
    }
    ListItemWithAction(
        modifier = modifier,
        action = markEpisodeAsWatchedAction,
        onClick = {
            navController.navigateToEpisode(upNext.episodeId)
        },
        leadingContentHeight = POSTER_HEIGHT,
        position = position,
        leadingContent = {
            Surface(shape = MaterialTheme.shapes.small) {
                AsyncImage(
                    with(LocalDensity.current) {
                        upNext.posterModel?.getCoilModel(POSTER_WIDTH.roundToPx(), POSTER_HEIGHT.roundToPx())
                    },
                    modifier = Modifier
                        .size(POSTER_WIDTH, POSTER_HEIGHT)
                        .clickable {
                            navController.navigateToShow(upNext.showId, preloadData = upNext.showPreloadData)
                        },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    fallback = rememberPlaceholderPainter(PlaceholdersDefaults.SHOW.icon, isError = false),
                    error = rememberPlaceholderPainter(PlaceholdersDefaults.SHOW.icon, isError = true),
                )
            }
        },
    ) {
        Column {
            if (upNext.showName != null) {
                Text(upNext.showName, style = MaterialTheme.typography.titleSmall)
            }
            Text(upNext.episodeLabel, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (dateTimeText != null) {
                Text(dateTimeText, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

data class UpNextListItemModel(
    val showId: ExternalShowId,
    val watchSession: WatchedEpisodeSessionWrapper?,
    val showPreloadData: BaseTmdbShow,
    val seasonId: ExternalSeasonId,
    val episodeId: ExternalEpisodeId,
    val posterModel: ImageModel?,
    val showName: String?,
    val episodeLabel: String,
    val episodeAirDate: LocalDate?,
    // For the marked as watched dialog
    val showOriginalLanguage: Bcp47Language?,
    val episodeRuntime: Duration?,
) {

    companion object {

        fun withShowData(
            context: Context,
            watchSession: WatchedEpisodeSessionWrapper?,
            show: ShowSectionViewModel.BookmarkedShowData,
            season: ShowSectionViewModel.BookmarkedSeasonData,
            episode: ShowSectionViewModel.BookmarkedEpisodeData,
        ): UpNextListItemModel {
            val showId = show.baseShowData.key.id
            val seasonId = TmdbSeasonId(showId, season.number)
            val episodeId = TmdbExternalEpisodeId(TmdbEpisodeId(seasonId, episode.number))
            val episodeNumberLabel = seasonEpisodeNumberToString(context, season.number, episode.number, episode.name)

            return UpNextListItemModel(
                showId = showId.toExternalId(),
                seasonId = seasonId.toExternalId(),
                watchSession = watchSession,
                showPreloadData = show.baseShowData,
                episodeId = episodeId,
                posterModel = show.baseShowData.poster?.toImageModel(),
                showName = show.baseShowData.name,
                episodeLabel = episodeNumberLabel,
                episodeAirDate = episode.airDate,
                showOriginalLanguage = show.originalLanguage,
                episodeRuntime = episode.runtime,
            )
        }
    }
}
