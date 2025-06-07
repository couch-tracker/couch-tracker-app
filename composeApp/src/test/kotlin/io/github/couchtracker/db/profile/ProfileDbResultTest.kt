package io.github.couchtracker.db.profile

import io.github.couchtracker.db.profile.ProfileDbResult.FileError.AttemptedOperation
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.spyk
import io.mockk.verify
import java.io.FileNotFoundException

class ProfileDbResultTest : FunSpec(
    {
        context("map()") {
            test("works for Success") {
                val result = ProfileDbResult.Completed.Success("123")
                result.map { it.toInt() } shouldBe ProfileDbResult.Completed.Success(123)
            }

            context("doesn't do anything for other types") {
                withData<ProfileDbResult<String>>(
                    ProfileDbResult.Completed.Error(Exception("")),
                    ProfileDbResult.FileError.InvalidDatabase,
                    ProfileDbResult.FileError.UriCannotBeOpened(FileNotFoundException(), AttemptedOperation.WRITE),
                    ProfileDbResult.FileError.ContentProviderFailure(AttemptedOperation.READ),
                ) { instance ->
                    instance.map { it.toInt() } shouldBeSameInstanceAs instance
                }
            }
        }

        context("onError()") {
            test("nothing happens on success") {
                val onError = spyk<(ProfileDbResult<Nothing>) -> Unit>(@JvmSerializableLambda {})
                ProfileDbResult.Completed.Success("yey!").onError(onError)
                verify(exactly = 0) { onError(any()) }
            }
            context("onError() is called when there is an error") {
                withData(
                    ProfileDbResult.Completed.Error(Exception("")),
                    ProfileDbResult.MetadataError,
                    ProfileDbResult.FileError.InvalidDatabase,
                    ProfileDbResult.FileError.UriCannotBeOpened(FileNotFoundException(), AttemptedOperation.WRITE),
                    ProfileDbResult.FileError.ContentProviderFailure(AttemptedOperation.READ),
                ) { instance ->
                    val onError = spyk<(ProfileDbResult<Nothing>) -> Unit>(@JvmSerializableLambda {})
                    instance.onError(onError)
                    verify(exactly = 1) { onError(instance) }
                }
            }
        }
    },
)
