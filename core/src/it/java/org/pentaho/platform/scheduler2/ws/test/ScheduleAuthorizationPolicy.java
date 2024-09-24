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

package org.pentaho.platform.scheduler2.ws.test;

import org.pentaho.platform.api.engine.IAuthorizationPolicy;

import java.util.Collections;
import java.util.List;

public class ScheduleAuthorizationPolicy implements IAuthorizationPolicy {
  public boolean isAllowed( String actionName ) {
    return true;
  }

  public List<String> getAllowedActions( String actionNamespace ) {
    return Collections.<String>emptyList();
  }
}
