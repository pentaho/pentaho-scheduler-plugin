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
 * Copyright (c) 2002-2023 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.web.http.api.resources.services;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.action.IAction;
import org.pentaho.platform.api.engine.IAuthorizationPolicy;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ISecurityHelper;
import org.pentaho.platform.api.engine.ServiceException;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.UnifiedRepositoryException;
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
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.scheduler2.blockout.BlockoutAction;
import org.pentaho.platform.security.policy.rolebased.actions.AdministerSecurityAction;
import org.pentaho.platform.security.policy.rolebased.actions.SchedulerAction;
import org.pentaho.platform.security.policy.rolebased.actions.SchedulerExecuteAction;
import org.pentaho.platform.util.ActionUtil;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.pentaho.platform.web.http.api.proxies.BlockStatusProxy;
import org.pentaho.platform.web.http.api.resources.ComplexJobTriggerProxy;
import org.pentaho.platform.web.http.api.resources.JobRequest;
import org.pentaho.platform.web.http.api.resources.JobScheduleParam;
import org.pentaho.platform.web.http.api.resources.JobScheduleRequest;
import org.pentaho.platform.web.http.api.resources.RepositoryFileStreamProvider;
import org.pentaho.platform.web.http.api.resources.SchedulerOutputPathResolver;
import org.pentaho.platform.web.http.api.resources.SchedulerResourceUtil;
import org.pentaho.platform.web.http.api.resources.SessionResource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings( "unused" )
public class SchedulerService implements ISchedulerServicePlugin {
  private static final Log logger = LogFactory.getLog( SchedulerService.class );
  protected IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", null ); //$NON-NLS-1$
  protected IAuthorizationPolicy policy;
  protected IUnifiedRepository repository;
  protected SessionResource sessionResource;
  protected FileService fileService;
  protected IBlockoutManager blockoutManager;

