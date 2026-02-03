package io.github.couchtracker.ui.components

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
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
import io.github.couchtracker.tmdb.toImageModelWithPlaceholder
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.rememberPlaceholderPainter
import kotlinx.datetime.LocalDate

private val POSTER_WIDTH = 64.dp
private val POSTER_HEIGHT = 96.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SeasonListItem(
    season: SeasonListItemModel,
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
            if (season.displayDefaultName) {
                Text(season.defaultName)
            }
        },
        content = {
            Text(season.name, style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = {
            Text(season.episodesCountString)
        },
        trailingContent = {
            if (season.firstAirDateString != null) {
                Text(season.firstAirDateString)
            }
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        shapes = ListItemShapes(isFirstInList = isFirstInList, isLastInList = isLastInList),
    )
}

data class SeasonListItemModel(
    val number: Int,
    val name: String,
    val defaultName: String,
    val poster: ImageModel?,
    val firstAirDate: LocalDate?,
    val firstAirDateString: String?,
    val episodesCount: Int,
    val episodesCountString: String,
) {
    val displayDefaultName = !name.equals(defaultName, ignoreCase = true) && name != "Series $number"

    companion object {
        fun fromTmdbSeason(context: Context, season: TmdbSeason): SeasonListItemModel {
            val episodes = season.episodeCount ?: 0
            return SeasonListItemModel(
                number = season.seasonNumber,
                name = season.name,
                defaultName = if (season.seasonNumber == 0) {
                    context.getString(R.string.season_specials)
                } else {
                    context.getString(R.string.season_x, season.seasonNumber)
                },
                poster = season.posterImage?.toImageModelWithPlaceholder(),
                firstAirDate = season.airDate,
                firstAirDateString = season.airDate?.let {
                    PartialDateTime.Local.YearMonth(it.year, it.month)
                        .localized(YearSkeleton.NUMERIC, MonthSkeleton.ABBREVIATED)
                        .localize()
                },
                episodesCount = episodes,
                episodesCountString = context.resources.getQuantityString(
                    R.plurals.n_episodes,
                    episodes,
                    episodes,
                ),
            )
        }
    }
}
