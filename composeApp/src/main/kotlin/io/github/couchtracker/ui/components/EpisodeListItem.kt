package io.github.couchtracker.ui.components

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import dev.mmauro.datetimepolyglot.localizers.absolute.DateComponents
import dev.mmauro.datetimepolyglot.localizers.absolute.localize
import dev.mmauro.datetimepolyglot.styles.DayOfMonthStyle
import dev.mmauro.datetimepolyglot.styles.DayOfWeekStyle
import dev.mmauro.datetimepolyglot.styles.MonthStyle
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.TmdbExternalEpisodeId
import io.github.couchtracker.intl.datetime.RUNTIME_LOCALIZER_OPTIONS
import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.runtime
import io.github.couchtracker.tmdb.toImageModelWithPlaceholder
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ItemPosition
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.rememberPlaceholderPainter
import io.github.couchtracker.ui.screens.episodes.navigateToEpisode

private val STILL_WIDTH = 112.dp
private val STILL_HEIGHT = 64.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EpisodeListItem(
    episode: EpisodeListItemModel,
    position: ItemPosition,
) {
    val navController = LocalNavController.current
    ListItem(
        onClick = {
            navController.navigateToEpisode(episode.episodeId)
        },
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
        overlineContent = {
            if (episode.name != null) {
                Text(episode.number)
            }
        },
        content = {
            if (episode.name != null) {
                Text(episode.name, style = MaterialTheme.typography.titleMedium)
            } else {
                Text(episode.number)
            }
        },
        supportingContent = {
            TagsRow(
                tags = listOfNotNull(
                    episode.firstAirDate,
                    episode.tmdbRating?.formatted,
                    episode.runtime,
                ),
                tagStyle = MaterialTheme.typography.labelSmall,
            )
        },
        trailingContent = {
            Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null)
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        shapes = ListItemShapes(position),
    )
}

data class EpisodeListItemModel(
    val episodeId: ExternalEpisodeId,
    val name: String?,
    val number: String,
    val backdrop: ImageModel?,
    val firstAirDate: String?,
    val runtime: String?,
    val tmdbRating: TmdbRating?,
) {

    companion object {
        private val FIRST_AIR_DATE_OPTIONS = DateComponents(
            monthStyle = MonthStyle.ABBREVIATED,
            dayOfMonthStyle = DayOfMonthStyle.NUMERIC,
            dayOfWeekStyle = DayOfWeekStyle.ABBREVIATED,
        )

        suspend fun fromTmdbEpisode(context: Context, show: TmdbShowId, episode: TmdbEpisode): EpisodeListItemModel {
            val id = TmdbExternalEpisodeId(TmdbEpisodeId(TmdbSeasonId(show, episode.seasonNumber), episode.episodeNumber))
            return EpisodeListItemModel(
                episodeId = id,
                name = episode.name,
                number = context.getString(R.string.episode_x, episode.episodeNumber),
                backdrop = episode.backdropImage?.toImageModelWithPlaceholder(),
                firstAirDate = episode.airDate?.localize(FIRST_AIR_DATE_OPTIONS),
                runtime = episode.runtime()?.localize(RUNTIME_LOCALIZER_OPTIONS),
                tmdbRating = TmdbRating.ofOrNull(episode.voteAverage, episode.voteCount),
            )
        }
    }
}
