import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

/** Common columns */
private val MOVIE_ID_COLUMN = SqlColumn.int("tmdbId", "io.github.couchtracker.tmdb.TmdbMovieId")
private val SHOW_ID_COLUMN = SqlColumn.int("tmdbId", "io.github.couchtracker.tmdb.TmdbShowId")
private val LANGUAGE_COLUMN = SqlColumn.text("language", "io.github.couchtracker.tmdb.TmdbLanguage")

/** Which caches to create */
private val CACHES = listOf(
    // Movie
    SqlTable(
        name = "MovieDetailsCache",
        key = listOf(MOVIE_ID_COLUMN, LANGUAGE_COLUMN),
        value = SqlColumn.text("details", "app.moviebase.tmdb.model.TmdbMovieDetail"),
    ),
    SqlTable(
        name = "MovieReleaseDatesCache",
        key = listOf(MOVIE_ID_COLUMN),
        value = SqlColumn.text("releaseDates", "kotlin.collections.List<app.moviebase.tmdb.model.TmdbReleaseDates>"),
    ),
    SqlTable(
        name = "MovieCreditsCache",
        key = listOf(MOVIE_ID_COLUMN),
        value = SqlColumn.text("credits", "app.moviebase.tmdb.model.TmdbCredits"),
    ),
    SqlTable(
        name = "MovieImagesCache",
        key = listOf(MOVIE_ID_COLUMN),
        value = SqlColumn.text("images", "app.moviebase.tmdb.model.TmdbImages"),
    ),
    SqlTable(
        name = "MovieVideosCache",
        key = listOf(MOVIE_ID_COLUMN),
        value = SqlColumn.text("videos", "kotlin.collections.List<app.moviebase.tmdb.model.TmdbVideo>"),
    ),
    // Show
    SqlTable(
        name = "ShowDetailsCache",
        key = listOf(SHOW_ID_COLUMN, LANGUAGE_COLUMN),
        value = SqlColumn.text("details", "app.moviebase.tmdb.model.TmdbShowDetail"),
    ),
    SqlTable(
        name = "ShowImagesCache",
        key = listOf(SHOW_ID_COLUMN),
        value = SqlColumn.text("images", "app.moviebase.tmdb.model.TmdbImages"),
    ),
    SqlTable(
        name = "ShowAggregateCreditsCache",
        key = listOf(SHOW_ID_COLUMN),
        value = SqlColumn.text("credits", "app.moviebase.tmdb.model.TmdbAggregateCredits"),
    ),
)

/** Which sql functions to generate */
private val SQL_FUNCTIONS = listOf(
    SqlFunction("get") { cache ->
        appendLine("SELECT ${cache.value.name}, ${cache.lastUpdate.name}")
        appendLine("FROM ${cache.name}")
        appendLine("WHERE " + cache.key.joinToString(" AND ") { "${it.name} = ?" } + ";")
    },
    SqlFunction("put") { cache ->
        appendLine("INSERT INTO ${cache.name}")
        appendLine("VALUES (${cache.key.joinToString { ":${it.name}" }}, :${cache.value.name}, :${cache.lastUpdate.name})")
        appendLine(
            "ON CONFLICT DO UPDATE SET " +
                "${cache.value.name} = :${cache.value.name}, " +
                "${cache.lastUpdate.name} = :${cache.lastUpdate.name};",
        )
    },
)

private data class SqlColumn(
    val name: String,
    val sqlType: String,
    val kotlinType: String,
    val nullable: Boolean = false,
) {
    fun columnDefinition() = buildString {
        append("$name $sqlType AS $kotlinType")
        if (!nullable) {
            append(" NOT NULL")
        }
    }

    companion object {
        fun int(name: String, kotlinType: String, nullable: Boolean = false) = SqlColumn(name, "INTEGER", kotlinType, nullable)
        fun text(name: String, kotlinType: String, nullable: Boolean = false) = SqlColumn(name, "TEXT", kotlinType, nullable)
    }
}

private data class SqlTable(
    val name: String,
    val key: List<SqlColumn>,
    val value: SqlColumn,
) {
    val lastUpdate = SqlColumn.text("lastUpdate", "kotlinx.datetime.Instant")

    init {
        require(key.isNotEmpty())
        require(key.all { !it.nullable })
    }

    fun tableDefinition() = buildString {
        appendLine("CREATE TABLE $name (")
        key.forEach { column ->
            appendLine("    ${column.columnDefinition()},")
        }
        appendLine("    ${value.columnDefinition()},")
        appendLine("    ${lastUpdate.columnDefinition()},")
        appendLine("    PRIMARY KEY(${key.joinToString { it.name }})")
        appendLine(");")
    }
}

private data class SqlFunction(
    val name: String,
    val content: StringBuilder.(SqlTable) -> Unit,
)

/**
 * A Gradle Task to generate the SQLDelight definitions related to the Tmdb Cache.
 */
open class SqlDelightTmdbCacheDefinitionsGenerator : DefaultTask() {
    /** Where to generate the .sq files */
    private val sqldelightRoot =
        project.layout.buildDirectory.dir("generated/sqldelight/tmdbCache/io/github/couchtracker/db/tmdbCache").get().asFile

    init {
        group = "io.github.couchtracker"
        description = "Generates the SqlDelight definitions for local the Tmdb Cache"
    }

    @TaskAction
    fun run() {
        sqldelightRoot.deleteRecursively()
        sqldelightRoot.mkdirs()
        for (cache in CACHES) {
            val cacheFile = File(sqldelightRoot, "${cache.name}.sq")
            cacheFile.writeText(
                buildString {
                    append(cache.tableDefinition())
                    SQL_FUNCTIONS.forEach { function ->
                        appendLine()
                        appendLine("${function.name}:")
                        function.content(this, cache)
                    }
                },
            )
        }
    }
}
