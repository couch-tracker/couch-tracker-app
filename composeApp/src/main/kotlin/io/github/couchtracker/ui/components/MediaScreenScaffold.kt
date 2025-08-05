package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import coil3.request.ImageRequest
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetScaffold
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetScaffoldState
import kotlin.time.Duration

/**
 * Scaffold for a screen showing of a media. Includes [WatchedItemSheetScaffold].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaScreenScaffold(
    watchedItemSheetScaffoldState: WatchedItemSheetScaffoldState,
    colorScheme: ColorScheme,
    watchedItemType: WatchedItemType,
    mediaRuntime: Duration?,
    mediaLanguages: List<Bcp47Language>,
    title: String,
    backdrop: ImageRequest?,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    MaterialTheme(colorScheme = colorScheme) {
        WatchedItemSheetScaffold(
            scaffoldState = watchedItemSheetScaffoldState,
            watchedItemType = watchedItemType,
            mediaRuntime = mediaRuntime,
            mediaLanguages = mediaLanguages,
        ) {
            Scaffold(
                modifier = modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                containerColor = Color.Transparent,
                topBar = {
                    OverviewScreenComponents.Header(title, backdrop, topAppBarScrollBehavior)
                },
                floatingActionButton = floatingActionButton,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                content = content,
            )
        }
    }
}
