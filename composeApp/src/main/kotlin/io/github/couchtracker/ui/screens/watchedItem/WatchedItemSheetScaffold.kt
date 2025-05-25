@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.watchedItem

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.WatchedItemDimensionChoice
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionWrapper
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.ui.components.BoxWithScrim
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration

object WatchedItemSheetScope

class WatchedItemDialogScaffoldState(
    val scaffoldState: BottomSheetScaffoldState,
) {
    /** This boolean is to avoid creating the bottom sheet when not necessary */
    var createBottomSheet by mutableStateOf(false)

    fun open(coroutineScope: CoroutineScope) {
        createBottomSheet = true
        coroutineScope.launch {
            scaffoldState.bottomSheetState.show()
        }
    }
}

@Composable
fun rememberWatchedItemDialogScaffoldState(): WatchedItemDialogScaffoldState {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false,
        ),
    )
    return remember {
        WatchedItemDialogScaffoldState(
            scaffoldState = scaffoldState,
        )
    }
}

@Composable
fun WatchedItemSheetScaffold(
    scaffoldState: WatchedItemDialogScaffoldState,
    watchedItemType: WatchedItemType,
    approximateVideoRuntime: Duration,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetState = scaffoldState.scaffoldState.bottomSheetState
    var headerHeight by remember { mutableIntStateOf(0) }
    var scrollLabelHeight by remember { mutableIntStateOf(0) }
    var footerHeight by remember { mutableIntStateOf(0) }
    var dragHandlerHeight by remember { mutableIntStateOf(0) }
    val sheetPeekHeight = with(LocalDensity.current) {
        (headerHeight + dragHandlerHeight + scrollLabelHeight + footerHeight).toDp()
    }
    val innerBottomPadding = with(LocalDensity.current) {
        WindowInsets.systemBars.getBottom(LocalDensity.current).toDp()
    }
    BackHandler(enabled = bottomSheetState.currentValue != SheetValue.Hidden) {
        coroutineScope.launch {
            bottomSheetState.hide()
        }
    }

    BottomSheetScaffold(
        containerColor = MaterialTheme.colorScheme.background,
        scaffoldState = scaffoldState.scaffoldState,
        modifier = Modifier.fillMaxSize(),
        sheetPeekHeight = sheetPeekHeight,
        sheetDragHandle = {
            Box(
                Modifier.onPlaced { coordinates ->
                    dragHandlerHeight = coordinates.size.height
                },
            ) {
                BottomSheetDefaults.DragHandle()
            }
        },
        sheetContent = {
            if (scaffoldState.createBottomSheet) {
                WatchedItemSheetContent(
                    bottomSheetState = bottomSheetState,
                    watchedItemType,
                    approximateVideoRuntime,
                    onDismissRequest = {
                        coroutineScope.launch {
                            bottomSheetState.hide()
                        }
                    },
                    onHeaderHeightChange = { headerHeight = it },
                    onScrollLabelHeightChange = { scrollLabelHeight = it },
                    onFooterHeightChange = { footerHeight = it },
                    innerBottomPadding = innerBottomPadding,
                )
            }
        },
        content = {
            BoxWithScrim(
                visible = bottomSheetState.targetValue != SheetValue.Hidden,
                color = BottomSheetDefaults.ScrimColor,
                onDismissRequest = {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                    }
                },
            ) {
                content()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchedItemSheetContent(
    bottomSheetState: SheetState,
    watchedItemType: WatchedItemType,
    approximateVideoRuntime: Duration,
    onDismissRequest: () -> Unit,
    onHeaderHeightChange: (Int) -> Unit,
    onScrollLabelHeightChange: (Int) -> Unit,
    onFooterHeightChange: (Int) -> Unit,
    innerBottomPadding: Dp,
) {
    val dateTimeSectionState = rememberDateTimeSectionState()
    val profileData = LocalFullProfileDataContext.current
    val showAdvancedOptions = bottomSheetState.targetValue == SheetValue.Expanded
    val lazyListState = rememberLazyListState()
    var titleHeight by remember { mutableIntStateOf(0) }
    var headerHeight by remember { mutableIntStateOf(0) }
    var scrollLabelHeight by remember { mutableIntStateOf(0) }

    DateTimeSectionDialog(dateTimeSectionState)

    LaunchedEffect(showAdvancedOptions) {
        if (!showAdvancedOptions) {
            lazyListState.animateScrollToItem(0)
        }
    }
    BoxWithConstraints {
        val c = this.constraints
        LazyColumn(state = lazyListState) {
            WatchedItemSheetScope.apply {
                item {
                    Column(
                        Modifier.onPlaced {
                            headerHeight = it.size.height
                            onHeaderHeightChange(it.size.height)
                        },
                    ) {
                        Text(
                            when (watchedItemType) {
                                WatchedItemType.MOVIE -> R.string.mark_movie_as_watched.str()
                                WatchedItemType.EPISODE -> R.string.mark_episode_as_watched.str()
                            },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .onPlaced { titleHeight = it.size.height },
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(16.dp))
                        DateTimeSection(dateTimeSectionState, watchedItemType, approximateVideoRuntime)
                        for (dimension in profileData.watchedItemDimensions.take(1)) {
                            when (dimension) {
                                is WatchedItemDimensionWrapper.Choice -> ChoiceSection(dimension)
                                is WatchedItemDimensionWrapper.FreeText -> FreeTextSection(dimension)
                            }
                        }
                    }
                }
                items(profileData.watchedItemDimensions.drop(1), key = { it.id }) { dimension ->
                    BelowScrollLabelContainer(showAdvancedOptions, scrollLabelHeight) {
                        when (dimension) {
                            is WatchedItemDimensionWrapper.Choice -> ChoiceSection(dimension)
                            is WatchedItemDimensionWrapper.FreeText -> FreeTextSection(dimension)
                        }
                    }
                }
                // This is just to have an element at the bottom that will take the same size as the other Button
                item {
                    BelowScrollLabelContainer(showAdvancedOptions, scrollLabelHeight) {
                        SaveButton(innerBottomPadding, onDismissRequest = { })
                    }
                }
            }
        }
        ScrollLabel(
            showAdvancedOptions,
            Modifier
                .offset {
                    IntOffset(0, lazyListState.firstVisibleItemScrollOffset + headerHeight)
                }
                .onPlaced {
                    onScrollLabelHeightChange(it.size.height)
                    scrollLabelHeight = it.size.height
                },
        )
        SaveButton(
            innerBottomPadding,
            Modifier
                .align(Alignment.TopCenter)
                .onPlaced {
                    onFooterHeightChange(it.size.height)
                }
                .graphicsLayer {
                    val naturalTranslation = -bottomSheetState.requireOffset() + c.maxHeight - this.size.height
                    translationY = naturalTranslation.coerceAtLeast(titleHeight.toFloat())
                },
        ) { onDismissRequest() }
    }
}

@Composable
private fun ScrollLabel(showAdvancedOptions: Boolean, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(
        targetValue = if (!showAdvancedOptions) 1f else 0f,
        animationSpec = tween(),
    )
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .alpha(alpha)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        Text(R.string.watched_item_scroll_for_more_options.str(), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun BelowScrollLabelContainer(showAdvancedOptions: Boolean, scrollLabelHeight: Int, content: @Composable () -> Unit) {
    val translationY by animateFloatAsState(
        targetValue = if (showAdvancedOptions) 0f else scrollLabelHeight.toFloat(),
        animationSpec = tween(),
    )
    Box(
        Modifier.graphicsLayer {
            this.translationY = translationY
        },
    ) {
        content()
    }
}

@Composable
private fun SaveButton(innerBottomPadding: Dp, modifier: Modifier = Modifier, onDismissRequest: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = innerBottomPadding)
            // Intercepting all input events, since this will be on top of other content
            .pointerInput(Unit) { },
        horizontalArrangement = Arrangement.End,
    ) {
        Button(onClick = { onDismissRequest() }) {
            Text(R.string.create_watched_item.str())
        }
    }
}

@Composable
fun WatchedItemSheetScope.Section(title: Text, content: @Composable () -> Unit) {
    Column {
        Text(
            title.string(),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        content()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WatchedItemSheetScope.ChoiceSection(dimension: WatchedItemDimensionWrapper.Choice) {
    Section(dimension.name.text) {
        var selected by remember { mutableStateOf<WatchedItemDimensionChoice?>(null) }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(dimension.choices, key = { it.id }) { choice ->
                FilterChip(
                    selected = choice == selected,
                    onClick = {
                        selected = if (choice == selected) {
                            null
                        } else {
                            choice
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    leadingIcon = {
                        if (choice.icon != null) {
                            Icon(choice.icon.icon.painter(), contentDescription = null, modifier = Modifier.height(16.dp))
                        }
                    },
                    label = { Text(choice.name.text.string()) },
                )
            }
        }
    }
}

@Composable
private fun WatchedItemSheetScope.FreeTextSection(dimension: WatchedItemDimensionWrapper.FreeText) {
    Section(dimension.name.text) {
        var text by remember { mutableStateOf("") }
        OutlinedTextField(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            value = text,
            onValueChange = { text = it },
            minLines = 2,
        )
    }
}
