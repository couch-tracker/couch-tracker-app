package io.github.couchtracker.ui.screens.watchedItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.ibm.icu.text.DisplayContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.model.partialtime.toLocalPartialDateTime
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionSelection
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import io.github.couchtracker.db.profile.model.watchedItem.localizedWatchAt
import io.github.couchtracker.intl.datetime.localizedFull
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.currentFirstLocale
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.toULocale
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchedItemInfoDialog(
    itemTitle: String,
    watchedItem: WatchedItemWrapper,
    onDismissRequest: () -> Unit,
    onEditRequest: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        content = {
            Surface(
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column {
                    Text(
                        modifier = Modifier.padding(20.dp),
                        text = R.string.viewing_of_x.str(itemTitle),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 20.dp).weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        section(Text.Resource(R.string.watch_date)) {
                            Text(watchedItem.localizedWatchAt(includeTimeZone = true))
                        }
                        section(Text.Resource(R.string.add_date)) {
                            Text(watchedItem.addedAt.toLocalPartialDateTime(TimeZone.currentSystemDefault()).localizedFull().string())
                        }

                        for (selection in watchedItem.dimensions) {
                            when (selection) {
                                is WatchedItemDimensionSelection.Choice -> choiceSection(watchedItem, selection)
                                is WatchedItemDimensionSelection.Language -> languageSection(watchedItem, selection)
                                is WatchedItemDimensionSelection.FreeText -> freeTextSection(watchedItem, selection)
                            }
                        }
                    }
                    FlowRow(
                        Modifier.align(Alignment.End).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        TextButton(
                            onClick = {
                                onDeleteRequest()
                                onDismissRequest()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(R.string.delete_action.str())
                        }
                        TextButton(
                            onClick = {
                                onEditRequest()
                                onDismissRequest()
                            },
                        ) {
                            Text(R.string.edit_action.str())
                        }
                        TextButton(onClick = onDismissRequest) {
                            Text(android.R.string.ok.str())
                        }
                    }
                }
            }
        },
    )
}

private fun LazyListScope.section(
    title: Text,
    content: @Composable () -> Unit,
) {
    item {
        Column {
            Text(title.string(), style = MaterialTheme.typography.titleMedium)
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                content = content,
            )
        }
    }
}

private fun LazyListScope.selectionSection(
    watchedItem: WatchedItemWrapper,
    selection: WatchedItemDimensionSelection<*>,
    notEmptyContent: @Composable () -> Unit,
) {
    if (selection.dimension.isVisible(watchedItem.dimensions) || !selection.isEmpty()) {
        section(title = selection.dimension.name.text) {
            if (selection.isEmpty()) {
                Text(R.string.watched_item_no_information_available.str(), fontStyle = FontStyle.Italic)
            } else {
                notEmptyContent()
            }
        }
    }
}

private fun LazyListScope.choiceSection(watchedItem: WatchedItemWrapper, selection: WatchedItemDimensionSelection.Choice) {
    selectionSection(watchedItem, selection) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (choice in selection.value) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (choice.icon != null) {
                        Icon(
                            modifier = Modifier.height(16.dp).padding(end = 4.dp),
                            painter = choice.icon.icon.painter(),
                            contentDescription = null,
                        )
                    }
                    Text(choice.name.text.string())
                }
            }
        }
    }
}

private fun LazyListScope.freeTextSection(watchedItem: WatchedItemWrapper, selection: WatchedItemDimensionSelection.FreeText) {
    selectionSection(watchedItem, selection) {
        Text(selection.value)
    }
}

private fun LazyListScope.languageSection(watchedItem: WatchedItemWrapper, selection: WatchedItemDimensionSelection.Language) {
    selectionSection(watchedItem, selection) {
        val locale = LocalConfiguration.currentFirstLocale.toULocale()
        Text(checkNotNull(selection.value).getDisplayName(locale, DisplayContext.CAPITALIZATION_FOR_STANDALONE))
    }
}
