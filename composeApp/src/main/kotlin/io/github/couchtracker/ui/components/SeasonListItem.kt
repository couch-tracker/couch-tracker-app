package io.github.couchtracker.ui.components

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.moviebase.tmdb.model.TmdbSeason
import coil3.compose.AsyncImage
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.intl.datetime.MonthSkeleton
import io.github.couchtracker.intl.datetime.YearSkeleton
import io.github.couchtracker.intl.datetime.localized
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.toImageModelWithPlaceholder
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ListItemPosition
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.SeasonNames
import io.github.couchtracker.ui.names
import io.github.couchtracker.ui.rememberPlaceholderPainter
import kotlinx.datetime.yearMonth

private val POSTER_WIDTH = 64.dp
private val POSTER_HEIGHT = 96.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SeasonListItem(
    season: SeasonListItemModel,
    onClick: () -> Unit = {},
    position: ListItemPosition,
) {
    ListItem(
        onClick = onClick,
        leadingContent = {
            Surface(shape = MaterialTheme.shapes.small) {
                AsyncImage(
                    with(LocalDensity.current) {
                        season.poster?.getCoilModel(POSTER_WIDTH.roundToPx(), POSTER_HEIGHT.roundToPx())
                    },
                    modifier = Modifier.size(POSTER_WIDTH, POSTER_HEIGHT),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    fallback = rememberPlaceholderPainter(PlaceholdersDefaults.SHOW.icon, isError = false),
                    error = rememberPlaceholderPainter(PlaceholdersDefaults.SHOW.icon, isError = true),
                )
            }
        },
        overlineContent = {
            if (season.names.secondaryName != null) {
                Text(season.names.secondaryName)
            }
        },
        content = {
            Text(season.names.mainName, style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = {
            TagsRow(
                tags = listOfNotNull(
                    season.episodesCount,
                    season.tmdbRating?.formatted,
                ),
                tagStyle = LocalTextStyle.current,
            )
        },
        trailingContent = {
            if (season.firstAirDate != null) {
                Text(season.firstAirDate)
            }
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        shapes = ListItemShapes(position),
    )
}

data class SeasonListItemModel(
    val number: Int,
    val names: SeasonNames,
    val poster: ImageModel?,
    val firstAirDate: String?,
    val episodesCount: String,
    val tmdbRating: TmdbRating?,
) {

    companion object {
        suspend fun fromTmdbSeason(context: Context, season: TmdbSeason): SeasonListItemModel {
            val episodes = season.episodeCount ?: 0
            return SeasonListItemModel(
                number = season.seasonNumber,
                names = season.names(context),
                poster = season.posterImage?.toImageModelWithPlaceholder(),
                firstAirDate = season.airDate?.let {
                    PartialDateTime.Local.YearMonth(it.yearMonth)
                        .localized(YearSkeleton.NUMERIC, MonthSkeleton.ABBREVIATED)
                        .localize()
                },
                episodesCount = context.resources.getQuantityString(
                    R.plurals.n_episodes,
                    episodes,
                    episodes,
                ),
                tmdbRating = TmdbRating.ofOrNull(season.voteAverage, count = null),
            )
        }
    }
}
