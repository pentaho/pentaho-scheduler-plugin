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

import org.pentaho.mantle.client.dialogs.scheduling.ScheduleEditor;

public class ScheduleEditorValidator implements IUiValidator {
  protected ScheduleEditor scheduleEditor;

  protected RecurrenceEditorValidator recurrenceEditorValidator;

  protected RunOnceEditorValidator runOnceEditorValidator;

  protected CronEditorValidator cronEditorValidator;

  protected BlockoutValidator blockoutValidator;

  public ScheduleEditorValidator( ScheduleEditor scheduleEditor ) {
    this.scheduleEditor = scheduleEditor;
    this.recurrenceEditorValidator = new RecurrenceEditorValidator( this.scheduleEditor.getRecurrenceEditor() );
    this.runOnceEditorValidator = new RunOnceEditorValidator( this.scheduleEditor.getRunOnceEditor() );
    this.cronEditorValidator = new CronEditorValidator( this.scheduleEditor.getCronEditor() );
    this.blockoutValidator = new BlockoutValidator( scheduleEditor );
  }

  public boolean isValid() {
    boolean isValid = true;

    switch ( scheduleEditor.getScheduleType() ) {
      case RUN_ONCE:
        isValid &= runOnceEditorValidator.isValid();
        break;
      case SECONDS: // fall through
      case MINUTES: // fall through
      case HOURS: // fall through
      case DAILY: // fall through
      case WEEKLY: // fall through
      case MONTHLY: // fall through
      case YEARLY:
        isValid &= recurrenceEditorValidator.isValid();
        break;
      case CRON:
        isValid &= cronEditorValidator.isValid();
        break;
      default:
    }

    if ( this.scheduleEditor.isBlockoutDialog() ) {
      isValid &= blockoutValidator.isValid();
    }

    return isValid;
  }

  public void clear() {
    recurrenceEditorValidator.clear();
    runOnceEditorValidator.clear();
    cronEditorValidator.clear();
  }
}
