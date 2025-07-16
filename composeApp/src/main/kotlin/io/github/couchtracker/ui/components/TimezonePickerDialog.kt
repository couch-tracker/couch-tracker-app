package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import io.github.couchtracker.R
import io.github.couchtracker.utils.TimeZoneCategory
import io.github.couchtracker.utils.countLeafs
import io.github.couchtracker.utils.currentFirstLocale
import io.github.couchtracker.utils.findLeafValue
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
    val timezonesTreeRoot = remember { TimeZone.timezonesTree() }
    val selected = timezonesTreeRoot.findLeafValue { it.timezone == timezone }

    TreePickerDialog(
        selected = selected,
        onSelect = { onTimezoneSelected(it?.timezone) },
        root = timezonesTreeRoot,
        itemName = { it.leafName },
        itemFullName = { it.timezone.id },
        itemSupportingName = {
            val androidTz = android.icu.util.TimeZone.getFrozenTimeZone(it.timezone.id)
            androidTz.getDisplayName(LocalConfiguration.currentFirstLocale)
        },
        itemKey = { it.timezone.id },
        categoryName = {
            when (it.value) {
                is TimeZoneCategory.GeographicalArea -> it.value.name
                is TimeZoneCategory.Uncategorized -> R.string.timezone_category_other.str()
            }
        },
        categorySupportingName = {
            val cnt = it.countLeafs()
            R.plurals.timezone_category_subtitle.pluralStr(cnt, cnt)
        },
        icon = { Icon(Icons.Default.Public, contentDescription = null) },
        title = { Text(R.string.select_time_zone.str()) },
        searchPlaceHolder = { R.string.timezone_search_placeholder.str() },
        nullSelectionButtonText = { R.string.no_timezone.str() },
        onClose = onClose,
    )
}
