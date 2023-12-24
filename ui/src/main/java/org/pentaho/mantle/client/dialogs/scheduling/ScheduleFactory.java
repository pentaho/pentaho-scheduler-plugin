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
 * Copyright (c) 2023 Hitachi Vantara. All rights reserved.
 */

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
