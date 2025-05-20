/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/

package org.pentaho.platform.scheduler2.quartz;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.scheduler2.ComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.CalendarIntervalTriggerImpl;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.spi.MutableTrigger;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pentaho.platform.api.scheduler2.IScheduler.RESERVEDMAPKEY_ACTIONUSER;

public class QuartzSchedulerTest {


  private static IUnifiedRepository repo;
  private static IUnifiedRepository oldRepo;


  @BeforeClass
  public static void setUp() {
    oldRepo = PentahoSystem.get( IUnifiedRepository.class );
    repo = mock( IUnifiedRepository.class );
    when( repo.getFile( Mockito.anyString() ) ).then( invocationOnMock -> {
      final RepositoryFile repositoryFile = mock( RepositoryFile.class );
      final String param = (String) invocationOnMock.getArguments()[0];
      if ( "/home/admin/notexist.ktr".equals( param ) ) {
        return null;
      }
      if ( "/home/admin".equals( param ) ) {
        when( repositoryFile.isFolder() ).thenReturn( true );
      }
      if ( "/home/admin/notallowed.ktr".equals( param ) ) {
        when( repositoryFile.isFolder() ).thenReturn( false );
        when( repositoryFile.isSchedulable() ).thenReturn( false );
      }
      if ( "/home/admin/allowed.ktr".equals( param ) ) {
        when( repositoryFile.isFolder() ).thenReturn( false );
        when( repositoryFile.isSchedulable() ).thenReturn( true );
      }
      return repositoryFile;
    } );
    PentahoSystem.registerObject( repo, IUnifiedRepository.class );
  }

  @AfterClass
  public static void tearDown() {
    repo = null;
    if ( oldRepo != null ) {
      PentahoSystem.registerObject( oldRepo, IUnifiedRepository.class );
    }
  }

  @Test
  public void testValidateParamsNoStreamProviderParam() throws SchedulerException {
    new QuartzScheduler().validateJobParams( Collections.emptyMap() );
  }

  @Test
  public void testValidateParamsNoStringConf() throws SchedulerException {
    new QuartzScheduler()
      .validateJobParams( Collections.singletonMap( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER, 1L ) );
  }

  @Test
  public void testValidateParamsNoInputFile() throws SchedulerException {
    new QuartzScheduler()
      .validateJobParams( Collections.singletonMap( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER, "someinputfile" ) );
  }

  @Test( expected = SchedulerException.class )
  public void testValidateParamsFileNotFound() throws SchedulerException {
    new QuartzScheduler()
      .validateJobParams( Collections.singletonMap( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER,
        "input = /home/admin/notexist.ktr : output = /home/admin/notexist" ) );
  }

  @Test( expected = SchedulerException.class )
  public void testValidateParamsFileIsFolder() throws SchedulerException {
    new QuartzScheduler()
      .validateJobParams( Collections.singletonMap( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER,
        "input = /home/admin : output = /home/admin/notexist" ) );
  }

  @Test( expected = SchedulerException.class )
  public void testValidateParamsSchedulingNotAllowed() throws SchedulerException {
    new QuartzScheduler()
      .validateJobParams( Collections.singletonMap( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER,
        "input = /home/admin/notallowed.ktr : output = /home/admin/notallowed" ) );
  }

  @Test
  public void testValidateParamsSchedulingAllowed() throws SchedulerException {
    new QuartzScheduler()
      .validateJobParams( Collections.singletonMap( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER,
        "input = /home/admin/allowed.ktr : output = /home/admin/allowed." ) );
  }

  @Test
  public void testSetTimezone() throws Exception {
    CronTrigger cronTrigger = TriggerBuilder.newTrigger()
      .withSchedule( CronScheduleBuilder.cronSchedule( new CronExpression( "0 15 10 ? * 6L 2002-2018" ) ) ).build();
    String currentTimezoneId = TimeZone.getDefault().getID();

    new QuartzScheduler().setTimezone( cronTrigger, currentTimezoneId );

    assertNotNull( cronTrigger.getTimeZone() );
    assertEquals( currentTimezoneId, cronTrigger.getTimeZone().getID() );
  }

