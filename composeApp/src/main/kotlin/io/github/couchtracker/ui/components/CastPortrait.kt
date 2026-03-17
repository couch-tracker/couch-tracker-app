package io.github.couchtracker.ui.components

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.moviebase.tmdb.image.TmdbImage
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.model.TmdbAggregateCast
import app.moviebase.tmdb.model.TmdbCast
import io.github.couchtracker.R
import io.github.couchtracker.tmdb.TmdbPersonId
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.toImageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

@Composable
fun CastPortrait(
    modifier: Modifier,
    person: CastPortraitModel?,
    onClick: (() -> Unit)?,
) {
    PortraitComposable(
        modifier,
        imageModel = person?.let {
            { w, h ->
                person.posterModel?.getCoilModel(w, h)
            }
        },
        elementTypeIcon = PlaceholdersDefaults.PERSON.icon,
        label = person?.name.orEmpty(),
        labelMinLines = 1,
        extraContent = {
            val subtitleItems = buildList {
                if (person == null) {
                    add(null)
                } else {
                    addAll(person.roles)
                    person.episodesCountString?.let { add(it) }
                }
            }
            subtitleItems.forEach { subtitle ->
                Text(
                    text = subtitle.orEmpty(),
                    textAlign = TextAlign.Center,
                    // Same style as ListItem's supporting content
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    minLines = 1,
                )
            }
        },
        onClick = onClick,
    )
}

data class CastPortraitModel(
    val id: TmdbPersonId,
    val name: String?,
    val posterModel: ImageModel?,
    val roles: List<String>,
    val episodesCount: Int? = null,
    val episodesCountString: String? = null,
) {
    companion object {

        fun fromTmdbCast(cast: TmdbCast): CastPortraitModel {
            return CastPortraitModel(
                id = TmdbPersonId(cast.id),
                name = cast.name,
                roles = listOfNotNull(cast.character),
                posterModel = cast.profilePath?.let { path ->
                    TmdbImage(path, TmdbImageType.PROFILE).toImageModel()
                },
            )
        }

        suspend fun fromTmdbAggregateCast(
            context: Context,
            cast: TmdbAggregateCast,
        ): CastPortraitModel {
            return withContext(Dispatchers.Default) {
                CastPortraitModel(
                    id = TmdbPersonId(cast.id),
                    name = cast.name,
                    roles = cast.roles.sortedByDescending { it.episodeCount }.mapNotNull { it.character },
                    posterModel = cast.profilePath?.let { path ->
                        TmdbImage(path, TmdbImageType.PROFILE).toImageModel()
                    },
                    episodesCount = cast.totalEpisodeCount,
                    episodesCountString = context.resources.getQuantityString(
                        R.plurals.n_episodes,
                        cast.totalEpisodeCount,
                        cast.totalEpisodeCount,
                    ),
                )
            }
        }
    }
}

@JvmName("TmdbCast_toCastPortraitModel")
fun List<TmdbCast>.toCastPortraitModel(): List<CastPortraitModel> {
    return map { CastPortraitModel.fromTmdbCast(it) }
}

@JvmName("TmdbAggregateCast_toCastPortraitModel")
suspend fun List<TmdbAggregateCast>.toCastPortraitModel(context: Context): List<CastPortraitModel> = coroutineScope {
    map {
        async {
            CastPortraitModel.fromTmdbAggregateCast(context, it)
        }
    }.awaitAll()
}
