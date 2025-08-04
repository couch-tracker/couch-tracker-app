package io.github.couchtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbShow
import io.github.couchtracker.tmdb.toInternalTmdbShow
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ImagePreloadOptions
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.rememberPlaceholderPainter
import io.github.couchtracker.ui.toImageModel
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
                show.year != null -> "${show.name} (${show.year})" // TODO translate
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
    val show: TmdbShow,
    val name: String,
    val year: Int?,
    val posterModel: ImageModel?,
) {
    companion object {

        suspend fun fromApiTmdbShow(
            show: TmdbShow,
            details: TmdbApiTmdbShow,
            imagePreloadOptions: ImagePreloadOptions,
        ): ShowPortraitModel {
            return ShowPortraitModel(
                show = show,
                name = details.name,
                year = details.firstAirDate?.year,
                posterModel = details.posterImage?.toImageModel(imagePreloadOptions),
            )
        }
    }
}

suspend fun TmdbApiTmdbShow.toShowPortraitModels(
    language: TmdbLanguage,
    imagePreloadOptions: ImagePreloadOptions,
): ShowPortraitModel {
    return ShowPortraitModel.fromApiTmdbShow(toInternalTmdbShow(language), this, imagePreloadOptions)
}
