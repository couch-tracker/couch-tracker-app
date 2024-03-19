package io.github.couchtracker.componens

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import io.github.couchtracker.db.app.User

@Composable
fun UserPane(user: User) {
    Column {
        Text("ID: ${user.id}")
        Text("Name: ${user.name}")
        Text("External file URI: ${user.externalFileUri}")
    }
}

