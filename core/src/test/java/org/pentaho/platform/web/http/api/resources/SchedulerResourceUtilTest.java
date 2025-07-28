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


package org.pentaho.platform.web.http.api.resources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.platform.api.engine.IPluginManager;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.scheduler2.ComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.CronJobTrigger;
import org.pentaho.platform.api.scheduler2.IComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.IJobTrigger;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.ISimpleJobTrigger;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.api.scheduler2.wrappers.DayOfMonthWrapper;
import org.pentaho.platform.api.scheduler2.wrappers.DayOfWeekWrapper;
import org.pentaho.platform.api.scheduler2.wrappers.HourlyWrapper;
import org.pentaho.platform.api.scheduler2.wrappers.MinuteWrapper;
import org.pentaho.platform.api.scheduler2.wrappers.MonthlyWrapper;
import org.pentaho.platform.api.scheduler2.wrappers.YearlyWrapper;
import org.pentaho.platform.api.util.IPdiContentProvider;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.services.exporter.ScheduleExportUtil;
import org.pentaho.platform.scheduler2.quartz.QuartzScheduler;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek;
import org.pentaho.platform.scheduler2.recur.RecurrenceList;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by rfellows on 11/9/15.
 */
@RunWith( MockitoJUnitRunner.class )
public class SchedulerResourceUtilTest {

  @Mock JobScheduleRequest scheduleRequest;
  @Mock IScheduler scheduler;
  @Mock SimpleJobTrigger simple;
  @Mock RepositoryFile repo;
  CronJobTrigger cron;
  ComplexJobTriggerProxy complex;
  Date now;

  private TimeZone system;

  @Before
  public void setUp() throws Exception {
    // this makes the test non-deterministic!
    now = new Date();

    complex = new ComplexJobTriggerProxy();
    complex.setStartTime( now );

    system = TimeZone.getDefault();
    TimeZone.setDefault( TimeZone.getTimeZone( "EST" ) );
  }

  @After
  public void tearDown() {
    TimeZone.setDefault( system );
    system = null;
  }

  @Test
  public void testConvertScheduleRequestToJobTrigger_SimpleJobTrigger() throws Exception {
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    IJobTrigger trigger = SchedulerResourceUtil.convertScheduleRequestToJobTrigger( scheduleRequest, quartzScheduler );
    assertNotNull( trigger );
    assertTrue( trigger instanceof ISimpleJobTrigger );
    assertTrue( trigger.getStartTime().getTime() > System.currentTimeMillis() );
  }

  @Test
  public void testConvertScheduleRequestToJobTrigger_SimpleJobTrigger_basedOnExisting() throws Exception {
    when( scheduleRequest.getSimpleJobTrigger() ).thenReturn( simple );
    IJobTrigger trigger = SchedulerResourceUtil.convertScheduleRequestToJobTrigger( scheduleRequest, scheduler );
    assertNotNull( trigger );
    assertEquals( simple, trigger );
    verify( simple ).setStartTime( any( Date.class ) );
  }

  @Test
  public void testConvertScheduleRequestToJobTrigger_ComplexJobTrigger_daysOfMonth() throws Exception {
    complex.setDaysOfMonth( new int[] { 1, 25 } );

    when( scheduleRequest.getComplexJobTrigger() ).thenReturn( complex );
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    IJobTrigger trigger = SchedulerResourceUtil.convertScheduleRequestToJobTrigger( scheduleRequest, quartzScheduler );
    assertNotNull( trigger );
    assertTrue( trigger instanceof IComplexJobTrigger );

    IComplexJobTrigger trig = (IComplexJobTrigger) trigger;
    DayOfMonthWrapper recurrences = trig.getDayOfMonthRecurrences();
    assertEquals( 2, recurrences.size() );
    RecurrenceList rec = (RecurrenceList) recurrences.get( 0 );
    assertEquals( 1, (long) rec.getValues().get( 0 ) );
    rec = (RecurrenceList) recurrences.get( 1 );
    assertEquals( 25, (long) rec.getValues().get( 0 ) );
    ComplexJobTrigger complexJobTrigger = (ComplexJobTrigger) trig;
    HourlyWrapper hourlyWrapper = complexJobTrigger.getHourlyRecurrences();
    assertEquals( 1, hourlyWrapper.getRecurrences().size() );
    assertTrue( hourlyWrapper.getRecurrences().get( 0 ) instanceof RecurrenceList );
    assertEquals( 0, (int) ( (RecurrenceList) hourlyWrapper.getRecurrences().get( 0 ) ).getValues().get( 0 ) );
    MinuteWrapper minuteWrapper = complexJobTrigger.getMinuteRecurrences();
    assertEquals( 1, minuteWrapper.getRecurrences().size() );
    assertTrue( minuteWrapper.getRecurrences().get( 0 ) instanceof RecurrenceList );
    assertEquals( 0, (int) ( (RecurrenceList) minuteWrapper.getRecurrences().get( 0 ) ).getValues().get( 0 ) );
  }

