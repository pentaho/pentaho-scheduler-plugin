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


package org.pentaho.platform.web.http.api.resources.services;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.platform.api.engine.IAuthorizationPolicy;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.webservices.RepositoryFileDto;
import org.pentaho.platform.api.scheduler2.IBackgroundExecutionStreamProvider;
import org.pentaho.platform.api.scheduler2.IBlockoutManager;
import org.pentaho.platform.api.scheduler2.IJob;
import org.pentaho.platform.api.scheduler2.IJobFilter;
import org.pentaho.platform.api.scheduler2.IJobScheduleParam;
import org.pentaho.platform.api.scheduler2.IJobTrigger;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.JobState;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.api.util.IPdiContentProvider;
import org.pentaho.platform.security.policy.rolebased.actions.AdministerSecurityAction;
import org.pentaho.platform.security.policy.rolebased.actions.SchedulerAction;
import org.pentaho.platform.security.policy.rolebased.actions.SchedulerExecuteAction;
import org.pentaho.platform.web.http.api.proxies.BlockStatusProxy;
import org.pentaho.platform.web.http.api.resources.JobRequest;
import org.pentaho.platform.web.http.api.resources.JobScheduleParam;
import org.pentaho.platform.web.http.api.resources.JobScheduleRequest;
import org.pentaho.platform.web.http.api.resources.SchedulerOutputPathResolver;
import org.pentaho.platform.web.http.api.resources.SchedulerResourceUtil;
import org.pentaho.platform.web.http.api.resources.SessionResource;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings( "unchecked" )
@RunWith( MockitoJUnitRunner.class )
public class SchedulerServiceTest {
  private static SchedulerService schedulerService;

  @Before
  public void setUp() throws SchedulerException, IOException, IllegalAccessException {
    schedulerService = Mockito.spy( new SchedulerService() );
    doCallRealMethod().when( schedulerService ).createJob( any() );
    schedulerService.policy = mock( IAuthorizationPolicy.class );
    schedulerService.scheduler = mock( IScheduler.class );
    schedulerService.repository = mock( IUnifiedRepository.class );
    schedulerService.blockoutManager = mock( IBlockoutManager.class );
  }

  @After
  public void cleanup() {
    schedulerService = null;
  }

  private JobScheduleRequest getBasicRequest() {
    List<IJobScheduleParam> jobParameters = new ArrayList<>();
    jobParameters.add( new JobScheduleParam( "name1", "value1" ) );
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    scheduleRequest.setActionClass( "className" );
    scheduleRequest.setJobName( "jobName" );
    scheduleRequest.setRunSafeMode( "runSafeMode" );
    scheduleRequest.setGatheringMetrics( "gatheringMetrics" );
    scheduleRequest.setLogLevel( "Basic" );
    scheduleRequest.setJobParameters( jobParameters );
    scheduleRequest.setTimeZone( "timezone" );
    SimpleJobTrigger simpleJobTrigger = new SimpleJobTrigger();
    scheduleRequest.setSimpleJobTrigger( simpleJobTrigger );
    scheduleRequest.setInputFile( "file.ext" );
    return scheduleRequest;
  }

  private interface TestRunnable {
    void run() throws Exception;
  }

  private void runWithCreateJobMocks( TestRunnable run ) throws Exception {
    setupCreateJobMocks();

    try ( MockedStatic<SchedulerResourceUtil> schedulerResourceUtilMockedStatic = Mockito.mockStatic(
      SchedulerResourceUtil.class ) ) {
      IPdiContentProvider mockPdiContentProvider = mock( IPdiContentProvider.class );
      schedulerResourceUtilMockedStatic.when( SchedulerResourceUtil::getiPdiContentProvider )
        .thenReturn( mockPdiContentProvider );

      schedulerResourceUtilMockedStatic.when( () ->
          SchedulerResourceUtil.convertScheduleRequestToJobTrigger( any(), any( IScheduler.class ) ) )
        .thenCallRealMethod();

      schedulerResourceUtilMockedStatic.when( () ->
          SchedulerResourceUtil.resolveActionIdFromClass( nullable( String.class ) ) )
        .thenReturn( "testResolveActionIdFromClass" );

        run.run();
      }
  }

  @Test
  public void testCreateJobEmptyName() throws Exception {
      JobScheduleRequest scheduleRequest = getBasicRequest();

      runWithCreateJobMocks( () -> {
        scheduleRequest.setJobName( "" );
        Job returnJob = schedulerService.createJob( scheduleRequest );
        assertNotNull( returnJob );
        assertEquals( "file", scheduleRequest.getJobName() );
      } );
  }

  /** was Test3 in old testCreateJob */
  @Test
  public void testCreateJob3NoInputFile() throws Exception {
      runWithCreateJobMocks( () -> {
        JobScheduleRequest scheduleRequest = getBasicRequest();
        scheduleRequest.setInputFile( "" );
        // action is mocked to return valid for any input, so this still works
        Job returned = schedulerService.createJob( scheduleRequest );
        assertNotNull( returned );
      } );
  }

  @Test
  public void testCreateJob4ActionIdFallback() throws Exception {
      JobScheduleRequest scheduleRequest = getBasicRequest();
      runWithCreateJobMocks( () -> {
        //Test 4 - when new ClassNotFoundException(), alternative logic to determine the actionId should execute
        // doReturn( "" ).when( scheduleRequest ).getInputFile();
        scheduleRequest.setInputFile( "" );
        doThrow( new ClassNotFoundException() ).when( schedulerService ).getAction( nullable( String.class ) );
        Job job = new Job();
        doReturn( job ).when( schedulerService.scheduler )
          .createJob( anyString(), anyString(), any( Map.class ), any( IJobTrigger.class ) );
        Job returnJob = schedulerService.createJob( scheduleRequest );
        assertEquals( job, returnJob );
      } );
  }

  @Test
  public void testCreateJob() throws Exception {
    JobScheduleRequest scheduleRequest = getBasicRequest();
    runWithCreateJobMocks( () -> {
      // test service passes validations and returns what the underlying schedule returns
      Job returnJob = schedulerService.createJob( scheduleRequest );
      assertNotNull( returnJob );
    } );
  }


  private void setupCreateJobMocks() throws SchedulerException, ClassNotFoundException  {
    setupCreateJobMocks( new Job() );
  }

