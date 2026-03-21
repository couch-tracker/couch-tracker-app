package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.LocalBackgroundColor

/**
 * A scaffold that applies correct background/content colors when used inside [io.github.couchtracker.ui.Screen]
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BaseCouchTrackerScreenScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    floatingActionButton: @Composable () -> Unit = {},
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = contentColorFor(LocalBackgroundColor.current),
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}

/**
 * A scaffold that applies correct background/content colors when used inside [io.github.couchtracker.ui.Screen].
 * This also applies a default top bar and scroll behavior.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CouchTrackerScreenScaffold(
    title: String,
    backdrop: ImageModel?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    belowAppBar: @Composable ColumnScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    floatingActionButton: @Composable () -> Unit = {},
    scaffoldContainer: @Composable (scaffold: @Composable () -> Unit) -> Unit = { it() },
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    scaffoldContainer {
        BaseCouchTrackerScreenScaffold(
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                OverviewScreenComponents.Header(
                    title = title,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    subtitle = subtitle,
                    belowAppBar = belowAppBar,
                )
            },
            snackbarHostState = snackbarHostState,
            floatingActionButton = floatingActionButton,
            content = content,
        )
    }
}
