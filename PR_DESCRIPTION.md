## fix[BISERVER-15614]: Prevent misfire on resume

### Problem
An issue in the Pentaho Scheduler Plugin causes immediate job execution when a paused schedule is resumed. This happens because:

1. When a trigger is paused, its `nextFireTime` remains stale (in the past) and is not updated by Quartz
2. Upon resume, the scheduler moves the paused trigger back to WAITING state
3. Quartz detects that `nextFireTime` is in the past and applies the trigger's misfire instruction
4. `CalendarIntervalTrigger` uses `MISFIRE_INSTRUCTION_FIRE_ONCE_NOW` by default, which fires the job immediately as a "catch-up" mechanism
5. Result: An unintended immediate execution of the job

### Solution
Before resuming or rescheduling a job, normalize the trigger's timing state by:

1. **Computing a future start time** using `getNextFireTimeInFuture()`:
   - If `nextFireTime` is already in the future, use it
   - Otherwise, compute the next fire time after *now* using the trigger's interval/schedule
   - As a fallback, use the stale `nextFireTime` or original `startTime`

2. **Rebuilding the trigger** with the normalized start time while preserving all original properties (interval, timezone, DST settings, misfire instruction, calendar name)

3. **Recreating the JobDetail** to persist any updated job data (e.g., Last Run timestamp) via Quartz's delete-and-reschedule mechanism

4. **Restoring the trigger state** (e.g., paused) after rescheduling

### Changes Made
- **`getNextFireTimeInFuture(Trigger)`**: Shared helper that computes a future start time using resolution order: future nextFireTime → computed getFireTimeAfter(now) → fallback to stale nextFireTime or startTime. Prevents misfire from treating rescheduled triggers as overdue.
- **`normalizeTriggerTimingState(JobKey, Date)`**: Central method that orchestrates trigger/job rebuild with normalized timing, deletes old trigger, reschedules with new timing, and restores trigger state
- **`recreateTriggerWithNewStartTime(Trigger)`**: Rebuilds the trigger with normalized start time from `getNextFireTimeInFuture()`, preserving all properties
- **`recreateJobDetail(JobDetail, JobKey, JobDataMap)`**: Creates a new JobDetail with the updated job data map
- **`applyMisfireInstruction()` & `restoreTriggerState()`**: Helper methods for misfire handling and state preservation
- **`resumeJob()` integration**: Calls `normalizeTriggerTimingState()` before resuming to ensure no immediate execution
- **`saveExecutionDate()` refactoring**: Uses the shared `normalizeTriggerTimingState()` to persist Last Run timestamps
- **`setJobNextRun()` simplification**: Refactored to use `getNextFireTimeInFuture()` for consistent timing logic

### Testing
- Unit tests verify trigger normalization, state preservation, and calendar name handling
- Integration tests confirm that resumed paused schedules do NOT immediately execute
- All 33 tests pass in `QuartzSchedulerTest` and `QuartzSchedulerSaveExecutionDateTest`
