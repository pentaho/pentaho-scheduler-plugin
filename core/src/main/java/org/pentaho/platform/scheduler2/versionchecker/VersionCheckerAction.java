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


package org.pentaho.platform.scheduler2.versionchecker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.action.IAction;

//TODO Delete this class in a future release.
public class VersionCheckerAction implements IAction {

  public static final String VERSION_REQUEST_FLAGS = "versionRequestFlags"; //$NON-NLS-1$

  private int requestFlags;

  public Log getLogger() {
    return LogFactory.getLog( VersionCheckerAction.class );
  }

  public void setVersionRequestFlags( int value ) {
    this.requestFlags = value;
  }

  public int getVersionRequestFlags() {
    return this.requestFlags;
  }

  public void execute() {
    getLogger().warn( "Version Checker has been DEPRECATED and will be deleted in a upcoming release." );
  }
}
