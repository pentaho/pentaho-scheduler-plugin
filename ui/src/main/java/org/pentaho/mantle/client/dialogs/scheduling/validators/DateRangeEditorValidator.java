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


package org.pentaho.mantle.client.dialogs.scheduling.validators;

import org.pentaho.gwt.widgets.client.controls.DateRangeEditor;

public class DateRangeEditorValidator implements IUiValidator {

  private DateRangeEditor dateRangeEditor = null;

  public DateRangeEditorValidator( DateRangeEditor dateRangeEditor ) {
    this.dateRangeEditor = dateRangeEditor;
  }

  public boolean isValid() {
    boolean isValid = true;

    if ( null == dateRangeEditor.getStartDate() ) {
      isValid = false;
    }

    if ( dateRangeEditor.isEndBy() && ( null == dateRangeEditor.getEndDate() ) ) {
      isValid = false;
    }
    return isValid;
  }

  public void clear() {
    dateRangeEditor.setStartDateError( null );
    dateRangeEditor.setEndByError( null );
  }
}
