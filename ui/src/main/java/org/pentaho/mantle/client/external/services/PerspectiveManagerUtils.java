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

import com.google.gwt.user.client.ui.DeckPanel;
import org.pentaho.mantle.client.external.services.definitions.IPerspectiveManagerUtils;
import org.pentaho.mantle.client.workspace.SchedulesPerspectivePanel;

public class PerspectiveManagerUtils implements IPerspectiveManagerUtils {

  static {
    setupNativeHooks( new PerspectiveManagerUtils() );
  }

  public int getSchedulerPerspectivePanelIndex( DeckPanel contentDeck ) {
    return contentDeck.getWidgetIndex( SchedulesPerspectivePanel.getInstance() );
  }

  public void addSchedulesPerspectivePanel( DeckPanel contentDeck ) {
    contentDeck.add( SchedulesPerspectivePanel.getInstance() );
  }

  public void refreshSchedulesPerspectivePanel() {
    SchedulesPerspectivePanel.getInstance().refresh();
  }

  private static native void setupNativeHooks( PerspectiveManagerUtils utils )
  /*-{
    $wnd.pho.getSchedulerPerspectivePanelIndex = function(contentDeck) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      return utils.@org.pentaho.mantle.client.external.services.PerspectiveManagerUtils::getSchedulerPerspectivePanelIndex(Lcom/google/gwt/user/client/ui/DeckPanel;)(contentDeck);
    }

    $wnd.pho.addSchedulesPerspectivePanel = function(contentDeck) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      utils.@org.pentaho.mantle.client.external.services.PerspectiveManagerUtils::addSchedulesPerspectivePanel(Lcom/google/gwt/user/client/ui/DeckPanel;)(contentDeck);
    }

    $wnd.pho.refreshSchedulesPerspectivePanel = function() {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      utils.@org.pentaho.mantle.client.external.services.PerspectiveManagerUtils::refreshSchedulesPerspectivePanel()();
    }
  }-*/;
}
