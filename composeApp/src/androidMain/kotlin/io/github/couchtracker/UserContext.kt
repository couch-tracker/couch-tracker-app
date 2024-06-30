package io.github.couchtracker

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.couchtracker.db.app.AppData
import io.github.couchtracker.db.app.User
import io.github.couchtracker.db.app.UserManager
import org.koin.compose.koinInject

val LocalUserManagerContext = staticCompositionLocalOf<UserManager> { error("no default user context") }

@Composable
fun UserContext(content: @Composable () -> Unit) {
    val appDb = koinInject<AppData>()

    val usersState = remember { appDb.userQueries.selectAll() }.asListState()
    val users = usersState.value

    if (users.isNullOrEmpty()) {
        Text("Loading...") // TODO do better
    } else {
        UserContext(users = users, content = content)
    }
}

@Composable
private fun UserContext(users: List<User>, content: @Composable () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val userManager = remember { UserManager(users, coroutineScope) }
    LaunchedEffect(users) {
        userManager.updateListOfUsers(users)
    }

    CompositionLocalProvider(LocalUserManagerContext provides userManager) {
        content()
    }
}