  @Override
  public Job createJob( JobScheduleRequest scheduleRequest )
    throws IOException, SchedulerException, IllegalAccessException {
    // Used to determine if created by a RunInBackgroundCommand
    boolean runInBackground =
      scheduleRequest.getSimpleJobTrigger() == null && scheduleRequest.getComplexJobTrigger() == null
        && scheduleRequest.getCronJobTrigger() == null;

    if ( !runInBackground && !isScheduleAllowed() ) {
      throw new SecurityException();
    }

    boolean hasInputFile = !StringUtils.isEmpty( scheduleRequest.getInputFile() );
    RepositoryFile file = null;

    if ( hasInputFile ) {
      try {
        file = getRepository().getFile( scheduleRequest.getInputFile() );
      } catch ( UnifiedRepositoryException ure ) {
        hasInputFile = false;
        logger.warn( ure.getMessage(), ure );
      }
    }

    // if we have an input file, generate job name based on that if the name is not passed in
    if ( hasInputFile && StringUtils.isEmpty( scheduleRequest.getJobName() ) ) {
      scheduleRequest.setJobName( file.getName().substring( 0, file.getName().lastIndexOf( "." ) ) ); //$NON-NLS-1$
    } else if ( !StringUtils.isEmpty( scheduleRequest.getActionClass() ) ) {
      String actionClass =
        scheduleRequest.getActionClass().substring( scheduleRequest.getActionClass().lastIndexOf( "." ) + 1 );
      scheduleRequest.setJobName( actionClass ); //$NON-NLS-1$
    } else if ( !hasInputFile && StringUtils.isEmpty( scheduleRequest.getJobName() ) ) {
      // just make up a name
      scheduleRequest.setJobName( "" + System.currentTimeMillis() ); //$NON-NLS-1$
    }

    if ( hasInputFile ) {
      if ( file == null ) {
        logger.error( "Cannot find input source file " + scheduleRequest.getInputFile() + " Aborting schedule..." );
        throw new SchedulerException(
          new ServiceException( "Cannot find input source file " + scheduleRequest.getInputFile() ) );
      }

      Map<String, Serializable> metadata = getRepository().getFileMetadata( file.getId() );

      if ( metadata.containsKey( RepositoryFile.SCHEDULABLE_KEY ) ) {
        boolean schedulable = BooleanUtils.toBoolean( (String) metadata.get( RepositoryFile.SCHEDULABLE_KEY ) );

        if ( !schedulable ) {
          throw new IllegalAccessException();
        }
      }
    }

    if ( scheduleRequest.getTimeZone() != null ) {
      updateStartDateForTimeZone( scheduleRequest );
    }

    Job job;

    IJobTrigger jobTrigger = SchedulerResourceUtil.convertScheduleRequestToJobTrigger( scheduleRequest, scheduler );

    HashMap<String, Serializable> parameterMap = new HashMap<>();

    List<IJobScheduleParam> parameters = scheduleRequest.getJobParameters();

    for ( IJobScheduleParam param : parameters ) {
      parameterMap.put( param.getName(), param.getValue() );
    }

    if ( scheduleRequest.getRunSafeMode() != null ) {
      parameterMap.put( "runSafeMode", scheduleRequest.getRunSafeMode() );
    }

    if ( scheduleRequest.getGatheringMetrics() != null ) {
      parameterMap.put( "gatheringMetrics", scheduleRequest.getGatheringMetrics() );
    }

    if ( scheduleRequest.getLogLevel() != null ) {
      parameterMap.put( "logLevel", scheduleRequest.getLogLevel() );
    }

    if ( isPdiFile( file ) ) {
      parameterMap = handlePDIScheduling( file, parameterMap, scheduleRequest.getPdiParameters() );
    }

    parameterMap.put( LocaleHelper.USER_LOCALE_PARAM, LocaleHelper.getLocale() );

    if ( hasInputFile ) {
      String outputFile = resolveOutputFilePath( scheduleRequest );
      String actionId = SchedulerResourceUtil.resolveActionId( scheduleRequest.getInputFile() );
      final String inputFile = scheduleRequest.getInputFile();
      parameterMap.put( ActionUtil.QUARTZ_STREAMPROVIDER_INPUT_FILE, inputFile );
      job =
        (Job) schedulerCreateJob( scheduleRequest.getJobName(), actionId, parameterMap, jobTrigger,
          inputFile, outputFile, scheduleRequest );
    } else {
      //TODO  need to locate actions from plugins if done this way too (but for now, we're just on main)
      // We will first attempt to get action class and if it fails we get the registered bean id.
      String actionClass = scheduleRequest.getActionClass();

      try {
        Class<IAction> iaction = getAction( actionClass );
        job = (Job) getScheduler().createJob( scheduleRequest.getJobName(), iaction, parameterMap, jobTrigger );
      } catch ( ClassNotFoundException e ) {
        String actionId = SchedulerResourceUtil.resolveActionIdFromClass( actionClass );
        job = (Job) getScheduler().createJob( scheduleRequest.getJobName(), actionId, parameterMap, jobTrigger );
      }
    }

    return job;
  }

  /**
   * Wrapper function around {@link SchedulerOutputPathResolver#resolveOutputFilePath()} calls
   * {@link #getSchedulerOutputPathResolver(JobScheduleRequest)} to get instance.
   */
  protected String resolveOutputFilePath( JobScheduleRequest scheduleRequest ) {
    SchedulerOutputPathResolver outputPathResolver = getSchedulerOutputPathResolver( scheduleRequest );
    return outputPathResolver.resolveOutputFilePath();
  }

  /**
   * Wrapper function around
   * {@link IScheduler#createJob(String, Class, Map, IJobTrigger, IBackgroundExecutionStreamProvider)} .
   * Mainly allowing for different implementation for the underlying input and output streams
   * through {@link #createIBackgroundExecutionStreamProvider(String, String, JobScheduleRequest)}
   */
  protected IJob schedulerCreateJob( String jobName, String action, Map<String, Serializable> jobParams,
                                     IJobTrigger trigger, final String inputFilePath, final String outputFilePath,
                                     JobScheduleRequest jobScheduleRequest ) throws SchedulerException {
    return getScheduler().createJob( jobName, action, jobParams, trigger,
      createIBackgroundExecutionStreamProvider( inputFilePath, outputFilePath, jobScheduleRequest ) );
  }

