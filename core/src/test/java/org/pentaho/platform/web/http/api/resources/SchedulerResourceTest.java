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

import com.google.gwt.thirdparty.guava.common.collect.Maps;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.pentaho.platform.api.repository2.unified.UnifiedRepositoryException;
import org.pentaho.platform.api.scheduler2.IJob;
import org.pentaho.platform.api.scheduler2.IJobTrigger;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.JobState;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.scheduler2.JobWrapper;
import org.pentaho.platform.web.http.api.proxies.BlockStatusProxy;
import org.pentaho.platform.web.http.api.resources.services.ISchedulerServicePlugin;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pentaho.platform.web.http.api.resources.SchedulerResource.REMOVED_JOB_STATE;

@SuppressWarnings( { "unchecked", "deprecation", "ConstantValue" } )
public class SchedulerResourceTest {
  SchedulerResource schedulerResource;

  @Before
  public void setUp() {
    schedulerResource = Mockito.spy( new SchedulerResource() );
    schedulerResource.schedulerService = mock( ISchedulerServicePlugin.class );
  }

  @After
  public void tearDown() {
    schedulerResource = null;
  }

  @Test
  public void testCreateJob() throws Exception {
    JobScheduleRequest mockRequest = mock( JobScheduleRequest.class );

    Job mockJob = mock( Job.class );
    doReturn( mockJob ).when( schedulerResource.schedulerService ).createJob( mockRequest );

    String jobId = "jobId";
    doReturn( jobId ).when( mockJob ).getJobId();

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildPlainTextOkResponse( jobId );

    Response testResponse = schedulerResource.createJob( mockRequest );
    assertEquals( mockResponse, testResponse );

    verify( schedulerResource.schedulerService, times( 1 ) ).createJob( mockRequest );
    verify( mockJob, times( 1 ) ).getJobId();
    verify( schedulerResource, times( 1 ) ).buildPlainTextOkResponse( jobId );
  }

  @Test
  public void testCreateJobError() throws Exception {
    JobScheduleRequest mockRequest = mock( JobScheduleRequest.class );

    SchedulerException mockSchedulerException = mock( SchedulerException.class );

    Throwable mockSchedulerExceptionCause = mock( Throwable.class );
    doReturn( mockSchedulerExceptionCause ).when( mockSchedulerException ).getCause();

    String schedulerExceptionMessage = "schedulerExceptionMessage";
    doReturn( schedulerExceptionMessage ).when( mockSchedulerExceptionCause ).getMessage();

    Response mockSchedulerExceptionResponse = mock( Response.class );
    doReturn( mockSchedulerExceptionResponse ).when( schedulerResource )
      .buildServerErrorResponse( schedulerExceptionMessage );

    IOException mockIOException = mock( IOException.class );

    Throwable mockIOExceptionCause = mock( Throwable.class );
    doReturn( mockIOExceptionCause ).when( mockIOException ).getCause();

    String ioExceptionMessage = "ioExceptionMessage";
    doReturn( ioExceptionMessage ).when( mockIOExceptionCause ).getMessage();

    Response mockIOExceptionResponse = mock( Response.class );
    doReturn( mockIOExceptionResponse ).when( schedulerResource ).buildServerErrorResponse( ioExceptionMessage );

    Response mockUnauthorizedResponse = mock( Response.class );
    doReturn( mockUnauthorizedResponse ).when( schedulerResource ).buildStatusResponse( UNAUTHORIZED );

    Response mockForbiddenResponse = mock( Response.class );
    doReturn( mockForbiddenResponse ).when( schedulerResource ).buildStatusResponse( FORBIDDEN );

    // Test 1
    Mockito.doThrow( mockSchedulerException ).when( schedulerResource.schedulerService ).createJob( mockRequest );

    Response testResponse = schedulerResource.createJob( mockRequest );
    assertEquals( mockSchedulerExceptionResponse, testResponse );

    // Test 2
    Mockito.doThrow( mockIOException ).when( schedulerResource.schedulerService ).createJob( mockRequest );

    testResponse = schedulerResource.createJob( mockRequest );
    assertEquals( mockIOExceptionResponse, testResponse );

    // Test 3
    SecurityException mockSecurityException = mock( SecurityException.class );
    Mockito.doThrow( mockSecurityException ).when( schedulerResource.schedulerService ).createJob( mockRequest );

    testResponse = schedulerResource.createJob( mockRequest );
    assertEquals( mockUnauthorizedResponse, testResponse );

    // Test 4
    IllegalAccessException mockIllegalAccessException = mock( IllegalAccessException.class );
    Mockito.doThrow( mockIllegalAccessException ).when( schedulerResource.schedulerService ).createJob( mockRequest );

    testResponse = schedulerResource.createJob( mockRequest );
    assertEquals( mockForbiddenResponse, testResponse );

    verify( schedulerResource, times( 1 ) ).buildServerErrorResponse( schedulerExceptionMessage );
    verify( schedulerResource, times( 1 ) ).buildServerErrorResponse( ioExceptionMessage );
    verify( schedulerResource, times( 1 ) ).buildStatusResponse( UNAUTHORIZED );
    verify( schedulerResource, times( 1 ) ).buildStatusResponse( FORBIDDEN );
    verify( schedulerResource.schedulerService, times( 4 ) ).createJob( mockRequest );
  }

