/*!
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
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.mantle.client.dialogs.scheduling;

import com.google.gwt.core.client.JavaScriptObject;
import org.pentaho.gwt.widgets.client.dialogs.MessageDialogBox;
import org.pentaho.gwt.widgets.client.filechooser.*;
import org.pentaho.gwt.widgets.client.utils.i18n.IResourceBundleLoadCallback;
import org.pentaho.gwt.widgets.client.utils.i18n.ResourceBundle;
import org.pentaho.mantle.client.external.services.MantleModelUtils;
import org.pentaho.mantle.client.messages.Messages;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import org.pentaho.mantle.client.external.services.ContentCleanerPanelUtils;
import org.pentaho.mantle.client.external.services.PerspectiveManagerUtils;
import org.pentaho.mantle.client.external.services.RunInBackgroundCommandUtils;

/**
 * Entry point for display schedule dialog.
 * 
 * It registers $wnd.openReportSchedulingDialog for call dialog from native js.
 */
public class NewScheduleDialogEntryPoint implements EntryPoint, IResourceBundleLoadCallback {
  @Override
  public void onModuleLoad() {
    ResourceBundle messages = new ResourceBundle();
    Messages.setResourceBundle( messages );
    messages
        .loadBundle( GWT.getModuleBaseURL() + "messages/", "schedulerMessages", true, NewScheduleDialogEntryPoint.this ); //$NON-NLS-1$ //$NON-NLS-2$
  }

  @Override
  public void bundleLoaded( String bundleName ) {
    initializeExternalServices();
    setupNativeHooks( this );
  }

  private void initializeExternalServices() {
    new PerspectiveManagerUtils();
    new ContentCleanerPanelUtils();
    new RunInBackgroundCommandUtils();
    new MantleModelUtils();
    new ScheduleHelper();
  }


  public native void notifyCallback( JavaScriptObject callback, RepositoryFile file, String filePath, String fileName,
                                     String title )
  /*-{
   try {
     callback.fileSelected(file, filePath, fileName, title);
   } catch (ex) {
   }
  }-*/;

  public native void notifyCallbackCanceled( JavaScriptObject callback )
  /*-{
   try {
     callback.dialogCanceled();
   } catch (ex) {
     alert(ex);
   }
  }-*/;

  public void openFolderChooserDialog(final JavaScriptObject callback, String selectedPath ) {
    FileChooserDialog dialog = new FileChooserDialog( FileChooser.FileChooserMode.OPEN, selectedPath, false, true );
    addFileChooserListener( dialog, callback );

    dialog.setFileFilter( new FileFilter() {
      @Override
      public boolean accept( String name, boolean isDirectory, boolean isVisible ) {
        return isDirectory;
      }
    } );
  }

  private void addFileChooserListener( FileChooserDialog dialog, final JavaScriptObject callback ) {
    dialog.addFileChooserListener( new FileChooserListener() {
      public void fileSelected(RepositoryFile file, String filePath, String fileName, String title ) {
        notifyCallback( callback, file, filePath, fileName, title );
      }

      public void fileSelectionChanged( RepositoryFile file, String filePath, String fileName, String title ) {
      }

      public void dialogCanceled() {
        notifyCallbackCanceled( callback );
      }
    } );
  }

  public void openScheduleDialog( String reportFile ) {
    IScheduleCallback callback = new IScheduleCallback() {

      @Override
      public void okPressed() {
        MessageDialogBox dialogBox =
            new MessageDialogBox(
                Messages.getString( "scheduleCreated" ), Messages.getString( "scheduleCreateSuccessNoSwitch" ), //$NON-NLS-1$ //$NON-NLS-2$
                false, false, true );
        dialogBox.center();
      }

      @Override
      public void cancelPressed() {
      }

      @Override
      public void scheduleJob() {
      }
    };
    NewScheduleDialog dialog = ScheduleFactory.getInstance().createNewScheduleDialog( reportFile, callback, false );
    dialog.center();
  }

  public void openBackgroundDialog( String reportFile ) {
    new ScheduleOutputLocationDialogExecutor( reportFile ).performOperation();
  }

  public native void displayMessage( String message ) /*-{
    $wnd.alert(message);
  }-*/;

  public native void setupNativeHooks( NewScheduleDialogEntryPoint reportSchedulingEntryPoint )
  /*-{
    $wnd.openReportSchedulingDialog = function(reportFile) {
      reportSchedulingEntryPoint.@org.pentaho.mantle.client.dialogs.scheduling.NewScheduleDialogEntryPoint::openScheduleDialog(Ljava/lang/String;)(reportFile);
    }
    $wnd.openReportBackgroundDialog = function(reportFile) {
      reportSchedulingEntryPoint.@org.pentaho.mantle.client.dialogs.scheduling.NewScheduleDialogEntryPoint::openBackgroundDialog(Ljava/lang/String;)(reportFile);
    }
    $wnd.openFolderChooserDialog = function(callback, selectedPath) {
      reportSchedulingEntryPoint.@org.pentaho.mantle.client.dialogs.scheduling.NewScheduleDialogEntryPoint::openFolderChooserDialog(Lcom/google/gwt/core/client/JavaScriptObject;Ljava/lang/String;)(callback, selectedPath);
    }
    $wnd.pho.displayMessage = function(message) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      reportSchedulingEntryPoint.@org.pentaho.mantle.client.dialogs.scheduling.NewScheduleDialogEntryPoint::displayMessage(Ljava/lang/String;)(message);
    }
  }-*/;
}
