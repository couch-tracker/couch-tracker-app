package io.github.couchtracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.couchtracker.R
import io.github.couchtracker.model.sort.SortAndCategorizedLocalizedList
import io.github.couchtracker.model.sort.SortableItemType
import io.github.couchtracker.model.sort.Sorter
import io.github.couchtracker.model.sort.SorterCategory
import io.github.couchtracker.model.sort.SorterCategoryState
import io.github.couchtracker.utils.str

private const val SORT_CATEGORIES_COLUMNS = 3
private val SORT_CATEGORY_WIDTH = 80.dp

@Composable
fun SortActionButton(
    itemTypes: Set<SortableItemType>,
    currentSorter: Sorter,
    setCurrentSorter: (Sorter) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton({ expanded = !expanded }) {
        Icon(Icons.AutoMirrored.Default.Sort, contentDescription = R.string.settings.str())
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        val categories = SorterCategory.entries.filter { sortCategory ->
            itemTypes.any { itemType -> itemType in sortCategory.supportedItems }
        }
        for (chunk in categories.chunked(SORT_CATEGORIES_COLUMNS)) {
            // See DropdownMenuVerticalPadding for padding
            Row(
                Modifier
                    .height(IntrinsicSize.Max)
                    .padding(horizontal = 8.dp),
            ) {
                for (category in chunk) {
                    DropdownSorterCategory(
                        modifier = Modifier.width(SORT_CATEGORY_WIDTH),
                        category = category,
                        state = category.currentState(currentSorter),
                        onSelected = {
                            setCurrentSorter(category.createSorter(currentSorter))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DropdownSorterCategory(
    modifier: Modifier,
    category: SorterCategory,
    state: SorterCategoryState,
    onSelected: () -> Unit,
) {
    Box(modifier = modifier.padding(4.dp)) {
        Surface(
            color = Color.Transparent,
            shape = MaterialTheme.shapes.small,
        ) {
            Column(
                modifier = Modifier
                    .clickable { onSelected() }
                    .padding(4.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val color = if (state == SorterCategoryState.UNSELECTED) {
                    LocalContentColor.current
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
                Icon(category.icon, contentDescription = null, tint = color)
                DropdownSorterCategoryLine(state, color)
                Text(
                    text = category.localizedName.string(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun DropdownSorterCategoryLine(state: SorterCategoryState, color: Color) {
    val triangleWidth = 8.dp
    val lineHeight = 3.dp
    val lineRadius = lineHeight / 2
    val lineTriangleSpacing = 4.dp

    @Suppress("MagicNumber")
    val triangleRotation by animateFloatAsState(if (state == SorterCategoryState.SELECTED_ASC) 0f else 180f)
    val triangleScale by animateFloatAsState(
        if (state == SorterCategoryState.SELECTED_ASC || state == SorterCategoryState.SELECTED_DESC) 1f else 0f,
    )
    val lineProgress by animateFloatAsState(if (state == SorterCategoryState.UNSELECTED) 0f else 1f)

    Canvas(
        modifier = Modifier
            .height(5.dp)
            .fillMaxWidth(),
    ) {
        val maxLineWidth = if (state == SorterCategoryState.SELECTED) {
            size.width
        } else {
            size.width - triangleWidth.toPx() - lineTriangleSpacing.toPx()
        }
        val lineWidth = maxLineWidth * lineProgress
        val lineCenterX = maxLineWidth / 2f
        val lineCenterY = size.height / 2f
        drawRoundRect(
            color = color,
            topLeft = Offset(lineCenterX - lineWidth / 2, lineCenterY - lineHeight.toPx() / 2),
            size = Size(lineWidth, lineHeight.toPx()),
            cornerRadius = CornerRadius(lineRadius.toPx()),
        )

        val triangleCenterX = size.width - triangleWidth.toPx() / 2f
        val trianglePath = Path().apply {
            moveTo(triangleCenterX, 0f)
            lineTo(size.width - triangleWidth.toPx(), size.height)
            lineTo(size.width, size.height)
            close()
        }
        withTransform(
            {
                val pivot = Offset(triangleCenterX, size.height / 2)
                rotate(triangleRotation, pivot = pivot)
                scale(triangleScale, triangleScale, pivot = pivot)
            },
        ) {
            drawPath(path = trianglePath, color = color)
        }
    }
}

@Composable
fun <T> SortedGrid(
    sorter: Sorter,
    sortedItems: SortAndCategorizedLocalizedList<T>,
    columns: GridCells,
    itemKey: (T) -> Any,
    itemComposable: @Composable LazyGridItemScope.(T) -> Unit,
) {
    val gridState = rememberLazyGridState()
    var previousSorter by remember { mutableStateOf(sorter) }
    // This effect will scroll to the top when the sorter change
    LaunchedEffect(sorter) {
        if (sorter != previousSorter) {
            gridState.scrollToItem(0)
        }
        previousSorter = sorter
    }
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        state = gridState,
        columns = columns,
        contentPadding = PaddingValues(8.dp) + PaddingValues(bottom = OverviewScreenComponents.LIST_BOTTOM_SPACE),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sortedItems.forEach { (category, items) ->
            if (category != null) {
                item(span = { GridItemSpan(this.maxLineSpan) }, key = category) {
                    Text(
                        category,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp, bottom = 12.dp)
                            .animateItem(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            items(items, key = { itemKey(it) }) { item ->
                itemComposable(item)
            }
            item(span = { GridItemSpan(this.maxLineSpan) }) {
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
