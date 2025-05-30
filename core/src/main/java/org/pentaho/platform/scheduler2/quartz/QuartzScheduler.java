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


package org.pentaho.platform.scheduler2.quartz;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.action.IAction;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.scheduler2.ComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.CronJobTrigger;
import org.pentaho.platform.api.scheduler2.IBackgroundExecutionStreamProvider;
import org.pentaho.platform.api.scheduler2.IComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.ICronJobTrigger;
import org.pentaho.platform.api.scheduler2.IJob;
import org.pentaho.platform.api.scheduler2.IJobFilter;
import org.pentaho.platform.api.scheduler2.IJobRequest;
import org.pentaho.platform.api.scheduler2.IJobResult;
import org.pentaho.platform.api.scheduler2.IJobScheduleParam;
import org.pentaho.platform.api.scheduler2.IJobScheduleRequest;
import org.pentaho.platform.api.scheduler2.IJobTrigger;
import org.pentaho.platform.api.scheduler2.IScheduleSubject;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.ISchedulerListener;
import org.pentaho.platform.api.scheduler2.ISchedulerResource;
import org.pentaho.platform.api.scheduler2.ISimpleJobTrigger;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.JobState;
import org.pentaho.platform.api.scheduler2.JobTrigger;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.scheduler2.messsages.Messages;
import org.pentaho.platform.scheduler2.recur.ITimeRecurrence;
import org.pentaho.platform.scheduler2.recur.IncrementalRecurrence;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfMonth;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek.DayOfWeek;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek.DayOfWeekQualifier;
import org.pentaho.platform.scheduler2.recur.RecurrenceList;
import org.pentaho.platform.scheduler2.recur.SequentialRecurrence;
import org.pentaho.platform.web.http.api.resources.JobRequest;
import org.pentaho.platform.web.http.api.resources.JobScheduleParam;
import org.pentaho.platform.web.http.api.resources.JobScheduleRequest;
import org.pentaho.platform.web.http.api.resources.SchedulerResource;
import org.quartz.Calendar;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.DateBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.CalendarIntervalTriggerImpl;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.spi.MutableTrigger;

import java.io.Serializable;
import java.security.Principal;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

/**
 * A Quartz implementation of {@link IScheduler}
 *
 * @author aphillips
 */
public class QuartzScheduler implements IScheduler {

  public static final String UI_PASS_PARAM_DAILY = "DAILY";
  public static final String UI_PASS_PARAM_SECONDS = "SECONDS";
  public static final String UI_PASS_PARAM_MINUTES = "MINUTES";
  public static final String UI_PASS_PARAM_HOURS = "HOURS";
  public static final String UI_PASS_PARAM_RUN_ONCE = "RUN_ONCE";

  public static final String COMPLEX_JOB_TRIGGER_ERROR_0001_INVALID_CRON_EXPRESSION = "ComplexJobTrigger.ERROR_0001_InvalidCronExpression";

  public static final String QUARTZ_SCHEDULER_ERROR_0001_FAILED_TO_SCHEDULE_JOB = "QuartzScheduler.ERROR_0001_FAILED_TO_SCHEDULE_JOB";
  public static final String QUARTZ_SCHEDULER_ERROR_0002_TRIGGER_WRONG_TYPE = "QuartzScheduler.ERROR_0002_TRIGGER_WRONG_TYPE";
  public static final String QUARTZ_SCHEDULER_ERROR_0003_ACTION_IS_NULL = "QuartzScheduler.ERROR_0003_ACTION_IS_NULL";
  public static final String QUARTZ_SCHEDULER_ERROR_0004_FAILED_TO_LIST_JOBS = "QuartzScheduler.ERROR_0004_FAILED_TO_LIST_JOBS";
  public static final String QUARTZ_SCHEDULER_ERROR_0005_FAILED_TO_PAUSE_JOBS = "QuartzScheduler.ERROR_0005_FAILED_TO_PAUSE_JOBS";
  public static final String QUARTZ_SCHEDULER_ERROR_0005_FAILED_TO_RESUME_JOBS = "QuartzScheduler.ERROR_0005_FAILED_TO_RESUME_JOBS";
  public static final String QUARTZ_SCHEDULER_ERROR_0006_FAILED_TO_GET_SCHEDULER_STATUS = "QuartzScheduler.ERROR_0006_FAILED_TO_GET_SCHEDULER_STATUS";
  public static final String QUARTZ_SCHEDULER_ERROR_0007_FAILED_TO_GET_JOB = "QuartzScheduler.ERROR_0007_FAILED_TO_GET_JOB";
  public static final String QUARTZ_SCHEDULER_ERROR_0008_SCHEDULING_IS_NOT_ALLOWED = "QuartzScheduler.ERROR_0008_SCHEDULING_IS_NOT_ALLOWED";

  private static Log logger;

  private SchedulerFactory quartzSchedulerFactory;

  private Scheduler quartzSchedulerInstance;

  private final ArrayList<ISchedulerListener> listeners = new ArrayList<>();

  private static final Pattern listPattern = Pattern.compile( "\\d+" );

  private static final Pattern dayOfWeekRangePattern = Pattern.compile( ".*-.*" );

  private static final Pattern sequencePattern = Pattern.compile( "\\d+-\\d+" );

  private static final Pattern intervalPattern = Pattern.compile( "[\\d*]+/\\d+" );

  private static final Pattern qualifiedDayPattern = Pattern.compile( "\\d+#\\d+" );

  private static final Pattern lastDayPattern = Pattern.compile( "\\d+L" );

  private final ReentrantReadWriteLock jobDetailLock = new ReentrantReadWriteLock();

  public QuartzScheduler( SchedulerFactory schedulerFactory ) {
    this.quartzSchedulerFactory = schedulerFactory;
  }

  public QuartzScheduler() {
    this.quartzSchedulerFactory = new StdSchedulerFactory();

    logger = LogFactory.getLog( QuartzScheduler.class );

    logger.info( "----------------------------------------" );
    logger.info( "email-source:  " + PentahoSystem.getSystemSetting( "email-source", "pentaho" ) );
    logger.info( "----------------------------------------" );
  }

  /**
   * Overrides the default Quartz {@link SchedulerFactory}. Note: depending on the type of scheduler you are setting
   * here, there may be initializing required prior to this setter being called. Only the
   * {@link SchedulerFactory#getScheduler()} will be called later, so the factory set here must already be in a state
   * where that invocation will be successful.
   *
   * @param quartzSchedulerFactory the quartz factory to use for generating scheduler instances
   */
  public void setQuartzSchedulerFactory( SchedulerFactory quartzSchedulerFactory ) throws SchedulerException {
    this.quartzSchedulerFactory = quartzSchedulerFactory;
    if ( quartzSchedulerInstance != null ) {
      this.shutdown();
      quartzSchedulerInstance = null;
    }
  }

