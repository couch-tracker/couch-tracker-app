package io.github.couchtracker.model.sort

import android.content.Context
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.model.sort.SortCategory.FirstPublicReleaseCenturyCategory
import io.github.couchtracker.model.sort.SortCategory.FirstPublicReleaseDecadeCategory
import io.github.couchtracker.model.sort.SortCategory.FirstPublicReleaseYearCategory
import io.github.couchtracker.model.sort.SortCategory.NameSortCategory
import io.github.couchtracker.model.sort.SortCategory.TmdbRatingCategory
import io.github.couchtracker.tmdb.TmdbRating
import kotlinx.datetime.YearMonth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias SortAndCategorizedList<T> = List<Pair<SortCategory, List<T>>>
typealias SortAndCategorizedLocalizedList<T> = List<Pair<String?, List<T>>>

@Serializable
sealed interface Sorter {
    /** Tne categories of this item, from most precise, to most generic */
    fun itemCategories(item: SortableItemModel): List<SortCategory>
    fun compareItems(a: SortableItemModel, b: SortableItemModel): Int

    fun <T> sortAndGroup(items: List<T>, getModel: (T) -> SortableItemModel): SortAndCategorizedList<T> = items.sortAndGroupBy(
        itemCategories = { itemCategories(getModel(it)) },
        compareItems = { a, b ->
            compareItems(getModel(a), getModel(b))
        },
    )

    @Serializable
    private sealed interface PropertySorter<T : Comparable<T>> : Sorter {
        val asc: Boolean
        fun getProperty(model: SortableItemModel): SortableProperty<T>
        fun propertyCategories(property: T): List<SortCategory>

        override fun itemCategories(item: SortableItemModel) = when (val property = getProperty(item)) {
            is SortableProperty.Known -> propertyCategories(property.value)
            SortableProperty.Error -> listOf(SortCategory.Error)
            SortableProperty.Unknown -> listOf(SortCategory.Unknown(this))
            SortableProperty.Unsupported -> listOf(SortCategory.Unsupported)
        }

        override fun compareItems(a: SortableItemModel, b: SortableItemModel): Int {
            return compareSortableProperties(asc, getProperty(a), getProperty(b))
        }
    }

    @Serializable
    @SerialName("Alphabetical")
    data class Alphabetical(override val asc: Boolean = true) : PropertySorter<String> {
        override fun getProperty(model: SortableItemModel) = model.name
        override fun propertyCategories(property: String) = listOf(
            // TODO: more categories
            SortCategory.RootCategory,
            NameSortCategory(property[0]),
        )
    }

    @Serializable
    @SerialName("ByTmdbRating")
    data class ByTmdbRating(override val asc: Boolean = false) : PropertySorter<TmdbRating> {
        override fun getProperty(model: SortableItemModel) = model.tmdbRating
        override fun propertyCategories(property: TmdbRating) = listOf(
            // TODO: more categories
            SortCategory.RootCategory,
            TmdbRatingCategory(property.average.toInt()),
        )
    }

    @Serializable
    @SerialName("ByReleaseDate")
    data class ByReleaseDate(override val asc: Boolean = false) : PropertySorter<PartialDateTime.Local> {
        override fun getProperty(model: SortableItemModel) = model.firstPublicRelease

        @Suppress("MagicNumber")
        override fun propertyCategories(property: PartialDateTime.Local) = buildList {
            add(SortCategory.RootCategory)
            if (property is PartialDateTime.Local.WithYear) {
                // Note: technically, this is the wrong formula for a century,
                // however we need for decades to partition a century if we want this to work
                add(FirstPublicReleaseCenturyCategory(property.year / 100 + 1))
                add(FirstPublicReleaseDecadeCategory(property.year / 10 * 10))
                add(FirstPublicReleaseYearCategory(property.year))
            }
            if (property is PartialDateTime.Local.WithYearMonth) {
                add(SortCategory.FirstPublicReleaseYearMonthCategory(YearMonth(property.year, property.month)))
            }
            if (property is PartialDateTime.Local.WithDate) {
                add(SortCategory.FirstPublicReleaseDateCategory(property.date))
            }
        }
    }
}

fun <T> List<T>.sortAndGroupBy(
    itemCategories: (T) -> List<SortCategory>,
    compareItems: (T, T) -> Int,
): SortAndCategorizedList<T> {
    if (this.isEmpty()) return emptyList()
    val categoriesByItem = this.associateWith { itemCategories(it) }

    val ret = mutableMapOf<SortCategory, List<T>>()
    buildGroups(
        categoriesByItem = categoriesByItem,
        ret = ret,
        depth = 1,
        items = this.groupBy {
            categoriesByItem.getValue(it)[0]
        },
    )

    return ret.sortGroups(compareItems)
}

private const val MIN_CATEGORIES_COUNT = 2

/** The minimum size the largest category should have to consider a split */
private const val MIN_LARGEST_CATEGORY_SIZE = 3

/** The minimum size the median category should have to consider a split */
private const val MIN_MEDIAN_CATEGORY_SIZE = 2

private fun <T> buildGroups(
    categoriesByItem: Map<T, List<SortCategory>>,
    ret: MutableMap<SortCategory, List<T>>,
    depth: Int,
    items: Map<SortCategory, List<T>>,
) {
    for ((category, items) in items) {
        if (items.all { categoriesByItem.getValue(it).size > depth }) {
            val subCategories = items
                .groupBy { categoriesByItem.getValue(it)[depth] }
            val sortedSizes = subCategories.values.map { it.size }.sorted()
            val maxSize = sortedSizes.last()
            val medianSize = sortedSizes[(sortedSizes.size - 1) / 2]
            val hasSingleChildCategory = subCategories.size == 1
            val childCategoriesMakeSense = subCategories.size >= MIN_CATEGORIES_COUNT &&
                maxSize >= MIN_LARGEST_CATEGORY_SIZE &&
                medianSize >= MIN_MEDIAN_CATEGORY_SIZE
            if (hasSingleChildCategory || childCategoriesMakeSense) {
                buildGroups(categoriesByItem, ret, depth + 1, subCategories)
            } else {
                ret[category] = items
            }
        } else {
            ret[category] = items
        }
    }
}

private fun <T> Map<SortCategory, List<T>>.sortGroups(compareItems: (T, T) -> Int): SortAndCategorizedList<T> {
    return entries
        .sortedWith { a, b ->
            compareItems(a.value.first(), b.value.first())
        }
        .map { (category, items) ->
            category to items.sortedWith { a, b -> compareItems(a, b) }
        }
}

fun <T> SortAndCategorizedList<T>.localized(context: Context): SortAndCategorizedLocalizedList<T> {
    return this.map { it.first.localize(context) to it.second }
}
