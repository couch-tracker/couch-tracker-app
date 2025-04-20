package io.github.couchtracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@Composable
fun <T : Any> Query<T>.asListState(coroutineContext: CoroutineContext = Dispatchers.IO): State<List<T>?> {
    return remember {
        this.asFlow().mapToList(coroutineContext)
    }.collectAsStateWithLifecycle(null)
}
