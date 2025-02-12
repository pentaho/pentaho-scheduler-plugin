/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/

package org.pentaho.platform.scheduler2.quartz;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pentaho.platform.api.scheduler2.IScheduler.RESERVEDMAPKEY_ACTIONUSER;

public class QuartzSchedulerTest {


  private static IUnifiedRepository repo;
  private static IUnifiedRepository oldRepo;


  @BeforeClass
  public static void setUp() throws Exception {

    oldRepo = PentahoSystem.get( IUnifiedRepository.class );
    repo = mock( IUnifiedRepository.class );
    when( repo.getFile( Mockito.anyString() ) ).then( new Answer<Object>() {
      @Override public Object answer( InvocationOnMock invocationOnMock ) throws Throwable {
        final RepositoryFile repositoryFile = mock( RepositoryFile.class );
        final String param = (String) invocationOnMock.getArguments()[ 0 ];
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
      }
    } );
    PentahoSystem.registerObject( repo, IUnifiedRepository.class );
  }

  @AfterClass
  public static void tearDown() throws Exception {
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
    long futureDate = nowDate+1000000000;

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
    long pastDate = nowDate-1000000000;

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

    assertEquals( null,  job.getNextRun() );
  }

  @Test
  public void testTriggerEndTime() throws SchedulerException, org.quartz.SchedulerException {
    SimpleJobTrigger simpleJobTrigger = new SimpleJobTrigger();
    SchedulerFactory mockSchedulerFactory = mock( SchedulerFactory.class );
    Scheduler mockScheduler = mock( Scheduler.class );
    when( mockSchedulerFactory.getScheduler() ).thenReturn( mockScheduler );
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
    Job job = quartzScheduler.createJob( "fooJob", jobParams, simpleJobTrigger, null );

    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass( Trigger.class );
    verify( mockScheduler ).scheduleJob( any( JobDetail.class ), triggerCaptor.capture() );

    assertEquals( testDates.getTime().getTime(), triggerCaptor.getValue().getEndTime().getTime() );
  }

  @Test
  public void testTriggerEndTimeWithTimeZone() throws SchedulerException, org.quartz.SchedulerException {
    SimpleJobTrigger simpleJobTrigger = new SimpleJobTrigger();
    SchedulerFactory mockSchedulerFactory = mock( SchedulerFactory.class );
    Scheduler mockScheduler = mock( Scheduler.class );
    when( mockSchedulerFactory.getScheduler() ).thenReturn( mockScheduler );
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

    // convert the input time and the trigger time to GMT and verify they are the same
    assertEquals( testDates.getTime().getTime() + TimeZone.getDefault().getRawOffset(), triggerCaptor.getValue().getEndTime().getTime() + tz.getRawOffset() );
  }

}
