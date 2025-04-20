package io.github.couchtracker.db.common.defaultdata

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransactionWithoutReturn
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withData
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence

class DefaultDataHandlerTest : FunSpec(
    {
        isolationMode = IsolationMode.InstancePerTest

        val db = mockk<Transacter> {
            // any transaction() calls simply invokes its body
            every { transaction(body = any()) } answers {
                arg<TransactionWithoutReturn.() -> Unit>(1).invoke(mockk())
            }
        }
        val defaultData = mockk<DefaultData<Transacter>>(relaxed = true)

        test("if default data version is null, data is initialized") {
            val handler = spyk(HandlerImpl(defaultData, currentVersion = null, latestVersion = 2))
            shouldNotThrowAny {
                handler.handle(db)
            }
            verify(exactly = 0) { defaultData.upgradeTo(any(), any()) }
            verifySequence {
                handler.handle(db)
                db.transaction(body = any())
                with(handler) { db.getVersion() }
                defaultData.insert(db)
                with(handler) { db.setVersion(2) }
            }
        }
        test("if default data version is out of date, upgrade is done correctly") {
            val handler = spyk(HandlerImpl(defaultData, currentVersion = 1, latestVersion = 4))
            shouldNotThrowAny {
                handler.handle(db)
            }
            verifySequence {
                handler.handle(db)
                db.transaction(body = any())
                with(handler) { db.getVersion() }
                defaultData.upgradeTo(db, 2)
                defaultData.upgradeTo(db, 3)
                defaultData.upgradeTo(db, 4)
                with(handler) { db.setVersion(4) }
            }
        }
        context("nothing is done if") {
            withData(
                mapOf(
                    "default data version is up to date" to tuple(2, 2),
                    "default data version is bigger than the app's latest version" to tuple(5, 2),
                ),
            ) { (current, latest) ->
                val handler = spyk(HandlerImpl(defaultData, currentVersion = current, latestVersion = latest))
                shouldNotThrowAny {
                    handler.handle(db)
                }
                confirmVerified(defaultData)
            }
        }
    },
)

private class HandlerImpl(
    defaultData: DefaultData<Transacter>,
    private val currentVersion: Int?,
    override val latestVersion: Int,
) : DefaultDataHandler<Transacter>(defaultData) {

    public override fun Transacter.setVersion(version: Int) {
        // no-op
    }

    public override fun Transacter.getVersion() = currentVersion
}
