/*
 * ! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2025 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/

package org.pentaho.platform.web.http.api.resources.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.pentaho.platform.api.engine.IAclHolder;
import org.pentaho.platform.api.engine.IAuthorizationPolicy;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ISecurityHelper;
import org.pentaho.platform.api.genericfile.GenericFilePath;
import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.scheduler2.ComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.IBlockoutManager;
import org.pentaho.platform.api.scheduler2.IJob;
import org.pentaho.platform.api.scheduler2.IJobScheduleParam;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.api.scheduler2.wrappers.DayOfMonthWrapper;
import org.pentaho.platform.api.scheduler2.wrappers.DayOfWeekWrapper;
import org.pentaho.platform.api.scheduler2.wrappers.MonthlyWrapper;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.scheduler2.blockout.PentahoBlockoutManager;
import org.pentaho.platform.scheduler2.quartz.QuartzScheduler;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek;
import org.pentaho.platform.scheduler2.recur.RecurrenceList;
import org.pentaho.platform.web.http.api.resources.ComplexJobTriggerProxy;
import org.pentaho.platform.web.http.api.resources.JobScheduleParam;
import org.pentaho.platform.web.http.api.resources.JobScheduleRequest;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;


@SuppressWarnings( { "squid:S1874", "squid:S5738" } )
public class SchedulerServiceIntegrationTest {

  private QuartzScheduler pentahoScheduler;
  private SchedulerService schedulerService;
  private IBlockoutManager blockoutManager;
  IUnifiedRepository mockRepository;
  IAuthorizationPolicy mockPolicy;
  IGenericFileService mockGenericFileService;
  IPentahoSession mockSession;

  @Before
  public void setup() throws SchedulerException, org.pentaho.platform.api.scheduler2.SchedulerException,
    OperationFailedException {
    Properties props = new Properties();
    props.setProperty( "org.quartz.scheduler.instanceName", "TestScheduler" );
    props.setProperty( "org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore" );
    props.setProperty( "org.quartz.threadPool.threadCount", "3" );
    StdSchedulerFactory factory = new StdSchedulerFactory( props );

    pentahoScheduler = new QuartzScheduler();
    pentahoScheduler.setQuartzSchedulerFactory( factory );
    blockoutManager = new PentahoBlockoutManager();

    mockPolicy = mock( IAuthorizationPolicy.class );
    when( mockPolicy.isAllowed( anyString() ) ).thenReturn( true );

    mockRepository = mock( IUnifiedRepository.class );
    RepositoryFile mockFile = mock( RepositoryFile.class );
    Map<String, Serializable> fileMetadata = new HashMap<>();
    fileMetadata.put( RepositoryFile.SCHEDULABLE_KEY, "true" );
    when( mockFile.getName() ).thenReturn( "test-file" );
    when( mockFile.getId() ).thenReturn( "test-file-id" );
    when( mockFile.getPath() ).thenReturn( "/test/file/path" );
    when( mockRepository.getFile( anyString() ) ).thenReturn( mockFile );
    when( mockRepository.getFileMetadata( mockFile ) ).thenReturn( fileMetadata );

    mockSession = mock( IPentahoSession.class );
    when( mockSession.getName() ).thenReturn( "admin" );
    when( mockSession.getId() ).thenReturn( "test-session-id" );
    when( mockSession.getLocale() ).thenReturn( java.util.Locale.ENGLISH );
    PentahoSessionHolder.setSession( mockSession );

    SecurityHelper.setMockInstance( new MockSecurityHelper() );

    mockGenericFileService = mock( IGenericFileService.class, Mockito.CALLS_REAL_METHODS );
    doReturn( true ).when( mockGenericFileService ).doesFolderExist( anyString() );
    doReturn( true ).when( mockGenericFileService ).doesFolderExist( any( GenericFilePath.class ) );
    doReturn( true ).when( mockGenericFileService ).hasAccess( any( GenericFilePath.class ), any() );

    schedulerService = new SchedulerService();
  }

  @AfterClass
  public static void tearDown() {
    PentahoSessionHolder.setSession( null );
    SecurityHelper.setMockInstance( null );
  }

  // Helper methods to create jobs for each subclass
  private Date startDate30MinsFromNow() {
    java.util.Calendar cal = java.util.Calendar.getInstance();
    cal.setTime( new Date() );
    cal.add( Calendar.MINUTE, 30 ); // Add 30 minutes to avoid issues with current time
    cal.set( java.util.Calendar.SECOND, 0 );
    cal.set( java.util.Calendar.MILLISECOND, 0 );
    return cal.getTime();
  }

  /*
   * This first block of tests verifies that a created job can be retrieved with the same parameters
   */
  @Test
  public void testCreateAndRetrieveRunOnceJob() throws Exception {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) )
        .thenReturn( pentahoScheduler );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ) )
        .thenReturn( blockoutManager );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IAuthorizationPolicy.class ) ).thenReturn( mockPolicy );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn(
        mockRepository );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class, mockSession ) ).thenReturn(
        mockGenericFileService );
      JobScheduleRequest request = new JobScheduleRequest();
      request.setGatheringMetrics( "false" );
      request.setInputFile( "/public/pentaho-operations-mart/ba_server/content-usage-last-N.prpt" );
      request.setJobName( "content-usage-last-N-runonce" );
      List<IJobScheduleParam> params = new ArrayList<>();
      params.add( new JobScheduleParam( "period_selection", 1 ) );
      params.add( new JobScheduleParam( "start_date", "2025-07-23T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "stop_date", "2025-07-24T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "end_date_hour", "2025-07-22T00:00:01.000-0400" ) );
      params.add( new JobScheduleParam( "output-target", "table/html;page-mode=stream" ) );
      params.add( new JobScheduleParam( "accepted-page", 0 ) );
      params.add( new JobScheduleParam( "::session", "5a3a26e5-689d-11f0-a4e4-00155dcacf3f" ) );
      params.add( new JobScheduleParam( "showParameters", true ) );
      params.add( new JobScheduleParam( "renderMode", "PARAMETER" ) );
      params.add( new JobScheduleParam( "htmlProportionalWidth", false ) );
      params.add( new JobScheduleParam( "query-limit-ui-enabled", true ) );
      params.add( new JobScheduleParam( "query-limit", 0 ) );
      params.add( new JobScheduleParam( "maximum-query-limit", 0 ) );
      params.add( new JobScheduleParam( "ActionAdapterQuartzJob-ActionUser", "admin" ) );
      request.setJobParameters( params );
      request.setLogLevel( "Basic" );
      request.setOutputFile( "/home/admin" );
      request.setRunSafeMode( "false" );
      request.setTimeZone( "America/New_York" );
      SimpleJobTrigger trigger = new SimpleJobTrigger();
      Date startTime = startDate30MinsFromNow();
      trigger.setStartYear( startTime.getYear() );
      trigger.setStartMonth( startTime.getMonth() );
      trigger.setStartDay( startTime.getDate() );
      trigger.setStartHour( startTime.getHours() );
      trigger.setStartMin( startTime.getMinutes() );
      trigger.setTimeZone( "America/New_York" );
      trigger.setRepeatInterval( -1 );
      trigger.setUiPassParam( "RUN_ONCE" );
      request.setSimpleJobTrigger( trigger );

      Job createdJob = schedulerService.createJob( request );
      List<IJob> jobs = schedulerService.getJobs();
      boolean found = false;
      for ( IJob job : jobs ) {
        if ( job.getJobId().equals( createdJob.getJobId() ) ) {
          found = true;
          Job returnedJob = (Job) job;
          assertEquals( request.getJobName(), returnedJob.getJobName() );
          assertEquals( request.getInputFile(), returnedJob.getJobParams().get(
            QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER_INPUTFILE ) );
          assertEquals( request.getLogLevel(), returnedJob.getJobParams().get( "logLevel" ) );
          assertEquals( request.getRunSafeMode(), returnedJob.getJobParams().get( "runSafeMode" ) );
          SimpleJobTrigger retrievedTrigger = (SimpleJobTrigger) returnedJob.getJobTrigger();
          assertEquals( trigger.getUiPassParam(), retrievedTrigger.getUiPassParam() );
          assertEquals( trigger.getRepeatInterval(), retrievedTrigger.getRepeatInterval() );
          assertEquals( trigger.getTimeZone(), retrievedTrigger.getTimeZone() );
        }
      }
      assertTrue( found );
    }
  }

  @Test
  public void testCreateAndRetrieveEvery90SecondsJob() throws Exception {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) )
        .thenReturn( pentahoScheduler );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ) )
        .thenReturn( blockoutManager );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IAuthorizationPolicy.class ) ).thenReturn( mockPolicy );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn(
        mockRepository );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class, mockSession ) ).thenReturn(
        mockGenericFileService );
      JobScheduleRequest request = new JobScheduleRequest();
      request.setGatheringMetrics( "false" );
      request.setInputFile( "/public/pentaho-operations-mart/ba_server/content-usage-last-N.prpt" );
      request.setJobName( "content-usage-last-N" );
      List<IJobScheduleParam> params = new ArrayList<>();
      params.add( new JobScheduleParam( "period_selection", 1 ) );
      params.add( new JobScheduleParam( "start_date", "2025-07-23T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "stop_date", "2025-07-24T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "end_date_hour", "2025-07-22T00:00:01.000-0400" ) );
      params.add( new JobScheduleParam( "output-target", "table/html;page-mode=stream" ) );
      params.add( new JobScheduleParam( "accepted-page", 0 ) );
      params.add( new JobScheduleParam( "::session", "9f3b408c-689d-11f0-a4e4-00155dcacf3f" ) );
      params.add( new JobScheduleParam( "showParameters", true ) );
      params.add( new JobScheduleParam( "renderMode", "PARAMETER" ) );
      params.add( new JobScheduleParam( "htmlProportionalWidth", false ) );
      params.add( new JobScheduleParam( "query-limit-ui-enabled", true ) );
      params.add( new JobScheduleParam( "query-limit", 0 ) );
      params.add( new JobScheduleParam( "maximum-query-limit", 0 ) );
      params.add( new JobScheduleParam( "lineage-id", "bbc043ca-d66c-4378-b7c2-bb48ed8aee39" ) );
      params.add( new JobScheduleParam( "ActionAdapterQuartzJob-ActionUser", "admin" ) );
      request.setJobParameters( params );
      request.setLogLevel( "Basic" );
      request.setOutputFile( "/home/admin" );
      request.setRunSafeMode( "false" );
      request.setTimeZone( "America/New_York" );
      SimpleJobTrigger trigger = new SimpleJobTrigger();
      Date startTime = startDate30MinsFromNow();
      trigger.setStartYear( startTime.getYear() );
      trigger.setStartMonth( startTime.getMonth() );
      trigger.setStartDay( startTime.getDate() );
      trigger.setStartHour( startTime.getHours() );
      trigger.setStartMin( startTime.getMinutes() );
      trigger.setTimeZone( "America/New_York" );
      trigger.setRepeatInterval( 90 );
      trigger.setUiPassParam( "SECONDS" );
      request.setSimpleJobTrigger( trigger );

      Job createdJob = schedulerService.createJob( request );
      List<IJob> jobs = schedulerService.getJobs();
      boolean found = false;
      for ( IJob job : jobs ) {
        if ( job.getJobId().equals( createdJob.getJobId() ) ) {
          found = true;
          Job returnedJob = (Job) job;
          assertEquals( request.getJobName(), returnedJob.getJobName() );
          assertEquals( request.getInputFile(), returnedJob.getJobParams().get(
            QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER_INPUTFILE ) );
          assertEquals( request.getLogLevel(), returnedJob.getJobParams().get( "logLevel" ) );
          assertEquals( request.getRunSafeMode(), returnedJob.getJobParams().get( "runSafeMode" ) );
          SimpleJobTrigger retrievedTrigger = (SimpleJobTrigger) returnedJob.getJobTrigger();
          assertEquals( trigger.getUiPassParam(), retrievedTrigger.getUiPassParam() );
          assertEquals( trigger.getRepeatInterval(), retrievedTrigger.getRepeatInterval() );
          assertEquals( trigger.getTimeZone(), retrievedTrigger.getTimeZone() );
        }
      }
      assertTrue( found );
    }
  }

  @Test
  public void testCreateAndRetrieveEveryWeekdayJob() throws Exception {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) )
        .thenReturn( pentahoScheduler );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ) )
        .thenReturn( blockoutManager );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IAuthorizationPolicy.class ) ).thenReturn( mockPolicy );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn(
        mockRepository );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class, mockSession ) ).thenReturn(
        mockGenericFileService );
      JobScheduleRequest request = new JobScheduleRequest();
      request.setGatheringMetrics( "false" );
      request.setInputFile( "/public/pentaho-operations-mart/ba_server/content-usage-last-N.prpt" );
      request.setJobName( "content-usage-last-N" );
      List<IJobScheduleParam> params = new ArrayList<>();
      params.add( new JobScheduleParam( "period_selection", 1 ) );
      params.add( new JobScheduleParam( "start_date", "2025-07-23T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "stop_date", "2025-07-24T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "end_date_hour", "2025-07-22T00:00:01.000-0400" ) );
      params.add( new JobScheduleParam( "output-target", "table/html;page-mode=stream" ) );
      params.add( new JobScheduleParam( "accepted-page", 0 ) );
      params.add( new JobScheduleParam( "::session", "bd31ff83-689d-11f0-a4e4-00155dcacf3f" ) );
      params.add( new JobScheduleParam( "showParameters", true ) );
      params.add( new JobScheduleParam( "renderMode", "PARAMETER" ) );
      params.add( new JobScheduleParam( "htmlProportionalWidth", false ) );
      params.add( new JobScheduleParam( "query-limit-ui-enabled", true ) );
      params.add( new JobScheduleParam( "query-limit", 0 ) );
      params.add( new JobScheduleParam( "maximum-query-limit", 0 ) );
      params.add( new JobScheduleParam( "lineage-id", "bbc043ca-d66c-4378-b7c2-bb48ed8aee39" ) );
      params.add( new JobScheduleParam( "ActionAdapterQuartzJob-ActionUser", "admin" ) );
      request.setJobParameters( params );
      request.setLogLevel( "Basic" );
      request.setOutputFile( "/home/admin" );
      request.setRunSafeMode( "false" );
      request.setTimeZone( "America/New_York" );
      ComplexJobTriggerProxy trigger = new ComplexJobTriggerProxy();
      Date startTime = startDate30MinsFromNow();
      trigger.setStartYear( startTime.getYear() );
      trigger.setStartMonth( startTime.getMonth() );
      trigger.setStartDay( startTime.getDate() );
      trigger.setStartHour( startTime.getHours() );
      trigger.setStartMin( startTime.getMinutes() );
      trigger.setDaysOfWeek( new int[] { 1, 2, 3, 4, 5 } );
      trigger.setUiPassParam( "DAILY" );
      request.setComplexJobTrigger( trigger );

      Job createdJob = schedulerService.createJob( request );
      List<IJob> jobs = schedulerService.getJobs();
      boolean found = false;
      for ( IJob job : jobs ) {
        if ( job.getJobId().equals( createdJob.getJobId() ) ) {
          found = true;
          Job returnedJob = (Job) job;
          assertEquals( request.getJobName(), returnedJob.getJobName() );
          assertEquals( request.getInputFile(), returnedJob.getJobParams().get(
            QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER_INPUTFILE ) );
          assertEquals( request.getLogLevel(), returnedJob.getJobParams().get( "logLevel" ) );
          assertEquals( request.getRunSafeMode(), returnedJob.getJobParams().get( "runSafeMode" ) );

          ComplexJobTrigger retrievedTrigger = (ComplexJobTrigger) returnedJob.getJobTrigger();
          assertEquals( request.getTimeZone(), retrievedTrigger.getTimeZone() );
          assertEquals( trigger.getUiPassParam(), retrievedTrigger.getUiPassParam() );
        }
      }
      assertTrue( found );
    }
  }


  @Test
  public void testCreateAndRetrieveEvery3DaysIgnoreDSTJob() throws Exception {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) )
        .thenReturn( pentahoScheduler );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ) )
        .thenReturn( blockoutManager );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IAuthorizationPolicy.class ) ).thenReturn( mockPolicy );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn(
        mockRepository );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class, mockSession ) ).thenReturn(
        mockGenericFileService );
      JobScheduleRequest request = new JobScheduleRequest();
      request.setGatheringMetrics( "false" );
      request.setInputFile( "/public/pentaho-operations-mart/ba_server/content-usage-last-N.prpt" );
      request.setJobName( "content-usage-last-N-every3days-ignoreDST" );
      List<IJobScheduleParam> params = new ArrayList<>();
      params.add( new JobScheduleParam( "period_selection", 1 ) );
      params.add( new JobScheduleParam( "start_date", "2025-07-23T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "stop_date", "2025-07-24T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "end_date_hour", "2025-07-22T00:00:01.000-0400" ) );
      params.add( new JobScheduleParam( "output-target", "table/html;page-mode=stream" ) );
      params.add( new JobScheduleParam( "accepted-page", 0 ) );
      params.add( new JobScheduleParam( "::session", "e3d1d1e3-689d-11f0-a4e4-00155dcacf3f" ) );
      params.add( new JobScheduleParam( "showParameters", true ) );
      params.add( new JobScheduleParam( "renderMode", "PARAMETER" ) );
      params.add( new JobScheduleParam( "htmlProportionalWidth", false ) );
      params.add( new JobScheduleParam( "query-limit-ui-enabled", true ) );
      params.add( new JobScheduleParam( "query-limit", 0 ) );
      params.add( new JobScheduleParam( "maximum-query-limit", 0 ) );
      params.add( new JobScheduleParam( "ActionAdapterQuartzJob-ActionUser", "admin" ) );
      request.setJobParameters( params );
      request.setLogLevel( "Basic" );
      request.setOutputFile( "/home/admin" );
      request.setRunSafeMode( "false" );
      request.setTimeZone( "America/New_York" );
      ComplexJobTriggerProxy trigger = new ComplexJobTriggerProxy();
      Date startTime = startDate30MinsFromNow();
      trigger.setStartYear( startTime.getYear() );
      trigger.setStartMonth( startTime.getMonth() );
      trigger.setStartDay( startTime.getDate() );
      trigger.setStartHour( startTime.getHours() );
      trigger.setStartMin( startTime.getMinutes() );
      trigger.setRepeatInterval( 3 );
      trigger.setUiPassParam( "DAILY" );
      request.setComplexJobTrigger( trigger );
      Job createdJob = schedulerService.createJob( request );
      List<IJob> jobs = schedulerService.getJobs();
      boolean found = false;
      for ( IJob job : jobs ) {
        if ( job.getJobId().equals( createdJob.getJobId() ) ) {
          found = true;
          Job returnedJob = (Job) job;
          assertEquals( request.getJobName(), returnedJob.getJobName() );
          assertEquals( request.getInputFile(), returnedJob.getJobParams().get(
            QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER_INPUTFILE ) );
          assertEquals( request.getLogLevel(), returnedJob.getJobParams().get( "logLevel" ) );
          assertEquals( request.getRunSafeMode(), returnedJob.getJobParams().get( "runSafeMode" ) );

          ComplexJobTrigger retrievedTrigger = (ComplexJobTrigger) returnedJob.getJobTrigger();
          assertEquals( request.getTimeZone(), retrievedTrigger.getTimeZone() );
          assertEquals( trigger.getUiPassParam(), retrievedTrigger.getUiPassParam() );
        }
      }
      assertTrue( found );
    }
  }

  @Test
  public void testCreateAndRetrieveEvery3DaysNoIgnoreDSTJob() throws Exception {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) )
        .thenReturn( pentahoScheduler );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ) )
        .thenReturn( blockoutManager );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IAuthorizationPolicy.class ) ).thenReturn( mockPolicy );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn(
        mockRepository );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class, mockSession ) ).thenReturn(
        mockGenericFileService );
      JobScheduleRequest request = new JobScheduleRequest();
      request.setGatheringMetrics( "false" );
      request.setInputFile( "/public/pentaho-operations-mart/ba_server/content-usage-last-N.prpt" );
      request.setJobName( "content-usage-last-N-every3days-noignoreDST" );
      List<IJobScheduleParam> params = new ArrayList<>();
      params.add( new JobScheduleParam( "period_selection", 1 ) );
      params.add( new JobScheduleParam( "start_date", "2025-07-23T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "stop_date", "2025-07-24T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "end_date_hour", "2025-07-22T00:00:01.000-0400" ) );
      params.add( new JobScheduleParam( "output-target", "table/html;page-mode=stream" ) );
      params.add( new JobScheduleParam( "accepted-page", 0 ) );
      params.add( new JobScheduleParam( "::session", "e3d1d1e3-689d-11f0-a4e4-00155dcacf3f" ) );
      params.add( new JobScheduleParam( "showParameters", true ) );
      params.add( new JobScheduleParam( "renderMode", "PARAMETER" ) );
      params.add( new JobScheduleParam( "htmlProportionalWidth", false ) );
      params.add( new JobScheduleParam( "query-limit-ui-enabled", true ) );
      params.add( new JobScheduleParam( "query-limit", 0 ) );
      params.add( new JobScheduleParam( "maximum-query-limit", 0 ) );
      params.add( new JobScheduleParam( "ActionAdapterQuartzJob-ActionUser", "admin" ) );
      request.setJobParameters( params );
      request.setLogLevel( "Basic" );
      request.setOutputFile( "/home/admin" );
      request.setRunSafeMode( "false" );
      request.setTimeZone( "America/New_York" );
      ComplexJobTriggerProxy trigger = new ComplexJobTriggerProxy();
      Date startTime = startDate30MinsFromNow();
      trigger.setStartYear( startTime.getYear() );
      trigger.setStartMonth( startTime.getMonth() );
      trigger.setStartDay( startTime.getDate() );
      trigger.setStartHour( startTime.getHours() );
      trigger.setStartMin( startTime.getMinutes() );
      trigger.setRepeatInterval( 3 );
      trigger.setUiPassParam( "DAILY" );
      request.setComplexJobTrigger( trigger );
      Job createdJob = schedulerService.createJob( request );
      List<IJob> jobs = schedulerService.getJobs();
      boolean found = false;
      for ( IJob job : jobs ) {
        if ( job.getJobId().equals( createdJob.getJobId() ) ) {
          found = true;
          Job returnedJob = (Job) job;
          assertEquals( request.getJobName(), returnedJob.getJobName() );
          assertEquals( request.getInputFile(), returnedJob.getJobParams().get(
            QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER_INPUTFILE ) );
          assertEquals( request.getLogLevel(), returnedJob.getJobParams().get( "logLevel" ) );
          assertEquals( request.getRunSafeMode(), returnedJob.getJobParams().get( "runSafeMode" ) );

          ComplexJobTrigger retrievedTrigger = (ComplexJobTrigger) returnedJob.getJobTrigger();
          assertEquals( request.getTimeZone(), retrievedTrigger.getTimeZone() );
          assertEquals( trigger.getUiPassParam(), retrievedTrigger.getUiPassParam() );
        }
      }
      assertTrue( found );
    }
  }

  @Test
  public void testCreateAndRetrieveWeeklyMWFJob() throws Exception {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) )
        .thenReturn( pentahoScheduler );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ) )
        .thenReturn( blockoutManager );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IAuthorizationPolicy.class ) ).thenReturn( mockPolicy );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn(
        mockRepository );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class, mockSession ) ).thenReturn(
        mockGenericFileService );
      JobScheduleRequest request = new JobScheduleRequest();
      request.setGatheringMetrics( "false" );
      request.setInputFile( "/public/pentaho-operations-mart/ba_server/content-usage-last-N.prpt" );
      request.setJobName( "content-usage-last-N-weeklyMWF" );
      List<IJobScheduleParam> params = new ArrayList<>();
      params.add( new JobScheduleParam( "period_selection", 1 ) );
      params.add( new JobScheduleParam( "start_date", "2025-07-23T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "stop_date", "2025-07-24T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "end_date_hour", "2025-07-22T00:00:01.000-0400" ) );
      params.add( new JobScheduleParam( "output-target", "table/html;page-mode=stream" ) );
      params.add( new JobScheduleParam( "accepted-page", 0 ) );
      params.add( new JobScheduleParam( "::session", "mwf-689d-11f0-a4e4-00155dcacf3f" ) );
      params.add( new JobScheduleParam( "showParameters", true ) );
      params.add( new JobScheduleParam( "renderMode", "PARAMETER" ) );
      params.add( new JobScheduleParam( "htmlProportionalWidth", false ) );
      params.add( new JobScheduleParam( "query-limit-ui-enabled", true ) );
      params.add( new JobScheduleParam( "query-limit", 0 ) );
      params.add( new JobScheduleParam( "maximum-query-limit", 0 ) );
      params.add( new JobScheduleParam( "ActionAdapterQuartzJob-ActionUser", "admin" ) );
      request.setJobParameters( params );
      request.setLogLevel( "Basic" );
      request.setOutputFile( "/home/admin" );
      request.setRunSafeMode( "false" );
      request.setTimeZone( "America/New_York" );
      ComplexJobTriggerProxy trigger = new ComplexJobTriggerProxy();
      Date startTime = startDate30MinsFromNow();
      trigger.setStartYear( startTime.getYear() );
      trigger.setStartMonth( startTime.getMonth() );
      trigger.setStartDay( startTime.getDate() );
      trigger.setStartHour( startTime.getHours() );
      trigger.setStartMin( startTime.getMinutes() );
      trigger.setDaysOfWeek( new int[] { 2, 4, 6 } ); // Monday, Wednesday, Friday
      trigger.setUiPassParam( "WEEKLY" );
      request.setComplexJobTrigger( trigger );
      Job createdJob = schedulerService.createJob( request );
      List<IJob> jobs = schedulerService.getJobs();
      boolean found = false;
      for ( IJob job : jobs ) {
        if ( job.getJobId().equals( createdJob.getJobId() ) ) {
          found = true;
          Job returnedJob = (Job) job;
          assertEquals( request.getJobName(), returnedJob.getJobName() );
          assertEquals( request.getInputFile(), returnedJob.getJobParams().get(
            QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER_INPUTFILE ) );
          assertEquals( request.getLogLevel(), returnedJob.getJobParams().get( "logLevel" ) );
          assertEquals( request.getRunSafeMode(), returnedJob.getJobParams().get( "runSafeMode" ) );

          ComplexJobTrigger retrievedTrigger = (ComplexJobTrigger) returnedJob.getJobTrigger();
          assertEquals( request.getTimeZone(), retrievedTrigger.getTimeZone() );
          assertEquals( trigger.getUiPassParam(), retrievedTrigger.getUiPassParam() );

        }
      }
      assertTrue( found );
    }
  }

  @Test
  public void testCreateAndRetrieve17thOfMonthJob() throws Exception {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) )
        .thenReturn( pentahoScheduler );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ) )
        .thenReturn( blockoutManager );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IAuthorizationPolicy.class ) ).thenReturn( mockPolicy );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn(
        mockRepository );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class, mockSession ) ).thenReturn(
        mockGenericFileService );
      JobScheduleRequest request = new JobScheduleRequest();
      request.setGatheringMetrics( "false" );
      request.setInputFile( "/public/pentaho-operations-mart/ba_server/content-usage-last-N.prpt" );
      request.setJobName( "content-usage-last-N-17thOfMonth" );
      List<IJobScheduleParam> params = new ArrayList<>();
      params.add( new JobScheduleParam( "period_selection", 1 ) );
      params.add( new JobScheduleParam( "start_date", "2025-07-23T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "stop_date", "2025-07-24T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "end_date_hour", "2025-07-22T00:00:01.000-0400" ) );
      params.add( new JobScheduleParam( "output-target", "table/html;page-mode=stream" ) );
      params.add( new JobScheduleParam( "accepted-page", 0 ) );
      params.add( new JobScheduleParam( "::session", "17th-689d-11f0-a4e4-00155dcacf3f" ) );
      params.add( new JobScheduleParam( "showParameters", true ) );
      params.add( new JobScheduleParam( "renderMode", "PARAMETER" ) );
      params.add( new JobScheduleParam( "htmlProportionalWidth", false ) );
      params.add( new JobScheduleParam( "query-limit-ui-enabled", true ) );
      params.add( new JobScheduleParam( "query-limit", 0 ) );
      params.add( new JobScheduleParam( "maximum-query-limit", 0 ) );
      params.add( new JobScheduleParam( "ActionAdapterQuartzJob-ActionUser", "admin" ) );
      request.setJobParameters( params );
      request.setLogLevel( "Basic" );
      request.setOutputFile( "/home/admin" );
      request.setRunSafeMode( "false" );
      request.setTimeZone( "America/New_York" );
      ComplexJobTriggerProxy trigger = new ComplexJobTriggerProxy();
      Date startTime = startDate30MinsFromNow();
      trigger.setStartYear( startTime.getYear() );
      trigger.setStartMonth( startTime.getMonth() );
      trigger.setStartDay( startTime.getDate() );
      trigger.setStartHour( startTime.getHours() );
      trigger.setStartMin( startTime.getMinutes() );
      trigger.setDaysOfMonth( new int[] { 17 } );
      trigger.setUiPassParam( "MONTHLY" );
      request.setComplexJobTrigger( trigger );
      Job createdJob = schedulerService.createJob( request );
      List<IJob> jobs = schedulerService.getJobs();
      boolean found = false;
      for ( IJob job : jobs ) {
        if ( job.getJobId().equals( createdJob.getJobId() ) ) {
          found = true;
          Job returnedJob = (Job) job;
          assertEquals( request.getJobName(), returnedJob.getJobName() );
          assertEquals( request.getInputFile(), returnedJob.getJobParams().get(
            QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER_INPUTFILE ) );
          assertEquals( request.getLogLevel(), returnedJob.getJobParams().get( "logLevel" ) );
          assertEquals( request.getRunSafeMode(), returnedJob.getJobParams().get( "runSafeMode" ) );

          ComplexJobTrigger retrievedTrigger = (ComplexJobTrigger) returnedJob.getJobTrigger();
          assertEquals( request.getTimeZone(), retrievedTrigger.getTimeZone() );
          assertEquals( trigger.getUiPassParam(), retrievedTrigger.getUiPassParam() );
        }
      }
      assertTrue( found );
    }
  }

  @Test
  public void testCreateAndRetrieveThirdTuesdayJob() throws Exception {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) )
        .thenReturn( pentahoScheduler );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ) )
        .thenReturn( blockoutManager );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IAuthorizationPolicy.class ) ).thenReturn( mockPolicy );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn(
        mockRepository );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class, mockSession ) ).thenReturn(
        mockGenericFileService );
      JobScheduleRequest request = new JobScheduleRequest();
      request.setGatheringMetrics( "false" );
      request.setInputFile( "/public/pentaho-operations-mart/ba_server/content-usage-last-N.prpt" );
      request.setJobName( "content-usage-last-N-thirdTuesday" );
      List<IJobScheduleParam> params = new ArrayList<>();
      params.add( new JobScheduleParam( "period_selection", 1 ) );
      params.add( new JobScheduleParam( "start_date", "2025-07-23T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "stop_date", "2025-07-24T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "end_date_hour", "2025-07-22T00:00:01.000-0400" ) );
      params.add( new JobScheduleParam( "output-target", "table/html;page-mode=stream" ) );
      params.add( new JobScheduleParam( "accepted-page", 0 ) );
      params.add( new JobScheduleParam( "::session", "thirdtue-689d-11f0-a4e4-00155dcacf3f" ) );
      params.add( new JobScheduleParam( "showParameters", true ) );
      params.add( new JobScheduleParam( "renderMode", "PARAMETER" ) );
      params.add( new JobScheduleParam( "htmlProportionalWidth", false ) );
      params.add( new JobScheduleParam( "query-limit-ui-enabled", true ) );
      params.add( new JobScheduleParam( "query-limit", 0 ) );
      params.add( new JobScheduleParam( "maximum-query-limit", 0 ) );
      params.add( new JobScheduleParam( "ActionAdapterQuartzJob-ActionUser", "admin" ) );
      request.setJobParameters( params );
      request.setLogLevel( "Basic" );
      request.setOutputFile( "/home/admin" );
      request.setRunSafeMode( "false" );
      request.setTimeZone( "America/New_York" );
      ComplexJobTriggerProxy trigger = new ComplexJobTriggerProxy();
      Date startTime = startDate30MinsFromNow();
      trigger.setStartYear( startTime.getYear() );
      trigger.setStartMonth( startTime.getMonth() );
      trigger.setStartDay( startTime.getDate() );
      trigger.setStartHour( startTime.getHours() );
      trigger.setStartMin( startTime.getMinutes() );
      trigger.setDaysOfWeek( new int[] { 3 } ); // Tuesday
      trigger.setWeeksOfMonth( new int[] { 3 } ); // Third week
      trigger.setUiPassParam( "MONTHLY" );
      request.setComplexJobTrigger( trigger );
      Job createdJob = schedulerService.createJob( request );
      List<IJob> jobs = schedulerService.getJobs();
      boolean found = false;
      for ( IJob job : jobs ) {
        if ( job.getJobId().equals( createdJob.getJobId() ) ) {
          found = true;
          Job returnedJob = (Job) job;
          assertEquals( request.getJobName(), returnedJob.getJobName() );
          assertEquals( request.getInputFile(), returnedJob.getJobParams().get(
            QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER_INPUTFILE ) );
          assertEquals( request.getLogLevel(), returnedJob.getJobParams().get( "logLevel" ) );
          assertEquals( request.getRunSafeMode(), returnedJob.getJobParams().get( "runSafeMode" ) );

          ComplexJobTrigger retrievedTrigger = (ComplexJobTrigger) returnedJob.getJobTrigger();
          assertEquals( request.getTimeZone(), retrievedTrigger.getTimeZone() );
          assertEquals( trigger.getUiPassParam(), retrievedTrigger.getUiPassParam() );
          assertEquals( trigger.getDaysOfWeek()[ 0 ], ( (QualifiedDayOfWeek) retrievedTrigger.getDayOfWeekRecurrences()
            .get( 0 ) ).getDayOfWeek().ordinal() );
          assertEquals( trigger.getWeeksOfMonth()[ 0 ], ( (QualifiedDayOfWeek) retrievedTrigger
            .getDayOfWeekRecurrences().get( 0 ) ).getQualifier().ordinal() );
        }
      }
      assertTrue( found );
    }
  }

  @Test
  public void testCreateAndRetrieveEveryApril8thJob() throws Exception {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) )
        .thenReturn( pentahoScheduler );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ) )
        .thenReturn( blockoutManager );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IAuthorizationPolicy.class ) ).thenReturn( mockPolicy );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn(
        mockRepository );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class, mockSession ) ).thenReturn(
        mockGenericFileService );
      JobScheduleRequest request = new JobScheduleRequest();
      request.setGatheringMetrics( "false" );
      request.setInputFile( "/public/pentaho-operations-mart/ba_server/content-usage-last-N.prpt" );
      request.setJobName( "content-usage-last-N-everyApril8th" );
      List<IJobScheduleParam> params = new ArrayList<>();
      params.add( new JobScheduleParam( "period_selection", 1 ) );
      params.add( new JobScheduleParam( "start_date", "2025-07-23T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "stop_date", "2025-07-24T00:00:00.000-0400" ) );
      params.add( new JobScheduleParam( "end_date_hour", "2025-07-22T00:00:01.000-0400" ) );
      params.add( new JobScheduleParam( "output-target", "table/html;page-mode=stream" ) );
      params.add( new JobScheduleParam( "accepted-page", 0 ) );
      params.add( new JobScheduleParam( "::session", "april8th-689d-11f0-a4e4-00155dcacf3f" ) );
      params.add( new JobScheduleParam( "showParameters", true ) );
      params.add( new JobScheduleParam( "renderMode", "PARAMETER" ) );
      params.add( new JobScheduleParam( "htmlProportionalWidth", false ) );
      params.add( new JobScheduleParam( "query-limit-ui-enabled", true ) );
      params.add( new JobScheduleParam( "query-limit", 0 ) );
      params.add( new JobScheduleParam( "maximum-query-limit", 0 ) );
      params.add( new JobScheduleParam( "ActionAdapterQuartzJob-ActionUser", "admin" ) );
      request.setJobParameters( params );
      request.setLogLevel( "Basic" );
      request.setOutputFile( "/home/admin" );
      request.setRunSafeMode( "false" );
      request.setTimeZone( "America/New_York" );
      ComplexJobTriggerProxy trigger = new ComplexJobTriggerProxy();
      Date startTime = startDate30MinsFromNow();
      trigger.setStartYear( startTime.getYear() );
      trigger.setStartMonth( startTime.getMonth() );
      trigger.setStartDay( startTime.getDate() );
      trigger.setStartHour( startTime.getHours() );
      trigger.setStartMin( startTime.getMinutes() );
      trigger.setDaysOfMonth( new int[] { 8 } );
      trigger.setMonthsOfYear( new int[] { 4 } ); // April
      trigger.setUiPassParam( "YEARLY" );
      request.setComplexJobTrigger( trigger );
      Job createdJob = schedulerService.createJob( request );
      List<IJob> jobs = schedulerService.getJobs();
      boolean found = false;
      for ( IJob job : jobs ) {
        if ( job.getJobId().equals( createdJob.getJobId() ) ) {
          found = true;
          ComplexJobTrigger retrievedTrigger = (ComplexJobTrigger) job.getJobTrigger();
          assertEquals( trigger.getUiPassParam(), retrievedTrigger.getUiPassParam() );
          assertEquals( trigger.getMonthsOfYear()[ 0 ], ( (RecurrenceList) retrievedTrigger.getMonthlyRecurrences()
            .getRecurrences().get( 0 ) ).getValues().get( 0 ).intValue() - 1 );
          assertEquals( trigger.getDaysOfMonth()[ 0 ], ( (RecurrenceList) retrievedTrigger.getDayOfMonthRecurrences()
            .get( 0 ) ).getValues().get( 0 ).intValue() );
        }
      }
      assertTrue( found );
    }
  }

  /*
   * This first block of tests verifies that blockout jobs are created, updated, and deleted correctly.
   */
  @Test
  public void testAddAndRetrieveRunOnceBlockoutJob() throws Exception {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) )
        .thenReturn( pentahoScheduler );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ) )
        .thenReturn( blockoutManager );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IAuthorizationPolicy.class ) ).thenReturn( mockPolicy );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn(
        mockRepository );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class ) ).thenReturn(
        mockGenericFileService );
      JobScheduleRequest request = new JobScheduleRequest();
      request.setDuration( 7200000L );
      request.setGatheringMetrics( "false" );
      request.setInputFile( "" );
      request.setJobName( "RUN_ONCE-1321875142:admin:7200000" );
      request.setJobParameters( new ArrayList<>() );
      request.setLogLevel( "Basic" );
      request.setOutputFile( "" );
      request.setRunSafeMode( "false" );
      request.setTimeZone( "America/New_York" );
      SimpleJobTrigger trigger = new SimpleJobTrigger();
      trigger.setRepeatInterval( -1 );
      trigger.setStartDay( 26 );
      trigger.setStartHour( 14 );
      trigger.setStartMin( 0 );
      trigger.setStartMonth( 6 );
      trigger.setStartYear( 125 );
      trigger.setUiPassParam( "RUN_ONCE" );
      request.setSimpleJobTrigger( trigger );

      IJob job = schedulerService.addBlockout( request );
      request.setJobId( job.getJobId() );
      List<IJob> blockoutJobs = schedulerService.getBlockOutJobs();
      boolean found = false;
      for ( IJob j : blockoutJobs ) {
        if ( j.getJobId().equals( request.getJobId() ) ) {
          found = true;
          assertEquals( request.getDuration(), j.getJobTrigger().getDuration() );
          assertEquals( request.getTimeZone(), j.getJobTrigger().getTimeZone() );
          assertEquals( request.getSimpleJobTrigger().getStartDay(), j.getJobTrigger().getStartDay() );
          assertEquals( request.getSimpleJobTrigger().getStartHour(), j.getJobTrigger().getStartHour() );
          assertEquals( request.getSimpleJobTrigger().getStartMin(), j.getJobTrigger().getStartMin() );
          assertEquals( request.getSimpleJobTrigger().getStartMonth(), j.getJobTrigger().getStartMonth() );
          assertEquals( request.getSimpleJobTrigger().getStartYear(), j.getJobTrigger().getStartYear() );
          assertEquals( request.getSimpleJobTrigger().getUiPassParam(), j.getJobTrigger().getUiPassParam() );
        }
      }
      assertTrue( found );

      // Update blockout job
      request.setDuration( 3600000L ); // change duration
      request.setJobId( schedulerService.updateBlockout( request.getJobId(), request ).getJobId() );

      List<IJob> updatedJobs = schedulerService.getBlockOutJobs();
      boolean updatedFound = false;
      for ( IJob j : updatedJobs ) {
        if ( j.getJobId().equals( request.getJobId() ) ) {
          updatedFound = true;
          assertEquals( 3600000L, j.getJobTrigger().getDuration() );
        }
      }
      assertTrue( updatedFound );

      // Delete blockout job
      schedulerService.removeJob( request.getJobId() );
      List<IJob> afterDeleteJobs = schedulerService.getBlockOutJobs();
      boolean deletedFound = false;
      for ( IJob j : afterDeleteJobs ) {
        if ( j.getJobId().equals( request.getJobId() ) ) {
          deletedFound = true;
        }
      }
      assertFalse( deletedFound );
    }
  }

  @Test
  public void testAddAndRetrieveDailyBlockoutJob() throws Exception {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) )
        .thenReturn( pentahoScheduler );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ) )
        .thenReturn( blockoutManager );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IAuthorizationPolicy.class ) ).thenReturn( mockPolicy );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn(
        mockRepository );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class ) ).thenReturn(
        mockGenericFileService );
      JobScheduleRequest request = new JobScheduleRequest();
      request.setDuration( 10800000L );
      request.setGatheringMetrics( "false" );
      request.setInputFile( "" );
      request.setJobName( "DAILY1294379481:admin:10800000" );
      request.setJobParameters( new ArrayList<>() );
      request.setLogLevel( "Basic" );
      request.setOutputFile( "" );
      request.setRunSafeMode( "false" );
      request.setTimeZone( "America/New_York" );
      ComplexJobTriggerProxy trigger = new ComplexJobTriggerProxy();
      trigger.setDaysOfWeek( new int[] { 1, 2, 3, 4, 5 } );
      trigger.setStartDay( 22 );
      trigger.setStartHour( 14 );
      trigger.setStartMin( 0 );
      trigger.setStartMonth( 6 );
      trigger.setStartYear( 125 );
      trigger.setUiPassParam( "DAILY" );
      request.setComplexJobTrigger( trigger );

      IJob job = schedulerService.addBlockout( request );
      request.setJobId( job.getJobId() );
      List<IJob> blockoutJobs = schedulerService.getBlockOutJobs();
      boolean found = false;
      for ( IJob j : blockoutJobs ) {
        if ( j.getJobId().equals( request.getJobId() ) ) {
          found = true;
          assertEquals( request.getDuration(), j.getJobTrigger().getDuration() );
          assertEquals( request.getTimeZone(), j.getJobTrigger().getTimeZone() );
          assertEquals( request.getComplexJobTrigger().getStartDay(), j.getJobTrigger().getStartDay() );
          assertEquals( request.getComplexJobTrigger().getStartHour(), j.getJobTrigger().getStartHour() );
          assertEquals( request.getComplexJobTrigger().getStartMin(), j.getJobTrigger().getStartMin() );
          assertEquals( request.getComplexJobTrigger().getStartMonth(), j.getJobTrigger().getStartMonth() );
          assertEquals( request.getComplexJobTrigger().getStartYear(), j.getJobTrigger().getStartYear() );
          assertEquals( request.getComplexJobTrigger().getUiPassParam(), j.getJobTrigger().getUiPassParam() );
          // found job dayOfWeekRecurrences must include all weekdays
          DayOfWeekWrapper dayOfWeekWrapper = ( (ComplexJobTrigger) j.getJobTrigger() ).getDayOfWeekRecurrences();
          assertEquals( 5, ( (RecurrenceList) dayOfWeekWrapper.getRecurrences().get( 0 ) ).getValues().size() );
          assertTrue( ( (RecurrenceList) dayOfWeekWrapper.getRecurrences().get( 0 ) ).getValues().containsAll( Arrays
            .asList( 2, 3, 4, 5, 6 ) ) );
        }
      }
      assertTrue( found );

      // Update blockout job
      request.setDuration( 7200000L ); // change duration
      request.setJobId( schedulerService.updateBlockout( request.getJobId(), request ).getJobId() );

      List<IJob> updatedJobs = schedulerService.getBlockOutJobs();
      boolean updatedFound = false;
      for ( IJob j : updatedJobs ) {
        if ( j.getJobId().equals( request.getJobId() ) ) {
          updatedFound = true;
          assertEquals( 7200000L, j.getJobTrigger().getDuration() );
        }
      }
      assertTrue( updatedFound );

      // Delete blockout job
      schedulerService.removeJob( request.getJobId() );
      List<IJob> afterDeleteJobs = schedulerService.getBlockOutJobs();
      boolean deletedFound = false;
      for ( IJob j : afterDeleteJobs ) {
        if ( j.getJobId().equals( request.getJobId() ) ) {
          deletedFound = true;
        }
      }
      assertFalse( deletedFound );
    }
  }

  @Test
  public void testAddAndRetrieveWeeklyBlockoutJob() throws Exception {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) )
        .thenReturn( pentahoScheduler );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ) )
        .thenReturn( blockoutManager );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IAuthorizationPolicy.class ) ).thenReturn( mockPolicy );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn(
        mockRepository );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class ) ).thenReturn(
        mockGenericFileService );
      JobScheduleRequest request = new JobScheduleRequest();
      request.setDuration( 14400000L );
      request.setGatheringMetrics( "false" );
      request.setInputFile( "" );
      request.setJobName( "DAILY-1173435273:admin:14400000" );
      request.setJobParameters( new ArrayList<>() );
      request.setLogLevel( "Basic" );
      request.setOutputFile( "" );
      request.setRunSafeMode( "false" );
      request.setTimeZone( "America/New_York" );
      ComplexJobTriggerProxy trigger = new ComplexJobTriggerProxy();
      trigger.setDaysOfWeek( new int[] { 1, 2, 3, 4, 5 } );
      trigger.setStartDay( 22 );
      trigger.setStartHour( 16 );
      trigger.setStartMin( 0 );
      trigger.setStartMonth( 6 );
      trigger.setStartYear( 125 );
      trigger.setUiPassParam( "WEEKLY" );
      request.setComplexJobTrigger( trigger );

      IJob job = schedulerService.addBlockout( request );
      request.setJobId( job.getJobId() );
      List<IJob> blockoutJobs = schedulerService.getBlockOutJobs();
      boolean found = false;
      for ( IJob j : blockoutJobs ) {
        if ( j.getJobId().equals( request.getJobId() ) ) {
          found = true;
          assertEquals( request.getDuration(), j.getJobTrigger().getDuration() );
          assertEquals( request.getTimeZone(), j.getJobTrigger().getTimeZone() );
          assertEquals( request.getComplexJobTrigger().getStartDay(), j.getJobTrigger().getStartDay() );
          assertEquals( request.getComplexJobTrigger().getStartHour(), j.getJobTrigger().getStartHour() );
          assertEquals( request.getComplexJobTrigger().getStartMin(), j.getJobTrigger().getStartMin() );
          assertEquals( request.getComplexJobTrigger().getStartMonth(), j.getJobTrigger().getStartMonth() );
          assertEquals( request.getComplexJobTrigger().getStartYear(), j.getJobTrigger().getStartYear() );
          assertEquals( request.getComplexJobTrigger().getUiPassParam(), j.getJobTrigger().getUiPassParam() );
          // found job dayOfWeekRecurrences must include all weekdays
          DayOfWeekWrapper dayOfWeekWrapper = ( (ComplexJobTrigger) j.getJobTrigger() ).getDayOfWeekRecurrences();
          assertEquals( 5, ( (RecurrenceList) dayOfWeekWrapper.getRecurrences().get( 0 ) ).getValues().size() );
          assertTrue( ( (RecurrenceList) dayOfWeekWrapper.getRecurrences().get( 0 ) ).getValues().containsAll( Arrays
            .asList( 2, 3, 4, 5, 6 ) ) );
        }
      }
      assertTrue( found );

      // Update blockout job
      request.setDuration( 7200000L ); // change duration
      request.setJobId( schedulerService.updateBlockout( request.getJobId(), request ).getJobId() );

      List<IJob> updatedJobs = schedulerService.getBlockOutJobs();
      boolean updatedFound = false;
      for ( IJob j : updatedJobs ) {
        if ( j.getJobId().equals( request.getJobId() ) ) {
          updatedFound = true;
          assertEquals( 7200000L, j.getJobTrigger().getDuration() );
        }
      }
      assertTrue( updatedFound );

      // Delete blockout job
      schedulerService.removeJob( request.getJobId() );
      List<IJob> afterDeleteJobs = schedulerService.getBlockOutJobs();
      boolean deletedFound = false;
      for ( IJob j : afterDeleteJobs ) {
        if ( j.getJobId().equals( request.getJobId() ) ) {
          deletedFound = true;
        }
      }
      assertFalse( deletedFound );
    }
  }

  @Test
  public void testAddAndRetrieveMonthlyBlockoutJob() throws Exception {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) )
        .thenReturn( pentahoScheduler );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ) )
        .thenReturn( blockoutManager );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IAuthorizationPolicy.class ) ).thenReturn( mockPolicy );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn(
        mockRepository );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class ) ).thenReturn(
        mockGenericFileService );
      JobScheduleRequest request = new JobScheduleRequest();
      request.setDuration( 18000000L );
      request.setGatheringMetrics( "false" );
      request.setInputFile( "" );
      request.setJobName( "MONTHLY-826814084:admin:18000000" );
      request.setJobParameters( new ArrayList<>() );
      request.setLogLevel( "Basic" );
      request.setOutputFile( "" );
      request.setRunSafeMode( "false" );
      request.setTimeZone( "America/New_York" );
      ComplexJobTriggerProxy trigger = new ComplexJobTriggerProxy();
      trigger.setDaysOfMonth( new int[] { 7 } );
      trigger.setStartDay( 22 );
      trigger.setStartHour( 17 );
      trigger.setStartMin( 0 );
      trigger.setStartMonth( 6 );
      trigger.setStartYear( 125 );
      trigger.setUiPassParam( "MONTHLY" );
      request.setComplexJobTrigger( trigger );

      IJob job = schedulerService.addBlockout( request );
      request.setJobId( job.getJobId() );
      List<IJob> blockoutJobs = schedulerService.getBlockOutJobs();
      boolean found = false;
      for ( IJob j : blockoutJobs ) {
        if ( j.getJobId().equals( request.getJobId() ) ) {
          found = true;
          assertEquals( request.getDuration(), j.getJobTrigger().getDuration() );
          assertEquals( request.getTimeZone(), j.getJobTrigger().getTimeZone() );
          assertEquals( request.getComplexJobTrigger().getStartDay(), j.getJobTrigger().getStartDay() );
          assertEquals( request.getComplexJobTrigger().getStartHour(), j.getJobTrigger().getStartHour() );
          assertEquals( request.getComplexJobTrigger().getStartMin(), j.getJobTrigger().getStartMin() );
          assertEquals( request.getComplexJobTrigger().getStartMonth(), j.getJobTrigger().getStartMonth() );
          assertEquals( request.getComplexJobTrigger().getStartYear(), j.getJobTrigger().getStartYear() );
          assertEquals( request.getComplexJobTrigger().getUiPassParam(), j.getJobTrigger().getUiPassParam() );
          // found job dayOfMonthRecurrences must include the 7th of the month
          DayOfMonthWrapper dayOfMonthRecurrences = ( (ComplexJobTrigger) j.getJobTrigger() )
            .getDayOfMonthRecurrences();
          assertEquals( 1, ( (RecurrenceList) dayOfMonthRecurrences.getRecurrences().get( 0 ) ).getValues().size() );
          assertTrue( ( (RecurrenceList) dayOfMonthRecurrences.getRecurrences().get( 0 ) ).getValues().contains( 7 ) );
        }
      }
      assertTrue( found );

      // Update blockout job
      request.setDuration( 7200000L ); // change duration
      request.setJobId( schedulerService.updateBlockout( request.getJobId(), request ).getJobId() );
      List<IJob> updatedJobs = schedulerService.getBlockOutJobs();
      boolean updatedFound = false;
      for ( IJob j : updatedJobs ) {
        if ( j.getJobId().equals( request.getJobId() ) ) {
          updatedFound = true;
          assertEquals( 7200000L, j.getJobTrigger().getDuration() );
        }
      }
      assertTrue( updatedFound );

      // Delete blockout job
      schedulerService.removeJob( request.getJobId() );
      List<IJob> afterDeleteJobs = schedulerService.getBlockOutJobs();
      boolean deletedFound = false;
      for ( IJob j : afterDeleteJobs ) {
        if ( j.getJobId().equals( request.getJobId() ) ) {
          deletedFound = true;
        }
      }
      assertFalse( deletedFound );
    }
  }

  @Test
  public void testAddAndRetrieveYearlyBlockoutJob() throws Exception {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) )
        .thenReturn( pentahoScheduler );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ) )
        .thenReturn( blockoutManager );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IAuthorizationPolicy.class ) ).thenReturn( mockPolicy );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn(
        mockRepository );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class ) ).thenReturn(
        mockGenericFileService );
      JobScheduleRequest request = new JobScheduleRequest();
      request.setDuration( 25200000L );
      request.setGatheringMetrics( "false" );
      request.setInputFile( "" );
      request.setJobName( "YEARLY1522193893:admin:25200000" );
      request.setJobParameters( new ArrayList<>() );
      request.setLogLevel( "Basic" );
      request.setOutputFile( "" );
      request.setRunSafeMode( "false" );
      request.setTimeZone( "America/New_York" );
      ComplexJobTriggerProxy trigger = new ComplexJobTriggerProxy();
      trigger.setDaysOfMonth( new int[] { 17 } );
      trigger.setMonthsOfYear( new int[] { 5 } );
      trigger.setStartDay( 22 );
      trigger.setStartHour( 19 );
      trigger.setStartMin( 0 );
      trigger.setStartMonth( 6 );
      trigger.setStartYear( 125 );
      trigger.setUiPassParam( "YEARLY" );
      request.setComplexJobTrigger( trigger );

      IJob job = schedulerService.addBlockout( request );
      request.setJobId( job.getJobId() );
      List<IJob> blockoutJobs = schedulerService.getBlockOutJobs();
      boolean found = false;
      for ( IJob j : blockoutJobs ) {
        if ( j.getJobId().equals( request.getJobId() ) ) {
          found = true;
          assertEquals( request.getDuration(), j.getJobTrigger().getDuration() );
          assertEquals( request.getTimeZone(), j.getJobTrigger().getTimeZone() );
          assertEquals( request.getComplexJobTrigger().getStartDay(), j.getJobTrigger().getStartDay() );
          assertEquals( request.getComplexJobTrigger().getStartHour(), j.getJobTrigger().getStartHour() );
          assertEquals( request.getComplexJobTrigger().getStartMin(), j.getJobTrigger().getStartMin() );
          assertEquals( request.getComplexJobTrigger().getStartMonth(), j.getJobTrigger().getStartMonth() );
          assertEquals( request.getComplexJobTrigger().getStartYear(), j.getJobTrigger().getStartYear() );
          assertEquals( request.getComplexJobTrigger().getUiPassParam(), j.getJobTrigger().getUiPassParam() );
          // found job dayOfMonthRecurrences must include the 7th of the month
          DayOfMonthWrapper dayOfMonthRecurrences = ( (ComplexJobTrigger) j.getJobTrigger() )
            .getDayOfMonthRecurrences();
          assertEquals( 1, ( (RecurrenceList) dayOfMonthRecurrences.getRecurrences().get( 0 ) ).getValues().size() );
          assertTrue( ( (RecurrenceList) dayOfMonthRecurrences.getRecurrences().get( 0 ) ).getValues().contains( 17 ) );
          // found job monthsOfYearRecurrences must include May
          MonthlyWrapper monthsOfYearRecurrences = ( (ComplexJobTrigger) j.getJobTrigger() ).getMonthlyRecurrences();
          assertEquals( 1, ( (RecurrenceList) monthsOfYearRecurrences.getRecurrences().get( 0 ) ).getValues().size() );
          // yes, we mean 6; these months are indexed from 1 on the way out
          assertTrue( ( (RecurrenceList) monthsOfYearRecurrences.getRecurrences().get( 0 ) ).getValues().contains(
            6 ) );
        }
      }
      assertTrue( found );

      // Update blockout job
      request.setDuration( 7200000L ); // change duration
      request.setJobId( schedulerService.updateBlockout( request.getJobId(), request ).getJobId() );
      List<IJob> updatedJobs = schedulerService.getBlockOutJobs();
      boolean updatedFound = false;
      for ( IJob j : updatedJobs ) {
        if ( j.getJobId().equals( request.getJobId() ) ) {
          updatedFound = true;
          assertEquals( 7200000L, j.getJobTrigger().getDuration() );
        }
      }
      assertTrue( updatedFound );

      // Delete blockout job
      schedulerService.removeJob( request.getJobId() );
      List<IJob> afterDeleteJobs = schedulerService.getBlockOutJobs();
      boolean deletedFound = false;
      for ( IJob j : afterDeleteJobs ) {
        if ( j.getJobId().equals( request.getJobId() ) ) {
          deletedFound = true;
        }
      }
      assertFalse( deletedFound );
    }
  }

  /*
   * This test verifies that the scheduler will correctly report whether a job will be impacted by a blockout job.
   */
  @Test
  public void verifyBlockoutsBlockJobs() throws Exception {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) )
        .thenReturn( pentahoScheduler );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ) )
        .thenReturn( blockoutManager );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IAuthorizationPolicy.class ) ).thenReturn( mockPolicy );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn(
        mockRepository );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class ) ).thenReturn(
        mockGenericFileService );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IGenericFileService.class, mockSession ) ).thenReturn(
        mockGenericFileService );

      // create a blockout job
      JobScheduleRequest blockoutRequest = new JobScheduleRequest();
      blockoutRequest.setDuration( 7200000L );
      blockoutRequest.setGatheringMetrics( "false" );
      blockoutRequest.setInputFile( "" );
      blockoutRequest.setJobName( "RUN_ONCE-1321875142:admin:7200000" );
      blockoutRequest.setJobParameters( new ArrayList<>() );
      blockoutRequest.setLogLevel( "Basic" );
      blockoutRequest.setOutputFile( "" );
      blockoutRequest.setRunSafeMode( "false" );
      blockoutRequest.setTimeZone( "America/New_York" );
      SimpleJobTrigger blockoutTrigger = new SimpleJobTrigger();
      java.util.Date startTime = startDate30MinsFromNow();

      blockoutTrigger.setRepeatInterval( -1 );
      blockoutTrigger.setStartDay( startTime.getDate() );
      blockoutTrigger.setStartHour( startTime.getHours() );
      blockoutTrigger.setStartMin( startTime.getMinutes() );
      blockoutTrigger.setStartMonth( startTime.getMonth() );
      blockoutTrigger.setStartYear( startTime.getYear() );
      blockoutTrigger.setUiPassParam( "RUN_ONCE" );
      blockoutRequest.setSimpleJobTrigger( blockoutTrigger );

      IJob blockoutJob = schedulerService.addBlockout( blockoutRequest );
      blockoutRequest.setJobId( blockoutJob.getJobId() );
      List<IJob> blockoutJobs = schedulerService.getBlockOutJobs();
      boolean found = false;
      for ( IJob j : blockoutJobs ) {
        if ( j.getJobId().equals( blockoutRequest.getJobId() ) ) {
          found = true;
          assertEquals( blockoutRequest.getDuration(), j.getJobTrigger().getDuration() );
          assertEquals( blockoutRequest.getTimeZone(), j.getJobTrigger().getTimeZone() );
          assertEquals( blockoutRequest.getSimpleJobTrigger().getStartDay(), j.getJobTrigger().getStartDay() );
          assertEquals( blockoutRequest.getSimpleJobTrigger().getStartHour(), j.getJobTrigger().getStartHour() );
          assertEquals( blockoutRequest.getSimpleJobTrigger().getStartMin(), j.getJobTrigger().getStartMin() );
          assertEquals( blockoutRequest.getSimpleJobTrigger().getStartMonth(), j.getJobTrigger().getStartMonth() );
          assertEquals( blockoutRequest.getSimpleJobTrigger().getStartYear(), j.getJobTrigger().getStartYear() );
          assertEquals( blockoutRequest.getSimpleJobTrigger().getUiPassParam(), j.getJobTrigger().getUiPassParam() );
        }
      }
      assertTrue( found );

      // create a job to run at the same time as the blockout job
      JobScheduleRequest scheduleRequest = new JobScheduleRequest();
      scheduleRequest.setJobName( "simpleJob" );
      scheduleRequest.setActionClass( "className" );
      scheduleRequest.setInputFile( "/test/file/path.rpt" );
      scheduleRequest.setOutputFile( "/test/file/output" );
      scheduleRequest.setJobParameters( new ArrayList<IJobScheduleParam>() {
        {
          add( new JobScheduleParam( IScheduler.RESERVEDMAPKEY_ACTIONUSER, "admin" ) );
        }
      } );

      SimpleJobTrigger trigger = new SimpleJobTrigger();
      // set the start time to 30 minutes from now
      // set the time components of the trigger to match the start time
      trigger.setStartYear( startTime.getYear() );
      trigger.setStartMonth( startTime.getMonth() );
      trigger.setStartDay( startTime.getDate() );
      trigger.setStartHour( startTime.getHours() );
      trigger.setStartMin( startTime.getMinutes() );
      trigger.setTimeZone( "UTC" );
      scheduleRequest.setTimeZone( "UTC" );
      trigger.setUiPassParam( "RUN_ONCE" );
      trigger.setStartTime( startTime );
      // set the time components of the schedule request to match the start time

      trigger.setRepeatCount( -1 );
      trigger.setRepeatInterval( -1 );
      scheduleRequest.setSimpleJobTrigger( trigger );

      Job createdJob = schedulerService.createJob( scheduleRequest );

      List<IJob> jobs = schedulerService.getJobs();
      found = false;
      for ( IJob job : jobs ) {
        if ( job.getJobName().equals( createdJob.getJobName() ) ) {
          found = true;
          SimpleJobTrigger retrievedTrigger = (SimpleJobTrigger) job.getJobTrigger();
          assertEquals( trigger.getStartTime(), retrievedTrigger.getStartTime() );
          assertEquals( trigger.toString(), retrievedTrigger.toString() );
        }
      }
      assertTrue( found );

      assertFalse( schedulerService.willFire( trigger ) );

      // Delete blockout job
      schedulerService.removeJob( blockoutRequest.getJobId() );
      List<IJob> afterDeleteJobs = schedulerService.getBlockOutJobs();
      boolean deletedFound = false;
      for ( IJob j : afterDeleteJobs ) {
        if ( j.getJobId().equals( blockoutRequest.getJobId() ) ) {
          deletedFound = true;
        }
      }
      assertFalse( deletedFound );
    }
  }

}

