package io.github.couchtracker.ui.screens.show

import android.app.Application
import androidx.compose.runtime.getValue
import app.moviebase.tmdb.image.TmdbImageType
import io.github.couchtracker.db.profile.show.ExternalShowId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.linearize
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.components.toCastPortraitModel
import io.github.couchtracker.ui.components.toCrewCompactListItemModel
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.ApiLoadable
import io.github.couchtracker.utils.mapResult
import kotlinx.coroutines.flow.map

class ShowScreenViewModel(
    application: Application,
    externalShowId: ExternalShowId,
    showId: TmdbShowId,
) : AbsShowScreenViewModel(
    application = application,
    externalShowId = externalShowId,
    showId = showId,
) {

    data class Credits(
        val cast: List<CastPortraitModel>,
        val crew: List<CrewCompactListItemModel>,
    )

    val images: ApiLoadable<List<ImageModel>> by loadable(
        flow = item
            .callApi { it.images }
            .map { result ->
                result.mapResult { images ->
                    images
                        .linearize()
                        .map { it.toImageModel(TmdbImageType.BACKDROP) }
                }
            },
    )

    val credits: ApiLoadable<Credits> by loadable(
        flow = item
            .callApi { it.aggregateCredits }
            .map { result ->
                result.mapResult { credits ->
                    Credits(
                        cast = credits.cast.toCastPortraitModel(application),
                        crew = credits.crew.toCrewCompactListItemModel(application),
                    )
                }
            },
    )
}
