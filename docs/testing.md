# Testing Strategy

## Test Inventory

All tests are JVM unit tests (`app/src/test/`). No instrumented tests exist (`androidTest/` directory does not exist).

| File | What it covers | Dispatcher setup |
|---|---|---|
| `ActiveWorkoutViewModelTest` | Full ViewModel coverage: set completion, AMRAP, toggleSetDone, add/remove set, reorder, swap, finish/resume/cancel | `UnconfinedTestDispatcher` |
| `RoutinesViewModelTest` | ViewModel with mocked repository | `UnconfinedTestDispatcher` |
| `RestTimerPreferencesRepositoryTest` | Real DataStore + JUnit `TemporaryFolder` | None needed |
| `UserProfileRepositoryTest` | Real DataStore + JUnit `TemporaryFolder` | None needed |
| `ExerciseFilterTest` | Pure function test (`filterExercises`) | None needed |
| `FormatElapsedTimeTest` | Pure function test | None needed |

## Patterns

### ViewModel Tests
```kotlin
@Before fun setUp() {
    val testDispatcher = UnconfinedTestDispatcher()
    Dispatchers.setMain(testDispatcher)
}
@After fun tearDown() {
    Dispatchers.resetMain()
}
```

- Mock repositories with `mockk(relaxed = true)`
- Flow assertions via Turbine (`app.cash.turbine`)

### Repository Tests
- Use JUnit `TemporaryFolder` for isolated DataStore files
- Real DataStore instance, no mocking

### Pure Function Tests
- No dispatcher setup needed
- Direct assertion on return values

## Naming Convention
Use backtick strings for test names:
```kotlin
@Test fun `cancelWorkout resets state to defaults`()
@Test fun `toggleSetDone on AMRAP set does nothing`()
```

## What to Test
- **ViewModels**: state transitions, edge cases (empty lists, boundary conditions), coroutine flows
- **Repositories**: DataStore read/write, Room operations (if isolated)
- **Pure functions**: input → output mapping, edge cases

## What NOT to Test (currently)
- **Composables**: no Compose UI tests configured
- **Navigation**: no nav testing setup
- **Instrumented tests**: no `androidTest/` directory, no custom Hilt test runner

## Adding a New ViewModel Test
1. Create file at `app/src/test/java/.../ui/feature/<feature>/<Feature>ViewModelTest.kt`
2. Set up `UnconfinedTestDispatcher` in `@Before`/`@After`
3. Mock repository: `val repo = mockk<WorkoutRepository>(relaxed = true)`
4. Instantiate ViewModel: `val vm = FeatureViewModel(repo)`
5. Use Turbine for Flow assertions: `vm.someFlow.test { assertEquals(expected, awaitItem()) }`

## Hilt Note
`testInstrumentationRunner` is the plain `AndroidJUnitRunner`. Any future instrumented Hilt tests will need `HiltTestRunner` added to `build.gradle.kts`.
