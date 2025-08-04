package io.github.couchtracker.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.ibm.icu.text.DisplayContext
import io.github.couchtracker.R
import io.github.couchtracker.Settings
import io.github.couchtracker.db.profile.toLossyBcp47Language
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.languageTree
import io.github.couchtracker.tmdb.tmdbDownloadResult
import io.github.couchtracker.tmdb.toTmdbLanguage
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.SuggestedOptions
import io.github.couchtracker.ui.components.TreePickerDialog
import io.github.couchtracker.utils.ApiLoadable
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.MixedValueTree
import io.github.couchtracker.utils.allLeafs
import io.github.couchtracker.utils.collectAsLoadableWithLifecycle
import io.github.couchtracker.utils.countLeafs
import io.github.couchtracker.utils.currentFirstLocale
import io.github.couchtracker.utils.currentLocales
import io.github.couchtracker.utils.pluralStr
import io.github.couchtracker.utils.rememberWriteThroughAsyncMutableState
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.toList
import io.github.couchtracker.utils.toULocale
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.SwitchPreference
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.collections.orEmpty
import kotlin.collections.plus
import kotlin.collections.toMutableList

private const val LOG_TAG = "TmdbLanguagesSettingsScreen"
private val CAPITALIZATION = DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU
private const val MAX_LANGUAGES = 5

@Serializable
data object TmdbLanguagesSettingsScreen : Screen() {

    override fun profileDataContext() = false

    @Composable
    override fun content() = Content()
}

// Header + Switch + Category label
private const val NUMBER_OF_ITEMS_BEFORE_LANGUAGE_LIST = 3

@Composable
private fun Content() {
    val settingsTmdbLanguages by Settings.TmdbLanguages.collectAsLoadableWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var allTmdbLanguages by remember { mutableStateOf<ApiLoadable<List<TmdbLanguage>>>(Loadable.Loading) }

    suspend fun downloadTmdbLanguages() {
        allTmdbLanguages = Loadable.Loading
        allTmdbLanguages = tmdbDownloadResult(logTag = LOG_TAG) { tmdb ->
            tmdb.configuration.getPrimaryTranslations().map { TmdbLanguage.parse(it) }
        }
    }
    LaunchedEffect(Unit) { downloadTmdbLanguages() }

    LoadableScreen(
        data = allTmdbLanguages,
        onError = {
            DefaultErrorScreen(
                errorMessage = it.title.string(),
                errorDetails = it.details?.string(),
                retry = { coroutineScope.launch { downloadTmdbLanguages() } },
            )
        },
    ) { allTmdbLanguages ->
        LoadableScreen(data = settingsTmdbLanguages) { settingsTmdbLanguages ->
            LoadedContent(
                allTmdbLanguages = allTmdbLanguages,
                settingsTmdbLanguages = settingsTmdbLanguages,
            )
        }
    }
}

@Composable
private fun LoadedContent(
    allTmdbLanguages: List<TmdbLanguage>,
    settingsTmdbLanguages: List<TmdbLanguage>?,
) {
    val systemLanguages = LocalConfiguration.currentLocales.toList()
        .mapNotNull { it.toTmdbLanguage() }
        .ifEmpty { listOf(TmdbLanguage.FALLBACK) }
        .distinct()
        .take(MAX_LANGUAGES)

    var settingsTmdbLanguages by rememberWriteThroughAsyncMutableState(
        asyncValue = settingsTmdbLanguages,
        setValue = {
            if (it != null) {
                Settings.TmdbLanguages.set(it)
            } else {
                Settings.TmdbLanguages.reset()
            }
        },
    )
    val actualLanguages = settingsTmdbLanguages ?: systemLanguages
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            settingsTmdbLanguages?.let {
                settingsTmdbLanguages = it.toMutableList().apply {
                    add(
                        index = to.index - NUMBER_OF_ITEMS_BEFORE_LANGUAGE_LIST,
                        element = removeAt(from.index - NUMBER_OF_ITEMS_BEFORE_LANGUAGE_LIST),
                    )
                }
            }
        },
    )

    BaseSettings(
        title = R.string.tmdb_languages.str(),
        header = R.string.tmdb_languages_settings_header.str(),
        footer = R.string.tmdb_languages_settings_footer.str(),
        lazyColumnState = lazyListState,
    ) {
        val enableCustomLanguages = settingsTmdbLanguages != null
        item("use-app-languages") {
            SwitchPreference(
                modifier = Modifier.animateItem(),
                value = !enableCustomLanguages,
                onValueChange = {
                    settingsTmdbLanguages = if (it) null else systemLanguages
                },
                title = { Text(R.string.use_system_languages.str()) },
            )
        }
        item("language-list") {
            PreferenceCategory(
                modifier = Modifier.animateItem(),
                title = { Text(R.string.language_list.str()) },
            )
        }
        check(itemCount == NUMBER_OF_ITEMS_BEFORE_LANGUAGE_LIST)
        languages(
            languages = actualLanguages,
            enabled = enableCustomLanguages,
            reorderableState = reorderableState,
            onLanguageDeleted = { lang ->
                settingsTmdbLanguages?.takeIf { it.size > 1 }?.let {
                    settingsTmdbLanguages = it - lang
                }
            },
        )
        addLanguage(
            allTmdbLanguages = allTmdbLanguages - settingsTmdbLanguages.orEmpty(),
            enabled = enableCustomLanguages,
            suggestedLanguages = systemLanguages - settingsTmdbLanguages.orEmpty(),
            reachedMax = settingsTmdbLanguages.orEmpty().size >= MAX_LANGUAGES,
            onLanguageSelected = { lang ->
                settingsTmdbLanguages?.let {
                    settingsTmdbLanguages = (it + lang).distinct()
                }
            },
        )
    }
}

