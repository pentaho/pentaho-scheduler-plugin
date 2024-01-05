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
 *
 * Copyright (c) 2002-2023 Hitachi Vantara. All rights reserved.
 */

package org.pentaho.mantle.client.external.services;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import org.pentaho.gwt.widgets.client.dialogs.MessageDialogBox;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleEmailDialog;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleOutputLocationDialog;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleParamsDialog;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleParamsHelper;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleFactory;
import org.pentaho.mantle.client.external.services.definitions.IRunInBackgroundCommandUtils;
import org.pentaho.mantle.client.messages.Messages;
import org.pentaho.mantle.client.solutionbrowser.ScheduleCreateStatusDialog;

public class RunInBackgroundCommandUtils implements IRunInBackgroundCommandUtils {

  static {
    setupNativeHooks( new RunInBackgroundCommandUtils() );
  }

  private ScheduleOutputLocationDialog outputLocationDialog = null;

  @Override public void createScheduleOutputLocationDialog( String solutionPath, Boolean feedback ) {
    final ScheduleOutputLocationDialog outputLocationDialog = new ScheduleOutputLocationDialog( solutionPath ) {
      @Override
      protected void onSelect( final String name, final String outputLocationPath, final boolean overwriteFile,
                               final String dateFormat ) {
        setOutputName( name );
        setOutputLocationPath( outputLocationPath );
        setOverwriteFile( String.valueOf( overwriteFile ) );
        setDateFormat( dateFormat );
        performOperation( feedback );
      }
    };
  }

  @Override
  public void createScheduleOutputLocationDialog( String solutionPath, String outputLocationPath, Boolean feedback ) {
    outputLocationDialog = new ScheduleOutputLocationDialog( solutionPath ) {
      @Override
      protected void onSelect( final String name, final String outputPath, final boolean overwriteFile,
                               final String dateFormat ) {
        setOutputName( name );
        setOutputLocationPath( outputPath );
        setOverwriteFile( String.valueOf( overwriteFile ) );
        setDateFormat( dateFormat );
        performOperation( feedback );
      }

      @Override protected void onCancel() {
        super.onCancel();
        RunInBackgroundCommandUtils.onCancel();
      }

      @Override protected void onOk() {
        super.onOk();
        RunInBackgroundCommandUtils.onOk( outputLocationPath );
      }

      @Override protected void onAttach() {
        super.onAttach();
        RunInBackgroundCommandUtils.onAttach();
      }
    };

    outputLocationDialog.setOkButtonText( Messages.getString( "ok" ) );
    outputLocationDialog.setScheduleNameText( Messages.getString( "scheduleNameColonReportviewer" ) );
    outputLocationDialog.center();
  }

  @Override public void setOkButtonText() {
    outputLocationDialog.setOkButtonText( Messages.getString( "ok" ) );
  }

  @Override public void centerScheduleOutputLocationDialog() {
    outputLocationDialog.center();
  }

  @Override
  public void createScheduleParamsDialog( String filePath, JSONObject scheduleRequest, Boolean isEmailConfigValid,
                                          Boolean isSchedulesPerspectiveActive ) {
    ScheduleParamsDialog dialog = ScheduleFactory.getInstance()
      .createScheduleParamsDialog( filePath, scheduleRequest, isEmailConfigValid );
    dialog.center();
    dialog.setAfterResponseCallback(
      new ScheduleParamsDialog.IAfterResponse() {
        @Override
        public void onResponse( JSONValue rib ) {
          if ( rib != null && rib.isBoolean() != null && rib.isBoolean().booleanValue() ) {
            MessageDialogBox dialogBox =
              new MessageDialogBox(
                Messages.getString( "runInBackground" ), Messages.getString( "backgroundExecutionStarted" ),
                //$NON-NLS-1$ //$NON-NLS-2$
                false, false, true );
            dialogBox.center();
          } else if ( isSchedulesPerspectiveActive ) {
            ScheduleCreateStatusDialog successDialog = new ScheduleCreateStatusDialog();
            successDialog.center();
          } else {
            MessageDialogBox dialogBox =
              new MessageDialogBox(
                Messages.getString( "scheduleUpdatedTitle" ), Messages.getString( "scheduleUpdatedMessage" ),
                //$NON-NLS-1$ //$NON-NLS-2$
                false, false, true );
            dialogBox.center();
          }
        }
      }
    );
  }