  /**
   * Instantiate of {@link IBackgroundExecutionStreamProvider}.
   */
  public IBackgroundExecutionStreamProvider createIBackgroundExecutionStreamProvider( final String inputFilePath,
                                                                                      final String outputFilePath,
                                                                                      JobScheduleRequest jobScheduleRequest ) {
    //NOTE: this code base only supports one type of provider
    return createRepositoryFileStreamProvider( inputFilePath, outputFilePath, jobScheduleRequest );
  }

  /**
   * Instantiate of {@link RepositoryFileStreamProvider}.
   */
  protected IBackgroundExecutionStreamProvider createRepositoryFileStreamProvider( final String inputFilePath,
                                                                                   final String outputFilePath,
                                                                                   JobScheduleRequest jobScheduleRequest ) {
    return new RepositoryFileStreamProvider( inputFilePath, outputFilePath,
      getAutoCreateUniqueFilename( jobScheduleRequest ), getAppendDateFormat( jobScheduleRequest ) );
  }

  @Override
  public Job updateJob( JobScheduleRequest scheduleRequest )
    throws IllegalAccessException, IOException, SchedulerException {
    Job job = (Job) getJob( scheduleRequest.getJobId() );

    if ( job != null ) {
      scheduleRequest.getJobParameters()
        .add( new JobScheduleParam( IScheduler.RESERVEDMAPKEY_ACTIONUSER, job.getUserName() ) );
    }

    Job newJob = createJob( scheduleRequest );
    removeJob( scheduleRequest.getJobId() );

    return newJob;
  }

  @Override
  public Job triggerNow( String jobId ) throws SchedulerException {
    Job job = (Job) getJob( jobId );

    if ( isScheduleAllowed() || getSession().getName().equals( job.getUserName() ) ) {
      getScheduler().triggerNow( jobId );
      // update job state
      job = (Job) getJob( jobId );
    }

    return job;
  }

  @Override
  public Job getContentCleanerJob() throws SchedulerException {
    IPentahoSession session = getSession();
    final String principalName = session.getName(); // this authentication wasn't matching with the job username,
    // changed to get name via the current session
    final boolean canAdminister = canAdminister();

    List<IJob> jobs = getScheduler().getJobs( getJobFilter( canAdminister, principalName ) );

    if ( !jobs.isEmpty() ) {
      return (Job) jobs.get( 0 );
    }

    return null;
  }

  @Override
  public List<RepositoryFileDto> doGetGeneratedContentForSchedule( String lineageId ) throws FileNotFoundException {
    return getFileService().searchGeneratedContent( getSessionResource().doGetCurrentUserDir(), lineageId,
      IScheduler.RESERVEDMAPKEY_LINEAGE_ID );
  }

  @Override
  public IJob getJob( String jobId ) throws SchedulerException {
    return getScheduler().getJob( jobId );
  }

  public boolean isScheduleAllowed() {
    return getPolicy().isAllowed( SchedulerAction.NAME );
  }

  public boolean isExecuteScheduleAllowed() {
    return getPolicy().isAllowed( SchedulerExecuteAction.NAME );
  }

  @Override
  public boolean isScheduleAllowed( String id ) {
    boolean canSchedule = isScheduleAllowed();

    if ( canSchedule ) {
      Map<String, Serializable> metadata = getRepository().getFileMetadata( id );

      if ( metadata.containsKey( RepositoryFile.SCHEDULABLE_KEY ) ) {
        canSchedule = BooleanUtils.toBoolean( (String) metadata.get( RepositoryFile.SCHEDULABLE_KEY ) );
      }
    }

    return canSchedule;
  }

  public IJobFilter getJobFilter( boolean canAdminister, String principalName ) {
    return new JobFilter( canAdminister, principalName );
  }

  private static class JobFilter implements IJobFilter {
    private final boolean canAdminister;
    private final String principalName;

    public JobFilter( boolean canAdminister, String principalName ) {
      this.canAdminister = canAdminister;
      this.principalName = principalName;
    }

