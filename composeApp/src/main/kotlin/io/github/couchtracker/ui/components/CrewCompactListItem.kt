package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.moviebase.tmdb.image.TmdbImage
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.model.TmdbCrew
import app.moviebase.tmdb.model.TmdbDepartment
import coil3.compose.AsyncImage
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbPerson
import io.github.couchtracker.tmdb.TmdbPersonId
import io.github.couchtracker.tmdb.title
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ImagePreloadOptions
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.rememberPlaceholderPainter
import io.github.couchtracker.ui.toImageModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Composable
fun CrewCompactListItem(crew: CrewCompactListItemModel) {
    CompactListItem(
        leadingContent = {
            Surface(
                shape = CircleShape,
                shadowElevation = 8.dp,
                tonalElevation = 8.dp,
                modifier = Modifier.size(40.dp),
            ) {
                BoxWithConstraints(contentAlignment = Alignment.Companion.BottomStart) {
                    AsyncImage(
                        model = crew.posterModel?.getCoilModel(
                            this.constraints.maxWidth,
                            this.constraints.maxHeight,
                        ),
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = null,
                        contentScale = ContentScale.Companion.Crop,
                        fallback = rememberPlaceholderPainter(PlaceholdersDefaults.PERSON.icon, isError = false),
                        error = rememberPlaceholderPainter(PlaceholdersDefaults.PERSON.icon, isError = true),
                    )
                }
            }
        },
        headlineContent = { Text(crew.name) },
        supportingContent = {
            Text(
                text = formatAndList(crew.departments.map { it.title().string() }),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

data class CrewCompactListItemModel(
    val person: TmdbPerson,
    val name: String,
    val posterModel: ImageModel?,
    val departments: Set<TmdbDepartment>,
) {
    companion object {
        suspend fun fromTmdbCrew(
            crew: List<TmdbCrew>,
            language: TmdbLanguage,
            imagePreloadOptions: ImagePreloadOptions,
        ): CrewCompactListItemModel {
            require(crew.isNotEmpty()) { "At least one crew has to be specified." }
            val base = crew.first()
            require(crew.all { it.id == base.id })
            require(crew.all { it.name == base.name })
            require(crew.all { it.profilePath == base.profilePath })
            return CrewCompactListItemModel(
                person = TmdbPerson(TmdbPersonId(base.id), language),
                name = base.name,
                departments = crew.mapNotNull { it.department }.toSet(),
                posterModel = base.profilePath?.let { path ->
                    TmdbImage(path, TmdbImageType.PROFILE).toImageModel(imagePreloadOptions)
                },
            )
        }
    }
}

suspend fun List<TmdbCrew>.toCrewCompactListItemModel(
    language: TmdbLanguage,
    imagePreloadOptions: ImagePreloadOptions,
): List<CrewCompactListItemModel> = coroutineScope {
    groupBy { it.id }.map { (_, crew) ->
        async {
            CrewCompactListItemModel.fromTmdbCrew(crew, language, imagePreloadOptions)
        }
    }.awaitAll()
}
