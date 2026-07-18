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



package org.pentaho.mantle.client.external.services;

import com.google.gwt.user.client.Command;
import org.pentaho.mantle.client.commands.RefreshSchedulesCommand;
import org.pentaho.mantle.client.external.services.definitions.IMantleModelUtils;

public class MantleModelUtils implements IMantleModelUtils {

  static {
    setupNativeHooks( new MantleModelUtils() );
  }

  public void refreshSchedules() {
    Command cmd = new RefreshSchedulesCommand();
    cmd.execute();
  }

  private static native void setupNativeHooks( MantleModelUtils utils )
  /*-{
    $wnd.pho.refreshSchedules = function() {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      return utils.@org.pentaho.mantle.client.external.services.MantleModelUtils::refreshSchedules()();
    }
  }-*/;
}
