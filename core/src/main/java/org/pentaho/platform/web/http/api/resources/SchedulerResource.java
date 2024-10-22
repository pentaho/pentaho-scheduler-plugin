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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.enunciate.Facet;
import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.pentaho.platform.api.repository2.unified.UnifiedRepositoryException;
import org.pentaho.platform.api.repository2.unified.webservices.RepositoryFileDto;
import org.pentaho.platform.api.scheduler2.IJob;
import org.pentaho.platform.api.scheduler2.IJobRequest;
import org.pentaho.platform.api.scheduler2.IJobScheduleRequest;
import org.pentaho.platform.api.scheduler2.IJobTrigger;
import org.pentaho.platform.api.scheduler2.ISchedulerResource;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.JobState;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.web.http.api.proxies.BlockStatusProxy;
import org.pentaho.platform.web.http.api.resources.services.ISchedulerServicePlugin;
import org.pentaho.platform.web.http.messages.Messages;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

/**
 * The SchedulerResource service provides the means to create, read, update, delete, and list schedules and blockout
 * periods. Also provides the ability to control the status of schedules and the scheduler.
 */
@SuppressWarnings( { "java:S112", "unchecked" } )
@Path( "/scheduler-plugin/api/scheduler" )
public class SchedulerResource implements ISchedulerResource {
  protected ISchedulerServicePlugin schedulerService;
  protected static final Log logger = LogFactory.getLog( SchedulerResource.class );
  public static final String REMOVED_JOB_STATE = "REMOVED";
  public static final String ERROR_JOB_STATE = "UNKNOWN_ERROR";

  public SchedulerResource() {
    this( PentahoSystem.get( ISchedulerServicePlugin.class, "ISchedulerService2", null ) ); // TODO don't pass in key
  }

  public SchedulerResource( ISchedulerServicePlugin schedulerService ) {
    this.schedulerService = schedulerService;
    logger.info( "-----------------------------------------------------------------------" );
    logger.info( this.getClass().getSimpleName() + " was initialized." );
    logger.info( "-----------------------------------------------------------------------" );
  }

