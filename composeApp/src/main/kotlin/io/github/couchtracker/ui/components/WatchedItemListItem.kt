package io.github.couchtracker.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import io.github.couchtracker.db.profile.model.watchedItem.localizedWatchAt
import io.github.couchtracker.settings.StyleAndBehaviorSettings
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ItemPosition
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.screens.show.ShowScreenViewModelHelper
import io.github.couchtracker.ui.seasonEpisodeNumberToString
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.error.ApiLoadable
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.valueOrNull

@Composable
fun WatchedItemListItem(
    model: WatchedItemListItemModel.EpisodeInShow,
    position: ItemPosition,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        shapes = ListItemShapes(position),
    ) {
        Column {
            if (model.headline != null) {
                Text(model.headline, style = MaterialTheme.typography.titleSmall)
            }
            if (model.title != null) {
                Text(model.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (model.subtitle != null) {
                Text(model.subtitle, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}


sealed interface WatchedItemListItemModel {

    data class EpisodeInShow(
        val watchedItem: WatchedItemWrapper,
        val backdropModel: ImageModel?,
        val headline: String?,
        val title: String?,
        val subtitle: String?,
    ) : WatchedItemListItemModel {

        companion object {

            fun withShowData(
                context: Context,
                watchedItem: WatchedItemWrapper,
                episodeFormatting: StyleAndBehaviorSettings.EpisodeNumberFormattingOption,
                episode: ApiLoadable<ShowScreenViewModelHelper.EpisodeDetails?>,
            ): EpisodeInShow {
                return EpisodeInShow(
                    watchedItem = watchedItem,
                    backdropModel = episode.resultValueOrNull()?.backdrop,
                    headline = null,
                    title = episode.resultValueOrNull()?.let {
                        seasonEpisodeNumberToString(
                            context = context,
                            formatting = episodeFormatting,
                            seasonNumber = it.seasonNumber,
                            episodeNumber = it.episodeNumber,
                            episodeName = it.name,
                        )
                    } ?: watchedItem.itemId.toString(), // TODO pensare meglio a questo fallback
                    subtitle = watchedItem.localizedWatchAt(context, includeTimeZone = false),
                )
            }
        }
    }
}