  @Test
  public void testConvertScheduleRequestToJobTrigger_ComplexJobTrigger_monthsOfYear() throws Exception {
    complex.setMonthsOfYear( new int[] { 1, 8 } );

    when( scheduleRequest.getComplexJobTrigger() ).thenReturn( complex );
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    IJobTrigger trigger = SchedulerResourceUtil.convertScheduleRequestToJobTrigger( scheduleRequest, quartzScheduler );
    assertNotNull( trigger );
    assertTrue( trigger instanceof IComplexJobTrigger );

    IComplexJobTrigger trig = (IComplexJobTrigger) trigger;
    MonthlyWrapper recurrences = trig.getMonthlyRecurrences();
    assertEquals( 2, recurrences.size() );
    RecurrenceList rec = (RecurrenceList) recurrences.get( 0 );
    assertEquals( 2, (long) rec.getValues().get( 0 ) );
    rec = (RecurrenceList) recurrences.get( 1 );
    assertEquals( 9, (long) rec.getValues().get( 0 ) );
    ComplexJobTrigger complexJobTrigger = (ComplexJobTrigger) trig;
    HourlyWrapper hourlyWrapper = complexJobTrigger.getHourlyRecurrences();
    assertEquals( 1, hourlyWrapper.getRecurrences().size() );
    assertTrue( hourlyWrapper.getRecurrences().get( 0 ) instanceof RecurrenceList );
    assertEquals( 0, (int) ( (RecurrenceList) hourlyWrapper.getRecurrences().get( 0 ) ).getValues().get( 0 ) );
    MinuteWrapper minuteWrapper = complexJobTrigger.getMinuteRecurrences();
    assertEquals( 1, minuteWrapper.getRecurrences().size() );
    assertTrue( minuteWrapper.getRecurrences().get( 0 ) instanceof RecurrenceList );
    assertEquals( 0, (int) ( (RecurrenceList) minuteWrapper.getRecurrences().get( 0 ) ).getValues().get( 0 ) );
    
  }

  @Test
  public void testConvertScheduleRequestToJobTrigger_ComplexJobTrigger_years() throws Exception {
    complex.setYears( new int[] { 2016, 2020 } );

    when( scheduleRequest.getComplexJobTrigger() ).thenReturn( complex );
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    IJobTrigger trigger = SchedulerResourceUtil.convertScheduleRequestToJobTrigger( scheduleRequest, quartzScheduler );
    assertNotNull( trigger );
    assertTrue( trigger instanceof IComplexJobTrigger );

    IComplexJobTrigger trig = (IComplexJobTrigger) trigger;
    YearlyWrapper recurrences = trig.getYearlyRecurrences();
    assertEquals( 2, recurrences.size() );
    RecurrenceList rec = (RecurrenceList) recurrences.get( 0 );
    assertEquals( 2016, (long) rec.getValues().get( 0 ) );
    rec = (RecurrenceList) recurrences.get( 1 );
    assertEquals( 2020, (long) rec.getValues().get( 0 ) );
    ComplexJobTrigger complexJobTrigger = (ComplexJobTrigger) trig;
    HourlyWrapper hourlyWrapper = complexJobTrigger.getHourlyRecurrences();
    assertEquals( 1, hourlyWrapper.getRecurrences().size() );
    assertTrue( hourlyWrapper.getRecurrences().get( 0 ) instanceof RecurrenceList );
    assertEquals( 0, (int) ( (RecurrenceList) hourlyWrapper.getRecurrences().get( 0 ) ).getValues().get( 0 ) );
    MinuteWrapper minuteWrapper = complexJobTrigger.getMinuteRecurrences();
    assertEquals( 1, minuteWrapper.getRecurrences().size() );
    assertTrue( minuteWrapper.getRecurrences().get( 0 ) instanceof RecurrenceList );
    assertEquals( 0, (int) ( (RecurrenceList) minuteWrapper.getRecurrences().get( 0 ) ).getValues().get( 0 ) );
  }