  /** general purpose mocks that were being reused in many tests */
  private void setupCreateJobMocks( Job job )
    throws SchedulerException, ClassNotFoundException {

    doReturn( true ).when( schedulerService ).isPdiFile( "file.ext" );

    SchedulerOutputPathResolver schedulerOutputPathResolver = mock( SchedulerOutputPathResolver.class );
    doReturn( "outputFile" ).when( schedulerOutputPathResolver ).resolveOutputFilePath();
    doReturn( schedulerOutputPathResolver ).when( schedulerService )
      .getSchedulerOutputPathResolver( any( JobScheduleRequest.class ) );

    RepositoryFile repositoryFile = mock( RepositoryFile.class );
    doReturn( "file.ext" ).when( repositoryFile ).getName();

    Map<String, Serializable> metadata = new HashMap<>();
    metadata.put( RepositoryFile.SCHEDULABLE_KEY, "true" );

    doReturn( metadata ).when( schedulerService.repository ).getFileMetadata( nullable( String.class ) );
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    doReturn( repositoryFile ).when( schedulerService.repository ).getFile( nullable( String.class ) );

    doReturn( true ).when( schedulerService ).getAutoCreateUniqueFilename( any( JobScheduleRequest.class ) );

    doReturn( job ).when( schedulerService.scheduler )
      .createJob( nullable( String.class ), nullable( String.class ), any( Map.class ), any( IJobTrigger.class ),
        any( IBackgroundExecutionStreamProvider.class ) );

    doReturn( Class.class ).when( schedulerService ).getAction( nullable( String.class ) );

    doReturn( job ).when( schedulerService.scheduler )
      .createJob( nullable( String.class ), any( Class.class ), any( Map.class ), any( IJobTrigger.class ) );

  }

  @Test(expected = SecurityException.class )
  public void testCreateJobSecurityException() throws Exception {
    JobScheduleRequest scheduleRequest = getBasicRequest();
    setupCreateJobMocks();

    doReturn( false ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    schedulerService.createJob( scheduleRequest );
  }

  @Test(expected = IllegalAccessException.class )
  public void testCreateJobFileAccessException() throws Exception {
    JobScheduleRequest scheduleRequest = getBasicRequest();

    runWithCreateJobMocks( () -> {
      // file in repository not sched
      Map<String, Serializable> metadata = new HashMap<>();
      metadata.put( RepositoryFile.SCHEDULABLE_KEY, "false" );
      doReturn( metadata ).when( schedulerService.repository ).getFileMetadata( nullable( String.class ) );

      schedulerService.createJob( scheduleRequest );
    });
  }

  @Test
  public void testTriggerNow() throws Exception {
    JobRequest jobRequest = mock( JobRequest.class );
    Job job = mock( Job.class );

    doReturn( job ).when( schedulerService.scheduler ).getJob( nullable( String.class ) );
    doReturn( true ).when( schedulerService ).isScheduleAllowed();

    IJob resultJob = schedulerService.triggerNow( jobRequest.getJobId() );
    assertEquals( job, resultJob );

    verify( schedulerService.scheduler, times( 2 ) ).getJob( nullable( String.class ) );
    verify( schedulerService.scheduler, times( 1 ) ).triggerNow( nullable( String.class ) );
    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
  }

  @Test
  public void testTriggerNowExecuteSchedulePermission() throws Exception {
    JobRequest jobRequest = mock( JobRequest.class );
    Job job = mock( Job.class );

    doReturn( job ).when( schedulerService.scheduler ).getJob( nullable( String.class ) );
    doReturn( false ).when( schedulerService ).isScheduleAllowed();
    doReturn( true ).when( schedulerService ).isExecuteScheduleAllowed();

    IJob resultJob = schedulerService.triggerNow( jobRequest.getJobId() );
    assertEquals( job, resultJob );

    verify( schedulerService.scheduler, times( 2 ) ).getJob( nullable( String.class ) );
    verify( schedulerService.scheduler, times( 1 ) ).triggerNow( nullable( String.class ) );
    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
    verify( schedulerService, times( 1 ) ).isExecuteScheduleAllowed();
  }

  @Test
  public void testTriggerNowSameUser() throws Exception {
    JobRequest jobRequest = mock( JobRequest.class );
    Job job = mock( Job.class );
    IPentahoSession mockSession = mock( IPentahoSession.class );
    final String username = "username";

    doReturn( job ).when( schedulerService.scheduler ).getJob( nullable( String.class ) );
    doReturn( false ).when( schedulerService ).isScheduleAllowed();
    doReturn( false ).when( schedulerService ).isExecuteScheduleAllowed();
    doReturn( mockSession ).when( schedulerService ).getSession();
    doReturn( username ).when( job ).getUserName();
    doReturn( username ).when( mockSession ).getName();

    IJob resultJob = schedulerService.triggerNow( jobRequest.getJobId() );
    assertEquals( job, resultJob );

    verify( schedulerService.scheduler, times( 2 ) ).getJob( nullable( String.class ) );
    verify( schedulerService.scheduler, times( 1 ) ).triggerNow( nullable( String.class ) );
    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
    verify( schedulerService, times( 1 ) ).isExecuteScheduleAllowed();
  }

  @Test
  public void testTriggerNowForbidden() throws Exception {
    JobRequest jobRequest = mock( JobRequest.class );
    Job job = mock( Job.class );
    IPentahoSession mockSession = mock( IPentahoSession.class );
    final String username1 = "username1";
    final String username2 = "username2";

    doReturn( job ).when( schedulerService.scheduler ).getJob( nullable( String.class ) );
    doReturn( false ).when( schedulerService ).isScheduleAllowed();
    doReturn( false ).when( schedulerService ).isExecuteScheduleAllowed();
    doReturn( mockSession ).when( schedulerService ).getSession();
    doReturn( username1 ).when( job ).getUserName();
    doReturn( username2 ).when( mockSession ).getName();

    IJob resultJob = schedulerService.triggerNow( jobRequest.getJobId() );
    assertEquals( job, resultJob );

    verify( schedulerService.scheduler, times( 1 ) ).getJob( nullable( String.class ) );
    verify( schedulerService.scheduler, times( 0 ) ).triggerNow( nullable( String.class ) );
    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
    verify( schedulerService, times( 1 ) ).isExecuteScheduleAllowed();
  }

  @Test
  public void testGetContentCleanerJob() throws Exception {
    IJobFilter jobFilter = mock( IJobFilter.class );
    List<IJob> jobs = new ArrayList<>();

    IPentahoSession session = mock( IPentahoSession.class );
    doReturn( session ).when( schedulerService ).getSession();
    doReturn( "sessionName" ).when( session ).getName();

    doReturn( true ).when( schedulerService.policy ).isAllowed( AdministerSecurityAction.NAME );
    doReturn( jobFilter ).when( schedulerService ).getJobFilter( anyBoolean(), nullable( String.class ) );
    doReturn( jobs ).when( schedulerService.scheduler ).getJobs( any( IJobFilter.class ) );

    //Test 1
    IJob job = schedulerService.getContentCleanerJob();

    assertNull( job );

    //Test 2
    Job job1 = mock( Job.class );
    jobs.add( job1 );

    job = schedulerService.getContentCleanerJob();

    assertNotNull( job );

    verify( schedulerService, times( 2 ) ).getSession();
    verify( session, times( 2 ) ).getName();
    verify( schedulerService.policy, times( 2 ) ).isAllowed( AdministerSecurityAction.NAME );
    verify( schedulerService.scheduler, times( 2 ) ).getJobs( any( IJobFilter.class ) );
  }

  @Test
  public void testGetContentCleanerJobException() throws Exception {
    IJobFilter jobFilter = mock( IJobFilter.class );

    IPentahoSession session = mock( IPentahoSession.class );
    doReturn( session ).when( schedulerService ).getSession();
    doReturn( "sessionName" ).when( session ).getName();

    doReturn( true ).when( schedulerService.policy ).isAllowed( AdministerSecurityAction.NAME );
    doReturn( jobFilter ).when( schedulerService ).getJobFilter( anyBoolean(), nullable( String.class ) );
    doThrow( new SchedulerException( "" ) ).when( schedulerService.scheduler ).getJobs( any( IJobFilter.class ) );

    try {
      schedulerService.getContentCleanerJob();
      fail();
    } catch ( SchedulerException e ) {
      //Should catch the exception
    }

    verify( schedulerService ).getSession();
    verify( session ).getName();
    verify( schedulerService.policy ).isAllowed( AdministerSecurityAction.NAME );
    verify( schedulerService.scheduler ).getJobs( any( IJobFilter.class ) );
  }

  @Test
  public void testDoGetCanSchedule() {
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    //Test 1
    String isAllowed = schedulerService.doGetCanSchedule();
    assertEquals( "true", isAllowed );

    //Test 2
    doReturn( false ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );
    isAllowed = schedulerService.doGetCanSchedule();
    assertEquals( "false", isAllowed );
    verify( schedulerService.policy, times( 2 ) ).isAllowed( SchedulerAction.NAME );
  }

  @Test
  public void testDoGetCanExecuteScheduleTrue() {
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerExecuteAction.NAME );
    String isAllowed = schedulerService.doGetCanExecuteSchedule();
    assertEquals( "true", isAllowed );
    verify( schedulerService.policy, times( 1 ) ).isAllowed( SchedulerExecuteAction.NAME );
  }

