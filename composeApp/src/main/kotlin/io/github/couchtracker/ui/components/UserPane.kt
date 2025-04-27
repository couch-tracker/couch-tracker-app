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
import io.github.couchtracker.LocalUserManagerContext
import io.github.couchtracker.db.app.FullUserDataState
import io.github.couchtracker.db.user.UserDbResult
import io.github.couchtracker.tmdb.TmdbShowId
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
@Suppress("LongMethod") // TODO: remove this debug pane
fun UserPane() {
    val userManager = LocalUserManagerContext.current

    val coroutineScope = rememberCoroutineScope()

    val userInfo = userManager.current
    val user = userInfo.user
    val userData by userInfo.fullUserDataState.collectAsStateWithLifecycle(initialValue = FullUserDataState.Loading)

    var lastActionStatus by remember { mutableStateOf<UserDbResult<Unit>?>(null) }

    Column {
        Text("ID: ${user.id}")
        Text("External file URI: ${user.externalFileUri}")
        Text("Cached DB last modified: ${user.cachedDbLastModified}")

        Text("Last action status: $lastActionStatus")

        val showsInCollectionText = "Show IDs in collection: " + when (val data = userData) {
            is FullUserDataState.NotLoaded -> "Loading..."
            is FullUserDataState.Error -> "Error: ${data.error}"
            is FullUserDataState.Loaded -> data.data.showCollection.joinToString { it.showId.value }
        }
        Text(showsInCollectionText, maxLines = 3)

        Button(
            onClick = {
                coroutineScope.launch {
                    lastActionStatus = userInfo.write { db ->
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
                val data = userData
                if (data is FullUserDataState.Loaded) {
                    val item = data.data.showCollection.randomOrNull()
                    if (item != null) {
                        coroutineScope.launch {
                            lastActionStatus = userInfo.write { db ->
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
