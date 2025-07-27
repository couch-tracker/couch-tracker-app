package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.LocalProfilesContext
import io.github.couchtracker.db.profile.ProfileDbResult
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.ui.screens.main.TMDB_LANGUAGE
import io.github.couchtracker.ui.screens.movie.navigateToMovie
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
@Suppress("LongMethod") // TODO: remove this debug pane
fun ProfilePane() {
    val profilesInfo = LocalProfilesContext.current
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()

    val profileInfo = profilesInfo.current
    val profile = profileInfo.profile
    val profileData = LocalFullProfileDataContext.current

    var lastActionStatus by remember { mutableStateOf<ProfileDbResult<Unit>?>(null) }

    Column {
        Text("ID: ${profile.id}")
        Text("External file URI: ${profile.externalFileUri}")
        Text("Cached DB last modified: ${profile.cachedDbLastModified}")

        Text("Last action status: $lastActionStatus")

        val showsInCollectionText = "Show IDs in collection: " + profileData.showCollection.joinToString { it.showId.value }
        Text(showsInCollectionText, maxLines = 3)

        Button(
            onClick = {
                coroutineScope.launch {
                    lastActionStatus = profileInfo.write { db ->
                        @Suppress("MagicNumber")
                        db.showInCollectionQueries.insertShow(TmdbShowId(Random.nextInt(0, 999_999)).toExternalId())
                    }
                }
            },
            content = {
                Text("Add random show to collection")
            },
        )
        Button(
            onClick = {
                val item = profileData.showCollection.randomOrNull()
                if (item != null) {
                    coroutineScope.launch {
                        lastActionStatus = profileInfo.write { db ->
                            db.showInCollectionQueries.deleteShow(item.showId)
                        }
                    }
                }
            },
            content = {
                Text("Remove random show from collection")
            },
        )
        Button(onClick = { navController.navigateToMovie(TmdbMovie(TmdbMovieId(76122), TMDB_LANGUAGE)) }) {
            Text("Open short movie")
        }
        Button(onClick = { navController.navigateToMovie(TmdbMovie(TmdbMovieId(393209), TMDB_LANGUAGE)) }) {
            Text("Open movie with no duration")
        }
        Button(onClick = { navController.navigateToMovie(TmdbMovie(TmdbMovieId(19995), TMDB_LANGUAGE)) }) {
            Text("Open normal movie")
        }
    }
}