private fun LazyListScope.languages(
    languages: List<TmdbLanguage>,
    enabled: Boolean,
    reorderableState: ReorderableLazyListState,
    onLanguageDeleted: (TmdbLanguage) -> Unit,
) {
    items(languages, key = { it.toString() }) { language ->
        val bcp47Language = language.toBcp47Language()
        val systemLanguage = LocalConfiguration.currentFirstLocale.toULocale().toLossyBcp47Language()
        ReorderableItem(
            enabled = enabled,
            state = reorderableState,
            key = language.toString(),
        ) { isDragging ->
            Box(Modifier.background(MaterialTheme.colorScheme.background)) {
                Preference(
                    enabled = enabled,
                    title = { Text(bcp47Language.getDisplayName(systemLanguage.locale, CAPITALIZATION)) },
                    summary = { Text(bcp47Language.getDisplayName(bcp47Language.locale, CAPITALIZATION)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = R.string.drag_handle.str(),
                            modifier = Modifier.draggableHandle(enabled = enabled),
                        )
                    },
                    widgetContainer = {
                        AnimatedVisibility(visible = languages.size > 1, enter = fadeIn(), exit = fadeOut()) {
                            IconButton(
                                enabled = enabled,
                                onClick = { onLanguageDeleted(language) },
                                modifier = Modifier.padding(end = 8.dp),
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = R.string.remove_language.str())
                            }
                        }
                    },
                )
            }
        }
    }
}

private fun LazyListScope.addLanguage(
    allTmdbLanguages: List<TmdbLanguage>,
    suggestedLanguages: List<TmdbLanguage>,
    enabled: Boolean,
    reachedMax: Boolean,
    onLanguageSelected: (TmdbLanguage) -> Unit,
) {
    item("add-language") {
        var pickerOpen by remember { mutableStateOf(false) }
        Preference(
            modifier = Modifier.animateItem(),
            enabled = enabled,
            icon = { Icon(if (reachedMax) Icons.Default.Warning else Icons.Default.Add, contentDescription = null) },
            title = { if (!reachedMax) Text(R.string.add_language.str()) },
            summary = {
                if (reachedMax) {
                    Text(R.string.tmdb_languages_limit_reached.str())
                }
            },
            onClick = if (!reachedMax) {
                { pickerOpen = true }
            } else {
                null
            },
        )

        if (pickerOpen) {
            TmdbLanguagePickerDialog(
                allTmdbLanguages = allTmdbLanguages,
                onLanguageSelected = {
                    if (it != null) {
                        onLanguageSelected(it)
                    }
                },
                suggestedLanguages = suggestedLanguages,
                onClose = { pickerOpen = false },
            )
        }
    }
}

@Composable
private fun TmdbLanguagePickerDialog(
    allTmdbLanguages: List<TmdbLanguage>,
    onLanguageSelected: (TmdbLanguage?) -> Unit,
    suggestedLanguages: List<TmdbLanguage>,
    onClose: () -> Unit,
) {
    val userLocale = LocalConfiguration.currentFirstLocale.toULocale()

    val languagesTreeRoot = remember(userLocale) {
        allTmdbLanguages.languageTree(
            compareBy {
                when (it) {
                    is MixedValueTree.Intermediate -> it.value
                    is MixedValueTree.Leaf -> it.value
                }.toBcp47Language().getDisplayName(userLocale, CAPITALIZATION)
            },
        )
    }

    val itemName: @Composable (TmdbLanguage) -> String = { it.toBcp47Language().getDisplayName(userLocale, CAPITALIZATION) }
    val itemSupportingName: @Composable (TmdbLanguage) -> String = {
        it.toBcp47Language().getDisplayName(it.toBcp47Language().locale, CAPITALIZATION)
    }
    TreePickerDialog(
        selected = null,
        onSelect = { onLanguageSelected(it) },
        root = languagesTreeRoot,
        suggestedOptions = SuggestedOptions(
            suggestedString = { R.string.suggested_languages.str() },
            allString = { R.string.all_languages.str() },
            suggestedItems = { node ->
                if (node is MixedValueTree.Root<*, *, *>) {
                    languagesTreeRoot
                        .allLeafs()
                        .filter { it.value in suggestedLanguages }
                        .sortedBy { suggestedLanguages.indexOf(it.value) }
                        .toList()
                } else {
                    emptyList()
                }
            },
        ),
        itemName = itemName,
        itemTrailingContent = { Text(it.serialize()) },
        itemSupportingName = itemSupportingName,
        itemKey = { it.toString() },
        itemSearchStrings = {
            listOf(
                itemName(it),
                itemSupportingName(it),
                it.serialize(),
            )
        },
        categoryName = { itemName(it.value) },
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
