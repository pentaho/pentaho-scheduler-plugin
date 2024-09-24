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

package org.pentaho.platform.scheduler2.versionchecker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPluginLifecycleListener;
import org.pentaho.platform.api.engine.PluginLifecycleException;
import org.pentaho.platform.api.scheduler2.IJob;
import org.pentaho.platform.api.scheduler2.IJobFilter;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import java.util.List;

//TODO Delete this class in a future release.
public class EmbeddedVersionCheckSystemListener implements IPluginLifecycleListener {

  /**
   * This is a direct copy of VersionCheckSystemListener except that the mechanism for talking to quartz goes through
   * the PentahoSystem factory
   */
  private final Log logger;
  public static final String VERSION_CHECK_JOBNAME = "PentahoSystemVersionCheck"; //$NON-NLS-1$

  public EmbeddedVersionCheckSystemListener() {
    logger = LogFactory.getLog( EmbeddedVersionCheckSystemListener.class );
  }

  @Override
  public void init() throws PluginLifecycleException {
    logger.info("***************************************************************");
    logger.info("EmbeddedVersionCheckSystemListener initialized.");
    logger.info("***************************************************************");
  }

  @Override
  public void loaded() {
    try {
      logger.warn( "Version Checker has been DEPRECATED and will be deleted in a upcoming release." );
      deleteJobIfNecessary();
    } catch ( SchedulerException ignoredOnPurpose ) {
      // By version checker requirement, we must not log unless it's trace
      if ( logger.isTraceEnabled() ) {
        logger.trace( "Exception in VersionCheck", ignoredOnPurpose ); //$NON-NLS-1$
      }
    }
  }

  protected void deleteJobIfNecessary() throws SchedulerException {
    IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", null ); //$NON-NLS-1$
    IJobFilter filter = new IJobFilter() {
      public boolean accept( IJob job ) {
        return job.getJobName().contains( EmbeddedVersionCheckSystemListener.VERSION_CHECK_JOBNAME );
      }
    };

    // Like old code - remove the existing job
    List<IJob> matchingJobs = scheduler.getJobs( filter );
    if ( ( matchingJobs != null ) && ( matchingJobs.size() > 0 ) ) {
      for ( IJob verCkJob : matchingJobs ) {
        scheduler.removeJob( verCkJob.getJobId() );
      }
    }
  }

  @Override
  public void unLoaded() {
  }
}
