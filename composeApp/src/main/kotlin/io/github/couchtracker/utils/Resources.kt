package io.github.couchtracker.utils

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun @receiver:StringRes Int.str() = stringResource(this)

@Composable
fun @receiver:StringRes Int.str(vararg formatArgs: Any) = stringResource(this, *formatArgs)
