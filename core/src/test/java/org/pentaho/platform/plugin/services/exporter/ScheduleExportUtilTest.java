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


package org.pentaho.platform.plugin.services.exporter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.pentaho.platform.api.engine.IPentahoObjectFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.scheduler2.ComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.CronJobTrigger;
import org.pentaho.platform.api.scheduler2.IBlockoutManager;
import org.pentaho.platform.api.scheduler2.IJob;
import org.pentaho.platform.api.scheduler2.IJobScheduleParam;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.JobTrigger;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.action.mondrian.catalog.IMondrianCatalogService;
import org.pentaho.platform.plugin.services.importexport.exportManifest.ExportManifest;
import org.pentaho.platform.plugin.services.importexport.legacy.MondrianCatalogRepositoryHelper;
import org.pentaho.platform.web.http.api.resources.JobScheduleRequest;
import org.pentaho.platform.web.http.api.resources.RepositoryFileStreamProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ScheduleExportUtilTest {

  ScheduleExportUtil exporterSpy;
  IUnifiedRepository repo;
  IScheduler scheduler;
  IPentahoSession session;
  IMondrianCatalogService mondrianCatalogService;
  MondrianCatalogRepositoryHelper mondrianCatalogRepositoryHelper;
  IPentahoObjectFactory objectFactory;
  JobScheduleRequest jobScheduleRequest;
  MockedStatic<PentahoSystem> pentahoSystem;
  ExportManifest exportManifest;

  @Before
  public void setUp() throws Exception {
    repo = mock( IUnifiedRepository.class );
    scheduler = mock( IScheduler.class );
    session = mock( IPentahoSession.class );
    mondrianCatalogService = mock( IMondrianCatalogService.class );
    mondrianCatalogRepositoryHelper = mock( MondrianCatalogRepositoryHelper.class );
    pentahoSystem = mockStatic( PentahoSystem.class );
    objectFactory = mock( IPentahoObjectFactory.class );
    jobScheduleRequest = mock( JobScheduleRequest.class );
    exportManifest = spy( new ExportManifest() );
    PentahoSessionHolder.setSession( session );
    exporterSpy = spy( new ScheduleExportUtil() );
    doReturn( "session name" ).when( session ).getName();
    pentahoSystem.when( () -> PentahoSystem.get( IScheduler.class, "IScheduler2", null ) ).thenReturn( scheduler );
    when( scheduler.createJobScheduleRequest() ).thenReturn( jobScheduleRequest );
    when( jobScheduleRequest.getJobName() ).thenReturn( "JOB" );
  }

  @After
  public void tearDown() {
    PentahoSystem.clearObjectFactory();
    pentahoSystem.close();
  }


  @Test( expected = IllegalArgumentException.class )
  public void testCreateJobScheduleRequest_null() {
    ScheduleExportUtil.createJobScheduleRequest( null );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testCreateJobScheduleRequest_unknownTrigger() {

    Job job = mock( Job.class );
    JobTrigger trigger = mock( JobTrigger.class );

    when( job.getJobTrigger() ).thenReturn( trigger );

    JobScheduleRequest jobScheduleRequest = ScheduleExportUtil.createJobScheduleRequest( job );

  }

  @Test
  public void testCreateJobScheduleRequest_SimpleJobTrigger() {
    String jobName = "JOB";

    Job job = mock( Job.class );
    SimpleJobTrigger trigger = mock( SimpleJobTrigger.class );

    when( job.getJobTrigger() ).thenReturn( trigger );
    when( job.getJobName() ).thenReturn( jobName );
    when( jobScheduleRequest.getSimpleJobTrigger() ).thenReturn( trigger );

    JobScheduleRequest jobScheduleRequest = ScheduleExportUtil.createJobScheduleRequest( job );

    assertNotNull( jobScheduleRequest );
    assertEquals( jobName, jobScheduleRequest.getJobName() );
    assertEquals( trigger, jobScheduleRequest.getSimpleJobTrigger() );
  }

  @Test
  public void testCreateJobScheduleRequest_NoStreamProvider() {
    String jobName = "JOB";

    Job job = mock( Job.class );
    SimpleJobTrigger trigger = mock( SimpleJobTrigger.class );

    when( job.getJobTrigger() ).thenReturn( trigger );
    when( job.getJobName() ).thenReturn( jobName );
    Map<String, Object> params = new HashMap<>();
    params.put( "directory", "/home/admin" );
    params.put( "transformation", "myTransform" );

    HashMap<String, String> pdiParams = new HashMap<>();
    pdiParams.put( "pdiParam", "pdiParamValue" );
    params.put( ScheduleExportUtil.RUN_PARAMETERS_KEY, pdiParams );

    when( job.getJobParams() ).thenReturn( params );
    when( jobScheduleRequest.getSimpleJobTrigger() ).thenReturn( trigger );
    when( jobScheduleRequest.getInputFile() ).thenReturn( "/home/admin/myTransform.ktr" );
    when( jobScheduleRequest.getOutputFile() ).thenReturn( "/home/admin/myTransform*" );

    JobScheduleRequest jobScheduleRequest = ScheduleExportUtil.createJobScheduleRequest( job );

    assertNotNull( jobScheduleRequest );
    assertEquals( jobName, jobScheduleRequest.getJobName() );
    assertEquals( trigger, jobScheduleRequest.getSimpleJobTrigger() );
    assertEquals( "/home/admin/myTransform.ktr", jobScheduleRequest.getInputFile() );
    assertEquals( "/home/admin/myTransform*", jobScheduleRequest.getOutputFile() );
  }

  @Test
  public void testCreateJobScheduleRequest_StringStreamProvider() {
    String jobName = "JOB";

    Job job = mock( Job.class );
    SimpleJobTrigger trigger = mock( SimpleJobTrigger.class );

    when( job.getJobTrigger() ).thenReturn( trigger );
    when( job.getJobName() ).thenReturn( jobName );
    Map<String, Object> params = new HashMap<>();
    params.put( IScheduler.RESERVEDMAPKEY_STREAMPROVIDER,
      "import file = /home/admin/myJob.kjb:output file=/home/admin/myJob*" );
    when( job.getJobParams() ).thenReturn( params );
    when( jobScheduleRequest.getInputFile() ).thenReturn( "/home/admin/myJob.kjb" );
    when( jobScheduleRequest.getOutputFile() ).thenReturn( "/home/admin/myJob*" );
    when( jobScheduleRequest.getSimpleJobTrigger() ).thenReturn( trigger );
    JobScheduleRequest jobScheduleRequest = ScheduleExportUtil.createJobScheduleRequest( job );

    assertNotNull( jobScheduleRequest );
    assertEquals( jobName, jobScheduleRequest.getJobName() );
    assertEquals( trigger, jobScheduleRequest.getSimpleJobTrigger() );
    assertEquals( "/home/admin/myJob.kjb", jobScheduleRequest.getInputFile() );
    assertEquals( "/home/admin/myJob*", jobScheduleRequest.getOutputFile() );
  }

  @Test
  public void testCreateJobScheduleRequest_ComplexJobTrigger() {
    String jobName = "JOB";
    Date now = new Date();

    Job job = mock( Job.class );
    ComplexJobTrigger trigger = mock( ComplexJobTrigger.class );

    CronJobTrigger cronJobTrigger = mock( CronJobTrigger.class );
    when( scheduler.createCronJobTrigger() ).thenReturn( cronJobTrigger );
    when( jobScheduleRequest.getCronJobTrigger() ).thenReturn( cronJobTrigger );

    when( job.getJobTrigger() ).thenReturn( trigger );
    when( job.getJobName() ).thenReturn( jobName );

    when( trigger.getCronString() ).thenReturn( "0 30 13 ? * 2,3,4,5,6 *" );
    when( trigger.getDuration() ).thenReturn( -1L );
    when( trigger.getStartTime() ).thenReturn( now );
    when( trigger.getEndTime() ).thenReturn( now );
    when( trigger.getUiPassParam() ).thenReturn( "uiPassParam" );
    when( jobScheduleRequest.getCronJobTrigger().getCronString() ).thenReturn( "0 30 13 ? * 2,3,4,5,6 *" );
    when( jobScheduleRequest.getCronJobTrigger().getDuration() ).thenReturn( -1L );
    when( jobScheduleRequest.getCronJobTrigger().getEndTime() ).thenReturn( now );
    when( jobScheduleRequest.getCronJobTrigger().getStartTime() ).thenReturn( now );
    when( jobScheduleRequest.getCronJobTrigger().getUiPassParam() ).thenReturn( "uiPassParam" );

    JobScheduleRequest jobScheduleRequest = ScheduleExportUtil.createJobScheduleRequest( job );

    assertNotNull( jobScheduleRequest );
    assertEquals( jobName, jobScheduleRequest.getJobName() );

    // we should be getting back a cron trigger, not a complex trigger.
    assertNull( jobScheduleRequest.getSimpleJobTrigger() );
    assertNull( jobScheduleRequest.getComplexJobTrigger() );
    assertNotNull( jobScheduleRequest.getCronJobTrigger() );

    assertEquals( trigger.getCronString(), jobScheduleRequest.getCronJobTrigger().getCronString() );
    assertEquals( trigger.getDuration(), jobScheduleRequest.getCronJobTrigger().getDuration() );
    assertEquals( trigger.getEndTime(), jobScheduleRequest.getCronJobTrigger().getEndTime() );
    assertEquals( trigger.getStartTime(), jobScheduleRequest.getCronJobTrigger().getStartTime() );
    assertEquals( trigger.getUiPassParam(), jobScheduleRequest.getCronJobTrigger().getUiPassParam() );
  }

  @Test
  public void testCreateJobScheduleRequest_CronJobTrigger() {
    String jobName = "JOB";

    Job job = mock( Job.class );
    CronJobTrigger trigger = mock( CronJobTrigger.class );

    when( job.getJobTrigger() ).thenReturn( trigger );
    when( job.getJobName() ).thenReturn( jobName );
    when( jobScheduleRequest.getCronJobTrigger() ).thenReturn( trigger );

    JobScheduleRequest jobScheduleRequest = ScheduleExportUtil.createJobScheduleRequest( job );

    assertNotNull( jobScheduleRequest );
    assertEquals( jobName, jobScheduleRequest.getJobName() );
    assertEquals( trigger, jobScheduleRequest.getCronJobTrigger() );
  }

  @Test
  public void testCreateJobScheduleRequest_StreamProviderJobParam() {
    String jobName = "JOB";
    String inputPath = "/input/path/to/file.ext";
    String outputPath = "/output/path/location.*";

    Map<String, Object> params = new HashMap<>();

    RepositoryFileStreamProvider streamProvider = mock( RepositoryFileStreamProvider.class );
    params.put( IScheduler.RESERVEDMAPKEY_STREAMPROVIDER, streamProvider );

    Job job = mock( Job.class );
    CronJobTrigger trigger = mock( CronJobTrigger.class );

    when( jobScheduleRequest.getInputFile() ).thenReturn( inputPath );
    when( jobScheduleRequest.getOutputFile() ).thenReturn( outputPath );
    when( job.getJobTrigger() ).thenReturn( trigger );
    when( job.getJobName() ).thenReturn( jobName );
    when( job.getJobParams() ).thenReturn( params );
    when( streamProvider.getInputFilePath() ).thenReturn( inputPath );
    when( streamProvider.getOutputFilePath() ).thenReturn( outputPath );

    JobScheduleRequest jobScheduleRequest = ScheduleExportUtil.createJobScheduleRequest( job );
    assertEquals( inputPath, jobScheduleRequest.getInputFile() );
    assertEquals( outputPath, jobScheduleRequest.getOutputFile() );
    assertEquals( 0, jobScheduleRequest.getJobParameters().size() );
  }

  @Test
  public void testCreateJobScheduleRequest_ActionClassJobParam() {
    String jobName = "JOB";
    String actionClass = "com.pentaho.Action";
    Map<String, Object> params = new HashMap<>();

    params.put( IScheduler.RESERVEDMAPKEY_ACTIONCLASS, actionClass );

    Job job = mock( Job.class );
    CronJobTrigger trigger = mock( CronJobTrigger.class );

    when( jobScheduleRequest.getActionClass() ).thenReturn( actionClass );
    when( job.getJobTrigger() ).thenReturn( trigger );
    when( job.getJobName() ).thenReturn( jobName );
    when( job.getJobParams() ).thenReturn( params );

    JobScheduleRequest jobScheduleRequest = ScheduleExportUtil.createJobScheduleRequest( job );
    assertEquals( actionClass, jobScheduleRequest.getActionClass() );
  }

  @Test
  public void testCreateJobScheduleRequest_TimeZoneJobParam() {
    String jobName = "JOB";
    String timeZone = "America/New_York";
    Map<String, Object> params = new HashMap<>();

    params.put( IBlockoutManager.TIME_ZONE_PARAM, timeZone );

    Job job = mock( Job.class );
    CronJobTrigger trigger = mock( CronJobTrigger.class );
    when( jobScheduleRequest.getTimeZone() ).thenReturn( timeZone );
    when( job.getJobTrigger() ).thenReturn( trigger );
    when( job.getJobName() ).thenReturn( jobName );
    when( job.getJobParams() ).thenReturn( params );

    JobScheduleRequest jobScheduleRequest = ScheduleExportUtil.createJobScheduleRequest( job );
    assertEquals( timeZone, jobScheduleRequest.getTimeZone() );
  }

  @Test
  public void testCreateJobScheduleRequest_MultipleTypesJobParam() {
    String jobName = "JOB";
    Long l = Long.MAX_VALUE;
    Date d = new Date();
    Boolean b = true;

    Map<String, Object> params = new HashMap<>();

    params.put( "NumberValue", l );
    params.put( "DateValue", d );
    params.put( "BooleanValue", b );

    Job job = mock( Job.class );
    CronJobTrigger trigger = mock( CronJobTrigger.class );

    when( job.getJobTrigger() ).thenReturn( trigger );
    when( job.getJobName() ).thenReturn( jobName );
    when( job.getJobParams() ).thenReturn( params );

    JobScheduleRequest jobScheduleRequest = ScheduleExportUtil.createJobScheduleRequest( job );
    List<IJobScheduleParam> jobScheduleParams = jobScheduleRequest.getJobParameters();
    for ( IJobScheduleParam jobScheduleParam : jobScheduleParams ) {
      assertTrue( jobScheduleParam.getValue().equals( l )
        || jobScheduleParam.getValue().equals( d )
        || jobScheduleParam.getValue().equals( b ) );
    }
  }

  @Test
  public void testConstructor() {
    // only needed to get 100% code coverage
    assertNotNull( new ScheduleExportUtil() );
  }

  @Test
  public void testExportSchedules() throws Exception {
    List<IJob> jobs = new ArrayList<>();
    ComplexJobTrigger trigger = mock( ComplexJobTrigger.class );
    JobTrigger unknownTrigger = mock( JobTrigger.class );
    CronJobTrigger cronJobTrigger = mock( CronJobTrigger.class );

    Job job1 = mock( Job.class );
    Job job2 = mock( Job.class );
    Job job3 = mock( Job.class );
    jobs.add( job1 );
    jobs.add( job2 );
    jobs.add( job3 );

    when( scheduler.getJobs( null ) ).thenReturn( jobs );
    when( scheduler.createCronJobTrigger() ).thenReturn( cronJobTrigger );
    when( job1.getJobName() ).thenReturn( "job 1" );
    when( job1.getJobTrigger() ).thenReturn( trigger );
    when( job2.getJobName() ).thenReturn( "job 2" );
    when( job2.getJobTrigger() ).thenReturn( trigger );
    when( job3.getJobName() ).thenReturn( "job 3" );
    when( job3.getJobTrigger() ).thenReturn( unknownTrigger );
    PentahoPlatformExporter exporter = new PentahoPlatformExporter( repo );
    exporter.setExportManifest( exportManifest );
    exporterSpy.doExport( exporter );

    verify( scheduler ).getJobs( null );
    assertEquals( 2, exportManifest.getScheduleList().size() );
  }

  @Test
  public void testExportSchedules_SchedulereThrowsException() throws Exception {
    when( scheduler.getJobs( null ) ).thenThrow( new SchedulerException( "bad" ) );

    exporterSpy.exportSchedules();

    verify( scheduler ).getJobs( null );
    assertEquals( 0, exportManifest.getScheduleList().size() );
  }
}