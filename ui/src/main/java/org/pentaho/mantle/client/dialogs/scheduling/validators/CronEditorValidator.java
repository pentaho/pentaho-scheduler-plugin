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


package org.pentaho.mantle.client.dialogs.scheduling.validators;

import org.pentaho.gwt.widgets.client.utils.CronExpression;
import org.pentaho.mantle.client.dialogs.scheduling.CronEditor;

public class CronEditorValidator implements IUiValidator {

  private CronEditor editor = null;
  protected DateRangeEditorValidator dateRangeEditorValidator = null;

  public CronEditorValidator( CronEditor editor ) {
    this.editor = editor;
    this.dateRangeEditorValidator = new DateRangeEditorValidator( editor.getDateRangeEditor() );
  }

  public boolean isValid() {
    boolean isValid = true;

    if ( !CronExpression.isValidExpression( editor.getCronString() ) ) {
      isValid = false;
    }
    isValid &= dateRangeEditorValidator.isValid();

    return isValid;
  }

  public void clear() {
    editor.setCronError( null );
    dateRangeEditorValidator.clear();
  }
}
