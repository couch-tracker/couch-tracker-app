@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.watchedItem

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionSelection
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemSelections
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.db.profile.model.watchedItem.rememberWatchedItemSelections
import io.github.couchtracker.ui.components.BoxWithScrim
import io.github.couchtracker.ui.components.DelayedActionLoadingIndicator
import io.github.couchtracker.ui.components.ProfileDbErrorDialog
import io.github.couchtracker.utils.ProfileDbActionState
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.rememberProfileDbActionState
import io.github.couchtracker.utils.str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration

object WatchedItemSheetScope

class WatchedItemSheetScaffoldState(
    val coroutineScope: CoroutineScope,
    val scaffoldState: BottomSheetScaffoldState,
) {
    var mode: WatchedItemSheetMode? by mutableStateOf(null)
        private set

    // This is needed to differentiate each open request, so that we can create a fresh WatchedItemSheetContent for each of them
    var openCounter by mutableIntStateOf(0)
        private set

    fun open(mode: WatchedItemSheetMode) {
        this.mode = mode
        openCounter++
        coroutineScope.launch {
            scaffoldState.bottomSheetState.show()
        }
    }

    fun close() {
        coroutineScope.launch {
            scaffoldState.bottomSheetState.hide()
        }
    }
}

@Composable
fun rememberWatchedItemSheetScaffoldState(): WatchedItemSheetScaffoldState {
    val coroutineScope = rememberCoroutineScope()

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false,
        ),
    )
    return remember(coroutineScope, bottomSheetScaffoldState) {
        WatchedItemSheetScaffoldState(
            coroutineScope = coroutineScope,
            scaffoldState = bottomSheetScaffoldState,
        )
    }
}

