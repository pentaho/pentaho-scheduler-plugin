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

import com.google.gwt.i18n.client.DateTimeFormat;
import org.pentaho.gwt.widgets.client.utils.TimeUtil;
import org.pentaho.mantle.client.dialogs.scheduling.RunOnceEditor;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleEditor;

import java.util.Date;

public class RunOnceEditorValidator implements IUiValidator {

  private RunOnceEditor editor = null;
  private ScheduleEditor scheduleEditor;

  public RunOnceEditorValidator( ScheduleEditor scheduleEditor, RunOnceEditor runOnceEditor ) {
    this.scheduleEditor = scheduleEditor;
    this.editor = runOnceEditor;
  }

  public boolean isValid() {
    String timeZone = scheduleEditor.getTimeZonePicker().getSelectedValue();
    Date startDate = editor.getStartDate();
    if ( null == startDate || null == timeZone ) {
      return false;
    }
    //BISERVER-14912 - Date.before() does not work as expected in GWT, so we need a custom validation to check the day
    int startHour = Integer.parseInt( editor.getStartHour() );
    if ( "pm".equalsIgnoreCase( editor.getStartTimeOfDay() ) ) {
      startHour += 12;
    }
    return dateInFuture( timeZone, editor.getStartDate().getYear() + 1900, editor.getStartDate().getMonth(), editor.getStartDate().getDate(),
      startHour, Integer.parseInt( editor.getStartMinute() ) );
  }

  public void clear() {
  }

  final native boolean dateInFuture( String timeZone, int startYear, int startMonth, int startDay, int startHour, int startMin ) /*-{
    var options = {
      timeZone: timeZone,
      year: "numeric",
      month: "numeric",
      day: "numeric",
      hour12: false,
      hour: "numeric",
      minute: "numeric",
      second: "numeric"
    };
    // takes advantage of Intl.DateTimeFormat to do the time zone math, the downside is we need to reassemble the date after it's parsed
    var formatter = Intl.DateTimeFormat('en-US', options);
    var dateParts = formatter.formatToParts( new Date() );
    var yearNow = 0;
    var monthNow = 0;
    var dayNow = 0;
    var hourNow = 0;
    var minNow = 0;
    var secNow = 0;
    dateParts.forEach(function (p) {
      switch (p.type) {
        case "year":
          yearNow = p.value;
          break;
        case "month":
          monthNow = p.value;
          break;
        case "day":
          dayNow = p.value;
          break;
        case "hour":
          hourNow = p.value;
          break;
        case "minute":
          minNow = p.value;
          break;
        case "second":
          secNow = p.value;
          break;
      }
    });
    var nowInSelectedTimeZone = new Date( yearNow, monthNow - 1, dayNow, hourNow, minNow, secNow );
    var selectedDate = new Date( startYear, startMonth, startDay, startHour, startMin );
    return selectedDate > nowInSelectedTimeZone;
  }-*/;
}