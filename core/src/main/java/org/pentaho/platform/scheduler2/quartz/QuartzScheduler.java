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
import org.quartz.impl.triggers.AbstractTrigger;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A Quartz implementation of {@link IScheduler}
 *
 * @author aphillips
 */
public class QuartzScheduler implements IScheduler {

  private Log logger;

  private SchedulerFactory quartzSchedulerFactory;

  private Scheduler quartzScheduler;

  private ArrayList<ISchedulerListener> listeners = new ArrayList<>();

  private static final Pattern listPattern = Pattern.compile( "\\d+" );

  private static final Pattern dayOfWeekRangePattern = Pattern.compile( ".*\\-.*" );

  private static final Pattern sequencePattern = Pattern.compile( "\\d+\\-\\d+" );

  private static final Pattern intervalPattern = Pattern.compile( "[\\d*]+/[\\d]+" );

  private static final Pattern qualifiedDayPattern = Pattern.compile( "\\d+#\\d+" );

  private static final Pattern lastDayPattern = Pattern.compile( "\\d+L" );

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
    if ( quartzScheduler != null ) {
      this.shutdown();
      quartzScheduler = null;
    }
  }

  public Scheduler getQuartzScheduler() throws org.quartz.SchedulerException {
    if ( quartzScheduler == null ) {
      /*
       * Currently, quartz will always give you the same scheduler object when any factory instance is asked for a
       * scheduler. In other words there is no such thing as scheduler-level isolation. If we really need multiple
       * isolated scheduler instances, we should investigate named schedulers, but this API getScheduler() will not help
       * us in that regard.
       */
      quartzScheduler = quartzSchedulerFactory.getScheduler();
    }

    logger.debug( "Using quartz scheduler " + quartzScheduler );
    return quartzScheduler;
  }

  private void setQuartzScheduler( Scheduler quartzScheduler ) {
    this.quartzScheduler = quartzScheduler;
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
        Messages.getInstance().getString( "QuartzScheduler.ERROR_0003_ACTION_IS_NULL" ) );
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
        Messages.getInstance().getString( "QuartzScheduler.ERROR_0003_ACTION_IS_NULL" ) );
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
    MutableTrigger quartzTrigger = null;
    java.util.Calendar startDateCal = null;
    java.util.Calendar endDateCal = null;
    Date triggerEndDate = null;
    if ( null != jobTrigger.getEndTime() ) {
      endDateCal = getEndDateCalFromTrigger( jobTrigger );
      triggerEndDate = endDateCal.getTime();
    }

    TimeZone tz = null;
    if ( null == jobTrigger ) {
      throw new SchedulerException( "jobTrigger cannot be null" );
    }

    if ( jobTrigger.getStartHour() >= 0 ) {
      // set  time zone from PUC UI input
      startDateCal = getStartDateCalFromTrigger( jobTrigger );
    } else {
      // handle legacy imports
      startDateCal = java.util.Calendar.getInstance();
      startDateCal.setTime( null != jobTrigger.getStartTime() ? jobTrigger.getStartTime() : new Date() );
    }
    if ( null != jobTrigger.getTimeZone() ) {
      tz = TimeZone.getTimeZone( jobTrigger.getTimeZone() );
    }
    if ( jobTrigger instanceof ComplexJobTrigger ) {

      try {
        ComplexJobTrigger complexJobTrigger = (ComplexJobTrigger) jobTrigger;
        CronTriggerImpl cronTrigger = new CronTriggerImpl();

        cronTrigger.setName( jobId.toString() );
        cronTrigger.setGroup( jobId.getUserName() );
        cronTrigger.setCronExpression( complexJobTrigger.getCronString() != null ? complexJobTrigger.getCronString()
          : QuartzCronStringFactory.createCronString( complexJobTrigger ) );
        if ( jobTrigger.getStartHour() >= 0 && null != tz ) {
          cronTrigger.setTimeZone( tz );
        }
        if ( null != triggerEndDate ) {
          cronTrigger.setEndTime( triggerEndDate );
        }
        quartzTrigger = cronTrigger;
      } catch ( ParseException e ) {
        throw new SchedulerException( Messages.getInstance().getString(
          "QuartzScheduler.ERROR_0001_FAILED_TO_SCHEDULE_JOB", jobId.getJobName() ), e );
      }
    } else if ( jobTrigger instanceof SimpleJobTrigger ) {
      // UIs will no longer create simple triggers, but we need to keep this for handling old exports and existing installs
      try {
        SimpleJobTrigger simpleTrigger = (SimpleJobTrigger) jobTrigger;
        long interval = simpleTrigger.getRepeatInterval();
        int triggerInterval = 0;

        DateBuilder.IntervalUnit intervalUnit = null;
        CalendarIntervalTriggerImpl calendarIntervalTrigger = new CalendarIntervalTriggerImpl();

        if ( "SECONDS".equalsIgnoreCase( jobTrigger.getUiPassParam() ) ) {
          triggerInterval = (int) interval;
          intervalUnit = DateBuilder.IntervalUnit.SECOND;
        } else if ( "MINUTES".equalsIgnoreCase( jobTrigger.getUiPassParam() ) ) {
          triggerInterval = (int) interval / 60;
          intervalUnit = DateBuilder.IntervalUnit.MINUTE;
        } else if ( "HOURS".equalsIgnoreCase( jobTrigger.getUiPassParam() ) ) {
          triggerInterval = (int) interval / 3600;
          intervalUnit = DateBuilder.IntervalUnit.HOUR;
        } else if ( "DAILY".equalsIgnoreCase( jobTrigger.getUiPassParam() ) ) {
          // "ignore DST" case; execute on multiples of 24 hours ignoring DST adjustments to time of day
          triggerInterval = (int) interval / 86400;
          intervalUnit = DateBuilder.IntervalUnit.DAY;
          calendarIntervalTrigger.setPreserveHourOfDayAcrossDaylightSavings( true );
          calendarIntervalTrigger.setSkipDayIfHourDoesNotExist( false );
        } else if ( "RUN_ONCE".equalsIgnoreCase( jobTrigger.getUiPassParam() ) ) {
          // set the repeat interval to 2 years and the end date to an hour after the start date to ensure this job only runs once
          // simpletrigger can't handle time zones and no other triggers provide a number of iterations, so this is an alternative
          triggerInterval = 2;
          intervalUnit = DateBuilder.IntervalUnit.YEAR;
          endDateCal = (java.util.Calendar) startDateCal.clone();
          endDateCal.add( java.util.Calendar.HOUR, 1 );
          triggerEndDate = endDateCal.getTime();
        }

        calendarIntervalTrigger.setRepeatInterval( triggerInterval );
        calendarIntervalTrigger.setRepeatIntervalUnit( intervalUnit );
        // Set the misfire instruction to ignore misfires. This is required due to triggerNow() requiring to
        // update the previous fire time to the current time, which Quartz does not allow.
        calendarIntervalTrigger.setMisfireInstruction( CalendarIntervalTrigger.MISFIRE_INSTRUCTION_DO_NOTHING );
        if ( null != triggerEndDate ) {
          calendarIntervalTrigger.setEndTime( triggerEndDate );
        }
        if ( jobTrigger.getStartHour() >= 0 ) {
          calendarIntervalTrigger.setStartTime( startDateCal.getTime() );
        } else {
          calendarIntervalTrigger.setStartTime( simpleTrigger.getStartTime() );
        }

        if ( null != tz ) {
          calendarIntervalTrigger.setTimeZone( tz );
        }

        quartzTrigger = calendarIntervalTrigger;
        quartzTrigger.setKey( new TriggerKey( jobId.toString(), jobId.getUserName() ) );
      } catch ( IllegalArgumentException e ) {
        throw new SchedulerException( Messages.getInstance().getString(
          "QuartzScheduler.ERROR_0001_FAILED_TO_SCHEDULE_JOB", jobId.getJobName() ), e );
      }
    } else {
      throw new SchedulerException(
        Messages.getInstance().getString( "QuartzScheduler.ERROR_0002_TRIGGER_WRONG_TYPE" ) );
    }
    return quartzTrigger;
  }

  private JobDetail createJobDetails( QuartzJobKey jobId, Map<String, Object> jobParams ) {
    jobParams.put( RESERVEDMAPKEY_ACTIONUSER, jobId.getUserName() );
    JobDataMap jobDataMap = new JobDataMap( jobParams );
    JobDetail jobDetail = JobBuilder.newJob( BlockingQuartzJob.class )
      .withIdentity( jobId.toString(), jobId.getUserName() )
      .setJobData( jobDataMap )
      .build();
    return jobDetail;
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
    //startDateCal.set( java.util.Calendar.AM_PM, jobTrigger.getStartAmPm() == 0 ? java.util.Calendar.AM : java.util.Calendar.PM );
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
    if ( jobOwner != null && jobOwner.toString().length() > 0 ) {
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

    JobDetail jobDetail = createJobDetails( jobId, jobParams );

    try {
      Scheduler scheduler = getQuartzScheduler();
      if ( triggerCalendar != null ) {
        scheduler.addCalendar( jobId.toString(), triggerCalendar, false, false );
        quartzTrigger.setCalendarName( jobId.toString() );
      }
      logger.debug(
        MessageFormat.format( "Scheduling job {0} with trigger {1} and job parameters [ {2} ]", jobId.toString(),
          trigger, prettyPrintMap( jobParams ) ) );

      if ( quartzTrigger instanceof CronTrigger ) {
        Serializable timezone = (Serializable) jobParams.get( "timezone" );
        if ( timezone != null ) {
          setTimezone( (CronTrigger) quartzTrigger, timezone.toString() );
        }
      }

      scheduler.scheduleJob( jobDetail, quartzTrigger );

      logger.debug( MessageFormat.format( "Scheduled job {0} successfully", jobId.toString() ) );
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException( Messages.getInstance().getString(
        "QuartzScheduler.ERROR_0001_FAILED_TO_SCHEDULE_JOB", jobName ), e );
    }

    Job job = new Job();
    job.setJobParams( jobParams );
    job.setJobTrigger( (JobTrigger) trigger );
    job.setNextRun( quartzTrigger.getNextFireTime() );
    job.setLastRun( quartzTrigger.getPreviousFireTime() );
    job.setJobId( jobId.toString() );
    job.setJobName( jobName );
    job.setUserName( curUser );
    job.setState( JobState.NORMAL );

    return job;
  }

  @Override
  public void updateJob( String jobId, Map<String, Object> jobParams, IJobTrigger trigger )
    throws SchedulerException {
    QuartzJobKey jobKey = QuartzJobKey.parse( jobId );

    MutableTrigger quartzTrigger = createQuartzTrigger( trigger, jobKey );
    quartzTrigger.setJobKey( JobKey.jobKey( jobId, jobKey.getUserName() ) );

    Calendar triggerCalendar =
      quartzTrigger instanceof CronTrigger ? createQuartzCalendar( (ComplexJobTrigger) trigger ) : null;

    try {
      Scheduler scheduler = getQuartzScheduler();
      JobDetail origJobDetail = scheduler.getJobDetail( JobKey.jobKey( jobId, jobKey.getUserName() ) );
      if ( origJobDetail.getJobDataMap().containsKey( IScheduler.RESERVEDMAPKEY_ACTIONCLASS ) ) {
        jobParams.put( IScheduler.RESERVEDMAPKEY_ACTIONCLASS,
          origJobDetail.getJobDataMap().get( IScheduler.RESERVEDMAPKEY_ACTIONCLASS )
            .toString() );
      } else if ( origJobDetail.getJobDataMap().containsKey( RESERVEDMAPKEY_ACTIONID ) ) {
        jobParams
          .put( RESERVEDMAPKEY_ACTIONID, origJobDetail.getJobDataMap().get( RESERVEDMAPKEY_ACTIONID ).toString() );
      }

      if ( origJobDetail.getJobDataMap().containsKey( RESERVEDMAPKEY_STREAMPROVIDER ) ) {
        jobParams.put( RESERVEDMAPKEY_STREAMPROVIDER, origJobDetail.getJobDataMap().get(
          RESERVEDMAPKEY_STREAMPROVIDER ) );
      }
      if ( origJobDetail.getJobDataMap().containsKey( RESERVEDMAPKEY_UIPASSPARAM ) ) {
        jobParams.put( RESERVEDMAPKEY_UIPASSPARAM, origJobDetail.getJobDataMap().get(
          RESERVEDMAPKEY_UIPASSPARAM ) );
      }

      JobDetail jobDetail = createJobDetails( jobKey, jobParams );
      scheduler.addJob( jobDetail, true );
      if ( triggerCalendar != null ) {
        scheduler.addCalendar( jobId, triggerCalendar, true, true );
        quartzTrigger.setCalendarName( jobId );
      }

      if ( quartzTrigger instanceof CronTrigger ) {
        Serializable timezone = (Serializable) jobParams.get( "timezone" );
        if ( timezone != null ) {
          setTimezone( (CronTrigger) quartzTrigger, timezone.toString() );
        }
      }

      new TriggerKey( jobId, jobKey.getUserName() );
      scheduler.rescheduleJob( new TriggerKey( jobId, jobKey.getUserName() ), quartzTrigger );
      logger
        .debug( MessageFormat
          .format(
            "Scheduling job {0} with trigger {1} and job parameters [ {2} ]", jobId.toString(), trigger,
            prettyPrintMap( jobParams ) ) ); //$NON-NLS-1$
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException( Messages.getInstance().getString(
        "QuartzScheduler.ERROR_0001_FAILED_TO_SCHEDULE_JOB", jobKey.getJobName() ), e );
    }
  }

  /**
   * {@inheritDoc}
   */
  public Map<IScheduleSubject, IComplexJobTrigger> getAvailabilityWindows() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public List<IJobResult> getJobHistory( String jobId ) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public void triggerNow( String jobId ) throws SchedulerException {
    try {
      QuartzJobKey jobKey = QuartzJobKey.parse( jobId );
      Scheduler scheduler = getQuartzScheduler();
      String groupName = jobKey.getUserName();
      for ( Trigger trigger : scheduler.getTriggersOfJob( new JobKey( jobId, groupName ) ) ) {
        // triggerJob below causes quartz to make a new trigger starting with MT_ internally.  Ignore those.
        if ( isManualTrigger( trigger ) ) {
          continue;
        }

        if ( !previousFireTimeInMisfireWindow( trigger ) ) {
          AbstractTrigger<?> abstractTrigger = (AbstractTrigger<?>) trigger;
          // Update trigger with the execution date
          //   this ensures the Last Run column shows this manual execution
          abstractTrigger.setPreviousFireTime( new Date() );
          // Reschedule the original trigger to update the previous fire time
          //   this does not cause the job to run as long as we are not inside the misfire window
          scheduler.rescheduleJob( trigger.getKey(), trigger );
        }

        // Execute the job
        scheduler.triggerJob( new JobKey( jobId, groupName ) );
      }
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException( Messages.getInstance().getString(
        "QuartzScheduler.ERROR_0007_FAILED_TO_GET_JOB", jobId ), e );
    }
  }

  private boolean previousFireTimeInMisfireWindow( Trigger trigger ) throws org.quartz.SchedulerException {
    Scheduler scheduler = getQuartzScheduler();
    long misfireThresholdMillis = getMisfireThresholdMillis( scheduler );
    long currentTime = System.currentTimeMillis();
    long previousFireTime;

    if ( ( trigger instanceof CalendarIntervalTrigger || trigger instanceof ComplexJobTrigger )
        && trigger.getNextFireTime() != null ) {
      // previous fire time is next fire time minus repeat interval, so that it's not affected by manual triggers
      // this is only possible for instances of triggers that have a repeat interval, not CronTriggers for example
      previousFireTime = trigger.getNextFireTime().getTime() - getRepeatIntervalMillis( trigger );
    } else if ( trigger.getPreviousFireTime() != null ) {
      previousFireTime = trigger.getPreviousFireTime().getTime();
    } else {
      // if the trigger has never fired, we don't want to consider it in the misfire window
      return false;
    }

    return currentTime - previousFireTime < misfireThresholdMillis;
  }

  private static long getMisfireThresholdMillis( Scheduler scheduler ) throws org.quartz.SchedulerException {
    String misfireThreshold = (String) scheduler.getContext().get( "org.quartz.jobStore.misfireThreshold" );
    return misfireThreshold == null ? 60000 : Long.parseLong( misfireThreshold );
  }

  private static long getRepeatIntervalMillis( Trigger trigger ) {
    if ( trigger instanceof CalendarIntervalTrigger ) {
      CalendarIntervalTrigger calendarIntervalTrigger = (CalendarIntervalTrigger) trigger;
      DateBuilder.IntervalUnit intervalUnit = calendarIntervalTrigger.getRepeatIntervalUnit();
      int repeatInterval = calendarIntervalTrigger.getRepeatInterval();
      switch ( intervalUnit ) {
        case SECOND:
          return repeatInterval * 1000L;
        case MINUTE:
          return repeatInterval * 60 * 1000L;
        case HOUR:
          return repeatInterval * 60 * 60 * 1000L;
        case DAY:
          return repeatInterval * 24 * 60 * 60 * 1000L;
        default:
          return 0;
      }
    } else {
      return 0;
    }
  }

  /**
   * Indicates if this trigger was created by quartz internally as a result of a triggerJob call
   * @param trigger
   * @return
   */
  private boolean isManualTrigger( Trigger trigger ) {
    return null != trigger.getKey() && null != trigger.getKey().getName() && trigger.getKey().getName().startsWith( "MT_" );
  }

  @Override public void setSubjectAvailabilityWindow( IScheduleSubject subject, IComplexJobTrigger window ) {

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
      for ( Trigger trigger : scheduler.getTriggersOfJob( jobKey ) ) {
        Job job = new Job();
        JobDetail jobDetail = scheduler.getJobDetail( jobKey );
        if ( jobDetail != null ) {
          JobDataMap jobDataMap = jobDetail.getJobDataMap();
          if ( jobDataMap != null ) {
            Map<String, Object> wrappedMap = jobDataMap.getWrappedMap();
            job.setJobParams( wrappedMap );
          }
        }

        job.setJobId( jobId );
        setJobTrigger( scheduler, job, trigger );
        job.setUserName( jobDetail.getKey().getGroup() );
        return job;
      }
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException( Messages.getInstance().getString(
        "QuartzScheduler.ERROR_0007_FAILED_TO_GET_JOB", jobId ), e );
    }
    return null;
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
          for ( Trigger trigger : scheduler.getTriggersOfJob( jobKey ) ) {
            if ( isManualTrigger( trigger ) ) {
              continue;
            }
            Job job = new Job();
            job.setGroupName( groupName );
            JobDetail jobDetail = scheduler.getJobDetail( jobKey );
            if ( jobDetail != null ) {
              job.setUserName( jobDetail.getKey().getGroup() );
              JobDataMap jobDataMap = jobDetail.getJobDataMap();
              if ( jobDataMap != null ) {
                Map<String, Object> wrappedMap = jobDataMap.getWrappedMap();
                job.setJobParams( wrappedMap );
              }
            }

            job.setJobId( jobId );
            setJobTrigger( scheduler, job, trigger );
            job.setJobName( QuartzJobKey.parse( jobId ).getJobName() );
            setJobNextRun( job, trigger );
            job.setLastRun( trigger.getPreviousFireTime() );
            if ( ( filter == null ) || filter.accept( job ) ) {
              jobs.add( job );
            }
          }
        }
      }
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException(
        Messages.getInstance().getString( "QuartzScheduler.ERROR_0004_FAILED_TO_LIST_JOBS" ), e );
    }
    return jobs;
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
      long interval = 0l;

      switch ( calendarIntervalTrigger.getRepeatIntervalUnit() ) {
        case SECOND:
          interval = calendarIntervalTrigger.getRepeatInterval();
          break;
        case MINUTE:
          interval = calendarIntervalTrigger.getRepeatInterval() * 60;
          break;
        case HOUR:
          interval = calendarIntervalTrigger.getRepeatInterval() * 3600;
          break;
        case DAY:
          interval = calendarIntervalTrigger.getRepeatInterval() * 86400;
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
          complexJobTrigger.setRepeatInterval( incrementalRecurrence.getIncrement() * 86400 );
        }
      } else if ( "DAILY".equals( job.getJobParams().get( RESERVEDMAPKEY_UIPASSPARAM ) ) ) {
        // this is a special case; we know we have a daily schedule and the day of month field was *
        complexJobTrigger.setRepeatInterval( 86400 );
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
    job.setLastRun( trigger.getPreviousFireTime() );

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
    //trigger.setStartAmPm( clientStartDate.getHour() >= 12 ? 1: 0 );
  }

  /**
   * {@inheritDoc}
   */
  public Integer getMinScheduleInterval( IScheduleSubject subject ) {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  public ComplexJobTrigger getSubjectAvailabilityWindow( IScheduleSubject subject ) {
    // TODO Auto-generated method stub
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
      throw new SchedulerException( Messages.getInstance()
        .getString( "QuartzScheduler.ERROR_0005_FAILED_TO_PAUSE_JOBS" ), e );
    }
  }

  /**
   * {@inheritDoc}
   */
  public void removeJob( String jobId ) throws SchedulerException {
    try {
      Scheduler scheduler = getQuartzScheduler();
      scheduler.deleteJob( new JobKey( jobId, QuartzJobKey.parse( jobId ).getUserName() ) );
    } catch ( org.quartz.SchedulerException e ) {
      throw new SchedulerException( Messages.getInstance()
        .getString( "QuartzScheduler.ERROR_0005_FAILED_TO_PAUSE_JOBS" ), e );
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
      throw new SchedulerException( Messages.getInstance().getString(
        "QuartzScheduler.ERROR_0005_FAILED_TO_RESUME_JOBS" ), e );
    }
  }

  /**
   * {@inheritDoc}
   */
  public void setAvailabilityWindows( Map<IScheduleSubject, IComplexJobTrigger> availability ) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   */
  public void setMinScheduleInterval( IScheduleSubject subject, int intervalInSeconds ) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   */
  public void setSubjectAvailabilityWindow( IScheduleSubject subject, ComplexJobTrigger availability ) {
    // TODO Auto-generated method stub

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

  @Override public ArrayList<IJobScheduleParam> getJobParameters() {
    return null;
  }

  private static List<ITimeRecurrence> parseDayOfWeekRecurrences( String cronExpression ) {
    List<ITimeRecurrence> dayOfWeekRecurrence = new ArrayList<>();
    String delims = "[ ]+";
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
        "ComplexJobTrigger.ERROR_0001_InvalidCronExpression" ) );
    }
    return dayOfWeekRecurrence;
  }

  private static List<ITimeRecurrence> parseRecurrence( String cronExpression, int tokenIndex ) {
    List<ITimeRecurrence> timeRecurrence = new ArrayList<>();
    String delims = "[ ]+";
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
                "ComplexJobTrigger.ERROR_0001_InvalidCronExpression" ) );
            }
          }

        }
        if ( timeList != null ) {
          timeRecurrence.add( timeList );
        }
      }
    } else {
      throw new IllegalArgumentException( Messages.getInstance().getErrorString(
        "ComplexJobTrigger.ERROR_0001_InvalidCronExpression" ) );
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
      TriggerBuilder triggerBuilder = cronTrigger.getTriggerBuilder();
      CronExpression cronEx = new CronExpression( cronTrigger.getCronExpression() );
      cronEx.setTimeZone( TimeZone.getTimeZone( timezone ) );
      triggerBuilder.withSchedule( CronScheduleBuilder.cronSchedule( cronEx ) );
    } catch ( ParseException e ) {
      throw new SchedulerException( Messages.getInstance().getString(
        "ComplexJobTrigger.ERROR_0001_InvalidCronExpression" ), e );
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
      throw new SchedulerException( Messages.getInstance().getString(
        "QuartzScheduler.ERROR_0006_FAILED_TO_GET_SCHEDULER_STATUS" ), e );
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

  @Override public ISimpleJobTrigger createSimpleJobTrigger( Date startTime, Date endTime, int repeatCount,
                                                             long repeatIntervalSeconds ) {
    return new SimpleJobTrigger( startTime, endTime, repeatCount, repeatIntervalSeconds );
  }

  @Override public ICronJobTrigger createCronJobTrigger() {
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

  @Override public IJobRequest createJobRequest() {
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
  @Override public void validateJobParams( Map<String, Object> jobParams ) throws SchedulerException {
    final Object streamProviderObj = jobParams.get( RESERVEDMAPKEY_STREAMPROVIDER );
    if ( streamProviderObj instanceof String ) {
      final String inputOutputString = (String) streamProviderObj;
      final String[] tokens = inputOutputString.split( ":" );
      if ( !ArrayUtils.isEmpty( tokens ) && tokens.length == 2 ) {
        String inputFilePath = tokens[ 0 ].split( "=" )[ 1 ].trim();
        if ( StringUtils.isNotBlank( inputFilePath ) ) {
          final IUnifiedRepository repository = PentahoSystem.get( IUnifiedRepository.class );
          final RepositoryFile repositoryFile = repository.getFile( inputFilePath );
          if ( ( repositoryFile == null ) || repositoryFile.isFolder() || !repositoryFile.isSchedulable() ) {
            throw new SchedulerException( Messages.getInstance().getString(
              "QuartzScheduler.ERROR_0008_SCHEDULING_IS_NOT_ALLOWED" ) );
          }
        }
      }
    }
  }
}