@Composable
fun WatchedItemSheetScaffold(
    scaffoldState: WatchedItemSheetScaffoldState,
    watchedItemType: WatchedItemType,
    approximateVideoRuntime: Duration,
    content: @Composable () -> Unit,
) {
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
        scaffoldState.close()
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
            scaffoldState.mode?.let { mode ->
                key(scaffoldState.openCounter) {
                    WatchedItemSheetContent(
                        selections = rememberWatchedItemSelections(watchedItemType, mode = mode),
                        bottomSheetState = bottomSheetState,
                        watchedItemType = watchedItemType,
                        approximateVideoRuntime = approximateVideoRuntime,
                        onDismissRequest = { scaffoldState.close() },
                        onHeaderHeightChange = { headerHeight = it },
                        onScrollLabelHeightChange = { scrollLabelHeight = it },
                        onFooterHeightChange = { footerHeight = it },
                        innerBottomPadding = innerBottomPadding,
                    )
                }
            }
        },
        content = {
            BoxWithScrim(
                visible = bottomSheetState.targetValue != SheetValue.Hidden,
                color = BottomSheetDefaults.ScrimColor,
                onDismissRequest = { scaffoldState.close() },
            ) {
                content()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
private fun WatchedItemSheetContent(
    selections: WatchedItemSelections,
    bottomSheetState: SheetState,
    watchedItemType: WatchedItemType,
    approximateVideoRuntime: Duration,
    onDismissRequest: () -> Unit,
    onHeaderHeightChange: (Int) -> Unit,
    onScrollLabelHeightChange: (Int) -> Unit,
    onFooterHeightChange: (Int) -> Unit,
    innerBottomPadding: Dp,
) {
    val saveAction = rememberProfileDbActionState<Unit>(onSuccess = { onDismissRequest() })
    val deleteAction = rememberProfileDbActionState<Unit>(onSuccess = { onDismissRequest() })
    val showAdvancedOptions = bottomSheetState.targetValue == SheetValue.Expanded
    val lazyListState = rememberLazyListState()
    var titleHeight by remember { mutableIntStateOf(0) }
    var headerHeight by remember { mutableIntStateOf(0) }
    var scrollLabelHeight by remember { mutableIntStateOf(0) }

    DateTimeSectionDialog(selections.datetime)

    LaunchedEffect(showAdvancedOptions) {
        if (!showAdvancedOptions) {
            lazyListState.animateScrollToItem(0)
        }
    }

    val enabled = !saveAction.isLoading && !deleteAction.isLoading

    @Composable
    fun WatchedItemSheetScope.DimensionSection(selection: WatchedItemDimensionSelection<*>) {
        when (selection) {
            is WatchedItemDimensionSelection.Choice -> ChoiceSection(
                enabled = enabled,
                selection = selection,
                onSelectionChange = selections::update,
            )

            is WatchedItemDimensionSelection.FreeText -> FreeTextSection(
                enabled = enabled,
                selection = selection,
                onSelectionChange = selections::update,
            )
        }
    }

    BoxWithConstraints {
        ProfileDbErrorDialog(saveAction)

        val c = this.constraints
        LazyColumn(state = lazyListState, modifier = Modifier.animateContentSize()) {
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
                        DateTimeSection(enabled = enabled, selections.datetime, watchedItemType, approximateVideoRuntime)
                        for (selection in selections.dimensions.filter { it.dimension.isImportant }) {
                            AnimatedVisibility(visible = selection.dimension.isVisible(selections)) {
                                DimensionSection(selection)
                            }
                        }
                    }
                }
                for (selection in selections.dimensions.filterNot { it.dimension.isImportant }) {
                    if (selection.dimension.isVisible(selections)) {
                        item(key = selection.dimension.id) {
                            BelowScrollLabelContainer(showAdvancedOptions, scrollLabelHeight, modifier = Modifier.animateItem()) {
                                DimensionSection(selection)
                            }
                        }
                    }
                }
                // This is just to have an element at the bottom that will take the same size as the other Button
                item {
                    BelowScrollLabelContainer(showAdvancedOptions, scrollLabelHeight) {
                        ButtonRow(
                            selections = selections,
                            saveAction = saveAction,
                            deleteAction = deleteAction,
                            enabled = enabled,
                            innerBottomPadding = innerBottomPadding,
                            modifier = Modifier.alpha(0f),
                        )
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
        ButtonRow(
            selections = selections,
            saveAction = saveAction,
            deleteAction = deleteAction,
            enabled = enabled,
            innerBottomPadding = innerBottomPadding,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onPlaced {
                    onFooterHeightChange(it.size.height)
                }
                .graphicsLayer {
                    val naturalTranslation = -bottomSheetState.requireOffset() + c.maxHeight - this.size.height
                    translationY = naturalTranslation.coerceAtLeast(titleHeight.toFloat())
                },
        )
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
private fun BelowScrollLabelContainer(
    showAdvancedOptions: Boolean,
    scrollLabelHeight: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val translationY by animateFloatAsState(
        targetValue = if (showAdvancedOptions) 0f else scrollLabelHeight.toFloat(),
        animationSpec = tween(),
    )
    Box(modifier = modifier.graphicsLayer { this.translationY = translationY }) {
        content()
    }
}

@Composable
private fun ButtonRow(
    enabled: Boolean,
    selections: WatchedItemSelections,
    saveAction: ProfileDbActionState<Unit>,
    deleteAction: ProfileDbActionState<Unit>,
    innerBottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val mode = selections.sheetMode
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = innerBottomPadding)
            // Intercepting all input events, since this will be on top of other content
            .pointerInput(Unit) { },
        horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
    ) {
        if (mode is WatchedItemSheetMode.Edit) {
            OutlinedButton(
                enabled = enabled,
                onClick = { deleteAction.execute { db -> mode.watchedItem.delete(db) } },
                content = {
                    DelayedActionLoadingIndicator(action = deleteAction, modifier = Modifier.padding(end = 8.dp))
                    Text(R.string.delete_profile.str())
                },
            )
        }
        Button(
            enabled = enabled,
            onClick = { saveAction.execute(selections::save) },
        ) {
            DelayedActionLoadingIndicator(action = saveAction, modifier = Modifier.padding(end = 8.dp))
            Text(
                text = when (mode) {
                    is WatchedItemSheetMode.Edit -> R.string.save_action.str()
                    is WatchedItemSheetMode.New -> R.string.create_action.str()
                },
            )
        }
    }
}

@Composable
@Suppress("UnusedReceiverParameter")
fun WatchedItemSheetScope.Section(title: Text, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier) {
        Text(
            title.string(),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        content()
        Spacer(Modifier.height(16.dp))
    }
}
