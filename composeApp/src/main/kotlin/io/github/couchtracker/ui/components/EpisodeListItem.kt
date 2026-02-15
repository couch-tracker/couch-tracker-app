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
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.intl.datetime.DayOfMonthSkeleton
import io.github.couchtracker.intl.datetime.DayOfWeekSkeleton
import io.github.couchtracker.intl.datetime.MonthSkeleton
import io.github.couchtracker.intl.datetime.YearSkeleton
import io.github.couchtracker.intl.datetime.localized
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.runtime
import io.github.couchtracker.tmdb.toImageModelWithPlaceholder
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.format
import io.github.couchtracker.ui.rememberPlaceholderPainter

private val STILL_WIDTH = 112.dp
private val STILL_HEIGHT = 64.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EpisodeListItem(
    episode: EpisodeListItemModel,
    onClick: () -> Unit = {},
    isFirstInList: Boolean = true,
    isLastInList: Boolean = true,
) {
    ListItem(
        onClick = onClick,
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
        shapes = ListItemShapes(isFirstInList = isFirstInList, isLastInList = isLastInList),
    )
}

data class EpisodeListItemModel(
    val name: String?,
    val number: String,
    val backdrop: ImageModel?,
    val firstAirDate: String?,
    val runtime: String?,
    val tmdbRating: TmdbRating?,
) {

    companion object {
        suspend fun fromTmdbEpisode(context: Context, episode: TmdbEpisode): EpisodeListItemModel {
            return EpisodeListItemModel(
                name = episode.name,
                number = context.getString(R.string.episode_x, episode.episodeNumber),
                backdrop = episode.backdropImage?.toImageModelWithPlaceholder(),
                firstAirDate = episode.airDate?.let {
                    PartialDateTime.Local.Date(it)
                        .localized(
                            YearSkeleton.NUMERIC,
                            MonthSkeleton.ABBREVIATED,
                            DayOfMonthSkeleton.NUMERIC,
                            DayOfWeekSkeleton.ABBREVIATED,
                        )
                        .localize()
                },
                runtime = episode.runtime()?.format(),
                tmdbRating = TmdbRating.ofOrNull(episode.voteAverage, episode.voteCount),
            )
        }
    }
}
