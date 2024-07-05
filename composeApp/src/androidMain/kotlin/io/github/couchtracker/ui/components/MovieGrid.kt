package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.ui.screens.navigateToMovie

@Composable
fun MovieGrid(
    movies: List<MoviePortraitModel>,
) {
    val navController = LocalNavController.current
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(minSize = MoviePortraitModel.SUGGESTED_WIDTH),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(movies) { movie ->
            MoviePortrait(
                Modifier.fillMaxWidth(),
                movie,
            ) {
                navController.navigateToMovie(movie.movie)
            }
        }
    }
}
