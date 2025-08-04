@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.ui.SizeAwareLazyListScope
import io.github.couchtracker.ui.composable
import io.github.couchtracker.ui.countingElements
import io.github.couchtracker.utils.str
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.FooterPreference
import me.zhanghai.compose.preference.LocalPreferenceTheme
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.preferenceTheme

@Serializable
data object Settings

fun NavGraphBuilder.settings() {
    navigation<Settings>(startDestination = MainSettingsScreen) {
        composable<MainSettingsScreen>()
        composable<ProfilesSettingsScreen>()
        composable<ProfileSettingsScreen>()
        composable<TmdbLanguagesSettingsScreen>()
    }
}

@Composable
fun BaseSettings(
    title: String,
    header: String?,
    footer: String?,
    modifier: Modifier = Modifier,
    lazyColumnModifier: Modifier = Modifier,
    lazyColumnState: LazyListState = rememberLazyListState(),
    content: SizeAwareLazyListScope.() -> Unit,
) {
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = title) },
                modifier = Modifier.fillMaxWidth(),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton({ navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = R.string.back_action.str(),
                        )
                    }
                },
            )
        },
        containerColor = Color.Transparent,
        contentColor = contentColorFor(MaterialTheme.colorScheme.background),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
    ) { contentPadding ->
        CompositionLocalProvider(LocalPreferenceTheme provides preferenceTheme()) {
            LazyColumn(modifier = lazyColumnModifier.fillMaxSize(), contentPadding = contentPadding, state = lazyColumnState) {
                countingElements {
                    if (header != null) {
                        item(key = "settings-description") {
                            Preference(
                                modifier = Modifier.animateItem(),
                                title = {},
                                summary = { Text(header) },
                            )
                        }
                    }
                    content()
                    if (footer != null) {
                        item("footer") {
                            FooterPreference(
                                modifier = Modifier.animateItem(),
                                summary = { Text(footer) },
                            )
                        }
                    }
                }
            }
        }
    }
}
