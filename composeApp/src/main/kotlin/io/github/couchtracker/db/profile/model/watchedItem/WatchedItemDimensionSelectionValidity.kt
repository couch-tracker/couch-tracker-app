package io.github.couchtracker.db.profile.model.watchedItem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import io.github.couchtracker.R
import io.github.couchtracker.utils.str

sealed interface WatchedItemDimensionSelectionValidity {

    data object Valid : WatchedItemDimensionSelectionValidity

    sealed interface Invalid : WatchedItemDimensionSelectionValidity {

        @Composable
        @ReadOnlyComposable
        fun message(): String

        data class TooManyChoicesSelected(val max: Int) : Invalid {
            @Composable
            override fun message() = R.string.too_many_choices_selected.str(max)
        }
    }
}