  @Test
  public void testConvertScheduleRequestToJobTrigger_ComplexJobTrigger_daysOfWeek() throws Exception {
    complex.setDaysOfWeek( new int[] { 1, 5 } );

    when( scheduleRequest.getComplexJobTrigger() ).thenReturn( complex );
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    IJobTrigger trigger = SchedulerResourceUtil.convertScheduleRequestToJobTrigger( scheduleRequest, quartzScheduler );
    assertNotNull( trigger );
    assertTrue( trigger instanceof IComplexJobTrigger );

    IComplexJobTrigger trig = (IComplexJobTrigger) trigger;
    DayOfWeekWrapper recurrences = trig.getDayOfWeekRecurrences();
    assertEquals( 2, recurrences.size() );
    RecurrenceList recurrence = (RecurrenceList) recurrences.get( 0 );
    assertEquals( 2, (long) recurrence.getValues().get( 0 ) );
    recurrence = (RecurrenceList) recurrences.get( 1 );
    assertEquals( 6, (long) recurrence.getValues().get( 0 ) );
    ComplexJobTrigger complexJobTrigger = (ComplexJobTrigger) trig;
    HourlyWrapper hourlyWrapper = complexJobTrigger.getHourlyRecurrences();
    assertEquals( 1, hourlyWrapper.getRecurrences().size() );
    assertTrue( hourlyWrapper.getRecurrences().get( 0 ) instanceof RecurrenceList );
    assertEquals( 0, (int) ( (RecurrenceList) hourlyWrapper.getRecurrences().get( 0 ) ).getValues().get( 0 ) );
    MinuteWrapper minuteWrapper = complexJobTrigger.getMinuteRecurrences();
    assertEquals( 1, minuteWrapper.getRecurrences().size() );
    assertTrue( minuteWrapper.getRecurrences().get( 0 ) instanceof RecurrenceList );
    assertEquals( 0, (int) ( (RecurrenceList) minuteWrapper.getRecurrences().get( 0 ) ).getValues().get( 0 ) );
  }

  @Test
  public void testConvertScheduleRequestToJobTrigger_ComplexJobTrigger_weeksOfMonth() throws Exception {
    complex.setDaysOfWeek( new int[] { 1, 5 } );
    complex.setWeeksOfMonth( new int[] { 3, 4 } );

    when( scheduleRequest.getComplexJobTrigger() ).thenReturn( complex );
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    IJobTrigger trigger = SchedulerResourceUtil.convertScheduleRequestToJobTrigger( scheduleRequest, quartzScheduler );
    assertNotNull( trigger );
    assertTrue( trigger instanceof IComplexJobTrigger );

    IComplexJobTrigger trig = (IComplexJobTrigger) trigger;
    DayOfWeekWrapper recurrences =  trig.getDayOfWeekRecurrences();
    assertEquals( 4, recurrences.size() );

    QualifiedDayOfWeek rec = (QualifiedDayOfWeek) recurrences.get( 0 );
    assertEquals( "MON", rec.getDayOfWeek().toString() );
    assertEquals( "FOURTH", rec.getQualifier().toString() );

    rec = (QualifiedDayOfWeek) recurrences.get( 1 );
    assertEquals( "MON", rec.getDayOfWeek().toString() );
    assertEquals( "LAST", rec.getQualifier().toString() );

    rec = (QualifiedDayOfWeek) recurrences.get( 2 );
    assertEquals( "FRI", rec.getDayOfWeek().toString() );
    assertEquals( "FOURTH", rec.getQualifier().toString() );

    rec = (QualifiedDayOfWeek) recurrences.get( 3 );
    assertEquals( "FRI", rec.getDayOfWeek().toString() );
    assertEquals( "LAST", rec.getQualifier().toString() );
    ComplexJobTrigger complexJobTrigger = (ComplexJobTrigger) trig;
    HourlyWrapper hourlyWrapper = complexJobTrigger.getHourlyRecurrences();
    assertEquals( 1, hourlyWrapper.getRecurrences().size() );
    assertTrue( hourlyWrapper.getRecurrences().get( 0 ) instanceof RecurrenceList );
    assertEquals( 0, (int) ( (RecurrenceList) hourlyWrapper.getRecurrences().get( 0 ) ).getValues().get( 0 ) );
    MinuteWrapper minuteWrapper = complexJobTrigger.getMinuteRecurrences();
    assertEquals( 1, minuteWrapper.getRecurrences().size() );
    assertTrue( minuteWrapper.getRecurrences().get( 0 ) instanceof RecurrenceList );
    assertEquals( 0, (int) ( (RecurrenceList) minuteWrapper.getRecurrences().get( 0 ) ).getValues().get( 0 ) );
  }

