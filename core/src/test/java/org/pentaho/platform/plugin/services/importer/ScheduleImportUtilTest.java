package org.pentaho.platform.plugin.services.importer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.pentaho.platform.api.engine.IAuthorizationPolicy;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.security.userroledao.IUserRoleDao;
import org.pentaho.platform.api.importexport.IImportHelper;
import org.pentaho.platform.api.mimetype.IPlatformMimeResolver;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.scheduler2.ICronJobTrigger;
import org.pentaho.platform.api.scheduler2.IJobScheduleParam;
import org.pentaho.platform.api.scheduler2.IJobScheduleRequest;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.ISchedulerResource;
import org.pentaho.platform.api.scheduler2.ISimpleJobTrigger;
import org.pentaho.platform.api.scheduler2.JobState;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.services.importexport.ImportSession;
import org.pentaho.platform.security.policy.rolebased.IRoleAuthorizationPolicyRoleBindingDao;

import jakarta.ws.rs.core.Response;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScheduleImportUtilTest {
  private ScheduleImportUtil scheduleImportUtil;
  private IImportHelper.ImportContext importContext;

  @Before
  public void setUp() throws Exception {
    mockToPentahoSystem( IUserRoleDao.class );
    mockToPentahoSystem( IUnifiedRepository.class );
    mockToPentahoSystem( IRoleAuthorizationPolicyRoleBindingDao.class );

    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      IPlatformMimeResolver mockMimeResolver = mock( IPlatformMimeResolver.class );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IPlatformMimeResolver.class ) )
        .thenReturn( mockMimeResolver );
    }

    IJobScheduleRequest jobScheduleRequest1 = mock( IJobScheduleRequest.class );
    IJobScheduleRequest jobScheduleRequest2 = mock( IJobScheduleRequest.class );
    ArrayList<IJobScheduleRequest> jobScheduleRequests = new ArrayList<>();
    jobScheduleRequests.add( jobScheduleRequest1 );
    jobScheduleRequests.add( jobScheduleRequest2 );

    importContext = new IImportHelper.ImportContext() {

      @Override
      public Log getLogger() {
        return mock( Log.class );
      }

      @Override
      public boolean isPerformingRestore() {
        return false;
      }

      @Override
      public boolean isOverwriteFile() {
        return false;
      }
    };

    scheduleImportUtil = spy( new ScheduleImportUtil() {
      protected List<IJobScheduleRequest> getScheduleList() {
        return jobScheduleRequests;
      };
    } );
  }

  private <T> T mockToPentahoSystem( Class<T> cl ) {
    T t = mock( cl );
    PentahoSystem.registerObject( t );
    return t;
  }

  @Test
  @Ignore
  public void testImportSchedules() throws Exception {
    List<IJobScheduleRequest> schedules = new ArrayList<>();
    IJobScheduleRequest scheduleRequest = Mockito.spy( new FakeJobSchedluerRequest() );
    schedules.add( scheduleRequest );

    Response response = mock( Response.class );
    when( response.getStatus() ).thenReturn( Response.Status.OK.getStatusCode() );
    when( response.getEntity() ).thenReturn( "job id" );

    doReturn( response ).when( scheduleImportUtil )
      .createSchedulerJob( ArgumentMatchers.any( ISchedulerResource.class ), ArgumentMatchers.eq( scheduleRequest ) );

    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class );
          MockedStatic<PentahoSessionHolder> pentahoSessionHolderMockedStatic = Mockito.mockStatic(
            PentahoSessionHolder.class ) ) {
      IAuthorizationPolicy iAuthorizationPolicyMock = mock( IAuthorizationPolicy.class );
      IScheduler iSchedulerMock = mock( IScheduler.class );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( ArgumentMatchers.eq( IAuthorizationPolicy.class ) ) )
        .thenReturn( iAuthorizationPolicyMock );
      pentahoSystemMockedStatic.when(
          () -> PentahoSystem.get( ArgumentMatchers.eq( IScheduler.class ), ArgumentMatchers.anyString(),
            ArgumentMatchers.eq( null ) ) )
        .thenReturn( iSchedulerMock );
      when( iSchedulerMock.getStatus() ).thenReturn( mock( IScheduler.SchedulerStatus.class ) );
      pentahoSessionHolderMockedStatic.when( PentahoSessionHolder::getSession )
        .thenReturn( mock( IPentahoSession.class ) );

      scheduleImportUtil.doImport( importContext );

      verify( scheduleImportUtil )
        .createSchedulerJob( ArgumentMatchers.any( ISchedulerResource.class ), ArgumentMatchers.eq( scheduleRequest ) );
      Assert.assertEquals( 1, ImportSession.getSession().getImportedScheduleJobIds().size() );
    }
  }

  @Test
  @Ignore
  public void testImportSchedules_FailsToCreateSchedule() throws Exception {
    List<IJobScheduleRequest> schedules = new ArrayList<>();
    IJobScheduleRequest scheduleRequest = Mockito.spy( new FakeJobSchedluerRequest() );
    scheduleRequest.setInputFile( "/home/admin/scheduledTransform.ktr" );
    scheduleRequest.setOutputFile( "/home/admin/scheduledTransform*" );
    schedules.add( scheduleRequest );

    Mockito.doThrow( new IOException( "error creating schedule" ) ).when( scheduleImportUtil ).createSchedulerJob(
      ArgumentMatchers.any( ISchedulerResource.class ), ArgumentMatchers.eq( scheduleRequest ) );

    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class );
          MockedStatic<PentahoSessionHolder> pentahoSessionHolderMockedStatic = Mockito.mockStatic(
            PentahoSessionHolder.class ) ) {
      IAuthorizationPolicy iAuthorizationPolicyMock = mock( IAuthorizationPolicy.class );
      IScheduler iSchedulerMock = mock( IScheduler.class );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( ArgumentMatchers.eq( IAuthorizationPolicy.class ) ) )
        .thenReturn( iAuthorizationPolicyMock );
      pentahoSystemMockedStatic.when(
          () -> PentahoSystem.get( ArgumentMatchers.eq( IScheduler.class ), ArgumentMatchers.anyString(),
            ArgumentMatchers.eq( null ) ) )
        .thenReturn( iSchedulerMock );
      when( iSchedulerMock.getStatus() ).thenReturn( mock( IScheduler.SchedulerStatus.class ) );
      pentahoSessionHolderMockedStatic.when( PentahoSessionHolder::getSession )
        .thenReturn( mock( IPentahoSession.class ) );


      scheduleImportUtil.doImport( importContext );
      Assert.assertEquals( 0, ImportSession.getSession().getImportedScheduleJobIds().size() );
    }
  }

  @Test
  @Ignore
  public void testImportSchedules_FailsToCreateScheduleWithSpace() throws Exception {
    List<IJobScheduleRequest> schedules = new ArrayList<>();
    IJobScheduleRequest scheduleRequest = Mockito.spy( new FakeJobSchedluerRequest() );
    scheduleRequest.setInputFile( "/home/admin/scheduled Transform.ktr" );
    scheduleRequest.setOutputFile( "/home/admin/scheduled Transform*" );
    schedules.add( scheduleRequest );

    ScheduleRequestMatcher throwMatcher =
      new ScheduleRequestMatcher( "/home/admin/scheduled Transform.ktr", "/home/admin/scheduled Transform*" );
    Mockito.doThrow( new IOException( "error creating schedule" ) ).when( scheduleImportUtil ).createSchedulerJob(
      ArgumentMatchers.any( ISchedulerResource.class ), ArgumentMatchers.argThat( throwMatcher ) );

    Response response = mock( Response.class );
    when( response.getStatus() ).thenReturn( Response.Status.OK.getStatusCode() );
    when( response.getEntity() ).thenReturn( "job id" );
    ScheduleRequestMatcher goodMatcher =
      new ScheduleRequestMatcher( "/home/admin/scheduled_Transform.ktr", "/home/admin/scheduled_Transform*" );
    doReturn( response ).when( scheduleImportUtil ).createSchedulerJob( ArgumentMatchers.any( ISchedulerResource.class ),
      ArgumentMatchers.argThat( goodMatcher ) );

    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class );
          MockedStatic<PentahoSessionHolder> pentahoSessionHolderMockedStatic = Mockito.mockStatic(
            PentahoSessionHolder.class ) ) {
      IAuthorizationPolicy iAuthorizationPolicyMock = mock( IAuthorizationPolicy.class );
      IScheduler iSchedulerMock = mock( IScheduler.class );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( ArgumentMatchers.eq( IAuthorizationPolicy.class ) ) )
        .thenReturn( iAuthorizationPolicyMock );
      pentahoSystemMockedStatic.when(
          () -> PentahoSystem.get( ArgumentMatchers.eq( IScheduler.class ), ArgumentMatchers.anyString(),
            ArgumentMatchers.eq( null ) ) )
        .thenReturn( iSchedulerMock );
      when( iSchedulerMock.getStatus() ).thenReturn( mock( IScheduler.SchedulerStatus.class ) );
      pentahoSessionHolderMockedStatic.when( PentahoSessionHolder::getSession )
        .thenReturn( mock( IPentahoSession.class ) );
      scheduleImportUtil.doImport( importContext );

      verify( scheduleImportUtil, times( 2 ) ).createSchedulerJob(
        ArgumentMatchers.any( ISchedulerResource.class ), ArgumentMatchers.any( IJobScheduleRequest.class ) );
      Assert.assertEquals( 1, ImportSession.getSession().getImportedScheduleJobIds().size() );
    }
  }


  private static class FakeJobSchedluerRequest implements IJobScheduleRequest {
    private String inputFile;

    @Override public void setJobId( String jobId ) {

    }

    @Override public String getJobId() {
      return null;
    }

    @Override public void setJobName( String jobName ) {

    }

    @Override public void setDuration( long duration ) {

    }

    @Override public void setJobState( JobState state ) {

    }

    @Override public void setInputFile( String inputFilePath ) {
      inputFile = inputFilePath;
    }

    @Override public void setOutputFile( String outputFilePath ) {

    }

    @Override public Map<String, String> getPdiParameters() {
      return null;
    }

    @Override public void setPdiParameters( Map<String, String> stringStringHashMap ) {

    }

    @Override public void setActionClass( String value ) {

    }

    @Override public String getActionClass() {
      return null;
    }

    @Override public void setTimeZone( String value ) {

    }

    @Override public String getTimeZone() {
      return null;
    }

    @Override public void setSimpleJobTrigger( ISimpleJobTrigger jobTrigger ) {

    }

    @Override public ISimpleJobTrigger getSimpleJobTrigger() {
      return null;
    }

    @Override public void setCronJobTrigger( ICronJobTrigger cron ) {

    }

    @Override public String getInputFile() {
      return inputFile;
    }

    @Override public String getJobName() {
      return null;
    }

    @Override public String getOutputFile() {
      return null;
    }

    @Override public List<IJobScheduleParam> getJobParameters() {
      return null;
    }

    @Override public void setJobParameters( List<IJobScheduleParam> parameters ) {

    }

    @Override public long getDuration() {
      return 0;
    }

    @Override public JobState getJobState() {
      return null;
    }

    @Override public ICronJobTrigger getCronJobTrigger() {
      return null;
    }
  }

  private static class ScheduleRequestMatcher implements ArgumentMatcher<IJobScheduleRequest> {
    private final String input;
    private final String output;

    public ScheduleRequestMatcher( String input, String output ) {
      this.input = input;
      this.output = output;
    }

    @Override public boolean matches( IJobScheduleRequest jsr ) {
      boolean matchedInput = input.equals( FilenameUtils.separatorsToUnix( jsr.getInputFile() ) );
      boolean matchedOutput = output.equals( FilenameUtils.separatorsToUnix( jsr.getOutputFile() ) );
      return matchedInput && matchedOutput;
    }
  }
}
