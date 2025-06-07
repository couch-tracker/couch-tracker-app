package io.github.couchtracker.utils

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

@Composable
@ReadOnlyComposable
fun @receiver:StringRes Int.str() = stringResource(this)

@Composable
@ReadOnlyComposable
fun @receiver:StringRes Int.str(vararg formatArgs: Any) = stringResource(this, *formatArgs)

@Composable
@ReadOnlyComposable
fun @receiver:PluralsRes Int.pluralStr(count: Int) = pluralStringResource(this, count)

@Composable
@ReadOnlyComposable
fun @receiver:PluralsRes Int.pluralStr(count: Int, vararg formatArgs: Any) = pluralStringResource(this, count, *formatArgs)
