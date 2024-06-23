@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times

@Composable
fun BackgroundTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    backgroundColor: Color,
    image: @Composable (Modifier) -> Unit,
    appBar: @Composable (Modifier, TopAppBarColors) -> Unit,
) {
    Box {
        image(
            Modifier.matchParentSize().blur(scrollBehavior.state.collapsedFraction * 8.dp),
        )
        appBar(
            Modifier
                .background(
                    Brush.verticalGradient(
                        0f to backgroundColor.copy(alpha = 0.5f),
                        1f to backgroundColor,
                    ),
                ),
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
        )
    }
}
