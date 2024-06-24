package io.github.couchtracker.tmdb

class TmdbException(e: Exception) : Exception(e.message, e)
