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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.couchtracker.LocalProfileManagerContext
import io.github.couchtracker.db.profile.ProfileDbResult
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.utils.Loadable
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
@Suppress("LongMethod") // TODO: remove this debug pane
fun ProfilePane() {
    val profileManager = LocalProfileManagerContext.current

    val coroutineScope = rememberCoroutineScope()

    val profileInfo = profileManager.current
    val profile = profileInfo.profile
    val profileData by profileInfo.fullProfileDataState.collectAsStateWithLifecycle(initialValue = Loadable.Loading)

    var lastActionStatus by remember { mutableStateOf<ProfileDbResult<Unit>?>(null) }

    Column {
        Text("ID: ${profile.id}")
        Text("External file URI: ${profile.externalFileUri}")
        Text("Cached DB last modified: ${profile.cachedDbLastModified}")

        Text("Last action status: $lastActionStatus")

        val showsInCollectionText = "Show IDs in collection: " + when (val data = profileData) {
            is Loadable.Loading -> "Loading..."
            is Loadable.Error -> "Error: ${data.error}"
            is Loadable.Loaded -> data.value.showCollection.joinToString { it.showId.value }
        }
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
                val data = profileData
                if (data is Loadable.Loaded) {
                    val item = data.value.showCollection.randomOrNull()
                    if (item != null) {
                        coroutineScope.launch {
                            lastActionStatus = profileInfo.write { db ->
                                db.showInCollectionQueries.deleteShow(item.showId)
                            }
                        }
                    }
                }
            },
            content = {
                Text("Remove random show from collection")
            },
        )
    }
}
