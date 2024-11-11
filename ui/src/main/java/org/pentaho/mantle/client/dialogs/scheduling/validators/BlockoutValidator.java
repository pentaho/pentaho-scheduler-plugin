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

import static org.pentaho.gwt.widgets.client.utils.TimeUtil.TimeOfDay.PM;

import org.pentaho.gwt.widgets.client.controls.TimePicker;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleEditor;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleEditor.DurationValues;

public class BlockoutValidator implements IUiValidator {

  private ScheduleEditor scheduleEditor;

  public BlockoutValidator( ScheduleEditor scheduleEditor ) {
    this.scheduleEditor = scheduleEditor;
  }

  @Override
  public boolean isValid() {
    switch ( this.scheduleEditor.getBlockoutEndsType() ) {
      case DURATION:
        DurationValues durationValues = this.scheduleEditor.getDurationValues();
        return durationValues.days != 0 || durationValues.hours != 0 || durationValues.minutes != 0;
      case TIME:
        TimePicker startTimePicker = this.scheduleEditor.getStartTimePicker();
        int startTimeHour = Integer.parseInt( startTimePicker.getHour() );
        int startTimeMinute = Integer.parseInt( startTimePicker.getMinute() );

        int startTime =
            startTimeMinute + ( startTimeHour + ( PM.equals( startTimePicker.getTimeOfDay() ) ? 12 : 0 ) ) * 60;

        TimePicker endTimePicker = this.scheduleEditor.getBlockoutEndTimePicker();
        int endTimeHour = Integer.parseInt( endTimePicker.getHour() );
        int endTimeMinute = Integer.parseInt( endTimePicker.getMinute() );

        int endTime = endTimeMinute + ( endTimeHour + ( PM.equals( endTimePicker.getTimeOfDay() ) ? 12 : 0 ) ) * 60;

        return endTime > startTime;
      default:
        return false; // TODO EXCEPTION
    }
  }

  @Override
  public void clear() {
    // No values needing to be cleared
  }

}
