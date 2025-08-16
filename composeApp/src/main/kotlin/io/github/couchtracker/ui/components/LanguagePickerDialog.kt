package io.github.couchtracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.ibm.icu.text.DisplayContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.utils.LanguageCategory
import io.github.couchtracker.utils.LanguageItem
import io.github.couchtracker.utils.LocaleData
import io.github.couchtracker.utils.MixedValueTree
import io.github.couchtracker.utils.allLeafs
import io.github.couchtracker.utils.countLeafs
import io.github.couchtracker.utils.currentFirstLocale
import io.github.couchtracker.utils.findLeafValue
import io.github.couchtracker.utils.languageTree
import io.github.couchtracker.utils.pluralStr
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.toULocale
import org.koin.compose.koinInject

private val CAPITALIZATION = DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("SpreadOperator")
fun LanguagePickerDialog(
    language: Bcp47Language?,
    onLanguageSelected: (Bcp47Language?) -> Unit,
    suggestedLanguages: List<Bcp47Language>,
    onClose: () -> Unit,
) {
    val userLocale = LocalConfiguration.currentFirstLocale.toULocale()
    val allLocales = koinInject<LocaleData>().allLocales
    val languagesTreeRoot = remember(userLocale) {
        Bcp47Language.languageTree(
            allLocales = allLocales,
            comparator = compareBy {
                when (it) {
                    is MixedValueTree.Intermediate -> it.value.language
                    is MixedValueTree.Leaf -> it.value.language
                }.getDisplayName(userLocale, CAPITALIZATION)
            },
        )
    }
    val selected = language?.let { languagesTreeRoot.findLeafValue { it.language isEqualTo language } }

    TreePickerDialog(
        selected = selected,
        onSelect = { onLanguageSelected(it?.language) },
        root = languagesTreeRoot,
        suggestedOptions = SuggestedOptions(
            suggestedString = { R.string.suggested_languages.str() },
            allString = { R.string.all_languages.str() },
            suggestedItems = { node ->
                if (node is MixedValueTree.Root<*, *, *>) {
                    languagesTreeRoot
                        .allLeafs()
                        .filter { it.value.language in suggestedLanguages }
                        .sortedBy { suggestedLanguages.indexOf(it.value.language) }
                        .toList()
                } else {
                    emptyList()
                }
            },
        ),
        itemName = { it.language.getDisplayName(userLocale, CAPITALIZATION) },
        itemTrailingContent = { Text(it.language.toString()) },
        itemSupportingName = {
            when (it) {
                is LanguageItem.Normal -> it.language.getDisplayName(it.language.locale, CAPITALIZATION)
                is LanguageItem.Special -> it.description.str()
            }
        },
        itemKey = { it.toString() },
        itemSearchStrings = {
            listOfNotNull(
                it.language.getDisplayName(userLocale, CAPITALIZATION),
                when (it) {
                    is LanguageItem.Normal -> it.language.getDisplayName(it.language.locale, CAPITALIZATION)
                    is LanguageItem.Special -> null
                },
                it.language.toString(),
            )
        },
        categoryName = {
            when (it.value) {
                is LanguageCategory.Language -> it.value.language.getDisplayName(userLocale, CAPITALIZATION)
                LanguageCategory.Special -> R.string.language_category_special.str()
            }
        },
        categorySupportingName = {
            val cnt = it.countLeafs()
            R.plurals.language_category_subtitle.pluralStr(cnt, cnt)
        },
        icon = { Icon(Icons.Default.Language, contentDescription = null) },
        title = { Text(R.string.select_language.str()) },
        searchPlaceHolder = { R.string.language_search_placeholder.str() },
        nullSelectionButtonText = null,
        onClose = onClose,
    )
}
