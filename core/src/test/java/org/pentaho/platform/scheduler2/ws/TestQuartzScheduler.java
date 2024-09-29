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


package org.pentaho.platform.scheduler2.ws;

import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.scheduler2.quartz.QuartzScheduler;

public class TestQuartzScheduler extends QuartzScheduler {

  static final String TEST_USER = "TestUser";

  @Override
  protected String getCurrentUser() {
    SecurityHelper.getInstance().becomeUser( TEST_USER );
    return super.getCurrentUser();
  }
}
