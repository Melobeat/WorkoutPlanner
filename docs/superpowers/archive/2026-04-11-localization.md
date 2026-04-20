# Localization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract all hardcoded user-visible strings from ~21 Kotlin/Compose UI files into Android resource files for English (default) and German locales.

**Architecture:** Add `res/values/strings.xml` (already exists, extend it) and create `res/values-de/strings.xml`. Use `stringResource(R.string.<key>)` in all Compose composables. The one non-Composable function `getDateGroupLabel()` in `HistoryScreen.kt` uses plain Kotlin strings for group labels — these become parameters passed in from the Composable call site using `stringResource`.

**Tech Stack:** Android resource system, Compose `stringResource()`, no new dependencies

---

## File Map

| File | Action | String keys added |
|---|---|---|
| `app/src/main/res/values/strings.xml` | Extend (add all keys) | ~130 keys |
| `app/src/main/res/values-de/strings.xml` | Create (German translations) | ~130 keys |
| `app/src/main/java/de/melobeat/workoutplanner/MainActivity.kt` | Modify (3 strings) | nav_home, nav_history, minibar_resume |
| `app/src/main/java/de/melobeat/workoutplanner/ui/HomeScreen.kt` | Modify (~15 strings) | home_*, action_cancel |
| `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt` | Modify (~12 strings) | workout_* |
| `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseCard.kt` | Modify (~18 strings) | workout_* (shared), action_* |
| `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutSummaryScreen.kt` | Modify (~8 strings) | summary_* |
| `app/src/main/java/de/melobeat/workoutplanner/ui/HistoryScreen.kt` | Modify (~10 strings) | history_* |
| `app/src/main/java/de/melobeat/workoutplanner/ui/SettingsScreen.kt` | Modify (~15 strings) | settings_* |
| `app/src/main/java/de/melobeat/workoutplanner/ui/TimerSettingsScreen.kt` | Modify (~10 strings) | timer_* |
| `app/src/main/java/de/melobeat/workoutplanner/ui/ProfileScreen.kt` | Modify (~7 strings) | profile_* |
| `app/src/main/java/de/melobeat/workoutplanner/ui/ExercisesScreen.kt` | Modify (~15 strings) | exercises_* |
| `app/src/main/java/de/melobeat/workoutplanner/ui/EquipmentScreen.kt` | Modify (~12 strings) | equipment_* |
| `app/src/main/java/de/melobeat/workoutplanner/ui/RoutinesScreen.kt` | Modify (~10 strings) | routines_* |
| `app/src/main/java/de/melobeat/workoutplanner/ui/RoutineDetailScreen.kt` | Modify (~5 strings) | routines_* (shared) |
| `app/src/main/java/de/melobeat/workoutplanner/ui/CreateRoutineScreen.kt` | Modify (~8 strings) | create_routine_* |
| `app/src/main/java/de/melobeat/workoutplanner/ui/RoutineDayCard.kt` | Modify (~5 strings) | create_routine_* (shared) |
| `app/src/main/java/de/melobeat/workoutplanner/ui/RoutineExerciseEditItem.kt` | Modify (~6 strings) | create_routine_* (shared) |
| `app/src/main/java/de/melobeat/workoutplanner/ui/RoutineExercisePicker.kt` | Modify (~2 strings) | create_routine_select_exercise, action_cancel |
| `app/src/main/java/de/melobeat/workoutplanner/ui/RestTimerBanner.kt` | Modify (~5 strings) | rest_timer_* |
| `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseSelectionDialog.kt` | Modify (~1 string) | exercises_search_placeholder |
| `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutStepperCard.kt` | No change | (label comes from call site — already uses `stringResource` at call site after Task 3) |

---

