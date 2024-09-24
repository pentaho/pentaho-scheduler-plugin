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

/**
 * Callback for some schedule dialog result. Required for communication between caller and dialogs.
 */
public interface IScheduleCallback extends IDialogCallback {

  void scheduleJob();
}
