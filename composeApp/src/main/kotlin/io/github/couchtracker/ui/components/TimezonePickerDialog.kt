package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import io.github.couchtracker.R
import io.github.couchtracker.utils.TimezoneItem
import io.github.couchtracker.utils.currentFirstLocale
import io.github.couchtracker.utils.pluralStr
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.timezonesTree
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimezonePickerDialog(
    timezone: TimeZone?,
    onTimezoneSelected: (TimeZone?) -> Unit,
    onClose: () -> Unit,
) {
    val timezonesTreeRoot = remember { TimeZone.timezonesTree().toTree() as TreeNode.Root }
    TreeItemPickerDialog(
        selected = timezone,
        onSelect = onTimezoneSelected,
        root = timezonesTreeRoot,
        itemKey = { it.id },
        icon = { Icon(Icons.Default.Public, contentDescription = null) },
        title = { Text(R.string.select_time_zone.str()) },
        onClose = onClose,
    )
}


private fun TimezoneItem.toTree(): TreeNode<TimeZone> {
    return when (this) {
        is TimezoneItem.Category.Root -> TreeNode.Root(children = items.map { it.toTree() as TreeNode.NonRoot })
        is TimezoneItem.Category.GeographicalArea -> TreeNode.Category(
            children = items.map { it.toTree() as TreeNode.NonRoot },
            name = { name },
            supportingName = { supportingName() },
        )

        is TimezoneItem.Category.Uncategorized -> TreeNode.Category(
            children = items.map { it.toTree() as TreeNode.NonRoot },
            name = { R.string.timezone_category_other.str() },
            supportingName = { supportingName() },
        )

        is TimezoneItem.Zone -> TreeNode.Item(
            value = timezone,
            name = { timezone.id },
            supportingName = {
                val androidTz = android.icu.util.TimeZone.getFrozenTimeZone(timezone.id)
                androidTz.getDisplayName(LocalConfiguration.currentFirstLocale)
            },
            headerName = { leafName },
            searchName = { timezone.id },
        )
    }
}

@Composable
private fun TimezoneItem.Category.supportingName(): String {
    val cnt = totalItemCount()
    return R.plurals.timezone_category_subtitle.pluralStr(cnt, cnt)
}