## Task 1: Create Resource Files with All String Keys

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values-de/strings.xml`

- [ ] **Step 1: Write the complete English strings.xml**

Replace the entire file content:

```xml
<resources>
    <string name="app_name">Workout Planner</string>

    <!-- Shared / common -->
    <string name="action_cancel">Cancel</string>
    <string name="action_save">Save</string>
    <string name="action_delete">Delete</string>
    <string name="action_add">Add</string>
    <string name="action_back_cd">Back</string>
    <string name="unit_kg">kg</string>
    <string name="unit_reps">Reps</string>
    <string name="unit_seconds_suffix">s</string>
    <string name="label_amrap">AMRAP</string>

    <!-- Navigation -->
    <string name="nav_home">Home</string>
    <string name="nav_history">History</string>

    <!-- Mini-bar (MainActivity) -->
    <string name="minibar_resume">Resume</string>

    <!-- HomeScreen -->
    <string name="home_app_title">Workout Planner</string>
    <string name="home_settings_cd">Settings</string>
    <string name="home_routine_label">%1$s · DAY %2$d OF %3$d</string>
    <string name="home_exercises_count">%1$d exercises</string>
    <string name="home_more_exercises">+%1$d more</string>
    <string name="home_start_workout">▶ Start Workout</string>
    <string name="home_swap_day_cd">Swap Day</string>
    <string name="home_no_active_routine">No Active Routine</string>
    <string name="home_no_routine_hint">Select a routine to start tracking your progress.</string>
    <string name="home_manage_routines">Manage Routines</string>
    <string name="home_recent_workouts">RECENT WORKOUTS</string>
    <string name="home_choose_next_workout">Choose Next Workout</string>
    <string name="home_day_item_label">Day %1$d: %2$s</string>

    <!-- WorkoutScreen -->
    <string name="workout_minimize_cd">Minimize</string>
    <string name="workout_add_exercise">Exercise</string>
    <string name="workout_more_options_cd">More options</string>
    <string name="workout_menu_finish">Finish Workout</string>
    <string name="workout_menu_cancel">Cancel Workout</string>
    <string name="workout_exercise_counter">EXERCISE %1$d OF %2$d</string>
    <string name="workout_no_exercises">No exercises in this workout.</string>
    <string name="workout_dialog_add_exercise_title">Add Exercise</string>
    <string name="workout_dialog_swap_exercise_title">Swap Exercise</string>
    <string name="workout_cancel_dialog_title">Cancel Workout?</string>
    <string name="workout_cancel_dialog_body">All progress in this session will be lost.</string>
    <string name="workout_keep_going">Keep Going</string>

    <!-- ExerciseCard -->
    <string name="workout_set_active_label">SET %1$d — ACTIVE</string>
    <string name="workout_set_label">Set %1$d</string>
    <string name="workout_set_reps_weight">%1$s × %2$s kg</string>
    <string name="workout_done_cd">Done</string>
    <string name="workout_cta_finish">✓  Finish Workout</string>
    <string name="workout_cta_next_exercise">✓  Next Exercise</string>
    <string name="workout_cta_done_set">✓  Done — Set %1$d</string>
    <string name="workout_back_button">← Back</string>
    <string name="workout_skip_exercise">Skip Exercise →</string>
    <string name="workout_up_next">UP NEXT</string>
    <string name="workout_sets_done">%1$d sets done</string>
    <string name="workout_set_done_incomplete">Set %1$d done · Set %2$d incomplete</string>
    <string name="workout_sets_summary">%1$d sets · %2$s reps · %3$s kg</string>
    <string name="workout_completed_cd">Completed</string>
    <string name="workout_collapse_cd">Collapse</string>
    <string name="workout_expand_cd">Expand</string>
    <string name="workout_swap_exercise_cd">Swap exercise</string>
    <string name="workout_swap_button">Swap</string>
    <string name="workout_tap_to_jump">Tap any set to jump to it</string>

    <!-- WorkoutSummaryScreen -->
    <string name="summary_back_to_workout_cd">Back to workout</string>
    <string name="summary_total_volume">Total Volume</string>
    <string name="summary_total_volume_kg">%1$d kg</string>
    <string name="summary_set_label">Set %1$d</string>
    <string name="summary_skipped">Skipped</string>
    <string name="summary_reps_weight">%1$s reps × %2$s kg</string>
    <string name="summary_save_exit">Save &amp; Exit</string>
    <string name="summary_resume">Resume Workout</string>

    <!-- HistoryScreen -->
    <string name="history_title">History</string>
    <string name="history_empty_title">No workouts yet</string>
    <string name="history_empty_body">Complete a workout to see it here.</string>
    <string name="history_this_week">This Week</string>
    <string name="history_last_week">Last Week</string>
    <string name="history_more_exercises">+%1$d more exercises</string>
    <string name="history_set_reps_label">Set %1$d: %2$d%3$s reps</string>

    <!-- SettingsScreen -->
    <string name="settings_title">Settings</string>
    <string name="settings_back_cd">Back</string>
    <string name="settings_theme">Theme</string>
    <string name="settings_theme_dark">Dark</string>
    <string name="settings_theme_light">Light</string>
    <string name="settings_theme_system">System</string>
    <string name="settings_theme_follow_system">Follow system</string>
    <string name="settings_timer_title">Timer Settings</string>
    <string name="settings_timer_subtitle">Rest timer durations between sets and exercises</string>
    <string name="settings_profile_title">My Profile</string>
    <string name="settings_profile_subtitle">Age, height and body weight</string>
    <string name="settings_exercises_title">Manage Exercises</string>
    <string name="settings_exercises_subtitle">Add, edit or delete exercises</string>
    <string name="settings_equipment_title">Manage Equipment</string>
    <string name="settings_equipment_subtitle">Dumbbells, barbells, machines, etc.</string>
    <string name="settings_routines_title">Manage Routines</string>
    <string name="settings_routines_subtitle">Create and organize your workout routines</string>

    <!-- TimerSettingsScreen -->
    <string name="timer_settings_title">Timer Settings</string>
    <string name="timer_back_cd">Back</string>
    <string name="timer_easy_set_rest">Easy set rest</string>
    <string name="timer_easy_set_rest_subtitle">Notify when it\'s time for the next easy set</string>
    <string name="timer_hard_set_rest">Hard set rest</string>
    <string name="timer_hard_set_rest_subtitle">Notify when it\'s time for the next hard set</string>
    <string name="timer_between_exercises">Between exercises</string>
    <string name="timer_between_exercises_subtitle">Notify when it\'s time for the next exercise</string>
    <string name="timer_dialog_enter_duration">Enter duration in seconds:</string>
    <string name="timer_dialog_ok">OK</string>

    <!-- ProfileScreen -->
    <string name="profile_title">My Profile</string>
    <string name="profile_back_cd">Back</string>
    <string name="profile_personal_details">Personal Details</string>
    <string name="profile_age_label">Age (years)</string>
    <string name="profile_height_label">Height (cm)</string>
    <string name="profile_body_weight_label">Body weight (kg)</string>
    <string name="profile_save">Save Profile</string>

    <!-- ExercisesScreen -->
    <string name="exercises_title">Exercise Library</string>
    <string name="exercises_back_cd">Back</string>
    <string name="exercises_add_fab">Add Exercise</string>
    <string name="exercises_delete_cd">Delete Exercise</string>
    <string name="exercises_delete_dialog_title">Delete Exercise</string>
    <string name="exercises_delete_dialog_body">Are you sure you want to delete \'%1$s\'? This may affect existing routines.</string>
    <string name="exercises_add_dialog_title">Add New Exercise</string>
    <string name="exercises_edit_dialog_title">Edit Exercise</string>
    <string name="exercises_name_label">Exercise Name</string>
    <string name="exercises_muscle_label">Muscle Group</string>
    <string name="exercises_description_label">Description</string>
    <string name="exercises_bodyweight_label">Bodyweight exercise</string>
    <string name="exercises_equipment_label">Equipment</string>
    <string name="exercises_no_equipment">No Equipment</string>
    <string name="exercises_search_placeholder">Search…</string>

    <!-- EquipmentScreen -->
    <string name="equipment_title">Manage Equipment</string>
    <string name="equipment_back_cd">Back</string>
    <string name="equipment_add_fab">Add Equipment</string>
    <string name="equipment_delete_cd">Delete Equipment</string>
    <string name="equipment_bar_weight">Bar weight: %1$s kg</string>
    <string name="equipment_delete_dialog_title">Delete Equipment</string>
    <string name="equipment_delete_dialog_body">Are you sure you want to delete \'%1$s\'?</string>
    <string name="equipment_add_dialog_title">Add Equipment</string>
    <string name="equipment_edit_dialog_title">Edit Equipment</string>
    <string name="equipment_name_label">Equipment Name</string>
    <string name="equipment_bar_weight_label">Bar weight (kg, optional)</string>

    <!-- RoutinesScreen -->
    <string name="routines_title">Routines</string>
    <string name="routines_back_cd">Back</string>
    <string name="routines_new_fab">New Routine</string>
    <string name="routines_days_count">%1$d days</string>
    <string name="routines_set_active_cd">Set Active</string>
    <string name="routines_active_cd">Active</string>
    <string name="routines_delete_cd">Delete</string>
    <string name="routines_delete_dialog_title">Delete Routine?</string>
    <string name="routines_delete_dialog_body">"%1$s" will be permanently deleted.</string>

    <!-- RoutineDetailScreen -->
    <string name="routines_edit_cd">Edit Routine</string>

    <!-- CreateRoutineScreen / RoutineDayCard / RoutineExerciseEditItem -->
    <string name="create_routine_title_new">New Routine</string>
    <string name="create_routine_title_edit">Edit Routine</string>
    <string name="create_routine_save">Save</string>
    <string name="create_routine_name_label">Routine Name</string>
    <string name="create_routine_description_label">Description</string>
    <string name="create_routine_add_day">Add Day</string>
    <string name="create_routine_day_name_label">Day Name</string>
    <string name="create_routine_move_day_up_cd">Move Day Up</string>
    <string name="create_routine_move_day_down_cd">Move Day Down</string>
    <string name="create_routine_remove_day_cd">Remove Day</string>
    <string name="create_routine_add_exercise">Add Exercise</string>
    <string name="create_routine_move_exercise_up_cd">Move Exercise Up</string>
    <string name="create_routine_move_exercise_down_cd">Move Exercise Down</string>
    <string name="create_routine_remove_exercise_cd">Remove Exercise</string>
    <string name="create_routine_reps_label">Reps</string>
    <string name="create_routine_weight_label">Weight</string>
    <string name="create_routine_add_set">Add Set</string>
    <string name="create_routine_remove_set_cd">Remove Set</string>
    <string name="create_routine_select_exercise">Select Exercise</string>
    <string name="create_routine_back_cd">Back</string>

    <!-- RestTimerBanner -->
    <string name="rest_timer_label">REST</string>
    <string name="rest_timer_easy_milestone">EASY? TIME TO GO</string>
    <string name="rest_timer_hard_milestone">HARD? TIME TO GO</string>
    <string name="rest_timer_exercise_milestone">READY FOR NEXT EXERCISE?</string>
