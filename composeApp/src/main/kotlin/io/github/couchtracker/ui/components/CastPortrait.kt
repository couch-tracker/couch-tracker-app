package io.github.couchtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import app.moviebase.tmdb.image.TmdbImage
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.model.TmdbAggregateCast
import app.moviebase.tmdb.model.TmdbCast
import coil3.compose.AsyncImage
import io.github.couchtracker.R
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbPerson
import io.github.couchtracker.tmdb.TmdbPersonId
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ImagePreloadOptions
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.rememberPlaceholderPainter
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.pluralStr
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Composable
fun CastPortrait(
    modifier: Modifier,
    person: CastPortraitModel?,
    onClick: ((CastPortraitModel) -> Unit)?,
) {
    PortraitComposable(
        modifier,
        image = { w, h ->
            if (person != null) {
                AsyncImage(
                    model = person.posterModel?.getCoilModel(w, h),
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick != null) { onClick?.invoke(person) },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    fallback = rememberPlaceholderPainter(PlaceholdersDefaults.PERSON.icon, isError = false),
                    error = rememberPlaceholderPainter(PlaceholdersDefaults.PERSON.icon, isError = true),
                )
            }
        },
        label = {
            Text(
                text = person?.name.orEmpty(),
                textAlign = TextAlign.Center,
                minLines = 1,
            )
            person?.roles?.forEach { role ->
                Text(
                    text = role,
                    textAlign = TextAlign.Center,
                    // Same style as ListItem's supporting content
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            person?.episodesCount?.let { episodeCount ->
                Text(
                    text = R.plurals.n_episodes.pluralStr(episodeCount, episodeCount),
                    textAlign = TextAlign.Center,
                    // Same style as ListItem's supporting content
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}

data class CastPortraitModel(
    val person: TmdbPerson,
    val name: String,
    val posterModel: ImageModel?,
    val roles: List<String>,
    val episodesCount: Int? = null,
) {
    companion object {

        suspend fun fromTmdbCast(
            cast: TmdbCast,
            language: TmdbLanguage,
            imagePreloadOptions: ImagePreloadOptions,
        ): CastPortraitModel {
            return CastPortraitModel(
                person = TmdbPerson(TmdbPersonId(cast.id), language),
                name = cast.name,
                roles = listOf(cast.character),
                posterModel = cast.profilePath?.let { path ->
                    TmdbImage(path, TmdbImageType.PROFILE).toImageModel(imagePreloadOptions)
                },
            )
        }

        suspend fun fromTmdbAggregateCast(
            cast: TmdbAggregateCast,
            language: TmdbLanguage,
            imagePreloadOptions: ImagePreloadOptions,
        ): CastPortraitModel {
            return CastPortraitModel(
                person = TmdbPerson(TmdbPersonId(cast.id), language),
                name = cast.name,
                roles = cast.roles.sortedByDescending { it.episodeCount }.map { it.character },
                posterModel = cast.profilePath?.let { path ->
                    TmdbImage(path, TmdbImageType.PROFILE).toImageModel(imagePreloadOptions)
                },
                episodesCount = cast.totalEpisodeCount,
            )
        }
    }
}

@JvmName("TmdbCast_toCastPortraitModel")
suspend fun List<TmdbCast>.toCastPortraitModel(
    language: TmdbLanguage,
    imagePreloadOptions: ImagePreloadOptions,
): List<CastPortraitModel> = coroutineScope {
    map {
        async {
            CastPortraitModel.fromTmdbCast(it, language, imagePreloadOptions)
        }
    }.awaitAll()
}

@JvmName("TmdbAggregateCast_toCastPortraitModel")
suspend fun List<TmdbAggregateCast>.toCastPortraitModel(
    language: TmdbLanguage,
    imagePreloadOptions: ImagePreloadOptions,
): List<CastPortraitModel> = coroutineScope {
    map {
        async {
            CastPortraitModel.fromTmdbAggregateCast(it, language, imagePreloadOptions)
        }
    }.awaitAll()
}
