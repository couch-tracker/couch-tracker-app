package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.couchtracker.utils.Loadable

object TagsRowComposableDefaults {
    val TAG_STYLE = @Composable {
        MaterialTheme.typography.labelLarge
    }
}

@Composable
fun TagsRow(
    tags: List<String>,
    modifier: Modifier = Modifier,
    tagStyle: TextStyle = TagsRowComposableDefaults.TAG_STYLE(),
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        for (tag in tags) {
            Text(tag, style = tagStyle)
        }
    }
}

@Composable
fun LoadableTagsRow(
    tags: Loadable<List<String>>,
    placeholderLines: Int,
    modifier: Modifier = Modifier,
) {
    LoadableContainer(
        tags,
        modifier = modifier,
        content = { TagsRow(tags = it) },
        onLoading = {
            Column {
                repeat(placeholderLines) {
                    LoadableText(Loadable.Loading, placeholderLines = 1, style = TagsRowComposableDefaults.TAG_STYLE())
                }
            }
        },
    )
}
