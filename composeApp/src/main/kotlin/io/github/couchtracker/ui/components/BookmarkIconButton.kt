package io.github.couchtracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.BookmarkableExternalId
import io.github.couchtracker.utils.rememberProfileDbActionState
import io.github.couchtracker.utils.str

@Composable
fun BookmarkIconButton(id: BookmarkableExternalId) {
    val fullProfileData = LocalFullProfileDataContext.current
    val bookmarkAction = rememberProfileDbActionState<Unit>()

    val isBookmarked = id in fullProfileData.bookmarkedItems
    IconButton(
        enabled = !bookmarkAction.isLoading,
        onClick = {
            if (isBookmarked) {
                bookmarkAction.execute { db -> db.bookmarkedItemQueries.delete(id) }
            } else {
                bookmarkAction.execute { db -> db.bookmarkedItemQueries.insert(id) }
            }
        },
    ) {
        ProfileDbErrorDialog(bookmarkAction)
        if (isBookmarked) {
            Icon(Icons.Filled.BookmarkRemove, contentDescription = R.string.bookmark_remove.str())
        } else {
            Icon(Icons.Outlined.BookmarkAdd, contentDescription = R.string.bookmark_add.str())
        }
    }
}
