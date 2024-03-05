package io.github.couchtracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

@Composable
fun <T : Any> Query<T>.asListState(coroutineContext: CoroutineContext = Dispatchers.IO): State<List<T>?> {
    return this.asFlow().mapToList(coroutineContext).collectAsStateWithLifecycle(null)
}

inline fun <reified T : Any> jsonAdapter() = object : ColumnAdapter<T, String> {
    override fun decode(databaseValue: String) = Json.decodeFromString<T>(databaseValue)
    override fun encode(value: T) = Json.encodeToString(value)
}
