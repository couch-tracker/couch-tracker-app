package io.github.couchtracker.ui.screens.main.show

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.couchtracker.R
import io.github.couchtracker.settings.StyleAndBehaviorSettings
import io.github.couchtracker.settings.appSettings
import io.github.couchtracker.settings.upNextOptions
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MessageComposable
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.components.UpNextListItem
import io.github.couchtracker.ui.itemsWithPosition
import io.github.couchtracker.ui.screens.main.show.ShowSectionViewModel.UpNextEntry
import io.github.couchtracker.ui.screens.main.show.ShowSectionViewModel.UpNextModel
import io.github.couchtracker.ui.screens.main.show.ShowSectionViewModel.UpNextSection
import io.github.couchtracker.utils.error.CouchTrackerLoadable
import io.github.couchtracker.utils.str
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun UpNextAppBarActions() {
    val cs = rememberCoroutineScope()
    val settings = appSettings()
    val current = settings.upNextOptions()
    var expanded by remember { mutableStateOf(false) }
    IconButton({ expanded = !expanded }) {
        Icon(Icons.AutoMirrored.Default.Sort, contentDescription = R.string.settings.str())
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Default.Assistant, contentDescription = null)
            },
            text = { Text(R.string.up_next_enable_suggested_section.str()) },
            onClick = {
                cs.launch {
                    settings.get { StyleAndBehavior.UpNextEnableSuggestions }.setting.set(!current.enableSmartSuggestions)
                }
            },
            trailingIcon = {
                if (current.enableSmartSuggestions) {
                    Icon(Icons.Default.CheckBox, contentDescription = null)
                } else {
                    Icon(Icons.Default.CheckBoxOutlineBlank, contentDescription = null)
                }
            },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Default.Today, contentDescription = null)
            },
            text = { Text(R.string.up_next_group_by_airing_status.str()) },
            onClick = {
                cs.launch {
                    settings.get { StyleAndBehavior.UpNextDivideAired }.setting.set(!current.divideAired)
                }
            },
            trailingIcon = {
                if (current.divideAired) {
                    Icon(Icons.Default.CheckBox, contentDescription = null)
                } else {
                    Icon(Icons.Default.CheckBoxOutlineBlank, contentDescription = null)
                }
            },
        )
        HorizontalDivider()
        for (sortOrder in StyleAndBehaviorSettings.UpNextSortOrderOption.entries) {
            DropdownMenuItem(
                text = { Text(sortOrder.stringRes.str()) },
                onClick = {
                    cs.launch {
                        settings.get { StyleAndBehavior.UpNextSortOrder }.setting.set(sortOrder)
                    }
                },
                trailingIcon = {
                    if (current.sortOrder == sortOrder) {
                        Icon(Icons.Default.RadioButtonChecked, contentDescription = null)
                    } else {
                        Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null)
                    }
                },
            )
        }
    }
}

@Composable
fun UpNextTab(
    upNextModel: CouchTrackerLoadable<UpNextModel>,
    onRetry: () -> Unit,
) {
    LoadableScreen(
        upNextModel,
        onError = { apiError ->
            DefaultErrorScreen(
                error = apiError,
                retry = onRetry,
            )
        },
    ) { (upNextOptions, sections) ->
        if (sections.isEmpty()) {
            MessageComposable(
                modifier = Modifier.fillMaxSize(),
                icon = Icons.Default.BookmarkBorder,
                message = R.string.tab_shows_up_next_empty.str(),
                details = R.string.tab_shows_up_next_empty_description.str(),
            )
        } else {
            val listState = rememberLazyListState()
            var previousSections = remember { sections }
            var previousUpNextOptions = remember { upNextOptions }
            val scrollOffset = with(LocalDensity.current) { 200.dp.toPx().roundToInt() }
            // This effect will scroll the list to items that change
            LaunchedEffect(sections) {
                if (sections != previousSections) {
                    val changed = findChangedUpNextEntry(previousSections, sections)
                    if (changed != null) {
                        val index = findIndex(sections, changed)
                        val item = listState.layoutInfo.visibleItemsInfo.singleOrNull { it.index == index }
                        if (item == null || item.offset < 0) {
                            // Scroll only if it's not visible
                            listState.animateScrollToItem(index, -scrollOffset)
                        }
                    }
                    previousSections = sections
                }
            }
            // This effect will scroll to the top when up-next options change
            LaunchedEffect(upNextOptions) {
                if (upNextOptions != previousUpNextOptions) {
                    listState.scrollToItem(0)
                }
                previousUpNextOptions = upNextOptions
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp) + PaddingValues(bottom = OverviewScreenComponents.LIST_BOTTOM_SPACE),
                state = listState,
            ) {
                // Note: when changing the items in this lazy column, please also update `findChangedUpNextEntry` function
                for ((section, entries) in sections) {
                    item {
                        // Note: I'm keeping an empty item to help the lazy column retain the scroll position
                        // when the section title appears/disappears
                        if (sections.size > 1 || section != UpNextSection.OTHER) {
                            Text(
                                text = section.stringRes.str(),
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 4.dp),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    itemsWithPosition(
                        items = entries,
                        key = { _, upNextEntry -> upNextEntry.itemKey },
                    ) { position, upNextEntry ->
                        UpNextListItem(upNextEntry.model, position, modifier = Modifier.animateItem())
                        Spacer(Modifier.height(2.dp))
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

/** Finds the first up-next entry that changed */
private fun findChangedUpNextEntry(
    previousSections: Map<UpNextSection, List<UpNextEntry>>,
    sections: Map<UpNextSection, List<UpNextEntry>>,
): UpNextEntry? {
    val previousByKey = previousSections.values.flatten().associateBy { it.itemKey }
    for (entry in sections.values.flatten()) {
        val previous = previousByKey[entry.itemKey]
        // New or changed
        if (previous == null || previous.episodeId != entry.episodeId) {
            return entry
        }
    }
    return null
}

/** The index of this up-nexty entry in the lazy column */
private fun findIndex(
    sections: Map<UpNextSection, List<UpNextEntry>>,
    entry: UpNextEntry,
): Int {
    var index = 0
    for ((_, entries) in sections) {
        // Section title
        index++
        val iof = entries.indexOf(entry)
        if (iof >= 0) {
            return index + iof
        }
        index += entries.size
        // Spacer
        index++
    }
    error("Entry not found")
}
