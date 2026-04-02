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
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
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
import org.pentaho.platform.plugin.services.exporter.ScheduleExportUtil;
import org.pentaho.platform.scheduler2.quartz.QuartzScheduler;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek;
import org.pentaho.platform.scheduler2.recur.RecurrenceList;
import org.pentaho.platform.web.http.api.resources.services.SchedulerService.InputFileInfo;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
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
  @Mock InputFileInfo inputFileInfo;
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
    when( inputFileInfo.getName() ).thenReturn( "transform.ktr" );
    when( inputFileInfo.getPath() ).thenReturn( "/home/me/transform.ktr" );
    HashMap<String, String> pdiParams = new HashMap<>();
    pdiParams.put( "pdiParam", "pdiParamValue" );
    HashMap<String, Object> result = SchedulerResourceUtil.handlePDIScheduling( inputFileInfo, params, pdiParams, false );
    assertEquals( params.size() + 4, result.size() );
    assertEquals( "transform", result.get( "transformation" ) );
    assertEquals( "home/me", result.get( "directory" ) );
    assertNotNull( result.get( "variables" ) );
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

    when( inputFileInfo.getName() ).thenReturn( "job.kjb" );
    when( inputFileInfo.getPath() ).thenReturn( "/home/me/job.kjb" );
    HashMap<String, Object> result = SchedulerResourceUtil.handlePDIScheduling( inputFileInfo, params, null, false );
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

    when( inputFileInfo.getName() ).thenReturn( "job.kjb" );
    when( inputFileInfo.getPath() ).thenReturn( "/home/me/job.kjb" );
    HashMap<String, Object> result;
    try ( MockedStatic<SchedulerResourceUtil> schedulerResourceUtilMockedStatic = mockStatic( SchedulerResourceUtil.class ) ) {
      IPdiContentProvider mockPdiContentProvider = mock( IPdiContentProvider.class );
      when( mockPdiContentProvider.getUserParameters( any() ) ).thenReturn( paramsFromJob );
      schedulerResourceUtilMockedStatic.when( SchedulerResourceUtil::getPdiContentProvider ).thenReturn( mockPdiContentProvider );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.handlePDIScheduling( any( InputFileInfo.class ), any(), any(), anyBoolean() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isPdiFile( anyString() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isTransformation( anyString() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isJob( anyString() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.loadPdiSchedulingMetadata( any(), any(), any(), anyBoolean() ) ).thenCallRealMethod();
      result = SchedulerResourceUtil.handlePDIScheduling( inputFileInfo, params, null, false );
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
    when( inputFileInfo.getName() ).thenReturn( "job.kjb" );
    when( inputFileInfo.getPath() ).thenReturn( "/home/me/job.kjb" );
    HashMap<String, String> pdiParams = new HashMap<>();
    pdiParams.put( "pdiParam", "pdiParamValue" );

    HashMap<String, Object> result = SchedulerResourceUtil.handlePDIScheduling( inputFileInfo, params, pdiParams, false );
    assertEquals( params.size() + 4, result.size() );
    assertEquals( "job", result.get( "job" ) );
    assertEquals( "home/me", result.get( "directory" ) );
    assertNotNull( result.get( "variables" ) );
    assertEquals( "pdiParamValue", ( (HashMap) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY ) ).get( "pdiParam" ) );
    assertEquals( 1, ( (HashMap<?, ?>) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY ) ).size() );
  }

  @Test
  public void testHandlePdiScheduling_notPdiFile() {
    HashMap<String, Object> params = new HashMap<>();
    params.put( "test", "value" );
    when( inputFileInfo.getName() ).thenReturn( "readme.txt" );
    HashMap<String, String> pdiParams = new HashMap<>();
    pdiParams.put( "pdiParam", "pdiParamValue" );

    HashMap<String, Object> result = SchedulerResourceUtil.handlePDIScheduling( inputFileInfo, params, pdiParams, false );
    assertEquals( params.size() + pdiParams.size() + 1, result.size() );
  }

  @Test
  public void testHandlePdiScheduling_projectVariableValueIsBlanked() {
    HashMap<String, Object> params = new HashMap<>();
    params.put( "project", "secret-project-value" );
    when( inputFileInfo.getName() ).thenReturn( "job.kjb" );
    when( inputFileInfo.getPath() ).thenReturn( "/home/me/job.kjb" );

    IPdiContentProvider provider = Mockito.mock( IPdiContentProvider.class );
    HashMap<String, String> pdiVariables = new HashMap<>();
    pdiVariables.put( "project", "original" );
    when( provider.getVariables( inputFileInfo.getPath() ) ).thenReturn( pdiVariables );

    try ( MockedStatic<SchedulerResourceUtil> schedulerUtilMockedStatic =
            Mockito.mockStatic( SchedulerResourceUtil.class, Mockito.CALLS_REAL_METHODS ) ) {
      schedulerUtilMockedStatic.when( SchedulerResourceUtil::getPdiContentProvider ).thenReturn( provider );

      HashMap<String, Object> result =
        SchedulerResourceUtil.handlePDIScheduling( inputFileInfo, params, null, false );

      Map<String, String> resultVariables = (Map<String, String>) result.get( "variables" );
      assertEquals( "", resultVariables.get( "project" ) );
      assertEquals( "secret-project-value", result.get( "project" ) );
    }
  }

  @Test
  public void testHandlePdiScheduling_ktr_isPvfs() {
    HashMap<String, Object> params = new HashMap<>();
    params.put( "test", "value" );
    when( inputFileInfo.getName() ).thenReturn( "transform.ktr" );
    when( inputFileInfo.getPath() ).thenReturn( "/home/me/transform.ktr" );

    Object mockFileObject = mock( Object.class );
    when( inputFileInfo.getFile() ).thenReturn( mockFileObject );

    HashMap<String, String> pdiParams = new HashMap<>();
    pdiParams.put( "pdiParam", "pdiParamValue" );
    HashMap<String, Object> result;
    try ( MockedStatic<SchedulerResourceUtil> schedulerResourceUtilMockedStatic = mockStatic(
      SchedulerResourceUtil.class ) ) {
      IPdiContentProvider mockPdiContentProvider = mock( IPdiContentProvider.class );
      when( mockPdiContentProvider.getUserParameters( mockFileObject ) ).thenReturn( new HashMap<>() );
      when( mockPdiContentProvider.getVariables( mockFileObject ) ).thenReturn( new HashMap<>() );
      schedulerResourceUtilMockedStatic.when( SchedulerResourceUtil::getPdiContentProvider ).thenReturn(
        mockPdiContentProvider );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.handlePDIScheduling( any(
        InputFileInfo.class ), any(), any(), anyBoolean() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isPdiFile( anyString() ) )
        .thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isTransformation( anyString() ) )
        .thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isJob( anyString() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.loadPdiSchedulingMetadata( any(), any(),
        any(), anyBoolean() ) ).thenCallRealMethod();

      result = SchedulerResourceUtil.handlePDIScheduling( inputFileInfo, params, pdiParams, true );
    }
    assertEquals( params.size() + 4, result.size() );
    assertEquals( "transform", result.get( "transformation" ) );
    assertEquals( "home/me", result.get( "directory" ) );
    assertEquals( "pdiParamValue", ( (HashMap) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY ) ).get(
      "pdiParam" ) );
    assertEquals( 1, ( (HashMap<?, ?>) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY ) ).size() );
  }

  @Test
  public void testHandlePdiScheduling_backupRestoreSeededVariable_noNPE() {
    // Variable "newVar" exists in kettleVars (declared in .kjb file) but NOT in parameterMap
    // (simulates backup/restore where schedule was created before the variable was added).
    // It should be seeded with "" via effectiveParameterMap and NOT cause an NPE.
    HashMap<String, Object> params = new HashMap<>();
    params.put( "existingParam", "existingValue" );
    when( inputFileInfo.getName() ).thenReturn( "job.kjb" );
    when( inputFileInfo.getPath() ).thenReturn( "/home/me/job.kjb" );

    IPdiContentProvider provider = Mockito.mock( IPdiContentProvider.class );
    HashMap<String, String> pdiVariables = new HashMap<>();
    pdiVariables.put( "newVar", "kettlePropsValue" );
    when( provider.getVariables( inputFileInfo.getPath() ) ).thenReturn( pdiVariables );

    try ( MockedStatic<SchedulerResourceUtil> schedulerUtilMockedStatic =
            Mockito.mockStatic( SchedulerResourceUtil.class, Mockito.CALLS_REAL_METHODS ) ) {
      schedulerUtilMockedStatic.when( SchedulerResourceUtil::getPdiContentProvider ).thenReturn( provider );

      HashMap<String, Object> result =
        SchedulerResourceUtil.handlePDIScheduling( inputFileInfo, params, null, false );

      // newVar should be present in "variables" manifest with ""
      Map<String, String> resultVariables = (Map<String, String>) result.get( "variables" );
      assertNotNull( resultVariables );
      assertEquals( "", resultVariables.get( "newVar" ) );

      // newVar should be present at root level with "" (seeded by effectiveParameterMap)
      assertEquals( "", result.get( "newVar" ) );
    }
  }

  @Test
  public void testHandlePdiScheduling_backupRestoreSeededParameters_bothKettleParamsAndVars() {
    // Test BISERVER-15478: Both kettleParams and kettleVars should be seeded into effectiveParameterMap
    // Scenario: KJB has parameters (LOG_LEVEL) and variables (PROJECT_NAME) with defaults,
    // but user clears them in the scheduler UI (not present in parameterMap).
    // They should still reach the backend as empty strings.
    HashMap<String, Object> params = new HashMap<>();
    params.put( "existingParam", "existingValue" );
    when( inputFileInfo.getName() ).thenReturn( "job.kjb" );
    when( inputFileInfo.getPath() ).thenReturn( "/home/me/job.kjb" );

    IPdiContentProvider provider = Mockito.mock( IPdiContentProvider.class );

    // KJB defines these parameters
    HashMap<String, String> pdiParameters = new HashMap<>();
    pdiParameters.put( "LOG_LEVEL", "Basic" );
    pdiParameters.put( "OUTPUT_DIR", "/default/output" );

    // KJB defines these variables
    HashMap<String, String> pdiVariables = new HashMap<>();
    pdiVariables.put( "PROJECT_NAME", "default-project" );
    pdiVariables.put( "ENV_TYPE", "dev" );

    when( provider.getUserParameters( inputFileInfo.getPath() ) ).thenReturn( pdiParameters );
    when( provider.getVariables( inputFileInfo.getPath() ) ).thenReturn( pdiVariables );

    try ( MockedStatic<SchedulerResourceUtil> schedulerUtilMockedStatic =
            Mockito.mockStatic( SchedulerResourceUtil.class, Mockito.CALLS_REAL_METHODS ) ) {
      schedulerUtilMockedStatic.when( SchedulerResourceUtil::getPdiContentProvider ).thenReturn( provider );

      HashMap<String, Object> result =
        SchedulerResourceUtil.handlePDIScheduling( inputFileInfo, params, null, false );

      // Verify that parameters from kettleParams are seeded as empty strings
      assertEquals( "", result.get( "LOG_LEVEL" ) );
      assertEquals( "", result.get( "OUTPUT_DIR" ) );

      // Verify that variables from kettleVars are seeded as empty strings
      assertEquals( "", result.get( "PROJECT_NAME" ) );
      assertEquals( "", result.get( "ENV_TYPE" ) );

      // Verify that both parameters and variables manifest are present with blanked values
      Map<String, String> resultVariables = (Map<String, String>) result.get( "variables" );
      assertNotNull( resultVariables );
      assertEquals( "", resultVariables.get( "PROJECT_NAME" ) );
      assertEquals( "", resultVariables.get( "ENV_TYPE" ) );

      // Verify pdiParameters contains both kettleParams (seeded as empty)
      Map<String, String> resultPdiParameters = (Map<String, String>) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY );
      assertNotNull( resultPdiParameters );
      assertEquals( "", resultPdiParameters.get( "LOG_LEVEL" ) );
      assertEquals( "", resultPdiParameters.get( "OUTPUT_DIR" ) );
      assertEquals( "", resultPdiParameters.get( "existingParam" ) );
    }
  }
}