@SuppressWarnings( "squid:S1135" )
class MockSecurityHelper implements ISecurityHelper {
  @Override
  public <T> T runAsUser( String principalName, Callable<T> callable ) throws Exception {
    // Mocked to run the callable as the specified user
    return callable.call();
  }

  @Override
  public void becomeUser( String principalName ) {
    // TODO Auto-generated method stub

  }

  @Override
  public void becomeUser( String principalName, IParameterProvider paramProvider ) {
    // TODO Auto-generated method stub

  }

  @Override
  public <T> T runAsUser( String principalName, IParameterProvider paramProvider, Callable<T> callable )
    throws Exception {
    // TODO Auto-generated method stub
    return callable.call();
  }

  @Override
  public <T> T runAsAnonymous( Callable<T> callable ) throws Exception {
    // TODO Auto-generated method stub
    return callable.call();
  }

  @Override
  public boolean isPentahoAdministrator( IPentahoSession session ) {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public boolean isGranted( IPentahoSession session, GrantedAuthority role ) {
    // TODO Auto-generated method stub
    return true;
  }

  @SuppressWarnings( { "squid:S1874" } )
  @Override
  public boolean hasAccess( IAclHolder aHolder, int actionOperation, IPentahoSession session ) {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public Authentication createAuthentication( String principalName ) {
    // TODO Auto-generated method stub
    return Mockito.mock( Authentication.class );
  }

  @Override
  public Authentication getAuthentication() {
    // TODO Auto-generated method stub
    return Mockito.mock( Authentication.class );
  }

  @Override
  public Authentication getAuthentication( IPentahoSession ignoredSession, boolean ignoredAllowAnonymous ) {
    // TODO Auto-generated method stub
    return Mockito.mock( Authentication.class );
  }

  @Override
  public <T> T runAsSystem( Callable<T> callable ) throws Exception {
    // TODO Auto-generated method stub
    return callable.call();
  }
}