  @Test
  public void testDoGetCanExecuteScheduleFalse() {
    doReturn( false ).when( schedulerService.policy ).isAllowed( SchedulerExecuteAction.NAME );
    String isAllowed = schedulerService.doGetCanExecuteSchedule();
    assertEquals( "false", isAllowed );
    verify( schedulerService.policy, times( 1 ) ).isAllowed( SchedulerExecuteAction.NAME );
  }

  @Test
  public void testGetState() throws SchedulerException {
    doReturn( IScheduler.SchedulerStatus.RUNNING ).when( schedulerService.scheduler ).getStatus();
    String state = schedulerService.getState();
    assertEquals( "RUNNING", state );
    verify( schedulerService.scheduler ).getStatus();
  }

  @Test
  public void testGetStateException() throws SchedulerException {
    doThrow( new SchedulerException( "" ) ).when( schedulerService.scheduler ).getStatus();

    try {
      schedulerService.getState();
      fail();
    } catch ( SchedulerException e ) {
      //Should go here
    }

    verify( schedulerService.scheduler ).getStatus();
  }

  @Test
  public void testStart() throws SchedulerException {
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    doNothing().when( schedulerService.scheduler ).start();

    doReturn( IScheduler.SchedulerStatus.RUNNING ).when( schedulerService.scheduler ).getStatus();

    //Test 1
    String state = schedulerService.start();
    assertEquals( "RUNNING", state );

    //Test 2
    doReturn( IScheduler.SchedulerStatus.STOPPED ).when( schedulerService.scheduler ).getStatus();
    doReturn( false ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    state = schedulerService.start();

    assertEquals( "STOPPED", state );

    verify( schedulerService.policy, times( 2 ) ).isAllowed( SchedulerAction.NAME );
    verify( schedulerService.scheduler, times( 1 ) ).start();
    verify( schedulerService.scheduler, times( 2 ) ).getStatus();
  }

  @Test
  public void testStartException() throws SchedulerException {
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    doThrow( new SchedulerException( "" ) ).when( schedulerService.scheduler ).start();

    try {
      schedulerService.start();
      fail();
    } catch ( SchedulerException e ) {
      //Should go here
    }

    verify( schedulerService.policy ).isAllowed( SchedulerAction.NAME );
    verify( schedulerService.scheduler ).start();
  }

  @Test
  public void testPause() throws SchedulerException {
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );
    doNothing().when( schedulerService.scheduler ).pause();
    doReturn( IScheduler.SchedulerStatus.PAUSED ).when( schedulerService.scheduler ).getStatus();

    //Test 1
    String state = schedulerService.pause();
    assertEquals( "PAUSED", state );

    //Test 2
    doReturn( IScheduler.SchedulerStatus.RUNNING ).when( schedulerService.scheduler ).getStatus();
    doReturn( false ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    state = schedulerService.pause();

    assertEquals( "RUNNING", state );

    verify( schedulerService.policy, times( 2 ) ).isAllowed( SchedulerAction.NAME );
    verify( schedulerService.scheduler, times( 1 ) ).pause();
    verify( schedulerService.scheduler, times( 2 ) ).getStatus();
  }

  @Test
  public void testPauseException() throws SchedulerException {
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );
    doThrow( new SchedulerException( "" ) ).when( schedulerService.scheduler ).pause();

    try {
      schedulerService.pause();
      fail();
    } catch ( SchedulerException e ) {
      //Should go here
    }

    verify( schedulerService.policy ).isAllowed( SchedulerAction.NAME );
    verify( schedulerService.scheduler ).pause();
  }