    @Override
    public boolean accept( IJob job ) {
      String actionClass = (String) job.getJobParams().get( "ActionAdapterQuartzJob-ActionClass" );

      if ( canAdminister && "org.pentaho.platform.admin.GeneratedContentCleaner".equals( actionClass ) ) {
        return true;
      }

      return principalName.equals( job.getUserName() ) && "org.pentaho.platform.admin.GeneratedContentCleaner".equals(
        actionClass );
    }
  }

  @Override
  public String doGetCanSchedule() {
    return String.valueOf( isScheduleAllowed() );
  }

  @Override
  public String doGetCanExecuteSchedule() {
    return String.valueOf( isExecuteScheduleAllowed() );
  }

  @Override
  public String getState() throws SchedulerException {
    return getScheduler().getStatus().name();
  }

  @Override
  public String start() throws SchedulerException {
    if ( isScheduleAllowed() ) {
      getScheduler().start();
    }

    return getScheduler().getStatus().name();
  }

  @Override
  public String pause() throws SchedulerException {
    if ( isScheduleAllowed() ) {
      getScheduler().pause();
    }

    return getScheduler().getStatus().name();
  }

  @Override
  public String shutdown() throws SchedulerException {
    if ( isScheduleAllowed() ) {
      getScheduler().shutdown();
    }

    return getScheduler().getStatus().name();
  }

  @Override
  public JobState pauseJob( String jobId ) throws SchedulerException {
    Job job = (Job) getJob( jobId );

    if ( isScheduleAllowed() || getSession().getName().equals( job.getUserName() ) ) {
      getScheduler().pauseJob( jobId );
      job = (Job) getJob( jobId );
    }

    return job.getState();
  }

  @Override
  public JobState resumeJob( String jobId ) throws SchedulerException {
    Job job = (Job) getJob( jobId );

    if ( isScheduleAllowed() || getSession().getName().equals( job.getUserName() ) ) {
      getScheduler().resumeJob( jobId );
      job = (Job) getJob( jobId );
    }

    return job.getState();
  }

  @Override
  public boolean removeJob( String jobId ) throws SchedulerException {
    Job job = (Job) getJob( jobId );

    if ( isScheduleAllowed() || getSession().getName().equals( job.getUserName() ) ) {
      getScheduler().removeJob( jobId );
      return true;
    }

    return false;
  }

  @SuppressWarnings( "java:S112" )
  @Override
  public IJob getJobInfo( String jobId ) throws SchedulerException {
    Job job = (Job) getJob( jobId );

    if ( job == null ) {
      return null;
    }

    if ( canAdminister() || getSession().getName().equals( job.getUserName() ) ) {
      for ( String key : job.getJobParams().keySet() ) {
        Serializable value = job.getJobParams().get( key );

        if ( value != null && value.getClass().isArray() ) {
          ArrayList<String> list = new ArrayList<>();
          Collections.addAll( list, (String[]) value );
          job.getJobParams().put( key, list );
        }
      }

      return job;
    } else {
      throw new RuntimeException( "Job not found or improper credentials for access" );
    }
  }

  @Override
  public List<IJob> getBlockOutJobs() {
    return getBlockoutManager().getBlockOutJobs();
  }

  @Override
  public boolean hasBlockouts() {
    List<IJob> jobs = getBlockoutManager().getBlockOutJobs();
    return jobs != null && !jobs.isEmpty();
  }

  @Override
  public boolean willFire( IJobTrigger trigger ) {
    return getBlockoutManager().willFire( trigger );
  }

  @Override
  public boolean shouldFireNow() {
    return getBlockoutManager().shouldFireNow();
  }

  @Override
  public IJob addBlockout( JobScheduleRequest jobScheduleRequest )
    throws IOException, IllegalAccessException, SchedulerException {
    if ( canAdminister() ) {
      jobScheduleRequest.setActionClass( BlockoutAction.class.getCanonicalName() );
      jobScheduleRequest.getJobParameters()
        .add( getJobScheduleParam( IBlockoutManager.DURATION_PARAM, jobScheduleRequest.getDuration() ) );
      jobScheduleRequest.getJobParameters()
        .add( getJobScheduleParam( IBlockoutManager.TIME_ZONE_PARAM, jobScheduleRequest.getTimeZone() ) );

      return createJob( jobScheduleRequest );
    }

    throw new IllegalAccessException();
  }