</resources>
```

- [ ] **Step 2: Create German strings.xml**

Create `app/src/main/res/values-de/strings.xml` (create the directory too):

```xml
<resources>
    <string name="app_name">Workout Planner</string>

    <!-- Shared / common -->
    <string name="action_cancel">Abbrechen</string>
    <string name="action_save">Speichern</string>
    <string name="action_delete">Löschen</string>
    <string name="action_add">Hinzufügen</string>
    <string name="action_back_cd">Zurück</string>
    <string name="unit_kg">kg</string>
    <string name="unit_reps">Wdh.</string>
    <string name="unit_seconds_suffix">s</string>
    <string name="label_amrap">AMRAP</string>

    <!-- Navigation -->
    <string name="nav_home">Home</string>
    <string name="nav_history">Verlauf</string>

    <!-- Mini-bar (MainActivity) -->
    <string name="minibar_resume">Weiter</string>

    <!-- HomeScreen -->
    <string name="home_app_title">Workout Planner</string>
    <string name="home_settings_cd">Einstellungen</string>
    <string name="home_routine_label">%1$s · TAG %2$d VON %3$d</string>
    <string name="home_exercises_count">%1$d Übungen</string>
    <string name="home_more_exercises">+%1$d weitere</string>
    <string name="home_start_workout">▶ Workout starten</string>
    <string name="home_swap_day_cd">Tag wechseln</string>
    <string name="home_no_active_routine">Kein aktives Programm</string>
    <string name="home_no_routine_hint">Wähle ein Programm, um deinen Fortschritt zu verfolgen.</string>
    <string name="home_manage_routines">Programme verwalten</string>
    <string name="home_recent_workouts">LETZTE WORKOUTS</string>
    <string name="home_choose_next_workout">Nächstes Workout wählen</string>
    <string name="home_day_item_label">Tag %1$d: %2$s</string>

    <!-- WorkoutScreen -->
    <string name="workout_minimize_cd">Minimieren</string>
    <string name="workout_add_exercise">Übung</string>
    <string name="workout_more_options_cd">Weitere Optionen</string>
    <string name="workout_menu_finish">Workout beenden</string>
    <string name="workout_menu_cancel">Workout abbrechen</string>
    <string name="workout_exercise_counter">ÜBUNG %1$d VON %2$d</string>
    <string name="workout_no_exercises">Keine Übungen in diesem Workout.</string>
    <string name="workout_dialog_add_exercise_title">Übung hinzufügen</string>
    <string name="workout_dialog_swap_exercise_title">Übung tauschen</string>
    <string name="workout_cancel_dialog_title">Workout abbrechen?</string>
    <string name="workout_cancel_dialog_body">Alle Fortschritte dieser Einheit gehen verloren.</string>
    <string name="workout_keep_going">Weiter machen</string>

    <!-- ExerciseCard -->
    <string name="workout_set_active_label">SATZ %1$d — AKTIV</string>
    <string name="workout_set_label">Satz %1$d</string>
    <string name="workout_set_reps_weight">%1$s × %2$s kg</string>
    <string name="workout_done_cd">Erledigt</string>
    <string name="workout_cta_finish">✓  Workout beenden</string>
    <string name="workout_cta_next_exercise">✓  Nächste Übung</string>
    <string name="workout_cta_done_set">✓  Fertig — Satz %1$d</string>
    <string name="workout_back_button">← Zurück</string>
    <string name="workout_skip_exercise">Übung überspringen →</string>
    <string name="workout_up_next">ALS NÄCHSTES</string>
    <string name="workout_sets_done">%1$d Sätze erledigt</string>
    <string name="workout_set_done_incomplete">Satz %1$d erledigt · Satz %2$d unvollständig</string>
    <string name="workout_sets_summary">%1$d Sätze · %2$s Wdh. · %3$s kg</string>
    <string name="workout_completed_cd">Abgeschlossen</string>
    <string name="workout_collapse_cd">Einklappen</string>
    <string name="workout_expand_cd">Ausklappen</string>
    <string name="workout_swap_exercise_cd">Übung tauschen</string>
    <string name="workout_swap_button">Tauschen</string>
    <string name="workout_tap_to_jump">Tippe auf einen Satz, um zu springen</string>

    <!-- WorkoutSummaryScreen -->
    <string name="summary_back_to_workout_cd">Zurück zum Workout</string>
    <string name="summary_total_volume">Gesamtvolumen</string>
    <string name="summary_total_volume_kg">%1$d kg</string>
    <string name="summary_set_label">Satz %1$d</string>
    <string name="summary_skipped">Übersprungen</string>
    <string name="summary_reps_weight">%1$s Wdh. × %2$s kg</string>
    <string name="summary_save_exit">Speichern &amp; Beenden</string>
    <string name="summary_resume">Workout fortsetzen</string>

    <!-- HistoryScreen -->
    <string name="history_title">Verlauf</string>
    <string name="history_empty_title">Noch keine Workouts</string>
    <string name="history_empty_body">Schließe ein Workout ab, um es hier zu sehen.</string>
    <string name="history_this_week">Diese Woche</string>
    <string name="history_last_week">Letzte Woche</string>
    <string name="history_more_exercises">+%1$d weitere Übungen</string>
    <string name="history_set_reps_label">Satz %1$d: %2$d%3$s Wdh.</string>

    <!-- SettingsScreen -->
    <string name="settings_title">Einstellungen</string>
    <string name="settings_back_cd">Zurück</string>
    <string name="settings_theme">Design</string>
    <string name="settings_theme_dark">Dunkel</string>
    <string name="settings_theme_light">Hell</string>
    <string name="settings_theme_system">System</string>
    <string name="settings_theme_follow_system">System folgen</string>
    <string name="settings_timer_title">Timer-Einstellungen</string>
    <string name="settings_timer_subtitle">Pausenzeiten zwischen Sätzen und Übungen</string>
    <string name="settings_profile_title">Mein Profil</string>
    <string name="settings_profile_subtitle">Alter, Größe und Körpergewicht</string>
    <string name="settings_exercises_title">Übungen verwalten</string>
    <string name="settings_exercises_subtitle">Übungen hinzufügen, bearbeiten oder löschen</string>
    <string name="settings_equipment_title">Equipment verwalten</string>
    <string name="settings_equipment_subtitle">Kurzhanteln, Langhantel, Maschinen usw.</string>
    <string name="settings_routines_title">Programme verwalten</string>
    <string name="settings_routines_subtitle">Trainingsprogramme erstellen und verwalten</string>

    <!-- TimerSettingsScreen -->
    <string name="timer_settings_title">Timer-Einstellungen</string>
    <string name="timer_back_cd">Zurück</string>
    <string name="timer_easy_set_rest">Leichte Satzpause</string>
    <string name="timer_easy_set_rest_subtitle">Benachrichtigen, wenn es Zeit für den nächsten leichten Satz ist</string>
    <string name="timer_hard_set_rest">Schwere Satzpause</string>
    <string name="timer_hard_set_rest_subtitle">Benachrichtigen, wenn es Zeit für den nächsten schweren Satz ist</string>
    <string name="timer_between_exercises">Zwischen Übungen</string>
    <string name="timer_between_exercises_subtitle">Benachrichtigen, wenn es Zeit für die nächste Übung ist</string>
    <string name="timer_dialog_enter_duration">Dauer in Sekunden eingeben:</string>
    <string name="timer_dialog_ok">OK</string>

    <!-- ProfileScreen -->
    <string name="profile_title">Mein Profil</string>
    <string name="profile_back_cd">Zurück</string>
    <string name="profile_personal_details">Persönliche Daten</string>
    <string name="profile_age_label">Alter (Jahre)</string>
    <string name="profile_height_label">Größe (cm)</string>
    <string name="profile_body_weight_label">Körpergewicht (kg)</string>
    <string name="profile_save">Profil speichern</string>

    <!-- ExercisesScreen -->
    <string name="exercises_title">Übungsbibliothek</string>
    <string name="exercises_back_cd">Zurück</string>
    <string name="exercises_add_fab">Übung hinzufügen</string>
    <string name="exercises_delete_cd">Übung löschen</string>
    <string name="exercises_delete_dialog_title">Übung löschen</string>
    <string name="exercises_delete_dialog_body">\'%1$s\' wirklich löschen? Dies kann bestehende Programme beeinflussen.</string>
    <string name="exercises_add_dialog_title">Neue Übung hinzufügen</string>
    <string name="exercises_edit_dialog_title">Übung bearbeiten</string>
    <string name="exercises_name_label">Übungsname</string>
    <string name="exercises_muscle_label">Muskelgruppe</string>
    <string name="exercises_description_label">Beschreibung</string>
    <string name="exercises_bodyweight_label">Körpergewichtsübung</string>
    <string name="exercises_equipment_label">Equipment</string>
    <string name="exercises_no_equipment">Kein Equipment</string>
    <string name="exercises_search_placeholder">Suchen…</string>

    <!-- EquipmentScreen -->
    <string name="equipment_title">Equipment verwalten</string>
    <string name="equipment_back_cd">Zurück</string>
    <string name="equipment_add_fab">Equipment hinzufügen</string>
    <string name="equipment_delete_cd">Equipment löschen</string>
    <string name="equipment_bar_weight">Stangengewicht: %1$s kg</string>
    <string name="equipment_delete_dialog_title">Equipment löschen</string>
    <string name="equipment_delete_dialog_body">\'%1$s\' wirklich löschen?</string>
    <string name="equipment_add_dialog_title">Equipment hinzufügen</string>
    <string name="equipment_edit_dialog_title">Equipment bearbeiten</string>
    <string name="equipment_name_label">Equipment-Name</string>
    <string name="equipment_bar_weight_label">Stangengewicht (kg, optional)</string>

    <!-- RoutinesScreen -->
    <string name="routines_title">Programme</string>
    <string name="routines_back_cd">Zurück</string>
    <string name="routines_new_fab">Neues Programm</string>
    <string name="routines_days_count">%1$d Tage</string>
    <string name="routines_set_active_cd">Aktivieren</string>
    <string name="routines_active_cd">Aktiv</string>
    <string name="routines_delete_cd">Löschen</string>
    <string name="routines_delete_dialog_title">Programm löschen?</string>
    <string name="routines_delete_dialog_body">"%1$s" wird dauerhaft gelöscht.</string>

    <!-- RoutineDetailScreen -->
    <string name="routines_edit_cd">Programm bearbeiten</string>

    <!-- CreateRoutineScreen / RoutineDayCard / RoutineExerciseEditItem -->
    <string name="create_routine_title_new">Neues Programm</string>
    <string name="create_routine_title_edit">Programm bearbeiten</string>
    <string name="create_routine_save">Speichern</string>
    <string name="create_routine_name_label">Programmname</string>
    <string name="create_routine_description_label">Beschreibung</string>
    <string name="create_routine_add_day">Tag hinzufügen</string>
    <string name="create_routine_day_name_label">Tagesname</string>
    <string name="create_routine_move_day_up_cd">Tag nach oben</string>
    <string name="create_routine_move_day_down_cd">Tag nach unten</string>
    <string name="create_routine_remove_day_cd">Tag entfernen</string>
    <string name="create_routine_add_exercise">Übung hinzufügen</string>
    <string name="create_routine_move_exercise_up_cd">Übung nach oben</string>
    <string name="create_routine_move_exercise_down_cd">Übung nach unten</string>
    <string name="create_routine_remove_exercise_cd">Übung entfernen</string>
    <string name="create_routine_reps_label">Wdh.</string>
    <string name="create_routine_weight_label">Gewicht</string>
    <string name="create_routine_add_set">Satz hinzufügen</string>
    <string name="create_routine_remove_set_cd">Satz entfernen</string>
    <string name="create_routine_select_exercise">Übung auswählen</string>
    <string name="create_routine_back_cd">Zurück</string>

    <!-- RestTimerBanner -->
    <string name="rest_timer_label">PAUSE</string>
    <string name="rest_timer_easy_milestone">LEICHT? JETZT STARTEN</string>
    <string name="rest_timer_hard_milestone">SCHWER? JETZT STARTEN</string>
    <string name="rest_timer_exercise_milestone">BEREIT FÜR DIE NÄCHSTE ÜBUNG?</string>
