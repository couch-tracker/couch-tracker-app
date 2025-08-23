package io.github.couchtracker.ui.screens.movie

import android.app.Application
import androidx.compose.runtime.getValue
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.model.TmdbCrew
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.movie.ExternalMovieId
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.directors
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

class MovieScreenViewModel(
    application: Application,
    externalMovieId: ExternalMovieId,
    movieId: TmdbMovieId,
) : AbsMovieScreenViewModel(
    application = application,
    externalMovieId = externalMovieId,
    movieId = movieId,
) {

    data class Credits(
        val directors: List<TmdbCrew>,
        val cast: List<CastPortraitModel>,
        val crew: List<CrewCompactListItemModel>,
        val directorsString: String?,
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
            .callApi { it.credits }
            .map { result ->
                result.mapResult { credits ->
                    val directors = credits.crew.directors()
                    Credits(
                        directors = directors,
                        cast = credits.cast.toCastPortraitModel(),
                        crew = credits.crew.toCrewCompactListItemModel(application),
                        directorsString = if (directors.isEmpty()) {
                            null
                        } else {
                            application.getString(R.string.movie_by_director, formatAndList(directors.map { it.name }))
                        },
                    )
                }
            },
    )
}
