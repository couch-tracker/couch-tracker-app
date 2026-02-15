@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.watchedItem

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
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
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionSelectionValidity
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemSelectionsState
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.db.profile.model.watchedItem.rememberWatchedItemSelectionsState
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

@Stable
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
    mediaRuntime: () -> Duration?,
    mediaLanguages: () -> List<Bcp47Language>,
    containerColor: () -> Color,
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
        containerColor = containerColor(),
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
                        selections = rememberWatchedItemSelectionsState(watchedItemType, mode = mode),
                        bottomSheetState = bottomSheetState,
                        watchedItemType = watchedItemType,
                        mediaRuntime = mediaRuntime,
                        mediaLanguages = mediaLanguages,
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
    selections: WatchedItemSelectionsState,
    bottomSheetState: SheetState,
    watchedItemType: WatchedItemType,
    mediaRuntime: () -> Duration?,
    mediaLanguages: () -> List<Bcp47Language>,
    onDismissRequest: () -> Unit,
    onHeaderHeightChange: (Int) -> Unit,
    onScrollLabelHeightChange: (Int) -> Unit,
    onFooterHeightChange: (Int) -> Unit,
    innerBottomPadding: Dp,
) {
    val saveAction = rememberProfileDbActionState<Unit>(onSuccess = { onDismissRequest() })
    var showDeleteDialog by remember { mutableStateOf(false) }
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

    val enabled = !saveAction.isLoading

    BoxWithConstraints {
        ProfileDbErrorDialog(saveAction)
        if (selections.sheetMode is WatchedItemSheetMode.Edit) {
            DeleteWatchedItemConfirmDialog(
                watchedItem = if (showDeleteDialog) selections.sheetMode.watchedItem else null,
                onDismissRequest = { showDeleteDialog = false },
                onDeleted = { onDismissRequest() },
            )
        }

        val c = this.constraints
        LazyColumn(state = lazyListState, modifier = Modifier.animateContentSize()) {
            WatchedItemSelectionsScope.apply {
                item {
                    Column(
                        Modifier.onPlaced {
                            headerHeight = it.size.height
                            onHeaderHeightChange(it.size.height)
                        },
                    ) {
                        Text(
                            text = when (selections.sheetMode) {
                                is WatchedItemSheetMode.Edit -> R.string.edit_viewing.str()
                                is WatchedItemSheetMode.New -> when (watchedItemType) {
                                    WatchedItemType.MOVIE -> R.string.mark_movie_as_watched.str()
                                    WatchedItemType.EPISODE -> R.string.mark_episode_as_watched.str()
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .onPlaced { titleHeight = it.size.height },
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(16.dp))
                        DateTimeSection(enabled = enabled, selections.datetime, watchedItemType, mediaRuntime)
                        for (selection in selections.dimensions.filter { it.dimension.isImportant }) {
                            AnimatedVisibility(visible = selection.dimension.isEnabled(selections.dimensions)) {
                                DimensionSection(
                                    enabled = enabled,
                                    selection = selection,
                                    mediaLanguages = mediaLanguages,
                                    onSelectionChange = selections::update,
                                )
                            }
                        }
                    }
                }
                for (selection in selections.dimensions.filterNot { it.dimension.isImportant }) {
                    if (selection.dimension.isEnabled(selections.dimensions)) {
                        item(key = selection.dimension.id) {
                            BelowScrollLabelContainer(showAdvancedOptions, scrollLabelHeight, modifier = Modifier.animateItem()) {
                                DimensionSection(
                                    enabled = enabled,
                                    selection = selection,
                                    mediaLanguages = mediaLanguages,
                                    onSelectionChange = selections::update,
                                )
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
                            onDelete = { error("This should never be called") },
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
            onDelete = { showDeleteDialog = true },
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
    selections: WatchedItemSelectionsState,
    saveAction: ProfileDbActionState<Unit>,
    onDelete: () -> Unit,
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
                onClick = onDelete,
                content = { Text(R.string.delete_profile.str()) },
            )
        }
        Button(
            enabled = enabled && selections.isValid(),
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
fun WatchedItemSelectionsScope.Section(
    title: Text,
    validity: WatchedItemDimensionSelectionValidity,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            title.string(),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        content()

        val transition = updateTransition(validity)
        transition.AnimatedContent(
            contentKey = { it is WatchedItemDimensionSelectionValidity.Invalid },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { validity ->
            Box(Modifier.fillMaxWidth()) {
                if (validity is WatchedItemDimensionSelectionValidity.Invalid) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            modifier = Modifier.padding(start = 4.dp),
                            text = validity.message(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
