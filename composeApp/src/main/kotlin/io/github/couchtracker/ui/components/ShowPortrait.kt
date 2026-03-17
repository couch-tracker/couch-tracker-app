package io.github.couchtracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.couchtracker.R
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.tmdbShowId
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.PlaceholdersDefaults
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
        imageModel = show?.let {
            { w, h ->
                show.posterModel?.getCoilModel(w, h)
            }
        },
        elementTypeIcon = PlaceholdersDefaults.SHOW.icon,
        label = when {
            show?.name == null -> ""
            show.year != null -> R.string.item_tile_with_year.str(show.name, show.year)
            else -> show.name
        },
        labelMinLines = if (show == null) 2 else 1,
        onClick = show?.let {
            {
                onClick(show)
            }
        },
    )
}

data class ShowPortraitModel(
    val id: TmdbShowId,
    val name: String?,
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