  @Test
  public void testTriggerNow() throws Exception {
    JobRequest mockJobRequest = mock( JobRequest.class );

    String jobId = "jobId";
    doReturn( jobId ).when( mockJobRequest ).getJobId();

    Job mockJob = mock( Job.class );
    doReturn( mockJob ).when( schedulerResource.schedulerService ).triggerNow( jobId );

    JobState mockJobState = JobState.BLOCKED;
    doReturn( mockJobState ).when( mockJob ).getState();

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildPlainTextOkResponse( mockJobState.name() );

    Response testResponse = schedulerResource.triggerNow( mockJobRequest );
    assertEquals( mockResponse, testResponse );

    verify( mockJobRequest, times( 1 ) ).getJobId();
    verify( schedulerResource.schedulerService, times( 1 ) ).triggerNow( jobId );
    verify( mockJob, times( 1 ) ).getState();
    verify( schedulerResource, times( 1 ) ).buildPlainTextOkResponse( mockJobState.name() );
  }

  @Test
  public void testTriggerNowError() throws Exception {
    JobRequest mockJobRequest = mock( JobRequest.class );

    String jobId = "jobId";
    doReturn( jobId ).when( mockJobRequest ).getJobId();

    SchedulerException mockSchedulerException = mock( SchedulerException.class );
    Mockito.doThrow( mockSchedulerException ).when( schedulerResource.schedulerService ).triggerNow( jobId );

    try {
      schedulerResource.triggerNow( mockJobRequest );
      fail();
    } catch ( RuntimeException e ) {
      // correct
    }

    verify( mockJobRequest, times( 1 ) ).getJobId();
    verify( schedulerResource.schedulerService, times( 1 ) ).triggerNow( jobId );
  }

  @Test
  public void testGetContentCleanerJob() throws Exception {
    Job mockJob = mock( Job.class );
    doReturn( mockJob ).when( schedulerResource.schedulerService ).getContentCleanerJob();

    IJob testJob = schedulerResource.getContentCleanerJob();
    assertEquals( mockJob, testJob );

    verify( schedulerResource.schedulerService, times( 1 ) ).getContentCleanerJob();
  }

  @Test
  public void testGetContentCleanerJobError() throws Exception {
    SchedulerException mockSchedulerException = mock( SchedulerException.class );
    Mockito.doThrow( mockSchedulerException ).when( schedulerResource.schedulerService ).getContentCleanerJob();

    try {
      schedulerResource.getContentCleanerJob();
      fail();
    } catch ( RuntimeException e ) {
      // correct
    }

    verify( schedulerResource.schedulerService, times( 1 ) ).getContentCleanerJob();
  }

  @Test
  public void testGetJobs() throws Exception {
    Boolean asCronString = Boolean.FALSE;
    List<IJob> mockJobs = mock( List.class );
    doReturn( mockJobs ).when( schedulerResource.schedulerService ).getJobs();

    JobWrapper testResult = schedulerResource.getJobs( asCronString );
    assertNotNull( testResult.getJobs() );
    assertEquals( mockJobs, testResult.getJobs() );

    verify( schedulerResource.schedulerService, times( 1 ) ).getJobs();
  }

  @Test
  public void testGetJobsError() throws Exception {
    Boolean asCronString = Boolean.FALSE;
    Mockito.doThrow( SchedulerException.class ).when( schedulerResource.schedulerService ).getJobs();

    try {
      schedulerResource.getJobs( asCronString );
      fail();
    } catch ( RuntimeException e ) {
      // correct
    }

    verify( schedulerResource.schedulerService, times( 1 ) ).getJobs();
  }

  @Test
  public void testGetAllJobsForbidden() throws Exception {
    Mockito.doThrow( IllegalAccessException.class ).when( schedulerResource.schedulerService ).getJobs();

    try {
      schedulerResource.getAllJobs();
      fail();
    } catch ( RuntimeException e ) {
      // correct
    }

    verify( schedulerResource.schedulerService, times( 1 ) ).getJobs();
  }

  @Test
  public void testIsScheduleAllowed() {
    String id = "id";

    boolean isScheduleAllowed = true;
    doReturn( isScheduleAllowed ).when( schedulerResource.schedulerService ).isScheduleAllowed( id );

    String testResult = schedulerResource.isScheduleAllowed( id );
    assertEquals( "" + isScheduleAllowed, testResult );

    verify( schedulerResource.schedulerService, times( 1 ) ).isScheduleAllowed( id );
  }

