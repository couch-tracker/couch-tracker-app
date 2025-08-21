package io.github.couchtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import io.github.couchtracker.R
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.tmdbShowId
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.rememberPlaceholderPainter
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.str
import app.moviebase.tmdb.model.TmdbShow as TmdbApiTmdbShow

@Composable
fun ShowPortrait(
    modifier: Modifier,
    /** A nullable show will render a placeholder */
    show: ShowPortraitModel?,
    onClick: (ShowPortraitModel) -> Unit,
) {
    PortraitComposable(
        modifier,
        image = { w, h ->
            if (show != null) {
                AsyncImage(
                    model = show.posterModel?.getCoilModel(w, h),
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            onClick(show)
                        },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    fallback = rememberPlaceholderPainter(PlaceholdersDefaults.SHOW.icon, isError = false),
                    error = rememberPlaceholderPainter(PlaceholdersDefaults.SHOW.icon, isError = true),
                )
            }
        },
        label = {
            val label = when {
                show == null -> ""
                show.year != null -> R.string.item_tile_with_year.str(show.name, show.year)
                else -> show.name
            }
            Text(
                label,
                textAlign = TextAlign.Center,
                minLines = if (show == null) 2 else 1,
            )
        },
    )
}

data class ShowPortraitModel(
    val id: TmdbShowId,
    val name: String,
    val year: Int?,
    val posterModel: ImageModel?,
) {
    companion object {

        fun fromApiTmdbShow(
            id: TmdbShowId,
            details: TmdbApiTmdbShow,
        ): ShowPortraitModel {
            return ShowPortraitModel(
                id = id,
                name = details.name,
                year = details.firstAirDate?.year,
                posterModel = details.posterImage?.toImageModel(),
            )
        }
    }
}

fun TmdbApiTmdbShow.toShowPortraitModels(): ShowPortraitModel {
    return ShowPortraitModel.fromApiTmdbShow(tmdbShowId, this)
}
