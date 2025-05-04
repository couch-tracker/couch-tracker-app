@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import io.github.couchtracker.R
import io.github.couchtracker.ui.components.BackgroundTopAppBar
import io.github.couchtracker.utils.str
import kotlinx.coroutines.launch

@Composable
fun MainSection(
    innerPadding: PaddingValues,
    pagerState: PagerState,
    backgroundImage: Painter,
    actions: @Composable RowScope.() -> Unit = {},
    tabText: @Composable (page: Int) -> Unit,
    page: @Composable (page: Int) -> Unit,
) {
    val cs = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        topBar = {
            BackgroundTopAppBar(
                scrollBehavior = scrollBehavior,
                image = { modifier ->
                    Image(
                        modifier = modifier,
                        painter = backgroundImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                    )
                },
                appBar = { colors ->
                    Column {
                        TopAppBar(
                            colors = colors,
                            scrollBehavior = scrollBehavior,
                            title = {},
                            actions = actions,
                        )
                        PrimaryScrollableTabRow(
                            scrollState = scrollState,
                            selectedTabIndex = pagerState.currentPage,
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            divider = {},
                        ) {
                            for (pageIndex in 0..<pagerState.pageCount) {
                                Tab(
                                    selected = pageIndex == pagerState.currentPage,
                                    onClick = { cs.launch { pagerState.scrollToPage(pageIndex) } },
                                    text = { tabText(pageIndex) },
                                )
                            }
                        }
                    }
                },
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top),
        content = { scaffoldInnerPadding ->
            HorizontalPager(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = scaffoldInnerPadding,
                state = pagerState,
            ) { pageIndex ->
                Box(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) {
                    page(pageIndex)
                }
            }
        },
    )
}

object MainSectionDefaults {

    @Composable
    fun SearchButton(onOpenSearch: () -> Unit) {
        IconButton(onClick = { onOpenSearch() }) {
            Icon(Icons.Outlined.Search, contentDescription = R.string.search_action.str())
        }
    }
}
