package io.github.couchtracker.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.moviebase.tmdb.image.TmdbImage
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.model.TmdbAggregateCrew
import app.moviebase.tmdb.model.TmdbAnyPerson
import app.moviebase.tmdb.model.TmdbCrew
import app.moviebase.tmdb.model.TmdbDepartment
import coil3.compose.AsyncImage
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.tmdb.TmdbPersonId
import io.github.couchtracker.tmdb.title
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.rememberPlaceholderPainter
import io.github.couchtracker.ui.toImageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

private val AVATAR_IMAGE_SIZE = 40.dp

@Composable
fun CrewCompactListItem(crew: CrewCompactListItemModel?) {
    CompactListItem(
        leadingContent = {
            Surface(
                shape = CircleShape,
                shadowElevation = 8.dp,
                tonalElevation = 8.dp,
                modifier = Modifier.size(AVATAR_IMAGE_SIZE),
            ) {
                val size = LocalDensity.current.run { AVATAR_IMAGE_SIZE.roundToPx() }
                if (crew != null) {
                    AsyncImage(
                        model = crew.posterModel?.getCoilModel(size, size),
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        fallback = rememberPlaceholderPainter(PlaceholdersDefaults.PERSON.icon, isError = false),
                        error = rememberPlaceholderPainter(PlaceholdersDefaults.PERSON.icon, isError = true),
                    )
                }
            }
        },
        headlineContent = {
            if (crew?.name != null) {
                Text(crew.name)
            }
        },
        supportingContent = {
            if (crew?.departmentsString != null) {
                Text(text = crew.departmentsString, overflow = TextOverflow.Ellipsis)
            }
        },
    )
}

data class CrewCompactListItemModel(
    val id: TmdbPersonId,
    val name: String?,
    val posterModel: ImageModel?,
    val departments: Set<TmdbDepartment>,
    val departmentsString: String?,
) {
    companion object {
        suspend fun <T : TmdbAnyPerson> fromTmdbCrew(
            context: Context,
            crew: List<T>,
            department: (T) -> TmdbDepartment?,
        ): CrewCompactListItemModel {
            return withContext(Dispatchers.Default) {
                require(crew.isNotEmpty()) { "At least one crew has to be specified." }
                val base = crew.first()
                require(crew.all { it.id == base.id })
                require(crew.all { it.name == base.name })
                require(crew.all { it.profilePath == base.profilePath })
                val departments = crew.mapNotNull { department(it) }.toSet()
                CrewCompactListItemModel(
                    id = TmdbPersonId(base.id),
                    name = base.name,
                    departments = departments,
                    departmentsString = if (departments.isNotEmpty()) {
                        formatAndList(departments.map { context.getString(it.title().resource) })
                    } else {
                        null
                    },
                    posterModel = base.profilePath?.let { path ->
                        TmdbImage(path, TmdbImageType.PROFILE).toImageModel()
                    },
                )
            }
        }
    }
}

@JvmName("TmdbCrew_toCrewCompactListItemModel")
suspend fun List<TmdbCrew>.toCrewCompactListItemModel(context: Context): List<CrewCompactListItemModel> = coroutineScope {
    groupBy { it.id }.map { (_, crew) ->
        async {
            CrewCompactListItemModel.fromTmdbCrew(
                context = context,
                crew = crew,
                department = { it.department },
            )
        }
    }.awaitAll()
}

@JvmName("TmdbAggregateCrew_toCrewCompactListItemModel")
suspend fun List<TmdbAggregateCrew>.toCrewCompactListItemModel(context: Context): List<CrewCompactListItemModel> = coroutineScope {
    groupBy { it.id }.map { (_, crew) ->
        async {
            CrewCompactListItemModel.fromTmdbCrew(
                context = context,
                crew = crew,
                department = { it.department },
            )
        }
    }.awaitAll()
}
