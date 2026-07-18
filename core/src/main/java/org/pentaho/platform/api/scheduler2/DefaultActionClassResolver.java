/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/


package org.pentaho.platform.api.scheduler2;
import java.util.Map;

public class DefaultActionClassResolver implements IActionClassResolver {
  public Map<String, String> getActionClassMapping() {
    return actionClassMapping;
  }

  public void setActionClassMapping( Map<String, String> actionClassMapping ) {
    this.actionClassMapping = actionClassMapping;
  }

  private Map<String, String> actionClassMapping;

  @Override
  public String resolve( String className ) {
    return actionClassMapping.get( className );
  }

  @Override
  public void register( String className, String beanId ) {
    actionClassMapping.put( className, beanId );
  }
}
