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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import io.github.couchtracker.R
import io.github.couchtracker.ui.backgroundColor

@Composable
fun ShowSection(innerPadding: PaddingValues) {
    val pagerState = rememberPagerState(initialPage = ShowTab.UP_NEXT.ordinal) { ShowTab.entries.size }

    MainSection(
        innerPadding = innerPadding,
        pagerState = pagerState,
        backgroundColor = Color.Red.backgroundColor(),
        backgroundImage = painterResource(id = R.drawable.sunset),
        tabText = { page -> Text(text = ShowTab.entries[page].displayName) },
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

private enum class ShowTab(
    val displayName: String, // TODO translate
) {
    TIMELINE("Timeline"),
    FOLLOWED("Followed"),
    UP_NEXT("Up next"),
    CALENDAR("Calendar"),
    EXPLORE("Explore"),
}
