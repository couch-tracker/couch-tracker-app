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
import io.github.couchtracker.db.app.AppData
import io.github.couchtracker.db.app.User
import io.github.couchtracker.db.user.ExternalUserDb
import io.github.couchtracker.db.user.ManagedUserDb
import io.github.couchtracker.db.user.UserDbResult
import io.github.couchtracker.db.user.UserDbUtils
import io.github.couchtracker.db.user.db
import io.github.couchtracker.db.user.debugSuccessOr
import io.github.couchtracker.db.user.show.ExternalShowId
import io.github.couchtracker.tmdb.TmdbShowId
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
@Suppress("LongMethod") // TODO: remove this debug pane
fun UserPane(appData: AppData, user: User) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showCollection by remember { mutableStateOf<List<ExternalShowId>>(emptyList()) }
    var lastActionStatus by remember { mutableStateOf<UserDbResult<Unit>?>(null) }
    var moveStatus by remember { mutableStateOf<UserDbResult<Unit>?>(null) }

    val userDb = remember(user, context, appData) { user.db(context, appData) }

    val takeOwnershipWorkflow = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(UserDbUtils.MIME_TYPE)) { uri ->
        if (uri != null && userDb is ManagedUserDb) {
            coroutineScope.launch {
                moveStatus = userDb.moveToExternalDb(context, uri)
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

        for (show in showCollection) {
            Text("Show with ID: $show")
        }

        when (userDb) {
            is ManagedUserDb -> {
                Button(onClick = { takeOwnershipWorkflow.launch("couch-tracker.db") }) {
                    Text("Take ownership of DB")
                }
            }
            is ExternalUserDb -> {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            moveStatus = userDb.moveToManagedDb(context)
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
                    showCollection = userDb.read(context) { db ->
                        db.showCollectionQueries.selectShowCollection().executeAsList()
                    }.debugSuccessOr { emptyList() }
                }
            },
            content = {
                Text("List shows in collection")
            },
        )
        Button(
            onClick = {
                coroutineScope.launch {
                    lastActionStatus = userDb.write(context) { db ->
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
                val id = showCollection.randomOrNull()
                if (id != null) {
                    coroutineScope.launch {
                        lastActionStatus = userDb.write(context) { db ->
                            db.showCollectionQueries.deleteShow(id)
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
