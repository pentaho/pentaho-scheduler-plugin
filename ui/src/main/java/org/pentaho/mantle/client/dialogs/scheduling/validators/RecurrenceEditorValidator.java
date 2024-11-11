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

import org.pentaho.gwt.widgets.client.utils.string.StringUtils;
import org.pentaho.gwt.widgets.client.utils.TimeUtil;
import org.pentaho.mantle.client.dialogs.scheduling.RecurrenceEditor;
import org.pentaho.mantle.client.dialogs.scheduling.RecurrenceEditor.DailyRecurrenceEditor;
import org.pentaho.mantle.client.dialogs.scheduling.RecurrenceEditor.HourlyRecurrenceEditor;
import org.pentaho.mantle.client.dialogs.scheduling.RecurrenceEditor.MinutelyRecurrenceEditor;
import org.pentaho.mantle.client.dialogs.scheduling.RecurrenceEditor.MonthlyRecurrenceEditor;
import org.pentaho.mantle.client.dialogs.scheduling.RecurrenceEditor.WeeklyRecurrenceEditor;
import org.pentaho.mantle.client.dialogs.scheduling.RecurrenceEditor.YearlyRecurrenceEditor;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleEditor;

/**
 * 
 * @author Steven Barkdull
 * 
 */
@SuppressWarnings( "deprecation" )
public class RecurrenceEditorValidator implements IUiValidator {

  private RecurrenceEditor recurrenceEditor = null;
  protected DateRangeEditorValidator dateRangeEditorValidator = null;
  private ScheduleEditor scheduleEditor;

  public RecurrenceEditorValidator( RecurrenceEditor recurrenceEditor ) {
    this.recurrenceEditor = recurrenceEditor;
    this.dateRangeEditorValidator = new DateRangeEditorValidator( recurrenceEditor.getDateRangeEditor() );
  }

  public boolean isValid() {
    boolean isValid = true;
    switch ( recurrenceEditor.getTemporalState() ) {
      case SECONDS:
        RecurrenceEditor.SecondlyRecurrenceEditor sEd = recurrenceEditor.getSecondlyEditor();
        String seconds = sEd.getValue();
        if ( !StringUtils.isPositiveInteger( seconds )
            || ( Integer.parseInt( seconds ) <= 0 )
            || Integer.parseInt( seconds ) > TimeUtil.MAX_SECOND_BY_MILLISEC ) {
          isValid = false;
        }
        break;
      case MINUTES:
        MinutelyRecurrenceEditor mEd = recurrenceEditor.getMinutelyEditor();
        String minutes = mEd.getValue();
        if ( !StringUtils.isPositiveInteger( minutes )
            || ( Integer.parseInt( minutes ) <= 0 )
            || Integer.parseInt( minutes ) > TimeUtil.MAX_MINUTE_BY_MILLISEC ) {
          isValid = false;
        }
        break;
      case HOURS:
        HourlyRecurrenceEditor hEd = recurrenceEditor.getHourlyEditor();
        String hours = hEd.getValue();
        if ( !StringUtils.isPositiveInteger( hours )
            || ( Integer.parseInt( hours ) <= 0 )
            || Integer.parseInt( hours ) > TimeUtil.MAX_HOUR_BY_MILLISEC ) {
          isValid = false;
        }
        break;
      case DAILY:
        DailyRecurrenceEditor dEd = recurrenceEditor.getDailyEditor();
        if ( dEd.isEveryNDays() ) {
          String days = dEd.getDailyRepeatValue();
          if ( !StringUtils.isPositiveInteger( days ) || ( Integer.parseInt( days ) <= 0 ) ) {
            isValid = false;
          }
        }
        break;
      case WEEKLY:
        WeeklyRecurrenceEditor wEd = recurrenceEditor.getWeeklyEditor();
        if ( wEd.getNumCheckedDays() < 1 ) {
          isValid = false;
        }
        break;
      case MONTHLY:
        MonthlyRecurrenceEditor monthlyEd = recurrenceEditor.getMonthlyEditor();
        if ( monthlyEd.isDayNOfMonth() ) {
          String dayNOfMonth = monthlyEd.getDayOfMonth();
          if ( !StringUtils.isPositiveInteger( dayNOfMonth )
              || !TimeUtil.isDayOfMonth( Integer.parseInt( dayNOfMonth ) ) ) {
            isValid = false;
          }
        }
        break;
      case YEARLY:
        YearlyRecurrenceEditor yearlyEd = recurrenceEditor.getYearlyEditor();
        if ( yearlyEd.isEveryMonthOnNthDay() ) {
          String dayNOfMonth = yearlyEd.getDayOfMonth();
          if ( !StringUtils.isPositiveInteger( dayNOfMonth )
              || !TimeUtil.isDayOfMonth( Integer.parseInt( dayNOfMonth ) ) ) {
            isValid = false;
          }
        }
        break;
      default:
    }
    isValid &= dateRangeEditorValidator.isValid();
    return isValid;
  }

  public void clear() {
    recurrenceEditor.getSecondlyEditor().setValueError( null );
    recurrenceEditor.getMinutelyEditor().setValueError( null );
    recurrenceEditor.getHourlyEditor().setValueError( null );
    recurrenceEditor.getDailyEditor().setRepeatError( null );
    recurrenceEditor.getWeeklyEditor().setEveryDayOnError( null );
    recurrenceEditor.getMonthlyEditor().setDayNOfMonthError( null );
    recurrenceEditor.getYearlyEditor().setDayOfMonthError( null );
    dateRangeEditorValidator.clear();
  }
}
