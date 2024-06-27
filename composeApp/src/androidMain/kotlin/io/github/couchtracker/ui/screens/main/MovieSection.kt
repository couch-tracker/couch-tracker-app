@file:OptIn(ExperimentalFoundationApi::class)

package io.github.couchtracker.ui.screens.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.couchtracker.R

@Composable
fun MoviesSection(innerPadding: PaddingValues) {
    val pagerState = rememberPagerState(initialPage = MovieTab.EXPLORE.ordinal) { MovieTab.entries.size }

    MainSection(
        innerPadding = innerPadding,
        pagerState = pagerState,
        backgroundImage = painterResource(id = R.drawable.aurora_borealis),
        tabText = { page -> Text(text = MovieTab.entries[page].displayName) },
        page = { page ->
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item { Text("page $page") }
                @Suppress("MagicNumber")
                items(100) {
                    Text("item $it")
                }
            }
        },
    )
}

private enum class MovieTab(
    val displayName: String, // TODO translate
) {
    TIMELINE("Timeline"),
    EXPLORE("Explore"),
    FOLLOWED("Followed"),
    UP_NEXT("Up next"),
    CALENDAR("Calendar"),
}