  @Test
  public void testDoGetCanSchedule() {
    String canSchedule = "true";
    doReturn( canSchedule ).when( schedulerResource.schedulerService ).doGetCanSchedule();

    String testResult = schedulerResource.doGetCanSchedule();
    assertEquals( canSchedule, testResult );

    verify( schedulerResource.schedulerService, times( 1 ) ).doGetCanSchedule();
  }

  @Test
  public void testDoGetCanExecuteSchedule() {
    String canExecuteSchedule = "true";
    doReturn( canExecuteSchedule ).when( schedulerResource.schedulerService ).doGetCanExecuteSchedule();

    String testResult = schedulerResource.doGetCanExecuteSchedules();
    assertEquals( canExecuteSchedule, testResult );

    verify( schedulerResource.schedulerService, times( 1 ) ).doGetCanExecuteSchedule();
  }

  @Test
  public void testGetState() throws Exception {
    String state = "state";
    doReturn( state ).when( schedulerResource.schedulerService ).getState();

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildPlainTextOkResponse( state );

    Response testResult = schedulerResource.getState();
    assertEquals( mockResponse, testResult );

    verify( schedulerResource.schedulerService, times( 1 ) ).getState();
    verify( schedulerResource, times( 1 ) ).buildPlainTextOkResponse( state );
  }

  @Test
  public void testGetStateError() throws Exception {
    SchedulerException mockSchedulerException = mock( SchedulerException.class );
    Mockito.doThrow( mockSchedulerException ).when( schedulerResource.schedulerService ).getState();

    try {
      schedulerResource.getState();
      fail();
    } catch ( RuntimeException e ) {
      // correct
    }

    verify( schedulerResource.schedulerService, times( 1 ) ).getState();
  }

  @Test
  public void testStart() throws Exception {
    String status = "state";
    doReturn( status ).when( schedulerResource.schedulerService ).start();

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildPlainTextOkResponse( status );

    Response testResult = schedulerResource.start();
    assertEquals( mockResponse, testResult );

    verify( schedulerResource.schedulerService, times( 1 ) ).start();
    verify( schedulerResource, times( 1 ) ).buildPlainTextOkResponse( status );
  }

  @Test
  public void testStartError() throws Exception {
    SchedulerException mockSchedulerException = mock( SchedulerException.class );
    Mockito.doThrow( mockSchedulerException ).when( schedulerResource.schedulerService ).start();

    try {
      schedulerResource.start();
      fail();
    } catch ( RuntimeException e ) {
      // correct
    }

    verify( schedulerResource.schedulerService, times( 1 ) ).start();
  }

  @Test
  public void testPause() throws Exception {
    String status = "state";
    doReturn( status ).when( schedulerResource.schedulerService ).pause();

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildPlainTextOkResponse( status );

    Response testResult = schedulerResource.pause();
    assertEquals( mockResponse, testResult );

    verify( schedulerResource.schedulerService, times( 1 ) ).pause();
    verify( schedulerResource, times( 1 ) ).buildPlainTextOkResponse( status );
  }

  @Test
  public void testPauseError() throws Exception {
    SchedulerException mockSchedulerException = mock( SchedulerException.class );
    Mockito.doThrow( mockSchedulerException ).when( schedulerResource.schedulerService ).pause();

    try {
      schedulerResource.pause();
      fail();
    } catch ( RuntimeException e ) {
      // correct
    }

    verify( schedulerResource.schedulerService, times( 1 ) ).pause();
  }

  @Test
  public void testShutdown() throws Exception {
    String status = "state";
    doReturn( status ).when( schedulerResource.schedulerService ).shutdown();

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildPlainTextOkResponse( status );

    Response testResult = schedulerResource.shutdown();
    assertEquals( mockResponse, testResult );

    verify( schedulerResource.schedulerService, times( 1 ) ).shutdown();
    verify( schedulerResource, times( 1 ) ).buildPlainTextOkResponse( status );
  }

  @Test
  public void testShutdownError() throws Exception {
    SchedulerException mockSchedulerException = mock( SchedulerException.class );
    Mockito.doThrow( mockSchedulerException ).when( schedulerResource.schedulerService ).shutdown();

    try {
      schedulerResource.shutdown();
      fail();
    } catch ( RuntimeException e ) {
      // correct
    }

    verify( schedulerResource.schedulerService, times( 1 ) ).shutdown();
  }

  @Test
  public void testGetJobState() throws Exception {
    JobRequest mockJobRequest = mock( JobRequest.class );

    JobState mockJobState = JobState.BLOCKED;
    doReturn( mockJobState ).when( schedulerResource.schedulerService ).getJobState( mockJobRequest );

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildPlainTextOkResponse( mockJobState.name() );

    Response testResponse = schedulerResource.getJobState( mockJobRequest );
    assertEquals( mockResponse, testResponse );

    verify( schedulerResource.schedulerService, times( 1 ) ).getJobState( mockJobRequest );
    verify( schedulerResource, times( 1 ) ).buildPlainTextOkResponse( mockJobState.name() );
  }

