package io.github.couchtracker.ui.screens.main.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.moviebase.tmdb.model.TmdbGenre
import io.github.couchtracker.R
import io.github.couchtracker.tmdb.getIcon
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.itemsWithPosition
import io.github.couchtracker.utils.str

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchExplorePage(padding: PaddingValues, viewModel: SearchViewModel) {
    LoadableScreen(
        data = viewModel.explorePageModel,
        onError = { exception ->
            Box(Modifier.padding(padding)) {
                DefaultErrorScreen(
                    errorMessage = exception.title.string(),
                    errorDetails = exception.details?.string(),
                    retry = { viewModel.retryMainPage() },
                )
            }
        },
    ) { defaultPageModel ->
        BoxWithConstraints {
            val columns = (maxWidth / 160.dp).toInt().coerceAtLeast(1)
            LazyVerticalGrid(
                GridCells.Fixed(columns),
                contentPadding = padding.plus(PaddingValues(vertical = 16.dp, horizontal = 16.dp)),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                genresList({ R.string.search_page_explore_shows.str() }, defaultPageModel.tvGenres, columns = columns)
                genresList({ R.string.search_page_explore_movies.str() }, defaultPageModel.movieGenres, columns = columns)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyGridScope.genresList(
    title: @Composable () -> String,
    genres: List<TmdbGenre>,
    columns: Int,
) {
    item(span = { GridItemSpan(this.maxLineSpan) }) {
        Text(
            text = title(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                // Plus the lazy list spacing is 4
                .padding(bottom = 2.dp),
        )
    }
    itemsWithPosition(genres, columns) { position, genre ->
        ListItem(
            onClick = { /* TODO */ },
            shapes = ListItemShapes(position = position),
            trailingContent = {
                val icon = genre.getIcon()
                if (icon != null) {
                    Text(
                        icon,
                        fontSize = with(LocalDensity.current) { 20.dp.toSp() },
                    )
                }
            },
        ) {
            Text(genre.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    item(span = { GridItemSpan(this.maxLineSpan) }) {
        // Plus the lazy list spacing is 24
        Spacer(Modifier.height(20.dp))
    }
}
