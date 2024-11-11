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


package org.pentaho.mantle.client.solutionbrowser;

import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import org.pentaho.gwt.widgets.client.dialogs.PromptDialogBox;
import org.pentaho.mantle.client.messages.Messages;

public class ScheduleCreateStatusDialog extends PromptDialogBox {

  public ScheduleCreateStatusDialog() {
    super( Messages.getString( "scheduleCreated" ), Messages.getString( "yes" ), Messages.getString( "no" ), false,
        true );
    Label label = new Label();
    label.setText( Messages.getString( "scheduleCreateSuccess" ) );
    label.setHorizontalAlignment( HasHorizontalAlignment.ALIGN_LEFT );
    setContent( label );
    setWidth( "400px" );
  }

  protected void onOk() {
    super.onOk();
    setSchedulesPerspective();
  }

  public native void setSchedulesPerspective() /*-{
   $wnd.mantle.setSchedulesPerspective();
  }-*/;

}
