package io.github.couchtracker.ui.components

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.moviebase.tmdb.model.TmdbShowDetail
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.externalids.TmdbExternalShowId
import io.github.couchtracker.tmdb.BaseTmdbShow
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.toBaseShow
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.components.ShowPortraitModel.DownloadState
import io.github.couchtracker.ui.screens.show.navigateToShow
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.error.ApiError
import io.github.couchtracker.utils.error.CouchTrackerResult
import io.github.couchtracker.utils.error.UnsupportedItemError
import app.moviebase.tmdb.model.TmdbShow as TmdbApiTmdbShow

@Composable
fun ShowPortrait(
    modifier: Modifier,
    /** A nullable show will render a placeholder */
    show: ShowPortraitModel?,
) {
    val navController = LocalNavController.current
    PortraitComposable(
        modifier,
        imageModel = show?.let {
            { w, h ->
                show.posterModel?.getCoilModel(w, h)
            }
        },
        elementTypeIcon = PlaceholdersDefaults.SHOW.icon,
        overlayIcon = when (show?.downloadState) {
            null, DownloadState.Downloaded -> null
            DownloadState.Error -> Icons.Outlined.Error
            DownloadState.Loading -> Icons.Outlined.Downloading
            DownloadState.NotFound -> Icons.Outlined.QuestionMark
        },
        label = show?.label.orEmpty(),
        labelMinLines = if (show == null) 2 else 1,
        onClick = if (show?.id != null) {
            {
                navController.navigateToShow(
                    id = show.id,
                    preloadData = show.preloadData,
                )
            }
        } else {
            null
        },
    )
}

@Composable
fun ShowPortrait(
    modifier: Modifier,
    showId: ExternalShowId,
    showResult: CouchTrackerResult<ShowPortraitModel>,
) {
    val model = when (showResult) {
        is Result.Value -> showResult.value
        is Result.Error -> ShowPortraitModel.forDownloadState(
            id = showId,
            state = when (showResult.error) {
                is ApiError.ItemNotFound -> DownloadState.NotFound
                is UnsupportedItemError -> DownloadState.NotFound
                else -> DownloadState.Error
            },
            label = showId.serialize(),
        )
    }
    ShowPortrait(
        modifier = modifier,
        show = model,
    )
}

data class ShowPortraitModel(
    val id: ExternalShowId,
    val label: String?,
    val posterModel: ImageModel?,
    val preloadData: BaseTmdbShow?,
    val downloadState: DownloadState = DownloadState.Downloaded,
) {

    enum class DownloadState {
        Downloaded,
        Error,
        Loading,
        NotFound,
    }

    companion object {

        fun fromApiTmdbShow(
            context: Context,
            details: TmdbApiTmdbShow,
            preloadData: BaseTmdbShow?,
        ): ShowPortraitModel {
            return ShowPortraitModel(
                id = TmdbExternalShowId(TmdbShowId(details.id)),
                label = labelFromNameYear(context, details.name, details.firstAirDate?.year),
                posterModel = details.posterImage?.toImageModel(),
                preloadData = preloadData,
            )
        }

        fun fromApiTmdbShow(
            context: Context,
            details: TmdbShowDetail,
            preloadData: BaseTmdbShow?,
        ): ShowPortraitModel {
            return ShowPortraitModel(
                id = TmdbExternalShowId(TmdbShowId(details.id)),
                label = labelFromNameYear(context, details.name, details.firstAirDate?.year),
                posterModel = details.posterImage?.toImageModel(),
                preloadData = preloadData,
            )
        }

        fun forDownloadState(id: ExternalShowId, state: DownloadState, label: String? = null): ShowPortraitModel {
            return ShowPortraitModel(
                id = id,
                label = label,
                posterModel = null,
                downloadState = state,
                preloadData = null,
            )
        }

        private fun labelFromNameYear(context: Context, name: String?, year: Int?): String? {
            return if (name != null && year != null) {
                context.getString(R.string.item_tile_with_year, name, year)
            } else {
                name
            }
        }
    }
}

fun TmdbApiTmdbShow.toShowPortraitModels(context: Context, language: TmdbLanguage): ShowPortraitModel {
    return ShowPortraitModel.fromApiTmdbShow(context, this, preloadData = this.toBaseShow(language))
}

fun TmdbShowDetail.toShowPortraitModels(context: Context, language: TmdbLanguage): ShowPortraitModel {
    return ShowPortraitModel.fromApiTmdbShow(context, this, preloadData = this.toBaseShow(language))
}
