package io.github.couchtracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import io.github.couchtracker.R
import io.github.couchtracker.utils.TimezoneItem
import io.github.couchtracker.utils.currentFirstLocale
import io.github.couchtracker.utils.pluralStr
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.timezonesTree
import kotlinx.datetime.TimeZone

@Composable
fun TimezonePickerDialog(
    timezone: TimeZone?,
    onTimezoneSelected: (TimeZone?) -> Unit,
    onClose: () -> Unit,
) {
    val timezonesTreeRoot = remember { TimeZone.timezonesTree() }
    val selected = remember(timezonesTreeRoot) { timezonesTreeRoot.allTimeZones().find { it.timezone == timezone } }

    TreeItemPickerDialog<TimezoneItem, TimezoneItem.Category, TimezoneItem.Zone>(
        root = timezonesTreeRoot.items,
        categoryChildren = { it.items },
        name = { it.mainName(false) },
        supportingName = { it.supportingName() },
        searchName = { it.mainName(true) },
        headerName = { it.mainName(false) },
        leafKey = { it.timezone.id },
        icon = { Icon(Icons.Default.Public, contentDescription = null) },
        title = { Text(R.string.select_time_zone.str()) },
        selected = selected,
        onSelect = { onTimezoneSelected(it?.timezone) },
        onClose = onClose,
    )
}

@Composable
private fun TimezoneItem.mainName(fullName: Boolean): String {
    return when (this) {
        is TimezoneItem.Zone -> if (fullName) timezone.id else leafName
        is TimezoneItem.Category.Root -> error("Root doesn't have a name")
        is TimezoneItem.Category.GeographicalArea -> this.name
        is TimezoneItem.Category.Uncategorized -> R.string.timezone_category_other.str()
    }
}

@Composable
private fun TimezoneItem.supportingName(): String {
    return when (this) {
        is TimezoneItem.Category -> {
            val cnt = totalItemCount()
            R.plurals.timezone_category_subtitle.pluralStr(cnt, cnt)
        }

        is TimezoneItem.Zone -> {
            val androidTz = android.icu.util.TimeZone.getFrozenTimeZone(timezone.id)
            androidTz.getDisplayName(LocalConfiguration.currentFirstLocale)
        }
    }
}
