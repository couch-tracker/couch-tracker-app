package io.github.couchtracker.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.SizeAwareLazyListScope
import io.github.couchtracker.ui.countingElements
import io.github.couchtracker.utils.ApiLoadable
import io.github.couchtracker.utils.DeferredApiResult
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.emitAsFlow
import io.github.couchtracker.utils.str
import kotlinx.coroutines.flow.any
import kotlin.math.roundToInt

@Suppress("TooManyFunctions")
object OverviewScreenComponents {

    private const val UNSELECTED_TABS_ALPHA = 0.75f
    private const val WIDE_COMPONENTS_FILL_PERCENTAGE = 0.75f
    private const val WIDE_COMPONENTS_ASPECT_RATIO = 16f / 9
    private const val COLUMN_COMPONENTS_ASPECT_RATIO = 3f / 2
    private const val ITEMS_PER_COLUMN = 4
    private const val PLACEHOLDER_IMAGES_COUNT = 6
    private const val PLACEHOLDER_CAST_COUNT = 6
    private const val PLACEHOLDER_CREW_COLUMNS_COUNT = 3

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Header(
        title: String,
        backdrop: ImageRequest?,
        scrollBehavior: TopAppBarScrollBehavior,
        belowAppBar: @Composable ColumnScope.() -> Unit = {},
    ) {
        val navController = LocalNavController.current
        BackgroundTopAppBar(
            scrollBehavior = scrollBehavior,
            backdrop = backdrop,
            appBar = { colors ->
                Column {
                    LargeTopAppBar(
                        colors = colors,
                        title = {
                            val isExpanded = LocalTextStyle.current == MaterialTheme.typography.headlineMedium
                            Text(
                                title,
                                maxLines = if (isExpanded) 6 else 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        navigationIcon = {
                            IconButton({ navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = R.string.back_action.str(),
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior,
                    )
                    belowAppBar()
                }
            },
        )
    }

    @Composable
    fun HeaderTabRow(
        pagerState: PagerState,
        tabText: @Composable (Int) -> String,
        onPageClick: (Int) -> Unit,
    ) {
        val color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background)
        val unselectedColor = lerp(MaterialTheme.colorScheme.background, color, UNSELECTED_TABS_ALPHA)
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = color,
            divider = {},
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    Modifier.tabIndicatorOffset(pagerState.currentPage, matchContentSize = true),
                    width = Dp.Unspecified,
                    color = color,
                )
            },
        ) {
            for (page in 0..<pagerState.pageCount) {
                Tab(
                    selected = page == pagerState.currentPage,
                    unselectedContentColor = unselectedColor,
                    onClick = { onPageClick(page) },
                ) {
                    HeaderTab(tabText(page))
                }
            }
        }
    }

    @Composable
    fun HeaderTab(text: String) {
        // This is a simplified and more compact version of TabBaselineLayout
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.heightIn(min = 40.dp),
        ) {
            Text(
                text,
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }

    @Composable
    fun ShowSnackbarOnErrorEffect(
        snackbarHostState: SnackbarHostState,
        state: Set<DeferredApiResult<*>>,
        onRetry: () -> Unit,
        retryMessage: String = R.string.error_loading_data.str(),
        retryAction: String = R.string.retry_action.str(),
    ) {
        LaunchedEffect(snackbarHostState, state, onRetry, retryMessage, retryAction) {
            if (state.emitAsFlow().any { it is Result.Error }) {
                val result = snackbarHostState.showSnackbar(
                    retryMessage,
                    actionLabel = retryAction,
                    duration = SnackbarDuration.Indefinite,
                    withDismissAction = true,
                )
                when (result) {
                    SnackbarResult.Dismissed -> {}
                    SnackbarResult.ActionPerformed -> {
                        onRetry()
                    }
                }
            }
        }
    }

    @Composable
    fun Paragraph(
        text: String?,
        modifier: Modifier = Modifier,
        maxLines: Int = Int.MAX_VALUE,
        style: TextStyle = MaterialTheme.typography.titleMedium,
    ) {
        if (!text.isNullOrBlank()) {
            Text(
                text,
                modifier = modifier.padding(horizontal = 16.dp),
                maxLines = maxLines,
                style = style,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    @Composable
    fun ContentList(
        innerPadding: PaddingValues,
        modifier: Modifier = Modifier,
        content: LazyListScope.() -> Unit,
    ) {
        LazyColumn(
            contentPadding = innerPadding,
            modifier = modifier.fillMaxSize(),
            // Same as SpaceBetweenItems
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            content()
            bottomSpace()
        }
    }

    @Composable
    fun SpaceBetweenItems() = Spacer(Modifier.height(4.dp))

    fun LazyListScope.space() = item(key = null, contentType = "spacer") {
        // Plus the lazy list spacing is 24
        Spacer(Modifier.height(16.dp))
    }

    fun LazyListScope.topSpace() = item(key = "top-spacer", contentType = "spacer") {
        // Plus the spacing is 28.dp. Same as AppBar.LargeTitleBottomPadding
        Spacer(Modifier.height(24.dp))
    }

    fun LazyListScope.bottomSpace() = item(key = "bottom-spacer", contentType = "spacer") {
        // Plus the lazy list spacing is 96
        Spacer(Modifier.height(92.dp))
    }

    fun LazyListScope.section(sectionKey: String, title: String?, content: SizeAwareLazyListScope.() -> Unit) {
        val nullableTitle = when {
            title.isNullOrBlank() -> null
            else -> Text.Literal(title)
        }
        section(sectionKey, nullableTitle, content)
    }

    fun LazyListScope.section(sectionKey: String, @StringRes title: Int, content: SizeAwareLazyListScope.() -> Unit) {
        section(sectionKey, Text.Resource(title), content)
    }

    fun LazyListScope.section(sectionKey: String, title: Text? = null, content: SizeAwareLazyListScope.() -> Unit) {
        countingElements {
            if (title != null) {
                item(key = "$sectionKey-title", contentType = "section-title") {
                    Paragraph(title.string(), modifier = Modifier.animateItem())
                }
            }
            content()
            if (itemCount > 0) {
                space()
            }
        }
    }

    @Composable
    fun TagsComposable(tags: List<String>) {
        TagsRow(tags, modifier = Modifier.padding(horizontal = 16.dp))
    }

    @Composable
    private fun <T> HorizontalListSection(
        totalHeight: Int,
        items: List<T>,
        modifier: Modifier = Modifier,
        itemComposable: @Composable (T, targetHeightPx: Int) -> Unit,
    ) {
        BoxWithConstraints(
            modifier
                .padding(top = 4.dp)
                .fillMaxWidth(),
        ) {
            val width = this.constraints.maxWidth
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                val targetHeightPx = (width * WIDE_COMPONENTS_FILL_PERCENTAGE / WIDE_COMPONENTS_ASPECT_RATIO)
                    .coerceAtMost(totalHeight / 2f)
                    .roundToInt()
                items(items) { item ->
                    itemComposable(item, targetHeightPx)
                }
            }
        }
    }

    @Composable
    private fun <T> HorizontalColumnsListSection(
        totalHeight: Int,
        items: List<T>,
        modifier: Modifier = Modifier,
        itemsPerColumn: Int = ITEMS_PER_COLUMN,
        itemComposable: @Composable (T) -> Unit,
    ) {
        HorizontalListSection(
            totalHeight = totalHeight,
            items = items.chunked(itemsPerColumn),
            modifier = modifier,
        ) { itemsInColumn, targetHeight ->
            val targetWidth = with(LocalDensity.current) {
                targetHeight.toDp() * COLUMN_COMPONENTS_ASPECT_RATIO
            }
            Column(Modifier.width(targetWidth), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                for (item in itemsInColumn) {
                    itemComposable(item)
                }
            }
        }
    }

    fun LazyListScope.paragraphSection(sectionKey: String, title: String?, content: String) {
        section(sectionKey, title) {
            item(key = "$sectionKey-content", contentType = "text-section-content") {
                Paragraph(content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.animateItem())
            }
        }
    }

    private fun <T> ApiLoadable<List<T>>.buildDefaults(default: () -> List<T>): List<T> {
        return when (this) {
            is Result.Value -> this.value
            Loadable.Loading -> default()
            is Result.Error<*> -> emptyList()
        }
    }

    fun LazyListScope.imagesSection(
        images: ApiLoadable<List<ImageModel>>,
        totalHeight: Int,
        // To align with other sections, using the default aspect ratio of PortraitComposable
        defaultAspectRatio: Float = PortraitComposableDefaults.POSTER_ASPECT_RATIO,
    ) {
        val imagesList = images.buildDefaults {
            List(PLACEHOLDER_IMAGES_COUNT) { ImageModel.Preloaded(defaultAspectRatio, null) }
        }
        if (imagesList.isNotEmpty()) {
            section("images", R.string.section_images) {
                item(key = "images-content") {
                    HorizontalListSection(totalHeight, imagesList, modifier = Modifier.animateItem()) { image, targetHeight ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            shadowElevation = 8.dp,
                            tonalElevation = 8.dp,
                        ) {
                            val transition = updateTransition(image)
                            transition.AnimatedContent(
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                            ) { image ->
                                val aspectRation = image.aspectRatio ?: defaultAspectRatio
                                val imgWidth = (targetHeight * aspectRation).roundToInt()
                                val model = image.getCoilModel(imgWidth, targetHeight)
                                AsyncImage(
                                    model,
                                    modifier = Modifier
                                        .width(with(LocalDensity.current) { imgWidth.toDp() })
                                        .height(with(LocalDensity.current) { targetHeight.toDp() }),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun LazyListScope.castSection(people: ApiLoadable<List<CastPortraitModel>>, totalHeight: Int) {
        val peopleList = people.buildDefaults { List(PLACEHOLDER_CAST_COUNT) { null } }
        if (peopleList.isNotEmpty()) {
            section("cast", R.string.section_cast) {
                item(key = "cast-content") {
                    HorizontalListSection(
                        totalHeight = totalHeight,
                        items = peopleList,
                        modifier = Modifier.animateItem(),
                    ) { person, targetHeight ->
                        val w = with(LocalDensity.current) {
                            targetHeight.toDp() * PortraitComposableDefaults.POSTER_ASPECT_RATIO
                        }
                        Crossfade(person) { p ->
                            CastPortrait(Modifier.width(w), p, onClick = null)
                        }
                    }
                }
            }
        }
    }

    fun LazyListScope.crewSection(people: ApiLoadable<List<CrewCompactListItemModel>>, totalHeight: Int) {
        val peopleList = people.buildDefaults { List(ITEMS_PER_COLUMN * PLACEHOLDER_CREW_COLUMNS_COUNT) { null } }
        if (peopleList.isNotEmpty()) {
            section("crew", R.string.section_crew) {
                item(key = "crew-content") {
                    HorizontalColumnsListSection(
                        totalHeight = totalHeight,
                        items = peopleList,
                        modifier = Modifier.animateItem(),
                    ) { person ->
                        Crossfade(person) { p ->
                            CrewCompactListItem(p)
                        }
                    }
                }
            }
        }
    }
}
