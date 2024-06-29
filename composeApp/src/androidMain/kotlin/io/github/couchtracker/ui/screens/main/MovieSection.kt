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
import couch_tracker_app.composeapp.generated.resources.Res
import couch_tracker_app.composeapp.generated.resources.aurora_borealis
import couch_tracker_app.composeapp.generated.resources.tab_movie_calendar
import couch_tracker_app.composeapp.generated.resources.tab_movie_explore
import couch_tracker_app.composeapp.generated.resources.tab_movie_followed
import couch_tracker_app.composeapp.generated.resources.tab_movie_timeline
import couch_tracker_app.composeapp.generated.resources.tab_movie_up_next
import io.github.couchtracker.utils.str
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun MoviesSection(innerPadding: PaddingValues) {
    val pagerState = rememberPagerState(initialPage = MovieTab.EXPLORE.ordinal) { MovieTab.entries.size }

    MainSection(
        innerPadding = innerPadding,
        pagerState = pagerState,
        backgroundImage = painterResource(Res.drawable.aurora_borealis),
        tabText = { page -> Text(text = MovieTab.entries[page].displayName.str()) },
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
    val displayName: StringResource,
) {
    TIMELINE(Res.string.tab_movie_timeline),
    EXPLORE(Res.string.tab_movie_explore),
    FOLLOWED(Res.string.tab_movie_followed),
    UP_NEXT(Res.string.tab_movie_up_next),
    CALENDAR(Res.string.tab_movie_calendar),
}
