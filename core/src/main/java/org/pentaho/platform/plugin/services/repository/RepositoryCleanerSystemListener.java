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


package org.pentaho.platform.plugin.services.repository;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPluginLifecycleListener;
import org.pentaho.platform.api.engine.PluginLifecycleException;
import org.pentaho.platform.api.scheduler2.IComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.IJob;
import org.pentaho.platform.api.scheduler2.IJobFilter;
import org.pentaho.platform.api.scheduler2.IJobTrigger;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * This is a 5.4-only class. To use it, update <tt>systemListeners.xml</tt> by adding the following section:
 * <pre>
 * &lt;bean id="repositoryCleanerSystemListener"
 * class="org.pentaho.platform.plugin.services.repository.RepositoryCleanerSystemListener"&gt;
 * &lt;property name="gcEnabled" value="true"/&gt;
 * &lt;property name="execute" value="now"/&gt;
 * &lt;/bean&gt;
 * </pre>
 * <tt>gcEnabled</tt> is a non-mandatory parameter, <tt>true</tt> by default. Use it to turn off the listener without
 * removing its description from the XML-file
 * <tt>execute</tt> is a parameter, that describes a time pattern of GC procedure. Supported values are:
 * <ul>
 *   <li><tt>now</tt> - for one time execution</li>
 *   <li><tt>weekly</tt> - for every Monday execution</li>
 *   <li><tt>monthly</tt> - for every first day of month execution</li>
 * </ul>
 * Note, that periodic executions will be planned to start at 0:00. If an execution was not started at that time,
 * e.g. the server was shut down, then it will be started as soon as the scheduler is restored.
 *
 * @author Andrey Khayrutdinov
 */
public class RepositoryCleanerSystemListener implements IPluginLifecycleListener, IJobFilter {

  private final Log logger = LogFactory.getLog( RepositoryCleanerSystemListener.class );
  String RESERVEDMAPKEY_ACTIONUSER = "ActionAdapterQuartzJob-ActionUser";

  @Override
  public void init() throws PluginLifecycleException {
    this.startup(PentahoSessionHolder.getSession());
  }

  @Override
  public void loaded() throws PluginLifecycleException {

  }

  @Override
  public void unLoaded() throws PluginLifecycleException {
    this.shutdown();
  }

  public enum Frequency {
    NOW( "now" ) {
      @Override public IJobTrigger createTrigger() {
        IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", null );
        IJobTrigger trigger = scheduler.createSimpleJobTrigger( new Date(),
                null, 0, 1 );
        trigger.setUiPassParam( "RUN_ONCE" );
        return trigger;
      }
    },

    WEEKLY( "weekly" ) {
      @Override public IJobTrigger createTrigger() {
        IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", null );
        // execute each first day of week at 0 hours
        IJobTrigger trigger = scheduler.createComplexTrigger( null, null, null, IComplexJobTrigger.SUNDAY, 0 );
        trigger.setUiPassParam( "WEEKLY" );
        trigger.setStartTime( new Date() );
        trigger.setCronString( "0 0,0 0 ? * 1 *" );
        return trigger;
      }
    },

    MONTHLY( "monthly" ) {
      @Override public IJobTrigger createTrigger() {
        IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", null );

        // execute each first day of month at 0 hours
        IJobTrigger trigger = scheduler.createComplexTrigger( null, null, 1, null, 0 );
        trigger.setUiPassParam( "MONTHLY" );
        trigger.setStartTime( new Date() );
        trigger.setCronString( "0 0,0 0 1 * ? *" );
        return trigger;
      }
    };

    private final String value;

    Frequency( String value ) {
      this.value = value;
    }

    public abstract IJobTrigger createTrigger();

    public static Frequency fromString( String name ) {
      for ( Frequency frequency : values() ) {
        if ( frequency.value.equalsIgnoreCase( name ) ) {
          return frequency;
        }
      }
      return null;
    }

    public String getValue() {
      return value;
    }
  }

  private boolean gcEnabled = true;
  private String execute;

  public boolean startup( IPentahoSession session ) {
    IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", session );
    if ( scheduler == null ) {
      logger.error( "Cannot obtain an instance of IScheduler2" );
      return false;
    }

    try {
      List<IJob> jobs = scheduler.getJobs( this );
      if ( gcEnabled ) {
        if ( jobs.isEmpty() ) {
          scheduleJob( scheduler );
        } else {
          rescheduleIfNecessary( scheduler, jobs );
        }
      } else {
        if ( !jobs.isEmpty() ) {
          unscheduleJob( scheduler, jobs );
        }
      }
    } catch ( SchedulerException e ) {
      logger.error( "Scheduler error", e );
    }
    return true;
  }

  private IJobTrigger findJobTrigger() {
    if ( StringUtil.isEmpty( execute ) ) {
      logger.error( "\"execute\" property is not specified!" );
      return null;
    }

    Frequency frequency = Frequency.fromString( execute );
    if ( frequency == null ) {
      logger.error( "Unknown value for property \"execute\": " + execute );
      return null;
    }

    return frequency.createTrigger();
  }

  private void scheduleJob( IScheduler scheduler ) throws SchedulerException {
    IJobTrigger trigger = findJobTrigger();
    if ( trigger != null ) {
      logger.info( "Creating new job with trigger: " + trigger );
      // Load the repository.spring.properties and extract singleTenantAdminUserName
      final String username = StringUtils.defaultIfEmpty( PentahoSystem.get( String.class
        , "singleTenantAdminUserName", null ), "admin" );
      HashMap<String, Object> parameterMap = new HashMap<>();
      parameterMap.put(RESERVEDMAPKEY_ACTIONUSER, username);
      scheduler.createJob( RepositoryGcJob.JOB_NAME, RepositoryGcJob.class, parameterMap, trigger );
    }
  }

  private void rescheduleIfNecessary( IScheduler scheduler, List<IJob> jobs ) throws SchedulerException {
    IJobTrigger trigger = findJobTrigger();
    if ( trigger == null ) {
      return;
    }

    List<IJob> matched = new ArrayList<IJob>( jobs.size() );
    for ( IJob job : jobs ) {
      IJobTrigger tr = job.getJobTrigger();
      // unfortunately, JobTrigger does not override equals
      if ( !trigger.getUiPassParam().equalsIgnoreCase( tr.getUiPassParam() ) ) {
        logger.info( "Removing job with id: " + job.getJobId() );
        scheduler.removeJob( job.getJobId() );
      } else {
        matched.add( job );
      }
    }

    if ( matched.isEmpty() ) {
      logger.info( "Need to re-schedule job" );
      scheduleJob( scheduler );
    }
  }

  private void unscheduleJob( IScheduler scheduler, List<IJob> jobs ) throws SchedulerException {
    for ( IJob job : jobs ) {
      logger.info( "Removing job with id: " + job.getJobId() );
      scheduler.removeJob( job.getJobId() );
    }
  }


  public void shutdown() {
    // nothing to do
  }

  @Override
  public boolean accept( IJob job ) {
    return RepositoryGcJob.JOB_NAME.equals( job.getJobName() );
  }


  public boolean isGcEnabled() {
    return gcEnabled;
  }

  public void setGcEnabled( boolean gcEnabled ) {
    this.gcEnabled = gcEnabled;
  }

  public String getExecute() {
    return execute;
  }

  public void setExecute( String execute ) {
    this.execute = execute;
  }
}