</resources>
```

- [ ] **Step 3: Build to verify no compilation errors from new resource file**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:generateDebugResources 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL (resource generation only, no code changes yet)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "feat(i18n): add strings.xml resource files for EN and DE"
```

---

## Task 2: Localize MainActivity

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/MainActivity.kt`

- [ ] **Step 1: Add import and update strings in MainActivity**

The file uses 3 hardcoded strings. Replace them:

In `MainActivity.kt`, find the `WorkoutPlannerApp()` composable.

Change:
```kotlin
icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
label = { Text("Home") },
```
To:
```kotlin
icon = { Icon(Icons.Rounded.Home, contentDescription = stringResource(R.string.nav_home)) },
label = { Text(stringResource(R.string.nav_home)) },
```

Change:
```kotlin
icon = { Icon(Icons.Rounded.History, contentDescription = "History") },
label = { Text("History") },
```
To:
```kotlin
icon = { Icon(Icons.Rounded.History, contentDescription = stringResource(R.string.nav_history)) },
label = { Text(stringResource(R.string.nav_history)) },
```

Change:
```kotlin
Text("Resume")
```
To:
```kotlin
Text(stringResource(R.string.minibar_resume))
```

Add import at the top of the file (after existing imports):
```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
```

- [ ] **Step 2: Build to verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/MainActivity.kt
git commit -m "feat(i18n): localize MainActivity nav labels and mini-bar"
```

---

## Task 3: Localize HomeScreen

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/HomeScreen.kt`

- [ ] **Step 1: Add import**

Add at the top of `HomeScreen.kt`:
```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
```

- [ ] **Step 2: Update hardcoded strings in HomeScreen**

In `HomeScreenContent`, replace each hardcoded string:

| Old | New |
|---|---|
| `"Workout Planner"` (Text in hero) | `stringResource(R.string.home_app_title)` |
| `contentDescription = "Settings"` | `contentDescription = stringResource(R.string.home_settings_cd)` |
| `"${routine.name.uppercase()} · DAY ${nextDayIndex + 1} OF ${routine.workoutDays.size}"` | `stringResource(R.string.home_routine_label, routine.name.uppercase(), nextDayIndex + 1, routine.workoutDays.size)` |
| `"${nextDay.exercises.size} exercises"` | `stringResource(R.string.home_exercises_count, nextDay.exercises.size)` |
| `"+${nextDay.exercises.size - 2} more"` | `stringResource(R.string.home_more_exercises, nextDay.exercises.size - 2)` |
| `"▶ Start Workout"` | `stringResource(R.string.home_start_workout)` |
| `contentDescription = "Swap Day"` | `contentDescription = stringResource(R.string.home_swap_day_cd)` |
| `"No Active Routine"` | `stringResource(R.string.home_no_active_routine)` |
| `"Select a routine to start tracking your progress."` | `stringResource(R.string.home_no_routine_hint)` |
| `"Manage Routines"` | `stringResource(R.string.home_manage_routines)` |
| `"RECENT WORKOUTS"` | `stringResource(R.string.home_recent_workouts)` |

In `WorkoutDayChooserDialog`, replace:

| Old | New |
|---|---|
| `title = { Text("Choose Next Workout") }` | `title = { Text(stringResource(R.string.home_choose_next_workout)) }` |
| `"Day ${index + 1}: ${day.name}"` | `stringResource(R.string.home_day_item_label, index + 1, day.name)` |
| `TextButton(onClick = onDismiss) { Text("Cancel") }` | `TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }` |

- [ ] **Step 3: Build to verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/HomeScreen.kt
git commit -m "feat(i18n): localize HomeScreen"
```

---

## Task 4: Localize WorkoutScreen

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt`

- [ ] **Step 1: Add import**

```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
```

- [ ] **Step 2: Update hardcoded strings in WorkoutScreenContent**

| Old | New |
|---|---|
| `contentDescription = "Minimize"` | `contentDescription = stringResource(R.string.workout_minimize_cd)` |
| `Text("Exercise", ...)` (FAB label) | `Text(stringResource(R.string.workout_add_exercise), ...)` |
| `contentDescription = "More options"` | `contentDescription = stringResource(R.string.workout_more_options_cd)` |
| `Text("Finish Workout")` (dropdown) | `Text(stringResource(R.string.workout_menu_finish))` |
| `Text("Cancel Workout", color = ...)` (dropdown) | `Text(stringResource(R.string.workout_menu_cancel), color = ...)` |
| `"EXERCISE ${ei + 1} OF ${exercises.size}"` | `stringResource(R.string.workout_exercise_counter, ei + 1, exercises.size)` |
| `Text("No exercises in this workout.")` | `Text(stringResource(R.string.workout_no_exercises))` |
| `title = "Add Exercise"` (ExerciseSelectionDialog) | `title = stringResource(R.string.workout_dialog_add_exercise_title)` |
| `title = "Swap Exercise"` (ExerciseSelectionDialog) | `title = stringResource(R.string.workout_dialog_swap_exercise_title)` |
| `title = { Text("Cancel Workout?") }` (AlertDialog) | `title = { Text(stringResource(R.string.workout_cancel_dialog_title)) }` |
| `text = { Text("All progress in this session will be lost.") }` | `text = { Text(stringResource(R.string.workout_cancel_dialog_body)) }` |
| `Text("Cancel Workout", color = MaterialTheme.colorScheme.error)` (confirm button) | `Text(stringResource(R.string.workout_menu_cancel), color = MaterialTheme.colorScheme.error)` |
| `Text("Keep Going")` | `Text(stringResource(R.string.workout_keep_going))` |

- [ ] **Step 3: Build to verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt
git commit -m "feat(i18n): localize WorkoutScreen"
```

---

