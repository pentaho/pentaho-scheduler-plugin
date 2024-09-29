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


package org.pentaho.mantle.client.external.services;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import org.pentaho.mantle.client.external.services.definitions.IPerspectiveManagerUtils;
import org.pentaho.mantle.client.workspace.SchedulesPerspectivePanel;

public class PerspectiveManagerUtils implements IPerspectiveManagerUtils {

  static {
    setupNativeHooks( new PerspectiveManagerUtils() );
  }

  private RootPanel schedulesPerspectiveRootPanel = null;

  public Element getSchedulesPerspectiveElement( Element containerElement ) {

    SchedulesPerspectivePanel perspectivePanel = SchedulesPerspectivePanel.getInstance();

    if ( schedulesPerspectiveRootPanel == null ) {
      // Setup
      schedulesPerspectiveRootPanel = RootPanel.get( containerElement.getId() );
      schedulesPerspectiveRootPanel.add( perspectivePanel );
    } else {
      perspectivePanel.refresh();
    }

    return perspectivePanel.getElement();
  }

  private static native void setupNativeHooks( PerspectiveManagerUtils utils )
    /*-{
      $wnd.pho.getSchedulesPerspectiveElement = function(containerElement) {
        //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
        return utils.@org.pentaho.mantle.client.external.services.PerspectiveManagerUtils::getSchedulesPerspectiveElement(Lcom/google/gwt/dom/client/Element;)(containerElement);
      }
    }-*/;
}