  @Test
  public void testSetJobNextRunToTheFuture() {

    Trigger trigger = mock( Trigger.class );
    Job job = new Job();
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    long nowDate = new Date().getTime();
    long futureDate = nowDate + 1000000000;

    when( trigger.getNextFireTime() ).thenReturn( new Date( futureDate ) );
    when( trigger.getFireTimeAfter( any() ) ).thenReturn( new Date( nowDate ) );

    quartzScheduler.setJobNextRun( job, trigger );

    assertEquals( new Date( futureDate ), job.getNextRun() );
  }

  @Test
  public void testSetJobNextRunToThePast() {

    Trigger trigger = mock( Trigger.class );
    Job job = new Job();
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    long nowDate = new Date().getTime();
    long pastDate = nowDate - 1000000000;

    when( trigger.getNextFireTime() ).thenReturn( new Date( pastDate ) );
    when( trigger.getFireTimeAfter( any() ) ).thenReturn( new Date( nowDate ) );

    quartzScheduler.setJobNextRun( job, trigger );

    assertEquals( new Date( nowDate ), job.getNextRun() );
  }

  @Test
  public void testSetJobNextRunToNullDate() {

    Trigger trigger = mock( Trigger.class );
    Job job = new Job();
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    long nowDate = new Date().getTime();

    when( trigger.getNextFireTime() ).thenReturn( null );
    when( trigger.getFireTimeAfter( any() ) ).thenReturn( new Date( nowDate ) );

    quartzScheduler.setJobNextRun( job, trigger );

    assertNull( job.getNextRun() );
  }

  @Test
  public void testTriggerEndTime() throws SchedulerException, org.quartz.SchedulerException {
    SimpleJobTrigger simpleJobTrigger = new SimpleJobTrigger();
    SchedulerFactory mockSchedulerFactory = mock( SchedulerFactory.class );
    Scheduler mockScheduler = mock( Scheduler.class );
    JobDetail mockJobDetail = mock( JobDetail.class );
    when( mockJobDetail.getKey() ).thenReturn( new org.quartz.JobKey( "fooJob", "fooGroup" ) );
    when( mockJobDetail.getJobDataMap() ).thenReturn( new JobDataMap() );
    when( mockSchedulerFactory.getScheduler() ).thenReturn( mockScheduler );
    when( mockScheduler.getJobDetail( any() ) ).thenReturn( mockJobDetail );
    Calendar testDates = Calendar.getInstance();
    testDates.set( Calendar.SECOND, 0 );
    testDates.set( Calendar.MILLISECOND, 0 );
    testDates.add( Calendar.DATE, 10 );

    simpleJobTrigger.setStartYear( testDates.get( Calendar.YEAR ) - 1900 );
    simpleJobTrigger.setStartMonth( testDates.get( Calendar.MONTH ) );
    simpleJobTrigger.setStartDay( testDates.get( Calendar.DATE ) );
    simpleJobTrigger.setStartHour( testDates.get( Calendar.HOUR_OF_DAY ) );
    simpleJobTrigger.setStartMin( testDates.get( Calendar.MINUTE ) );
    testDates.add( Calendar.DATE, 7 );

    simpleJobTrigger.setEndTime( testDates.getTime() );

    simpleJobTrigger.setUiPassParam( "HOURS" );
    simpleJobTrigger.setRepeatInterval( 2 * 60 * 60 ); // 2 hours in seconds

    QuartzScheduler quartzScheduler = new QuartzScheduler();
    quartzScheduler.setQuartzSchedulerFactory( mockSchedulerFactory );
    HashMap<String, Object> jobParams = new HashMap<>();
    jobParams.put( RESERVEDMAPKEY_ACTIONUSER, "fooUser" );
    quartzScheduler.createJob( "fooJob", jobParams, simpleJobTrigger, null );

    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass( Trigger.class );
    verify( mockScheduler ).scheduleJob( any( JobDetail.class ), triggerCaptor.capture() );

    assertEquals( testDates.getTime().getTime(), triggerCaptor.getValue().getEndTime().getTime() );
  }