## Task 5: Localize ExerciseCard

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseCard.kt`

- [ ] **Step 1: Add import**

```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
```

- [ ] **Step 2: Update ActiveCardHeader**

| Old | New |
|---|---|
| `"EXERCISE ${exerciseIndex + 1} OF $totalExercises"` | `stringResource(R.string.workout_exercise_counter, exerciseIndex + 1, totalExercises)` |
| `contentDescription = "Swap exercise"` | `contentDescription = stringResource(R.string.workout_swap_exercise_cd)` |
| `Text("Swap", ...)` | `Text(stringResource(R.string.workout_swap_button), ...)` |

- [ ] **Step 3: Update active set section in ExerciseCard**

In the active card body (`isActive` branch):

| Old | New |
|---|---|
| `"SET ${si + 1} — ACTIVE"` | `stringResource(R.string.workout_set_active_label, si + 1)` |
| `StepperCard(label = "Reps", ...)` | `StepperCard(label = stringResource(R.string.unit_reps), ...)` |
| `StepperCard(label = "kg", ...)` | `StepperCard(label = stringResource(R.string.unit_kg), ...)` |
| `"✓  Finish Workout"` | `stringResource(R.string.workout_cta_finish)` |
| `"✓  Next Exercise"` | `stringResource(R.string.workout_cta_next_exercise)` |
| `"✓  Done — Set ${si + 2}"` | `stringResource(R.string.workout_cta_done_set, si + 2)` |
| `Text("← Back", ...)` | `Text(stringResource(R.string.workout_back_button), ...)` |
| `Text("Skip Exercise →", ...)` | `Text(stringResource(R.string.workout_skip_exercise), ...)` |

In the active card's completed set row and pending set row:

| Old | New |
|---|---|
| `"Set ${si + 1}"` (completed, in active card) | `stringResource(R.string.workout_set_label, si + 1)` |
| `"${set.reps} × ${set.weight} kg"` (completed) | `stringResource(R.string.workout_set_reps_weight, set.reps, set.weight)` |
| `contentDescription = "Done"` | `contentDescription = stringResource(R.string.workout_done_cd)` |
| `"Set ${si + 1}"` (pending, in active card) | `stringResource(R.string.workout_set_label, si + 1)` |
| `"${set.reps} × ${set.weight} kg"` (pending) | `stringResource(R.string.workout_set_reps_weight, set.reps, set.weight)` |

- [ ] **Step 4: Update non-active card section**

In the non-active card (`else` branch):

| Old | New |
|---|---|
| `"UP NEXT"` | `stringResource(R.string.workout_up_next)` |
| `"${exercise.sets.size} sets done"` (allDone subtitle) | `stringResource(R.string.workout_sets_done, exercise.sets.size)` |
| `"Set $doneCount done · Set ${doneCount + 1} incomplete"` | `stringResource(R.string.workout_set_done_incomplete, doneCount, doneCount + 1)` |
| `"${exercise.sets.size} sets · ${first?.reps ?: "0"} reps · ${first?.weight ?: "0"} kg"` | `stringResource(R.string.workout_sets_summary, exercise.sets.size, first?.reps ?: "0", first?.weight ?: "0")` |
| `contentDescription = "Completed"` | `contentDescription = stringResource(R.string.workout_completed_cd)` |
| `contentDescription = if (exercise.isExpanded) "Collapse" else "Expand"` | `contentDescription = if (exercise.isExpanded) stringResource(R.string.workout_collapse_cd) else stringResource(R.string.workout_expand_cd)` |
| `"Set ${si + 1}"` (done row, non-active) | `stringResource(R.string.workout_set_label, si + 1)` |
| `"${set.reps} × ${set.weight} kg"` (done row) | `stringResource(R.string.workout_set_reps_weight, set.reps, set.weight)` |
| `contentDescription = "Done"` (non-active done row) | `contentDescription = stringResource(R.string.workout_done_cd)` |
| `"Set ${si + 1}"` (pending row, non-active) | `stringResource(R.string.workout_set_label, si + 1)` |
| `"${set.reps} × ${set.weight} kg"` (pending row) | `stringResource(R.string.workout_set_reps_weight, set.reps, set.weight)` |
| `"Tap any set to jump to it"` | `stringResource(R.string.workout_tap_to_jump)` |

- [ ] **Step 5: Build to verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseCard.kt
git commit -m "feat(i18n): localize ExerciseCard"
```

---

## Task 6: Localize WorkoutSummaryScreen

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutSummaryScreen.kt`

- [ ] **Step 1: Add import**

```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
```

- [ ] **Step 2: Update hardcoded strings**

| Old | New |
|---|---|
| `contentDescription = "Back to workout"` | `contentDescription = stringResource(R.string.summary_back_to_workout_cd)` |
| `Text("Total Volume", ...)` | `Text(stringResource(R.string.summary_total_volume), ...)` |
| `"${totalVolumeKg.toInt()} kg"` | `stringResource(R.string.summary_total_volume_kg, totalVolumeKg.toInt())` |
| `"Set ${index + 1}"` | `stringResource(R.string.summary_set_label, index + 1)` |
| `Text("AMRAP", ...)` | `Text(stringResource(R.string.label_amrap), ...)` |
| `Text("Skipped", ...)` | `Text(stringResource(R.string.summary_skipped), ...)` |
| `"${set.reps} reps × ${set.weight} kg"` | `stringResource(R.string.summary_reps_weight, set.reps, set.weight)` |
| `Text("Save & Exit", ...)` | `Text(stringResource(R.string.summary_save_exit), ...)` |
| `Text("Resume Workout")` | `Text(stringResource(R.string.summary_resume))` |

- [ ] **Step 3: Build to verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutSummaryScreen.kt
git commit -m "feat(i18n): localize WorkoutSummaryScreen"
```

---

## Task 7: Localize HistoryScreen

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/HistoryScreen.kt`

This file has a special case: `getDateGroupLabel()` is a plain Kotlin function (not a Composable) that returns `"This Week"` and `"Last Week"` as plain strings. These strings are used as group keys for sorting. The fix is:

1. Keep `getDateGroupLabel()` returning English strings (used as sort keys — not translated)
2. Add a new composable-context helper or do the translation inline at the display call site using `when(groupLabel)`

The display call is:
```kotlin
groupLabel.uppercase()  // used in Text(...)
```

The groupLabel is used both as a sort key AND as display text. To keep the sort logic intact, translate only at display time using a `when` expression.

- [ ] **Step 1: Add import**

```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
```

- [ ] **Step 2: Update `HistoryScreenContent` — group label display**

Find the `Text(groupLabel.uppercase(), ...)` call inside the `LazyColumn`. Replace:

```kotlin
Text(
    groupLabel.uppercase(),
    ...
)
```

With:

```kotlin
val localizedGroupLabel = when (groupLabel) {
    "This Week" -> stringResource(R.string.history_this_week)
    "Last Week" -> stringResource(R.string.history_last_week)
    else -> groupLabel // Month/Year strings (e.g. "April 2026") — already locale-aware via SimpleDateFormat(Locale.getDefault())
}
Text(
    localizedGroupLabel.uppercase(java.util.Locale.getDefault()),
    ...
)
```

- [ ] **Step 3: Update empty state strings**

| Old | New |
|---|---|
| `title = { Text("History", ...) }` (LargeTopAppBar) | `title = { Text(stringResource(R.string.history_title), ...) }` |
| `Text("No workouts yet", ...)` | `Text(stringResource(R.string.history_empty_title), ...)` |
| `Text("Complete a workout to see it here.", ...)` | `Text(stringResource(R.string.history_empty_body), ...)` |

- [ ] **Step 4: Update `HistorySessionCard` — more exercises string**

| Old | New |
|---|---|
| `"+${displayEntries.size - 3} more exercises"` | `stringResource(R.string.history_more_exercises, displayEntries.size - 3)` |

- [ ] **Step 5: Update `WorkoutSessionCard` — set/reps display**

In `WorkoutSessionCard`, the line:
```kotlin
"Set ${set.sets}: ${set.reps}${if (set.isAmrap) "+" else ""} reps"
```

Replace with:
```kotlin
stringResource(R.string.history_set_reps_label, set.sets, set.reps, if (set.isAmrap) "+" else "")
```

Also update the fallback "Unknown Exercise" string:
```kotlin
val exerciseName = exerciseNameMap[exerciseId] ?: "Unknown Exercise"
```
This is a data fallback, not user-displayed UI text, but since it appears in the UI — leave it in English (it is a defensive fallback that should rarely appear in production). No key needed.

- [ ] **Step 6: Build to verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/HistoryScreen.kt
git commit -m "feat(i18n): localize HistoryScreen"
```

