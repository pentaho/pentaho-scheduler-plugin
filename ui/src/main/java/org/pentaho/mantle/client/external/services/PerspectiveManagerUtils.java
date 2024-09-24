/*!
 *
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 *
 * Copyright (c) 2002-2023 Hitachi Vantara. All rights reserved.
 *
 */

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