  @Test
  public void testConvertScheduleRequestToJobTrigger_CronString() throws Exception {
    cron = new CronJobTrigger();
    cron.setCronString( "0 45 16 ? * 2#4,2L,6#4,6L *" );
    cron.setDuration( 200000 );
    cron.setStartTime( now );
    cron.setStartHour( now.getHours() );
    cron.setStartMin( now.getMinutes() );
    cron.setStartMonth( now.getMonth() );
    cron.setStartDay( now.getDate() );
    cron.setStartYear( now.getYear() );
    cron.setUiPassParam( "param" );
    cron.setEndTime( now );

    when( scheduleRequest.getCronJobTrigger() ).thenReturn( cron );

    QuartzScheduler quartzScheduler = new QuartzScheduler();
    IJobTrigger trigger = SchedulerResourceUtil.convertScheduleRequestToJobTrigger( scheduleRequest, quartzScheduler );
    assertTrue( trigger instanceof IComplexJobTrigger );

    IComplexJobTrigger trig = (IComplexJobTrigger) trigger;
    assertEquals( now.getHours(), trig.getStartHour() );
    assertEquals( now.getMinutes(), trig.getStartMin() );
    assertEquals( now.getMonth(), trig.getStartMonth() );
    assertEquals( now.getDate(), trig.getStartDay() );
    assertEquals( now.getYear(), trig.getStartYear() );
    assertEquals( now, trig.getEndTime() );
    assertEquals( 200000, trig.getDuration() );
    assertEquals( "param", trig.getUiPassParam() );

    DayOfWeekWrapper recurrences = trig.getDayOfWeekRecurrences();
    assertEquals( 4, recurrences.size() );

    QualifiedDayOfWeek rec = (QualifiedDayOfWeek) recurrences.get( 0 );
    assertEquals( "MON", rec.getDayOfWeek().toString() );
    assertEquals( "FOURTH", rec.getQualifier().toString() );

    rec = (QualifiedDayOfWeek) recurrences.get( 1 );
    assertEquals( "MON", rec.getDayOfWeek().toString() );
    assertEquals( "LAST", rec.getQualifier().toString() );

    rec = (QualifiedDayOfWeek) recurrences.get( 2 );
    assertEquals( "FRI", rec.getDayOfWeek().toString() );
    assertEquals( "FOURTH", rec.getQualifier().toString() );

    rec = (QualifiedDayOfWeek) recurrences.get( 3 );
    assertEquals( "FRI", rec.getDayOfWeek().toString() );
    assertEquals( "LAST", rec.getQualifier().toString() );
    ComplexJobTrigger complexJobTrigger = (ComplexJobTrigger) trig;
    HourlyWrapper hourlyWrapper = complexJobTrigger.getHourlyRecurrences();
    assertEquals( 1, hourlyWrapper.getRecurrences().size() );
    assertTrue( hourlyWrapper.getRecurrences().get( 0 ) instanceof RecurrenceList );
    assertEquals( 16, (int) ( (RecurrenceList) hourlyWrapper.getRecurrences().get( 0 ) ).getValues().get( 0 ) );
    MinuteWrapper minuteWrapper = complexJobTrigger.getMinuteRecurrences();
    assertEquals( 1, minuteWrapper.getRecurrences().size() );
    assertTrue( minuteWrapper.getRecurrences().get( 0 ) instanceof RecurrenceList );
    assertEquals( 45, (int) ( (RecurrenceList) minuteWrapper.getRecurrences().get( 0 ) ).getValues().get( 0 ) );

  }

