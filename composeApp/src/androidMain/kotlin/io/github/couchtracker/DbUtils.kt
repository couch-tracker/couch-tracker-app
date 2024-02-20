package io.github.couchtracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers

@Composable
fun <T : Any> Query<T>.asListState(): State<List<T>?> {
    return this.asFlow().mapToList(Dispatchers.IO).collectAsStateWithLifecycle(null)
}
