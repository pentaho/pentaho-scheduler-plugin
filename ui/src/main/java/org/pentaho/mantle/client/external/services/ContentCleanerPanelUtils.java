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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
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

  @Override public String processScheduleTextBoxValue( String jsonJobString ) {
    String value = null;
    JsJob tmpJsJob = parseJsonJob( jsonJobString );
    fakeJob = false;
    if ( tmpJsJob == null ) {
      tmpJsJob = createJsJob();
      fakeJob = true;
    }
    jsJob = tmpJsJob;

    if ( jsJob != null ) {
      value = String.valueOf( Long.parseLong( jsJob.getJobParamValue( "age" ) ) / DAY_IN_MILLIS );
    }
    return value;
  }

  @Override public void processScheduleTextBoxChangeHandler( String scheduleTextBoxValue) {
    if ( jsJob != null ) {
      JsArray<JsJobParam> params = jsJob.getJobParams();
      for ( int i = 0; i < params.length(); i++ ) {
        if ( params.get( i ).getName().equals( "age" ) ) {
          params.get( i ).setValue( String.valueOf( Long.parseLong( scheduleTextBoxValue ) * DAY_IN_MILLIS ) );
          break;
        }
      }
    }
  }

  @Override public void createScheduleRecurrenceDialog( String scheduleValue, String olderThanLabel, String daysLabel ) {

    final TextBox ccScheduleTextBox = new TextBox();
    ccScheduleTextBox.setVisibleLength( 4 );

    if ( scheduleValue != null ) {
      ccScheduleTextBox.setValue( scheduleValue );
    } else {
      ccScheduleTextBox.setText( "180" );
    }

    ccScheduleTextBox.addChangeHandler( new ChangeHandler() {
      public void onChange( ChangeEvent event ) {
        processScheduleTextBoxChangeHandler( ccScheduleTextBox.getText() );
      }
    } );

    HorizontalPanel scheduleLabelPanel = new HorizontalPanel();
    scheduleLabelPanel.add( new Label( olderThanLabel, false ) );
    scheduleLabelPanel.add( ccScheduleTextBox );
    scheduleLabelPanel.add( new Label( daysLabel, false ) );

    IDialogCallback callback = new IDialogCallback() {
      public void okPressed() {
        deleteContentCleaner();
      }

      public void cancelPressed() {
      }
    };

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

  private native boolean deleteContentCleaner()/*-{
   $wnd.mantle.deleteContentCleaner();
  }-*/;

  private static native void setupNativeHooks( ContentCleanerPanelUtils utils )
  /*-{
    $wnd.pho.processScheduleTextBoxValue = function(jsonJobString) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      utils.@org.pentaho.mantle.client.external.services.ContentCleanerPanelUtils::processScheduleTextBoxValue(Ljava/lang/String;)(jsonJobString);
    }

    $wnd.pho.processScheduleTextBoxChangeHandler = function(scheduleTextBoxValue) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      utils.@org.pentaho.mantle.client.external.services.ContentCleanerPanelUtils::processScheduleTextBoxChangeHandler(Ljava/lang/String;)(scheduleTextBoxValue);
    }

    $wnd.pho.isFakeJob = function() {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      return utils.@org.pentaho.mantle.client.external.services.ContentCleanerPanelUtils::isFakeJob()();
    }

    $wnd.pho.getJobId = function() {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      return utils.@org.pentaho.mantle.client.external.services.ContentCleanerPanelUtils::getJobId()();
    }

    $wnd.pho.createScheduleRecurrenceDialog = function( scheduleValue, olderThanLabel, daysLabel ) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      utils.@org.pentaho.mantle.client.external.services.ContentCleanerPanelUtils::createScheduleRecurrenceDialog(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)( scheduleValue, olderThanLabel, daysLabel );
    }

    $wnd.pho.getJobDescription = function() {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      return utils.@org.pentaho.mantle.client.external.services.ContentCleanerPanelUtils::getJobDescription()();
    }
  }-*/;
}
