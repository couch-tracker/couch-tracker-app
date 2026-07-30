package io.github.couchtracker.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.mmauro.datetimepolyglot.localizers.localize
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.externalids.TmdbExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.TmdbExternalShowId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedEpisodeSessionWrapper
import io.github.couchtracker.intl.datetime.DynamicDateAbsoluteTimeWithDurationLocalizer
import io.github.couchtracker.intl.datetime.DynamicDateAbsoluteTimeWithDurationOptions
import io.github.couchtracker.intl.datetime.rememberLocalizer
import io.github.couchtracker.tmdb.BaseTmdbShow
import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ItemPosition
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.rememberPlaceholderPainter
import io.github.couchtracker.ui.screens.episodes.navigateToEpisode
import io.github.couchtracker.ui.screens.show.navigateToShow
import io.github.couchtracker.ui.seasonEpisodeNumberToString
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.rememberTickingValue
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

private val POSTER_HEIGHT = 64.dp
private val POSTER_WIDTH = POSTER_HEIGHT * 2 / 3

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpNextListItem(
    upNext: UpNextListItemModel,
    position: ItemPosition,
) {
    val dateTimeLocalizer = rememberLocalizer(DynamicDateAbsoluteTimeWithDurationOptions(), ::DynamicDateAbsoluteTimeWithDurationLocalizer)
    val navController = LocalNavController.current

    val airInstant = remember {
        Clock.System.now() + Random.nextLong(-14.days.inWholeMinutes, 14.days.inWholeMinutes).minutes
    }
    val dateTimeText = rememberTickingValue(dateTimeLocalizer, airInstant) {
        dateTimeLocalizer.localize(airInstant.toLocalDateTime(TimeZone.currentSystemDefault()))
    }

    ListItem(
        onClick = {
            navController.navigateToEpisode(upNext.episodeId)
        },
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
        overlineContent = {
            Text(upNext.episodeLabel)
        },
        content = {
            Column {
                if (upNext.showName != null) {
                    Text(upNext.showName)
                } else {
                    Text("Ops")
                }
                Text(dateTimeText, style = MaterialTheme.typography.labelMedium)
            }
        },
        trailingContent = {
            Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null)
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        shapes = ListItemShapes(position),
    )
}

data class UpNextListItemModel(
    val showId: ExternalShowId,
    val watchSession: WatchedEpisodeSessionWrapper?,
    val showPreloadData: BaseTmdbShow,
    val episodeId: ExternalEpisodeId,
    val posterModel: ImageModel?,
    val showName: String?,
    val episodeLabel: String,
) {

    companion object {

        fun withShowData(
            context: Context,
            showPreloadData: BaseTmdbShow,
            watchSession: WatchedEpisodeSessionWrapper?,
            seasonNumber: Int,
            episodeNumber: Int,
        ): UpNextListItemModel {
            val showId = showPreloadData.key.id
            val seasonId = TmdbSeasonId(showId, seasonNumber)
            val episodeId = TmdbExternalEpisodeId(TmdbEpisodeId(seasonId, episodeNumber))
            return UpNextListItemModel(
                showId = TmdbExternalShowId(showId),
                watchSession = watchSession,
                episodeId = episodeId,
                showName = showPreloadData.name,
                showPreloadData = showPreloadData,
                episodeLabel = seasonEpisodeNumberToString(context, seasonNumber, episodeNumber),
                posterModel = showPreloadData.poster?.toImageModel(),
            )
        }
    }
}
