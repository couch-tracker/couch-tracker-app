package io.github.couchtracker.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.couchtracker.R
import io.github.couchtracker.ui.components.WipMessageComposable
import io.github.couchtracker.utils.str

@Composable
fun ProfileSection(innerPadding: PaddingValues) {
    val pagerState = rememberPagerState(initialPage = ProfileTab.LISTS.ordinal) { ProfileTab.entries.size }

    MainSection(
        innerPadding = innerPadding,
        pagerState = pagerState,
        imageModel = R.drawable.sunset,
        title = R.string.main_section_profile.str(),
        actions = {
            MainSectionDefaults.DefaultAppBarActions()
        },
        tabText = { page -> Text(text = ProfileTab.entries[page].displayName.str()) },
        page = { page ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (ProfileTab.entries[page]) {
                    ProfileTab.HISTORY -> WipMessageComposable(
                        gitHubIssueId = 126,
                        description = "All watched items",
                    )
                    ProfileTab.LISTS -> {
                        WipMessageComposable(
                            gitHubIssueId = 182,
                            description = "The list of lists",
                        )
                    }
                    ProfileTab.STATS -> WipMessageComposable(
                        gitHubIssueId = 180,
                        description = "Statistics about your profile",
                    )
                }
            }
        },
    )
}

private enum class ProfileTab(@StringRes val displayName: Int) {
    HISTORY(R.string.tab_profile_history),
    LISTS(R.string.tab_profile_lists),
    STATS(R.string.tab_profile_stats),
}

