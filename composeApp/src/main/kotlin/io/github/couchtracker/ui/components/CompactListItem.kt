package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A basic, more compact, [ListItem]. This component doesn't have any padding on its own.
 */
@Composable
fun CompactListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: @Composable () -> Unit = {},
    leadingContent: @Composable () -> Unit = {},
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        leadingContent()
        Spacer(Modifier.width(16.dp))
        Column {
            // Same style as ListItem's supporting content
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleSmall) {
                headlineContent()
            }
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            ) {
                supportingContent()
            }
        }
    }
}