  public Scheduler getQuartzScheduler() throws org.quartz.SchedulerException {
    if ( quartzSchedulerInstance == null ) {
      /*
       * Currently, quartz will always give you the same scheduler object when any factory instance is asked for a
       * scheduler. In other words there is no such thing as scheduler-level isolation. If we really need multiple
       * isolated scheduler instances, we should investigate named schedulers, but this API getScheduler() will not help
       * us in that regard.
       */
      quartzSchedulerInstance = quartzSchedulerFactory.getScheduler();
    }

    logger.debug( "Using quartz scheduler " + quartzSchedulerInstance );
    return quartzSchedulerInstance;
  }

  private void setQuartzScheduler( Scheduler quartzScheduler ) {
    this.quartzSchedulerInstance = quartzScheduler;
  }

  /**
   * {@inheritDoc}
   */
  public Job createJob( String jobName, String actionId, Map<String, Object> jobParams, IJobTrigger trigger )
    throws SchedulerException {
    return createJob( jobName, actionId, jobParams, trigger, null );
  }

  /**
   * {@inheritDoc}
   */
  public Job createJob( String jobName, Class<? extends IAction> action, Map<String, Object> jobParams,
                        IJobTrigger trigger ) throws SchedulerException {
    return createJob( jobName, action, jobParams, trigger, null );
  }