---

## Task 8: Localize SettingsScreen

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/SettingsScreen.kt`

- [ ] **Step 1: Add import**

```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
```

- [ ] **Step 2: Update all strings**

In `SettingsScreen`:

| Old | New |
|---|---|
| `title = { Text("Settings", ...) }` | `title = { Text(stringResource(R.string.settings_title), ...) }` |
| `contentDescription = "Back"` | `contentDescription = stringResource(R.string.settings_back_cd)` |
| `headlineContent = { Text("Theme", ...) }` | `headlineContent = { Text(stringResource(R.string.settings_theme), ...) }` |
| `"Light"` (theme label) | `stringResource(R.string.settings_theme_light)` |
| `"Follow system"` | `stringResource(R.string.settings_theme_follow_system)` |
| `"Dark"` (theme label) | `stringResource(R.string.settings_theme_dark)` |

For the `SingleChoiceSegmentedButtonRow`, the list:
```kotlin
listOf("dark" to "Dark", "light" to "Light", "system" to "System")
```
Replace with:
```kotlin
listOf(
    "dark"   to stringResource(R.string.settings_theme_dark),
    "light"  to stringResource(R.string.settings_theme_light),
    "system" to stringResource(R.string.settings_theme_system)
)
```

And the supporting text `when` block:
```kotlin
val label = when (themeMode) {
    "light"  -> "Light"
    "system" -> "Follow system"
    else     -> "Dark"
}
```
Replace with:
```kotlin
val label = when (themeMode) {
    "light"  -> stringResource(R.string.settings_theme_light)
    "system" -> stringResource(R.string.settings_theme_follow_system)
    else     -> stringResource(R.string.settings_theme_dark)
}
```

Replace `SettingsListItem` calls:
```kotlin
SettingsListItem(
    title = "Timer Settings",
    subtitle = "Rest timer durations between sets and exercises",
    ...
)
```
→
```kotlin
SettingsListItem(
    title = stringResource(R.string.settings_timer_title),
    subtitle = stringResource(R.string.settings_timer_subtitle),
    ...
)
```

```kotlin
SettingsListItem(
    title = "My Profile",
    subtitle = "Age, height and body weight",
    ...
)
```
→
```kotlin
SettingsListItem(
    title = stringResource(R.string.settings_profile_title),
    subtitle = stringResource(R.string.settings_profile_subtitle),
    ...
)
```

```kotlin
SettingsListItem(
    title = "Manage Exercises",
    subtitle = "Add, edit or delete exercises",
    ...
)
```
→
```kotlin
SettingsListItem(
    title = stringResource(R.string.settings_exercises_title),
    subtitle = stringResource(R.string.settings_exercises_subtitle),
    ...
)
```

```kotlin
SettingsListItem(
    title = "Manage Equipment",
    subtitle = "Dumbbells, barbells, machines, etc.",
    ...
)
```
→
```kotlin
SettingsListItem(
    title = stringResource(R.string.settings_equipment_title),
    subtitle = stringResource(R.string.settings_equipment_subtitle),
    ...
)
```

```kotlin
SettingsListItem(
    title = "Manage Routines",
    subtitle = "Create and organize your workout routines",
    ...
)
```
→
```kotlin
SettingsListItem(
    title = stringResource(R.string.settings_routines_title),
    subtitle = stringResource(R.string.settings_routines_subtitle),
    ...
)
```

- [ ] **Step 3: Build to verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/SettingsScreen.kt
git commit -m "feat(i18n): localize SettingsScreen"
```

---

## Task 9: Localize TimerSettingsScreen

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/TimerSettingsScreen.kt`

- [ ] **Step 1: Add import**

```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
```

- [ ] **Step 2: Update strings in TimerSettingsScreen**

| Old | New |
|---|---|
| `title = { Text("Timer Settings", ...) }` | `title = { Text(stringResource(R.string.timer_settings_title), ...) }` |
| `contentDescription = "Back"` | `contentDescription = stringResource(R.string.timer_back_cd)` |
| `TimerSettingRow(title = "Easy set rest", subtitle = "Notify when it's time for the next easy set", ...)` | `TimerSettingRow(title = stringResource(R.string.timer_easy_set_rest), subtitle = stringResource(R.string.timer_easy_set_rest_subtitle), ...)` |
| `TimerSettingRow(title = "Hard set rest", subtitle = "Notify when it's time for the next hard set", ...)` | `TimerSettingRow(title = stringResource(R.string.timer_hard_set_rest), subtitle = stringResource(R.string.timer_hard_set_rest_subtitle), ...)` |
| `TimerSettingRow(title = "Between exercises", subtitle = "Notify when it's time for the next exercise", ...)` | `TimerSettingRow(title = stringResource(R.string.timer_between_exercises), subtitle = stringResource(R.string.timer_between_exercises_subtitle), ...)` |

In `TimerEditDialog`:

| Old | New |
|---|---|
| `Text("Enter duration in seconds:")` | `Text(stringResource(R.string.timer_dialog_enter_duration))` |
| `suffix = { Text("s") }` | `suffix = { Text(stringResource(R.string.unit_seconds_suffix)) }` |
| `Text("OK")` | `Text(stringResource(R.string.timer_dialog_ok))` |
| `Text("Cancel")` | `Text(stringResource(R.string.action_cancel))` |

- [ ] **Step 3: Build to verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/TimerSettingsScreen.kt
git commit -m "feat(i18n): localize TimerSettingsScreen"
```

---

## Task 10: Localize ProfileScreen

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ProfileScreen.kt`

- [ ] **Step 1: Add import**

```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
```

- [ ] **Step 2: Update strings**

| Old | New |
|---|---|
| `title = { Text("My Profile", ...) }` | `title = { Text(stringResource(R.string.profile_title), ...) }` |
| `contentDescription = "Back"` | `contentDescription = stringResource(R.string.profile_back_cd)` |
| `Text("Personal Details", ...)` | `Text(stringResource(R.string.profile_personal_details), ...)` |
| `label = { Text("Age (years)") }` | `label = { Text(stringResource(R.string.profile_age_label)) }` |
| `label = { Text("Height (cm)") }` | `label = { Text(stringResource(R.string.profile_height_label)) }` |
| `label = { Text("Body weight (kg)") }` | `label = { Text(stringResource(R.string.profile_body_weight_label)) }` |
| `Text("Save Profile", ...)` | `Text(stringResource(R.string.profile_save), ...)` |

- [ ] **Step 3: Build to verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/ProfileScreen.kt
git commit -m "feat(i18n): localize ProfileScreen"
```

---