  @Test
  public void testGetJobStateError() throws Exception {
    JobRequest mockJobRequest = mock( JobRequest.class );

    Response mockUnauthorizedResponse = mock( Response.class );
    doReturn( mockUnauthorizedResponse ).when( schedulerResource ).buildPlainTextStatusResponse( UNAUTHORIZED );

    // Test 1
    UnsupportedOperationException mockUnsupportedOperationException = mock( UnsupportedOperationException.class );
    Mockito.doThrow( mockUnsupportedOperationException ).when( schedulerResource.schedulerService )
      .getJobState( mockJobRequest );

    Response testResponse = schedulerResource.getJobState( mockJobRequest );
    assertEquals( mockUnauthorizedResponse, testResponse );

    // Test 2
    SchedulerException mockSchedulerException = mock( SchedulerException.class );
    Mockito.doThrow( mockSchedulerException ).when( schedulerResource.schedulerService ).getJobState( mockJobRequest );

    try {
      schedulerResource.getJobState( mockJobRequest );
      fail();
    } catch ( RuntimeException e ) {
      // correct
    }

    verify( schedulerResource, times( 1 ) ).buildPlainTextStatusResponse( UNAUTHORIZED );
    verify( schedulerResource.schedulerService, times( 2 ) ).getJobState( mockJobRequest );
  }

  @Test
  public void testPauseJob() throws Exception {
    String jobId = "jobId";

    JobRequest mockJobRequest = mock( JobRequest.class );
    doReturn( jobId ).when( mockJobRequest ).getJobId();

    JobState state = JobState.BLOCKED;
    doReturn( state ).when( schedulerResource.schedulerService ).pauseJob( jobId );

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildPlainTextOkResponse( state.name() );

    Response testResult = schedulerResource.pauseJob( mockJobRequest );
    assertEquals( mockResponse, testResult );

    verify( schedulerResource.schedulerService, times( 1 ) ).pauseJob( jobId );
    verify( schedulerResource, times( 1 ) ).buildPlainTextOkResponse( state.name() );
  }

  @Test
  public void testPauseJobError() throws Exception {
    String jobId = "jobId";

    JobRequest mockJobRequest = mock( JobRequest.class );
    doReturn( jobId ).when( mockJobRequest ).getJobId();

    SchedulerException mockSchedulerException = mock( SchedulerException.class );
    Mockito.doThrow( mockSchedulerException ).when( schedulerResource.schedulerService ).pauseJob( jobId );

    try {
      schedulerResource.pauseJob( mockJobRequest );
      fail();
    } catch ( RuntimeException e ) {
      // correct
    }

    verify( schedulerResource.schedulerService, times( 1 ) ).pauseJob( jobId );
  }

  @Test
  public void testResumeJob() throws Exception {
    String jobId = "jobId";

    JobRequest mockJobRequest = mock( JobRequest.class );
    doReturn( jobId ).when( mockJobRequest ).getJobId();

    JobState state = JobState.BLOCKED;
    doReturn( state ).when( schedulerResource.schedulerService ).resumeJob( jobId );

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildPlainTextOkResponse( state.name() );

    Response testResult = schedulerResource.resumeJob( mockJobRequest );
    assertEquals( mockResponse, testResult );

    verify( schedulerResource.schedulerService, times( 1 ) ).resumeJob( jobId );
    verify( schedulerResource, times( 1 ) ).buildPlainTextOkResponse( state.name() );
  }

  @Test
  public void testResumeJobError() throws Exception {
    String jobId = "jobId";

    JobRequest mockJobRequest = mock( JobRequest.class );
    doReturn( jobId ).when( mockJobRequest ).getJobId();

    SchedulerException mockSchedulerException = mock( SchedulerException.class );
    Mockito.doThrow( mockSchedulerException ).when( schedulerResource.schedulerService ).resumeJob( jobId );

    try {
      schedulerResource.resumeJob( mockJobRequest );
      fail();
    } catch ( RuntimeException e ) {
      // correct
    }

    verify( schedulerResource.schedulerService, times( 1 ) ).resumeJob( jobId );
  }

