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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.user.client.ui.DeckPanel;
import org.pentaho.mantle.client.external.services.definitions.IPerspectiveManagerUtils;
import org.pentaho.mantle.client.workspace.SchedulesPerspectivePanel;

public class PerspectiveManagerUtils implements IPerspectiveManagerUtils {

  static {
    setupNativeHooks( new PerspectiveManagerUtils() );
  }

  public void showSchedulesPerspective( DeckPanel contentDeck ) {

    GWT.runAsync( new RunAsyncCallback() {

      public void onSuccess() {
        if ( contentDeck.getWidgetIndex( SchedulesPerspectivePanel.getInstance() ) == -1 ) {
          contentDeck.add( SchedulesPerspectivePanel.getInstance() );
        } else {
          SchedulesPerspectivePanel.getInstance().refresh();
        }
        contentDeck.showWidget( contentDeck.getWidgetIndex( SchedulesPerspectivePanel.getInstance() ) );
      }

      public void onFailure( Throwable reason ) {
      }
    } );
  }

  private static native void setupNativeHooks( PerspectiveManagerUtils utils )
  /*-{
    $wnd.pho.showSchedulesPerspective = function(contentDeck) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      return utils.@org.pentaho.mantle.client.external.services.PerspectiveManagerUtils::showSchedulesPerspective(Lcom/google/gwt/user/client/ui/DeckPanel;)(contentDeck);
    }
  }-*/;
}