  protected JobScheduleParam getJobScheduleParam( String name, String value ) {
    return new JobScheduleParam( name, value );
  }

  protected JobScheduleParam getJobScheduleParam( String name, long value ) {
    return new JobScheduleParam( name, value );
  }

  protected void updateStartDateForTimeZone( JobScheduleRequest jobScheduleRequest ) {
    SchedulerResourceUtil.updateStartDateForTimeZone( jobScheduleRequest );
  }

  @Override
  public IJob updateBlockout( String jobId, JobScheduleRequest jobScheduleRequest )
    throws IllegalAccessException, SchedulerException, IOException {

    if ( canAdminister() ) {
      boolean isJobRemoved = removeJob( jobId );

      if ( isJobRemoved ) {
        return addBlockout( jobScheduleRequest );
      }
    }

    throw new IllegalAccessException();
  }

  @Override
  public BlockStatusProxy getBlockStatus( JobScheduleRequest jobScheduleRequest ) throws SchedulerException {
    updateStartDateForTimeZone( jobScheduleRequest );
    IJobTrigger trigger = convertScheduleRequestToJobTrigger( jobScheduleRequest );
    boolean totallyBlocked = false;
    boolean partiallyBlocked = getBlockoutManager().isPartiallyBlocked( trigger );

    if ( partiallyBlocked ) {
      totallyBlocked = !getBlockoutManager().willFire( trigger );
    }

    return getBlockStatusProxy( totallyBlocked, partiallyBlocked );
  }

  protected BlockStatusProxy getBlockStatusProxy( Boolean totallyBlocked, Boolean partiallyBlocked ) {
    return new BlockStatusProxy( totallyBlocked, partiallyBlocked );
  }

  protected IJobTrigger convertScheduleRequestToJobTrigger( JobScheduleRequest jobScheduleRequest )
    throws SchedulerException {
    return SchedulerResourceUtil.convertScheduleRequestToJobTrigger( jobScheduleRequest, scheduler );
  }

  @Override
  public JobScheduleRequest getJobInfo() {
    JobScheduleRequest jobRequest = new JobScheduleRequest();
    ComplexJobTriggerProxy proxyTrigger = new ComplexJobTriggerProxy();
    proxyTrigger.setDaysOfMonth( new int[] { 1, 2, 3 } );
    proxyTrigger.setDaysOfWeek( new int[] { 1, 2, 3 } );
    proxyTrigger.setMonthsOfYear( new int[] { 1, 2, 3 } );
    proxyTrigger.setYears( new int[] { 2012, 2013 } );
    proxyTrigger.setStartTime( new Date() );
    jobRequest.setComplexJobTrigger( proxyTrigger );
    jobRequest.setInputFile( "aaaaa" );
    jobRequest.setOutputFile( "bbbbb" );
    List<IJobScheduleParam> jobParams = new ArrayList<>();
    jobParams.add( new JobScheduleParam( "param1", "aString" ) );
    jobParams.add( new JobScheduleParam( "param2", 1 ) );
    jobParams.add( new JobScheduleParam( "param3", true ) );
    jobParams.add( new JobScheduleParam( "param4", new Date() ) );
    jobRequest.setJobParameters( jobParams );
    return jobRequest;
  }

  @Override
  public JobState getJobState( JobRequest jobRequest ) throws SchedulerException {
    Job job = (Job) getJob( jobRequest.getJobId() );

    if ( isScheduleAllowed() || getSession().getName().equals( job.getUserName() ) ) {
      return job.getState();
    }

    throw new UnsupportedOperationException();
  }

  protected IPentahoSession getSession() {
    return PentahoSessionHolder.getSession();
  }

