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


package org.pentaho.mantle.client.dialogs.scheduling;

import org.pentaho.gwt.widgets.client.dialogs.IDialogCallback;
import org.pentaho.mantle.client.messages.Messages;
import org.pentaho.mantle.client.workspace.JsJob;
import org.pentaho.mantle.client.workspace.JsJobTrigger;

import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;

public class NewBlockoutScheduleDialog extends ScheduleRecurrenceDialog {
  protected boolean updateMode = false;

  public NewBlockoutScheduleDialog( final String filePath, final IDialogCallback callback, final boolean hasParams,
      final boolean isEmailConfValid ) {
    super( null, ScheduleDialogType.BLOCKOUT,
        Messages.getString( "newBlockoutSchedule" ), filePath, "", "", callback, hasParams, //$NON-NLS-1$
        isEmailConfValid );
    setNewSchedule( updateMode );
  }

  public NewBlockoutScheduleDialog( final JsJob jsJob, final IDialogCallback callback, final boolean hasParams,
      final boolean isEmailConfValid ) {
    super( null, jsJob, callback, hasParams, isEmailConfValid, ScheduleDialogType.BLOCKOUT );
  }

  @Override
  protected boolean onFinish() {
    JsJobTrigger trigger = getJsJobTrigger();
    JSONObject schedule = getSchedule();

    // TODO -- Add block out verification that it is not completely blocking an existing schedule
    if ( updateMode ) {
      addBlockoutPeriod( schedule, trigger, "update?jobid=" + URL.encodeQueryString( editJob.getJobId() ) ); //$NON-NLS-1$
    } else {
      addBlockoutPeriod( schedule, trigger, "add" ); //$NON-NLS-1$
    }
    getCallback().okPressed();

    return true;
  }

  public void setUpdateMode() {
    updateMode = true;
    setNewSchedule( updateMode );
  }
}
