package io.github.couchtracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import io.github.couchtracker.db.app.AppDb
import io.github.couchtracker.db.app.User

@Composable
fun App() {
    MaterialTheme {
        Column(Modifier.fillMaxSize()) {
            val appDb = AppDb.get(LocalContext.current)
            val users by appDb.userQueries.selectAll().asListState()
            var currentUser by remember { mutableStateOf<User?>(null) }

            Text(text = "Current user", fontSize = 30.sp)
            when (val u = currentUser) {
                null -> Text(text = "No current user", fontStyle = FontStyle.Italic)
                else -> Column {
                    Text("ID: ${u.id}")
                    Text("Name: ${u.name}")
                    Text("External file URI: ${u.externalFileUri}")
                }
            }

            Text(text = "User list", fontSize = 30.sp)
            when (val userList = users) {
                null -> Text("Loading...")
                else -> for (user in userList) {
                    Button(onClick = { currentUser = user }) {
                        Text(text = user.name)
                    }
                }
            }
            Button(
                onClick = {
                    appDb.userQueries.insert(
                        name = "Create time: ${System.currentTimeMillis()}",
                        externalFileUri = null,
                    )
                },
            ) {
                Text(text = "Add user")
            }
            Button(
                onClick = {
                    appDb.userQueries.deleteAll()
                },
            ) {
                Text(text = "Remove all")
            }
        }
    }
}
