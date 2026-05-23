## Test Report

Date: 2026-05-20

Summary:

- Ran unit tests for `app` module: `:app:testDebugUnitTest`.
- Result: ALL TESTS PASSED.

Details:

- Tests added:
  - `app/src/test/kotlin/com/carpoolapp/domain/usecase/GetFeedUseCaseTest.kt` — verifies `GetFeedUseCase` returns repository flow.
  - `app/src/test/kotlin/com/carpoolapp/ui/home/HomeViewModelTest.kt` — verifies `HomeViewModel` emits Success when user is present and Error when no user.
  - `app/src/test/kotlin/com/carpoolapp/domain/usecase/PublicarViajeUseCaseTest.kt` — verifies `PublicarViajeUseCase` calls repository `crear` and returns id.

- Command executed:
```
./gradlew clean :app:testDebugUnitTest
```

- Test output: BUILD SUCCESSFUL. All unit tests passed.

Modified files (tests and changes related to them):
- `app/src/test/kotlin/com/carpoolapp/domain/usecase/GetFeedUseCaseTest.kt`
- `app/src/test/kotlin/com/carpoolapp/ui/home/HomeViewModelTest.kt`
- `app/src/test/kotlin/com/carpoolapp/domain/usecase/PublicarViajeUseCaseTest.kt`

Notes:
- Tests use MockK and kotlinx-coroutines-test. They mock suspend functions with `coEvery`/`coVerify`.
- `HomeViewModelTest` sets `Dispatchers.Main` to a `StandardTestDispatcher` inside tests to control coroutine execution.
