/*!
 *
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 *
 * Copyright (c) 2002-2021 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.web.http.api.resources;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.scheduler2.CronJobTrigger;
import org.pentaho.platform.api.scheduler2.IComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.IJobTrigger;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.ISimpleJobTrigger;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.api.scheduler2.wrappers.DayOfMonthWrapper;
import org.pentaho.platform.api.scheduler2.wrappers.DayOfWeekWrapper;
import org.pentaho.platform.api.scheduler2.wrappers.MonthlyWrapper;
import org.pentaho.platform.api.scheduler2.wrappers.YearlyWrapper;
import org.pentaho.platform.api.util.IPdiContentProvider;
import org.pentaho.platform.plugin.services.exporter.ScheduleExportUtil;
import org.pentaho.platform.scheduler2.quartz.QuartzScheduler;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek;
import org.pentaho.platform.scheduler2.recur.RecurrenceList;

import java.io.Serializable;
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
import static org.mockito.Matchers.anyString;
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
  }

  @Test
  public void testConvertScheduleRequestToJobTrigger_CronString() throws Exception {
    cron = new CronJobTrigger();
    cron.setCronString( "0 45 16 ? * 2#4,2L,6#4,6L *" );
    cron.setDuration( 200000 );
    cron.setStartTime( now );
    cron.setUiPassParam( "param" );
    cron.setEndTime( now );

    when( scheduleRequest.getCronJobTrigger() ).thenReturn( cron );

    QuartzScheduler quartzScheduler = new QuartzScheduler();
    IJobTrigger trigger = SchedulerResourceUtil.convertScheduleRequestToJobTrigger( scheduleRequest, quartzScheduler );
    assertTrue( trigger instanceof IComplexJobTrigger );

    IComplexJobTrigger trig = (IComplexJobTrigger) trigger;
    assertEquals( now, trig.getStartTime() );
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

  }

  @Test
  public void testUpdateStartDateForTimeZone_simple() throws Exception {
    SimpleJobTrigger sjt = new SimpleJobTrigger();
    sjt.setStartTime( now );
    when( scheduleRequest.getSimpleJobTrigger() ).thenReturn( sjt );
    when( scheduleRequest.getTimeZone() ).thenReturn( "GMT" );

    long gmtTime = now.getTime() + TimeZone.getTimeZone( "EST" ).getRawOffset();

    SchedulerResourceUtil.updateStartDateForTimeZone( scheduleRequest );
    assertEquals( gmtTime, scheduleRequest.getSimpleJobTrigger().getStartTime().getTime() );
  }

  @Test
  public void testUpdateStartDateForTimeZone_complex() throws Exception {
    ComplexJobTriggerProxy t = new ComplexJobTriggerProxy();
    t.setStartTime( now );
    when( scheduleRequest.getComplexJobTrigger() ).thenReturn( t );
    when( scheduleRequest.getTimeZone() ).thenReturn( "GMT" );

    long gmtTime = now.getTime() + TimeZone.getTimeZone( "EST" ).getRawOffset();

    SchedulerResourceUtil.updateStartDateForTimeZone( scheduleRequest );
    assertEquals( gmtTime, scheduleRequest.getComplexJobTrigger().getStartTime().getTime() );
  }

  @Test
  @Ignore
  public void testUpdateStartDateForTimeZone_cron() throws Exception {
    CronJobTrigger t = new CronJobTrigger();
    t.setStartTime( now );
    when( scheduleRequest.getCronJobTrigger() ).thenReturn( t );
    when( scheduleRequest.getTimeZone() ).thenReturn( "GMT" );

    long gmtTime = now.getTime() + TimeZone.getTimeZone( "EST" ).getRawOffset();

    SchedulerResourceUtil.updateStartDateForTimeZone( scheduleRequest );
    assertEquals( gmtTime, scheduleRequest.getCronJobTrigger().getStartTime().getTime() );
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
    assertFalse( SchedulerResourceUtil.isPdiFile( null ) );
  }

  @Test
  public void testHandlePdiScheduling_ktr() throws Exception {
    HashMap<String, Serializable> params = new HashMap<>();
    params.put( "test", "value" );
    when( repo.getName() ).thenReturn( "transform.ktr" );
    when( repo.getPath() ).thenReturn( "/home/me/transform.ktr" );
    HashMap<String, String> pdiParams = new HashMap<>();
    pdiParams.put( "pdiParam", "pdiParamValue" );
    HashMap<String, Serializable> result;
    try ( MockedStatic<SchedulerResourceUtil> schedulerResourceUtilMockedStatic = mockStatic( SchedulerResourceUtil.class ) ) {
      IPdiContentProvider mockPdiContentProvider = mock( IPdiContentProvider.class );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.getiPdiContentProvider() ).thenReturn( mockPdiContentProvider );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.handlePDIScheduling( any(), any(), any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isPdiFile( any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isTransformation( any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isJob( any() ) ).thenCallRealMethod();
      result = SchedulerResourceUtil.handlePDIScheduling( repo, params, pdiParams );
    }
    assertEquals( params.size() + 4, result.size() );
    assertEquals( "transform", result.get( "transformation" ) );
    assertEquals( "home/me", result.get( "directory" ) );
    assertEquals( "pdiParamValue", ( (HashMap) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY ) ).get( "pdiParam" ) );
    assertEquals( 1, ( (HashMap<?, ?>) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY ) ).size() );
  }

  @Test
  public void testHandlePdiScheduling_requestParamsAreTransferred() throws Exception {
    HashMap<String, Serializable> params = new HashMap<>();
    HashMap<String, String> paramsFromJob = new HashMap<>();
    params.put( "test1", "value1" );
    paramsFromJob.put( "test1", "value1" );
    params.put( "test2", "value2" );
    paramsFromJob.put( "test2", "value2" );
    params.put( "test3", "value3" );
    paramsFromJob.put( "test3", "value3" );

    when( repo.getName() ).thenReturn( "job.kjb" );
    when( repo.getPath() ).thenReturn( "/home/me/job.kjb" );
    HashMap<String, Serializable> result;
    try ( MockedStatic<SchedulerResourceUtil> schedulerResourceUtilMockedStatic = mockStatic( SchedulerResourceUtil.class ) ) {
      IPdiContentProvider mockPdiContentProvider = mock( IPdiContentProvider.class );
      when( mockPdiContentProvider.getUserParameters( anyString() ) ).thenReturn( paramsFromJob );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.getiPdiContentProvider() ).thenReturn( mockPdiContentProvider );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.handlePDIScheduling( any(), any(), any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isPdiFile( any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isTransformation( any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isJob( any() ) ).thenCallRealMethod();
      result = SchedulerResourceUtil.handlePDIScheduling( repo, params, null );
    }
    assertEquals( params.size() + 4, result.size() );
    Map<String, String> resultPdiMap = (HashMap<String, String>) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY );
    assertEquals( "value1", resultPdiMap.get( "test1" ) );
    assertEquals( "value2", resultPdiMap.get( "test2" ) );
    assertEquals( "value3", resultPdiMap.get( "test3" ) );
  }

  @Test
  public void testHandlePdiScheduling_notAllRequestParamsAreTransferred() throws Exception {
    HashMap<String, Serializable> params = new HashMap<>();
    HashMap<String, String> paramsFromJob = new HashMap<>();
    params.put( "test1", "value1" );
    paramsFromJob.put( "test1", "value1" );
    params.put( "test2", "value2" );
    paramsFromJob.put( "test2", "value2" );
    params.put( "test3", "value3" );

    when( repo.getName() ).thenReturn( "job.kjb" );
    when( repo.getPath() ).thenReturn( "/home/me/job.kjb" );
    HashMap<String, Serializable> result;
    try ( MockedStatic<SchedulerResourceUtil> schedulerResourceUtilMockedStatic = mockStatic( SchedulerResourceUtil.class ) ) {
      IPdiContentProvider mockPdiContentProvider = mock( IPdiContentProvider.class );
      when( mockPdiContentProvider.getUserParameters( anyString() ) ).thenReturn( paramsFromJob );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.getiPdiContentProvider() ).thenReturn( mockPdiContentProvider );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.handlePDIScheduling( any(), any(), any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isPdiFile( any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isTransformation( any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isJob( any() ) ).thenCallRealMethod();
      result = SchedulerResourceUtil.handlePDIScheduling( repo, params, null );
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
    HashMap<String, Serializable> params = new HashMap<>();
    params.put( "test", "value" );
    when( repo.getName() ).thenReturn( "job.kjb" );
    when( repo.getPath() ).thenReturn( "/home/me/job.kjb" );
    HashMap<String, String> pdiParams = new HashMap<>();
    pdiParams.put( "pdiParam", "pdiParamValue" );

    HashMap<String, Serializable> result;
    try ( MockedStatic<SchedulerResourceUtil> schedulerResourceUtilMockedStatic = mockStatic( SchedulerResourceUtil.class ) ) {
      IPdiContentProvider mockPdiContentProvider = mock( IPdiContentProvider.class );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.getiPdiContentProvider() ).thenReturn( mockPdiContentProvider );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.handlePDIScheduling( any(), any(), any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isPdiFile( any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isTransformation( any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isJob( any() ) ).thenCallRealMethod();
      result = SchedulerResourceUtil.handlePDIScheduling( repo, params, pdiParams );
    }
    assertEquals( params.size() + 4, result.size() );
    assertEquals( "job", result.get( "job" ) );
    assertEquals( "home/me", result.get( "directory" ) );
    assertEquals( "pdiParamValue", ( (HashMap) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY ) ).get( "pdiParam" ) );
    assertEquals( 1, ( (HashMap<?, ?>) result.get( ScheduleExportUtil.RUN_PARAMETERS_KEY ) ).size() );
  }

  @Test
  public void testHandlePdiScheduling_notPdiFile() throws Exception {
    HashMap<String, Serializable> params = new HashMap<>();
    params.put( "test", "value" );
    when( repo.getName() ).thenReturn( "readme.txt" );
    HashMap<String, String> pdiParams = new HashMap<>();
    pdiParams.put( "pdiParam", "pdiParamValue" );

    HashMap<String, Serializable> result;
    try ( MockedStatic<SchedulerResourceUtil> schedulerResourceUtilMockedStatic = mockStatic( SchedulerResourceUtil.class ) ) {
      IPdiContentProvider mockPdiContentProvider = mock( IPdiContentProvider.class );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.getiPdiContentProvider() ).thenReturn( mockPdiContentProvider );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.handlePDIScheduling( any(), any(), any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isPdiFile( any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isTransformation( any() ) ).thenCallRealMethod();
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.isJob( any() ) ).thenCallRealMethod();
      result = SchedulerResourceUtil.handlePDIScheduling( repo, params, pdiParams );
    }
    assertEquals( params.size() + pdiParams.size() + 1, result.size() );
  }
}
