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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.scheduler2.IBlockoutManager;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.engine.core.audit.AuditHelper;
import org.pentaho.platform.engine.core.audit.MDCUtil;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.scheduler2.blockout.BlockoutAction;
import org.pentaho.platform.scheduler2.blockout.PentahoBlockoutManager;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

/**
 * A Quartz job that checks if execution is currently suspended before passing on to the underlying job
 * 
 * @author kwalker
 */
public class BlockingQuartzJob implements Job {
  public void execute( final JobExecutionContext jobExecutionContext ) throws JobExecutionException {
    JobDataMap jobDataMap = null;
    if ( jobExecutionContext.getJobDetail() != null && jobExecutionContext.getJobDetail().getJobDataMap() != null ) {
      jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
    }
    boolean jobRestarted = false;
    if ( jobDataMap != null ) {
      MDCUtil.setupSchedulerMDC( jobDataMap.get( QuartzScheduler.RESERVEDMAPKEY_ACTIONUSER ), jobDataMap.get(
          "lineage-id" ) );
      jobRestarted = jobDataMap.getBooleanValue( QuartzScheduler.RESERVEDMAPKEY_RESTART_FLAG );
    }
    String messageType = jobRestarted ? MessageTypes.RECREATED_INSTANCE_START : MessageTypes.INSTANCE_START;
    long start = System.currentTimeMillis();
    long end = start;
    try {
      if ( getBlockoutManager().shouldFireNow() || isBlockoutAction( jobExecutionContext ) ) { // We should always let the blockouts fire
        makeAuditRecord( 0, messageType, jobExecutionContext );
        // Record the actual execution time - this ensures Last Run is updated ONLY when the job actually executes
        recordExecutionTime( jobExecutionContext );
        createUnderlyingJob().execute( jobExecutionContext );
        end = System.currentTimeMillis();
        messageType = jobRestarted ? MessageTypes.RECREATED_INSTANCE_END : MessageTypes.INSTANCE_END;
      } else {
        getLogger().warn(
            "Job '" + jobExecutionContext.getJobDetail().getKey().getName()
                + "' attempted to run during a blockout period.  This job was not executed" );
      }
    } catch ( ActionAdapterQuartzJob.LoggingJobExecutionException le ) {
      // thrown by the execution code - if execution fails, there only thing we do is to write to pro_audit table failing message,
      // no point in trying to execute the job again
      end = System.currentTimeMillis();
      messageType = jobRestarted ? MessageTypes.RECREATED_INSTANCE_FAILED : MessageTypes.INSTANCE_FAILED;
    } catch ( SchedulerException e ) {
      end = System.currentTimeMillis();
      messageType = jobRestarted ? MessageTypes.RECREATED_INSTANCE_FAILED : MessageTypes.INSTANCE_FAILED;
      getLogger().warn(
          "Got Exception retrieving the Blockout Manager for job '" + jobExecutionContext.getJobDetail().getKey().getName()
              + "'. Executing the underlying job anyway", e );
      createUnderlyingJob().execute( jobExecutionContext );
      end = System.currentTimeMillis();
      messageType = jobRestarted ? MessageTypes.RECREATED_INSTANCE_END : MessageTypes.INSTANCE_END;
    } finally {
      makeAuditRecord( ( (float) ( end - start ) / 1000 ), messageType, jobExecutionContext );
    }
  }

  IBlockoutManager getBlockoutManager() throws SchedulerException {
    return new PentahoBlockoutManager();
  }

  Job createUnderlyingJob() {
    return new ActionAdapterQuartzJob();
  }

  Log getLogger() {
    return LogFactory.getLog( BlockingQuartzJob.class );
  }

  protected boolean isBlockoutAction( JobExecutionContext ctx ) {
    try {
      String actionClass = ctx.getJobDetail().getJobDataMap().getString( QuartzScheduler.RESERVEDMAPKEY_ACTIONCLASS );
      return BlockoutAction.class.getName().equals( actionClass );
    } catch ( Throwable t ) {
      getLogger().warn( t.getMessage(), t );
      return false;
    }
  }

  protected void makeAuditRecord( final float time, final String messageType,
                                  final JobExecutionContext jobExecutionContext ) {
    if ( jobExecutionContext != null && jobExecutionContext.getJobDetail() != null ) {
      final JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();

      if ( null == jobDataMap || null == jobDataMap.get( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER ) || null == jobDataMap.get( QuartzScheduler.RESERVEDMAPKEY_ACTIONID ) ) {
        //it's an action, no need to log
        return;
      }

      AuditHelper.audit( PentahoSessionHolder.getSession() != null ? PentahoSessionHolder.getSession().getId() : "",
        jobDataMap.get( QuartzScheduler.RESERVEDMAPKEY_ACTIONUSER ) != null ? jobDataMap.get( QuartzScheduler.RESERVEDMAPKEY_ACTIONUSER ).toString() : "",
        jobDataMap.get( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER ) != null ? jobDataMap.get( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER ).toString() : "",
        jobExecutionContext.getJobDetail().getJobClass() != null ? jobExecutionContext.getJobDetail().getJobClass().getName() : "",
        jobDataMap.get( QuartzScheduler.RESERVEDMAPKEY_ACTIONID ) != null ? jobDataMap.get( QuartzScheduler.RESERVEDMAPKEY_ACTIONID ).toString() : "",
        messageType,
        jobDataMap.get( "lineage-id" ) != null ? jobDataMap.get( "lineage-id" ).toString() : "",
        null,
        time,
        null ); //$NON-NLS-1$
    }
  }

  /**
   * Records the execution time for the job. This ensures that the Last Run field
   * is only updated when the job actually executes, not when it's blocked by blockout periods.
   *
   * Note: This operation is non-critical. If the scheduler is unavailable or not a QuartzScheduler instance,
   * execution time recording is silently skipped, but job execution continues normally. This allows jobs to
   * execute successfully even in non-standard scheduler configurations.
   *
   * @param jobExecutionContext the job execution context containing job details
   */
  protected void recordExecutionTime( JobExecutionContext jobExecutionContext ) {
    try {
      if ( jobExecutionContext != null && jobExecutionContext.getJobDetail() != null ) {
        IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", null );
        if ( scheduler instanceof QuartzScheduler ) {
          QuartzScheduler quartzScheduler = (QuartzScheduler) scheduler;
          quartzScheduler.saveExecutionDate( jobExecutionContext.getJobDetail().getKey(), new java.util.Date( System.currentTimeMillis() ) );
        }
      }
    } catch ( org.quartz.SchedulerException e ) {
      getLogger().warn( "Failed to record execution time for job '" + jobExecutionContext.getJobDetail().getKey().getName() + "'", e );
    } catch ( Exception e ) {
      getLogger().warn( "Unexpected error recording execution time for job", e );
    }
  }
}