  /**
   * {@inheritDoc}
   */
  public Job createJob( String jobName, Class<? extends IAction> action, Map<String, Object> jobParams,
                        IJobTrigger trigger, IBackgroundExecutionStreamProvider outputStreamProvider )
    throws SchedulerException {

    if ( action == null ) {
      throw new SchedulerException(
        Messages.getString( QUARTZ_SCHEDULER_ERROR_0003_ACTION_IS_NULL ) );
    }

    if ( jobParams == null ) {
      jobParams = new HashMap<>();
    }

    jobParams.put( IScheduler.RESERVEDMAPKEY_ACTIONCLASS, action.getName() );
    Job ret = createJob( jobName, jobParams, trigger, outputStreamProvider );
    ret.setSchedulableClass( action.getName() );
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  public Job createJob( String jobName, String actionId, Map<String, Object> jobParams, IJobTrigger trigger,
                        IBackgroundExecutionStreamProvider outputStreamProvider ) throws SchedulerException {
    if ( StringUtils.isEmpty( actionId ) ) {
      throw new SchedulerException(
        Messages.getString( QUARTZ_SCHEDULER_ERROR_0003_ACTION_IS_NULL ) );
    }

    if ( jobParams == null ) {
      jobParams = new HashMap<>();
    }

    jobParams.put( RESERVEDMAPKEY_ACTIONID, actionId );
    Job ret = createJob( jobName, jobParams, trigger, outputStreamProvider );
    ret.setSchedulableClass( "" );
    return ret;
  }

  public static MutableTrigger createQuartzTrigger( IJobTrigger jobTrigger, QuartzJobKey jobId ) throws SchedulerException {
    validateJobTrigger( jobTrigger );

    Date triggerEndDate = getTriggerEndDate( jobTrigger );
    java.util.Calendar startDateCal = getStartDateCalendar( jobTrigger );
    TimeZone tz = getTimeZone( jobTrigger );

    if ( jobTrigger instanceof ComplexJobTrigger ) {
      return createComplexQuartzTrigger( (ComplexJobTrigger) jobTrigger, jobId, triggerEndDate, tz );
    } else if ( jobTrigger instanceof SimpleJobTrigger ) {
      return createSimpleQuartzTrigger( (SimpleJobTrigger) jobTrigger, jobId, triggerEndDate, startDateCal, tz );
    } else {
      throw new SchedulerException( Messages.getString(
        QUARTZ_SCHEDULER_ERROR_0002_TRIGGER_WRONG_TYPE ) );
    }
  }

  private static void validateJobTrigger( IJobTrigger jobTrigger ) throws SchedulerException {
    if ( jobTrigger == null ) {
      throw new SchedulerException( "jobTrigger cannot be null" );
    }
  }

  private static Date getTriggerEndDate( IJobTrigger jobTrigger ) {
    if ( jobTrigger.getEndTime() != null ) {
      java.util.Calendar endDateCal = getEndDateCalFromTrigger( jobTrigger );
      return endDateCal.getTime();
    }
    return null;
  }

  private static java.util.Calendar getStartDateCalendar( IJobTrigger jobTrigger ) {
    if ( jobTrigger.getStartHour() >= 0 ) {
      return getStartDateCalFromTrigger( jobTrigger );
    } else {
      java.util.Calendar startDateCal = java.util.Calendar.getInstance();
      startDateCal.setTime( jobTrigger.getStartTime() != null ? jobTrigger.getStartTime() : new Date() );
      return startDateCal;
    }
  }

  private static TimeZone getTimeZone( IJobTrigger jobTrigger ) {
    return jobTrigger.getTimeZone() != null ? TimeZone.getTimeZone( jobTrigger.getTimeZone() ) : null;
  }

  private static MutableTrigger createComplexQuartzTrigger( ComplexJobTrigger complexJobTrigger, QuartzJobKey jobId,
                                                           Date triggerEndDate, TimeZone tz ) throws SchedulerException {
    try {
      CronTriggerImpl cronTrigger = new CronTriggerImpl();
      cronTrigger.setName( jobId.toString() );
      cronTrigger.setGroup( jobId.getUserName() );
      cronTrigger.setCronExpression( complexJobTrigger.getCronString() != null
        ? complexJobTrigger.getCronString()
        : QuartzCronStringFactory.createCronString( complexJobTrigger ) );
      if ( tz != null ) {
        cronTrigger.setTimeZone( tz );
      }
      if ( triggerEndDate != null ) {
        cronTrigger.setEndTime( triggerEndDate );
      }
      return cronTrigger;
    } catch ( ParseException e ) {
      throw new SchedulerException( Messages.getInstance().getString(
        QUARTZ_SCHEDULER_ERROR_0001_FAILED_TO_SCHEDULE_JOB, jobId.getJobName() ), e );
    }
  }

  private static MutableTrigger createSimpleQuartzTrigger( SimpleJobTrigger simpleTrigger, QuartzJobKey jobId,
                                                          Date triggerEndDate, java.util.Calendar startDateCal,
                                                          TimeZone tz ) throws SchedulerException {
    try {
      CalendarIntervalTriggerImpl calendarIntervalTrigger = new CalendarIntervalTriggerImpl();
      configureSimpleTrigger( simpleTrigger, calendarIntervalTrigger, triggerEndDate, startDateCal, tz );
      calendarIntervalTrigger.setKey( new TriggerKey( jobId.toString(), jobId.getUserName() ) );
      return calendarIntervalTrigger;
    } catch ( IllegalArgumentException e ) {
      throw new SchedulerException( Messages.getInstance().getString(
        QUARTZ_SCHEDULER_ERROR_0001_FAILED_TO_SCHEDULE_JOB, jobId.getJobName() ), e );
    }
  }

  private static void configureSimpleTrigger( SimpleJobTrigger simpleTrigger, CalendarIntervalTriggerImpl calendarIntervalTrigger,
                                             Date triggerEndDate, java.util.Calendar startDateCal, TimeZone tz ) {
    if ( simpleTrigger.getUiPassParam() == null ) {
      simpleTrigger.setUiPassParam( UI_PASS_PARAM_DAILY );
      logger.debug( "UiPassParam is null, defaulting to " + UI_PASS_PARAM_DAILY );
    }

    long interval = simpleTrigger.getRepeatInterval();
    int triggerInterval = calculateTriggerInterval( simpleTrigger, interval );
    DateBuilder.IntervalUnit intervalUnit = determineIntervalUnit( simpleTrigger );

    calendarIntervalTrigger.setRepeatInterval( triggerInterval );
    calendarIntervalTrigger.setRepeatIntervalUnit( intervalUnit );
    calendarIntervalTrigger.setMisfireInstruction( CalendarIntervalTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW );
    if ( triggerEndDate != null ) {
      calendarIntervalTrigger.setEndTime( triggerEndDate );
    }
    calendarIntervalTrigger.setStartTime( startDateCal.getTime() );
    if ( tz != null ) {
      calendarIntervalTrigger.setTimeZone( tz );
    }
  }

  private static int calculateTriggerInterval( SimpleJobTrigger simpleTrigger, long interval ) {
    if ( simpleTrigger.getUiPassParam() == null ) {
      throw new IllegalArgumentException( "Invalid UiPassParam: " + simpleTrigger.getUiPassParam() );
    }

    switch ( simpleTrigger.getUiPassParam().toUpperCase() ) {
      case UI_PASS_PARAM_SECONDS:
        return (int) interval;
      case UI_PASS_PARAM_MINUTES:
        return (int) interval / 60;
      case UI_PASS_PARAM_HOURS:
        return (int) interval / 3600;
      case UI_PASS_PARAM_DAILY:
        return (int) interval / 86400;
      case UI_PASS_PARAM_RUN_ONCE:
        return 2; // Special case for RUN_ONCE
      default:
        throw new IllegalArgumentException( "Invalid UiPassParam: " + simpleTrigger.getUiPassParam() );
    }
  }

  private static DateBuilder.IntervalUnit determineIntervalUnit( SimpleJobTrigger simpleTrigger ) {
    if ( simpleTrigger.getUiPassParam() == null ) {
      throw new IllegalArgumentException( "Invalid UiPassParam: " + simpleTrigger.getUiPassParam() );
    }

    switch ( simpleTrigger.getUiPassParam().toUpperCase() ) {
      case UI_PASS_PARAM_SECONDS:
        return DateBuilder.IntervalUnit.SECOND;
      case UI_PASS_PARAM_MINUTES:
        return DateBuilder.IntervalUnit.MINUTE;
      case UI_PASS_PARAM_HOURS:
        return DateBuilder.IntervalUnit.HOUR;
      case UI_PASS_PARAM_DAILY:
        return DateBuilder.IntervalUnit.DAY;
      case UI_PASS_PARAM_RUN_ONCE:
        return DateBuilder.IntervalUnit.YEAR; // Special case for RUN_ONCE
      default:
        throw new IllegalArgumentException( "Invalid UiPassParam: " + simpleTrigger.getUiPassParam() );
    }
  }

  private JobDetail createJobDetails( QuartzJobKey jobId, Map<String, Object> jobParams ) {
    jobParams.put( RESERVEDMAPKEY_ACTIONUSER, jobId.getUserName() );
    return JobBuilder.newJob( BlockingQuartzJob.class )
     .withIdentity( jobId.toString(), jobId.getUserName() )
     .setJobData( new JobDataMap( jobParams ) )
     .build();
  }

  private Calendar createQuartzCalendar( ComplexJobTrigger complexJobTrigger ) {
    Calendar triggerCalendar = null;
    if ( complexJobTrigger.getStartHour() > -1 ) {
      java.util.Calendar startDateCal = getStartDateCalFromTrigger( complexJobTrigger );
      if ( complexJobTrigger.getEndTime() != null ) {
        java.util.Calendar endDateCal = getEndDateCalFromTrigger( complexJobTrigger );
        triggerCalendar = new QuartzSchedulerAvailability( Date.from( startDateCal.toInstant() ), Date.from( endDateCal.toInstant() ) );
      } else {
        triggerCalendar = new QuartzSchedulerAvailability( Date.from( startDateCal.toInstant() ), null );
      }
    } else if ( ( complexJobTrigger.getStartTime() != null ) || ( complexJobTrigger.getEndTime() != null ) ) {
      triggerCalendar =
        new QuartzSchedulerAvailability( complexJobTrigger.getStartTime(), complexJobTrigger.getEndTime() );
    }
    return triggerCalendar;
  }

  private static java.util.Calendar getStartDateCalFromTrigger( IJobTrigger jobTrigger ) {
    java.util.Calendar startDateCal = java.util.Calendar.getInstance();
    startDateCal.clear();
    if ( null != jobTrigger.getTimeZone() ) {
      TimeZone tz = TimeZone.getTimeZone( jobTrigger.getTimeZone() );
      startDateCal.setTimeZone( tz );
    }
    startDateCal.set( jobTrigger.getStartYear() + 1900, jobTrigger.getStartMonth(), jobTrigger.getStartDay() );
    startDateCal.set( java.util.Calendar.HOUR_OF_DAY, jobTrigger.getStartHour() );
    startDateCal.set( java.util.Calendar.MINUTE, jobTrigger.getStartMin() );
    startDateCal.set( java.util.Calendar.SECOND, 0 );
    return startDateCal;
  }

  private static java.util.Calendar getEndDateCalFromTrigger( IJobTrigger jobTrigger ) {
    java.util.Calendar cal = java.util.Calendar.getInstance();
    cal.clear();
    if ( null != jobTrigger.getTimeZone() ) {
      TimeZone tz = TimeZone.getTimeZone( jobTrigger.getTimeZone() );
      cal.setTimeZone( tz );
    }
    cal.set( jobTrigger.getEndTime().getYear() + 1900, jobTrigger.getEndTime().getMonth(),
      jobTrigger.getEndTime().getDate(), jobTrigger.getEndTime().getHours(), jobTrigger.getEndTime().getMinutes() );
    return cal;
  }

  /**
   * {@inheritDoc}
   */
  protected Job createJob( String jobName, Map<String, Object> jobParams, IJobTrigger trigger,
                           IBackgroundExecutionStreamProvider outputStreamProvider ) throws SchedulerException {

    String curUser = getCurrentUser();

    // determine if the job params tell us who owns the job
    Serializable jobOwner = (Serializable) jobParams.get( RESERVEDMAPKEY_ACTIONUSER );
    if ( jobOwner != null && !jobOwner.toString().isEmpty() ) {
      curUser = jobOwner.toString();
    }

    QuartzJobKey jobId = new QuartzJobKey( jobName, curUser );
    logger.debug( " QuartzScheduler has received a request to createJob with jobId " + jobId );

    MutableTrigger quartzTrigger = createQuartzTrigger( trigger, jobId );

    Calendar triggerCalendar =
      quartzTrigger instanceof CronTrigger ? createQuartzCalendar( (ComplexJobTrigger) trigger ) : null;

    if ( outputStreamProvider != null ) {
      jobParams.put( RESERVEDMAPKEY_STREAMPROVIDER, outputStreamProvider );
    }

    if ( trigger.getUiPassParam() != null ) {
      jobParams.put( RESERVEDMAPKEY_UIPASSPARAM, trigger.getUiPassParam() );
    }

    if ( !jobParams.containsKey( RESERVEDMAPKEY_LINEAGE_ID ) ) {
      String uuid = UUID.randomUUID().toString();
      jobParams.put( RESERVEDMAPKEY_LINEAGE_ID, uuid );
    }

    if ( trigger.getStartTime() != null ) {
      jobParams.put( RESERVEDMAPKEY_START_TIME, trigger.getStartTime() );
    }

    JobDetail jobDetail = createJobDetails( jobId, jobParams );

    try {
      Scheduler scheduler = getQuartzScheduler();
      if ( triggerCalendar != null ) {
        scheduler.addCalendar( jobId.toString(), triggerCalendar, false, false );
        quartzTrigger.setCalendarName( jobId.toString() );
      }
      logger.debug(
        MessageFormat.format( "Scheduling job {0} with trigger {1} and job parameters [ {2} ]", jobId,
          trigger, prettyPrintMap( jobParams ) ) );

      if ( quartzTrigger instanceof CronTrigger ) {
        Serializable timezone = (Serializable) jobParams.get( "timezone" );
        if ( timezone != null ) {
          setTimezone( (CronTrigger) quartzTrigger, timezone.toString() );
        }
      }

      jobDetailLock.writeLock().lock();
      try {
        scheduler.scheduleJob( jobDetail, quartzTrigger );
      } finally {
        jobDetailLock.writeLock().unlock();
      }

      logger.debug( MessageFormat.format( "Scheduled job {0} successfully", jobId ) );
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException( Messages.getInstance().getString(
        QUARTZ_SCHEDULER_ERROR_0001_FAILED_TO_SCHEDULE_JOB, jobName ), e );
    }

    Job job = new Job();
    job.setJobParams( jobParams );
    job.setJobTrigger( (JobTrigger) trigger );
    job.setNextRun( quartzTrigger.getNextFireTime() );
    job.setLastRun( getLastRun( quartzTrigger ) );
    job.setJobId( jobId.toString() );
    job.setJobName( jobName );
    job.setUserName( curUser );
    job.setState( JobState.NORMAL );

    return job;
  }

  @Override
  public void updateJob( String jobId, Map<String, Object> jobParams, IJobTrigger trigger ) throws SchedulerException {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  public Map<IScheduleSubject, IComplexJobTrigger> getAvailabilityWindows() {
    // Not implemented
    return Collections.emptyMap();
  }

  /**
   * {@inheritDoc}
   */
  public List<IJobResult> getJobHistory( String jobId ) {
    // Not implemented
    return Collections.emptyList();
  }

  /**
   * {@inheritDoc}
   */
  public void triggerNow( String jobId ) throws SchedulerException {
    try {
      QuartzJobKey quartzJobKey = QuartzJobKey.parse( jobId );
      JobKey jobKey = new JobKey( jobId, quartzJobKey.getUserName() );

      saveTriggerNowDate( jobKey, new Date() );

      getQuartzScheduler().triggerJob( jobKey );
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException( Messages.getInstance().getString(
        QUARTZ_SCHEDULER_ERROR_0007_FAILED_TO_GET_JOB, jobId ), e );
    }
  }

  private void saveTriggerNowDate( JobKey jobKey, Date newDate ) throws org.quartz.SchedulerException {
    jobDetailLock.writeLock().lock();
    try {
      JobDetail oldJobDetail = getJobDetail( jobKey );

      JobDataMap jobDataMap = oldJobDetail.getJobDataMap();
      jobDataMap.put( RESERVEDMAPKEY_PREVIOUS_TRIGGER_NOW, newDate );
    
      JobDetail newJobDetail = JobBuilder.newJob( oldJobDetail.getJobClass() )
        .withIdentity( jobKey )
        .usingJobData( jobDataMap )
        .build();

      Trigger oldTrigger = getSingleJobTrigger( jobKey );

      // Create a new trigger with the same properties as the old one, but with the new start time
      // to avoid duplicated executions due to misfire instructions
      Trigger newTrigger = oldTrigger.getTriggerBuilder()
        .startAt( oldTrigger.getNextFireTime() )
        .build();

      // Delete the old trigger and schedule the new one
      // We cannot use addJob since the JobDetail is not being stored durably, so it's immutable
      getQuartzScheduler().deleteJob( jobKey );
      getQuartzScheduler().scheduleJob( newJobDetail, newTrigger );
    } finally {
      jobDetailLock.writeLock().unlock();
    }
  }

  /**
   * Indicates if this trigger was created by quartz internally as a result of a triggerJob call
   * @param trigger the trigger to check
   * @return true if the trigger is a manual trigger, false otherwise
   */
  protected boolean isManualTrigger( Trigger trigger ) {
    return null != trigger.getKey() && null != trigger.getKey().getName() && trigger.getKey().getName().startsWith( "MT_" );
  }

  @Override
  public void setSubjectAvailabilityWindow( IScheduleSubject subject, IComplexJobTrigger window ) {
    // Not implemented
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings( "unchecked" )
  public Job getJob( String jobId ) throws SchedulerException {
    try {
      Scheduler scheduler = getQuartzScheduler();
      QuartzJobKey quartzJobKey = QuartzJobKey.parse( jobId );
      String groupName = quartzJobKey.getUserName();
      JobKey jobKey = new JobKey( jobId, groupName );

      Trigger trigger = getSingleJobTrigger( jobKey );
      if ( trigger == null ) {
        return null;
      }

      Job job = new Job();

      JobDetail jobDetail = getJobDetail( jobKey );
      if ( jobDetail != null ) {
        job.setUserName( jobDetail.getKey().getGroup() );
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        if ( jobDataMap != null ) {
          job.setJobParams( jobDataMap.getWrappedMap() );
        }
      }

      job.setJobId( jobId );
      setJobTrigger( scheduler, job, trigger );
      return job;
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException( Messages.getInstance().getString(
        QUARTZ_SCHEDULER_ERROR_0007_FAILED_TO_GET_JOB, jobId ), e );
    }
  }

  /**
   * Retrieves the single trigger associated with a job.
   * The current implementation of the scheduler allows only one trigger per job.
   * If multiple triggers exist, this method returns the first one that is not a manual trigger,
   * as manual triggers are ignored.
   *
   * @param jobKey the unique identifier of the job
   * @return the first non-manual trigger associated with the job, or null if no such trigger exists
   * @throws org.quartz.SchedulerException if there is an error accessing the scheduler
   */
  protected Trigger getSingleJobTrigger( JobKey jobKey ) throws org.quartz.SchedulerException {
    return getQuartzScheduler().getTriggersOfJob( jobKey ).stream()
      .filter( t -> !isManualTrigger( t ) )
      .findFirst()
      .orElse( null );
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings( "unchecked" )
  public List<IJob> getJobs( IJobFilter filter ) throws SchedulerException {
    ArrayList<IJob> jobs = new ArrayList<>();
    try {
      Scheduler scheduler = getQuartzScheduler();
      for ( String groupName : scheduler.getJobGroupNames() ) {
        scheduler.getJobKeys( GroupMatcher.jobGroupEquals( groupName ) );
        for ( JobKey jobKey : scheduler.getJobKeys( GroupMatcher.jobGroupEquals( groupName ) ) ) {
          String jobId = jobKey.getName();
          Trigger trigger = getSingleJobTrigger( jobKey );
          if ( trigger == null ) {
            continue;
          }
          Job job = new Job();
          job.setGroupName( groupName );
          JobDetail jobDetail = getJobDetail( jobKey );
          if ( jobDetail != null ) {
            job.setUserName( jobDetail.getKey().getGroup() );
            job.setJobParams( jobDetail.getJobDataMap().getWrappedMap() );
          }

          job.setJobId( jobId );
          setJobTrigger( scheduler, job, trigger );
          job.setJobName( QuartzJobKey.parse( jobId ).getJobName() );
          setJobNextRun( job, trigger );
          job.setLastRun( getLastRun( trigger ) );
          if ( ( filter == null ) || filter.accept( job ) ) {
            jobs.add( job );
          }
        }
      }
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException(
        Messages.getString( QUARTZ_SCHEDULER_ERROR_0004_FAILED_TO_LIST_JOBS ), e );
    }
    return jobs;
  }

  protected Date getLastRun( Trigger trigger ) {
    Date previousTriggerNow = getPreviousTriggerNow( trigger );
    Date previousFireTime = trigger.getPreviousFireTime();

    if ( previousTriggerNow == null ) {
      return previousFireTime;
    }

    return ( previousFireTime == null || previousTriggerNow.after( previousFireTime ) )
         ? previousTriggerNow : previousFireTime;
  }

  private Date getPreviousTriggerNow( Trigger trigger ) {
    JobDetail jobDetail;

    try {
      jobDetail = getJobDetail( trigger.getJobKey() );
    } catch ( org.quartz.SchedulerException e ) {
      logger.warn( "Job not found: " + trigger.getJobKey().toString(), e  );
      return null;
    }

    JobDataMap jobDataMap = jobDetail.getJobDataMap();
    if ( !jobDetail.getJobDataMap().containsKey( RESERVEDMAPKEY_PREVIOUS_TRIGGER_NOW ) ) {
      return null;
    }

    Object previousTriggerNowObj = jobDataMap.get( RESERVEDMAPKEY_PREVIOUS_TRIGGER_NOW );
    if ( !( previousTriggerNowObj instanceof Date ) ) {
      return null;
    }

    return (Date) previousTriggerNowObj;
  }

  protected void setJobNextRun( Job job, Trigger trigger ) {
    //if getNextFireTime() is in the future, then we use it
    //if it is in the past, we call getFireTimeAfter( new Date() ) to get the correct next date from today on
    Date nextFire = trigger.getNextFireTime();
    job.setNextRun( nextFire != null && ( nextFire.getTime() < new Date().getTime() )
      ? trigger.getFireTimeAfter( new Date() )
      : nextFire );
  }

  private void setJobTrigger( Scheduler scheduler, Job job, Trigger trigger ) throws SchedulerException,
    org.quartz.SchedulerException {
    QuartzJobKey jobKey = QuartzJobKey.parse( job.getJobId() );
    String groupName = jobKey.getUserName();

    if ( trigger instanceof SimpleTrigger ) {
      // handle the legacy case where there were still simple triggers in the DB
      SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
      SimpleJobTrigger simpleJobTrigger = new SimpleJobTrigger();

      simpleJobTrigger.setStartTime( simpleTrigger.getStartTime() );
      simpleJobTrigger.setEndTime( simpleTrigger.getEndTime() );
      int startHour = simpleTrigger.getStartTime().getHours();
      if ( startHour > 12 ) {
        startHour -= 12;
      }
      simpleJobTrigger.setStartHour( startHour );
      simpleJobTrigger.setStartMin( simpleTrigger.getStartTime().getMinutes() );
      simpleJobTrigger.setStartYear( simpleTrigger.getStartTime().getYear() + 1900 );
      simpleJobTrigger.setStartMonth( simpleTrigger.getStartTime().getMonth() - 1 ); // keep java.util.Date compatibility to keep things consistent
      simpleJobTrigger.setStartDay( simpleTrigger.getStartTime().getDay() );
      simpleJobTrigger.setUiPassParam( (String) job.getJobParams().get( RESERVEDMAPKEY_UIPASSPARAM ) );
      long interval = simpleTrigger.getRepeatInterval();
      if ( interval > 0 ) {
        interval /= 1000;
      }
      simpleJobTrigger.setRepeatInterval( interval );
      simpleJobTrigger.setRepeatCount( simpleTrigger.getRepeatCount() );
      job.setJobTrigger( simpleJobTrigger );
    } else if ( trigger instanceof CalendarIntervalTrigger ) {
      CalendarIntervalTrigger calendarIntervalTrigger = (CalendarIntervalTrigger) trigger;
      SimpleJobTrigger simpleJobTrigger = new SimpleJobTrigger();

      setPentahoTriggerDates( simpleJobTrigger,
        calendarIntervalTrigger.getStartTime(),
        calendarIntervalTrigger.getEndTime(),
        calendarIntervalTrigger.getTimeZone() );

      simpleJobTrigger.setUiPassParam( (String) job.getJobParams().get( RESERVEDMAPKEY_UIPASSPARAM ) );
      long interval;

      switch ( calendarIntervalTrigger.getRepeatIntervalUnit() ) {
        case SECOND:
          interval = calendarIntervalTrigger.getRepeatInterval();
          break;
        case MINUTE:
          interval = calendarIntervalTrigger.getRepeatInterval() * 60L;
          break;
        case HOUR:
          interval = calendarIntervalTrigger.getRepeatInterval() * 3600L;
          break;
        case DAY:
          interval = calendarIntervalTrigger.getRepeatInterval() * 86400L;
          break;
        default: //year == run once
          interval = -1;
          break;
      }

      simpleJobTrigger.setRepeatInterval( interval );
      simpleJobTrigger.setRepeatCount( -1 ); // field not used but previous convention was to set it to -1
      simpleJobTrigger.setTimeZone( calendarIntervalTrigger.getTimeZone().getID() );
      job.setJobTrigger( simpleJobTrigger );

    } else if ( trigger instanceof CronTrigger ) {
      CronTrigger cronTrigger = (CronTrigger) trigger;
      IComplexJobTrigger complexJobTrigger = createComplexTrigger( cronTrigger.getCronExpression() );
      complexJobTrigger.setUiPassParam( (String) job.getJobParams().get( RESERVEDMAPKEY_UIPASSPARAM ) );
      complexJobTrigger.setCronString( cronTrigger.getCronExpression() );
      List<ITimeRecurrence> timeRecurrences = parseRecurrence( complexJobTrigger.getCronString(), 3 );
      if ( !timeRecurrences.isEmpty() ) {
        ITimeRecurrence recurrence = timeRecurrences.get( 0 );
        if ( recurrence instanceof IncrementalRecurrence ) {
          IncrementalRecurrence incrementalRecurrence = (IncrementalRecurrence) recurrence;
          complexJobTrigger.setRepeatInterval( incrementalRecurrence.getIncrement() * 86400L );
        }
      } else if ( UI_PASS_PARAM_DAILY.equals( job.getJobParams().get( RESERVEDMAPKEY_UIPASSPARAM ) ) ) {
        // this is a special case; we know we have a daily schedule and the day of month field was *
        complexJobTrigger.setRepeatInterval( 86400 );
        Object startTime = job.getJobParams().get( RESERVEDMAPKEY_START_TIME );
        if ( startTime instanceof Date ) {
          complexJobTrigger.setStartTime( (Date) startTime );
        }
      }
      job.setJobTrigger( complexJobTrigger );
      if ( trigger.getCalendarName() != null ) {
        Calendar calendar = scheduler.getCalendar( trigger.getCalendarName() );
        if ( calendar instanceof QuartzSchedulerAvailability ) {
          QuartzSchedulerAvailability quartzSchedulerAvailability = (QuartzSchedulerAvailability) calendar;

          setPentahoTriggerDates( complexJobTrigger,
            quartzSchedulerAvailability.getStartTime(),
            quartzSchedulerAvailability.getEndTime(),
            cronTrigger.getTimeZone() );
        }
      }
      complexJobTrigger.setCronString( ( (CronTrigger) trigger ).getCronExpression() );
      complexJobTrigger.setTimeZone( cronTrigger.getTimeZone().toZoneId().getId() );
    }

    Trigger.TriggerState triggerState = scheduler.getTriggerState( new TriggerKey( job.getJobId(), groupName ) );
    switch ( triggerState ) {
      case NORMAL:
        job.setState( JobState.NORMAL );
        break;
      case BLOCKED:
        job.setState( JobState.BLOCKED );
        break;
      case COMPLETE:
        job.setState( JobState.COMPLETE );
        break;
      case ERROR:
        job.setState( JobState.ERROR );
        break;
      case PAUSED:
        job.setState( JobState.PAUSED );
        break;
      default:
        job.setState( JobState.UNKNOWN );
        break;
    }

    job.setJobName( QuartzJobKey.parse( job.getJobId() ).getJobName() );
    job.setNextRun( trigger.getNextFireTime() );
    job.setLastRun( getLastRun( trigger ) );

  }

  private void setPentahoTriggerDates( IJobTrigger trigger, Date start, Date end, TimeZone timeZone ) {
    ZonedDateTime startTime = ZonedDateTime.ofInstant( start.toInstant(), TimeZone.getDefault().toZoneId() );
    ZonedDateTime clientStartDate = startTime.withZoneSameInstant( timeZone.toZoneId() );

    Date triggerEndDate = end;
    if ( null != triggerEndDate ) {
      ZonedDateTime endTime = ZonedDateTime.ofInstant( triggerEndDate.toInstant(), TimeZone.getDefault().toZoneId() );
      ZonedDateTime cliendEndTime = endTime.withZoneSameInstant( timeZone.toZoneId() );
      triggerEndDate = new Date( cliendEndTime.getYear() - 1900, cliendEndTime.getMonthValue() - 1, cliendEndTime.getDayOfMonth(), cliendEndTime.getHour(), cliendEndTime.getMinute() );
    }

    Date triggerDate = new Date( clientStartDate.getYear() - 1900, clientStartDate.getMonthValue() - 1, clientStartDate.getDayOfMonth(), clientStartDate.getHour(), clientStartDate.getMinute() );
    trigger.setStartTime( triggerDate );
    trigger.setEndTime( triggerEndDate );

    trigger.setStartHour( clientStartDate.getHour() );
    trigger.setStartMin( clientStartDate.getMinute() );
    trigger.setStartYear( clientStartDate.getYear() - 1900 ); // keep java.util.Date compatibility to keep things consistent
    trigger.setStartMonth( clientStartDate.getMonth().getValue() - 1 ); // keep java.util.Date compatibility to keep things consistent
    trigger.setStartDay( clientStartDate.getDayOfMonth() );
  }

  /**
   * {@inheritDoc}
   */
  public Integer getMinScheduleInterval( IScheduleSubject subject ) {
    // Not implemented
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  public ComplexJobTrigger getSubjectAvailabilityWindow( IScheduleSubject subject ) {
    // Not implemented
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public void pause() throws SchedulerException {
    try {
      getQuartzScheduler().standby();
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException( e );
    }
  }

  /**
   * {@inheritDoc}
   */
  public void pauseJob( String jobId ) throws SchedulerException {
    try {
      Scheduler scheduler = getQuartzScheduler();
      scheduler.pauseJob( new JobKey( jobId, QuartzJobKey.parse( jobId ).getUserName() ) );
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException( Messages
        .getString( QUARTZ_SCHEDULER_ERROR_0005_FAILED_TO_PAUSE_JOBS ), e );
    }
  }

  /**
   * {@inheritDoc}
   */
  public void removeJob( String jobId ) throws SchedulerException {
    jobDetailLock.writeLock().lock();
    try {
      Scheduler scheduler = getQuartzScheduler();
      scheduler.deleteJob( new JobKey( jobId, QuartzJobKey.parse( jobId ).getUserName() ) );
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException( Messages
        .getString( QUARTZ_SCHEDULER_ERROR_0005_FAILED_TO_PAUSE_JOBS ), e );
    } finally {
      jobDetailLock.writeLock().unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void start() throws SchedulerException {
    try {
      getQuartzScheduler().start();
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException( e );
    }
  }

  /**
   * {@inheritDoc}
   */
  public void resumeJob( String jobId ) throws SchedulerException {
    try {
      Scheduler scheduler = getQuartzScheduler();
      scheduler.resumeJob( new JobKey( jobId, QuartzJobKey.parse( jobId ).getUserName() ) );
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException( Messages.getString(
        QUARTZ_SCHEDULER_ERROR_0005_FAILED_TO_RESUME_JOBS ), e );
    }
  }

  /**
   * {@inheritDoc}
   */
  public void setAvailabilityWindows( Map<IScheduleSubject, IComplexJobTrigger> availability ) {
    // Not implemented
  }

  /**
   * {@inheritDoc}
   */
  public void setMinScheduleInterval( IScheduleSubject subject, int intervalInSeconds ) {
    // Not implemented
  }

  /**
   * {@inheritDoc}
   */
  public void setSubjectAvailabilityWindow( IScheduleSubject subject, ComplexJobTrigger availability ) {
    // Not implemented
  }

  /**
   * @return
   */
  protected String getCurrentUser() {
    IPentahoSession session = PentahoSessionHolder.getSession();
    if ( session == null ) {
      return null;
    }
    Principal p = SecurityHelper.getInstance().getAuthentication();
    return ( p == null ) ? null : p.getName();
  }

  public IComplexJobTrigger createComplexTrigger( String cronExpression ) {
    ComplexJobTrigger complexJobTrigger = new ComplexJobTrigger();
    complexJobTrigger.setHourlyRecurrence( (ITimeRecurrence) null );
    complexJobTrigger.setMinuteRecurrence( (ITimeRecurrence) null );
    complexJobTrigger.setSecondRecurrence( (ITimeRecurrence) null );

    for ( ITimeRecurrence recurrence : parseRecurrence( cronExpression, 6 ) ) {
      complexJobTrigger.addYearlyRecurrence( recurrence );
    }
    for ( ITimeRecurrence recurrence : parseRecurrence( cronExpression, 4 ) ) {
      complexJobTrigger.addMonthlyRecurrence( recurrence );
    }
    List<ITimeRecurrence> dayOfWeekRecurrences = parseDayOfWeekRecurrences( cronExpression );
    List<ITimeRecurrence> dayOfMonthRecurrences = parseRecurrence( cronExpression, 3 );
    if ( !dayOfWeekRecurrences.isEmpty() && dayOfMonthRecurrences.isEmpty() ) {
      for ( ITimeRecurrence recurrence : dayOfWeekRecurrences ) {
        complexJobTrigger.addDayOfWeekRecurrence( recurrence );
      }
    } else if ( dayOfWeekRecurrences.isEmpty() && !dayOfMonthRecurrences.isEmpty() ) {
      for ( ITimeRecurrence recurrence : dayOfMonthRecurrences ) {
        complexJobTrigger.addDayOfMonthRecurrence( recurrence );
      }
    }
    for ( ITimeRecurrence recurrence : parseRecurrence( cronExpression, 2 ) ) {
      complexJobTrigger.addHourlyRecurrence( recurrence );
    }
    for ( ITimeRecurrence recurrence : parseRecurrence( cronExpression, 1 ) ) {
      complexJobTrigger.addMinuteRecurrence( recurrence );
    }
    for ( ITimeRecurrence recurrence : parseRecurrence( cronExpression, 0 ) ) {
      complexJobTrigger.addSecondRecurrence( recurrence );
    }
    return complexJobTrigger;
  }

  @Override public IComplexJobTrigger createComplexJobTrigger() {
    return new ComplexJobTrigger();
  }


  @Override
  public IComplexJobTrigger createComplexTrigger( Integer year, Integer month, Integer dayOfMonth, Integer dayOfWeek,
                                                  Integer hourOfDay ) {
    return new ComplexJobTrigger();
  }

  @Override
  public ArrayList<IJobScheduleParam> getJobParameters() {
    return new ArrayList<>();
  }

  private static List<ITimeRecurrence> parseDayOfWeekRecurrences( String cronExpression ) {
    List<ITimeRecurrence> dayOfWeekRecurrence = new ArrayList<>();
    String delims = " +";
    String[] tokens = cronExpression.split( delims );
    if ( tokens.length >= 6 ) {
      String dayOfWeekTokens = tokens[ 5 ];
      tokens = dayOfWeekTokens.split( "," );
      if ( ( tokens.length > 1 ) || !( tokens[ 0 ].equals( "*" ) || tokens[ 0 ].equals( "?" ) ) ) {
        RecurrenceList dayOfWeekList = null;
        for ( String token : tokens ) {
          if ( listPattern.matcher( token ).matches() ) {
            if ( dayOfWeekList == null ) {
              dayOfWeekList = new RecurrenceList();
            }
            dayOfWeekList.getValues().add( Integer.parseInt( token ) );
          } else {
            if ( dayOfWeekList != null ) {
              dayOfWeekRecurrence.add( dayOfWeekList );
              dayOfWeekList = null;
            }
            if ( sequencePattern.matcher( token ).matches() ) {
              String[] days = token.split( "-" );
              dayOfWeekRecurrence.add( new SequentialRecurrence( Integer.parseInt( days[ 0 ] ), Integer
                .parseInt( days[ 1 ] ) ) );
            } else if ( intervalPattern.matcher( token ).matches() ) {
              String[] days = token.split( "/" );
              dayOfWeekRecurrence.add( new IncrementalRecurrence( days[ 0 ], Integer
                .parseInt( days[ 1 ] ) ) );
            } else if ( qualifiedDayPattern.matcher( token ).matches() ) {
              String[] days = token.split( "#" );
              dayOfWeekRecurrence
                .add( new QualifiedDayOfWeek( Integer.parseInt( days[ 1 ] ), Integer.parseInt( days[ 0 ] ) ) );
            } else if ( lastDayPattern.matcher( token ).matches() ) {
              DayOfWeek dayOfWeek =
                DayOfWeek.values()[ ( Integer.parseInt( token.substring( 0, token.length() - 1 ) ) - 1 ) % 7 ];
              dayOfWeekRecurrence.add( new QualifiedDayOfWeek( DayOfWeekQualifier.LAST, dayOfWeek ) );
            } else if ( dayOfWeekRangePattern.matcher( token ).matches() ) {
              String[] days = token.split( "-" );
              int start = DayOfWeek.valueOf( days[ 0 ] ).ordinal();
              int finish = DayOfWeek.valueOf( days[ 1 ] ).ordinal();
              dayOfWeekRecurrence.add( new SequentialRecurrence( start, finish ) );
            } else {
              dayOfWeekList = new RecurrenceList();
              dayOfWeekList.getValues().add( DayOfWeek.valueOf( token ).ordinal() );
              dayOfWeekRecurrence.add( dayOfWeekList );
              dayOfWeekList = null;
            }
          }

        }
        if ( dayOfWeekList != null ) {
          dayOfWeekRecurrence.add( dayOfWeekList );
        }
      }
    } else {
      throw new IllegalArgumentException( Messages.getInstance().getErrorString(
        COMPLEX_JOB_TRIGGER_ERROR_0001_INVALID_CRON_EXPRESSION ) );
    }
    return dayOfWeekRecurrence;
  }

  private static List<ITimeRecurrence> parseRecurrence( String cronExpression, int tokenIndex ) {
    List<ITimeRecurrence> timeRecurrence = new ArrayList<>();
    String delims = " +";
    String[] tokens = cronExpression.split( delims );
    if ( tokens.length > tokenIndex ) {
      String timeTokens = tokens[ tokenIndex ];
      tokens = timeTokens.split( "," );
      if ( ( tokens.length > 1 ) || !( tokens[ 0 ].equals( "*" ) || tokens[ 0 ].equals( "?" ) ) ) {
        RecurrenceList timeList = null;
        for ( String token : tokens ) {
          if ( listPattern.matcher( token ).matches() ) {
            if ( timeList == null ) {
              timeList = new RecurrenceList();
            }
            timeList.getValues().add( Integer.parseInt( token ) );
          } else {
            if ( timeList != null ) {
              timeRecurrence.add( timeList );
              timeList = null;
            }
            if ( sequencePattern.matcher( token ).matches() ) {
              String[] days = token.split( "-" );
              timeRecurrence.add( new SequentialRecurrence( Integer.parseInt( days[ 0 ] ),
                Integer.parseInt( days[ 1 ] ) ) );
            } else if ( intervalPattern.matcher( token ).matches() ) {
              String[] days = token.split( "/" );

              timeRecurrence
                .add( new IncrementalRecurrence( days[ 0 ], Integer.parseInt( days[ 1 ] ) ) );
            } else if ( "L".equalsIgnoreCase( token ) ) {
              timeRecurrence.add( new QualifiedDayOfMonth() );
            } else {
              throw new IllegalArgumentException( Messages.getInstance().getErrorString(
                COMPLEX_JOB_TRIGGER_ERROR_0001_INVALID_CRON_EXPRESSION ) );
            }
          }

        }
        if ( timeList != null ) {
          timeRecurrence.add( timeList );
        }
      }
    } else {
      throw new IllegalArgumentException( Messages.getInstance().getErrorString(
        COMPLEX_JOB_TRIGGER_ERROR_0001_INVALID_CRON_EXPRESSION ) );
    }
    return timeRecurrence;
  }

  /**
   * Update cronTrigger's timezone based on the info from caller
   *
   * @param cronTrigger the cron trigger to update
   * @param timezone    the timezone to set
   */
  void setTimezone( CronTrigger cronTrigger, String timezone ) throws SchedulerException {
    try {
      TriggerBuilder<CronTrigger> triggerBuilder = cronTrigger.getTriggerBuilder();
      CronExpression cronEx = new CronExpression( cronTrigger.getCronExpression() );
      cronEx.setTimeZone( TimeZone.getTimeZone( timezone ) );
      triggerBuilder.withSchedule( CronScheduleBuilder.cronSchedule( cronEx ) );
    } catch ( ParseException e ) {
      throw new SchedulerException( Messages.getString(
        COMPLEX_JOB_TRIGGER_ERROR_0001_INVALID_CRON_EXPRESSION ), e );
    }
  }

  /**
   * {@inheritDoc}
   */
  public SchedulerStatus getStatus() throws SchedulerException {
    SchedulerStatus schedulerStatus = SchedulerStatus.STOPPED;
    try {
      if ( getQuartzScheduler().isInStandbyMode() ) {
        schedulerStatus = SchedulerStatus.PAUSED;
      } else if ( getQuartzScheduler().isStarted() ) {
        schedulerStatus = SchedulerStatus.RUNNING;
      }
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException( Messages.getString(
        QUARTZ_SCHEDULER_ERROR_0006_FAILED_TO_GET_SCHEDULER_STATUS ), e );
    }
    return schedulerStatus;
  }

  /**
   * {@inheritDoc}
   */
  public void shutdown() throws SchedulerException {
    try {
      getQuartzScheduler().shutdown( true );
      setQuartzScheduler( null );
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException( e );
    }
  }

  public static String prettyPrintMap( Map<String, Object> map ) {
    StringBuilder b = new StringBuilder();
    for ( Map.Entry<String, Object> entry : map.entrySet() ) {
      b.append( entry.getKey() ).append( '=' ).append( entry.getValue() ).append( "; " );
    }
    return b.toString();
  }

  public void addListener( ISchedulerListener listener ) {
    listeners.add( listener );
  }

  public void setListeners( Collection<ISchedulerListener> listeners ) {
    this.listeners.addAll( listeners );
  }

  public void fireJobCompleted( IAction actionBean, String actionUser, Map<String, Object> params,
                                IBackgroundExecutionStreamProvider streamProvider ) {
    for ( ISchedulerListener listener : listeners ) {
      listener.jobCompleted( actionBean, actionUser, params, streamProvider );
    }
  }

  @Override
  public ISimpleJobTrigger createSimpleJobTrigger( Date startTime, Date endTime, int repeatCount,
                                                             long repeatIntervalSeconds ) {
    return new SimpleJobTrigger( startTime, endTime, repeatCount, repeatIntervalSeconds );
  }

  @Override
  public ICronJobTrigger createCronJobTrigger() {
    return new CronJobTrigger();
  }

  public IJobScheduleRequest createJobScheduleRequest() {
    return new JobScheduleRequest();
  }

  public IJobScheduleParam createJobScheduleParam() {
    return new JobScheduleParam();
  }

  public ISchedulerResource createSchedulerResource() {
    return new SchedulerResource();
  }

  @Override
  public IJobRequest createJobRequest() {
    return new JobRequest();
  }

  /**
   * Checks if the text configuration for the input/output files is present.
   * If not - silently returns. If present checks if the input file is allowed to be scheduled.
   *
   * @param jobParams scheduling job parameters
   * @throws SchedulerException the configuration is recognized but the file can't be scheduled, is a folder or
   *                            doesn't exist.
   */
  @Override
  public void validateJobParams( Map<String, Object> jobParams ) throws SchedulerException {
    final Object streamProviderObj = jobParams.get( RESERVEDMAPKEY_STREAMPROVIDER );
    if ( streamProviderObj instanceof String ) {
      final String inputOutputString = (String) streamProviderObj;
      final String[] tokens = inputOutputString.split( ":" );
      if ( !ArrayUtils.isEmpty( tokens ) && tokens.length == 2 ) {
        String inputFilePath = tokens[ 0 ].split( "=" )[ 1 ].trim();
        if ( StringUtils.isNotBlank( inputFilePath ) ) {
          final IUnifiedRepository repository = PentahoSystem.get( IUnifiedRepository.class );
          final RepositoryFile repositoryFile = repository.getFile( inputFilePath );
          if ( ( repositoryFile == null ) || repositoryFile.isFolder()
             // Conversion to boolean as to avoid NPE, isSchedulable() should
             // be changed to return a boolean primitive to avoid this.
             || Boolean.TRUE.equals( !repositoryFile.isSchedulable() ) ) {
            throw new SchedulerException( Messages.getString(
              QUARTZ_SCHEDULER_ERROR_0008_SCHEDULING_IS_NOT_ALLOWED ) );
          }
        }
      }
    }
  }

  private JobDetail getJobDetail( JobKey jobKey ) throws org.quartz.SchedulerException {
      jobDetailLock.readLock().lock();
      try {
        return getQuartzScheduler().getJobDetail( jobKey );
      } finally {
        jobDetailLock.readLock().unlock();
      }
  }
}