  @Test
  public void testTriggerEndTimeWithTimeZone() throws SchedulerException, org.quartz.SchedulerException {
    // Set the default timezone to UTC, so that the test is not affected by the local timezone
    TimeZone.setDefault( TimeZone.getTimeZone( "UTC" ) );
    SimpleJobTrigger simpleJobTrigger = new SimpleJobTrigger();
    SchedulerFactory mockSchedulerFactory = mock( SchedulerFactory.class );
    Scheduler mockScheduler = mock( Scheduler.class );
    JobDetail mockJobDetail = mock( JobDetail.class );
    when( mockJobDetail.getKey() ).thenReturn( new org.quartz.JobKey( "fooJob", "fooGroup" ) );
    when( mockJobDetail.getJobDataMap() ).thenReturn( new JobDataMap() );
    when( mockSchedulerFactory.getScheduler() ).thenReturn( mockScheduler );
    when( mockScheduler.getJobDetail( any() ) ).thenReturn( mockJobDetail );
    Calendar testDates = Calendar.getInstance();
    testDates.set( Calendar.SECOND, 0 );
    testDates.set( Calendar.MILLISECOND, 0 );
    testDates.add( Calendar.DATE, 10 );

    simpleJobTrigger.setStartYear( testDates.get( Calendar.YEAR ) - 1900 );
    simpleJobTrigger.setStartMonth( testDates.get( Calendar.MONTH ) );
    simpleJobTrigger.setStartDay( testDates.get( Calendar.DATE ) );
    simpleJobTrigger.setStartHour( testDates.get( Calendar.HOUR_OF_DAY ) );
    simpleJobTrigger.setStartMin( testDates.get( Calendar.MINUTE ) );
    TimeZone tz = TimeZone.getTimeZone( ZoneId.of( "Australia/Perth" ) );
    simpleJobTrigger.setTimeZone( tz.getID() );
    testDates.add( Calendar.DATE, 7 );

    simpleJobTrigger.setEndTime( testDates.getTime() );

    simpleJobTrigger.setUiPassParam( "HOURS" );
    simpleJobTrigger.setRepeatInterval( 2 * 60 * 60 ); // 2 hours in seconds

    QuartzScheduler quartzScheduler = new QuartzScheduler();
    quartzScheduler.setQuartzSchedulerFactory( mockSchedulerFactory );
    HashMap<String, Object> jobParams = new HashMap<>();
    jobParams.put( RESERVEDMAPKEY_ACTIONUSER, "fooUser" );
    quartzScheduler.createJob( "fooJob", jobParams, simpleJobTrigger, null );

    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass( Trigger.class );
    verify( mockScheduler ).scheduleJob( any( JobDetail.class ), triggerCaptor.capture() );

    // convert the input time and the trigger time to UTC and verify they are the same
    long origTime = testDates.getTime().getTime();
    long tzTime = triggerCaptor.getValue().getEndTime().getTime();
    assertEquals( origTime + TimeZone.getDefault().getOffset( origTime ), tzTime + tz.getOffset( tzTime ) );
  }

  @Test
  public void testGetSingleJobTrigger() throws Exception {
    // Arrange
    Scheduler mockScheduler = mock( Scheduler.class );
    JobKey jobKey = new JobKey( "testJob", "testGroup" );

    Trigger manualTrigger = mock( Trigger.class );
    when( manualTrigger.getKey() ).thenReturn( new TriggerKey( "MT_manualTrigger" ) );

    Trigger validTrigger = mock( Trigger.class );
    when( validTrigger.getKey() ).thenReturn( new TriggerKey( "validTrigger" ) );

    when( mockScheduler.getTriggersOfJob( jobKey ) ).thenAnswer(
      unused -> Arrays.asList( manualTrigger, validTrigger ) );

    SchedulerFactory mockSchedulerFactory = mock( SchedulerFactory.class );
    when( mockSchedulerFactory.getScheduler() ).thenReturn( mockScheduler );

    QuartzScheduler quartzScheduler = new QuartzScheduler();
    quartzScheduler.setQuartzSchedulerFactory( mockSchedulerFactory );

    // Act
    Trigger result = quartzScheduler.getSingleJobTrigger( jobKey );

    // Assert
    assertEquals( validTrigger, result );
  }

