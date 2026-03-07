package io.github.couchtracker.ui.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.runtime.Composable
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.BookmarkableExternalId
import io.github.couchtracker.ui.components.ProfileDbErrorDialog
import io.github.couchtracker.utils.rememberProfileDbActionState
import io.github.couchtracker.utils.str

@Composable
fun BookmarkAction(id: BookmarkableExternalId): Action {
    val fullProfileData = LocalFullProfileDataContext.current
    val bookmarkAction = rememberProfileDbActionState<Unit>()

    val isBookmarked = id in fullProfileData.bookmarkedItems
    return Action(
        name = if (isBookmarked) {
            R.string.action_bookmark_remove.str()
        } else {
            R.string.action_bookmark_add.str()
        },
        icon = if (isBookmarked) {
            Icons.Filled.BookmarkRemove
        } else {
            Icons.Outlined.BookmarkAdd
        },
        enabled = !bookmarkAction.isLoading,
        companionComposable = {
            ProfileDbErrorDialog(bookmarkAction)
        },
        onClick = {
            if (isBookmarked) {
                bookmarkAction.execute { db -> db.bookmarkedItemQueries.delete(id) }
            } else {
                bookmarkAction.execute { db -> db.bookmarkedItemQueries.insert(id) }
            }
        },
    )
}
