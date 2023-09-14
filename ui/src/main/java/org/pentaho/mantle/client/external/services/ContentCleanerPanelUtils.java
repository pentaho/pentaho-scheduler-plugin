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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import org.pentaho.gwt.widgets.client.dialogs.IDialogCallback;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;
import org.pentaho.gwt.widgets.client.wizards.AbstractWizardDialog;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleRecurrenceDialog;
import org.pentaho.mantle.client.external.services.definitions.IContentCleanerPanelUtils;
import org.pentaho.mantle.client.workspace.JsJob;
import org.pentaho.mantle.client.workspace.JsJobParam;

public class ContentCleanerPanelUtils implements IContentCleanerPanelUtils {

  static {
    setupNativeHooks( new ContentCleanerPanelUtils() );
  }

  private static final long DAY_IN_MILLIS = 24L * 60L * 60L * 1000L;
  private static boolean fakeJob = false;
  private static JsJob jsJob = null;

  @Override public void processScheduleTextBox( String jsonJobString, TextBox scheduleTextBox ) {

    JsJob tmpJsJob = parseJsonJob( jsonJobString );

    fakeJob = false;
    if ( tmpJsJob == null ) {
      tmpJsJob = createJsJob();
      fakeJob = true;
    }
    jsJob = tmpJsJob;

    if ( jsJob != null ) {
      scheduleTextBox.setValue( String.valueOf( Long.parseLong( jsJob.getJobParamValue( "age" ) ) / DAY_IN_MILLIS ) );
    } else {
      scheduleTextBox.setText( "180" );
    }
    scheduleTextBox.addChangeHandler( new ChangeHandler() {
      public void onChange( ChangeEvent event ) {
        if ( jsJob != null ) {
          JsArray<JsJobParam> params = jsJob.getJobParams();
          for ( int i = 0; i < params.length(); i++ ) {
            if ( params.get( i ).getName().equals( "age" ) ) {
              params.get( i ).setValue( String.valueOf( Long.parseLong( scheduleTextBox.getText() ) * DAY_IN_MILLIS ) );
              break;
            }
          }
        }
      }
    } );
  }

  @Override public void createScheduleRecurrenceDialog( HorizontalPanel scheduleLabelPanel, IDialogCallback callback ) {
    ScheduleRecurrenceDialog editSchedule =
      new ScheduleRecurrenceDialog( null, jsJob, callback, false, false,
        AbstractWizardDialog.ScheduleDialogType.SCHEDULER );
    editSchedule.setShowSuccessDialog( false );
    editSchedule.addCustomPanel( scheduleLabelPanel, DockPanel.NORTH );
    editSchedule.center();
  }

  @Override public String getJobId() {
    if ( jsJob == null || StringUtils.isEmpty( jsJob.getJobId() ) ) {
      return null;
    } else {
      return jsJob.getJobId();
    }
  }

  @Override public boolean isFakeJob() {
    return fakeJob;
  }

  @Override public String getJobDescription() {
    return jsJob.getJobTrigger().getDescription();
  }

  private final native JsJob parseJsonJob( String json )
  /*-{
    window.parent.jobjson = json;
    if (null == json || "" == json) {
      return null;
    }
    var obj = JSON.parse(json);
    return obj;
  }-*/;

  private final native JsJob createJsJob()
  /*-{
    var jsJob = new Object();
    jsJob.jobParams = new Object();
    jsJob.jobParams.jobParams = [];
    jsJob.jobParams.jobParams[0] = new Object();
    jsJob.jobParams.jobParams[0].name = "ActionAdapterQuartzJob-ActionClass";
    jsJob.jobParams.jobParams[0].value = "org.pentaho.platform.admin.GeneratedContentCleaner";
    jsJob.jobParams.jobParams[1] = new Object();
    jsJob.jobParams.jobParams[1].name = "age";
    jsJob.jobParams.jobParams[1].value = "15552000000";
    jsJob.jobTrigger = new Object();
    jsJob.jobTrigger['@type'] = "simpleJobTrigger";
    jsJob.jobTrigger.repeatCount = -1;
    jsJob.jobTrigger.repeatInterval = 86400;
    jsJob.jobTrigger.scheduleType = "DAILY";
    //jsJob.jobTrigger.startTime = "2013-03-22T09:35:52.276-04:00";
    jsJob.jobName = "GeneratedContentCleaner";
    return jsJob;
  }-*/;

  private static native void setupNativeHooks( ContentCleanerPanelUtils utils )
  /*-{
    $wnd.pho.processScheduleTextBox = function(jsonJobString, scheduleTextBox) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      utils.@org.pentaho.mantle.client.external.services.ContentCleanerPanelUtils::processScheduleTextBox(Ljava/lang/String;Lcom/google/gwt/user/client/ui/TextBox;)(jsonJobString, scheduleTextBox);
    }

    $wnd.pho.isFakeJob = function() {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      return utils.@org.pentaho.mantle.client.external.services.ContentCleanerPanelUtils::isFakeJob()();
    }

    $wnd.pho.getJobId = function() {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      return utils.@org.pentaho.mantle.client.external.services.ContentCleanerPanelUtils::getJobId()();
    }

    $wnd.pho.createScheduleRecurrenceDialog = function(scheduleLabelPanel, callback) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      utils.@org.pentaho.mantle.client.external.services.ContentCleanerPanelUtils::createScheduleRecurrenceDialog(Lcom/google/gwt/user/client/ui/HorizontalPanel;Lorg/pentaho/gwt/widgets/client/dialogs/IDialogCallback;)(scheduleLabelPanel, callback);
    }

    $wnd.pho.getJobDescription = function() {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      return utils.@org.pentaho.mantle.client.external.services.ContentCleanerPanelUtils::getJobDescription()();
    }
  }-*/;
}