  @Test
  public void testSetJobNextRun() {
    // Arrange
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    Job job = new Job();
    Trigger mockTrigger = mock( Trigger.class );

    Date nextFireTime = new Date( System.currentTimeMillis() + 10000 ); // 10 seconds in the future
    when( mockTrigger.getNextFireTime() ).thenReturn( nextFireTime );

    // Act
    quartzScheduler.setJobNextRun( job, mockTrigger );

    // Assert
    assertEquals( nextFireTime, job.getNextRun() );
  }

  @Test
  public void testIsManualTrigger() {
    // Arrange
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    Trigger manualTrigger = mock( Trigger.class );
    when( manualTrigger.getKey() ).thenReturn( new TriggerKey( "MT_manualTrigger" ) );

    Trigger nonManualTrigger = mock( Trigger.class );
    when( nonManualTrigger.getKey() ).thenReturn( new TriggerKey( "validTrigger" ) );

    // Act & Assert
    assertTrue( quartzScheduler.isManualTrigger( manualTrigger ) );
    assertFalse( quartzScheduler.isManualTrigger( nonManualTrigger ) );
  }

  @Test
  public void testCreateQuartzTriggerWithSimpleJobTrigger() throws Exception {
    // Arrange
    SimpleJobTrigger simpleTrigger = new SimpleJobTrigger();
    simpleTrigger.setStartTime( new Date() );
    simpleTrigger.setRepeatInterval( 1000 );
    simpleTrigger.setRepeatCount( 5 );
    simpleTrigger.setUiPassParam( "SECONDS" );

    QuartzJobKey jobKey = new QuartzJobKey( "testJob", "testUser" );

    // Act
    MutableTrigger quartzTrigger = QuartzScheduler.createQuartzTrigger( simpleTrigger, jobKey );

    // Assert
    assertNotNull( quartzTrigger );
    assertTrue( quartzTrigger instanceof CalendarIntervalTriggerImpl );
    assertEquals( 1000, ( (CalendarIntervalTriggerImpl) quartzTrigger ).getRepeatInterval() );
  }

  @Test
  public void testCreateQuartzTriggerWithComplexJobTrigger() throws Exception {
    // Arrange
    ComplexJobTrigger complexTrigger = new ComplexJobTrigger();
    complexTrigger.setCronString( "0 0/5 * * * ?" );

    QuartzJobKey jobKey = new QuartzJobKey( "testJob", "testUser" );

    // Act
    MutableTrigger quartzTrigger = QuartzScheduler.createQuartzTrigger( complexTrigger, jobKey );

    // Assert
    assertNotNull( quartzTrigger );
    assertTrue( quartzTrigger instanceof CronTriggerImpl );
    assertEquals( "0 0/5 * * * ?", ((CronTriggerImpl) quartzTrigger).getCronExpression() );
  }

