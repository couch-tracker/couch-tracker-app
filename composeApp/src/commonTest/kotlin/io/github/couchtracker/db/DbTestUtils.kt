package io.github.couchtracker.db

import app.cash.sqldelight.db.SqlDriver

expect fun inMemorySqliteDriver(): SqlDriver
