package io.github.couchtracker.ui.screens.watchedItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.ibm.icu.text.DisplayContext
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.FullProfileData
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionSelection
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionWrapper
import io.github.couchtracker.db.profile.toLossyBcp47Language
import io.github.couchtracker.ui.components.LanguagePickerDialog
import io.github.couchtracker.utils.currentFirstLocale
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.toULocale
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

private val USED_LANGUAGES_RECENCY_CUTOFF = 180.days
private const val SUGGESTED_LANGAUGES_TOP_USED_LIMIT = 3
private const val SUGGESTED_LANGAUGES_CHIPS_LIMT = 3

@Composable
fun WatchedItemSheetScope.LanguageSection(
    enabled: Boolean,
    mediaLanguages: List<Bcp47Language>,
    selection: WatchedItemDimensionSelection.Language,
    onSelectionChange: (WatchedItemDimensionSelection.Language) -> Unit,
    modifier: Modifier = Modifier,
) {
    val userLocale = LocalConfiguration.currentFirstLocale.toULocale()
    val data = LocalFullProfileDataContext.current
    val now = remember { Clock.System.now() }
    val suggestedLanguages = mediaLanguages
        .plus(userLocale.toLossyBcp47Language())
        .plus(data.topUsedLanguages(now, selection.dimension).take(SUGGESTED_LANGAUGES_TOP_USED_LIMIT))
        .distinct()

    // We want the list of languages in the chip list to be mostly static, except if we select a new language
    val initialSelection = remember { selection.value }
    val chipLanguages = remember(selection.value) {
        suggestedLanguages
            .take(SUGGESTED_LANGAUGES_CHIPS_LIMT)
            .plus(initialSelection)
            .plus(selection.value)
            .distinct()
            .filterNotNull()
    }

    var showDialog by remember { mutableStateOf(false) }

    Section(title = selection.dimension.name.text, validity = selection.validity(), modifier = modifier) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(chipLanguages, key = { it.toString() }) { language ->
                FilterChip(
                    enabled = enabled,
                    selected = language == selection.value,
                    onClick = { onSelectionChange(selection.toggled(language)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    label = { Text(language.getDisplayName(userLocale, DisplayContext.CAPITALIZATION_FOR_STANDALONE)) },
                )
            }
            item {
                FilterChip(
                    enabled = enabled,
                    selected = false,
                    onClick = { showDialog = true },
                    label = { Text(R.string.watched_item_language_other.str()) },
                )
            }
        }
        if (showDialog) {
            LanguagePickerDialog(
                language = selection.value,
                onLanguageSelected = {
                    onSelectionChange(selection.copy(value = it))
                    showDialog = false
                },
                suggestedLanguages = suggestedLanguages,
                onClose = { showDialog = false },
            )
        }
    }
}

/**
 * Returns list of all previously used languages for the given [dimension] that were added in the last [USED_LANGUAGES_RECENCY_CUTOFF].
 */
private fun FullProfileData.topUsedLanguages(
    now: Instant,
    dimension: WatchedItemDimensionWrapper.Language,
): List<Bcp47Language> {
    return this.watchedItems
        // Filter items that have been added recently
        .filter { now - it.addedAt < USED_LANGUAGES_RECENCY_CUTOFF }
        // Sort by descending added date so that count parity, most recently used languages win
        .sortedByDescending { it.addedAt }
        // Get selected language for the same dimension, excluding null selections
        .mapNotNull { watchedItem ->
            watchedItem.dimensions
                .find { it.dimension == dimension }
                ?.let { it as WatchedItemDimensionSelection.Language }
                ?.value
        }
        // Count each occurrence
        .groupingBy { it }
        .eachCount()
        .entries
        // Sort by most used
        .sortedByDescending { (_, count) -> count }
        .map { it.key }
}