  @Test
  public void testRemoveJob() throws Exception {
    JobRequest mockJobRequest = mock( JobRequest.class );

    String jobId = "jobId";
    doReturn( jobId ).when( mockJobRequest ).getJobId();

    IJob mockJob = mock( IJob.class );
    doReturn( mockJob ).when( schedulerResource.schedulerService ).getJob( jobId );

    JobState mockJobState = JobState.BLOCKED;
    doReturn( mockJobState ).when( mockJob ).getState();

    Response mockRemovedResponse = mock( Response.class );
    doReturn( mockRemovedResponse ).when( schedulerResource ).buildPlainTextOkResponse( REMOVED_JOB_STATE );

    Response mockJobStateResponse = mock( Response.class );
    doReturn( mockJobStateResponse ).when( schedulerResource ).buildPlainTextOkResponse( mockJobState.name() );

    // Test 1
    doReturn( true ).when( schedulerResource.schedulerService ).removeJob( jobId );

    Response testResponse = schedulerResource.removeJob( mockJobRequest );
    assertEquals( mockRemovedResponse, testResponse );

    // Test 2
    doReturn( false ).when( schedulerResource.schedulerService ).removeJob( jobId );
    testResponse = schedulerResource.removeJob( mockJobRequest );
    assertEquals( mockJobStateResponse, testResponse );

    verify( mockJobRequest, times( 2 ) ).getJobId();
    verify( schedulerResource.schedulerService, times( 1 ) ).getJob( jobId );
    verify( mockJob, times( 1 ) ).getState();
    verify( schedulerResource, times( 1 ) ).buildPlainTextOkResponse( REMOVED_JOB_STATE );
    verify( schedulerResource, times( 1 ) ).buildPlainTextOkResponse( mockJobState.name() );
  }

  @Test
  public void testRemoveJobError() throws Exception {
    String jobId = "jobId";

    JobRequest mockJobRequest = mock( JobRequest.class );
    doReturn( jobId ).when( mockJobRequest ).getJobId();

    SchedulerException mockSchedulerException = mock( SchedulerException.class );
    Mockito.doThrow( mockSchedulerException ).when( schedulerResource.schedulerService ).removeJob( jobId );

    try {
      schedulerResource.removeJob( mockJobRequest );
      fail();
    } catch ( RuntimeException e ) {
      // correct
    }

    verify( schedulerResource.schedulerService, times( 1 ) ).removeJob( jobId );
  }

  @Test
  public void testRemoveJobs() throws Exception {
    JobsRequest mockJobsRequest = mock( JobsRequest.class );

    List<String> jobIds = new ArrayList<>();
    jobIds.add( "jobId" );
    jobIds.add( "jobId2" );

    doReturn( jobIds ).when( mockJobsRequest ).getJobIds();
    JobsResponse mockJobsResponse = new JobsResponse();

    IJob mockJob1 = mock( IJob.class );
    doReturn( true ).when( schedulerResource.schedulerService ).removeJob( jobIds.get( 0 ) );
    doReturn( mockJob1 ).when( schedulerResource.schedulerService ).getJob( jobIds.get( 0 ) );
    mockJobsResponse.addChanges( jobIds.get( 0 ), REMOVED_JOB_STATE );

    IJob mockJob2 = mock( IJob.class );
    doReturn( false ).when( schedulerResource.schedulerService ).removeJob( jobIds.get( 1 ) );
    doReturn( mockJob2 ).when( schedulerResource.schedulerService ).getJob( jobIds.get( 1 ) );
    doReturn( JobState.NORMAL ).when( mockJob2 ).getState();
    mockJobsResponse.addChanges( jobIds.get( 1 ), JobState.NORMAL.toString() );

    JobsResponse testResponse = schedulerResource.removeJobs( mockJobsRequest );
    assertNotNull( testResponse );
    assertTrue( Maps.difference( convertToMap( testResponse.getChanges() ), convertToMap( mockJobsResponse.getChanges() ) ).areEqual() );
  }

  @Test
  public void testGetJob() throws Exception {
    String jobId = "jobId";
    String asCronString = "asCronString";

    IJob mockJob = mock( IJob.class );
    doReturn( mockJob ).when( schedulerResource.schedulerService ).getJobInfo( jobId );

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildOkResponse( mockJob );

    Response testResponse = schedulerResource.getJob( jobId, asCronString );
    assertEquals( mockResponse, testResponse );

    verify( schedulerResource.schedulerService, times( 2 ) ).getJobInfo( jobId );
  }

  @Test
  public void testGetJobNull() throws Exception {
    String jobId = "jobId";
    String asCronString = "asCronString";

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildStatusResponse( Response.Status.NO_CONTENT );

    Response testResponse = schedulerResource.getJob( jobId, asCronString );
    assertEquals( mockResponse, testResponse );

    verify( schedulerResource.schedulerService, times( 1 ) ).getJobInfo( jobId );
  }

  @Test
  public void testGetJobError() throws Exception {
    String jobId = "jobId";
    String asCronString = "asCronString";

    SchedulerException mockSchedulerException = mock( SchedulerException.class );
    Mockito.doThrow( mockSchedulerException ).when( schedulerResource.schedulerService ).getJobInfo( jobId );

    try {
      schedulerResource.getJob( jobId, asCronString );
      fail();
    } catch ( RuntimeException e ) {
      // correct
    }

    verify( schedulerResource.schedulerService, times( 1 ) ).getJobInfo( jobId );
  }