  @Test
  public void testIsPdiFile_ktr() throws Exception {
    when( repo.getName() ).thenReturn( "transform.ktr" );
    assertTrue( SchedulerResourceUtil.isPdiFile( repo ) );
  }

  @Test
  public void testIsPdiFile_kjb() throws Exception {
    when( repo.getName() ).thenReturn( "job.kjb" );
    assertTrue( SchedulerResourceUtil.isPdiFile( repo ) );
  }

  @Test
  public void testIsPdiFile_txt() throws Exception {
    when( repo.getName() ).thenReturn( "readme.txt" );
    assertFalse( SchedulerResourceUtil.isPdiFile( repo ) );
  }

  @Test
  public void testIsPdiFile_null() throws Exception {
    assertFalse( SchedulerResourceUtil.isPdiFile( (String) null ) );
  }

  @Test
  public void testHandlePdiScheduling_ktr() throws Exception {
    HashMap<String, Object> params = new HashMap<>();
    params.put( "test", "value" );
    when( repo.getName() ).thenReturn( "transform.ktr" );
    when( repo.getPath() ).thenReturn( "/home/me/transform.ktr" );
    HashMap<String, String> pdiParams = new HashMap<>();
    pdiParams.put( "pdiParam", "pdiParamValue" );
    HashMap<String, Object> result;
    try ( MockedStatic<SchedulerResourceUtil> schedulerResourceUtilMockedStatic = mockStatic( SchedulerResourceUtil.class ) ) {
      IPdiContentProvider mockPdiContentProvider = mock( IPdiContentProvider.class );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.getiPdiContentProvider() ).thenReturn( mockPdiContentProvider );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.handlePDIScheduling( anyString(), anyString(), any(), any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isPdiFile( anyString() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isTransformation( anyString() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isJob( anyString() ) ).thenCallRealMethod();
      result = SchedulerResourceUtil.handlePDIScheduling( repo.getName(), repo.getPath(), params, pdiParams );
    }
    assertEquals( params.size() + 4, result.size() );
    assertEquals( "transform", result.get( "transformation" ) );
    assertEquals( "home/me", result.get( "directory" ) );
    assertEquals( "pdiParamValue", ( (HashMap) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY ) ).get( "pdiParam" ) );
    assertEquals( 1, ( (HashMap<?, ?>) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY ) ).size() );
  }

  @Test
  public void testHandlePdiScheduling_requestParamsAreTransferred() throws Exception {
    HashMap<String, Object> params = new HashMap<>();
    HashMap<String, String> paramsFromJob = new HashMap<>();
    params.put( "test1", "value1" );
    paramsFromJob.put( "test1", "value1" );
    params.put( "test2", "value2" );
    paramsFromJob.put( "test2", "value2" );
    params.put( "test3", "value3" );
    paramsFromJob.put( "test3", "value3" );

    when( repo.getName() ).thenReturn( "job.kjb" );
    when( repo.getPath() ).thenReturn( "/home/me/job.kjb" );
    HashMap<String, Object> result;
    try ( MockedStatic<SchedulerResourceUtil> schedulerResourceUtilMockedStatic = mockStatic( SchedulerResourceUtil.class ) ) {
      IPdiContentProvider mockPdiContentProvider = mock( IPdiContentProvider.class );
      when( mockPdiContentProvider.getUserParameters( anyString() ) ).thenReturn( paramsFromJob );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.getiPdiContentProvider() ).thenReturn( mockPdiContentProvider );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.handlePDIScheduling( anyString(), anyString(), any(), any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isPdiFile( anyString() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isTransformation( anyString() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isJob( anyString() ) ).thenCallRealMethod();
      result = SchedulerResourceUtil.handlePDIScheduling( repo.getName(), repo.getPath(), params, null );
    }
    assertEquals( params.size() + 4, result.size() );
    Map<String, String> resultPdiMap = (HashMap<String, String>) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY );
    assertEquals( "value1", resultPdiMap.get( "test1" ) );
    assertEquals( "value2", resultPdiMap.get( "test2" ) );
    assertEquals( "value3", resultPdiMap.get( "test3" ) );
  }

  @Test
  public void testHandlePdiScheduling_notAllRequestParamsAreTransferred() throws Exception {
    HashMap<String, Object> params = new HashMap<>();
    HashMap<String, String> paramsFromJob = new HashMap<>();
    params.put( "test1", "value1" );
    paramsFromJob.put( "test1", "value1" );
    params.put( "test2", "value2" );
    paramsFromJob.put( "test2", "value2" );
    params.put( "test3", "value3" );

    when( repo.getName() ).thenReturn( "job.kjb" );
    when( repo.getPath() ).thenReturn( "/home/me/job.kjb" );
    HashMap<String, Object> result;
    try ( MockedStatic<SchedulerResourceUtil> schedulerResourceUtilMockedStatic = mockStatic( SchedulerResourceUtil.class ) ) {
      IPdiContentProvider mockPdiContentProvider = mock( IPdiContentProvider.class );
      when( mockPdiContentProvider.getUserParameters( anyString() ) ).thenReturn( paramsFromJob );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.getiPdiContentProvider() ).thenReturn( mockPdiContentProvider );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.handlePDIScheduling( anyString(), anyString(), any(), any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isPdiFile( anyString() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isTransformation( anyString() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isJob( anyString() ) ).thenCallRealMethod();
      result = SchedulerResourceUtil.handlePDIScheduling( repo.getName(), repo.getPath(), params, null );
    }
    assertEquals( params.size() + 4, result.size() );
    Map<String, String> resultPdiMap = (HashMap<String, String>) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY );
    assertEquals( "value1", resultPdiMap.get( "test1" ) );
    assertEquals( "value2", resultPdiMap.get( "test2" ) );
    assertNull( resultPdiMap.get( "test3" ) );
    assertEquals( 2, resultPdiMap.size() );
  }

  @Test
  public void testHandlePdiScheduling_job() throws Exception {
    HashMap<String, Object> params = new HashMap<>();
    params.put( "test", "value" );
    when( repo.getName() ).thenReturn( "job.kjb" );
    when( repo.getPath() ).thenReturn( "/home/me/job.kjb" );
    HashMap<String, String> pdiParams = new HashMap<>();
    pdiParams.put( "pdiParam", "pdiParamValue" );

    HashMap<String, Object> result;
    try ( MockedStatic<SchedulerResourceUtil> schedulerResourceUtilMockedStatic = mockStatic( SchedulerResourceUtil.class ) ) {
      IPdiContentProvider mockPdiContentProvider = mock( IPdiContentProvider.class );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.getiPdiContentProvider() ).thenReturn( mockPdiContentProvider );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.handlePDIScheduling( anyString(), anyString(), any(), any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isPdiFile( anyString() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isTransformation( anyString() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isJob( anyString() ) ).thenCallRealMethod();
      result = SchedulerResourceUtil.handlePDIScheduling( repo.getName(), repo.getPath(), params, pdiParams );
    }
    assertEquals( params.size() + 4, result.size() );
    assertEquals( "job", result.get( "job" ) );
    assertEquals( "home/me", result.get( "directory" ) );
    assertEquals( "pdiParamValue", ( (HashMap) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY ) ).get( "pdiParam" ) );
    assertEquals( 1, ( (HashMap<?, ?>) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY ) ).size() );
  }

  @Test
  public void testHandlePdiScheduling_notPdiFile() throws Exception {
    HashMap<String, Object> params = new HashMap<>();
    params.put( "test", "value" );
    when( repo.getName() ).thenReturn( "readme.txt" );
    HashMap<String, String> pdiParams = new HashMap<>();
    pdiParams.put( "pdiParam", "pdiParamValue" );

    HashMap<String, Object> result;
    IPdiContentProvider mockPdiContentProvider = mock( IPdiContentProvider.class );
    IPluginManager mockPluginMgr = mock( IPluginManager.class );
    doReturn( mockPdiContentProvider ).when( mockPluginMgr ).getBean( IPdiContentProvider.class.getSimpleName() );
    try ( MockedStatic<PentahoSystem> phoSystemStatic = mockStatic( PentahoSystem.class ) ) {
      phoSystemStatic.when( () -> PentahoSystem.get( IPluginManager.class ) ).thenReturn( mockPluginMgr );
      result = SchedulerResourceUtil.handlePDIScheduling( repo.getName(), repo.getPath(), params, pdiParams );
    }
    assertEquals( params.size() + pdiParams.size() + 1, result.size() );
  }
}