  @Test
  public void testPauseJob() throws SchedulerException {
    Job job = mock( Job.class );
    doReturn( job ).when( schedulerService ).getJob( nullable( String.class ) );
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    doNothing().when( schedulerService.scheduler ).pauseJob( nullable( String.class ) );
    JobState state = schedulerService.pauseJob( "job-id" );
    assertNull( state );
  }

  @Test
  public void testPauseJobException() throws SchedulerException {
    Job job = mock( Job.class );
    doReturn( job ).when( schedulerService ).getJob( nullable( String.class ) );
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    doThrow( new SchedulerException( "pause-exception" ) ).when( schedulerService.scheduler )
      .pauseJob( nullable( String.class ) );

    try {
      schedulerService.pauseJob( "job-id" );
    } catch ( SchedulerException e ) {
      assertEquals( "pause-exception", e.getMessage() );
    }
  }

  @Test
  public void testPauseJobExecuteSchedulePermission() throws Exception {
    JobRequest jobRequest = mock( JobRequest.class );
    Job job = mock( Job.class );

    doReturn( job ).when( schedulerService.scheduler ).getJob( nullable( String.class ) );
    doReturn( false ).when( schedulerService ).isScheduleAllowed();
    doReturn( true ).when( schedulerService ).isExecuteScheduleAllowed();
    doNothing().when( schedulerService.scheduler ).pauseJob( nullable( String.class ) );

    JobState state = schedulerService.pauseJob( jobRequest.getJobId() );
    assertNull( state );

    verify( schedulerService.scheduler, times( 2 ) ).getJob( nullable( String.class ) );
    verify( schedulerService.scheduler, times( 1 ) ).pauseJob( nullable( String.class ) );
    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
    verify( schedulerService, times( 1 ) ).isExecuteScheduleAllowed();
  }

  @Test
  public void testPauseJobSameUser() throws Exception {
    JobRequest jobRequest = mock( JobRequest.class );
    Job job = mock( Job.class );
    IPentahoSession mockSession = mock( IPentahoSession.class );
    final String username = "username";

    doReturn( job ).when( schedulerService.scheduler ).getJob( nullable( String.class ) );
    doReturn( false ).when( schedulerService ).isScheduleAllowed();
    doReturn( false ).when( schedulerService ).isExecuteScheduleAllowed();
    doNothing().when( schedulerService.scheduler ).pauseJob( nullable( String.class ) );
    doReturn( mockSession ).when( schedulerService ).getSession();
    doReturn( username ).when( job ).getUserName();
    doReturn( username ).when( mockSession ).getName();

    JobState state = schedulerService.pauseJob( jobRequest.getJobId() );
    assertNull( state );

    verify( schedulerService.scheduler, times( 2 ) ).getJob( nullable( String.class ) );
    verify( schedulerService.scheduler, times( 1 ) ).pauseJob( nullable( String.class ) );
    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
    verify( schedulerService, times( 1 ) ).isExecuteScheduleAllowed();
  }

  @Test
  public void testPauseJobForbidden() throws Exception {
    JobRequest jobRequest = mock( JobRequest.class );
    Job job = mock( Job.class );
    IPentahoSession mockSession = mock( IPentahoSession.class );
    final String username1 = "username1";
    final String username2 = "username2";

    doReturn( job ).when( schedulerService.scheduler ).getJob( nullable( String.class ) );
    doReturn( false ).when( schedulerService ).isScheduleAllowed();
    doReturn( false ).when( schedulerService ).isExecuteScheduleAllowed();
    doReturn( mockSession ).when( schedulerService ).getSession();
    doReturn( username1 ).when( job ).getUserName();
    doReturn( username2 ).when( mockSession ).getName();

    JobState state = schedulerService.pauseJob( jobRequest.getJobId() );
    assertNull( state );

    verify( schedulerService.scheduler, times( 1 ) ).getJob( nullable( String.class ) );
    verify( schedulerService.scheduler, times( 0 ) ).pauseJob( nullable( String.class ) );
    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
    verify( schedulerService, times( 1 ) ).isExecuteScheduleAllowed();
  }

  @Test
  public void testResumeJob() throws SchedulerException {
    Job job = mock( Job.class );
    doReturn( job ).when( schedulerService ).getJob( nullable( String.class ) );
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    doNothing().when( schedulerService.scheduler ).resumeJob( nullable( String.class ) );
    JobState state = schedulerService.resumeJob( "job-id" );
    assertNull( state );
  }

  @Test
  public void testResumeJobException() throws SchedulerException {
    Job job = mock( Job.class );
    doReturn( job ).when( schedulerService ).getJob( nullable( String.class ) );
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    doThrow( new SchedulerException( "resume-exception" ) ).when( schedulerService.scheduler )
      .resumeJob( nullable( String.class ) );

    try {
      schedulerService.resumeJob( "job-id" );
    } catch ( SchedulerException e ) {
      assertEquals( "resume-exception", e.getMessage() );
    }
  }

  @Test
  public void testResumeJobExecuteSchedulePermission() throws Exception {
    JobRequest jobRequest = mock( JobRequest.class );
    Job job = mock( Job.class );

    doReturn( job ).when( schedulerService.scheduler ).getJob( nullable( String.class ) );
    doReturn( false ).when( schedulerService ).isScheduleAllowed();
    doReturn( true ).when( schedulerService ).isExecuteScheduleAllowed();
    doNothing().when( schedulerService.scheduler ).resumeJob( nullable( String.class ) );

    JobState state = schedulerService.resumeJob( jobRequest.getJobId() );
    assertNull( state );

    verify( schedulerService.scheduler, times( 2 ) ).getJob( nullable( String.class ) );
    verify( schedulerService.scheduler, times( 1 ) ).resumeJob( nullable( String.class ) );
    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
    verify( schedulerService, times( 1 ) ).isExecuteScheduleAllowed();
  }

  @Test
  public void testResumeJobSameUser() throws Exception {
    JobRequest jobRequest = mock( JobRequest.class );
    Job job = mock( Job.class );
    IPentahoSession mockSession = mock( IPentahoSession.class );
    final String username = "username";

    doReturn( job ).when( schedulerService.scheduler ).getJob( nullable( String.class ) );
    doReturn( false ).when( schedulerService ).isScheduleAllowed();
    doReturn( false ).when( schedulerService ).isExecuteScheduleAllowed();
    doNothing().when( schedulerService.scheduler ).resumeJob( nullable( String.class ) );
    doReturn( mockSession ).when( schedulerService ).getSession();
    doReturn( username ).when( job ).getUserName();
    doReturn( username ).when( mockSession ).getName();

    JobState state = schedulerService.resumeJob( jobRequest.getJobId() );
    assertNull( state );

    verify( schedulerService.scheduler, times( 2 ) ).getJob( nullable( String.class ) );
    verify( schedulerService.scheduler, times( 1 ) ).resumeJob( nullable( String.class ) );
    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
    verify( schedulerService, times( 1 ) ).isExecuteScheduleAllowed();
  }

