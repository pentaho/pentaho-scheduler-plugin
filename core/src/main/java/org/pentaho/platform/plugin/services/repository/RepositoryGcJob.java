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


package org.pentaho.platform.plugin.services.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.action.IAction;
import org.pentaho.platform.repository2.unified.jcr.RepositoryCleaner;


/**
 * @author Andrey Khayrutdinov
 */
public class RepositoryGcJob implements IAction {
  public static final String JOB_NAME = "RepositoryGcJob";

  private static final Log logger = LogFactory.getLog( RepositoryGcJob.class );

  @Override
  public void execute() throws Exception {
    logger.info( "Starting repository GC" );
    new RepositoryCleaner().gc();
    logger.info( "Repository GC has been finished" );
  }
}
