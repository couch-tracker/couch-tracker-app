package io.github.couchtracker.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.image.TmdbImageUrlBuilder
import app.moviebase.tmdb.model.TmdbFileImage
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.str
import kotlin.math.roundToInt

@Suppress("TooManyFunctions")
object OverviewScreenComponents {

    private const val WIDE_COMPONENTS_FILL_PERCENTAGE = 0.75f
    private const val WIDE_COMPONENTS_ASPECT_RATIO = 16f / 9
    private const val COLUMN_COMPONENTS_ASPECT_RATIO = 3f / 2
    private const val ITEMS_PER_COLUMN = 4

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Header(
        title: String,
        backdrop: ImageRequest?,
        scrollBehavior: TopAppBarScrollBehavior,
        titleReplacement: (@Composable (isExpanded: Boolean) -> Unit)? = null,
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
                            if (titleReplacement != null) {
                                titleReplacement(isExpanded)
                            } else {
                                HeaderTitle(title, isExpanded)
                            }
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
    fun HeaderTitleTabRow(
        pagerState: PagerState,
        bigBoiFirst: Boolean,
        tabText: @Composable (Int) -> String,
        onPageClick: (Int) -> Unit,
    ) {
        val color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background)
        val indicator: @Composable TabIndicatorScope.() -> Unit = {
            TabRowDefaults.PrimaryIndicator(
                Modifier.tabIndicatorOffset(pagerState.currentPage, matchContentSize = true),
                width = Dp.Unspecified,
                color = color,
            )
        }
//        if (isExpanded) {
//            PrimaryTabRow(
//                selectedTabIndex = pagerState.currentPage,
//                containerColor = Color.Transparent,
//                contentColor = color,
//                divider = {},
//                indicator = indicator,
//            ) {
//                HeaderTitleTabs(pagerState, onPageClick, tabText)
//            }
//        } else {
//            BoxWithConstraints {
//                val widthLimit = this.maxWidth - 96.dp
//                PrimaryScrollableTabRow(
//                    selectedTabIndex = pagerState.currentPage,
//                    containerColor = Color.Transparent,
//                    contentColor = color,
//                    divider = {},
//                    indicator = indicator,
//                    edgePadding = 0.dp,
//                ) {
//                    HeaderTitleTabs(pagerState, onPageClick, tabText, widthLimit)
//                }
//            }
//        }
        BoxWithConstraints {
            val maxTabWidth = this.maxWidth - 64.dp
            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = color,
                divider = {},
                indicator = indicator,
                edgePadding = 0.dp,
                minTabWidth = 40.dp,
            ) {
                HeaderTitleTabs(pagerState, onPageClick, tabText, bigBoiFirst, maxTabWidth)
            }
        }
    }

    @Composable
    private fun HeaderTitleTabs(
        pagerState: PagerState,
        onPageClick: (Int) -> Unit,
        tabText: @Composable ((Int) -> String),
        bigBoiFirst: Boolean,
        maxTabWidth: Dp = Dp.Unspecified,
    ) {
        val titleStyle = MaterialTheme.typography.headlineMedium
        val baseStyle = MaterialTheme.typography.titleMedium
        for (page in 0..<pagerState.pageCount) {
            val chonk = page == 0 && bigBoiFirst
            val style = if (chonk) {
                titleStyle
            } else {
                baseStyle
            }
            val autoSize = if (chonk) {
                val base = TextAutoSize.StepBased(minFontSize = baseStyle.fontSize, maxFontSize = titleStyle.fontSize)
                object : TextAutoSize {
                    override fun TextAutoSizeLayoutScope.getFontSize(
                        constraints: Constraints,
                        text: AnnotatedString,
                    ): TextUnit {
                        base.run {
                            return getFontSize(constraints.copy(maxWidth = maxTabWidth.toPx().toInt()), text)
                        }
                    }

                    override fun equals(other: Any?) = other === this
                    override fun hashCode(): Int = super.hashCode()
                }
            } else {
                null
            }
            Tab(
                selected = page == pagerState.currentPage,
                onClick = { onPageClick(page) },
                text = {
                    Text(
                        tabText(page),
                        style = style,
                        maxLines = 1,
                        autoSize = autoSize,
                    )
                },
            )
        }
    }

    @Composable
    fun HeaderTitle(title: String, isExpanded: Boolean) {
        Text(
            title,
            maxLines = if (isExpanded) 2 else 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    @Composable
    fun Text(text: String?, maxLines: Int = Int.MAX_VALUE, style: TextStyle = MaterialTheme.typography.titleMedium) {
        if (!text.isNullOrBlank()) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 16.dp),
                maxLines = maxLines,
                style = style,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    fun LazyListScope.space() = item {
        Spacer(Modifier.height(8.dp))
    }

    fun LazyListScope.bottomSpace() = item {
        Spacer(Modifier.height(16.dp))
    }

    fun LazyListScope.tagsComposable(tags: List<String>) {
        if (tags.isNotEmpty()) {
            item {
                TagsRow(tags, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    fun LazyListScope.section(title: String, content: () -> Unit) {
        section(Text.Literal(title), content)
    }

    fun LazyListScope.section(@StringRes title: Int, content: () -> Unit) {
        section(Text.Resource(title), content)
    }

    fun LazyListScope.section(title: Text, content: () -> Unit) {
        item { Text(title.string()) }
        content()
        space()
    }

    @Composable
    private fun <T> HorizontalListSection(
        totalHeight: Int,
        items: List<T>,
        modifier: Modifier = Modifier,
        itemComposable: @Composable (T, targetHeightPx: Int) -> Unit,
    ) {
        BoxWithConstraints(modifier.fillMaxWidth()) {
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

    fun LazyListScope.textSection(title: String, content: String) {
        section(title) {
            item { Text(content, style = MaterialTheme.typography.bodyMedium) }
        }
    }

    fun LazyListScope.imagesSection(images: List<TmdbFileImage>, totalHeight: Int) {
        if (images.isNotEmpty()) {
            section(R.string.section_images) {
                item {
                    HorizontalListSection(totalHeight, images) { image, targetHeight ->
                        val imgWidth = (targetHeight * image.aspectRation).roundToInt()
                        val url = TmdbImageUrlBuilder.build(
                            image.filePath,
                            TmdbImageType.BACKDROP,
                            imgWidth,
                            targetHeight,
                        )
                        Surface(shape = MaterialTheme.shapes.small, shadowElevation = 8.dp, tonalElevation = 8.dp) {
                            AsyncImage(
                                url,
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

    fun LazyListScope.castSection(people: List<CastPortraitModel>, totalHeight: Int) {
        if (people.isNotEmpty()) {
            section(R.string.section_cast) {
                item {
                    HorizontalListSection(totalHeight, people, modifier = Modifier.animateContentSize()) { person, targetHeight ->
                        val w = with(LocalDensity.current) {
                            targetHeight.toDp() * PortraitComposableDefaults.POSTER_ASPECT_RATIO
                        }
                        CastPortrait(Modifier.width(w), person, onClick = null)
                    }
                }
            }
        }
    }

    fun LazyListScope.crewSection(people: List<CrewCompactListItemModel>, totalHeight: Int) {
        if (people.isNotEmpty()) {
            section(R.string.section_crew) {
                item {
                    HorizontalColumnsListSection(
                        totalHeight = totalHeight,
                        items = people,
                    ) { person ->
                        CrewCompactListItem(person)
                    }
                }
            }
        }
    }
}
