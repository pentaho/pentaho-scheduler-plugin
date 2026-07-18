/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/
package org.pentaho.platform.plugin.services.importer;

import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.pentaho.platform.api.importexport.IImportHelper;
import org.pentaho.platform.api.scheduler2.ICronJobTrigger;
import org.pentaho.platform.api.scheduler2.IJob;
import org.pentaho.platform.api.scheduler2.IJobRequest;
import org.pentaho.platform.api.scheduler2.IJobScheduleParam;
import org.pentaho.platform.api.scheduler2.IJobScheduleRequest;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.ISchedulerResource;
import org.pentaho.platform.api.scheduler2.ISimpleJobTrigger;
import org.pentaho.platform.api.scheduler2.JobState;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.services.importexport.ImportSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScheduleImportUtilTest {

  private IScheduler mockScheduler;
  private ISchedulerResource mockSchedulerResource;
  private IJobRequest mockJobRequest;
  private Log mockLogger;

  @Before
  public void setUp() {
    mockScheduler = mock( IScheduler.class );
    mockSchedulerResource = mock( ISchedulerResource.class );
    mockJobRequest = mock( IJobRequest.class );
    mockLogger = mock( Log.class );

    when( mockScheduler.createSchedulerResource() ).thenReturn( mockSchedulerResource );
    when( mockScheduler.createJobRequest() ).thenReturn( mockJobRequest );
    when( mockSchedulerResource.getJobsList() ).thenReturn( new ArrayList<>() );

    // Clear the import session before each test
    ImportSession.getSession().getImportedScheduleJobIds().clear();
  }

  private IImportHelper.ImportContext createImportContext( boolean performingRestore, boolean overwriteFile ) {
    return new IImportHelper.ImportContext() {
      @Override
      public Log getLogger() {
        return mockLogger;
      }

      @Override
      public boolean isPerformingRestore() {
        return performingRestore;
      }

      @Override
      public boolean isOverwriteFile() {
        return overwriteFile;
      }
    };
  }

  private void stubPentahoSystem( MockedStatic<PentahoSystem> pentahoSystemMock ) {
    pentahoSystemMock.when(
        () -> PentahoSystem.get( ArgumentMatchers.eq( IScheduler.class ), ArgumentMatchers.eq( "IScheduler2" ),
          ArgumentMatchers.isNull() ) )
      .thenReturn( mockScheduler );
  }

  // ========== doImport tests ==========

  @Test
  public void testDoImport_emptyScheduleList() throws Exception {
    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( new ArrayList<>() );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      IImportHelper.ImportContext ctx = createImportContext( false, false );
      scheduleImportUtil.doImport( ctx );

      // Should not interact with scheduler at all
      verify( mockSchedulerResource, never() ).pause();
      verify( mockSchedulerResource, never() ).start();
    }
  }

  @Test
  public void testDoImport_nullScheduleList() throws Exception {
    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( null );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      IImportHelper.ImportContext ctx = createImportContext( false, false );
      scheduleImportUtil.doImport( ctx );

      verify( mockSchedulerResource, never() ).pause();
      verify( mockSchedulerResource, never() ).start();
    }
  }

  @Test
  public void testDoImport_successfulScheduleCreation() throws Exception {
    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/home/admin/test.prpt" );
    scheduleRequest.setOutputFile( "/home/admin/test.*" );
    scheduleRequest.setJobName( "TestJob" );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    Response mockResponse = mock( Response.class );
    when( mockResponse.getStatus() ).thenReturn( Response.Status.OK.getStatusCode() );
    when( mockResponse.getEntity() ).thenReturn( "job-id-123" );
    when( mockSchedulerResource.createJob( scheduleRequest ) ).thenReturn( mockResponse );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      IImportHelper.ImportContext ctx = createImportContext( false, false );
      scheduleImportUtil.doImport( ctx );

      verify( mockSchedulerResource ).pause();
      verify( mockSchedulerResource ).start();
      verify( mockSchedulerResource ).createJob( scheduleRequest );
      Assert.assertEquals( 1, ImportSession.getSession().getImportedScheduleJobIds().size() );
      Assert.assertTrue( ImportSession.getSession().getImportedScheduleJobIds().contains( "job-id-123" ) );
    }
  }

  @Test
  public void testDoImport_responseNotOk_logsError() throws Exception {
    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/home/admin/test.prpt" );
    scheduleRequest.setOutputFile( "/home/admin/test.*" );
    scheduleRequest.setJobName( "TestJob" );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    Response mockResponse = mock( Response.class );
    when( mockResponse.getStatus() ).thenReturn( Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
    when( mockResponse.getEntity() ).thenReturn( "some error" );
    when( mockSchedulerResource.createJob( scheduleRequest ) ).thenReturn( mockResponse );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      IImportHelper.ImportContext ctx = createImportContext( false, false );
      scheduleImportUtil.doImport( ctx );

      verify( mockLogger ).error( ArgumentMatchers.argThat(
        ( String msg ) -> msg.contains( "TestJob" ) ) );
      Assert.assertEquals( 0, ImportSession.getSession().getImportedScheduleJobIds().size() );
    }
  }

  @Test
  public void testDoImport_responseOkButEntityNull_doesNotAddToSession() throws Exception {
    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/home/admin/test.prpt" );
    scheduleRequest.setOutputFile( "/home/admin/test.*" );
    scheduleRequest.setJobName( "TestJob" );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    Response mockResponse = mock( Response.class );
    when( mockResponse.getStatus() ).thenReturn( Response.Status.OK.getStatusCode() );
    when( mockResponse.getEntity() ).thenReturn( null );
    when( mockSchedulerResource.createJob( scheduleRequest ) ).thenReturn( mockResponse );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      IImportHelper.ImportContext ctx = createImportContext( false, false );
      scheduleImportUtil.doImport( ctx );

      Assert.assertEquals( 0, ImportSession.getSession().getImportedScheduleJobIds().size() );
    }
  }

  @Test
  public void testDoImport_overwriteExistingJob_removesAndReCreates() throws Exception {
    String lineageId = "lineage-123";

    // Set up a schedule request with a lineage-id parameter
    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/home/admin/test.prpt" );
    scheduleRequest.setOutputFile( "/home/admin/test.*" );
    scheduleRequest.setJobName( "TestJob" );
    IJobScheduleParam param = mock( IJobScheduleParam.class );
    when( param.getName() ).thenReturn( "lineage-id" );
    when( param.getValue() ).thenReturn( lineageId );
    scheduleRequest.setJobParameters( Collections.singletonList( param ) );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    // Set up an existing job with the same lineage-id
    IJob existingJob = mock( IJob.class );
    Map<String, Object> existingJobParams = new HashMap<>();
    existingJobParams.put( "lineage-id", lineageId );
    when( existingJob.getJobParams() ).thenReturn( existingJobParams );
    when( existingJob.getJobId() ).thenReturn( "existing-job-id" );

    List<IJob> existingJobs = new ArrayList<>();
    existingJobs.add( existingJob );
    when( mockSchedulerResource.getJobsList() ).thenReturn( existingJobs );

    Response mockResponse = mock( Response.class );
    when( mockResponse.getStatus() ).thenReturn( Response.Status.OK.getStatusCode() );
    when( mockResponse.getEntity() ).thenReturn( "new-job-id" );
    when( mockSchedulerResource.createJob( scheduleRequest ) ).thenReturn( mockResponse );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      // overwriteFile = true
      IImportHelper.ImportContext ctx = createImportContext( false, true );
      scheduleImportUtil.doImport( ctx );

      // Should remove the existing job
      verify( mockJobRequest ).setJobId( "existing-job-id" );
      verify( mockSchedulerResource ).removeJob( mockJobRequest );
      // Should create the new job
      verify( mockSchedulerResource ).createJob( scheduleRequest );
      Assert.assertEquals( 1, ImportSession.getSession().getImportedScheduleJobIds().size() );
    }
  }

  @Test
  public void testDoImport_existingJobNoOverwrite_skipsJob() throws Exception {
    String lineageId = "lineage-123";

    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/home/admin/test.prpt" );
    scheduleRequest.setOutputFile( "/home/admin/test.*" );
    scheduleRequest.setJobName( "TestJob" );
    IJobScheduleParam param = mock( IJobScheduleParam.class );
    when( param.getName() ).thenReturn( "lineage-id" );
    when( param.getValue() ).thenReturn( lineageId );
    scheduleRequest.setJobParameters( Collections.singletonList( param ) );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    // Existing job with matching lineage-id
    IJob existingJob = mock( IJob.class );
    Map<String, Object> existingJobParams = new HashMap<>();
    existingJobParams.put( "lineage-id", lineageId );
    when( existingJob.getJobParams() ).thenReturn( existingJobParams );

    List<IJob> existingJobs = new ArrayList<>();
    existingJobs.add( existingJob );
    when( mockSchedulerResource.getJobsList() ).thenReturn( existingJobs );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      // overwriteFile = false
      IImportHelper.ImportContext ctx = createImportContext( false, false );
      scheduleImportUtil.doImport( ctx );

      // Should NOT create a new job
      verify( mockSchedulerResource, never() ).createJob( ArgumentMatchers.any( IJobScheduleRequest.class ) );
      verify( mockSchedulerResource, never() ).removeJob( ArgumentMatchers.any( IJobRequest.class ) );
      Assert.assertEquals( 0, ImportSession.getSession().getImportedScheduleJobIds().size() );
    }
  }

  @Test
  public void testDoImport_failsToCreateSchedule_noSpace_logsError() throws Exception {
    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/home/admin/scheduledTransform.ktr" );
    scheduleRequest.setOutputFile( "/home/admin/scheduledTransform*" );
    scheduleRequest.setJobName( "TestJob" );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    when( mockSchedulerResource.createJob( ArgumentMatchers.any( IJobScheduleRequest.class ) ) )
      .thenThrow( new RuntimeException( "error creating schedule" ) );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      IImportHelper.ImportContext ctx = createImportContext( false, false );
      scheduleImportUtil.doImport( ctx );

      verify( mockLogger ).error( ArgumentMatchers.argThat(
        ( String msg ) -> msg.contains( "TestJob" ) ) );
      Assert.assertEquals( 0, ImportSession.getSession().getImportedScheduleJobIds().size() );
    }
  }

  @Test
  public void testDoImport_failsWithSpace_retriesWithUnderscores() throws Exception {
    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/home/admin/scheduled Transform.ktr" );
    scheduleRequest.setOutputFile( "/home/admin/scheduled Transform*" );
    scheduleRequest.setJobName( "TestJob" );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    // Capture the inputFile and outputFile at each invocation, since the object is mutated in-place
    List<String> capturedInputFiles = new ArrayList<>();
    List<String> capturedOutputFiles = new ArrayList<>();

    Response mockResponse = mock( Response.class );
    when( mockResponse.getStatus() ).thenReturn( Response.Status.OK.getStatusCode() );
    when( mockResponse.getEntity() ).thenReturn( "job-id-retry" );

    // First call (with spaces) throws, second call (with underscores) succeeds.
    // Use an Answer to snapshot the file paths at each invocation time.
    final int[] callCount = { 0 };
    when( mockSchedulerResource.createJob( ArgumentMatchers.any( IJobScheduleRequest.class ) ) )
      .thenAnswer( invocation -> {
        IJobScheduleRequest req = invocation.getArgument( 0 );
        capturedInputFiles.add( req.getInputFile() );
        capturedOutputFiles.add( req.getOutputFile() );
        if ( callCount[0]++ == 0 ) {
          throw new RuntimeException( "error creating schedule" );
        }
        return mockResponse;
      } );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      IImportHelper.ImportContext ctx = createImportContext( false, false );
      scheduleImportUtil.doImport( ctx );

      // Verify two calls were made
      Assert.assertEquals( 2, capturedInputFiles.size() );

      // First call should have the original paths with spaces
      Assert.assertEquals( "/home/admin/scheduled Transform.ktr", capturedInputFiles.get( 0 ) );
      Assert.assertEquals( "/home/admin/scheduled Transform*", capturedOutputFiles.get( 0 ) );

      // Second call should have spaces replaced with underscores
      Assert.assertEquals( "/home/admin/scheduled_Transform.ktr", capturedInputFiles.get( 1 ) );
      Assert.assertEquals( "/home/admin/scheduled_Transform*", capturedOutputFiles.get( 1 ) );

      // Job should have been imported successfully on retry
      Assert.assertEquals( 1, ImportSession.getSession().getImportedScheduleJobIds().size() );
      Assert.assertTrue( ImportSession.getSession().getImportedScheduleJobIds().contains( "job-id-retry" ) );
    }
  }

  @Test
  public void testDoImport_performingRestore_logsMessages() throws Exception {
    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/home/admin/test.prpt" );
    scheduleRequest.setOutputFile( "/home/admin/test.*" );
    scheduleRequest.setJobName( "TestJob" );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    Response mockResponse = mock( Response.class );
    when( mockResponse.getStatus() ).thenReturn( Response.Status.OK.getStatusCode() );
    when( mockResponse.getEntity() ).thenReturn( "job-id-456" );
    when( mockSchedulerResource.createJob( scheduleRequest ) ).thenReturn( mockResponse );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      // performingRestore = true
      IImportHelper.ImportContext ctx = createImportContext( true, false );
      scheduleImportUtil.doImport( ctx );

      // Verify restore-related logging happened
      verify( mockLogger, Mockito.atLeastOnce() ).info( ArgumentMatchers.argThat(
        ( String msg ) -> msg != null && !msg.isEmpty() ) );
      verify( mockLogger, Mockito.atLeastOnce() ).debug( ArgumentMatchers.argThat(
        ( String msg ) -> msg.contains( "TestJob" ) || msg.contains( "scheduler" ) ) );
    }
  }

  // ========== convertFromPreTimeZoneTrigger integration tests ==========

  /**
   * Reproduces the FillAuditMartTime scenario from exportManifest.xml:
   * A pre-timezone trigger with uiPassParam=MINUTES but repeatInterval=10 (seconds).
   * The interval of 10 divided by 60 gives 0, which is invalid.
   * convertFromPreTimeZoneTrigger should detect this mismatch and force uiPassParam to SECONDS
   * so the job imports successfully.
   */
  @Test
  public void testDoImport_fillAuditMartTime_preTimeZoneTriggerCorrected() throws Exception {
    // Simulate the FillAuditMartTime schedule from exportManifest.xml:
    // uiPassParam=MINUTES, repeatInterval=10, no start date fields (all default to -1)
    SimpleJobTrigger simpleJobTrigger = new SimpleJobTrigger();
    simpleJobTrigger.setRepeatInterval( 10 );
    simpleJobTrigger.setRepeatCount( 0 );
    simpleJobTrigger.setUiPassParam( "MINUTES" );
    // startDay, startMonth, startYear, startHour, startMin all default to -1 (pre-timezone)

    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/public/pentaho-operations-mart/update_audit_mart_data/FillAuditMartTime.xaction" );
    scheduleRequest.setOutputFile( "/home/admin/FillAuditMartTime.*" );
    scheduleRequest.setJobName( "FillAuditMartTime" );
    scheduleRequest.setJobState( JobState.PAUSED );
    scheduleRequest.setSimpleJobTrigger( simpleJobTrigger );

    IJobScheduleParam uiPassParamEntry = mock( IJobScheduleParam.class );
    when( uiPassParamEntry.getName() ).thenReturn( "uiPassParam" );
    when( uiPassParamEntry.getValue() ).thenReturn( "MINUTES" );

    IJobScheduleParam lineageParam = mock( IJobScheduleParam.class );
    when( lineageParam.getName() ).thenReturn( "lineage-id" );
    when( lineageParam.getValue() ).thenReturn( "e770c5a5-ec94-44fc-967a-87e6ad85ce85" );

    List<IJobScheduleParam> params = new ArrayList<>();
    params.add( uiPassParamEntry );
    params.add( lineageParam );
    scheduleRequest.setJobParameters( params );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    Response mockResponse = mock( Response.class );
    when( mockResponse.getStatus() ).thenReturn( Response.Status.OK.getStatusCode() );
    when( mockResponse.getEntity() ).thenReturn( "fill-audit-mart-job-id" );
    when( mockSchedulerResource.createJob( ArgumentMatchers.any( IJobScheduleRequest.class ) ) )
      .thenReturn( mockResponse );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      IImportHelper.ImportContext ctx = createImportContext( true, true );
      scheduleImportUtil.doImport( ctx );

      // The job should have been imported successfully
      Assert.assertEquals( 1, ImportSession.getSession().getImportedScheduleJobIds().size() );
      Assert.assertTrue(
        ImportSession.getSession().getImportedScheduleJobIds().contains( "fill-audit-mart-job-id" ) );

      // The trigger's uiPassParam should have been corrected from MINUTES to SECONDS
      // because 10 / 60 = 0, which is invalid
      ISimpleJobTrigger correctedTrigger = scheduleRequest.getSimpleJobTrigger();
      Assert.assertEquals( "SECONDS", ( (SimpleJobTrigger) correctedTrigger ).getUiPassParam() );

      // A warning should have been logged about the mismatch
      verify( mockLogger, Mockito.atLeastOnce() ).warn( ArgumentMatchers.argThat(
        ( String msg ) -> msg.contains( "FillAuditMartTime" )
          && msg.contains( "repeat interval of [ 10 ]" )
          && msg.contains( "UiPassParam of [ MINUTES ]" )
          && msg.contains( "converted to be a SECONDS trigger" ) ) );
    }
  }

  /**
   * Verifies that a pre-timezone trigger with a valid interval/uiPassParam combination
   * (e.g. repeatInterval=1800 with MINUTES -> 1800/60 = 30, which is > 0)
   * is NOT forced to SECONDS — it passes through unchanged.
   */
  @Test
  public void testDoImport_preTimeZoneTrigger_validInterval_notCorrected() throws Exception {
    // Simulate the UpdateAuditData schedule: uiPassParam=MINUTES, repeatInterval=1800
    // 1800 / 60 = 30 which is valid, so no correction should be applied
    SimpleJobTrigger simpleJobTrigger = new SimpleJobTrigger();
    simpleJobTrigger.setRepeatInterval( 1800 );
    simpleJobTrigger.setRepeatCount( -1 );
    simpleJobTrigger.setUiPassParam( "MINUTES" );
    // All start fields default to -1 (pre-timezone)

    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/public/pentaho-operations-mart/update_audit_mart_data/UpdateAuditData.xaction" );
    scheduleRequest.setOutputFile( "/home/admin/UpdateAuditData.*" );
    scheduleRequest.setJobName( "UpdateAuditData" );
    scheduleRequest.setJobState( JobState.PAUSED );
    scheduleRequest.setSimpleJobTrigger( simpleJobTrigger );

    IJobScheduleParam lineageParam = mock( IJobScheduleParam.class );
    when( lineageParam.getName() ).thenReturn( "lineage-id" );
    when( lineageParam.getValue() ).thenReturn( "87bbfe7f-d126-48a5-8f8d-3e401667fbac" );
    scheduleRequest.setJobParameters( Collections.singletonList( lineageParam ) );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    Response mockResponse = mock( Response.class );
    when( mockResponse.getStatus() ).thenReturn( Response.Status.OK.getStatusCode() );
    when( mockResponse.getEntity() ).thenReturn( "update-audit-job-id" );
    when( mockSchedulerResource.createJob( ArgumentMatchers.any( IJobScheduleRequest.class ) ) )
      .thenReturn( mockResponse );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      IImportHelper.ImportContext ctx = createImportContext( true, true );
      scheduleImportUtil.doImport( ctx );

      // The job should have been imported successfully
      Assert.assertEquals( 1, ImportSession.getSession().getImportedScheduleJobIds().size() );

      // The trigger's uiPassParam should remain MINUTES (1800/60=30 is valid)
      ISimpleJobTrigger correctedTrigger = scheduleRequest.getSimpleJobTrigger();
      Assert.assertEquals( "MINUTES", ( (SimpleJobTrigger) correctedTrigger ).getUiPassParam() );

      // No warning should have been logged
      verify( mockLogger, never() ).warn( ArgumentMatchers.anyString() );
    }
  }

  /**
   * Verifies that a trigger with valid start date fields (post-timezone)
   * is NOT processed by convertFromPreTimeZoneTrigger at all.
   */
  @Test
  public void testDoImport_postTimeZoneTrigger_notModified() throws Exception {
    SimpleJobTrigger simpleJobTrigger = new SimpleJobTrigger();
    simpleJobTrigger.setRepeatInterval( 10 );
    simpleJobTrigger.setRepeatCount( 0 );
    simpleJobTrigger.setUiPassParam( "MINUTES" );
    // Set start fields to non-default values (post-timezone support)
    simpleJobTrigger.setStartDay( 23 );
    simpleJobTrigger.setStartMonth( 5 );
    simpleJobTrigger.setStartYear( 2023 );
    simpleJobTrigger.setStartHour( 10 );
    simpleJobTrigger.setStartMin( 59 );

    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/public/pentaho-operations-mart/update_audit_mart_data/FillAuditMartTime.xaction" );
    scheduleRequest.setOutputFile( "/public/pentaho-operations-mart/generated_logs/FillAuditMartTime.*" );
    scheduleRequest.setJobName( "FillAuditMartTime" );
    scheduleRequest.setJobState( JobState.PAUSED );
    scheduleRequest.setSimpleJobTrigger( simpleJobTrigger );

    IJobScheduleParam lineageParam = mock( IJobScheduleParam.class );
    when( lineageParam.getName() ).thenReturn( "lineage-id" );
    when( lineageParam.getValue() ).thenReturn( "297962bd-4bea-41e8-9e0b-e01c624a274b" );
    scheduleRequest.setJobParameters( Collections.singletonList( lineageParam ) );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    Response mockResponse = mock( Response.class );
    when( mockResponse.getStatus() ).thenReturn( Response.Status.OK.getStatusCode() );
    when( mockResponse.getEntity() ).thenReturn( "post-tz-job-id" );
    when( mockSchedulerResource.createJob( ArgumentMatchers.any( IJobScheduleRequest.class ) ) )
      .thenReturn( mockResponse );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      IImportHelper.ImportContext ctx = createImportContext( false, false );
      scheduleImportUtil.doImport( ctx );

      // Job imported successfully
      Assert.assertEquals( 1, ImportSession.getSession().getImportedScheduleJobIds().size() );

      // uiPassParam should remain MINUTES — not corrected because this is a post-timezone trigger
      ISimpleJobTrigger resultTrigger = scheduleRequest.getSimpleJobTrigger();
      Assert.assertEquals( "MINUTES", ( (SimpleJobTrigger) resultTrigger ).getUiPassParam() );

      // No warning logged
      verify( mockLogger, never() ).warn( ArgumentMatchers.anyString() );
    }
  }

  // ========== createSchedulerJob tests ==========

  /**
   * Verifies that when the job trigger is null (not a SimpleJobTrigger),
   * convertFromPreTimeZoneTrigger returns true and the job is imported normally.
   */
  @Test
  public void testDoImport_nullSimpleJobTrigger_importsSuccessfully() throws Exception {
    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/home/admin/test.prpt" );
    scheduleRequest.setOutputFile( "/home/admin/test.*" );
    scheduleRequest.setJobName( "NullTriggerJob" );
    // simpleJobTrigger is null by default in FakeJobScheduleRequest

    IJobScheduleParam lineageParam = mock( IJobScheduleParam.class );
    when( lineageParam.getName() ).thenReturn( "lineage-id" );
    when( lineageParam.getValue() ).thenReturn( "null-trigger-lineage" );
    scheduleRequest.setJobParameters( Collections.singletonList( lineageParam ) );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    Response mockResponse = mock( Response.class );
    when( mockResponse.getStatus() ).thenReturn( Response.Status.OK.getStatusCode() );
    when( mockResponse.getEntity() ).thenReturn( "null-trigger-job-id" );
    when( mockSchedulerResource.createJob( ArgumentMatchers.any( IJobScheduleRequest.class ) ) )
      .thenReturn( mockResponse );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      IImportHelper.ImportContext ctx = createImportContext( false, false );
      scheduleImportUtil.doImport( ctx );

      // Job should still be imported — convertFromPreTimeZoneTrigger returns true for non-SimpleJobTrigger
      Assert.assertEquals( 1, ImportSession.getSession().getImportedScheduleJobIds().size() );
      Assert.assertTrue(
        ImportSession.getSession().getImportedScheduleJobIds().contains( "null-trigger-job-id" ) );
    }
  }

  /**
   * Verifies that a pre-timezone trigger with uiPassParam=RUN_ONCE and repeatInterval=0
   * is allowed through without any modification or error — RUN_ONCE triggers are valid
   * regardless of their interval value.
   */
  @Test
  public void testDoImport_preTimeZoneTrigger_runOnce_zeroInterval_importsSuccessfully() throws Exception {
    SimpleJobTrigger simpleJobTrigger = new SimpleJobTrigger();
    simpleJobTrigger.setRepeatInterval( 0 );
    simpleJobTrigger.setRepeatCount( 0 );
    simpleJobTrigger.setUiPassParam( "RUN_ONCE" );
    // All start fields default to -1 (pre-timezone)

    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/home/admin/runonce.prpt" );
    scheduleRequest.setOutputFile( "/home/admin/runonce.*" );
    scheduleRequest.setJobName( "RunOnceJob" );
    scheduleRequest.setSimpleJobTrigger( simpleJobTrigger );

    IJobScheduleParam lineageParam = mock( IJobScheduleParam.class );
    when( lineageParam.getName() ).thenReturn( "lineage-id" );
    when( lineageParam.getValue() ).thenReturn( "run-once-lineage" );
    scheduleRequest.setJobParameters( Collections.singletonList( lineageParam ) );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    Response mockResponse = mock( Response.class );
    when( mockResponse.getStatus() ).thenReturn( Response.Status.OK.getStatusCode() );
    when( mockResponse.getEntity() ).thenReturn( "run-once-job-id" );
    when( mockSchedulerResource.createJob( ArgumentMatchers.any( IJobScheduleRequest.class ) ) )
      .thenReturn( mockResponse );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      IImportHelper.ImportContext ctx = createImportContext( false, false );
      scheduleImportUtil.doImport( ctx );

      // Job should be imported successfully — RUN_ONCE with interval=0 is valid
      Assert.assertEquals( 1, ImportSession.getSession().getImportedScheduleJobIds().size() );
      Assert.assertTrue(
        ImportSession.getSession().getImportedScheduleJobIds().contains( "run-once-job-id" ) );

      // uiPassParam should remain RUN_ONCE (unchanged)
      ISimpleJobTrigger resultTrigger = scheduleRequest.getSimpleJobTrigger();
      Assert.assertEquals( "RUN_ONCE", ( (SimpleJobTrigger) resultTrigger ).getUiPassParam() );

      // No error or warning should have been logged
      verify( mockLogger, never() ).error( ArgumentMatchers.anyString() );
      verify( mockLogger, never() ).warn( ArgumentMatchers.anyString() );
    }
  }

  /**
   * Verifies that a pre-timezone trigger with repeatInterval <= 0 causes the job to be skipped
   * (convertFromPreTimeZoneTrigger returns false) and an error is logged.
   */
  @Test
  public void testDoImport_preTimeZoneTrigger_zeroInterval_skipsJob() throws Exception {
    SimpleJobTrigger simpleJobTrigger = new SimpleJobTrigger();
    simpleJobTrigger.setRepeatInterval( 0 );
    simpleJobTrigger.setRepeatCount( -1 );
    simpleJobTrigger.setUiPassParam( "MINUTES" );
    // All start fields default to -1 (pre-timezone)

    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/home/admin/badinterval.prpt" );
    scheduleRequest.setOutputFile( "/home/admin/badinterval.*" );
    scheduleRequest.setJobName( "ZeroIntervalJob" );
    scheduleRequest.setSimpleJobTrigger( simpleJobTrigger );

    IJobScheduleParam lineageParam = mock( IJobScheduleParam.class );
    when( lineageParam.getName() ).thenReturn( "lineage-id" );
    when( lineageParam.getValue() ).thenReturn( "zero-interval-lineage" );
    scheduleRequest.setJobParameters( Collections.singletonList( lineageParam ) );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      IImportHelper.ImportContext ctx = createImportContext( false, false );
      scheduleImportUtil.doImport( ctx );

      // Job should NOT be imported — interval <= 0 means the schedule is invalid
      Assert.assertEquals( 0, ImportSession.getSession().getImportedScheduleJobIds().size() );
      verify( mockSchedulerResource, never() ).createJob( ArgumentMatchers.any( IJobScheduleRequest.class ) );
      // Error should have been logged mentioning the job name and the interval problem
      verify( mockLogger ).error( ArgumentMatchers.argThat(
        ( String msg ) -> msg.contains( "ZeroIntervalJob" ) ) );
    }
  }

  /**
   * Verifies that a pre-timezone trigger with null uiPassParam defaults to SECONDS
   * and the job imports successfully.
   */
  @Test
  public void testDoImport_preTimeZoneTrigger_nullUiPassParam_defaultsToSeconds() throws Exception {
    SimpleJobTrigger simpleJobTrigger = new SimpleJobTrigger();
    simpleJobTrigger.setRepeatInterval( 300 );
    simpleJobTrigger.setRepeatCount( -1 );
    simpleJobTrigger.setUiPassParam( null ); // null — should default to SECONDS
    // All start fields default to -1 (pre-timezone)

    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/home/admin/nullparam.prpt" );
    scheduleRequest.setOutputFile( "/home/admin/nullparam.*" );
    scheduleRequest.setJobName( "NullUiPassParamJob" );
    scheduleRequest.setSimpleJobTrigger( simpleJobTrigger );

    IJobScheduleParam lineageParam = mock( IJobScheduleParam.class );
    when( lineageParam.getName() ).thenReturn( "lineage-id" );
    when( lineageParam.getValue() ).thenReturn( "null-uipass-lineage" );
    scheduleRequest.setJobParameters( Collections.singletonList( lineageParam ) );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    Response mockResponse = mock( Response.class );
    when( mockResponse.getStatus() ).thenReturn( Response.Status.OK.getStatusCode() );
    when( mockResponse.getEntity() ).thenReturn( "null-uipass-job-id" );
    when( mockSchedulerResource.createJob( ArgumentMatchers.any( IJobScheduleRequest.class ) ) )
      .thenReturn( mockResponse );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      IImportHelper.ImportContext ctx = createImportContext( false, false );
      scheduleImportUtil.doImport( ctx );

      // Job should be imported successfully
      Assert.assertEquals( 1, ImportSession.getSession().getImportedScheduleJobIds().size() );

      // uiPassParam should have been set to SECONDS
      ISimpleJobTrigger resultTrigger = scheduleRequest.getSimpleJobTrigger();
      Assert.assertEquals( "SECONDS", ( (SimpleJobTrigger) resultTrigger ).getUiPassParam() );

      // Warning should have been logged about null uiPassParam, mentioning the job name
      verify( mockLogger, Mockito.atLeastOnce() ).warn( ArgumentMatchers.argThat(
        ( String msg ) -> msg.contains( "NullUiPassParamJob" )
          && msg.contains( "No UiPassParam set for job" )
          && msg.contains( "Defaulting it to SECONDS" ) ) );
    }
  }

  /**
   * Verifies that a pre-timezone trigger with an unrecognized/invalid uiPassParam value
   * causes calculateTriggerInterval to throw IllegalArgumentException, which results in
   * the job being skipped (not imported) and an error being logged.
   */
  @Test
  public void testDoImport_preTimeZoneTrigger_invalidUiPassParam_skipsJob() throws Exception {
    SimpleJobTrigger simpleJobTrigger = new SimpleJobTrigger();
    simpleJobTrigger.setRepeatInterval( 600 );
    simpleJobTrigger.setRepeatCount( -1 );
    simpleJobTrigger.setUiPassParam( "BOGUS_VALUE" ); // unrecognized value
    // All start fields default to -1 (pre-timezone)

    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setInputFile( "/home/admin/invalidparam.prpt" );
    scheduleRequest.setOutputFile( "/home/admin/invalidparam.*" );
    scheduleRequest.setJobName( "InvalidUiPassParamJob" );
    scheduleRequest.setSimpleJobTrigger( simpleJobTrigger );

    IJobScheduleParam lineageParam = mock( IJobScheduleParam.class );
    when( lineageParam.getName() ).thenReturn( "lineage-id" );
    when( lineageParam.getValue() ).thenReturn( "invalid-uipass-lineage" );
    scheduleRequest.setJobParameters( Collections.singletonList( lineageParam ) );

    List<IJobScheduleRequest> scheduleList = new ArrayList<>();
    scheduleList.add( scheduleRequest );

    TestableScheduleImportUtil scheduleImportUtil = new TestableScheduleImportUtil( scheduleList );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      IImportHelper.ImportContext ctx = createImportContext( false, false );
      scheduleImportUtil.doImport( ctx );

      // Job should NOT be imported — invalid uiPassParam causes the schedule to be skipped
      Assert.assertEquals( 0, ImportSession.getSession().getImportedScheduleJobIds().size() );
      verify( mockSchedulerResource, never() ).createJob( ArgumentMatchers.any( IJobScheduleRequest.class ) );
      // Error should have been logged about the invalid trigger, mentioning the job name
      verify( mockLogger ).error( ArgumentMatchers.argThat(
        ( String msg ) -> msg.contains( "InvalidUiPassParamJob" )
          && msg.contains( "import bundle is invalid" )
          && msg.contains( "BOGUS_VALUE" ) ) );
    }
  }

  @Test
  public void testCreateSchedulerJob_normalState_doesNotPauseJob() throws Exception {
    ScheduleImportUtil scheduleImportUtil = new ScheduleImportUtil();

    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setJobState( JobState.NORMAL );

    Response mockResponse = mock( Response.class );
    when( mockResponse.getEntity() ).thenReturn( "job-id-789" );
    when( mockSchedulerResource.createJob( scheduleRequest ) ).thenReturn( mockResponse );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      Response result = scheduleImportUtil.createSchedulerJob( mockSchedulerResource, scheduleRequest );

      Assert.assertEquals( mockResponse, result );
      verify( mockSchedulerResource, never() ).pauseJob( ArgumentMatchers.any( IJobRequest.class ) );
    }
  }

  @Test
  public void testCreateSchedulerJob_pausedState_pausesJob() throws Exception {
    ScheduleImportUtil scheduleImportUtil = new ScheduleImportUtil();

    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setJobState( JobState.PAUSED );

    Response mockResponse = mock( Response.class );
    when( mockResponse.getEntity() ).thenReturn( "job-id-paused" );
    when( mockSchedulerResource.createJob( scheduleRequest ) ).thenReturn( mockResponse );

    try ( MockedStatic<PentahoSystem> pentahoSystemMock = Mockito.mockStatic( PentahoSystem.class ) ) {
      stubPentahoSystem( pentahoSystemMock );

      Response result = scheduleImportUtil.createSchedulerJob( mockSchedulerResource, scheduleRequest );

      Assert.assertEquals( mockResponse, result );
      verify( mockJobRequest ).setJobId( "job-id-paused" );
      verify( mockSchedulerResource ).pauseJob( mockJobRequest );
    }
  }

  @Test
  public void testCreateSchedulerJob_nullScheduler_returnsNull() throws Exception {
    ScheduleImportUtil scheduleImportUtil = new ScheduleImportUtil();

    FakeJobScheduleRequest scheduleRequest = new FakeJobScheduleRequest();
    scheduleRequest.setJobState( JobState.NORMAL );

    Response result = scheduleImportUtil.createSchedulerJob( null, scheduleRequest );

    Assert.assertNull( result );
  }

  // ========== Helper classes ==========

  /**
   * A testable subclass that overrides the protected {@code getScheduleList()} method
   * to return controlled test data, avoiding the need to mock the ImportSession singleton
   * or use Mockito.spy().
   */
  private static class TestableScheduleImportUtil extends ScheduleImportUtil {
    private final List<IJobScheduleRequest> scheduleList;

    TestableScheduleImportUtil( List<IJobScheduleRequest> scheduleList ) {
      this.scheduleList = scheduleList;
    }

    @Override
    protected List<IJobScheduleRequest> getScheduleList() {
      return scheduleList;
    }
  }

  private static class FakeJobScheduleRequest implements IJobScheduleRequest {
    private String jobId;
    private String jobName;
    private String inputFile;
    private String outputFile;
    private long duration;
    private JobState jobState = JobState.NORMAL;
    private String actionClass;
    private String timeZone;
    private ISimpleJobTrigger simpleJobTrigger;
    private ICronJobTrigger cronJobTrigger;
    private List<IJobScheduleParam> jobParameters = new ArrayList<>();
    private Map<String, String> pdiParameters;

    @Override
    public void setJobId( String jobId ) {
      this.jobId = jobId;
    }

    @Override
    public String getJobId() {
      return jobId;
    }

    @Override
    public void setJobName( String jobName ) {
      this.jobName = jobName;
    }

    @Override
    public String getJobName() {
      return jobName;
    }

    @Override
    public void setDuration( long duration ) {
      this.duration = duration;
    }

    @Override
    public long getDuration() {
      return duration;
    }

    @Override
    public void setJobState( JobState state ) {
      this.jobState = state;
    }

    @Override
    public JobState getJobState() {
      return jobState;
    }

    @Override
    public void setInputFile( String inputFilePath ) {
      this.inputFile = inputFilePath;
    }

    @Override
    public String getInputFile() {
      return inputFile;
    }

    @Override
    public void setOutputFile( String outputFilePath ) {
      this.outputFile = outputFilePath;
    }

    @Override
    public String getOutputFile() {
      return outputFile;
    }

    @Override
    public Map<String, String> getPdiParameters() {
      return pdiParameters;
    }

    @Override
    public void setPdiParameters( Map<String, String> stringStringHashMap ) {
      this.pdiParameters = stringStringHashMap;
    }

    @Override
    public void setActionClass( String value ) {
      this.actionClass = value;
    }

    @Override
    public String getActionClass() {
      return actionClass;
    }

    @Override
    public void setTimeZone( String value ) {
      this.timeZone = value;
    }

    @Override
    public String getTimeZone() {
      return timeZone;
    }

    @Override
    public void setSimpleJobTrigger( ISimpleJobTrigger jobTrigger ) {
      this.simpleJobTrigger = jobTrigger;
    }

    @Override
    public ISimpleJobTrigger getSimpleJobTrigger() {
      return simpleJobTrigger;
    }

    @Override
    public void setCronJobTrigger( ICronJobTrigger cron ) {
      this.cronJobTrigger = cron;
    }

    @Override
    public ICronJobTrigger getCronJobTrigger() {
      return cronJobTrigger;
    }

    @Override
    public List<IJobScheduleParam> getJobParameters() {
      return jobParameters;
    }

    @Override
    public void setJobParameters( List<IJobScheduleParam> parameters ) {
      this.jobParameters = parameters;
    }
  }

  private static class ScheduleRequestMatcher implements ArgumentMatcher<IJobScheduleRequest> {
    private final String input;
    private final String output;

    public ScheduleRequestMatcher( String input, String output ) {
      this.input = input;
      this.output = output;
    }

    @Override
    public boolean matches( IJobScheduleRequest jsr ) {
      if ( jsr == null ) {
        return false;
      }
      boolean matchedInput = input.equals( FilenameUtils.separatorsToUnix( jsr.getInputFile() ) );
      boolean matchedOutput = output.equals( FilenameUtils.separatorsToUnix( jsr.getOutputFile() ) );
      return matchedInput && matchedOutput;
    }
  }
}
