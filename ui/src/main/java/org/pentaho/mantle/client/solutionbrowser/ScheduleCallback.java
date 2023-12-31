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

package org.pentaho.mantle.client.solutionbrowser;

import org.pentaho.gwt.widgets.client.dialogs.IDialogCallback;
import org.pentaho.gwt.widgets.client.dialogs.MessageDialogBox;
import org.pentaho.gwt.widgets.client.filechooser.RepositoryFile;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleHelper;
import org.pentaho.mantle.client.messages.Messages;

public class ScheduleCallback implements IDialogCallback {
  private final String repositoryFilePath;

  public ScheduleCallback( RepositoryFile repositoryFile ) {
    this( repositoryFile.getPath() );
  }

  public ScheduleCallback( String repositoryFilePath ) {
    this.repositoryFilePath = repositoryFilePath;
  }

  @Override
  public void okPressed() {
    String extension = ""; //$NON-NLS-1$
    if ( repositoryFilePath.lastIndexOf( "." ) > 0 ) { //$NON-NLS-1$
      extension = repositoryFilePath.substring( repositoryFilePath.lastIndexOf( "." ) + 1 ); //$NON-NLS-1$
    }

    if ( containsExtension( extension ) ) {

      IDialogCallback callback = new IDialogCallback() {
        @Override
        public void okPressed() {
          ScheduleCreateStatusDialog dialog = new ScheduleCreateStatusDialog();
          dialog.center();
        }

        @Override
        public void cancelPressed() {
        }
      };

      ScheduleHelper.showScheduleDialog( repositoryFilePath, callback );
    } else {
      final MessageDialogBox dialogBox =
          new MessageDialogBox(
              Messages.getString( "open" ), Messages.getString( "scheduleInvalidFileType", repositoryFilePath ), false, false, true ); //$NON-NLS-1$ //$NON-NLS-2$

      dialogBox.setCallback( new IDialogCallback() {
        public void cancelPressed() {
        }

        public void okPressed() {
          dialogBox.hide();
        }
      } );

      dialogBox.center();
      return;
    }
  }

  @Override
  public void cancelPressed() {
  }

  public native boolean containsExtension( String extension ) /*-{
   return $wnd.mantle.containsExtension( extension );
  }-*/;

}