  @Test
  public void testResumeJobForbidden() throws Exception {
    JobRequest jobRequest = mock( JobRequest.class );
    Job job = mock( Job.class );
    IPentahoSession mockSession = mock( IPentahoSession.class );
    final String username1 = "username1";
    final String username2 = "username2";

    doReturn( job ).when( schedulerService.scheduler ).getJob( nullable( String.class ) );
    doReturn( false ).when( schedulerService ).isScheduleAllowed();
    doReturn( false ).when( schedulerService ).isExecuteScheduleAllowed();
    doReturn( mockSession ).when( schedulerService ).getSession();
    doReturn( username1 ).when( job ).getUserName();
    doReturn( username2 ).when( mockSession ).getName();

    JobState state = schedulerService.resumeJob( jobRequest.getJobId() );
    assertNull( state );

    verify( schedulerService.scheduler, times( 1 ) ).getJob( nullable( String.class ) );
    verify( schedulerService.scheduler, times( 0 ) ).resumeJob( nullable( String.class ) );
    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
    verify( schedulerService, times( 1 ) ).isExecuteScheduleAllowed();
  }

  @Test
  public void testRemoveJob() throws SchedulerException {
    Job job = mock( Job.class );
    doReturn( job ).when( schedulerService ).getJob( nullable( String.class ) );
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    doNothing().when( schedulerService.scheduler ).removeJob( nullable( String.class ) );
    boolean result = schedulerService.removeJob( "job-id" );
    assertTrue( result );
  }

  @Test
  public void testRemoveJobException() throws SchedulerException {
    Job job = mock( Job.class );
    doReturn( job ).when( schedulerService ).getJob( nullable( String.class ) );
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    doThrow( new SchedulerException( "remove-exception" ) ).when( schedulerService.scheduler )
      .removeJob( nullable( String.class ) );

    try {
      schedulerService.removeJob( "job-id" );
    } catch ( SchedulerException e ) {
      assertEquals( "remove-exception", e.getMessage() );
    }
  }

  @Test
  public void testShutdown() throws SchedulerException {
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );
    doNothing().when( schedulerService.scheduler ).shutdown();
    doReturn( IScheduler.SchedulerStatus.STOPPED ).when( schedulerService.scheduler ).getStatus();

    //Test 1
    String state = schedulerService.shutdown();
    assertEquals( "STOPPED", state );

    //Test 2
    doReturn( IScheduler.SchedulerStatus.RUNNING ).when( schedulerService.scheduler ).getStatus();
    doReturn( false ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    state = schedulerService.shutdown();

    assertEquals( "RUNNING", state );

    verify( schedulerService.policy, times( 2 ) ).isAllowed( SchedulerAction.NAME );
    verify( schedulerService.scheduler, times( 1 ) ).shutdown();
    verify( schedulerService.scheduler, times( 2 ) ).getStatus();
  }


  @Test
  public void testShutdownException() throws SchedulerException {
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );
    doThrow( new SchedulerException( "" ) ).when( schedulerService.scheduler ).shutdown();

    try {
      schedulerService.shutdown();
      fail();
    } catch ( SchedulerException e ) {
      //Should go here
    }

