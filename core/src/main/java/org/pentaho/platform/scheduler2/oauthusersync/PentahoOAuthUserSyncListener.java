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

package com.pentaho.platform.scheduler2.oauthusersync;

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

public class PentahoOAuthUserSyncListener implements IPluginLifecycleListener, IJobFilter {

  private final Log logger = LogFactory.getLog( PentahoOAuthUserSyncListener.class );

  String RESERVEDMAPKEY_ACTIONUSER = "ActionAdapterQuartzJob-ActionUser";

  private boolean userSyncEnabled = true;

  private String execute;

  public boolean isUserSyncEnabled() {
    return userSyncEnabled;
  }

  public void setUserSyncEnabled( boolean userSyncEnabled ) {
    this.userSyncEnabled = userSyncEnabled;
  }

  public String getExecute() {
    return execute;
  }

  public void setExecute( String execute ) {
    this.execute = execute;
  }

  @Override
  public void init() throws PluginLifecycleException {
    this.startup( PentahoSessionHolder.getSession() );
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
        IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", PentahoSessionHolder.getSession() );
        IJobTrigger trigger = scheduler.createSimpleJobTrigger( new Date(),
                null, 0, 1 );
        trigger.setUiPassParam( "RUN_ONCE" );
        return trigger;
      }
    },

    EVERY_MONDAY( "weekly" ) {
      @Override public IJobTrigger createTrigger() {
        IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", PentahoSessionHolder.getSession() );
        // execute each first day of week at 0 hours
        IJobTrigger trigger = scheduler.createComplexTrigger( null, null, null, IComplexJobTrigger.SUNDAY, 0 );
        trigger.setUiPassParam( "WEEKLY" );
        trigger.setStartTime( new Date() );
        trigger.setCronString( "0 0,0 0 ? * 1 *" );
        return trigger;
      }
    },

    EVERY_5_MINS( "every_5_mins" ) {
      @Override public IJobTrigger createTrigger() {
        IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", PentahoSessionHolder.getSession() );

        // execute each first day of month at 0 hours
        IJobTrigger trigger = scheduler.createComplexTrigger( null, null, null, null, 0 );
        trigger.setUiPassParam( "EVERY_5_MINS" );
        trigger.setStartTime( new Date() );
        trigger.setCronString( "0 0/5 * 1/1 * ? *" );
        return trigger;
      }
    },

    EVERY_3_HRS( "every_3_hrs" ) {
      @Override public IJobTrigger createTrigger() {
        IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", PentahoSessionHolder.getSession() );

        // execute each first day of month at 0 hours
        IJobTrigger trigger = scheduler.createComplexTrigger( null, null, null, null, 0 );
        trigger.setUiPassParam( "EVERY_3_HRS" );
        trigger.setStartTime( new Date() );
        trigger.setCronString( "0 0 */3 * * ?" );
        return trigger;
      }
    },

    EVERY_6_HRS( "every_6_hrs" ) {
      @Override public IJobTrigger createTrigger() {
        IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", PentahoSessionHolder.getSession() );

        // execute each first day of month at 0 hours
        IJobTrigger trigger = scheduler.createComplexTrigger( null, null, null, null, 0 );
        trigger.setUiPassParam( "EVERY_6_HRS" );
        trigger.setStartTime( new Date() );
        trigger.setCronString( "0 0 */6 * * ?" );
        return trigger;
      }
    },

    EVERY_12_HRS( "every_12_hrs" ) {
      @Override public IJobTrigger createTrigger() {
        IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", PentahoSessionHolder.getSession() );

        // execute each first day of month at 0 hours
        IJobTrigger trigger = scheduler.createComplexTrigger( null, null, null, null, 0 );
        trigger.setUiPassParam( "EVERY_12_HRS" );
        trigger.setStartTime( new Date() );
        trigger.setCronString( "0 0 */12 * * ?" );
        return trigger;
      }
    },

    EVERY_DAY( "every_day" ) {
      @Override public IJobTrigger createTrigger() {
        IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", PentahoSessionHolder.getSession() );

        // execute each first day of month at 0 hours
        IJobTrigger trigger = scheduler.createComplexTrigger( null, null, null, null, 0 );
        trigger.setUiPassParam( "EVERY_DAY" );
        trigger.setStartTime( new Date() );
        trigger.setCronString( "0 0 0 * * ?" );
        return trigger;
      }
    },

    ALTERNATE_DAY( "alternate_day" ) {
      @Override public IJobTrigger createTrigger() {
        IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", PentahoSessionHolder.getSession() );

        // execute each first day of month at 0 hours
        IJobTrigger trigger = scheduler.createComplexTrigger( null, null, null, null, 0 );
        trigger.setUiPassParam( "ALTERNATE_DAY" );
        trigger.setStartTime( new Date() );
        trigger.setCronString( "0 0 0 */2 * ?" );
        return trigger;
      }
    };

    private final String value;

    Frequency( String value ) {
      this.value = value;
    }

    public abstract IJobTrigger createTrigger();

    public static PentahoOAuthUserSyncListener.Frequency fromString(String name ) {
      for ( PentahoOAuthUserSyncListener.Frequency frequency : values() ) {
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

  public boolean startup( IPentahoSession session ) {
    IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", session );
    if ( scheduler == null ) {
      logger.error( "Cannot obtain an instance of IScheduler2" );
      return false;
    }

    try {
      List<IJob> jobs = scheduler.getJobs( this );
      if ( userSyncEnabled ) {
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

    PentahoOAuthUserSyncListener.Frequency frequency = PentahoOAuthUserSyncListener.Frequency.fromString( execute );
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
              , "singleTenantAdminUserName", PentahoSessionHolder.getSession() ), "admin" );
      HashMap<String, Object> parameterMap = new HashMap<>();
      parameterMap.put( RESERVEDMAPKEY_ACTIONUSER, username );
      scheduler.createJob( PentahoOAuthUserSyncJob.JOB_NAME, PentahoOAuthUserSyncJob.class, parameterMap, trigger );
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
    return PentahoOAuthUserSyncJob.JOB_NAME.equals( job.getJobName() );
  }

}
