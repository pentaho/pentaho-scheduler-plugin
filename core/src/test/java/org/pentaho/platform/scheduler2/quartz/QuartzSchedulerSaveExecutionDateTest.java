package org.pentaho.platform.scheduler2.quartz;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.DateBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CalendarIntervalTriggerImpl;
import org.quartz.impl.triggers.CronTriggerImpl;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QuartzSchedulerSaveExecutionDateTest {

  private static final String TRIGGER_EXISTS_MESSAGE = "A trigger should exist after saveExecutionDate";
  private static final String TEST_GROUP = "testGroup";
  private static final String TEST_VALUE = "value1";
  private static final String TEST_JOB = "testJob";
  private static final String TEST_TRIGGER = "testTrigger";
  private static final String MOCK_TEST_GROUP = "test-group";
  private static final String PARAM_KEY = "key1";
  private static final String UTC_TIMEZONE = "UTC";
  private static final String MANUAL_TRIGGER_PREFIX = "MT_";
  private static final String MOCK_CALENDAR_NAME = "my-calendar";
  private static final String CRON_CALENDAR_NAME = "cron-calendar";
  private static final int EXECUTION_TIMEOUT_SECONDS = 5;

  private Scheduler scheduler;
  private QuartzScheduler quartzScheduler;

  public static class CountingJob implements org.quartz.Job {
    private static final AtomicInteger EXECUTIONS = new AtomicInteger();
    private static final AtomicReference<CountDownLatch> LATCH = new AtomicReference<>( new CountDownLatch( 1 ) );

    static void reset() {
      EXECUTIONS.set( 0 );
      LATCH.set( new CountDownLatch( 1 ) );
    }

    static int getExecutionCount() {
      return EXECUTIONS.get();
    }

    static boolean awaitExecution() throws InterruptedException {
      return LATCH.get().await( EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS );
    }

    @Override
    public void execute( JobExecutionContext context ) {
      EXECUTIONS.incrementAndGet();
      LATCH.get().countDown();
    }
  }

  @Before
  public void setUp() throws Exception {
    scheduler = new StdSchedulerFactory().getScheduler();
    scheduler.start();
    quartzScheduler = new QuartzScheduler();
    CountingJob.reset();
  }

  @After
  public void tearDown() throws Exception {
    if ( scheduler != null && scheduler.isStarted() ) {
      scheduler.shutdown();
    }
  }

  @Test
  public void testSaveExecutionDate() throws Exception {
    // Arrange
    Date executionTime = new Date();
    JobDetail mockJobDetail = mock( JobDetail.class );
    JobKey jobKey = new JobKey( TEST_JOB, TEST_GROUP );
    JobDataMap jobDataMap = new JobDataMap();

    when( mockJobDetail.getKey() ).thenReturn( jobKey );
    when( mockJobDetail.getJobDataMap() ).thenReturn( jobDataMap );
    when( mockJobDetail.getJobClass() ).thenReturn( (Class) BlockingQuartzJob.class );

    // Mock Scheduler and related objects
    Scheduler mockScheduler = mock( Scheduler.class );
    Trigger mockTrigger = mock( Trigger.class );

    when( mockTrigger.getTriggerBuilder() ).thenAnswer( unused -> TriggerBuilder.newTrigger() );
    when( mockTrigger.getNextFireTime() ).thenReturn( new Date() );

    when( mockScheduler.getJobDetail( jobKey ) ).thenReturn( mockJobDetail );
    when( mockScheduler.getTriggersOfJob( jobKey ) )
      .thenAnswer( unused -> Collections.singletonList( mockTrigger ) );

    // Mock SchedulerFactory
    SchedulerFactory mockSchedulerFactory = mock( SchedulerFactory.class );
    when( mockSchedulerFactory.getScheduler() ).thenReturn( mockScheduler );

    // Instantiate QuartzScheduler and set the mock SchedulerFactory
    QuartzScheduler mockQuartzScheduler = new QuartzScheduler();
    mockQuartzScheduler.setQuartzSchedulerFactory( mockSchedulerFactory );

    // Act
    mockQuartzScheduler.saveExecutionDate( jobKey, executionTime );

    // Assert
    verify( mockScheduler ).deleteJob( jobKey );

    ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass( JobDetail.class );
    verify( mockScheduler ).scheduleJob( jobDetailCaptor.capture(), any( Trigger.class ) );
    assertEquals( executionTime,
      jobDetailCaptor.getValue().getJobDataMap().get( QuartzScheduler.RESERVEDMAPKEY_LAST_EXECUTION_TIME ) );
  }

  @Test
  public void testSaveExecutionDateCalendarIntervalTriggerPreservesSchedule() throws Exception {
    Date executionTime = new Date();
    JobDetail mockJobDetail = mock( JobDetail.class );
    JobKey jobKey = new JobKey( TEST_JOB, TEST_GROUP );
    JobDataMap jobDataMap = new JobDataMap();

    when( mockJobDetail.getKey() ).thenReturn( jobKey );
    when( mockJobDetail.getJobDataMap() ).thenReturn( jobDataMap );
    when( mockJobDetail.getJobClass() ).thenReturn( (Class) BlockingQuartzJob.class );

    CalendarIntervalTriggerImpl oldTrigger = new CalendarIntervalTriggerImpl();
    Date startTime = new Date( System.currentTimeMillis() - 1000 );
    Date nextFireTime = new Date( System.currentTimeMillis() + 300000 );

    oldTrigger.setKey( new TriggerKey( TEST_TRIGGER, TEST_GROUP ) );
    oldTrigger.setJobKey( jobKey );
    oldTrigger.setStartTime( startTime );
    oldTrigger.setNextFireTime( nextFireTime );
    oldTrigger.setRepeatInterval( 5 );
    oldTrigger.setRepeatIntervalUnit( DateBuilder.IntervalUnit.MINUTE );
    oldTrigger.setTimeZone( TimeZone.getTimeZone( UTC_TIMEZONE ) );
    oldTrigger.setMisfireInstruction( CalendarIntervalTrigger.MISFIRE_INSTRUCTION_DO_NOTHING );

    Scheduler mockScheduler = mock( Scheduler.class );
    when( mockScheduler.getJobDetail( jobKey ) ).thenReturn( mockJobDetail );
    when( mockScheduler.getTriggersOfJob( jobKey ) )
      .thenAnswer( unused -> Collections.singletonList( oldTrigger ) );

    SchedulerFactory mockSchedulerFactory = mock( SchedulerFactory.class );
    when( mockSchedulerFactory.getScheduler() ).thenReturn( mockScheduler );

    QuartzScheduler mockQuartzScheduler = new QuartzScheduler();
    mockQuartzScheduler.setQuartzSchedulerFactory( mockSchedulerFactory );

    mockQuartzScheduler.saveExecutionDate( jobKey, executionTime );

    verify( mockScheduler ).deleteJob( jobKey );

    ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass( JobDetail.class );
    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass( Trigger.class );
    verify( mockScheduler ).scheduleJob( jobDetailCaptor.capture(), triggerCaptor.capture() );
    assertEquals( executionTime,
      jobDetailCaptor.getValue().getJobDataMap().get( QuartzScheduler.RESERVEDMAPKEY_LAST_EXECUTION_TIME ) );
    Trigger scheduledTrigger = triggerCaptor.getValue();
    assertTrue( scheduledTrigger instanceof CalendarIntervalTriggerImpl );
    CalendarIntervalTriggerImpl t = (CalendarIntervalTriggerImpl) scheduledTrigger;
    assertEquals( 5, t.getRepeatInterval() );
    assertEquals( DateBuilder.IntervalUnit.MINUTE, t.getRepeatIntervalUnit() );
    assertEquals( UTC_TIMEZONE, t.getTimeZone( ).getID( ) );
    assertEquals( CalendarIntervalTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, t.getMisfireInstruction() );
    assertNotNull( t.getStartTime() );
  }

  @Test
  public void testSaveExecutionDatePreservesCalendarName() throws Exception {
    // Arrange: Create a job with CalendarIntervalTrigger and calendar name
    JobKey jobKey = new JobKey( TEST_JOB, TEST_GROUP );

    Map<String, Object> jobParams = new HashMap<>();
    jobParams.put( PARAM_KEY, TEST_VALUE );
    jobParams.put( QuartzScheduler.RESERVEDMAPKEY_LAST_EXECUTION_TIME, new Date( System.currentTimeMillis() - 10000 ) );

    JobDetail jobDetail = JobBuilder.newJob( BlockingQuartzJob.class )
      .withIdentity( jobKey )
      .usingJobData( new JobDataMap( jobParams ) )
      .build();

    CalendarIntervalTriggerImpl trigger = new CalendarIntervalTriggerImpl();
    trigger.setKey( new TriggerKey( TEST_TRIGGER, TEST_GROUP ) );
    trigger.setJobKey( jobKey );
    trigger.setRepeatInterval( 1 );
    trigger.setRepeatIntervalUnit( org.quartz.DateBuilder.IntervalUnit.HOUR );
    trigger.setStartTime( new Date() );
    trigger.setCalendarName( MOCK_CALENDAR_NAME );
    trigger.setMisfireInstruction( CalendarIntervalTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW );

    scheduler.scheduleJob( jobDetail, trigger );

    // Act: Call saveExecutionDate
    Date newExecutionTime = new Date();
    quartzScheduler.saveExecutionDate( jobKey, newExecutionTime );

    // Assert: Verify calendar name is preserved on new trigger
    Trigger newTrigger = scheduler.getTriggersOfJob( jobKey ).stream()
      .filter( t -> !t.getKey().getName().startsWith( MANUAL_TRIGGER_PREFIX ) )
      .findFirst()
      .orElse( null );

    assertNotNull( TRIGGER_EXISTS_MESSAGE, newTrigger );
    assertEquals( "Calendar name should be preserved", MOCK_CALENDAR_NAME, newTrigger.getCalendarName( ) );
  }

  @Test
  public void testSaveExecutionDateHandlesNullCalendarName() throws Exception {
    // Arrange: Create a job with trigger without calendar name
    JobKey jobKey = new JobKey( TEST_JOB, MOCK_TEST_GROUP );

    Map<String, Object> jobParams = new HashMap<>();
    jobParams.put( PARAM_KEY, TEST_VALUE );
    jobParams.put( QuartzScheduler.RESERVEDMAPKEY_LAST_EXECUTION_TIME, new Date( System.currentTimeMillis() - 10000 ) );

    JobDetail jobDetail = JobBuilder.newJob( BlockingQuartzJob.class )
      .withIdentity( jobKey )
      .usingJobData( new JobDataMap( jobParams ) )
      .build();

    CalendarIntervalTriggerImpl trigger = new CalendarIntervalTriggerImpl();
    trigger.setKey( new TriggerKey( TEST_TRIGGER, MOCK_TEST_GROUP ) );
    trigger.setJobKey( jobKey );
    trigger.setRepeatInterval( 1 );
    trigger.setRepeatIntervalUnit( org.quartz.DateBuilder.IntervalUnit.HOUR );
    trigger.setStartTime( new Date() );
    trigger.setMisfireInstruction( CalendarIntervalTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW );

    scheduler.scheduleJob( jobDetail, trigger );

    // Act: Call saveExecutionDate
    Date newExecutionTime = new Date();
    quartzScheduler.saveExecutionDate( jobKey, newExecutionTime );

    // Assert: Verify calendar name remains null
    Trigger newTrigger = scheduler.getTriggersOfJob( jobKey ).stream()
      .filter( t -> !t.getKey().getName().startsWith( MANUAL_TRIGGER_PREFIX ) )
      .findFirst()
      .orElse( null );

    assertNotNull( TRIGGER_EXISTS_MESSAGE, newTrigger );
    assertNull( "Calendar name should be null", newTrigger.getCalendarName() );
  }

  @Test
  public void testSaveExecutionDatePreservesScheduleProperties() throws Exception {
    // Arrange: Create a job with specific schedule properties
    JobKey jobKey = new JobKey( TEST_JOB, MOCK_TEST_GROUP );

    Map<String, Object> jobParams = new HashMap<>();
    jobParams.put( QuartzScheduler.RESERVEDMAPKEY_LAST_EXECUTION_TIME, new Date( System.currentTimeMillis() - 10000 ) );

    JobDetail jobDetail = JobBuilder.newJob( BlockingQuartzJob.class )
      .withIdentity( jobKey )
      .usingJobData( new JobDataMap( jobParams ) )
      .build();

    CalendarIntervalTriggerImpl trigger = new CalendarIntervalTriggerImpl();
    trigger.setKey( new TriggerKey( TEST_TRIGGER, MOCK_TEST_GROUP ) );
    trigger.setJobKey( jobKey );
    trigger.setRepeatInterval( 5 );
    trigger.setRepeatIntervalUnit( org.quartz.DateBuilder.IntervalUnit.MINUTE );
    trigger.setTimeZone( TimeZone.getTimeZone( UTC_TIMEZONE ) );
    trigger.setStartTime( new Date() );
    trigger.setMisfireInstruction( CalendarIntervalTrigger.MISFIRE_INSTRUCTION_DO_NOTHING );

    scheduler.scheduleJob( jobDetail, trigger );

    // Act: Call saveExecutionDate
    Date newExecutionTime = new Date();
    quartzScheduler.saveExecutionDate( jobKey, newExecutionTime );

    // Assert: Verify schedule properties are preserved
    Trigger newTrigger = scheduler.getTriggersOfJob( jobKey ).stream()
      .filter( t -> !t.getKey().getName().startsWith( MANUAL_TRIGGER_PREFIX ) )
      .findFirst( )
      .orElse( null );

    assertNotNull( TRIGGER_EXISTS_MESSAGE, newTrigger );
    assertTrue( "New trigger should be CalendarIntervalTrigger", newTrigger instanceof CalendarIntervalTrigger );

    CalendarIntervalTrigger castedTrigger = ( CalendarIntervalTrigger ) newTrigger;
    assertEquals( "Repeat interval should be preserved", 5, castedTrigger.getRepeatInterval( ) );
    assertEquals( "Repeat interval unit should be preserved", org.quartz.DateBuilder.IntervalUnit.MINUTE,
      castedTrigger.getRepeatIntervalUnit() );
    assertEquals( "Timezone should be preserved", TimeZone.getTimeZone( UTC_TIMEZONE ), castedTrigger.getTimeZone( ) );
    assertEquals( "Misfire instruction should be preserved", CalendarIntervalTrigger.MISFIRE_INSTRUCTION_DO_NOTHING,
      castedTrigger.getMisfireInstruction() );
  }

  @Test
  public void testSaveExecutionDateHandlesNonCalendarIntervalTrigger() throws Exception {
    // Arrange: Create a job with CronTrigger
    JobKey jobKey = new JobKey( TEST_JOB, MOCK_TEST_GROUP );

    Map<String, Object> jobParams = new HashMap<>();
    jobParams.put( QuartzScheduler.RESERVEDMAPKEY_LAST_EXECUTION_TIME, new Date( System.currentTimeMillis() - 10000 ) );

    JobDetail jobDetail = JobBuilder.newJob( BlockingQuartzJob.class )
      .withIdentity( jobKey )
      .usingJobData( new JobDataMap( jobParams ) )
      .build();

    CronTriggerImpl trigger = new CronTriggerImpl();
    trigger.setKey( new TriggerKey( "cron-trigger", MOCK_TEST_GROUP ) );
    trigger.setJobKey( jobKey );
    trigger.setStartTime( new Date() );
    trigger.setCalendarName( CRON_CALENDAR_NAME );
    try {
      trigger.setCronExpression( "0 0 12 * * ?" );
    } catch ( Exception e ) {
      fail( "Failed to set cron expression: " + e.getMessage() );
    }

    scheduler.scheduleJob( jobDetail, trigger );

    // Act: Call saveExecutionDate
    Date newExecutionTime = new Date();
    quartzScheduler.saveExecutionDate( jobKey, newExecutionTime );

    // Assert: Verify calendar name is preserved for CronTrigger
    Trigger newTrigger = scheduler.getTriggersOfJob( jobKey ).stream()
      .filter( t -> !t.getKey().getName().startsWith( MANUAL_TRIGGER_PREFIX ) )
      .findFirst( )
      .orElse( null );

    assertNotNull( TRIGGER_EXISTS_MESSAGE, newTrigger );
    assertEquals( "Calendar name should be preserved for CronTrigger", CRON_CALENDAR_NAME, newTrigger.getCalendarName( ) );
  }

  @Test
  public void testSaveExecutionDateRestoresTriggerPausedState() throws Exception {
    // Arrange: Create and schedule a job with a paused trigger
    JobKey jobKey = new JobKey( TEST_JOB, MOCK_TEST_GROUP );

    Map<String, Object> jobParams = new HashMap<>();
    jobParams.put( PARAM_KEY, TEST_VALUE );
    jobParams.put( QuartzScheduler.RESERVEDMAPKEY_LAST_EXECUTION_TIME, new Date( System.currentTimeMillis() - 10000 ) );

    JobDetail jobDetail = JobBuilder.newJob( BlockingQuartzJob.class )
      .withIdentity( jobKey )
      .usingJobData( new JobDataMap( jobParams ) )
      .build();

    Trigger trigger = TriggerBuilder.newTrigger()
      .withIdentity( new TriggerKey( TEST_TRIGGER, MOCK_TEST_GROUP ) )
      .withSchedule( CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
        .withIntervalInHours( 1 ) )
      .startAt( new Date() )
      .build();

    scheduler.scheduleJob( jobDetail, trigger );

    // Pause the trigger to establish initial state
    scheduler.pauseTrigger( trigger.getKey() );

    // Verify trigger is paused
    assertEquals( "Trigger should be PAUSED", Trigger.TriggerState.PAUSED,
      scheduler.getTriggerState( trigger.getKey() ) );

    // Act: Call saveExecutionDate which should preserve the paused state
    Date newExecutionTime = new Date();
    quartzScheduler.saveExecutionDate( jobKey, newExecutionTime );

    // Assert: Get the new trigger and verify it's still paused
    Trigger newTrigger = scheduler.getTriggersOfJob( jobKey ).stream()
      .filter( t -> !t.getKey().getName().startsWith( MANUAL_TRIGGER_PREFIX ) )
      .findFirst()
      .orElse( null );

    assertNotNull( TRIGGER_EXISTS_MESSAGE, newTrigger );
    assertEquals( "New trigger should be PAUSED after saveExecutionDate", Trigger.TriggerState.PAUSED,
      scheduler.getTriggerState( newTrigger.getKey() ) );
  }

  @Test
  public void testTriggerNowExecutesCalendarIntervalJobExactlyOnce() throws Exception {
    QuartzJobKey quartzJobKey = new QuartzJobKey( TEST_JOB, TEST_GROUP );
    JobKey jobKey = new JobKey( quartzJobKey.toString(), quartzJobKey.getUserName() );
    Date lastExecutionTime = new Date( System.currentTimeMillis() - 10_000 );

    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put( QuartzScheduler.RESERVEDMAPKEY_LAST_EXECUTION_TIME, lastExecutionTime );

    JobDetail jobDetail = JobBuilder.newJob( CountingJob.class )
      .withIdentity( jobKey )
      .usingJobData( jobDataMap )
      .build();

    CalendarIntervalTriggerImpl trigger = new CalendarIntervalTriggerImpl();
    trigger.setKey( new TriggerKey( TEST_TRIGGER, TEST_GROUP ) );
    trigger.setJobKey( jobKey );
    trigger.setRepeatInterval( 1 );
    trigger.setRepeatIntervalUnit( DateBuilder.IntervalUnit.HOUR );
    trigger.setStartTime( new Date( System.currentTimeMillis() + 60_000 ) );
    trigger.setMisfireInstruction( CalendarIntervalTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW );

    scheduler.scheduleJob( jobDetail, trigger );

    SchedulerFactory schedulerFactory = mock( SchedulerFactory.class );
    when( schedulerFactory.getScheduler() ).thenReturn( scheduler );
    quartzScheduler.setQuartzSchedulerFactory( schedulerFactory );

    quartzScheduler.triggerNow( jobKey.getName() );

    assertTrue( "Execute Now should fire the job once", CountingJob.awaitExecution() );

    await()
      .pollInterval( 50, TimeUnit.MILLISECONDS )
      .during( 750, TimeUnit.MILLISECONDS )
      .atMost( 1, TimeUnit.SECONDS )
      .until( () -> CountingJob.getExecutionCount() <= 1 );

    assertEquals( "Execute Now should produce a single execution", 1, CountingJob.getExecutionCount() );
    assertEquals( "Execute Now should not pre-update Last Run", lastExecutionTime,
      scheduler.getJobDetail( jobKey ).getJobDataMap().get( QuartzScheduler.RESERVEDMAPKEY_LAST_EXECUTION_TIME ) );

    long manualTriggerCount = scheduler.getTriggersOfJob( jobKey ).stream()
      .filter( this::isManualTrigger )
      .count();
    assertTrue( "Manual trigger handling should remain intact", manualTriggerCount <= 1 );
  }

  @Test
  public void testResumeJobDoesNotImmediatelyFirePausedCalendarIntervalSchedule() throws Exception {
    QuartzJobKey quartzJobKey = new QuartzJobKey( TEST_JOB, TEST_GROUP );
    JobKey jobKey = new JobKey( quartzJobKey.toString(), quartzJobKey.getUserName() );
    Date lastExecutionTime = new Date( System.currentTimeMillis() - 10_000 );

    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put( QuartzScheduler.RESERVEDMAPKEY_LAST_EXECUTION_TIME, lastExecutionTime );

    JobDetail jobDetail = JobBuilder.newJob( CountingJob.class )
      .withIdentity( jobKey )
      .usingJobData( jobDataMap )
      .build();

    CalendarIntervalTriggerImpl trigger = new CalendarIntervalTriggerImpl();
    trigger.setKey( new TriggerKey( TEST_TRIGGER, TEST_GROUP ) );
    trigger.setJobKey( jobKey );
    trigger.setRepeatInterval( 1 );
    trigger.setRepeatIntervalUnit( DateBuilder.IntervalUnit.MINUTE );
    trigger.setStartTime( new Date( System.currentTimeMillis() + 1_000 ) );
    trigger.setMisfireInstruction( CalendarIntervalTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW );

    scheduler.scheduleJob( jobDetail, trigger );
    scheduler.pauseJob( jobKey );

    // Wait until the trigger's nextFireTime has lapsed so Quartz would normally misfire on resume
    await()
      .pollInterval( 100, TimeUnit.MILLISECONDS )
      .atMost( 5, TimeUnit.SECONDS )
      .until( () -> {
        Trigger t = scheduler.getTriggersOfJob( jobKey ).stream().findFirst().orElse( null );
        return t != null && t.getNextFireTime() != null && t.getNextFireTime().before( new Date() );
      } );

    SchedulerFactory schedulerFactory = mock( SchedulerFactory.class );
    when( schedulerFactory.getScheduler() ).thenReturn( scheduler );
    quartzScheduler.setQuartzSchedulerFactory( schedulerFactory );

    quartzScheduler.resumeJob( jobKey.getName() );

    await()
      .pollInterval( 50, TimeUnit.MILLISECONDS )
      .during( 750, TimeUnit.MILLISECONDS )
      .atMost( 1, TimeUnit.SECONDS )
      .until( () -> CountingJob.getExecutionCount() == 0 );

    assertEquals( "Resume should not immediately execute a paused schedule", 0, CountingJob.getExecutionCount() );
    assertEquals( "Resume should not pre-update Last Run", lastExecutionTime,
      scheduler.getJobDetail( jobKey ).getJobDataMap().get( QuartzScheduler.RESERVEDMAPKEY_LAST_EXECUTION_TIME ) );
  }

  private boolean isManualTrigger( Trigger trigger ) {
    return trigger != null
      && trigger.getKey() != null
      && trigger.getKey().getName() != null
      && trigger.getKey().getName().startsWith( MANUAL_TRIGGER_PREFIX );
  }
}
