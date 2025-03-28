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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.action.IAction;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.services.security.userrole.oauth.PentahoOAuthUserSync;

public class PentahoOAuthUserSyncJob implements IAction {

  public static final String JOB_NAME = "PentahoOAuthUserSyncJob";

  private static final Log logger = LogFactory.getLog( PentahoOAuthUserSyncJob.class );

  @Override
  public void execute() {
    logger.info( "Starting Pentaho User Sync" );
    PentahoOAuthUserSync pentahoOAuthUserSync =
            PentahoSystem.get( PentahoOAuthUserSync.class, "pentahoOAuthUserSync", PentahoSessionHolder.getSession() );
    pentahoOAuthUserSync.performSync();
    logger.info( "Pentaho User Sync has been finished" );
  }

}