  @SuppressWarnings( "unchecked" )
  public Class<IAction> getAction( String actionClass ) throws ClassNotFoundException {
    return ( (Class<IAction>) Class.forName( actionClass ) );
  }

  public IUnifiedRepository getRepository() {
    if ( repository == null ) {
      repository = PentahoSystem.get( IUnifiedRepository.class );
    }

    return repository;
  }

  @Override
  public IScheduler getScheduler() {
    if ( scheduler == null ) {
      scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", null );
    }

    return scheduler;
  }

  public IAuthorizationPolicy getPolicy() {
    if ( policy == null ) {
      policy = PentahoSystem.get( IAuthorizationPolicy.class );
    }

    return policy;
  }

  /**
   * Instantiates {@link SchedulerOutputPathResolver}.
   */
  protected SchedulerOutputPathResolver getSchedulerOutputPathResolver( JobScheduleRequest scheduleRequest ) {
    return new SchedulerOutputPathResolver( scheduleRequest );
  }

  protected boolean isPdiFile( RepositoryFile file ) {
    return SchedulerResourceUtil.isPdiFile( file );
  }

  protected HashMap<String, Serializable> handlePDIScheduling( RepositoryFile file,
                                                               HashMap<String, Serializable> parameterMap,
                                                               Map<String, String> pdiParameters ) {
    return SchedulerResourceUtil.handlePDIScheduling( file, parameterMap, pdiParameters );
  }

  public boolean getAutoCreateUniqueFilename( final JobScheduleRequest scheduleRequest ) {
    List<IJobScheduleParam> jobParameters = scheduleRequest.getJobParameters();

    for ( IJobScheduleParam jobParameter : jobParameters ) {
      if ( IScheduler.RESERVEDMAPKEY_AUTO_CREATE_UNIQUE_FILENAME.equals( jobParameter.getName() ) && "boolean".equals(
        jobParameter.getType() ) ) {
        return (Boolean) jobParameter.getValue();
      }
    }

    return true;
  }

  public String getAppendDateFormat( final JobScheduleRequest scheduleRequest ) {
    List<IJobScheduleParam> jobParameters = scheduleRequest.getJobParameters();

    for ( IJobScheduleParam jobParameter : jobParameters ) {
      if ( IScheduler.RESERVEDMAPKEY_APPEND_DATE_FORMAT.equals( jobParameter.getName() ) && "string".equals(
        jobParameter.getType() ) ) {
        return (String) jobParameter.getValue();
      }
    }

    return null;
  }

  @Override
  public List<IJob> getJobs() throws SchedulerException {
    IPentahoSession session = getSession();
    final String principalName = session.getName(); // this authentication wasn't matching with the job username,
    // changed to get name via the current session
    final boolean canAdminister = canAdminister();

    return getScheduler().getJobs( job -> {
      if ( canAdminister ) {
        return !IBlockoutManager.BLOCK_OUT_JOB_NAME.equals( job.getJobName() );
      }

      return principalName.equals( job.getUserName() );
    } );
  }

  protected boolean canAdminister() {
    return getPolicy().isAllowed( AdministerSecurityAction.NAME );
  }

  protected String resolveActionId( final String inputFile ) {
    return SchedulerResourceUtil.resolveActionId( inputFile );
  }

  protected String getExtension( String filename ) {
    return SchedulerResourceUtil.getExtension( filename );
  }

  /**
   * Gets an instance of SessionResource
   *
   * @return <code>SessionResource</code>
   */
  protected SessionResource getSessionResource() {
    if ( sessionResource == null ) {
      sessionResource = new SessionResource();
    }

    return sessionResource;
  }

  protected FileService getFileService() {
    if ( fileService == null ) {
      fileService = new FileService();
    }

    return fileService;
  }

  protected IBlockoutManager getBlockoutManager() {
    if ( blockoutManager == null ) {
      blockoutManager = PentahoSystem.get( IBlockoutManager.class, "IBlockoutManager", null ); //$NON-NLS-1$;
    }

    return blockoutManager;
  }

  protected ISecurityHelper getSecurityHelper() {
    return SecurityHelper.getInstance();
  }
}
