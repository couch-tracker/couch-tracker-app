@file:OptIn(ExperimentalFoundationApi::class)

package io.github.couchtracker.ui.screens.main

import androidx.annotation.StringRes
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
import io.github.couchtracker.utils.str

@Composable
fun ShowSection(innerPadding: PaddingValues) {
    val pagerState = rememberPagerState(initialPage = ShowTab.UP_NEXT.ordinal) { ShowTab.entries.size }

    MainSection(
        innerPadding = innerPadding,
        pagerState = pagerState,
        backgroundImage = painterResource(R.drawable.sunset),
        tabText = { page -> Text(text = ShowTab.entries[page].displayName.str()) },
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
    @StringRes
    val displayName: Int,
) {
    TIMELINE(R.string.tab_show_timeline),
    FOLLOWED(R.string.tab_show_followed),
    UP_NEXT(R.string.tab_show_up_next),
    CALENDAR(R.string.tab_show_calendar),
    EXPLORE(R.string.tab_show_explore),
}