## Task 11: Localize ExercisesScreen

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ExercisesScreen.kt`

- [ ] **Step 1: Add import**

```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
```

- [ ] **Step 2: Update strings in ExercisesScreenContent**

| Old | New |
|---|---|
| `title = { Text("Exercise Library", ...) }` | `title = { Text(stringResource(R.string.exercises_title), ...) }` |
| `contentDescription = "Back"` (navigationIcon) | `contentDescription = stringResource(R.string.exercises_back_cd)` |
| `text = { Text("Add Exercise") }` (FAB) | `text = { Text(stringResource(R.string.exercises_add_fab)) }` |
| `contentDescription = "Delete Exercise"` (list item trailing) | `contentDescription = stringResource(R.string.exercises_delete_cd)` |
| `title = { Text("Delete Exercise") }` (AlertDialog) | `title = { Text(stringResource(R.string.exercises_delete_dialog_title)) }` |
| `text = { Text("Are you sure you want to delete '${exerciseToDelete?.name}'? This may affect existing routines.") }` | `text = { Text(stringResource(R.string.exercises_delete_dialog_body, exerciseToDelete?.name ?: "")) }` |
| `Text("Delete")` (confirm button) | `Text(stringResource(R.string.action_delete))` |
| `Text("Cancel")` (dismiss button) | `Text(stringResource(R.string.action_cancel))` |

In `AddExerciseDialog`:

| Old | New |
|---|---|
| `title = { Text(if (initialExercise == null) "Add New Exercise" else "Edit Exercise") }` | `title = { Text(if (initialExercise == null) stringResource(R.string.exercises_add_dialog_title) else stringResource(R.string.exercises_edit_dialog_title)) }` |
| `label = { Text("Exercise Name") }` | `label = { Text(stringResource(R.string.exercises_name_label)) }` |
| `label = { Text("Muscle Group") }` | `label = { Text(stringResource(R.string.exercises_muscle_label)) }` |
| `label = { Text("Description") }` | `label = { Text(stringResource(R.string.exercises_description_label)) }` |
| `Text("Bodyweight exercise", ...)` | `Text(stringResource(R.string.exercises_bodyweight_label), ...)` |
| `Text("Equipment", ...)` | `Text(stringResource(R.string.exercises_equipment_label), ...)` |
| `equipmentList.find {...}?.name ?: "No Equipment"` | `equipmentList.find {...}?.name ?: stringResource(R.string.exercises_no_equipment)` |
| `DropdownMenuItem(text = { Text("No Equipment") }, ...)` | `DropdownMenuItem(text = { Text(stringResource(R.string.exercises_no_equipment)) }, ...)` |
| `Text(if (initialExercise == null) "Add" else "Save")` (confirm button) | `Text(if (initialExercise == null) stringResource(R.string.action_add) else stringResource(R.string.action_save))` |
| `Text("Cancel")` (dismiss button) | `Text(stringResource(R.string.action_cancel))` |

Also update `ExerciseLibraryItem`:

| Old | New |
|---|---|
| `contentDescription = "Delete Exercise"` | `contentDescription = stringResource(R.string.exercises_delete_cd)` |

- [ ] **Step 3: Build to verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/ExercisesScreen.kt
git commit -m "feat(i18n): localize ExercisesScreen"
```

---

## Task 12: Localize EquipmentScreen

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/EquipmentScreen.kt`

- [ ] **Step 1: Add import**

```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
```

- [ ] **Step 2: Update strings in EquipmentScreenContent**

| Old | New |
|---|---|
| `title = { Text("Manage Equipment", ...) }` | `title = { Text(stringResource(R.string.equipment_title), ...) }` |
| `contentDescription = "Back"` | `contentDescription = stringResource(R.string.equipment_back_cd)` |
| `text = { Text("Add Equipment") }` (FAB) | `text = { Text(stringResource(R.string.equipment_add_fab)) }` |
| `contentDescription = "Delete Equipment"` (list trailing) | `contentDescription = stringResource(R.string.equipment_delete_cd)` |
| `{ Text("Bar weight: ${...} kg") }` (supportingContent) | `{ Text(stringResource(R.string.equipment_bar_weight, if (w % 1.0 == 0.0) w.toInt().toString() else w.toString())) }` |
| `title = { Text("Delete Equipment") }` | `title = { Text(stringResource(R.string.equipment_delete_dialog_title)) }` |
| `text = { Text("Are you sure you want to delete '${equipmentToDelete?.name}'?") }` | `text = { Text(stringResource(R.string.equipment_delete_dialog_body, equipmentToDelete?.name ?: "")) }` |
| `Text("Delete")` (confirm) | `Text(stringResource(R.string.action_delete))` |
| `Text("Cancel")` (dismiss) | `Text(stringResource(R.string.action_cancel))` |

In `EquipmentDialog`:

| Old | New |
|---|---|
| `title = { Text(if (initialEquipment == null) "Add Equipment" else "Edit Equipment") }` | `title = { Text(if (initialEquipment == null) stringResource(R.string.equipment_add_dialog_title) else stringResource(R.string.equipment_edit_dialog_title)) }` |
| `label = { Text("Equipment Name") }` | `label = { Text(stringResource(R.string.equipment_name_label)) }` |
| `label = { Text("Bar weight (kg, optional)") }` | `label = { Text(stringResource(R.string.equipment_bar_weight_label)) }` |
| `Text(if (initialEquipment == null) "Add" else "Save")` | `Text(if (initialEquipment == null) stringResource(R.string.action_add) else stringResource(R.string.action_save))` |
| `Text("Cancel")` (dismiss) | `Text(stringResource(R.string.action_cancel))` |

Also update `EquipmentItem`:

| Old | New |
|---|---|
| `contentDescription = "Delete Equipment"` | `contentDescription = stringResource(R.string.equipment_delete_cd)` |

- [ ] **Step 3: Build to verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/EquipmentScreen.kt
git commit -m "feat(i18n): localize EquipmentScreen"
```

---

## Task 13: Localize RoutinesScreen and RoutineDetailScreen

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/RoutinesScreen.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/RoutineDetailScreen.kt`

- [ ] **Step 1: Add imports to both files**

In `RoutinesScreen.kt`:
```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
```

In `RoutineDetailScreen.kt`:
```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
```

- [ ] **Step 2: Update RoutinesScreen**

| Old | New |
|---|---|
| `title = { Text("Routines", ...) }` | `title = { Text(stringResource(R.string.routines_title), ...) }` |
| `contentDescription = "Back"` (nav icon) | `contentDescription = stringResource(R.string.routines_back_cd)` |
| `text = { Text("New Routine") }` (FAB) | `text = { Text(stringResource(R.string.routines_new_fab)) }` |
| `"${routine.workoutDays.size} days"` | `stringResource(R.string.routines_days_count, routine.workoutDays.size)` |
| `contentDescription = if (isActive) "Active" else "Set Active"` | `contentDescription = if (isActive) stringResource(R.string.routines_active_cd) else stringResource(R.string.routines_set_active_cd)` |
| `contentDescription = "Delete"` | `contentDescription = stringResource(R.string.routines_delete_cd)` |
| `title = { Text("Delete Routine?") }` | `title = { Text(stringResource(R.string.routines_delete_dialog_title)) }` |
| `text = { Text("\"${routine.name}\" will be permanently deleted.") }` | `text = { Text(stringResource(R.string.routines_delete_dialog_body, routine.name)) }` |
| `Text("Delete", color = MaterialTheme.colorScheme.error)` | `Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)` |
| `Text("Cancel")` | `Text(stringResource(R.string.action_cancel))` |

- [ ] **Step 3: Update RoutineDetailScreen**

| Old | New |
|---|---|
| `contentDescription = "Back"` (nav icon) | `contentDescription = stringResource(R.string.action_back_cd)` |
| `contentDescription = "Edit Routine"` | `contentDescription = stringResource(R.string.routines_edit_cd)` |

Note: The `repsSummary` string in `WorkoutDayItem` is composed from data values (e.g. `"3 sets x 10+ reps"`) using Kotlin string templates. These remain as Kotlin code — do not extract them to resource keys. They are data-derived display strings, not UI labels.

- [ ] **Step 4: Build to verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/RoutinesScreen.kt app/src/main/java/de/melobeat/workoutplanner/ui/RoutineDetailScreen.kt
git commit -m "feat(i18n): localize RoutinesScreen and RoutineDetailScreen"
```

---