  @Test
  public void testTriggerNowUpdatesJobData() throws Exception {
    // Arrange
    // Mock JobDetail and related objects
    JobDetail mockJobDetail = mock( JobDetail.class );
    JobKey jobKey = new JobKey( "testJob\ttestGroup\trandomUuid", "testJob" );
    JobDataMap jobDataMap = new JobDataMap();

    when( mockJobDetail.getKey() ).thenReturn( jobKey );
    when( mockJobDetail.getJobDataMap() ).thenReturn( jobDataMap );
    when( mockJobDetail.getJobClass() )
      .thenAnswer( unused -> BlockingQuartzJob.class );

    // Mock Scheduler and related objects
    Scheduler mockScheduler = mock( Scheduler.class );
    Trigger mockTrigger = mock( Trigger.class );
    
    when( mockTrigger.getTriggerBuilder() ).thenAnswer( unused -> TriggerBuilder.newTrigger() );
    when( mockTrigger.getNextFireTime() ).thenReturn( new Date());

    when( mockScheduler.getJobDetail( jobKey ) ).thenReturn( mockJobDetail );
    when( mockScheduler.getTriggersOfJob( jobKey ) )
      .thenAnswer( unused -> Collections.singletonList( mockTrigger ) );

    // Mock SchedulerFactory
    SchedulerFactory mockSchedulerFactory = mock( SchedulerFactory.class );

    when( mockSchedulerFactory.getScheduler() ).thenReturn( mockScheduler );

    // Instantiate QuartzScheduler and set the mock SchedulerFactory
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    quartzScheduler.setQuartzSchedulerFactory( mockSchedulerFactory );

    // Act
    quartzScheduler.triggerNow( "testJob\ttestGroup\trandomUuid" );

    // Assert
    assertNotNull( jobDataMap.get( QuartzScheduler.RESERVEDMAPKEY_PREVIOUS_TRIGGER_NOW ) );
    verify( mockScheduler ).deleteJob( jobKey );
    verify( mockScheduler ).scheduleJob( any( JobDetail.class ), any( Trigger.class ) );
    verify( mockScheduler ).triggerJob( jobKey );
  }

  @Test
  public void testGetLastRun_PreviousTriggerNowLater() throws Exception {
    // Arrange
    // Mock JobDetail and related objects
    Date previousTriggerNow = new Date( System.currentTimeMillis() - 1000 );
    Date previousFireTime = new Date( System.currentTimeMillis() - 2000 );

    JobDetail mockJobDetail = mock( JobDetail.class );
    JobKey jobKey = new JobKey( "testJob\ttestGroup\trandomUuid", "testJob" );
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put( QuartzScheduler.RESERVEDMAPKEY_PREVIOUS_TRIGGER_NOW, previousTriggerNow );

    when( mockJobDetail.getKey() ).thenReturn( jobKey );
    when( mockJobDetail.getJobDataMap() ).thenReturn( jobDataMap );

    // Mock Scheduler and related objects
    Scheduler mockScheduler = mock( Scheduler.class );
    Trigger mockTrigger = mock( Trigger.class );

    when( mockTrigger.getJobKey() ).thenReturn( jobKey );
    when( mockTrigger.getPreviousFireTime() ).thenReturn( previousFireTime );
    when( mockScheduler.getJobDetail( jobKey ) ).thenReturn( mockJobDetail );

    // Mock SchedulerFactory
    SchedulerFactory mockSchedulerFactory = mock( SchedulerFactory.class );

    when( mockSchedulerFactory.getScheduler() ).thenReturn( mockScheduler );

    // Instantiate QuartzScheduler and set the mock SchedulerFactory
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    quartzScheduler.setQuartzSchedulerFactory( mockSchedulerFactory );

    // Act
    Date lastRun = quartzScheduler.getLastRun( mockTrigger );

    // Assert
    assertEquals( previousTriggerNow, lastRun );
  }

  @Test
  public void testGetLastRun_PreviousTriggerNowAbsent() throws Exception {
    // Arrange
    // Mock JobDetail and related objects
    Date previousFireTime = new Date( System.currentTimeMillis() - 2000 );

    JobDetail mockJobDetail = mock( JobDetail.class );
    JobKey jobKey = new JobKey( "testJob\ttestGroup\trandomUuid", "testJob" );
    JobDataMap jobDataMap = new JobDataMap();

    when( mockJobDetail.getKey() ).thenReturn( jobKey );
    when( mockJobDetail.getJobDataMap() ).thenReturn( jobDataMap );

    // Mock Scheduler and related objects
    Scheduler mockScheduler = mock( Scheduler.class );
    Trigger mockTrigger = mock( Trigger.class );

    when( mockTrigger.getJobKey() ).thenReturn( jobKey );
    when( mockTrigger.getPreviousFireTime() ).thenReturn( previousFireTime );
    when( mockScheduler.getJobDetail( jobKey ) ).thenReturn( mockJobDetail );

    // Mock SchedulerFactory
    SchedulerFactory mockSchedulerFactory = mock( SchedulerFactory.class );

    when( mockSchedulerFactory.getScheduler() ).thenReturn( mockScheduler );

    // Instantiate QuartzScheduler and set the mock SchedulerFactory
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    quartzScheduler.setQuartzSchedulerFactory( mockSchedulerFactory );

    // Act
    Date lastRun = quartzScheduler.getLastRun( mockTrigger );

    // Assert
    assertEquals( previousFireTime, lastRun );
  }