  @Test
  public void testGetJobInfo() {
    JobScheduleRequest mockJobScheduleRequest = mock( JobScheduleRequest.class );
    doReturn( mockJobScheduleRequest ).when( schedulerResource.schedulerService ).getJobInfo();

    JobScheduleRequest testJobScheduleRequest = schedulerResource.getJobInfo();
    assertEquals( mockJobScheduleRequest, testJobScheduleRequest );

    verify( schedulerResource.schedulerService, times( 1 ) ).getJobInfo();
  }

  @Test
  public void testGetBlockoutJobs() throws Exception {
    List<IJob> mockJobs = mock( List.class );
    doReturn( mockJobs ).when( schedulerResource.schedulerService ).getBlockOutJobs();

    JobWrapper testResult = schedulerResource.getBlockoutJobs();
    assertNotNull( testResult.getJobs() );
    assertEquals( mockJobs, testResult.getJobs() );

    verify( schedulerResource, times( 1 ) ).getBlockoutJobs();
  }

  @Test
  public void testGetBlockoutJobsError() throws Exception {
    Mockito.doThrow( RuntimeException.class ).when( schedulerResource.schedulerService ).getBlockOutJobs();

    Response mockSchedulerExceptionResponse = mock( Response.class );
    doReturn( mockSchedulerExceptionResponse ).when( schedulerResource ).buildServerErrorResponse( any() );

    try {
      schedulerResource.getBlockoutJobs();
      fail();
    } catch ( RuntimeException e ) {
      // correct
    }

    verify( schedulerResource.schedulerService, times( 1 ) ).getBlockOutJobs();
  }

  @Test
  public void testGetBlockoutJobsForbidden() throws Exception {
    Mockito.doThrow( IllegalAccessException.class ).when( schedulerResource.schedulerService ).getBlockOutJobs();

    try {
      schedulerResource.getBlockoutJobs();
      fail();
    } catch ( RuntimeException e ) {
      // correct
    }

    verify( schedulerResource.schedulerService, times( 1 ) ).getBlockOutJobs();
  }

  @Test
  public void testHasBlockouts() {
    Boolean hasBlockouts = Boolean.FALSE;
    doReturn( hasBlockouts ).when( schedulerResource.schedulerService ).hasBlockouts();

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildOkResponse( hasBlockouts.toString() );

    Response testResponse = schedulerResource.hasBlockouts();
    assertEquals( mockResponse, testResponse );

    verify( schedulerResource.schedulerService, times( 1 ) ).hasBlockouts();
    verify( schedulerResource, times( 1 ) ).buildOkResponse( hasBlockouts.toString() );
  }

  @Test
  public void testAddBlockout() throws Exception {
    JobScheduleRequest mockJobScheduleRequest = mock( JobScheduleRequest.class );

    IJob mockJob = mock( IJob.class );
    doReturn( mockJob ).when( schedulerResource.schedulerService ).addBlockout( mockJobScheduleRequest );

    String jobId = "jobId";
    doReturn( jobId ).when( mockJob ).getJobId();

    Response mockJobResponse = mock( Response.class );
    doReturn( mockJobResponse ).when( schedulerResource ).buildPlainTextOkResponse( jobId );

    Response testResponse = schedulerResource.addBlockout( mockJobScheduleRequest );
    assertEquals( mockJobResponse, testResponse );

    verify( schedulerResource.schedulerService, times( 1 ) ).addBlockout( mockJobScheduleRequest );
    verify( mockJob, times( 1 ) ).getJobId();
    verify( schedulerResource, times( 1 ) ).buildPlainTextOkResponse( jobId );
  }

  @Test
  public void testAddBlockoutError() throws Exception {
    JobScheduleRequest mockJobScheduleRequest = mock( JobScheduleRequest.class );

    Response mockUnauthorizedResponse = mock( Response.class );
    doReturn( mockUnauthorizedResponse ).when( schedulerResource ).buildStatusResponse( UNAUTHORIZED );

    // Test 1
    IOException mockIOException = mock( IOException.class );
    Mockito.doThrow( mockIOException ).when( schedulerResource.schedulerService ).addBlockout( mockJobScheduleRequest );

    Response testResponse = schedulerResource.addBlockout( mockJobScheduleRequest );
    assertEquals( mockUnauthorizedResponse, testResponse );

    // Test 2
    SchedulerException mockSchedulerException = mock( SchedulerException.class );
    Mockito.doThrow( mockSchedulerException ).when( schedulerResource.schedulerService )
      .addBlockout( mockJobScheduleRequest );

    testResponse = schedulerResource.addBlockout( mockJobScheduleRequest );
    assertEquals( mockUnauthorizedResponse, testResponse );

    // Test 3
    IllegalAccessException mockIllegalAccessException = mock( IllegalAccessException.class );
    Mockito.doThrow( mockIllegalAccessException ).when( schedulerResource.schedulerService )
      .addBlockout( mockJobScheduleRequest );

    testResponse = schedulerResource.addBlockout( mockJobScheduleRequest );
    assertEquals( mockUnauthorizedResponse, testResponse );

    verify( schedulerResource.schedulerService, times( 3 ) ).addBlockout( mockJobScheduleRequest );
    verify( schedulerResource, times( 3 ) ).buildStatusResponse( UNAUTHORIZED );
  }

