package io.github.couchtracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

data class AndroidResourceText(val resource: Int) : Text {
    @Composable
    override fun string(): String {
        return stringResource(android.R.string.ok)
    }
}
