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

package org.pentaho.mantle.client.external.services.definitions;

public interface IMantleModelUtils {

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.refreshSchedules
  */
  void refreshSchedules();
}