  @Test
  public void testUpdateBlockout() throws Exception {
    String jobId = "jobId";
    JobScheduleRequest mockJobScheduleRequest = mock( JobScheduleRequest.class );

    JobRequest mockJobRequest = mock( JobRequest.class );
    doReturn( mockJobRequest ).when( schedulerResource ).getJobRequest();

    IJob mockJob = mock( IJob.class );
    doReturn( mockJob ).when( schedulerResource.schedulerService ).updateBlockout( jobId, mockJobScheduleRequest );

    doReturn( jobId ).when( mockJob ).getJobId();

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildPlainTextOkResponse( jobId );

    Response testResponse = schedulerResource.updateBlockout( jobId, mockJobScheduleRequest );
    assertEquals( mockResponse, testResponse );

    verify( schedulerResource.schedulerService, times( 1 ) ).updateBlockout( jobId, mockJobScheduleRequest );
    verify( mockJob, times( 1 ) ).getJobId();
  }

  @Test
  public void testUpdateBlockoutError() throws Exception {
    String jobId = "jobId";
    JobScheduleRequest mockJobScheduleRequest = mock( JobScheduleRequest.class );

    Response mockUnauthorizedResponse = mock( Response.class );
    doReturn( mockUnauthorizedResponse ).when( schedulerResource ).buildStatusResponse( UNAUTHORIZED );

    // Test 1
    IOException mockIOException = mock( IOException.class );
    Mockito.doThrow( mockIOException ).when( schedulerResource.schedulerService ).updateBlockout( jobId,
      mockJobScheduleRequest );

    Response testResponse = schedulerResource.updateBlockout( jobId, mockJobScheduleRequest );
    assertEquals( mockUnauthorizedResponse, testResponse );

    // Test 2
    SchedulerException mockSchedulerException = mock( SchedulerException.class );
    Mockito.doThrow( mockSchedulerException ).when( schedulerResource.schedulerService ).updateBlockout( jobId,
      mockJobScheduleRequest );

    testResponse = schedulerResource.updateBlockout( jobId, mockJobScheduleRequest );
    assertEquals( mockUnauthorizedResponse, testResponse );

    // Test 3
    IllegalAccessException mockIllegalAccessException = mock( IllegalAccessException.class );
    Mockito.doThrow( mockIllegalAccessException ).when( schedulerResource.schedulerService )
      .updateBlockout( jobId, mockJobScheduleRequest );

    testResponse = schedulerResource.updateBlockout( jobId, mockJobScheduleRequest );
    assertEquals( mockUnauthorizedResponse, testResponse );

    verify( schedulerResource.schedulerService, times( 3 ) ).updateBlockout( jobId, mockJobScheduleRequest );
    verify( schedulerResource, times( 3 ) ).buildStatusResponse( UNAUTHORIZED );
  }

  @Test
  public void testBlockoutWillFire() throws Exception {
    JobScheduleRequest mockJobScheduleRequest = mock( JobScheduleRequest.class );

    IJobTrigger mockJobTrigger = mock( IJobTrigger.class );
    doReturn( mockJobTrigger ).when( schedulerResource ).convertScheduleRequestToJobTrigger( mockJobScheduleRequest );

    Boolean willFire = Boolean.FALSE;
    doReturn( willFire ).when( schedulerResource.schedulerService ).willFire( mockJobTrigger );

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildOkResponse( willFire.toString() );

    Response testResponse = schedulerResource.blockoutWillFire( mockJobScheduleRequest );
    assertEquals( mockResponse, testResponse );

    verify( schedulerResource, times( 1 ) ).convertScheduleRequestToJobTrigger( mockJobScheduleRequest );
    verify( schedulerResource.schedulerService, times( 1 ) ).willFire( mockJobTrigger );
    verify( schedulerResource, times( 1 ) ).buildOkResponse( willFire.toString() );
  }

