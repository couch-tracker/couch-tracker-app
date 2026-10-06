package io.github.couchtracker.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import io.github.couchtracker.settings.StyleAndBehaviorSettings
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ItemPosition
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.screens.show.ShowScreenViewModelHelper
import io.github.couchtracker.ui.seasonEpisodeNumberToString
import io.github.couchtracker.utils.error.ApiLoadable

@Composable
fun WatchedItemListItem(
    model: WatchedItemListItemModel,
    position: ItemPosition,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        shapes = ListItemShapes(position),
    ) {
        Column {
            val text = when (model) {
                is WatchedItemListItemModel.EpisodeInShow -> model.episodeLabel
            }
            Text(text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}


sealed interface WatchedItemListItemModel {

    data class EpisodeInShow(
        val backdropModel: ImageModel?,
        val episodeLabel: String,
    ) : WatchedItemListItemModel {

        companion object {

            fun withShowData(
                context: Context,
                episodeFormatting: StyleAndBehaviorSettings.EpisodeNumberFormattingOption,
                episode: ApiLoadable<ShowScreenViewModelHelper.EpisodeDetails?>,
            ): EpisodeInShow {
                val episodeNumberLabel = seasonEpisodeNumberToString(
                    context = context,
                    formatting = episodeFormatting,
                    seasonNumber = season.number,
                    episodeNumber = episode.number,
                    episodeName = episode.name,
                )
                return EpisodeInShow(
                    backdropModel = episode.backdrop,
                    episodeLabel = episodeNumberLabel,
                )
            }

        }
    }
}
