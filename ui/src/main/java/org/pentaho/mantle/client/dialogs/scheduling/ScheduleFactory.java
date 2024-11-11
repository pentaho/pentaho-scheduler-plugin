/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.mantle.client.dialogs.scheduling;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import org.pentaho.gwt.widgets.client.dialogs.IDialogCallback;
import org.pentaho.gwt.widgets.client.wizards.AbstractWizardDialog;
import org.pentaho.mantle.client.workspace.JsJob;

@SuppressWarnings("deprecation")
public class ScheduleFactory {
  private static final ScheduleFactory instance = GWT.create( ScheduleFactory.class );

  protected ScheduleFactory() {}

  public static ScheduleFactory getInstance() {
    return instance;
  }

  public NewScheduleDialog createNewScheduleDialog( String filePath, IDialogCallback callback,
                                                    boolean isEmailConfValid ) {
    return new NewScheduleDialog( filePath, callback, isEmailConfValid );
  }

  public NewScheduleDialog createNewScheduleDialog( JsJob job, IDialogCallback callback, boolean isEmailConfValid ) {
    return new NewScheduleDialog( job, callback, isEmailConfValid );
  }

  public ScheduleParamsDialog createScheduleParamsDialog( ScheduleRecurrenceDialog dialog,
                                                          boolean isEmailConfValid, JsJob job ) {
    return new ScheduleParamsDialog( dialog, isEmailConfValid, job );
  }

  public ScheduleParamsDialog createScheduleParamsDialog( String path, JSONObject schedule, boolean isEmailConfValid ) {
    return new ScheduleParamsDialog( path, schedule, isEmailConfValid );
  }

  public ScheduleEmailDialog createScheduleEmailDialog( AbstractWizardDialog parent, String filePath,
                                                        JSONObject jobSchedule, JSONArray scheduleParams, JsJob job ) {
    return new ScheduleEmailDialog( parent, filePath, jobSchedule, scheduleParams, job );
  }

  public ScheduleEmailWizardPanel createScheduleEmailWizardPanel ( String filePath, JSONObject jobSchedule, JsJob editJob, JSONArray scheduleParams ) {
    return new ScheduleEmailWizardPanel( filePath, jobSchedule, editJob, scheduleParams );
  }
}
