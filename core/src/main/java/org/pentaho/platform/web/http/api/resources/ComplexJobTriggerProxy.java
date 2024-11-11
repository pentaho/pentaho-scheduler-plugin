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


package org.pentaho.platform.web.http.api.resources;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement
public class ComplexJobTriggerProxy {

  int[] daysOfWeek = new int[0];
  int[] daysOfMonth = new int[0];
  int[] weeksOfMonth = new int[0];
  int[] monthsOfYear = new int[0];
  int[] years = new int[0];

  Date startTime;
  Date endTime;
  String uiPassParam;
  String cronString;
  String cronDescription;
  private long repeatInterval = 0;

  public long getRepeatInterval() {
    return repeatInterval;
  }
  public void setRepeatInterval( long repeatIntervalSeconds ) {
    this.repeatInterval = repeatIntervalSeconds;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime( Date startTime ) {
    this.startTime = startTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime( Date endTime ) {
    this.endTime = endTime;
  }

  public int[] getDaysOfWeek() {
    return daysOfWeek;
  }

  public void setDaysOfWeek( int[] daysOfWeek ) {
    if ( ( daysOfWeek != null ) && ( daysOfWeek.length > 0 ) ) {
      setDaysOfMonth( null );
    }
    this.daysOfWeek = daysOfWeek == null ? new int[0] : daysOfWeek;
  }

  public int[] getDaysOfMonth() {
    return daysOfMonth;
  }

  public void setDaysOfMonth( int[] daysOfMonth ) {
    if ( ( daysOfMonth != null ) && ( daysOfMonth.length > 0 ) ) {
      setDaysOfWeek( null );
    }
    this.daysOfMonth = daysOfMonth == null ? new int[0] : daysOfMonth;
  }

  public int[] getWeeksOfMonth() {
    return weeksOfMonth;
  }

  public void setWeeksOfMonth( int[] weeksOfMonth ) {
    this.weeksOfMonth = weeksOfMonth == null ? new int[0] : weeksOfMonth;
  }

  public int[] getMonthsOfYear() {
    return monthsOfYear;
  }

  public void setMonthsOfYear( int[] monthsOfYear ) {
    this.monthsOfYear = monthsOfYear == null ? new int[0] : monthsOfYear;
  }

  public int[] getYears() {
    return years;
  }

  public void setYears( int[] years ) {
    this.years = years == null ? new int[0] : years;
  }

  public String getUiPassParam() {
    return uiPassParam;
  }

  public void setUiPassParam( String uiPassParam ) {
    this.uiPassParam = uiPassParam;
  }

  public String getCronString() {
    return cronString;
  }

  public void setCronString( String cronString ) {
    this.cronString = cronString;
  }

  public String getCronDescription() {
    return cronDescription;
  }

  public void setCronDescription( String cronDescription ) {
    this.cronDescription = cronDescription;
  }

}