  @Test
  public void testGetLastRun_BothNull() throws Exception {
    // Arrange
    // Mock JobDetail and related objects
    JobDetail mockJobDetail = mock( JobDetail.class );
    JobKey jobKey = new JobKey( "testJob\ttestGroup\trandomUuid", "testJob" );
    JobDataMap jobDataMap = new JobDataMap();

    when( mockJobDetail.getKey() ).thenReturn( jobKey );
    when( mockJobDetail.getJobDataMap() ).thenReturn( jobDataMap );

    // Mock Scheduler and related objects
    Scheduler mockScheduler = mock( Scheduler.class );
    Trigger mockTrigger = mock( Trigger.class );

    when( mockTrigger.getJobKey() ).thenReturn( jobKey );
    when( mockTrigger.getPreviousFireTime() ).thenReturn( null );
    when( mockScheduler.getJobDetail( jobKey ) ).thenReturn( mockJobDetail );

    // Mock SchedulerFactory
    SchedulerFactory mockSchedulerFactory = mock( SchedulerFactory.class );

    when( mockSchedulerFactory.getScheduler() ).thenReturn( mockScheduler );

    // Instantiate QuartzScheduler and set the mock SchedulerFactory
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    quartzScheduler.setQuartzSchedulerFactory( mockSchedulerFactory );

    // Act
    Date lastRun = quartzScheduler.getLastRun( mockTrigger );

    // Assert
    assertNull( lastRun );
  }

  @Test
  public void testGetLastRun_PreviousTriggerNowEarlier() throws Exception {
    // Arrange
    // Mock JobDetail and related objects
    Date previousTriggerNow = new Date( System.currentTimeMillis() - 2000 );
    Date previousFireTime = new Date( System.currentTimeMillis() - 1000 );

    JobDetail mockJobDetail = mock( JobDetail.class );
    JobKey jobKey = new JobKey( "testJob\ttestGroup\trandomUuid", "testJob" );
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put( QuartzScheduler.RESERVEDMAPKEY_PREVIOUS_TRIGGER_NOW, previousTriggerNow );

    when( mockJobDetail.getKey() ).thenReturn( jobKey );
    when( mockJobDetail.getJobDataMap() ).thenReturn( jobDataMap );

    // Mock Scheduler and related objects
    Scheduler mockScheduler = mock( Scheduler.class );
    Trigger mockTrigger = mock( Trigger.class );

    when( mockTrigger.getJobKey() ).thenReturn( jobKey );
    when( mockTrigger.getPreviousFireTime() ).thenReturn( previousFireTime );
    when( mockScheduler.getJobDetail( jobKey ) ).thenReturn( mockJobDetail );

    // Mock SchedulerFactory
    SchedulerFactory mockSchedulerFactory = mock( SchedulerFactory.class );

    when( mockSchedulerFactory.getScheduler() ).thenReturn( mockScheduler );

    // Instantiate QuartzScheduler and set the mock SchedulerFactory
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    quartzScheduler.setQuartzSchedulerFactory( mockSchedulerFactory );

    // Act
    Date lastRun = quartzScheduler.getLastRun( mockTrigger );

    // Assert
    assertEquals( previousFireTime, lastRun );
  }
}
