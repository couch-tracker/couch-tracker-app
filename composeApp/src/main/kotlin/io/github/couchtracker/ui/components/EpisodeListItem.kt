package io.github.couchtracker.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.moviebase.tmdb.model.TmdbEpisode
import coil3.compose.AsyncImage
import dev.mmauro.datetimepolyglot.TickingValue
import dev.mmauro.datetimepolyglot.localizers.absolute.localize
import dev.mmauro.datetimepolyglot.localizers.dynamic.DynamicLocalDateLocalizer
import dev.mmauro.datetimepolyglot.localizers.localizeNow
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.externalids.TmdbExternalEpisodeId
import io.github.couchtracker.intl.datetime.EPISODE_FIRST_AIRDATE_LOCALIZER_OPTIONS
import io.github.couchtracker.intl.datetime.RUNTIME_LOCALIZER_OPTIONS
import io.github.couchtracker.intl.datetime.rememberLocalizer
import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.runtime
import io.github.couchtracker.tmdb.toImageModelWithPlaceholder
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ItemPosition
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.actions.markEpisodeAsWatchedAction
import io.github.couchtracker.ui.rememberPlaceholderPainter
import io.github.couchtracker.ui.screens.episodes.navigateToEpisode
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode
import io.github.couchtracker.utils.rememberTickingValue
import kotlinx.datetime.LocalDate
import kotlin.time.Duration

private val STILL_WIDTH = 112.dp
private val STILL_HEIGHT = 64.dp

@Composable
fun EpisodeListItem(
    episode: EpisodeListItemModel,
    position: ItemPosition,
    colors: ListItemColors = ListItemDefaults.colors(),
) {
    val navController = LocalNavController.current

    val dateTimeLocalizer = rememberLocalizer(EPISODE_FIRST_AIRDATE_LOCALIZER_OPTIONS, ::DynamicLocalDateLocalizer)
    val dateTimeText = rememberTickingValue(dateTimeLocalizer, episode.firstAirDate) {
        if (episode.firstAirDate == null) {
            TickingValue(null, null)
        } else {
            dateTimeLocalizer.localizeNow(episode.firstAirDate)
        }
    }

    val markEpisodeAsWatchedAction = markEpisodeAsWatchedAction(episode.showId, episode.episodeId) { watchedSession ->
        WatchedItemSheetMode.New.Episode(
            itemId = episode.episodeId,
            watchedSession = watchedSession,
            mediaRuntime = episode.runtimeDuration,
            mediaLanguages = listOfNotNull(episode.showOriginalLanguage()),
        )
    }
    ListItemWithAction(
        action = markEpisodeAsWatchedAction,
        onClick = {
            navController.navigateToEpisode(episode.episodeId)
        },
        leadingContentHeight = STILL_HEIGHT,
        position = position,
        leadingContent = {
            Surface(shape = MaterialTheme.shapes.small) {
                AsyncImage(
                    with(LocalDensity.current) {
                        episode.backdrop?.getCoilModel(STILL_WIDTH.roundToPx(), STILL_HEIGHT.roundToPx())
                    },
                    modifier = Modifier.size(STILL_WIDTH, STILL_HEIGHT),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    fallback = rememberPlaceholderPainter(PlaceholdersDefaults.SHOW.icon, isError = false),
                    error = rememberPlaceholderPainter(PlaceholdersDefaults.SHOW.icon, isError = true),
                )
            }
        },
        colors = colors,
    ) {
        Column {
            if (episode.name != null) {
                Text(episode.number, style = MaterialTheme.typography.labelSmall)
            }

            if (episode.name != null) {
                Text(episode.name, style = MaterialTheme.typography.titleMedium)
            } else {
                Text(episode.number)
            }

            TagsRow(
                tags = listOfNotNull(
                    dateTimeText,
                    episode.tmdbRating?.formatted,
                    episode.runtime,
                ),
                tagStyle = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

data class EpisodeListItemModel(
    val showId: ExternalShowId,
    val episodeId: ExternalEpisodeId,
    val name: String?,
    val number: String,
    val backdrop: ImageModel?,
    val firstAirDate: LocalDate?,
    val runtime: String?,
    val tmdbRating: TmdbRating?,
    // For the marked as watched dialog.
    // This data will be read when opening the dialog to mark as watched
    val showOriginalLanguage: () -> Bcp47Language?,
    val runtimeDuration: Duration?,
) {

    companion object {
        suspend fun fromTmdbEpisode(
            context: Context,
            show: TmdbShowId,
            episode: TmdbEpisode,
            showOriginalLanguage: () -> Bcp47Language?,
        ): EpisodeListItemModel {
            val id = TmdbExternalEpisodeId(TmdbEpisodeId(TmdbSeasonId(show, episode.seasonNumber), episode.episodeNumber))
            val runtime = episode.runtime()
            return EpisodeListItemModel(
                showId = show.toExternalId(),
                episodeId = id,
                name = episode.name,
                number = context.getString(R.string.episode_x, episode.episodeNumber),
                backdrop = episode.backdropImage?.toImageModelWithPlaceholder(),
                firstAirDate = episode.airDate,
                runtime = runtime?.localize(RUNTIME_LOCALIZER_OPTIONS),
                tmdbRating = TmdbRating.ofOrNull(episode.voteAverage, episode.voteCount),
                runtimeDuration = runtime,
                showOriginalLanguage = showOriginalLanguage,
            )
        }
    }
}