  @Test
  public void testBlockoutWillFireError() throws Exception {
    JobScheduleRequest mockJobScheduleRequest = mock( JobScheduleRequest.class );

    UnifiedRepositoryException mockUnifiedRepositoryException = mock( UnifiedRepositoryException.class );

    SchedulerException mockSchedulerException = mock( SchedulerException.class );

    Response mockUnifiedRepositoryExceptionResponse = mock( Response.class );
    doReturn( mockUnifiedRepositoryExceptionResponse ).when( schedulerResource )
      .buildServerErrorResponse( mockUnifiedRepositoryException );

    Response mockSchedulerExceptionResponse = mock( Response.class );
    doReturn( mockSchedulerExceptionResponse ).when( schedulerResource )
      .buildServerErrorResponse( mockSchedulerException );

    // Test 1
    Mockito.doThrow( mockUnifiedRepositoryException ).when( schedulerResource )
      .convertScheduleRequestToJobTrigger( mockJobScheduleRequest );

    Response testResponse = schedulerResource.blockoutWillFire( mockJobScheduleRequest );
    assertEquals( mockUnifiedRepositoryExceptionResponse, testResponse );

    // Test 2
    Mockito.doThrow( mockSchedulerException ).when( schedulerResource )
      .convertScheduleRequestToJobTrigger( mockJobScheduleRequest );

    testResponse = schedulerResource.blockoutWillFire( mockJobScheduleRequest );
    assertEquals( mockSchedulerExceptionResponse, testResponse );

    verify( schedulerResource, times( 1 ) ).buildServerErrorResponse( mockUnifiedRepositoryException );
    verify( schedulerResource, times( 1 ) ).buildServerErrorResponse( mockSchedulerException );
    verify( schedulerResource, times( 2 ) ).convertScheduleRequestToJobTrigger( mockJobScheduleRequest );
  }

  @Test
  public void testShouldFireNow() {
    Boolean shouldFireNow = Boolean.FALSE;
    doReturn( shouldFireNow ).when( schedulerResource.schedulerService ).shouldFireNow();

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildOkResponse( shouldFireNow.toString() );

    Response testResponse = schedulerResource.shouldFireNow();
    assertEquals( mockResponse, testResponse );

    verify( schedulerResource.schedulerService, times( 1 ) ).shouldFireNow();
    verify( schedulerResource, times( 1 ) ).buildOkResponse( shouldFireNow.toString() );
  }

  @Test
  public void testGetBlockStatus() throws Exception {
    JobScheduleRequest mockJobScheduleRequest = mock( JobScheduleRequest.class );

    BlockStatusProxy mockBlockStatusProxy = mock( BlockStatusProxy.class );
    doReturn( mockBlockStatusProxy ).when( schedulerResource.schedulerService )
      .getBlockStatus( mockJobScheduleRequest );

    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( schedulerResource ).buildOkResponse( mockBlockStatusProxy );

    Response testResponse = schedulerResource.getBlockStatus( mockJobScheduleRequest );
    assertEquals( mockResponse, testResponse );

    verify( schedulerResource.schedulerService, times( 1 ) ).getBlockStatus( mockJobScheduleRequest );
    verify( schedulerResource, times( 1 ) ).buildOkResponse( mockBlockStatusProxy );
  }


  @Test
  public void updateJob_ReturnsJobId() throws Exception {
    JobScheduleRequest request = new JobScheduleRequest();
    Job job = new Job();
    job.setJobId( "job-id" );
    when( schedulerResource.schedulerService.updateJob( request ) ).thenReturn( job );

    assertUpdateJob( request, OK, job.getJobId() );
  }

  @Test
  public void updateJob_Returns500_WhenSchedulerFails() throws Exception {
    JobScheduleRequest request = new JobScheduleRequest();
    when( schedulerResource.schedulerService.updateJob( request ) )
      .thenThrow( new SchedulerException( new RuntimeException( "error" ) ) );

    assertUpdateJob( request, INTERNAL_SERVER_ERROR, "error" );
  }

  @Test
  public void updateJob_Returns500_WhenIoFails() throws Exception {
    JobScheduleRequest request = new JobScheduleRequest();
    when( schedulerResource.schedulerService.updateJob( request ) )
      .thenThrow( new IOException( new RuntimeException( "error" ) ) );

    assertUpdateJob( request, INTERNAL_SERVER_ERROR, "error" );
  }

  @Test
  public void updateJob_Returns401_WhenNotAuthorized() throws Exception {
    JobScheduleRequest request = new JobScheduleRequest();
    when( schedulerResource.schedulerService.updateJob( request ) )
      .thenThrow( new SecurityException( "error" ) );

    assertUpdateJob( request, UNAUTHORIZED, null );
  }

  @Test
  public void updateJob_Returns403_WhenNotPermitted() throws Exception {
    JobScheduleRequest request = new JobScheduleRequest();
    when( schedulerResource.schedulerService.updateJob( request ) )
      .thenThrow( new IllegalAccessException( "error" ) );

    assertUpdateJob( request, FORBIDDEN, null );
  }

  private void assertUpdateJob( JobScheduleRequest request, Response.Status expectedStatus, String expectedResponse ) {
    Response response = schedulerResource.updateJob( request );
    assertEquals( expectedStatus.getStatusCode(), response.getStatus() );
    assertEquals( expectedResponse, response.getEntity() );
  }

  private Map<String, String> convertToMap( JobsResponse.JobsResponseEntries entries ) {
    return entries.getEntry().stream()
      .collect( Collectors.toMap( JobsResponseEntry::getKey, JobsResponseEntry::getValue ) );
  }
}
