package io.github.couchtracker.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.Koin
import org.koin.core.awaitAllStartJobs
import org.koin.core.module.KoinDslMarker
import org.koin.core.module.LazyModule
import org.koin.dsl.ModuleDeclaration
import org.koin.dsl.module

/**
 * A lazy Koin module, where all definitions are eager.
 */
@KoinDslMarker
fun lazyEagerModule(moduleDefinition: ModuleDeclaration): LazyModule = LazyModule {
    module(createdAtStart = true) {
        moduleDefinition()
    }
}

/**
 * Loads all Koin modules and their eager definitions
 */
suspend fun Koin.loadAll() {
    withContext(Dispatchers.Default) {
        awaitAllStartJobs()
        createEagerInstances()
    }
}
