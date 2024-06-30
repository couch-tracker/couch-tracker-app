package io.github.couchtracker.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.couchtracker.LocalUserManagerContext
import io.github.couchtracker.db.app.FullUserDataState
import io.github.couchtracker.db.user.ExternalUserDb
import io.github.couchtracker.db.user.ManagedUserDb
import io.github.couchtracker.db.user.UserDbResult
import io.github.couchtracker.db.user.UserDbUtils
import io.github.couchtracker.tmdb.TmdbShowId
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
@Suppress("LongMethod") // TODO: remove this debug pane
fun UserPane() {
    val context = LocalContext.current
    val userManager = LocalUserManagerContext.current

    val coroutineScope = rememberCoroutineScope()

    val userInfo = userManager.current
    val user = userInfo.user
    val userData by userInfo.fullUserDataState.collectAsStateWithLifecycle(initialValue = FullUserDataState.Loading)

    var lastActionStatus by remember { mutableStateOf<UserDbResult<Unit>?>(null) }
    var moveStatus by remember { mutableStateOf<UserDbResult<Unit>?>(null) }

    val takeOwnershipWorkflow = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(UserDbUtils.MIME_TYPE)) { uri ->
        if (uri != null && userInfo.db is ManagedUserDb) {
            coroutineScope.launch {
                moveStatus = userInfo.db.moveToExternalDb(context, uri)
            }
        }
    }

    Column {
        Text("ID: ${user.id}")
        Text("Name: ${user.name}")
        Text("External file URI: ${user.externalFileUri}")
        Text("Cached DB last modified: ${user.cachedDbLastModified}")

        Text("Last action status: $lastActionStatus")
        Text("Last move status: $moveStatus")

        val showsInCollectionText = "Show IDs in collection: " + when (val data = userData) {
            is FullUserDataState.NotLoaded -> "Loading..."
            is FullUserDataState.Error -> "Error: ${data.error}"
            is FullUserDataState.Loaded -> data.data.showCollection.joinToString { it.showId.value }
        }
        Text(showsInCollectionText, maxLines = 3)

        when (userInfo.db) {
            is ManagedUserDb -> {
                Button(onClick = { takeOwnershipWorkflow.launch("couch-tracker.db") }) {
                    Text("Take ownership of DB")
                }
            }

            is ExternalUserDb -> {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            moveStatus = userInfo.db.moveToManagedDb(context)
                        }
                    },
                ) {
                    Text("Give ownership of DB to app")
                }
            }
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    lastActionStatus = userInfo.write { db ->
                        @Suppress("MagicNumber")
                        db.showCollectionQueries.insertShow(TmdbShowId(Random.nextInt(0, 999_999)).toExternalId())
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
                                db.showCollectionQueries.deleteShow(item.showId)
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