    verify( schedulerService.policy ).isAllowed( SchedulerAction.NAME );
    verify( schedulerService.scheduler ).shutdown();
  }

  @Test
  public void testGetJobs() throws Exception {
    IPentahoSession mockPentahoSession = mock( IPentahoSession.class );

    doReturn( mockPentahoSession ).when( schedulerService ).getSession();
    doReturn( "admin" ).when( mockPentahoSession ).getName();
    doReturn( true ).when( schedulerService ).canAdminister();
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    List<IJob> mockJobs = new ArrayList<>();
    mockJobs.add( mock( IJob.class ) );
    doReturn( mockJobs ).when( schedulerService.scheduler ).getJobs( any( IJobFilter.class ) );

    List<IJob> jobs = schedulerService.getJobs();

    assertEquals( mockJobs, jobs );

    verify( schedulerService, times( 1 ) ).getSession();
    verify( mockPentahoSession, times( 1 ) ).getName();
    verify( schedulerService, times( 1 ) ).canAdminister();
    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
    verify( schedulerService.scheduler, times( 1 ) ).getJobs( any( IJobFilter.class ) );
  }

  @Test
  public void testGetJobsForbidden() throws Exception {
    doReturn( false ).when( schedulerService ).isScheduleAllowed();

    try {
      schedulerService.getJobs();
      fail();
    } catch ( IllegalAccessException e ) {
      // Expected
    }

    verify( schedulerService, times( 0 ) ).canAdminister();
    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
    verify( schedulerService.scheduler, times( 0 ) ).getJobs( any( IJobFilter.class ) );
  }

  @Test
  public void testGetJobsExecuteSchedulePermission() throws Exception {
    IPentahoSession mockPentahoSession = mock( IPentahoSession.class );

    doReturn( mockPentahoSession ).when( schedulerService ).getSession();
    doReturn( "bob" ).when( mockPentahoSession ).getName();
    doReturn( false ).when( schedulerService ).canAdminister();
    doReturn( false ).when( schedulerService ).isScheduleAllowed();
    doReturn( true ).when( schedulerService ).isExecuteScheduleAllowed();
    List<IJob> mockJobs = new ArrayList<>();
    mockJobs.add( mock( IJob.class ) );
    doReturn( mockJobs ).when( schedulerService.scheduler ).getJobs( any( IJobFilter.class ) );

    List<IJob> jobs = schedulerService.getJobs();

    assertEquals( mockJobs, jobs );

    verify( schedulerService, times( 1 ) ).getSession();
    verify( mockPentahoSession, times( 1 ) ).getName();
    verify( schedulerService, times( 1 ) ).canAdminister();
    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
    verify( schedulerService, times( 2 ) ).isExecuteScheduleAllowed();
    verify( schedulerService.scheduler, times( 1 ) ).getJobs( any( IJobFilter.class ) );
  }

  @Test
  public void testDoGetGeneratedContentForSchedule() throws Exception {
    String lineageId = "test.prpt";

    FileService mockFileService = mock( FileService.class );
    doReturn( mockFileService ).when( schedulerService ).getFileService();

    SessionResource mockSessionResource = mock( SessionResource.class );
    doReturn( mockSessionResource ).when( schedulerService ).getSessionResource();

    String currentUserDir = "currentUserDir";
    doReturn( currentUserDir ).when( mockSessionResource ).doGetCurrentUserDir();

    List<RepositoryFileDto> mockList = mock( List.class );
    doReturn( mockList ).when( mockFileService )
      .searchGeneratedContent( currentUserDir, lineageId, IScheduler.RESERVEDMAPKEY_LINEAGE_ID );

    List<RepositoryFileDto> list = schedulerService.doGetGeneratedContentForSchedule( lineageId );
    assertEquals( mockList, list );
  }

  @Test
  public void testGetJobState() throws Exception {
    JobRequest mockJobRequest = mock( JobRequest.class );

    String jobId = "jobId";
    doReturn( jobId ).when( mockJobRequest ).getJobId();

    IPentahoSession mockSession = mock( IPentahoSession.class );
    doReturn( mockSession ).when( schedulerService ).getSession();

    Job mockJob = mock( Job.class );
    doReturn( mockJob ).when( schedulerService ).getJob( jobId );
    doReturn( JobState.BLOCKED ).when( mockJob ).getState();

    String username = "username";
    doReturn( username ).when( mockJob ).getUserName();
    doReturn( username ).when( mockSession ).getName();

    // Test 1
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    JobState testState = schedulerService.getJobState( mockJobRequest );
    assertEquals( JobState.BLOCKED, testState );

    // Test 2
    doReturn( false ).when( schedulerService ).isScheduleAllowed();
    testState = schedulerService.getJobState( mockJobRequest );
    assertEquals( JobState.BLOCKED, testState );

    verify( mockJobRequest, times( 2 ) ).getJobId();
    verify( schedulerService, times( 1 ) ).getSession();
    verify( schedulerService, times( 2 ) ).getJob( jobId );
    verify( mockJob, times( 2 ) ).getState();
    verify( mockJob, times( 1 ) ).getUserName();
    verify( mockSession, times( 1 ) ).getName();
  }

  @Test
  public void testGetJobStateError() throws Exception {
    JobRequest mockJobRequest = mock( JobRequest.class );

    String jobId = "jobId";
    doReturn( jobId ).when( mockJobRequest ).getJobId();

    IPentahoSession mockSession = mock( IPentahoSession.class );
    doReturn( mockSession ).when( schedulerService ).getSession();

    Job mockJob = mock( Job.class );
    doReturn( mockJob ).when( schedulerService ).getJob( jobId );

    String username = "username";
    doReturn( username ).when( mockJob ).getUserName();

    String sessionName = "notUsername";
    doReturn( sessionName ).when( mockSession ).getName();

    doReturn( false ).when( schedulerService ).isScheduleAllowed();

    try {
      schedulerService.getJobState( mockJobRequest );
      fail();
    } catch ( UnsupportedOperationException e ) {
      // Expected
    }
  }

  @Test
  public void testGetJobInfo() throws Exception {
    String jobId = "jobId";

    Job mockJob = mock( Job.class );
    doReturn( mockJob ).when( schedulerService ).getJob( jobId );

    IPentahoSession mockPentahoSession = mock( IPentahoSession.class );
    doReturn( mockPentahoSession ).when( schedulerService ).getSession();

    String sessionName = "sessionName";
    doReturn( sessionName ).when( mockPentahoSession ).getName();
    doReturn( sessionName ).when( mockJob ).getUserName();

    Map<String, Serializable> mockJobParams = mock( Map.class );
    doReturn( mockJobParams ).when( mockJob ).getJobParams();

    Set<String> jobParamsKeyset = new HashSet<>();
    doReturn( jobParamsKeyset ).when( mockJobParams ).keySet();

    String jobParamKey = "key";
    jobParamsKeyset.add( jobParamKey );

    String value = "value";
    String[] testArray = new String[] { value };
    doReturn( testArray ).when( mockJobParams ).get( jobParamKey );

    // Test 1
    IJob testJob = schedulerService.getJobInfo( jobId );
    assertEquals( mockJob, testJob );

    // Test 2
    testJob = schedulerService.getJobInfo( jobId );
    assertEquals( mockJob, testJob );

    verify( mockJobParams, times( 2 ) ).put( eq( jobParamKey ), any( Serializable.class ) );
    verify( schedulerService, times( 2 ) ).getJob( jobId );
    verify( schedulerService, times( 2 ) ).getSession();
    verify( mockPentahoSession, times( 2 ) ).getName();
    verify( mockJob, times( 2 ) ).getUserName();
    verify( mockJob, times( 6 ) ).getJobParams();
    verify( mockJobParams, times( 2 ) ).keySet();
    verify( mockJobParams, times( 2 ) ).get( jobParamKey );
    verify( schedulerService, times( 2 ) ).canAdminister();
  }

  @Test
  public void testGetJobInfoError() throws Exception {
    String jobId = "jobId";

    IJob mockJob = mock( IJob.class );
    doReturn( mockJob ).when( schedulerService ).getJob( jobId );

    try {
      schedulerService.getJobInfo( jobId );
      fail();
    } catch ( RuntimeException e ) {
      // Expected
    }
  }

  @Test
  public void testIsScheduleAllowed() {
    // Test 1
    doReturn( true ).when( schedulerService ).isScheduleAllowed();

    Map<String, Serializable> metadata = mock( Map.class );

    doReturn( metadata ).when( schedulerService.repository ).getFileMetadata( nullable( String.class ) );

    doReturn( true ).when( metadata ).containsKey( RepositoryFile.SCHEDULABLE_KEY );
    doReturn( "true" ).when( metadata ).get( RepositoryFile.SCHEDULABLE_KEY );

    boolean canSchedule = schedulerService.isScheduleAllowed( nullable( String.class ) );

    assertTrue( canSchedule );

    // Test 2
    doReturn( false ).when( schedulerService ).isScheduleAllowed();

    canSchedule = schedulerService.isScheduleAllowed( nullable( String.class ) );

    assertFalse( canSchedule );

    // Test 3
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    doReturn( false ).when( metadata ).containsKey( RepositoryFile.SCHEDULABLE_KEY );

    canSchedule = schedulerService.isScheduleAllowed( nullable( String.class ) );

    assertTrue( canSchedule );

    // Test 4
    doReturn( true ).when( metadata ).containsKey( RepositoryFile.SCHEDULABLE_KEY );
    doReturn( "false" ).when( metadata ).get( RepositoryFile.SCHEDULABLE_KEY );

    canSchedule = schedulerService.isScheduleAllowed( nullable( String.class ) );

    assertFalse( canSchedule );

    verify( schedulerService, times( 4 ) ).isScheduleAllowed();
    verify( schedulerService.repository, times( 3 ) ).getFileMetadata( nullable( String.class ) );
    verify( metadata, times( 3 ) ).containsKey( RepositoryFile.SCHEDULABLE_KEY );
    verify( metadata, times( 2 ) ).get( RepositoryFile.SCHEDULABLE_KEY );
  }

  @Test
  public void testGetBlockoutJobs() throws Exception {
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    List<IJob> mockJobs = new ArrayList<>();
    mockJobs.add( mock( IJob.class ) );
    doReturn( mockJobs ).when( schedulerService.blockoutManager ).getBlockOutJobs();

    List<IJob> jobs = schedulerService.getBlockOutJobs();

    assertEquals( mockJobs, jobs );

    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
    verify( schedulerService.blockoutManager, times( 1 ) ).getBlockOutJobs();
  }

  @Test
  public void testGetBlockoutJobsForbidden() {
    doReturn( false ).when( schedulerService ).isScheduleAllowed();

    try {
      schedulerService.getBlockOutJobs();
      fail();
    } catch ( IllegalAccessException e ) {
      // Expected
    }

    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
    verify( schedulerService.blockoutManager, times( 0 ) ).getBlockOutJobs();
  }

  @Test
  public void testGetBlockoutJobsExecuteSchedulePermission() throws Exception {
    doReturn( false ).when( schedulerService ).isScheduleAllowed();
    doReturn( true ).when( schedulerService ).isExecuteScheduleAllowed();
    List<IJob> mockJobs = new ArrayList<>();
    mockJobs.add( mock( IJob.class ) );
    doReturn( mockJobs ).when( schedulerService.blockoutManager ).getBlockOutJobs();

    List<IJob> jobs = schedulerService.getBlockOutJobs();

    assertEquals( mockJobs, jobs );

    verify( schedulerService, times( 1 ) ).isScheduleAllowed();
    verify( schedulerService, times( 1 ) ).isExecuteScheduleAllowed();
    verify( schedulerService.blockoutManager, times( 1 ) ).getBlockOutJobs();
  }

  @Test
  public void testHasBlockouts() {
    List<IJob> jobs = new ArrayList<>();

    doReturn( jobs ).when( schedulerService.blockoutManager ).getBlockOutJobs();

    // Test 1
    boolean hasBlockouts = schedulerService.hasBlockouts();

    assertFalse( hasBlockouts );

    // Test 2
    jobs.add( mock( IJob.class ) );
    hasBlockouts = schedulerService.hasBlockouts();

    assertTrue( hasBlockouts );

    verify( schedulerService.blockoutManager, times( 2 ) ).getBlockOutJobs();
  }

  @Test
  public void testAddBlockout() throws Exception {
    JobScheduleRequest jobScheduleRequest = mock( JobScheduleRequest.class );
    Job jobMock = mock( Job.class );

    JobScheduleParam jobScheduleParamMock1 = mock( JobScheduleParam.class );
    JobScheduleParam jobScheduleParamMock2 = mock( JobScheduleParam.class );

    List<JobScheduleParam> jobScheduleParams = new ArrayList<>();

    doReturn( true ).when( schedulerService ).canAdminister();
    doNothing().when( jobScheduleRequest ).setActionClass( nullable( String.class ) );
    doReturn( jobScheduleParams ).when( jobScheduleRequest ).getJobParameters();
    doReturn( jobScheduleParamMock1 ).when( schedulerService )
      .getJobScheduleParam( nullable( String.class ), nullable( String.class ) );
    doReturn( jobScheduleParamMock2 ).when( schedulerService )
      .getJobScheduleParam( nullable( String.class ), anyLong() );
    doReturn( jobMock ).when( schedulerService ).createJob( any( JobScheduleRequest.class ) );

    IJob job = schedulerService.addBlockout( jobScheduleRequest );

    assertNotNull( job );
    assertEquals( 2, jobScheduleParams.size() );

    verify( schedulerService ).canAdminister();
    verify( jobScheduleRequest ).setActionClass( nullable( String.class ) );
    verify( jobScheduleRequest, times( 2 ) ).getJobParameters();
    verify( schedulerService ).createJob( any( JobScheduleRequest.class ) );
  }

  @Test
  public void testAddBlockoutException() throws Exception {
    // Test 1
    JobScheduleRequest jobScheduleRequest = mock( JobScheduleRequest.class );
    doReturn( false ).when( schedulerService ).canAdminister();

    try {
      schedulerService.addBlockout( jobScheduleRequest );
      fail();
    } catch ( IllegalAccessException e ) {
      //Should catch exception
    }

    // Test 2
    JobScheduleParam jobScheduleParamMock1 = mock( JobScheduleParam.class );
    JobScheduleParam jobScheduleParamMock2 = mock( JobScheduleParam.class );

    List<JobScheduleParam> jobScheduleParams = new ArrayList<>();

    doReturn( true ).when( schedulerService ).canAdminister();
    doNothing().when( jobScheduleRequest ).setActionClass( nullable( String.class ) );
    doReturn( jobScheduleParams ).when( jobScheduleRequest ).getJobParameters();
    doReturn( jobScheduleParamMock1 ).when( schedulerService )
      .getJobScheduleParam( nullable( String.class ), nullable( String.class ) );
    doReturn( jobScheduleParamMock2 ).when( schedulerService )
      .getJobScheduleParam( nullable( String.class ), anyLong() );

    doThrow( new IOException() ).when( schedulerService ).createJob( jobScheduleRequest );

    try {
      schedulerService.addBlockout( jobScheduleRequest );
      fail();
    } catch ( IOException e ) {
      //Should catch exception
    }

    // Test 3
    doThrow( new SchedulerException( "" ) ).when( schedulerService ).createJob( jobScheduleRequest );

    try {
      schedulerService.addBlockout( jobScheduleRequest );
      fail();
    } catch ( SchedulerException e ) {
      //Should catch exception
    }

    verify( schedulerService, times( 3 ) ).canAdminister();
    verify( jobScheduleRequest, times( 2 ) ).setActionClass( nullable( String.class ) );
    verify( jobScheduleRequest, times( 4 ) ).getJobParameters();
    verify( schedulerService, times( 2 ) ).createJob( any( JobScheduleRequest.class ) );
  }

  @Test
  public void testUpdateBlockout() throws Exception {
    String jobId = "jobId";
    JobScheduleRequest jobScheduleRequest = mock( JobScheduleRequest.class );
    IJob jobMock = mock( IJob.class );

    doReturn( true ).when( schedulerService ).canAdminister();
    doReturn( true ).when( schedulerService ).removeJob( nullable( String.class ) );
    doReturn( jobMock ).when( schedulerService ).addBlockout( jobScheduleRequest );

    IJob job = schedulerService.updateBlockout( jobId, jobScheduleRequest );

    assertNotNull( job );

    verify( schedulerService ).canAdminister();
    verify( schedulerService ).removeJob( nullable( String.class ) );
    verify( schedulerService ).addBlockout( jobScheduleRequest );
  }

  @Test
  public void testUpdateBlockoutException() throws Exception {
    String jobId = "jobId";
    JobScheduleRequest jobScheduleRequest = mock( JobScheduleRequest.class );

    // Test 1
    doReturn( false ).when( schedulerService ).canAdminister();

    try {
      schedulerService.updateBlockout( jobId, jobScheduleRequest );
      fail();
    } catch ( IllegalAccessException e ) {
      // Should catch the exception
    }

    // Test 2
    doReturn( true ).when( schedulerService ).canAdminister();
    doThrow( new SchedulerException( "" ) ).when( schedulerService ).removeJob( nullable( String.class ) );

    try {
      schedulerService.updateBlockout( jobId, jobScheduleRequest );
      fail();
    } catch ( SchedulerException e ) {
      // Should catch the exception
    }

    // Test 3
    doReturn( false ).when( schedulerService ).removeJob( nullable( String.class ) );

    try {
      schedulerService.updateBlockout( jobId, jobScheduleRequest );
      fail();
    } catch ( IllegalAccessException e ) {
      // Should catch the exception
    }

    // Test 4
    doReturn( true ).when( schedulerService ).removeJob( nullable( String.class ) );
    doThrow( new IOException() ).when( schedulerService ).addBlockout( jobScheduleRequest );

    try {
      schedulerService.updateBlockout( jobId, jobScheduleRequest );
      fail();
    } catch ( IOException e ) {
      // Should catch the exception
    }

    // Test 5
    doThrow( new SchedulerException( "" ) ).when( schedulerService ).addBlockout( jobScheduleRequest );

    try {
      schedulerService.updateBlockout( jobId, jobScheduleRequest );
      fail();
    } catch ( SchedulerException e ) {
      // Should catch the exception
    }

    verify( schedulerService, times( 5 ) ).canAdminister();
    verify( schedulerService, times( 4 ) ).removeJob( nullable( String.class ) );
    verify( schedulerService, times( 2 ) ).addBlockout( jobScheduleRequest );
  }

  @Test
  public void testWillFire() {
    IJobTrigger jobTrigger = mock( IJobTrigger.class );

    // Test 1
    doReturn( true ).when( schedulerService.blockoutManager ).willFire( jobTrigger );

    boolean willFire = schedulerService.willFire( jobTrigger );

    assertTrue( willFire );

    // Test 2
    doReturn( false ).when( schedulerService.blockoutManager ).willFire( jobTrigger );

    willFire = schedulerService.willFire( jobTrigger );

    assertFalse( willFire );

    verify( schedulerService.blockoutManager, times( 2 ) ).willFire( jobTrigger );
  }

  @Test
  public void testShouldFireNow() {
    // Test 1
    doReturn( true ).when( schedulerService.blockoutManager ).shouldFireNow();

    boolean shouldFireNow = schedulerService.shouldFireNow();

    assertTrue( shouldFireNow );

    // Test 2
    doReturn( false ).when( schedulerService.blockoutManager ).shouldFireNow();

    shouldFireNow = schedulerService.shouldFireNow();

    assertFalse( shouldFireNow );

    verify( schedulerService.blockoutManager, times( 2 ) ).shouldFireNow();
  }

  @Test
  public void testGetBlockStatus() throws Exception {
    JobScheduleRequest jobScheduleRequestMock = mock( JobScheduleRequest.class );
    BlockStatusProxy blockStatusProxyMock = mock( BlockStatusProxy.class );
    IJobTrigger jobTrigger = mock( IJobTrigger.class );

    doReturn( jobTrigger ).when( schedulerService ).convertScheduleRequestToJobTrigger( jobScheduleRequestMock );
    doReturn( true ).when( schedulerService.blockoutManager ).isPartiallyBlocked( jobTrigger );
    doReturn( true ).when( schedulerService.blockoutManager ).willFire( jobTrigger );
    doReturn( blockStatusProxyMock ).when( schedulerService ).getBlockStatusProxy( anyBoolean(), anyBoolean() );

    // Test 1
    BlockStatusProxy blockStatusProxy = schedulerService.getBlockStatus( jobScheduleRequestMock );

    assertNotNull( blockStatusProxy );

    // Test 2
    doReturn( false ).when( schedulerService.blockoutManager ).isPartiallyBlocked( jobTrigger );

    blockStatusProxy = schedulerService.getBlockStatus( jobScheduleRequestMock );

    assertNotNull( blockStatusProxy );

    verify( schedulerService, times( 2 ) ).convertScheduleRequestToJobTrigger( jobScheduleRequestMock );
    verify( schedulerService.blockoutManager, times( 2 ) ).isPartiallyBlocked( jobTrigger );
    verify( schedulerService, times( 2 ) ).getBlockStatusProxy( anyBoolean(), anyBoolean() );
    verify( schedulerService.blockoutManager, times( 1 ) ).willFire( jobTrigger );
  }

  @Test
  public void testGetBlockStatusException() throws Exception {
    JobScheduleRequest jobScheduleRequestMock = mock( JobScheduleRequest.class );

    doThrow( new SchedulerException( "" ) ).when( schedulerService )
      .convertScheduleRequestToJobTrigger( jobScheduleRequestMock );

    try {
      schedulerService.getBlockStatus( jobScheduleRequestMock );
      fail();
    } catch ( SchedulerException e ) {
      // Should catch the exception
    }

    verify( schedulerService ).convertScheduleRequestToJobTrigger( jobScheduleRequestMock );
  }
}