  @Override public void createScheduleEmailDialog( String filePath, JSONObject scheduleRequest ) {
    ScheduleEmailDialog scheduleEmailDialog = ScheduleFactory.getInstance()
      .createScheduleEmailDialog( null, filePath, scheduleRequest, null, null );
    scheduleEmailDialog.center();
  }

  @Override public String getScheduleParams( JSONObject scheduleRequest ) {
    JSONArray scheduleParams = ScheduleParamsHelper.getScheduleParams( scheduleRequest );
    //scheduleRequest.put( "jobParameters", scheduleParams ); will add manually to json like before
    return scheduleParams.toString(); // POC GWT translation should preserve this
  }

  private static native void onCancel()
  /*-{
    $wnd.mantle_fireEvent('GenericEvent', {eventSubType: 'locationPromptCanceled'});
  }-*/;

  private static native void onOk( final String outputPath )
  /*-{
    $wnd.mantle_fireEvent('GenericEvent', {eventSubType: 'locationPromptOk', stringParam: outputPath});
  }-*/;

  private static native void onAttach()
  /*-{
git  }-*/;

  private native void setOutputName( String name )/*-{
   $wnd.mantle_runInBackgroundCommand_setOutputName( name );
  }-*/;

  private native void setOutputLocationPath( String outputPath )/*-{
   $wnd.mantle_runInBackgroundCommand_setOutputLocationPath( outputPath );
  }-*/;

  private native void setOverwriteFile( String overwriteFile )/*-{
   $wnd.mantle_runInBackgroundCommand_setOverwriteFile( overwriteFile );
  }-*/;

  private native void setDateFormat( String dateFormat )/*-{
   $wnd.mantle_runInBackgroundCommand_setDateFormat( dateFormat );
  }-*/;

  private native void performOperation( Boolean feedback )/*-{
   $wnd.mantle_runInBackgroundCommand_performOperation( feedback );
  }-*/;

  private static native void setupNativeHooks( RunInBackgroundCommandUtils utils )
  /*-{
    $wnd.pho.createScheduleOutputLocationDialog = function(solutionPath, feedback) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      utils.@org.pentaho.mantle.client.external.services.RunInBackgroundCommandUtils::createScheduleOutputLocationDialog(Ljava/lang/String;Ljava/lang/Boolean;)(solutionPath, feedback);
    }

    $wnd.pho.createScheduleOutputLocationDialog = function(solutionPath, outputLocationPath, feedback) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      utils.@org.pentaho.mantle.client.external.services.RunInBackgroundCommandUtils::createScheduleOutputLocationDialog(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;)(solutionPath, outputLocationPath, feedback);
    }

    $wnd.pho.setOkButtonText = function() {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      utils.@org.pentaho.mantle.client.external.services.RunInBackgroundCommandUtils::setOkButtonText()();
    }

    $wnd.pho.centerScheduleOutputLocationDialog = function() {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      utils.@org.pentaho.mantle.client.external.services.RunInBackgroundCommandUtils::centerScheduleOutputLocationDialog()();
    }

    $wnd.pho.createScheduleParamsDialog = function(filePath, scheduleRequest, isEmailConfigValid, isSchedulesPerspectiveActive) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      utils.@org.pentaho.mantle.client.external.services.RunInBackgroundCommandUtils::createScheduleParamsDialog(Ljava/lang/String;Lcom/google/gwt/json/client/JSONObject;Ljava/lang/Boolean;Ljava/lang/Boolean;)(filePath, scheduleRequest, isEmailConfigValid, isSchedulesPerspectiveActive);
    }

    $wnd.pho.createScheduleEmailDialog = function(filePath, scheduleRequest ) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      utils.@org.pentaho.mantle.client.external.services.RunInBackgroundCommandUtils::createScheduleEmailDialog(Ljava/lang/String;Lcom/google/gwt/json/client/JSONObject;)(filePath, scheduleRequest);
    }

    $wnd.pho.getScheduleParams = function( scheduleRequest ) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
     return utils.@org.pentaho.mantle.client.external.services.RunInBackgroundCommandUtils::getScheduleParams(Lcom/google/gwt/json/client/JSONObject;)(scheduleRequest);
    }
  }-*/;
}