  /**
   * Creates a new scheduled job.
   *
   * <p><b>Example Request:</b><br />
   * POST pentaho/api/scheduler/job
   * </p>
   * <br /><b>POST data:</b>
   * <pre function="syntax.xml">
   *   &lt;jobScheduleRequest&gt;
   *     &lt;jobName&gt;JobName&lt;/jobName&gt;
   *     &lt;simpleJobTrigger&gt;
   *       &lt;uiPassParam&gt;MINUTES&lt;/uiPassParam&gt;
   *       &lt;repeatInterval&gt;1800&lt;/repeatInterval&gt;
   *       &lt;repeatCount&gt;-1&lt;/repeatCount&gt;
   *       &lt;startTime&gt;2014-08-14T11:46:00.000-04:00&lt;/startTime&gt;
   *       &lt;endTime /&gt;
   *     &lt;/simpleJobTrigger&gt;
   *     &lt;inputFile&gt;/public/Steel Wheels/Top Customers (report).prpt&lt;/inputFile&gt;
   *     &lt;outputFile&gt;/public/output&lt;/outputFile&gt;
   *     &lt;jobParameters&gt;
   *       &lt;name&gt;ParameterName&lt;/name&gt;
   *       &lt;type&gt;string&lt;/type&gt;
   *       &lt;stringValue&gt;false&lt;/stringValue&gt;
   *     &lt;/jobParameters&gt;
   *   &lt;/jobScheduleRequest&gt;
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   admin  JobName  1410786491777
   * </pre>
   *
   * @param scheduleRequest A JobScheduleRequest object to define the parameters of the job being created.
   * @return A jax-rs Response object with the created jobId.
   */
  @POST
  @Path( "/job" )
  @Consumes( { APPLICATION_JSON, APPLICATION_XML } )
  @Produces( "text/plain" )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Schedule created successfully." ),
    @ResponseCode( code = 401, condition = "User is not allowed to create schedules." ),
    @ResponseCode( code = 403, condition = "Cannot create schedules for the specified file." ),
    @ResponseCode( code = 500, condition = "An error occurred while creating a schedule." )
  } )
  public Response createJob( JobScheduleRequest scheduleRequest ) {
    try {
      return buildPlainTextOkResponse( schedulerService.createJob( scheduleRequest ).getJobId() );
    } catch ( SchedulerException | IOException e ) {
      return buildServerErrorResponse( getErrorMessage( e ) );
    } catch ( SecurityException e ) {
      return buildStatusResponse( UNAUTHORIZED );
    } catch ( IllegalAccessException e ) {
      return buildStatusResponse( FORBIDDEN );
    }
  }

  public Response createJob( IJobScheduleRequest scheduleRequest ) {
    try {
      return buildPlainTextOkResponse( schedulerService.createJob( (JobScheduleRequest) scheduleRequest ).getJobId() );
    } catch ( SchedulerException | IOException e ) {
      return buildServerErrorResponse( getErrorMessage( e ) );
    } catch ( SecurityException e ) {
      return buildStatusResponse( UNAUTHORIZED );
    } catch ( IllegalAccessException e ) {
      return buildStatusResponse( FORBIDDEN );
    }
  }

  /**
   * Changes an existing job by creating an instance with new content (picked from {@code scheduleRequest}) and
   * removing the current instance.
   *
   * <p><b>Example Request:</b><br />
   * POST pentaho/api/scheduler/job/update
   * </p>
   * <br /><b>POST data:</b>
   * <pre function="syntax.xml">
   *   &lt;jobScheduleRequest&gt;
   *     &lt;jobName&gt;JobName&lt;/jobName&gt;
   *     &lt;jobId&gt;admin  JobName 1410786491777&lt;/jobId&gt;
   *     &lt;simpleJobTrigger&gt;
   *       &lt;uiPassParam&gt;MINUTES&lt;/uiPassParam&gt;
   *       &lt;repeatInterval&gt;1800&lt;/repeatInterval&gt;
   *       &lt;repeatCount&gt;-1&lt;/repeatCount&gt;
   *       &lt;startTime&gt;2014-08-14T11:46:00.000-04:00&lt;/startTime&gt;
   *       &lt;endTime /&gt;
   *     &lt;/simpleJobTrigger&gt;
   *     &lt;inputFile&gt;/public/Steel Wheels/Top Customers (report).prpt&lt;/inputFile&gt;
   *     &lt;outputFile&gt;/public/output&lt;/outputFile&gt;
   *     &lt;jobParameters&gt;
   *       &lt;name&gt;ParameterName&lt;/name&gt;
   *       &lt;type&gt;string&lt;/type&gt;
   *       &lt;stringValue&gt;false&lt;/stringValue&gt;
   *     &lt;/jobParameters&gt;
   *   &lt;/jobScheduleRequest&gt;
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   admin JobName 1410786491777
   * </pre>
   *
   * @param scheduleRequest A JobScheduleRequest object to define the parameters of the job being updated.
   * @return A jax-rs Response object with the created jobId.
   */
  @POST
  @Path( "/job/update" )
  @Consumes( { APPLICATION_JSON, APPLICATION_XML } )
  @Produces( "text/plain" )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Schedule updated successfully." ),
    @ResponseCode( code = 401, condition = "User is not allowed to update schedules." ),
    @ResponseCode( code = 403, condition = "Cannot update schedules for the specified file." ),
    @ResponseCode( code = 500, condition = "An error occurred while updating a schedule." )
  } )
  public Response updateJob( JobScheduleRequest scheduleRequest ) {
    try {
      return buildPlainTextOkResponse( schedulerService.updateJob( scheduleRequest ).getJobId() );
    } catch ( SchedulerException | IOException e ) {
      return buildServerErrorResponse( getErrorMessage( e ) );
    } catch ( SecurityException e ) {
      return buildStatusResponse( UNAUTHORIZED );
    } catch ( IllegalAccessException e ) {
      return buildStatusResponse( FORBIDDEN );
    }
  }

  /**
   * Execute a previously scheduled job.
   *
   * <p><b>Example Request:</b><br />
   * POST pentaho/api/scheduler/triggerNow
   * </p>
   * <br /><b>POST data:</b>
   * <pre function="syntax.xml">
   *   &lt;jobRequest&gt;
   *     &lt;jobId&gt;admin  JobName 1410786491777&lt;/jobId&gt;
   *   &lt;/jobRequest&gt;
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   NORMAL
   * </pre>
   *
   * @param jobRequest A JobRequest object containing the jobId.
   * @return A Response object indicating the status of the scheduler.
   */
  @POST
  @Path( "/triggerNow" )
  @Produces( "text/plain" )
  @Consumes( { APPLICATION_XML, APPLICATION_JSON } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Job triggered successfully." ),
    @ResponseCode( code = 400, condition = "Invalid input." ),
    @ResponseCode( code = 500, condition = "Invalid jobId." )
  } )
  public Response triggerNow( JobRequest jobRequest ) {
    try {
      return buildPlainTextOkResponse( schedulerService.triggerNow( jobRequest.getJobId() ).getState().name() );
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  /**
   * Get the scheduled job created by the system for deleting generated files.
   *
   * <p><b>Example Request:</b><br />
   * GET pentaho/api/scheduler/getContentCleanerJob
   * </p>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   &lt;job&gt;
   *     &lt;groupName&gt;admin&lt;/groupName&gt;
   *     &lt;jobId&gt;admin  GeneratedContentCleaner 1408377444383&lt;/jobId&gt;
   *     &lt;jobName&gt;GeneratedContentCleaner&lt;/jobName&gt;
   *     &lt;jobParams&gt;
   *       &lt;jobParams&gt;
   *         &lt;name&gt;uiPassParam&lt;/name&gt;
   *         &lt;value&gt;DAILY&lt;/value&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobParams&gt;
   *         &lt;name&gt;age&lt;/name&gt;
   *         &lt;value&gt;15552000&lt;/value&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobParams&gt;
   *         &lt;name&gt;user_locale&lt;/name&gt;
   *         &lt;value&gt;en_US&lt;/value&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobParams&gt;
   *         &lt;name&gt;ActionAdapterQuartzJob-ActionUser&lt;/name&gt;
   *         &lt;value&gt;admin&lt;/value&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobParams&gt;
   *         &lt;name&gt;ActionAdapterQuartzJob-ActionClass&lt;/name&gt;
   *         &lt;value&gt;org.pentaho.platform.admin.GeneratedContentCleaner&lt;/value&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobParams&gt;
   *         &lt;name&gt;lineage-id&lt;/name&gt;
   *         &lt;value&gt;c3cfbad4-2e34-4dbd-8071-a2f3c7e8fab9&lt;/value&gt;
   *       &lt;/jobParams&gt;
   *     &lt;/jobParams&gt;
   *     &lt;jobTrigger xsi:type="simpleJobTrigger"&gt;
   *       &lt;duration&gt;-1&lt;/duration&gt;
   *       &lt;startTime&gt;2014-08-18T11:57:00-04:00&lt;/startTime&gt;
   *       &lt;uiPassParam&gt;DAILY&lt;/uiPassParam&gt;
   *       &lt;repeatCount&gt;-1&lt;/repeatCount&gt;
   *       &lt;repeatInterval&gt;86400&lt;/repeatInterval&gt;
   *     &lt;/jobTrigger&gt;
   *     &lt;lastRun&gt;2014-08-18T11:57:00-04:00&lt;/lastRun&gt;
   *     &lt;nextRun&gt;2014-08-19T11:57:00-04:00&lt;/nextRun&gt;
   *     &lt;state&gt;NORMAL&lt;/state&gt;
   *     &lt;userName&gt;admin&lt;/userName&gt;
   *   &lt;/job&gt;
   * </pre>
   *
   * @return A Job object containing the definition of the content cleaner job.
   */
  @GET
  @Path( "/getContentCleanerJob" )
  @Produces( { APPLICATION_JSON, APPLICATION_XML } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Content cleaner job successfully retrieved." ),
    @ResponseCode( code = 204, condition = "No content cleaner job exists." ),
  } )
  public IJob getContentCleanerJob() {
    try {
      return schedulerService.getContentCleanerJob();
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  /**
   * Retrieve the all the job(s) visible to the current users.
   *
   * <p><b>Example Request:</b><br />
   * GET pentaho/api/scheduler/jobs
   * </p>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   &lt;jobs&gt;
   *     &lt;job&gt;
   *       &lt;groupName&gt;admin&lt;/groupName&gt;
   *       &lt;jobId&gt;admin  PentahoSystemVersionCheck 1408369303507&lt;/jobId&gt;
   *       &lt;jobName&gt;PentahoSystemVersionCheck&lt;/jobName&gt;
   *       &lt;jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;ActionAdapterQuartzJob-ActionUser&lt;/name&gt;
   *           &lt;value&gt;admin&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;ActionAdapterQuartzJob-ActionClass&lt;/name&gt;
   *           &lt;value&gt;org.pentaho.platform.scheduler2.versionchecker.VersionCheckerAction&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;lineage-id&lt;/name&gt;
   *           &lt;value&gt;1986cc90-cf87-43f6-8924-9d6e443e7d5d&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;versionRequestFlags&lt;/name&gt;
   *           &lt;value&gt;0&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobTrigger xsi:type="simpleJobTrigger"&gt;
   *         &lt;duration&gt;-1&lt;/duration&gt;
   *         &lt;startTime&gt;2014-08-18T09:41:43.506-04:00&lt;/startTime&gt;
   *         &lt;repeatCount&gt;-1&lt;/repeatCount&gt;
   *         &lt;repeatInterval&gt;86400&lt;/repeatInterval&gt;
   *       &lt;/jobTrigger&gt;
   *       &lt;lastRun&gt;2014-08-18T11:37:31.412-04:00&lt;/lastRun&gt;
   *       &lt;nextRun&gt;2014-08-19T09:41:43.506-04:00&lt;/nextRun&gt;
   *       &lt;state&gt;NORMAL&lt;/state&gt;
   *       &lt;userName&gt;admin&lt;/userName&gt;
   *     &lt;/job&gt;
   *     &lt;job&gt;
   *       &lt;groupName&gt;admin&lt;/groupName&gt;
   *       &lt;jobId&gt;admin  UpdateAuditData 1408373019115&lt;/jobId&gt;
   *       &lt;jobName&gt;UpdateAuditData&lt;/jobName&gt;
   *       &lt;jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;autoCreateUniqueFilename&lt;/name&gt;
   *           &lt;value&gt;false&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;uiPassParam&lt;/name&gt;
   *           &lt;value&gt;MINUTES&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;ActionAdapterQuartzJob-StreamProvider&lt;/name&gt;
   *           &lt;value&gt;input file = /public/pentaho-operations-mart/update_audit_mart_data/UpdateAuditData.xaction:outputFile = /public/pentaho-operations-mart/generated_logs/UpdateAuditData.*&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;user_locale&lt;/name&gt;
   *           &lt;value&gt;en_US&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;ActionAdapterQuartzJob-ActionUser&lt;/name&gt;
   *           &lt;value&gt;admin&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;ActionAdapterQuartzJob-ActionId&lt;/name&gt;
   *           &lt;value&gt;xaction.backgroundExecution&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;lineage-id&lt;/name&gt;
   *           &lt;value&gt;1f2402c4-0a70-40e4-b428-0d328f504cb3&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobTrigger xsi:type="simpleJobTrigger"&gt;
   *         &lt;duration&gt;-1&lt;/duration&gt;
   *         &lt;startTime&gt;2014-07-14T12:47:00-04:00&lt;/startTime&gt;
   *         &lt;uiPassParam&gt;MINUTES&lt;/uiPassParam&gt;
   *         &lt;repeatCount&gt;-1&lt;/repeatCount&gt;
   *         &lt;repeatInterval&gt;1800&lt;/repeatInterval&gt;
   *       &lt;/jobTrigger&gt;
   *       &lt;lastRun&gt;2014-08-18T12:47:00-04:00&lt;/lastRun&gt;
   *       &lt;nextRun&gt;2014-08-18T13:17:00-04:00&lt;/nextRun&gt;
   *       &lt;state&gt;NORMAL&lt;/state&gt;
   *       &lt;userName&gt;admin&lt;/userName&gt;
   *     &lt;/job&gt;
   *   &lt;/jobs&gt;
   * </pre>
   *
   * @param asCronString Cron string (Unused).
   * @return A list of jobs that are visible to the current users.
   * @deprecated use "GET pentaho/api/scheduler/getJobs" instead.
   */
  @Deprecated
  @GET
  @Path( "/jobs" )
  @Produces( { APPLICATION_JSON, APPLICATION_XML } )
  @Facet( name = "Unsupported" )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Jobs retrieved successfully." ),
    @ResponseCode( code = 500, condition = "Error while retrieving jobs." )
  } )
  public List<Job> getJobs( @DefaultValue( "false" ) @QueryParam( "asCronString" ) Boolean asCronString ) {
    try {
      return (List<Job>) (List<?>) schedulerService.getJobs();
    } catch ( Exception e ) {
      throw new RuntimeException( e );
    }
  }

  /**
   * Retrieve the all the scheduled job(s) visible to the current users.
   *
   * <p><b>Example Request:</b><br />
   * GET pentaho/api/scheduler/getJobs
   * </p>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   &lt;jobs&gt;
   *     &lt;job&gt;
   *       &lt;groupName&gt;admin&lt;/groupName&gt;
   *       &lt;jobId&gt;admin  PentahoSystemVersionCheck 1408369303507&lt;/jobId&gt;
   *       &lt;jobName&gt;PentahoSystemVersionCheck&lt;/jobName&gt;
   *       &lt;jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;ActionAdapterQuartzJob-ActionUser&lt;/name&gt;
   *           &lt;value&gt;admin&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;ActionAdapterQuartzJob-ActionClass&lt;/name&gt;
   *           &lt;value&gt;org.pentaho.platform.scheduler2.versionchecker.VersionCheckerAction&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;lineage-id&lt;/name&gt;
   *           &lt;value&gt;1986cc90-cf87-43f6-8924-9d6e443e7d5d&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;versionRequestFlags&lt;/name&gt;
   *           &lt;value&gt;0&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobTrigger xsi:type="simpleJobTrigger"&gt;
   *         &lt;duration&gt;-1&lt;/duration&gt;
   *         &lt;startTime&gt;2014-08-18T09:41:43.506-04:00&lt;/startTime&gt;
   *         &lt;repeatCount&gt;-1&lt;/repeatCount&gt;
   *         &lt;repeatInterval&gt;86400&lt;/repeatInterval&gt;
   *       &lt;/jobTrigger&gt;
   *       &lt;lastRun&gt;2014-08-18T11:37:31.412-04:00&lt;/lastRun&gt;
   *       &lt;nextRun&gt;2014-08-19T09:41:43.506-04:00&lt;/nextRun&gt;
   *       &lt;state&gt;NORMAL&lt;/state&gt;
   *       &lt;userName&gt;admin&lt;/userName&gt;
   *     &lt;/job&gt;
   *     &lt;job&gt;
   *       &lt;groupName&gt;admin&lt;/groupName&gt;
   *       &lt;jobId&gt;admin UpdateAuditData 1408373019115&lt;/jobId&gt;
   *       &lt;jobName&gt;UpdateAuditData&lt;/jobName&gt;
   *       &lt;jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;autoCreateUniqueFilename&lt;/name&gt;
   *           &lt;value&gt;false&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;uiPassParam&lt;/name&gt;
   *           &lt;value&gt;MINUTES&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;ActionAdapterQuartzJob-StreamProvider&lt;/name&gt;
   *           &lt;value&gt;input file = /public/pentaho-operations-mart/update_audit_mart_data/UpdateAuditData.xaction:outputFile = /public/pentaho-operations-mart/generated_logs/UpdateAuditData.*&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;user_locale&lt;/name&gt;
   *           &lt;value&gt;en_US&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;ActionAdapterQuartzJob-ActionUser&lt;/name&gt;
   *           &lt;value&gt;admin&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;ActionAdapterQuartzJob-ActionId&lt;/name&gt;
   *           &lt;value&gt;xaction.backgroundExecution&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;lineage-id&lt;/name&gt;
   *           &lt;value&gt;1f2402c4-0a70-40e4-b428-0d328f504cb3&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobTrigger xsi:type="simpleJobTrigger"&gt;
   *         &lt;duration&gt;-1&lt;/duration&gt;
   *         &lt;startTime&gt;2014-07-14T12:47:00-04:00&lt;/startTime&gt;
   *         &lt;uiPassParam&gt;MINUTES&lt;/uiPassParam&gt;
   *         &lt;repeatCount&gt;-1&lt;/repeatCount&gt;
   *         &lt;repeatInterval&gt;1800&lt;/repeatInterval&gt;
   *       &lt;/jobTrigger&gt;
   *       &lt;lastRun&gt;2014-08-18T12:47:00-04:00&lt;/lastRun&gt;
   *       &lt;nextRun&gt;2014-08-18T13:17:00-04:00&lt;/nextRun&gt;
   *       &lt;state&gt;NORMAL&lt;/state&gt;
   *       &lt;userName&gt;admin&lt;/userName&gt;
   *     &lt;/job&gt;
   *   &lt;/jobs&gt;
   * </pre>
   *
   * @return A list of jobs that are visible to the current users.
   */
  @GET
  @Path( "/getJobs" )
  @Produces( { APPLICATION_JSON, APPLICATION_XML } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Jobs retrieved successfully." ),
    @ResponseCode( code = 500, condition = "Error while retrieving jobs." ),
  } )
  public List<Job> getAllJobs() {
    try {
      return (List<Job>) (List<?>) schedulerService.getJobs();
    } catch ( Exception e ) {
      throw new RuntimeException( e );
    }
  }

  public List<IJob> getJobsList() {
    try {
      return schedulerService.getJobs();
    } catch ( Exception e ) {
      throw new RuntimeException( e );
    }
  }

  /**
   * Checks whether the current user may schedule a repository file in the platform.
   *
   * <p><b>Example Request:</b><br />
   * GET pentaho/api/scheduler/isScheduleAllowed?id=b5f806b9-9f72-4814-b1e0-aa9e0ece7e1a
   * </p>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   true
   * </pre>
   *
   * @param id The repository file ID of the content to checked.
   * @return true or false. true indicates scheduling is allowed and false indicates scheduling is not allowed for
   * the file.
   */
  @GET
  @Path( "/isScheduleAllowed" )
  @Produces( TEXT_PLAIN )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully retrieved scheduling ability of repository file." ),
    @ResponseCode( code = 500, condition = "Invalid repository file id." ),
  } )
  public String isScheduleAllowed( @QueryParam( "id" ) String id ) {
    return "" + schedulerService.isScheduleAllowed( id );
  }

  /**
   * Checks whether the current user has authority to schedule any content in the platform.
   *
   * <p><b>Example Request:</b><br />
   * GET pentaho/api/scheduler/canSchedule
   * </p>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   true
   * </pre>
   *
   * @return true or false. true indicates scheduling is allowed and false indicates scheduling is not allowed for
   * the user.
   */
  @GET
  @Path( "/canSchedule" )
  @Produces( APPLICATION_JSON )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully retrieved the scheduling permission." ),
    @ResponseCode( code = 500, condition = "Unable to retrieve the scheduling permission." )
  } )
  public String doGetCanSchedule() {
    return schedulerService.doGetCanSchedule();
  }

  /**
   * Checks whether the current user has authority to execute schedules in the platform.
   *
   * <p><b>Example Request:</b><br />
   * GET pentaho/api/scheduler/canExecuteSchedules
   * </p>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   true
   * </pre>
   *
   * @return true or false. true indicates schedule execution is allowed and false indicates schedule execution is
   * not allowed for the user.
   */
  @GET
  @Path( "/canExecuteSchedules" )
  @Produces( APPLICATION_JSON )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully retrieved the scheduling permission." ),
    @ResponseCode( code = 500, condition = "Unable to retrieve the scheduling permission." )
  } )
  public String doGetCanExecuteSchedules() {
    return schedulerService.doGetCanExecuteSchedule();
  }

  /**
   * Returns the state of the scheduler with the value of RUNNING or PAUSED.
   *
   * <p><b>Example Request:</b><br />
   * GET pentaho/api/scheduler/state
   * </p>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   RUNNING
   * </pre>
   *
   * @return status of the scheduler as RUNNING or PAUSED.
   */
  @GET
  @Path( "/state" )
  @Produces( "text/plain" )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully retrieved the state of the scheduler." ),
    @ResponseCode( code = 500, condition = "An error occurred when getting the state of the scheduler." )
  } )
  public Response getState() {
    try {
      String state = schedulerService.getState();
      return buildPlainTextOkResponse( state );
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  /**
   * Resume the scheduler from a paused state.
   *
   * <p><b>Example Request:</b><br />
   * POST pentaho/api/scheduler/start
   * </p>
   * <br /><b>POST data:</b>
   * <pre function="syntax.xml">
   *   This POST body does not contain data.
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   RUNNING
   * </pre>
   *
   * @return A jax-rs Response object containing the status of the scheduler.
   */
  @POST
  @Path( "/start" )
  @Produces( "text/plain" )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully started the server." ),
    @ResponseCode( code = 500, condition = "An error occurred when resuming the scheduler." )
  } )
  public Response start() {
    try {
      String status = schedulerService.start();
      return buildPlainTextOkResponse( status );
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  /**
   * Pause the scheduler from a running state.
   *
   * <p><b>Example Request:</b><br />
   * POST pentaho/api/scheduler/pause
   * </p>
   * <br /><b>POST data:</b>
   * <pre function="syntax.xml">
   *   This POST body does not contain data.
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   PAUSED
   * </pre>
   *
   * @return A jax-rs Response object containing the status of the scheduler.
   */
  @POST
  @Path( "/pause" )
  @Produces( "text/plain" )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully paused the server." ),
    @ResponseCode( code = 500, condition = "An error occurred when pausing the scheduler." )
  } )
  public Response pause() {
    try {
      return buildPlainTextOkResponse( schedulerService.pause() );
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  /**
   * Shuts down the scheduler.
   *
   * <p><b>Example Request:</b><br />
   * POST pentaho/api/scheduler/shutdown
   * </p>
   * <br /><b>POST data:</b>
   * <pre function="syntax.xml">
   *   This POST body does not contain data.
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   PAUSED
   * </pre>
   *
   * @return A jax-rs Response object containing the status of the scheduler.
   */
  @POST
  @Path( "/shutdown" )
  @Produces( "text/plain" )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully shut down the server." ),
    @ResponseCode( code = 500, condition = "An error occurred when shutting down the scheduler." )
  } )
  public Response shutdown() {
    try {
      return buildPlainTextOkResponse( schedulerService.shutdown() );
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  /**
   * Checks the state of the selected scheduled job.
   *
   * <p><b>Example Request:</b><br />
   * POST pentaho/api/scheduler/jobState
   * </p>
   * <br /><b>POST data:</b>
   * <pre function="syntax.xml">
   *   &lt;jobRequest&gt;
   *     &lt;jobId&gt;admin  JobName 1410786491777&lt;/jobId&gt;
   *   &lt;/jobRequest&gt;
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   NORMAL
   * </pre>
   *
   * @param jobRequest A JobRequest object containing the jobId.
   * @return A jax-rs Response object containing the status of the scheduled job.
   */
  @POST
  @Path( "/jobState" )
  @Produces( "text/plain" )
  @Consumes( { APPLICATION_XML, APPLICATION_JSON } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully retrieved the state of the requested job." ),
    @ResponseCode( code = 500, condition = "Invalid jobId." )
  } )
  public Response getJobState( JobRequest jobRequest ) {
    try {
      return buildPlainTextOkResponse( schedulerService.getJobState( jobRequest ).name() );
    } catch ( UnsupportedOperationException e ) {
      return buildPlainTextStatusResponse( UNAUTHORIZED );
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  /**
   * Pause the specified scheduled job.
   *
   * <p><b>Example Request:</b><br />
   * POST pentaho/api/scheduler/pauseJob
   * </p>
   * <br /><b>POST data:</b>
   * <pre function="syntax.xml">
   *   &lt;jobRequest&gt;
   *     &lt;jobId&gt;admin  JobName 1410786491777&lt;/jobId&gt;
   *   &lt;/jobRequest&gt;
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   PAUSED
   * </pre>
   *
   * @param jobRequest A JobRequest object containing the jobId.
   * @return A jax-rs Response object containing the status of the scheduled job.
   */
  @POST
  @Path( "/pauseJob" )
  @Produces( "text/plain" )
  @Consumes( { APPLICATION_XML, APPLICATION_JSON } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully paused the job." ),
    @ResponseCode( code = 500, condition = "Invalid jobId." )
  } )
  public Response pauseJob( JobRequest jobRequest ) {
    try {
      JobState state = schedulerService.pauseJob( jobRequest.getJobId() );
      return buildPlainTextOkResponse( state.name() );
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  public void pauseJob( IJobRequest jobRequest ) {
    try {
      schedulerService.pauseJob( jobRequest.getJobId() );
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  /**
   * Resume the specified scheduled job.
   *
   * <p><b>Example Request:</b><br />
   * POST pentaho/api/scheduler/resumeJob
   * </p>
   * <br /><b>POST data:</b>
   * <pre function="syntax.xml">
   *   &lt;jobRequest&gt;
   *     &lt;jobId&gt;admin  JobName 1410786491777&lt;/jobId&gt;
   *   &lt;/jobRequest&gt;
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   NORMAL
   * </pre>
   *
   * @param jobRequest A JobRequest object containing the jobId.
   * @return A jax-rs Response object containing the status of the scheduled job.
   */
  @POST
  @Path( "/resumeJob" )
  @Produces( "text/plain" )
  @Consumes( { APPLICATION_XML, APPLICATION_JSON } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully resumed the job." ),
    @ResponseCode( code = 500, condition = "Invalid jobId." )
  } )
  public Response resumeJob( JobRequest jobRequest ) {
    try {
      JobState state = schedulerService.resumeJob( jobRequest.getJobId() );
      return buildPlainTextOkResponse( state.name() );
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  /**
   * Delete the specified scheduled job from the platform.
   *
   * <p><b>Example Request:</b><br />
   * DELETE pentaho/api/scheduler/removeJob
   * </p>
   * <br /><b>DELETE data:</b>
   * <pre function="syntax.xml">
   *   &lt;jobRequest&gt;
   *     &lt;jobId&gt;admin  BlockoutAction 1410786491503&lt;/jobId&gt;
   *   &lt;/jobRequest&gt;
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   REMOVED
   * </pre>
   *
   * @param jobRequest A JobRequest object containing the jobId.
   * @return A jax-rs Response object containing the status of the scheduled job.
   * @deprecated use "PUT pentaho/api/scheduler/removeJob" instead.
   */
  @Deprecated
  @DELETE
  @Path( "/removeJob" )
  @Produces( "text/plain" )
  @Consumes( { APPLICATION_XML, APPLICATION_JSON } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully removed the job." ),
    @ResponseCode( code = 500, condition = "Invalid jobId." )
  } )
  public Response removeJob( JobRequest jobRequest ) {
    return deleteJob( jobRequest );
  }

  public void removeJob( IJobRequest jobRequest ) {
    deleteJob( (JobRequest) jobRequest );
  }

  /**
   * Delete the specified scheduled job from the platform.
   *
   * <p><b>Example Request:</b><br />
   * PUT pentaho/api/scheduler/removeJob
   * </p>
   * <br /><b>PUT data:</b>
   * <pre function="syntax.xml">
   *   &lt;jobRequest&gt;
   *     &lt;jobId&gt;admin  BlockoutAction 1410786491503&lt;/jobId&gt;
   *   &lt;/jobRequest&gt;
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   REMOVED
   * </pre>
   *
   * @param jobRequest A JobRequest object containing the jobId.
   * @return A jax-rs Response object containing the status of the scheduled job.
   */
  @PUT
  @Path( "/removeJob" )
  @Produces( "text/plain" )
  @Consumes( { APPLICATION_XML, APPLICATION_JSON } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully removed the job." ),
    @ResponseCode( code = 500, condition = "Invalid jobId." )
  } )
  public Response deleteJob( JobRequest jobRequest ) {
    try {
      return buildPlainTextOkResponse( deleteJob( jobRequest.getJobId() ) );
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  private String deleteJob( String jobId ) throws SchedulerException {
    if ( schedulerService.removeJob( jobId ) ) {
      return REMOVED_JOB_STATE;
    }

    return schedulerService.getJob( jobId ).getState().name();
  }

  /**
   * Deletes all the specified scheduled jobs from the platform.
   *
   * <p><b>Example Request:</b><br />
   * POST pentaho/api/scheduler/removeJobs
   * </p>
   * <br /><b>POST data:</b>
   * <pre function="syntax.json">
   *   &lt;jobsRequest&gt;
   *     &lt;jobIds&gt;
   *       &lt;jobId&gt;admin  BlockoutAction 1410786491503&lt;/jobId&gt;
   *       &lt;jobId&gt;admin  BlockoutAction 1410786491503&lt;/jobId&gt;
   *     &lt;/jobIds&gt;
   *   &lt;/jobsRequest&gt;
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   &lt;JobsResponse&gt;
   *     &lt;changes&gt;
   *       &lt;entry&gt;
   *         &lt;key&gt;admin  BlockoutAction 1410786491503&lt;/key&gt;
   *         &lt;value&gt;REMOVED&lt;/value&gt;
   *       &lt;/entry&gt;
   *       &lt;entry&gt;
   *         &lt;key&gt;admin  BlockoutAction 1410786491503&lt;/key&gt;
   *         &lt;value&gt;ERROR&lt;/value&gt;
   *       &lt;/entry&gt;
   *     &lt;/changes&gt;
   *   &lt;/JobsResponse&gt;
   * </pre>
   *
   * @param jobsRequest A JobsRequest object containing a list of jobIds.
   * @return A jax-rs Response object containing all the scheduled jobs ids and their new status.
   */
  @POST
  @Path( "/removeJobs" )
  @Produces( { APPLICATION_XML, APPLICATION_JSON } )
  @Consumes( { APPLICATION_XML, APPLICATION_JSON } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully returned the all the jobs new states." ),
    @ResponseCode( code = 500, condition = "Invalid request or server error." )
  } )
  public JobsResponse removeJobs( JobsRequest jobsRequest ) {
    try {
      return removeJobs( jobsRequest.getJobIds() );
    } catch ( Exception e ) {
      throw new RuntimeException( e );
    }
  }

  private JobsResponse removeJobs( List<String> jobIds ) {
    JobsResponse response = new JobsResponse();

    for ( String jobId : jobIds ) {
      String newState;

      try {
        newState = deleteJob( jobId );
      } catch ( Exception e ) {
        newState = ERROR_JOB_STATE;
      }

      response.addChanges( jobId, newState );
    }

    return response;
  }

  /**
   * Return the information for a specified job.
   *
   * <p><b>Example Request:</b><br />
   * GET pentaho/api/scheduler/jobinfo?jobId=admin%09JobName%091410786491777
   * </p>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   &lt;job&gt;
   *     &lt;jobId&gt;admin JobName 1410786491777&lt;/jobId&gt;
   *     &lt;jobName&gt;JobName&lt;/jobName&gt;
   *     &lt;jobParams&gt;
   *       &lt;jobParams&gt;
   *         &lt;name&gt;uiPassParam&lt;/name&gt;
   *         &lt;value&gt;MINUTES&lt;/value&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobParams&gt;
   *         &lt;name&gt;ActionAdapterQuartzJob-StreamProvider&lt;/name&gt;
   *         &lt;value&gt;input file = /public/Steel Wheels/Top Customers (report).prpt:outputFile = /home/admin/JobName.*&lt;/value&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobParams&gt;
   *         &lt;name&gt;user_locale&lt;/name&gt;
   *         &lt;value&gt;en_US&lt;/value&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobParams&gt;
   *         &lt;name&gt;ActionAdapterQuartzJob-ActionUser&lt;/name&gt;
   *         &lt;value&gt;admin&lt;/value&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobParams&gt;
   *         &lt;name&gt;ActionAdapterQuartzJob-ActionId&lt;/name&gt;
   *         &lt;value&gt;prpt.backgroundExecution&lt;/value&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobParams&gt;
   *         &lt;name&gt;ParameterName&lt;/name&gt;
   *         &lt;value&gt;false&lt;/value&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobParams&gt;
   *         &lt;name&gt;lineage-id&lt;/name&gt;
   *         &lt;value&gt;5212a120-3294-49e8-9c5d-c755b9766c43&lt;/value&gt;
   *       &lt;/jobParams&gt;
   *     &lt;/jobParams&gt;
   *     &lt;jobTrigger xsi:type=&quot;simpleJobTrigger&quot;&gt;
   *       &lt;duration&gt;-1&lt;/duration&gt;
   *       &lt;startTime&gt;2014-08-14T11:46:00-04:00&lt;/startTime&gt;
   *       &lt;uiPassParam&gt;MINUTES&lt;/uiPassParam&gt;
   *       &lt;repeatCount&gt;-1&lt;/repeatCount&gt;
   *       &lt;repeatInterval&gt;1800&lt;/repeatInterval&gt;
   *     &lt;/jobTrigger&gt;
   *     &lt;nextRun&gt;2014-08-14T11:46:00-04:00&lt;/nextRun&gt;
   *     &lt;state&gt;NORMAL&lt;/state&gt;
   *     &lt;userName&gt;admin&lt;/userName&gt;
   *   &lt;/job&gt;
   * </pre>
   *
   * @param jobId        The jobId of the job for which we are requesting information.
   * @param asCronString Cron string (Unused).
   * @return A Job object containing the info for the specified job.
   */
  @GET
  @Path( "/jobinfo" )
  @Produces( { APPLICATION_JSON, APPLICATION_XML } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully retrieved the information for the requested job." ),
    @ResponseCode( code = 204, condition = "jobId is valid, but the job is either finished or does not exists." ),
    @ResponseCode( code = 500, condition = "Internal error or invalid jobId." )
  } )
  public Response getJob( @QueryParam( "jobId" ) String jobId,
                          @DefaultValue( "false" ) @QueryParam( "asCronString" ) String asCronString ) {
    try {
      if ( schedulerService.getJobInfo( jobId ) == null ) {
        return buildStatusResponse( Status.NO_CONTENT );
      }
      return buildOkResponse( schedulerService.getJobInfo( jobId ) );
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  /**
   * Return a test information for a job.
   *
   * @return A JobScheduleRequest object with all parameters of the test job.
   * @deprecated used for test only.
   */
  @Deprecated
  @GET
  @Path( "/jobinfotest" )
  @Produces( { APPLICATION_JSON } )
  @Facet( name = "Unsupported" )
  public JobScheduleRequest getJobInfo() {
    return schedulerService.getJobInfo();
  }

  /**
   * Retrieves all blockout jobs in the system.
   *
   * @return list of Job.
   * @deprecated Method is deprecated as the name getBlockoutJobs is preferred over getJobs.
   */
  @Deprecated
  @Facet( name = "Unsupported" )
  public List<Job> getJobs() {
    try {
      return (List<Job>) (List<?>) schedulerService.getBlockOutJobs();
    } catch ( Exception e ) {
      throw new RuntimeException( e );
    }
  }

  @GET
  @Path( "/hideInternalVariable" )
  @Produces( { MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML } )
  @StatusCodes( {
          @ResponseCode( code = 200, condition = "Operation successful" ),
          @ResponseCode( code = 403, condition = "Access forbidden" ),
          @ResponseCode( code = 500, condition = "Retrieving hide internal varible failed" )
  } )
  public Response retrieveHideInternalVariable() {
    try {
      String pSchedulerHideInternalVar = schedulerService.getHideInternalVariable();
      return buildStatusResponse( Response.Status.OK, pSchedulerHideInternalVar );
    } catch ( Exception e ) {
      return buildServerErrorResponse( e.getMessage() );
    }
  }



  /**
   * Get all the blockout jobs in the system.
   *
   * <p><b>Example Request:</b><br />
   * GET pentaho/api/scheduler/blockout/blockoutjobs
   * </p>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   &lt;jobs&gt;
   *     &lt;job&gt;
   *       &lt;groupName&gt;admin&lt;/groupName&gt;
   *       &lt;jobId&gt;admin  BlockoutAction  1408457558636&lt;/jobId&gt;
   *       &lt;jobName&gt;BlockoutAction&lt;/jobName&gt;
   *       &lt;jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;TIME_ZONE_PARAM&lt;/name&gt;
   *           &lt;value&gt;America/New_York&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;DURATION_PARAM&lt;/name&gt;
   *           &lt;value&gt;10080000&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;uiPassParam&lt;/name&gt;
   *           &lt;value&gt;DAILY&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;user_locale&lt;/name&gt;
   *           &lt;value&gt;en_US&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;ActionAdapterQuartzJob-ActionUser&lt;/name&gt;
   *           &lt;value&gt;admin&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;ActionAdapterQuartzJob-ActionClass&lt;/name&gt;
   *           &lt;value&gt;org.pentaho.platform.scheduler2.blockout.BlockoutAction&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *         &lt;jobParams&gt;
   *           &lt;name&gt;lineage-id&lt;/name&gt;
   *           &lt;value&gt;0989726c-3247-4864-bc79-8e2a1dc60c58&lt;/value&gt;
   *         &lt;/jobParams&gt;
   *       &lt;/jobParams&gt;
   *       &lt;jobTrigger xsi:type="complexJobTrigger"&gt;
   *         &lt;cronString&gt;0 12 10 ? * 2,3,4,5,6 *&lt;/cronString&gt;
   *         &lt;duration&gt;10080000&lt;/duration&gt;
   *         &lt;startTime&gt;2014-08-19T10:12:00-04:00&lt;/startTime&gt;
   *         &lt;uiPassParam&gt;DAILY&lt;/uiPassParam&gt;
   *         &lt;dayOfMonthRecurrences /&gt;
   *         &lt;dayOfWeekRecurrences&gt;
   *           &lt;recurrenceList&gt;
   *             &lt;values&gt;2&lt;/values&gt;
   *             &lt;values&gt;3&lt;/values&gt;
   *             &lt;values&gt;4&lt;/values&gt;
   *             &lt;values&gt;5&lt;/values&gt;
   *             &lt;values&gt;6&lt;/values&gt;
   *           &lt;/recurrenceList&gt;
   *         &lt;/dayOfWeekRecurrences&gt;
   *         &lt;hourlyRecurrences&gt;
   *           &lt;recurrenceList&gt;
   *             &lt;values&gt;10&lt;/values&gt;
   *           &lt;/recurrenceList&gt;
   *         &lt;/hourlyRecurrences&gt;
   *         &lt;minuteRecurrences&gt;
   *           &lt;recurrenceList&gt;
   *             &lt;values&gt;12&lt;/values&gt;
   *           &lt;/recurrenceList&gt;
   *         &lt;/minuteRecurrences&gt;
   *         &lt;monthlyRecurrences /&gt;
   *         &lt;secondRecurrences&gt;
   *           &lt;recurrenceList&gt;
   *             &lt;values&gt;0&lt;/values&gt;
   *           &lt;/recurrenceList&gt;
   *         &lt;/secondRecurrences&gt;
   *         &lt;yearlyRecurrences /&gt;
   *       &lt;/jobTrigger&gt;
   *       &lt;nextRun&gt;2014-08-20T10:12:00-04:00&lt;/nextRun&gt;
   *       &lt;state&gt;NORMAL&lt;/state&gt;
   *       &lt;userName&gt;admin&lt;/userName&gt;
   *     &lt;/job&gt;
   *   &lt;/jobs&gt;
   * </pre>
   *
   * @return A Response object that contains a list of blockout jobs.
   */
  @GET
  @Path( "/blockout/blockoutjobs" )
  @Produces( { APPLICATION_JSON, APPLICATION_XML } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully retrieved blockout jobs." ),
    @ResponseCode( code = 500, condition = "Error while retrieving blockout jobs." ),
  } )
  public List<Job> getBlockoutJobs() {
    try {
      return (List<Job>) (List<?>) schedulerService.getBlockOutJobs();
    } catch ( Exception e ) {
      throw new RuntimeException( e );
    }
  }

  /**
   * Checks if there are blockouts in the system.
   *
   * <p><b>Example Request:</b><br />
   * GET pentaho/api/scheduler/blockout/hasblockouts
   * </p>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   true
   * </pre>
   *
   * @return true or false whether there are blackouts or not.
   */
  @GET
  @Path( "/blockout/hasblockouts" )
  @Produces( { TEXT_PLAIN } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully determined whether or not the system contains blockouts." ),
  } )
  public Response hasBlockouts() {
    boolean hasBlockouts = schedulerService.hasBlockouts();
    return buildOkResponse( Boolean.toString( hasBlockouts ) );
  }

  /**
   * Creates a new blockout for scheduled jobs.
   *
   * <p><b>Example Request:</b><br />
   * POST pentaho/api/scheduler/blockout/add
   * </p>
   * <br /><b>POST data:</b>
   * <pre function="syntax.xml">
   *   &lt;jobScheduleRequest&gt;
   *     &lt;jobName&gt;DAILY-1820438815:admin:7740000&lt;/jobName&gt;
   *     &lt;complexJobTrigger&gt;
   *       &lt;uiPassParam&gt;DAILY&lt;/uiPassParam&gt;
   *       &lt;daysOfWeek&gt;1&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;2&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;3&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;4&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;5&lt;/daysOfWeek&gt;
   *       &lt;startTime&gt;2014-08-19T10:51:00.000-04:00&lt;/startTime&gt;
   *       &lt;endTime /&gt;
   *     &lt;/complexJobTrigger&gt;
   *     &lt;inputFile&gt;&lt;/inputFile&gt;
   *     &lt;outputFile&gt;&lt;/outputFile&gt;
   *     &lt;duration&gt;7740000&lt;/duration&gt;
   *     &lt;timeZone&gt;America/New_York&lt;/timeZone&gt;
   *   &lt;/jobScheduleRequest&gt;
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   admin BlockoutAction 1410786491209
   * </pre>
   *
   * @param jobScheduleRequest A JobScheduleRequest object defining the blockout job.
   * @return A Response object which contains the ID of the blockout which was created.
   */
  @POST
  @Path( "/blockout/add" )
  @Consumes( { APPLICATION_JSON, APPLICATION_XML } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successful operation." ),
    @ResponseCode( code = 401, condition = "User is not authorized to create blockout." )
  } )
  public Response addBlockout( JobScheduleRequest jobScheduleRequest ) {
    try {
      return buildPlainTextOkResponse( schedulerService.addBlockout( jobScheduleRequest ).getJobId() );
    } catch ( IOException | SchedulerException | IllegalAccessException e ) {
      return buildStatusResponse( UNAUTHORIZED );
    }
  }

  /**
   * Update an existing blockout.
   *
   * <p><b>Example Request:</b><br />
   * POST pentaho/api/scheduler/blockout/update?jobid=admin%09BlockoutAction%091410786491209
   * </p>
   * <br /><b>POST data:</b>
   * <pre function="syntax.xml">
   *   &lt;jobScheduleRequest&gt;
   *     &lt;jobName&gt;DAILY-1820438815:admin:7740000&lt;/jobName&gt;
   *     &lt;complexJobTrigger&gt;
   *       &lt;uiPassParam&gt;DAILY&lt;/uiPassParam&gt;
   *       &lt;daysOfWeek&gt;1&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;2&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;3&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;4&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;5&lt;/daysOfWeek&gt;
   *       &lt;startTime&gt;2012-01-12T10:51:00.000-04:00&lt;/startTime&gt;
   *       &lt;endTime /&gt;
   *     &lt;/complexJobTrigger&gt;
   *     &lt;inputFile&gt;&lt;/inputFile&gt;
   *     &lt;outputFile&gt;&lt;/outputFile&gt;
   *     &lt;duration&gt;7740000&lt;/duration&gt;
   *     &lt;timeZone&gt;America/New_York&lt;/timeZone&gt;
   *   &lt;/jobScheduleRequest&gt;
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   admin BlockoutAction 1410786491503
   * </pre>
   *
   * @param jobId              The jobId of the blockout we are editing.
   * @param jobScheduleRequest The payload containing the definition of the blockout.
   * @return A Response object which contains the ID of the blockout which was created.
   */
  @POST
  @Path( "/blockout/update" )
  @Consumes( { APPLICATION_JSON, APPLICATION_XML } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successful operation." ),
    @ResponseCode( code = 401, condition = "User is not authorized to update blockout." )
  } )
  public Response updateBlockout( @QueryParam( "jobid" ) String jobId, JobScheduleRequest jobScheduleRequest ) {
    try {
      return buildPlainTextOkResponse( schedulerService.updateBlockout( jobId, jobScheduleRequest ).getJobId() );
    } catch ( IOException | SchedulerException | IllegalAccessException e ) {
      return buildStatusResponse( Status.UNAUTHORIZED );
    }
  }

  /**
   * Checks if the selected blockout schedule will be fired.
   *
   * <p><b>Example Request:</b><br />
   * POST pentaho/api/scheduler/blockout/willFire
   * </p>
   * <br /><b>POST data:</b>
   * <pre function="syntax.xml">
   *   &lt;jobScheduleRequest&gt;
   *     &lt;jobName&gt;DAILY-1820438815:admin:7740000&lt;/jobName&gt;
   *     &lt;complexJobTrigger&gt;
   *       &lt;uiPassParam&gt;DAILY&lt;/uiPassParam&gt;
   *       &lt;daysOfWeek&gt;1&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;2&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;3&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;4&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;5&lt;/daysOfWeek&gt;
   *       &lt;startTime&gt;2014-08-19T10:51:00.000-04:00&lt;/startTime&gt;
   *       &lt;endTime /&gt;
   *     &lt;/complexJobTrigger&gt;
   *     &lt;inputFile&gt;&lt;/inputFile&gt;
   *     &lt;outputFile&gt;&lt;/outputFile&gt;
   *     &lt;duration&gt;7740000&lt;/duration&gt;
   *     &lt;timeZone&gt;America/New_York&lt;/timeZone&gt;
   *   &lt;/jobScheduleRequest&gt;
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   false
   * </pre>
   *
   * @param jobScheduleRequest The payload containing the definition of the blockout.
   * @return true or false indicating whether the blockout will fire.
   */
  @POST
  @Path( "/blockout/willFire" )
  @Consumes( { APPLICATION_JSON, APPLICATION_XML } )
  @Produces( { TEXT_PLAIN } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successful operation." ),
    @ResponseCode( code = 500, condition = "An error occurred while determining blockouts being fired." )
  } )
  public Response blockoutWillFire( JobScheduleRequest jobScheduleRequest ) {
    boolean willFire;

    try {
      willFire = schedulerService.willFire( convertScheduleRequestToJobTrigger( jobScheduleRequest ) );
    } catch ( UnifiedRepositoryException | SchedulerException e ) {
      return buildServerErrorResponse( e );
    }

    return buildOkResponse( Boolean.toString( willFire ) );
  }

  /**
   * Checks if the selected blockout schedule should be fired now.
   *
   * <p><b>Example Request:</b><br />
   * GET pentaho/api/scheduler/blockout/shouldFireNow
   * </p>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   true
   * </pre>
   *
   * @return true or false whether the blockout should fire now.
   */
  @GET
  @Path( "/blockout/shouldFireNow" )
  @Produces( { TEXT_PLAIN } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successful operation." )
  } )
  public Response shouldFireNow() {
    boolean result = schedulerService.shouldFireNow();
    return buildOkResponse( Boolean.toString( result ) );
  }

  /**
   * Check the status of the selected blockout schedule.
   *
   * <p><b>Example Request:</b><br />
   * POST pentaho/api/scheduler/blockout/blockstatus
   * </p>
   * <br /><b>POST data:</b>
   * <pre function="syntax.xml">
   *   &lt;jobScheduleRequest&gt;
   *     &lt;jobName&gt;DAILY-1820438815:admin:7740000&lt;/jobName&gt;
   *     &lt;complexJobTrigger&gt;
   *       &lt;uiPassParam&gt;DAILY&lt;/uiPassParam&gt;
   *       &lt;daysOfWeek&gt;1&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;2&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;3&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;4&lt;/daysOfWeek&gt;
   *       &lt;daysOfWeek&gt;5&lt;/daysOfWeek&gt;
   *       &lt;startTime&gt;2014-08-19T10:51:00.000-04:00&lt;/startTime&gt;
   *       &lt;endTime /&gt;
   *     &lt;/complexJobTrigger&gt;
   *     &lt;inputFile&gt;&lt;/inputFile&gt;
   *     &lt;outputFile&gt;&lt;/outputFile&gt;
   *     &lt;duration&gt;7740000&lt;/duration&gt;
   *     &lt;timeZone&gt;America/New_York&lt;/timeZone&gt;
   *   &lt;/jobScheduleRequest&gt;
   * </pre>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   &lt;blockStatusProxy&gt;
   *     &lt;partiallyBlocked&gt;true&lt;/partiallyBlocked&gt;
   *     &lt;totallyBlocked&gt;true&lt;/totallyBlocked&gt;
   *   &lt;/blockStatusProxy&gt;
   * </pre>
   *
   * @param jobScheduleRequest The payload containing the definition of the blockout.
   * @return A Response object which contains a BlockStatusProxy which contains totallyBlocked and partiallyBlocked
   * flags.
   */
  @POST
  @Path( "/blockout/blockstatus" )
  @Consumes( { APPLICATION_JSON, APPLICATION_XML } )
  @Produces( { APPLICATION_JSON, APPLICATION_XML } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully got the blockout status." ),
    @ResponseCode( code = 401, condition = "User is not authorized to get the blockout status." )
  } )
  public Response getBlockStatus( JobScheduleRequest jobScheduleRequest ) {
    try {
      BlockStatusProxy blockStatusProxy = schedulerService.getBlockStatus( jobScheduleRequest );
      return buildOkResponse( blockStatusProxy );
    } catch ( SchedulerException e ) {
      return buildStatusResponse( Status.UNAUTHORIZED );
    }
  }

  /**
   * Retrieve the list of execute content by lineage id.
   *
   * <p><b>Example Request:</b><br />
   * GET pentaho/api/scheduler/generatedContentForSchedule?lineageId=:public:Steel%20Wheels:Inventory%20List%20
   * (report).prpt
   * </p>
   *
   * <p><b>Example Response:</b></p>
   * <pre function="syntax.xml">
   *   &lt;List&gt;
   *     &lt;repositoryFileDto&gt;
   *       &lt;createdDate&gt;1402911997019&lt;/createdDate&gt;
   *       &lt;fileSize&gt;3461&lt;/fileSize&gt;
   *       &lt;folder&gt;false&lt;/folder&gt;
   *       &lt;hidden&gt;false&lt;/hidden&gt;
   *       &lt;id&gt;ff11ac89-7eda-4c03-aab1-e27f9048fd38&lt;/id&gt;
   *       &lt;lastModifiedDate&gt;1406647160536&lt;/lastModifiedDate&gt;
   *       &lt;locale&gt;en&lt;/locale&gt;
   *       &lt;localePropertiesMapEntries&gt;
   *         &lt;localeMapDto&gt;
   *           &lt;locale&gt;default&lt;/locale&gt;
   *           &lt;properties&gt;
   *             &lt;stringKeyStringValueDto&gt;
   *               &lt;key&gt;file.title&lt;/key&gt;
   *               &lt;value&gt;myFile&lt;/value&gt;
   *             &lt;/stringKeyStringValueDto&gt;
   *             &lt;stringKeyStringValueDto&gt;
   *               &lt;key&gt;jcr:primaryType&lt;/key&gt;
   *               &lt;value&gt;nt:unstructured&lt;/value&gt;
   *             &lt;/stringKeyStringValueDto&gt;
   *             &lt;stringKeyStringValueDto&gt;
   *               &lt;key&gt;title&lt;/key&gt;
   *               &lt;value&gt;myFile&lt;/value&gt;
   *             &lt;/stringKeyStringValueDto&gt;
   *             &lt;stringKeyStringValueDto&gt;
   *               &lt;key&gt;file.description&lt;/key&gt;
   *               &lt;value&gt;myFile Description&lt;/value&gt;
   *             &lt;/stringKeyStringValueDto&gt;
   *           &lt;/properties&gt;
   *         &lt;/localeMapDto&gt;
   *       &lt;/localePropertiesMapEntries&gt;
   *       &lt;locked&gt;false&lt;/locked&gt;
   *       &lt;name&gt;myFile.prpt&lt;/name&gt;&lt;/name&gt;
   *       &lt;originalParentFolderPath&gt;/public/admin&lt;/originalParentFolderPath&gt;
   *       &lt;ownerType&gt;-1&lt;/ownerType&gt;
   *       &lt;path&gt;/public/admin/ff11ac89-7eda-4c03-aab1-e27f9048fd38&lt;/path&gt;
   *       &lt;title&gt;myFile&lt;/title&gt;
   *       &lt;versionId&gt;1.9&lt;/versionId&gt;
   *       &lt;versioned&gt;true&lt;/versioned&gt;
   *     &lt;/repositoryFileAclDto&gt;
   *   &lt;/List&gt;
   * </pre>
   *
   * @param lineageId the path for the file.
   * @return A list of RepositoryFileDto objects.
   */
  @GET
  @Path( "/generatedContentForSchedule" )
  @Produces( { APPLICATION_XML, APPLICATION_JSON } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully got the generated content for schedule" )
  } )
  public List<RepositoryFileDto> doGetGeneratedContentForSchedule( @QueryParam( "lineageId" ) String lineageId ) {
    List<RepositoryFileDto> repositoryFileDtoList = new ArrayList<>();

    try {
      repositoryFileDtoList = schedulerService.doGetGeneratedContentForSchedule( lineageId );
    } catch ( FileNotFoundException e ) {
      //return the empty list
    } catch ( Exception e ) {
      logger.error( Messages.getInstance().getString( "FileResource.GENERATED_CONTENT_FOR_USER_FAILED", lineageId ),
        e );
    }

    return repositoryFileDtoList;
  }

  protected Response buildOkResponse( Object entity ) {
    return Response.ok( entity ).build();
  }

  protected Response buildPlainTextOkResponse( String msg ) {
    return Response.ok( msg ).type( MediaType.TEXT_PLAIN ).build();
  }

  protected Response buildServerErrorResponse( Object entity ) {
    return Response.serverError().entity( entity ).build();
  }

  protected Response buildStatusResponse( Status status ) {
    return Response.status( status ).build();
  }

  @SuppressWarnings( "SameParameterValue" )
  protected Response buildPlainTextStatusResponse( Status status ) {
    return Response.status( status ).type( MediaType.TEXT_PLAIN ).build();
  }

  @SuppressWarnings( "UnusedReturnValue" )
  protected JobRequest getJobRequest() {
    return new JobRequest();
  }

  protected IJobTrigger convertScheduleRequestToJobTrigger( JobScheduleRequest request ) throws SchedulerException {
    return SchedulerResourceUtil.convertScheduleRequestToJobTrigger( request, schedulerService.getScheduler() );
  }

  private String getErrorMessage( Exception exception ) {
    Throwable cause = exception.getCause();

    return cause != null ? cause.getMessage() : exception.getMessage();
  }

  protected Response buildStatusResponse( Response.Status status, Object entity ) {
    try {
      String json = new ObjectMapper().writeValueAsString( entity );
      return Response.status( status ).entity( json ).build();
    } catch ( JsonProcessingException e ) {
      e.printStackTrace();
      return Response.serverError().build();
    }
  }
}
