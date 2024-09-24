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

package org.pentaho.platform.scheduler.versionchecker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * This class is here for legacy reasons, to allow existing system with the old version checker class to exist and be
 * instanced by quartz. The code here is a no-op, the new version checker job will remove the old one before it is added
 * with the correct/new classname.
 */
public class VersionCheckerJob implements Job {

  public static final String VERSION_REQUEST_FLAGS = "versionRequestFlags"; //$NON-NLS-1$

  public Log getLogger() {
    return LogFactory.getLog( VersionCheckerJob.class );
  }

  public void execute( final JobExecutionContext context ) throws JobExecutionException {
  }
}