## Task 14: Localize CreateRoutine group (CreateRoutineScreen, RoutineDayCard, RoutineExerciseEditItem, RoutineExercisePicker)

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/CreateRoutineScreen.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/RoutineDayCard.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/RoutineExerciseEditItem.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/RoutineExercisePicker.kt`

- [ ] **Step 1: Add imports to all four files**

In each file, add:
```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
```

- [ ] **Step 2: Update CreateRoutineScreen**

| Old | New |
|---|---|
| `title = { Text(if (routineId == null) "New Routine" else "Edit Routine", ...) }` | `title = { Text(if (routineId == null) stringResource(R.string.create_routine_title_new) else stringResource(R.string.create_routine_title_edit), ...) }` |
| `contentDescription = "Back"` (nav icon) | `contentDescription = stringResource(R.string.create_routine_back_cd)` |
| `Text("Save")` (TextButton action) | `Text(stringResource(R.string.create_routine_save))` |
| `label = { Text("Routine Name") }` | `label = { Text(stringResource(R.string.create_routine_name_label)) }` |
| `label = { Text("Description") }` | `label = { Text(stringResource(R.string.create_routine_description_label)) }` |
| `Text("Add Day")` (Button) | `Text(stringResource(R.string.create_routine_add_day))` |

Also update the default day name on creation:
```kotlin
days = days + WorkoutDay(id = newDayId, name = "Day ${days.size + 1}")
```
This is a data default, not a UI label — leave it in English.

- [ ] **Step 3: Update RoutineDayCard**

| Old | New |
|---|---|
| `label = { Text("Day Name") }` | `label = { Text(stringResource(R.string.create_routine_day_name_label)) }` |
| `contentDescription = "Move Day Up"` | `contentDescription = stringResource(R.string.create_routine_move_day_up_cd)` |
| `contentDescription = "Move Day Down"` | `contentDescription = stringResource(R.string.create_routine_move_day_down_cd)` |
| `contentDescription = "Remove Day"` | `contentDescription = stringResource(R.string.create_routine_remove_day_cd)` |
| `Text("Add Exercise")` (TextButton) | `Text(stringResource(R.string.create_routine_add_exercise))` |

- [ ] **Step 4: Update RoutineExerciseEditItem**

| Old | New |
|---|---|
| `contentDescription = "Move Exercise Up"` | `contentDescription = stringResource(R.string.create_routine_move_exercise_up_cd)` |
| `contentDescription = "Move Exercise Down"` | `contentDescription = stringResource(R.string.create_routine_move_exercise_down_cd)` |
| `contentDescription = "Remove Exercise"` | `contentDescription = stringResource(R.string.create_routine_remove_exercise_cd)` |
| `label = { Text("Reps") }` | `label = { Text(stringResource(R.string.create_routine_reps_label)) }` |
| `label = { Text("Weight") }` | `label = { Text(stringResource(R.string.create_routine_weight_label)) }` |
| `Text("AMRAP", ...)` | `Text(stringResource(R.string.label_amrap), ...)` |
| `contentDescription = "Remove Set"` | `contentDescription = stringResource(R.string.create_routine_remove_set_cd)` |
| `Text("Add Set")` | `Text(stringResource(R.string.create_routine_add_set))` |

- [ ] **Step 5: Update RoutineExercisePicker**

| Old | New |
|---|---|
| `title = { Text("Select Exercise") }` | `title = { Text(stringResource(R.string.create_routine_select_exercise)) }` |
| `TextButton(onClick = onDismiss) { Text("Cancel") }` | `TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }` |

- [ ] **Step 6: Build to verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/CreateRoutineScreen.kt app/src/main/java/de/melobeat/workoutplanner/ui/RoutineDayCard.kt app/src/main/java/de/melobeat/workoutplanner/ui/RoutineExerciseEditItem.kt app/src/main/java/de/melobeat/workoutplanner/ui/RoutineExercisePicker.kt
git commit -m "feat(i18n): localize CreateRoutine group"
```

---

## Task 15: Localize RestTimerBanner and ExerciseSelectionDialog

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/RestTimerBanner.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseSelectionDialog.kt`

- [ ] **Step 1: Add imports to both files**

In each file:
```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
```

- [ ] **Step 2: Update RestTimerBanner**

The `milestoneLabel` is computed as a plain `String?` variable before the composable tree. Move the translation inline into the `Text()` call, or compute it as a nullable localized string using `stringResource`. Since `stringResource` is only callable in Composable context, the simplest approach is to resolve it inside the composable block.

Replace the `milestoneLabel` computation block:

```kotlin
val milestoneLabel: String? = when (restTimer.context) {
    RestTimerContext.BetweenSets -> when {
        elapsed >= restTimer.hardThresholdSeconds -> "HARD? TIME TO GO"
        elapsed >= restTimer.easyThresholdSeconds -> "EASY? TIME TO GO"
        else -> null
    }
    RestTimerContext.BetweenExercises -> when {
        elapsed >= restTimer.singleThresholdSeconds -> "READY FOR NEXT EXERCISE?"
        else -> null
    }
}
```

With:

```kotlin
val hardMilestone = stringResource(R.string.rest_timer_hard_milestone)
val easyMilestone = stringResource(R.string.rest_timer_easy_milestone)
val exerciseMilestone = stringResource(R.string.rest_timer_exercise_milestone)

val milestoneLabel: String? = when (restTimer.context) {
    RestTimerContext.BetweenSets -> when {
        elapsed >= restTimer.hardThresholdSeconds -> hardMilestone
        elapsed >= restTimer.easyThresholdSeconds -> easyMilestone
        else -> null
    }
    RestTimerContext.BetweenExercises -> when {
        elapsed >= restTimer.singleThresholdSeconds -> exerciseMilestone
        else -> null
    }
}
```

Also replace:
```kotlin
Text(
    text = "REST",
    ...
)
```
With:
```kotlin
Text(
    text = stringResource(R.string.rest_timer_label),
    ...
)
```

- [ ] **Step 3: Update ExerciseSelectionDialog**

| Old | New |
|---|---|
| `placeholder = { Text("Search...") }` | `placeholder = { Text(stringResource(R.string.exercises_search_placeholder)) }` |

Note: The `title` parameter is already a `String` passed from the call site (`WorkoutScreen.kt`) — those call sites were already updated in Task 4.

- [ ] **Step 4: Build to verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/RestTimerBanner.kt app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseSelectionDialog.kt
git commit -m "feat(i18n): localize RestTimerBanner and ExerciseSelectionDialog"
```

---

## Task 16: Final Build Verification

**Files:** None modified

- [ ] **Step 1: Run full debug build**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run unit tests**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 3: Run lint**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:lintDebug 2>&1 | grep -E "Error|Warning|error|warning" | head -30
```

Expected: No new errors related to string resources

- [ ] **Step 4: Final commit if there are any cleanup changes**

```bash
git status
# If clean, nothing to do. If there are any lint fixes:
git add -A
git commit -m "fix(i18n): address lint warnings from localization"
```

---

## Self-Review Checklist

### Spec coverage

- [x] `res/values/strings.xml` — all keys defined (Task 1)
- [x] `res/values-de/strings.xml` — all German translations (Task 1)
- [x] `MainActivity.kt` — nav labels + resume button (Task 2)
- [x] `HomeScreen.kt` — all 13 unique strings (Task 3)
- [x] `WorkoutScreen.kt` — all 13 strings (Task 4)
- [x] `ExerciseCard.kt` — all 20 strings (Task 5)
- [x] `WorkoutSummaryScreen.kt` — all 9 strings (Task 6)
- [x] `HistoryScreen.kt` — all strings + special `getDateGroupLabel` handling (Task 7)
- [x] `SettingsScreen.kt` — all 15 strings (Task 8)
- [x] `TimerSettingsScreen.kt` — all 10 strings (Task 9)
- [x] `ProfileScreen.kt` — all 7 strings (Task 10)
- [x] `ExercisesScreen.kt` — all 15 strings (Task 11)
- [x] `EquipmentScreen.kt` — all 12 strings (Task 12)
- [x] `RoutinesScreen.kt` + `RoutineDetailScreen.kt` — all strings (Task 13)
- [x] `CreateRoutineScreen.kt` + `RoutineDayCard.kt` + `RoutineExerciseEditItem.kt` + `RoutineExercisePicker.kt` (Task 14)
- [x] `RestTimerBanner.kt` + `ExerciseSelectionDialog.kt` (Task 15)
- [x] Build verification (Task 16)
- [x] German translation decisions from spec honored (AMRAP, Reps→Wdh., REST→PAUSE, UP NEXT→ALS NÄCHSTES, Skipped→Übersprungen, Save & Exit→Speichern & Beenden)
- [x] Shared keys reused: `action_cancel`, `action_save`, `action_delete`, `action_add`, `unit_kg`, `unit_reps`, `unit_seconds_suffix`, `label_amrap`, `action_back_cd`

### Special cases addressed

- `getDateGroupLabel()` — English strings used as sort keys, translation applied only at display site (Task 7)
- `WorkoutStepperCard.kt` — no changes needed; `label` is already passed in as a parameter from `ExerciseCard.kt` (Task 5 updates the call sites)
- `repsSummary` in `RoutineDetailScreen.kt` — data-derived string, deliberately left as Kotlin code per spec non-goals
- `RoutineDayCard.kt` — `DayCard` composable is a separate file (not inside `CreateRoutineScreen.kt`)
- `ExerciseSelectionDialog.kt` — `title` parameter already provided by call sites; only `placeholder` text updated in Task 15
