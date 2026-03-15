package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.engine.concurrency.SpecExecutionMode
import io.kotest.engine.coroutines.ThreadPerSpecCoroutineContextFactory

class ProjectConfig : AbstractProjectConfig() {
    override val specExecutionMode = SpecExecutionMode.Concurrent
    override val coroutineDispatcherFactory = ThreadPerSpecCoroutineContextFactory
}
