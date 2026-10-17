package io.github.couchtracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.moviebase.tmdb.model.TmdbShowDetail
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.externalids.TmdbExternalShowId
import io.github.couchtracker.tmdb.BaseTmdbShow
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.toBaseShow
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.components.ShowPortraitModel.DownloadState
import io.github.couchtracker.ui.components.ShowPortraitModel.SecondaryInfo
import io.github.couchtracker.ui.screens.show.navigateToShow
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.error.ApiError
import io.github.couchtracker.utils.error.CouchTrackerResult
import io.github.couchtracker.utils.error.UnsupportedItemError
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.valueOrNull
import app.moviebase.tmdb.model.TmdbShow as TmdbApiTmdbShow

@Composable
fun ShowPortrait(
    modifier: Modifier,
    /** A nullable show will render a placeholder */
    show: Loadable<ShowPortraitModel>,
    secondaryInfo: Set<SecondaryInfo>,
) {
    val navController = LocalNavController.current
    PortraitComposable(
        modifier,
        imageModel = show.valueOrNull()?.let {
            { w, h ->
                it.posterModel?.getCoilModel(w, h)
            }
        },
        elementTypeIcon = PlaceholdersDefaults.SHOW.icon,
        overlayIcon = when (show) {
            is Loadable.Loaded -> when (show.value.downloadState) {
                DownloadState.Downloaded -> null
                DownloadState.Error -> Icons.Outlined.Error
                DownloadState.Loading -> Icons.Outlined.Downloading
                DownloadState.NotFound -> Icons.Outlined.QuestionMark
            }
            Loadable.Loading -> null
        },
        titles = when (show) {
            is Loadable.Loaded -> listOfNotNull(show.value.label)
            // Some default space while downloading
            Loadable.Loading -> listOf(null)
        },
        subtitles = if (secondaryInfo.isNotEmpty()) {
            PortraitComposableDefaults.joinIntoSubtitle(
                tags = show.map { show ->
                    SecondaryInfo.entries
                        .filter { it in secondaryInfo }
                        .mapNotNull { it.toLabel(show) }
                },
            )
        } else {
            emptyList()
        },
        onClick = if (show is Loadable.Loaded) {
            {
                navController.navigateToShow(
                    id = show.value.id,
                    preloadData = show.value.preloadData,
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
    secondaryInfo: Set<SecondaryInfo>,
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
        show = Loadable.Loaded(model),
        secondaryInfo = secondaryInfo,
    )
}

data class ShowPortraitModel(
    val id: ExternalShowId,
    val label: String?,
    val yearStr: String?,
    val ratingStr: String?,
    val posterModel: ImageModel?,
    val preloadData: BaseTmdbShow?,
    val downloadState: DownloadState = DownloadState.Downloaded,
) {

    enum class SecondaryInfo(val toLabel: (ShowPortraitModel) -> String?) {
        Year({ it.yearStr }),
        Rating({ it.ratingStr }),
        ;

        companion object {
            // The year is sometimes necessary to disambiguate shows, e.g. Doctor Who; always display it
            val DEFAULT_SECONDARY_INFO = setOf(Year)
            val EXPLORE_SECONDARY_INFO = setOf(Year, Rating)
        }
    }

    enum class DownloadState {
        Downloaded,
        Error,
        Loading,
        NotFound,
    }

    companion object {

        fun fromApiTmdbShow(
            details: TmdbApiTmdbShow,
            preloadData: BaseTmdbShow?,
            rating: TmdbRating?,
        ): ShowPortraitModel {
            return ShowPortraitModel(
                id = TmdbExternalShowId(TmdbShowId(details.id)),
                label = details.name,
                yearStr = details.firstAirDate?.year?.toString(),
                ratingStr = rating?.formatted,
                posterModel = details.posterImage?.toImageModel(),
                preloadData = preloadData,
            )
        }

        fun fromApiTmdbShow(
            details: TmdbShowDetail,
            preloadData: BaseTmdbShow?,
            rating: TmdbRating?,
        ): ShowPortraitModel {
            return ShowPortraitModel(
                id = TmdbExternalShowId(TmdbShowId(details.id)),
                label = details.name,
                yearStr = details.firstAirDate?.year?.toString(),
                ratingStr = rating?.formatted,
                posterModel = details.posterImage?.toImageModel(),
                preloadData = preloadData,
            )
        }

        fun forDownloadState(id: ExternalShowId, state: DownloadState, label: String? = null): ShowPortraitModel {
            return ShowPortraitModel(
                id = id,
                label = label,
                yearStr = null,
                ratingStr = null,
                posterModel = null,
                downloadState = state,
                preloadData = null,
            )
        }
    }
}

fun TmdbApiTmdbShow.toShowPortraitModel(language: TmdbLanguage, rating: TmdbRating?): ShowPortraitModel {
    return ShowPortraitModel.fromApiTmdbShow(this, preloadData = this.toBaseShow(language), rating = rating)
}

fun TmdbShowDetail.toShowPortraitModel(language: TmdbLanguage, rating: TmdbRating?): ShowPortraitModel {
    return ShowPortraitModel.fromApiTmdbShow(this, preloadData = this.toBaseShow(language), rating = rating)
}
