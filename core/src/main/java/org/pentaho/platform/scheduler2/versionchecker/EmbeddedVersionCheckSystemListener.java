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
 * Copyright (c) 2002-2024 Hitachi Vantara. All rights reserved.
 *
 */

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
