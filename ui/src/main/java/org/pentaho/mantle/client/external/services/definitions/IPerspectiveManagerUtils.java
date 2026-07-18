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



package org.pentaho.mantle.client.external.services.definitions;

import com.google.gwt.dom.client.Element;

public interface IPerspectiveManagerUtils {

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.getSchedulesPerspectiveElement
  */
  Element getSchedulesPerspectiveElement( Element containerElement );
}
