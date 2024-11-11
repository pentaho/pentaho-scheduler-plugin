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
