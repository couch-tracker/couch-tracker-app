package io.github.couchtracker.db.user

import io.github.couchtracker.db.user.UserDbResult.FileError.AttemptedOperation
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.spyk
import io.mockk.verify
import java.io.FileNotFoundException

class UserDbResultTest : FunSpec(
    {
        context("map()") {
            test("works for Success") {
                val result = UserDbResult.Completed.Success("123")
                result.map { it.toInt() } shouldBe UserDbResult.Completed.Success(123)
            }

            context("doesn't do anything for other types") {
                withData<UserDbResult<String>>(
                    UserDbResult.Completed.Error(Exception("")),
                    UserDbResult.FileError.InvalidDatabase,
                    UserDbResult.FileError.UriCannotBeOpened(FileNotFoundException(), AttemptedOperation.WRITE),
                    UserDbResult.FileError.ContentProviderFailure(AttemptedOperation.READ),
                ) { instance ->
                    instance.map { it.toInt() } shouldBeSameInstanceAs instance
                }
            }
        }

        context("onError()") {
            test("nothing happens on success") {
                val onError = spyk<(UserDbResult<Nothing>) -> Unit>(@JvmSerializableLambda {})
                UserDbResult.Completed.Success("yey!").onError(onError)
                verify(exactly = 0) { onError(any()) }
            }
            context("onError() is called when there is an error") {
                withData(
                    UserDbResult.Completed.Error(Exception("")),
                    UserDbResult.InterruptedError,
                    UserDbResult.FileError.InvalidDatabase,
                    UserDbResult.FileError.UriCannotBeOpened(FileNotFoundException(), AttemptedOperation.WRITE),
                    UserDbResult.FileError.ContentProviderFailure(AttemptedOperation.READ),
                ) { instance ->
                    val onError = spyk<(UserDbResult<Nothing>) -> Unit>(@JvmSerializableLambda {})
                    instance.onError(onError)
                    verify(exactly = 1) { onError(instance) }
                }
            }
        }
    },
)
