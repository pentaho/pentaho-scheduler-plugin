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


package org.pentaho.mantle.client.workspace;

import java.util.Date;
import java.util.Objects;

import org.pentaho.gwt.widgets.client.utils.TimeUtil;
import org.pentaho.gwt.widgets.client.utils.TimeUtil.DayOfWeek;
import org.pentaho.gwt.widgets.client.utils.TimeUtil.MonthOfYear;
import org.pentaho.gwt.widgets.client.utils.TimeUtil.WeekOfMonth;
import org.pentaho.gwt.widgets.client.utils.string.StringTokenizer;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleEditor.ScheduleType;
import org.pentaho.mantle.client.messages.Messages;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;

public class JsJobTrigger extends JavaScriptObject {

  public static final String TIME_ZONE_FORMAT = " zzz";
  private static final String EVERY = "every";
  private static final String AT = "at";
  private static final String COMPLEX_JOB_TRIGGER = "complexJobTrigger";
  private static final String SIMPLE_JOB_TRIGGER = "simpleJobTrigger";

  // Overlay types always have protected, zero argument constructors.
  protected JsJobTrigger() {
  }

  public static JsJobTrigger instance() {
    return (JsJobTrigger) JavaScriptObject.createObject();
  }

  // JSNI methods to get job type.
  public final native String getType()
  /*-{
    return this['@type'];
  }-*/;

  public final native void setType( String type )
  /*-{
    this['@type'] = type;
  }-*/;

  public final String getScheduleType() {
    String s = getUiPassParamRaw();
    if ( s != null && !s.isEmpty() ) {
      return s;
    }
    return calcScheduleType().name();
  }

  private final native String getUiPassParamRaw()
  /*-{
    return this.uiPassParam;
  }-*/;

  public final native void setScheduleType( String scheduleType )
  /*-{
    this.ScheduleType = scheduleType;
  }-*/;

  public final native int getRepeatCount()
  /*-{
    return parseInt(this.repeatCount);
  }-*/;

  public final native void setRepeatCount( int count )
  /*-{
    this.repeatCount = count;
  }-*/;

  public final native int getRepeatInterval()
  /*-{
    return parseInt(this.repeatInterval);
  }-*/;

  public final native void setRepeatInterval( int interval )
  /*-{
    this.repeatInterval = interval;
  }-*/;

  public final native void setBlockDuration( Long duration )
  /*-{
    this.duration = duration.toString();
  }-*/;

  public final Long getBlockDuration() {
    return Long.parseLong( getDuration() );
  }

  private final native String getDuration()
  /*-{
      return this.duration;
  }-*/;

  private final native String getNativeStartTime()
  /*-{
    return this.startTime;
  }-*/;

  public final native void setNativeStartTime( String iso8601TimeString )
  /*-{
    this.startTime = iso8601TimeString;
  }-*/;

  private final native String getNativeEndTime()
  /*-{
    return this.endTime;
  }-*/;

  public final native void setNativeEndTime( String iso8601TimeString )
  /*-{
    this.endTime = iso8601TimeString;
  }-*/;

  public final Date getStartTime() {
    if ( StringUtils.isEmpty( getNativeStartTime() ) ) {
      return new Date();
    }
    return JsJob.formatDate( getNativeStartTime() );
  }

  public final Date getScheduleStartTime() {
    if ( StringUtils.isEmpty( getNativeStartTime() ) ) {
      return new Date();
    }
    String dte = getNativeStartTime();
    return JsJob.formatScheduleDate( dte.substring( 0, dte.indexOf( 'T' ) ) );
  }

  public final Date getEndTime() {
    return JsJob.formatDate( getNativeEndTime() );
  }

  public final Date getScheduleEndTime() {
    String dte = getNativeEndTime();
    return JsJob.formatScheduleDate( dte.substring( 0, dte.indexOf( 'T' ) ) );
  }

  public final native int[] getSecondRecurrences()
  /*-{
    if ('secondRecurrences' in this && this.secondRecurrences !== null &&
        'recurrences' in this.secondRecurrences && 
        this.secondRecurrences.recurrences !== null &&
        this.secondRecurrences.recurrences.length > 0 &&
        'recurrenceList' in this.secondRecurrences.recurrences[0] && 
        this.secondRecurrences.recurrences[0].recurrenceList !== null &&
        'values' in this.secondRecurrences.recurrences[0].recurrenceList) {
      return this.secondRecurrences.recurrences[0].recurrenceList.values;
    }
    return [];
  }-*/;

  public final native void setSecondRecurrences( JsArrayInteger seconds )
  /*-{
    if (!('secondRecurrences' in this) || !this.secondRecurrences) {
      this.secondRecurrences = {recurrences: [{recurrenceList: {values: []}}]};
    }
    if ( !('recurrences' in this.secondRecurrences) || !this.secondRecurrences.recurrences ) {
      this.secondRecurrences = {recurrences: [{recurrenceList: {values: []}}]};
    }
    if ( this.secondRecurrences.recurrences.length === 0 ) {
      this.secondRecurrences.recurrences.push( {recurrenceList: {values: []}} );
    }
    if ( !('recurrenceList' in this.secondRecurrences.recurrences[0]) || !this.secondRecurrences.recurrences[0].recurrenceList) {
      this.secondRecurrences.recurrences[0].recurrenceList = {values: []};
    }
    this.secondRecurrences.recurrences[0].recurrenceList.values = seconds;
  }-*/;

  public final native int[] getMinuteRecurrences()
  /*-{
    if ('minuteRecurrences' in this && this.minuteRecurrences !== null && 
        'recurrences' in this.minuteRecurrences && 
        this.minuteRecurrences.recurrences !== null &&
        this.minuteRecurrences.recurrences.length > 0 &&
        'recurrenceList' in this.minuteRecurrences.recurrences[0] &&
        this.minuteRecurrences.recurrences[0].recurrenceList !== null &&
        'values' in this.minuteRecurrences.recurrences[0].recurrenceList) {
      return this.minuteRecurrences.recurrences[0].recurrenceList.values;
    }
    return [];
  }-*/;

  public final native void setMinuteRecurrences( JsArrayInteger minutes )
  /*-{
    if (!('minuteRecurrences' in this) || !this.minuteRecurrences) {
      this.minuteRecurrences = {recurrences: [{recurrenceList: {values: []}}]};
    }
    if ( !('recurrences' in this.minuteRecurrences) || !this.minuteRecurrences.recurrences ) {
      this.minuteRecurrences = {recurrences: [{recurrenceList: {values: []}}]};
    }
    if ( this.minuteRecurrences.recurrences.length === 0 ) {
      this.minuteRecurrences.recurrences.push( {recurrenceList: {values: []}} );
    }
    if (!('recurrenceList' in this.minuteRecurrences.recurrences[0]) || !this.minuteRecurrences.recurrences[0].recurrenceList) {
      this.minuteRecurrences.recurrences[0].recurrenceList = {values: []};
    }
    this.minuteRecurrences.recurrences[0].recurrenceList.values = minutes;
  }-*/;

  public final native int[] getHourRecurrences()
  /*-{
    if ('hourRecurrences' in this && this.hourRecurrences !== null && 
        'recurrences' in this.hourRecurrences && 
        this.hourRecurrences.recurrences !== null &&
        this.hourRecurrences.recurrences.length > 0 &&
        'recurrenceList' in this.hourRecurrences.recurrences[0] &&
        this.hourRecurrences.recurrences[0].recurrenceList !== null &&
        'values' in this.hourRecurrences.recurrences[0].recurrenceList) {
      return this.hourRecurrences.recurrences[0].recurrenceList.values;
    }
    return [];
  }-*/;

  public final native void setHourRecurrences( JsArrayInteger hours )
  /*-{
    if (!('hourlyRecurrences' in this) || !this.hourlyRecurrences) {
      this.hourlyRecurrences = {recurrences: [{recurrenceList: {values: []}}]};
    }
    if ( !('recurrences' in this.hourlyRecurrences) || !this.hourlyRecurrences.recurrences ) {
      this.hourlyRecurrences.recurrences = [{recurrenceList: {values: []}}];
    }
    if ( this.hourlyRecurrences.recurrences.length === 0 ) {
      this.hourlyRecurrences.recurrences.push({recurrenceList: {values: []}});
    }
    if (!('recurrenceList' in this.hourlyRecurrences.recurrences[0]) || !this.hourlyRecurrences.recurrences[0].recurrenceList) {
      this.hourlyRecurrences.recurrences[0].recurrenceList = {values: []};
    }
    this.hourlyRecurrences.recurrences[0].recurrenceList.values = hours;
  }-*/;

  public final int[] getDayOfWeekRecurrences() {
    if ( getDayOfWeekRecurrencesRaw() == null ) {
      return null;
    } else {
      return convertJsArrayStringToIntArray( getDayOfWeekRecurrencesRaw() );
    }
  }

  private final native JsArrayString getDayOfWeekRecurrencesRaw()
  /*-{
    if ('dayOfWeekRecurrences' in this && 
        this.dayOfWeekRecurrences !== null &&
        'recurrences' in this.dayOfWeekRecurrences &&
        this.dayOfWeekRecurrences.recurrences !== null &&
        this.dayOfWeekRecurrences.recurrences.length > 0 ) {
      if ( 'sequentialRecurrence' in this.dayOfWeekRecurrences.recurrences[0] ) {
        var result = [];
        result.push( this.dayOfWeekRecurrences.recurrences[0].sequentialRecurrence.firstValue );
        var i = parseInt( this.dayOfWeekRecurrences.recurrences[0].sequentialRecurrence.firstValue );
        while( i < parseInt( this.dayOfWeekRecurrences.recurrences[0].sequentialRecurrence.lastValue ) ) {
            i++;
          result.push( '' + i );
        }
        return result;
      }
      if ('recurrenceList' in this.dayOfWeekRecurrences.recurrences[0] &&
          this.dayOfWeekRecurrences.recurrences[0].recurrenceList !== null &&
          'values' in this.dayOfWeekRecurrences.recurrences[0].recurrenceList) {
        return this.dayOfWeekRecurrences.recurrences[0].recurrenceList.values;
      }
    }
    return null;
  }-*/;

  /**
   * Converts javascript integer arrays that were stored as quoted numbers in the JSON as an int[] array.
   *
   * @param jsArrayString = Json Array with the integer elements quoted
   * @return int array
   */
  public final int[] convertJsArrayStringToIntArray( JsArrayString jsArrayString ) {
    if ( jsArrayString == null ) {
      return null;
    } else {
      int[] intArray = new int[ jsArrayString.length() ];
      StringTokenizer tokenizer = new StringTokenizer( jsArrayString.toString(), "," );
      for ( int i = 0; i < tokenizer.countTokens(); i++ ) {
        try {
          String value = tokenizer.tokenAt( i );
          intArray[ i ] = Integer.parseInt( value );
        } catch ( Throwable t ) {
          Window.alert( t.getMessage() );
        }
      }
      return intArray;
    }
  }

  public final native void setDayOfWeekRecurrences( JsArrayInteger days )
  /*-{
    if (!('dayOfWeekRecurrences' in this) || !this.dayOfWeekRecurrences) {
      this.dayOfWeekRecurrences = {recurrences: [{ recurrenceList: {values: []}}]};
    }
    if ( !('recurrences' in this.dayOfWeekRecurrences) || !this.dayOfWeekRecurrences.recurrences ) {
      this.dayOfWeekRecurrences = {recurrences: [{ recurrenceList: {values: []}}]};
    }
    if ( this.dayOfWeekRecurrences.recurrences.length === 0 ) {
      this.dayOfWeekRecurrences.recurrences.push( {recurrenceList: {values: []}} );
    }
    if (!('recurrenceList' in this.dayOfWeekRecurrences.recurrences[0]) || !this.dayOfWeekRecurrences.recurrences[0].recurrenceList) {
      this.dayOfWeekRecurrences.recurrences[0].recurrenceList = { values: [] };
    }
    this.dayOfWeekRecurrences.recurrences[0].recurrenceList.values = days;
  }-*/;

  public final native boolean isQualifiedDayOfWeekRecurrence()
  /*-{
    return 'dayOfWeekRecurrences' in this && this.dayOfWeekRecurrences !== null &&
      'recurrences' in this.dayOfWeekRecurrences &&
      this.dayOfWeekRecurrences.recurrences !== null &&
      this.dayOfWeekRecurrences.recurrences.length === 1 &&
      'qualifiedDayOfWeek' in this.dayOfWeekRecurrences.recurrences[0] &&
      this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek !== null &&
      'qualifier' in this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek &&
      this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek.qualifier !== null &&
      'dayOfWeek' in this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek &&
      this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek.dayOfWeek !== null;
  }-*/;

  public final native String getDayOfWeekQualifier()
  /*-{
    if ('dayOfWeekRecurrences' in this && this.dayOfWeekRecurrences !== null &&
      'recurrences' in this.dayOfWeekRecurrences &&
      this.dayOfWeekRecurrences.recurrences !== null &&
      this.dayOfWeekRecurrences.recurrences.length === 1 &&
      'qualifiedDayOfWeek' in this.dayOfWeekRecurrences.recurrences[0] &&
      this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek !== null &&
      'qualifier' in this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek) {
        return this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek.qualifier;
    }
    return "";
  }-*/;

  public final native void setDayOfWeekQualifier( String qualifier )
  /*-{
    if (!('dayOfWeekRecurrences' in this) || !this.dayOfWeekRecurrences) {
      this.dayOfWeekRecurrences = { recurrences: [ {qualifiedDayOfWeek: {}}] };
    }
    if ( !('recurrences' in this.dayOfWeekRecurrences) || !this.dayOfWeekRecurrences.recurrences ) {
      this.dayOfWeekRecurrences = { recurrences: [ {qualifiedDayOfWeek: {}}] };
    }
    if ( this.dayOfWeekRecurrences.recurrences.length === 0 ) {
      this.dayOfWeekRecurrences.recurrences.push( { qualifiedDayOfWeek: {} } );
    }
    if (!('qualifiedDayOfWeek' in this.dayOfWeekRecurrences.recurrences[0]) || !this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek) {
      this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek = {};
    }
    this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek.qualifier = qualifier;
  }-*/;

  public final native String getQualifiedDayOfWeek()
  /*-{
    if ('dayOfWeekRecurrences' in this && this.dayOfWeekRecurrences !== null &&
      'recurrences' in this.dayOfWeekRecurrences &&
      this.dayOfWeekRecurrences.recurrences !== null &&
      this.dayOfWeekRecurrences.recurrences.length === 1 &&
      'qualifiedDayOfWeek' in this.dayOfWeekRecurrences.recurrences[0] &&
      this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek !== null) {
        return this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek.dayOfWeek;
    }
    return "";
  }-*/;

  public final native void setQualifiedDayOfWeek( String dayOfWeek )
  /*-{
    if (!('dayOfWeekRecurrences' in this) || !this.dayOfWeekRecurrences) {
      this.dayOfWeekRecurrences = { recurrences: [ {qualifiedDayOfWeek: {}}] };
    }
    if ( !('recurrences' in this.dayOfWeekRecurrences) || !this.dayOfWeekRecurrences.recurrences ) {
      this.dayOfWeekRecurrences = { recurrences: [ {qualifiedDayOfWeek: {}}] };
    }
    if ( this.dayOfWeekRecurrences.recurrences.length === 0 ) {
      this.dayOfWeekRecurrences.recurrences.push( { qualifiedDayOfWeek: {} } );
    }
    if (!('qualifiedDayOfWeek' in this.dayOfWeekRecurrences.recurrences[0]) || !this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek) {
      this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek = {};
    }
    this.dayOfWeekRecurrences.recurrences[0].qualifiedDayOfWeek.dayOfWeek = dayOfWeek;
  }-*/;

  public final int[] getDayOfMonthRecurrences() {
    return convertJsArrayStringToIntArray( getDayOfMonthRecurrencesRaw() );
  }

  private final native JsArrayString getDayOfMonthRecurrencesRaw()
  /*-{
    if ('dayOfMonthRecurrences' in this && this.dayOfMonthRecurrences !== null &&
      'recurrences' in this.dayOfMonthRecurrences &&
      this.dayOfMonthRecurrences.recurrences.length > 0 && 
      'recurrenceList' in this.dayOfMonthRecurrences.recurrences[0] &&
      'values' in this.dayOfMonthRecurrences.recurrences[0].recurrenceList) {
      return this.dayOfMonthRecurrences.recurrences[0].recurrenceList.values;
    } else if('dayOfMonthRecurrences' in this && this.dayOfMonthRecurrences !== null &&
      'recurrences' in this.dayOfMonthRecurrences &&
      this.dayOfMonthRecurrences.recurrences.length > 0 && 
      'incrementalRecurrence' in this.dayOfMonthRecurrences.recurrences[0] &&
      this.dayOfMonthRecurrences.recurrences[0].incrementalRecurrence !== null && 
      'increment' in this.dayOfMonthRecurrences.recurrences[0].incrementalRecurrence &&
      this.dayOfMonthRecurrences.recurrences[0].incrementalRecurrence.increment !== null) {
      return this.dayOfMonthRecurrences.recurrences[0].incrementalRecurrence.increment;
    }
    return null;
  }-*/;

  public final native void setDayOfMonthRecurrences( JsArrayInteger days )
  /*-{
    if (!('dayOfMonthRecurrences' in this) || !this.dayOfMonthRecurrences) {
      this.dayOfMonthRecurrences = { recurrences: [{recurrenceList: {values: []}}] };
    }
    if ( !('recurrences' in this.dayOfMonthRecurrences) || !this.dayOfMonthRecurrences.recurrences ) {
      this.dayOfMonthRecurrences = { recurrences: [{recurrenceList: {values: []}}] };
    }
    if (this.dayOfMonthRecurrences.recurrences.length === 0) {
      this.dayOfMonthRecurrences.recurrences.push( {recurrenceList: {values: []}} );
    }
    if (!('recurrenceList' in this.dayOfMonthRecurrences.recurrences[0]) || !this.dayOfMonthRecurrences.recurrences[0].recurrenceList) {
      this.dayOfMonthRecurrences.recurrences[0].recurrenceList = { values: [] };
    }
    this.dayOfMonthRecurrences.recurrences[0].recurrenceList.values = days;
  }-*/;

  public final int[] getMonthlyRecurrences() {
    return convertJsArrayStringToIntArray( getMonthlyRecurrencesRaw() );
  }

  private final native JsArrayString getMonthlyRecurrencesRaw()
  /*-{
    if ('monthlyRecurrences' in this && this.monthlyRecurrences !== null &&
      'recurrences' in this.monthlyRecurrences &&
      this.monthlyRecurrences.recurrences !== null &&
      this.monthlyRecurrences.recurrences.length > 0 && 
      'recurrenceList' in this.monthlyRecurrences.recurrences[0] &&
      'values' in this.monthlyRecurrences.recurrences[0].recurrenceList) {
      return this.monthlyRecurrences.recurrences[0].recurrenceList.values;
    } else {
      return null;
    }
  }-*/;

  public final native void setMonthlyRecurrences( JsArrayInteger months )
  /*-{
    if (!('monthlyRecurrences' in this) || !this.monthlyRecurrences) {
      this.monthlyRecurrences = { recurrences: [{recurrenceList: {values: []}}] };
    }
    if ( !('recurrences' in this.monthlyRecurrences) || !this.monthlyRecurrences.recurrences ) {
      this.monthlyRecurrences = { recurrences: [{recurrenceList: {values: []}}] };
    }
    if (this.monthlyRecurrences.recurrences.length === 0) {
      this.monthlyRecurrences.recurrences.push( {recurrenceList: {values: []}} );
    }
    if (!('recurrenceList' in this.monthlyRecurrences.recurrences[0]) || !this.monthlyRecurrences.recurrences[0].recurrenceList) {
      this.monthlyRecurrences.recurrences[0].recurrenceList = { values: [] };
    }
    this.monthlyRecurrences.recurrences[0].recurrenceList.values = months;
  }-*/;

  public final native int[] getYearlyRecurrences()
  /*-{
    if ('yearlyRecurrences' in this && this.yearlyRecurrences !== null &&
      'recurrences' in this.yearlyRecurrences &&
      this.yearlyRecurrences.recurrences !== null &&
      this.yearlyRecurrences.recurrences.length > 0 &&
      'recurrenceList' in this.yearlyRecurrences.recurrences[0] &&
      this.yearlyRecurrences.recurrences[0].recurrenceList !== null &&
      'values' in this.yearlyRecurrences.recurrences[0].recurrenceList) {
        return this.yearlyRecurrences.recurrences[0].recurrenceList.values;
    }
    return null;
  }-*/;

  public final native void setYearlyRecurrences( JsArrayInteger years )
  /*-{
    if (!('yearlyRecurrences' in this) || !this.yearlyRecurrences) {
      this.yearlyRecurrences = { recurrences: [{ recurrenceList: { values: [] } }] };
    }
    if ( !('recurrences' in this.yearlyRecurrences) || !this.yearlyRecurrences.recurrences ) {
      this.yearlyRecurrences = { recurrences: [{ recurrenceList: { values: [] } }] };
    }
    if ( this.yearlyRecurrences.recurrences.length === 0 ) {
      this.yearlyRecurrences.recurrences.push( { recurrenceList: { values: [] } } );
    }
    if (!('recurrenceList' in this.yearlyRecurrences.recurrences[0]) || !this.yearlyRecurrences.recurrences[0].recurrenceList) {
      this.yearlyRecurrences.recurrences[0].recurrenceList = { values: [] };
    }
    this.yearlyRecurrences.recurrences[0].recurrenceList.values = years;
  }-*/;

  public final native String getCronString()
  /*-{
    return this.cronString;
  }-*/;

  public final native void setCronString( String cronString )
  /*-{
    this.cronString = cronString;
  }-*/;

  public final native String getCronDescription()
  /*-{
    return this.cronDescription;
  }-*/;

  public final native void setCronDescription( String cronDescription )
  /*-{
    this.cronDescription = cronDescription;
  }-*/;

  public final String getDescription( String defaultTimeZone ) {
    StringBuilder trigDesc = new StringBuilder();
    ScheduleType scheduleType = ScheduleType.valueOf( getScheduleType() );
    if ( scheduleType == ScheduleType.RUN_ONCE ) {
      return Messages.getString( "schedule.runOnce" );
    }
    if ( "cronJobTrigger".equals( getType() ) || ( getUiPassParamRaw()
      != null && getUiPassParamRaw().equals( "CRON" ) ) ) {
      if ( scheduleType == ScheduleType.DAILY && getCronDescription() != null && !getCronDescription().isEmpty() ) {
        trigDesc.append( getCronDesc( defaultTimeZone ) );
      } else {
        trigDesc.append( "CRON: " ).append( getCronString() );
      }
    } else if ( COMPLEX_JOB_TRIGGER.equals( getType() ) ) {
      try {
        // need to digest the recurrences
        int[] monthsOfYear = getMonthlyRecurrences();
        int[] daysOfMonth = getDayOfMonthRecurrences();

        // we are "YEARLY" if
        // monthsOfYear, daysOfMonth OR
        // monthsOfYear, qualifiedDayOfWeek
        if ( monthsOfYear != null && monthsOfYear.length > 0 ) {
          if ( isQualifiedDayOfWeekRecurrence() ) {
            // monthsOfYear, qualifiedDayOfWeek
            String qualifier = getDayOfWeekQualifier();
            String dayOfWeek = getQualifiedDayOfWeek();
            trigDesc = new StringBuilder(
              Messages.getString( "the" ) + " " + Messages.getString( WeekOfMonth.valueOf( qualifier ).toString() )
                + " "
                + Messages.getString( DayOfWeek.valueOf( dayOfWeek ).toString() ) + " " + Messages.getString( "of" )
                + " "
                + Messages.getString( MonthOfYear.get( monthsOfYear[ 0 ] - 1 ).toString() ) );
          } else {
            // monthsOfYear, daysOfMonth
            trigDesc = new StringBuilder( Messages.getString( EVERY ) + " " + Messages.getString(
              MonthOfYear.get( monthsOfYear[ 0 ] - 1 ).toString() ) + " " + daysOfMonth[ 0 ] );
          }
        } else if ( daysOfMonth != null && daysOfMonth.length > 0 ) {
          // MONTHLY: Day N of every month
          trigDesc = new StringBuilder(
            Messages.getString( "day" ) + " " + daysOfMonth[ 0 ] + " " + Messages.getString( "ofEveryMonth" ) );
        } else if ( isQualifiedDayOfWeekRecurrence() ) {
          // MONTHLY: The <qualifier> <dayOfWeek> of every month at <time>
          String qualifier = getDayOfWeekQualifier();
          String dayOfWeek = getQualifiedDayOfWeek();

          trigDesc = new StringBuilder(
            Messages.getString( "the" ) + " " + Messages.getString( WeekOfMonth.valueOf( qualifier ).toString() ) + " "
              + Messages.getString( DayOfWeek.valueOf( dayOfWeek ).toString() ) + " " + Messages.getString(
              "ofEveryMonth" ) );
        } else if ( Objects.requireNonNull( getDayOfWeekRecurrences() ).length > 0 ) {
          // WEEKLY: Every week on <day>..<day> at <time>
          // check if weekdays first
          if ( getDayOfWeekRecurrences().length == 5 && getDayOfWeekRecurrences()[ 0 ] == 2
            && getDayOfWeekRecurrences()[ 4 ] == 6 ) {
            trigDesc = new StringBuilder( Messages.getString( EVERY ) + " " + Messages.getString( "weekday" ) );
          } else {

            int variance = 0;
            if ( getNativeStartTime().indexOf( '.' ) < 0 ) {
              if ( DateTimeFormat.getFormat( "yyyy-MM-dd'T'HH:mm:ssZZZ" ).parse( getNativeStartTime() ) != null ) {
                variance = TimeUtil.getDayVariance( getStartTime().getHours(), getStartTime().getMinutes(),
                  getNativeStartTime() );
              }
            } else {
              if ( DateTimeFormat.getFormat( "yyyy-MM-dd'T'HH:mm:ss" )
                .parse( getNativeStartTime().substring( 0, getNativeStartTime().indexOf( '.' ) ) ) != null ) {
                variance = TimeUtil.getDayVariance( getStartTime().getHours(), getStartTime().getMinutes(),
                  getNativeStartTime() );
              }
            }

            int adjustedDayOfWeek =
              TimeUtil.getDayOfWeek( DayOfWeek.get( Objects.requireNonNull( getDayOfWeekRecurrences() )[ 0 ] - 1 ),
                variance );

            trigDesc = new StringBuilder( Messages.getString( EVERY ) + " "
              + Messages.getString( DayOfWeek.get( adjustedDayOfWeek )
              .toString().trim() ) );
            for ( int i = 1; i < Objects.requireNonNull( getDayOfWeekRecurrences() ).length; i++ ) {
              adjustedDayOfWeek =
                TimeUtil.getDayOfWeek( DayOfWeek.get( Objects.requireNonNull( getDayOfWeekRecurrences() )[ i ] - 1 ),
                  variance );
              trigDesc.append( ", " ).append( Messages.getString( DayOfWeek.get( adjustedDayOfWeek )
                .toString().trim() ) );
            }
          }
        }

        String timeFormatString = getStartTimeStringInJobTimeZone();

        trigDesc.append( " " ).append( Messages.getString( AT ) ).append( " " )
          .append( timeFormatString ).append( " " ).append( getRepeatsTimeZone( defaultTimeZone ) );
      } catch ( Throwable th ) {
        if ( getUiPassParamRaw() != null && getUiPassParamRaw().equals( "DAILY" ) ) {
          trigDesc.append( getCronDesc( defaultTimeZone ) );
        } else {
          trigDesc.append( getCronDescription() );
        }
      }
    } else if ( SIMPLE_JOB_TRIGGER.equals( getType() ) ) {
      // if (getRepeatInterval() > 0) {
      trigDesc = new StringBuilder( getSimpleDescription( defaultTimeZone ) );

      // if (getStartTime() != null) {
      // trigDesc += " from " + getStartTime();
      // }
      // if (getEndTime() != null) {
      // trigDesc += " until " + getEndTime();
      // }
    }
    return trigDesc.toString();
  }

  private String getStartTimeStringInJobTimeZone() {
    // since we want this time to be in the time zone selected, we can just grab the component numbers from the trigger
    // no need for fancy date math.
    String startHourString = Integer.toString( getStartHour() );
    if ( getStartHour() < 10 ) {
      startHourString = "0" + startHourString;
    }
    String startMinString = Integer.toString( getStartMin() );
    if ( getStartMin() < 10 ) {
      startMinString = "0" + startMinString;
    }
    return startHourString + ":" + startMinString + ":00";
  }

  public final String getSimpleDescription( String defaultTimeZone ) {
    ScheduleType scheduleType = getSimpleScheduleType();
    String trigDesc;
    String intervalUnits = "";
    int intervalSeconds = 1;
    if ( scheduleType == ScheduleType.DAILY ) {
      intervalSeconds = 86400;
      intervalUnits = timeUnitText( intervalSeconds, "day" );
      // DateTimeFormat timeFormat = DateTimeFormat.getFormat(PredefinedFormat.TIME_MEDIUM);
      if ( getRepeatInterval() == intervalSeconds ) {
        intervalUnits = Messages.getString( "dayAtLowercase" );
      }
      // else {
      // intervalUnits += " " + Messages.getString( AT_STRING );
      // }
      // intervalUnits += " " + timeFormat.format(getStartTime());
    } else if ( scheduleType == ScheduleType.HOURS ) {
      intervalSeconds = 3600;
      intervalUnits = timeUnitText( intervalSeconds, "hour" );
    } else if ( scheduleType == ScheduleType.MINUTES ) {
      intervalSeconds = 60;
      intervalUnits = timeUnitText( intervalSeconds, "minute" );
    } else if ( scheduleType == ScheduleType.SECONDS ) {
      intervalSeconds = 1;
      intervalUnits = timeUnitText( intervalSeconds, "second" );
    } else if ( scheduleType == ScheduleType.WEEKLY ) {
      intervalSeconds = 604800;
      intervalUnits = Messages.getString( "weekly" );
    }
    String startTimeString = getStartTimeStringInJobTimeZone();
    if ( scheduleType == ScheduleType.WEEKLY ) {
      int repeatInterval = getRepeatInterval();
      trigDesc =
        Messages.getString( EVERY ) + " " + ( repeatInterval / 86400 ) + " " + Messages.getString( "daysLower" );
      trigDesc +=
        " " + Messages.getString( AT ) + " " + startTimeString + " " + getRepeatsTimeZone( defaultTimeZone );
    } else {
      trigDesc = Messages.getString( EVERY ) + " " + intervalUnits;
      trigDesc +=
        " " + Messages.getString( AT ) + " " + startTimeString + " " + getRepeatsTimeZone( defaultTimeZone );
    }
    if ( getRepeatCount() > 0 ) {
      trigDesc += "; " + Messages.getString( "run" ) + " " + getRepeatCount() + " " + Messages.getString( "times" );
    }
    // if (getStartTime() != null) {
    // trigDesc += " from " + getStartTime();
    // }
    // if (getEndTime() != null) {
    // trigDesc += " until " + getEndTime();
    // }
    return trigDesc;
  }


  public final String getCronDesc( String defaultTimeZone ) {
    ScheduleType scheduleType = getSimpleScheduleType();
    String trigDesc;
    String intervalUnits = "";
    int intervalSeconds = 1;
    if ( scheduleType == ScheduleType.DAILY ) {
      intervalSeconds = 86400;
      intervalUnits = timeUnitText( intervalSeconds, "day" );
      if ( getRepeatInterval() == intervalSeconds ) {
        intervalUnits = Messages.getString( "dayAtLowercase" );
      }
    }
    String startTimeString = getStartTimeStringInJobTimeZone();

    trigDesc = Messages.getString( EVERY ) + " " + intervalUnits;
    trigDesc +=
      " " + Messages.getString( AT ) + " " + startTimeString + " " + getRepeatsTimeZone( defaultTimeZone );
    if ( getRepeatCount() > 0 ) {
      trigDesc += "; " + Messages.getString( "run" ) + " " + getRepeatCount() + " " + Messages.getString( "times" );
    }
    return trigDesc;
  }


  public final String timeUnitText( int intervalSeconds, String timeUnit ) {
    if ( getRepeatInterval() == intervalSeconds ) {
      return Messages.getString( timeUnit );
    } else {
      return getRepeatInterval() / intervalSeconds + " " + Messages.getString( timeUnit + "s" );
    }
  }

  public final String oldGetScheduleType() {
    if ( COMPLEX_JOB_TRIGGER.equals( getType() ) ) {
      // need to digest the recurrences
      int[] monthsOfYear = getMonthlyRecurrences();
      int[] daysOfMonth = getDayOfMonthRecurrences();

      // we are "YEARLY" if
      // monthsOfYear, daysOfMonth OR
      // monthsOfYear, qualifiedDayOfWeek
      if ( Objects.requireNonNull( monthsOfYear ).length > 0 ) {
        return "YEARLY";
      } else if ( Objects.requireNonNull( daysOfMonth ).length > 0 ) {
        // MONTHLY: Day N of every month
        return "MONTHLY";
      } else if ( isQualifiedDayOfWeekRecurrence() ) {
        // MONTHLY: The <qualifier> <dayOfWeek> of every month at <time>
        return "MONTHLY";
      } else if ( Objects.requireNonNull( getDayOfWeekRecurrences() ).length > 0 ) {
        // WEEKLY: Every week on <day>..<day> at <time>
        return "WEEKLY";
      }
    } else if ( SIMPLE_JOB_TRIGGER.equals( getType() ) ) {
      if ( getRepeatInterval() < 86400 ) {
        return "HOURLY";
      } else if ( getRepeatInterval() < 604800 ) {
        return "DAILY";
      } else if ( getRepeatInterval() == 604800 ) {
        return "WEEKLY";
      }
    }
    return null;
  }

  /**
   * Intended to deduce the ScheduleType if not already set. This method should only be called if the schedule type
   * is unassigned.
   *
   * @return
   */
  public final ScheduleType calcScheduleType() {
    if ( COMPLEX_JOB_TRIGGER.equals( getType() ) ) {
      // need to digest the recurrences
      int[] monthsOfYear = getMonthlyRecurrences();
      int[] daysOfMonth = getDayOfMonthRecurrences();
      // we are "YEARLY" if
      // monthsOfYear, daysOfMonth OR
      // monthsOfYear, qualifiedDayOfWeek
      if ( monthsOfYear != null && monthsOfYear.length > 0 ) {
        return ScheduleType.YEARLY;
      } else if ( daysOfMonth != null && daysOfMonth.length > 0 ) {
        // MONTHLY: Day N of every month
        return ScheduleType.MONTHLY;
      } else if ( isQualifiedDayOfWeekRecurrence() ) {
        // MONTHLY: The <qualifier> <dayOfWeek> of every month at <time>
        return ScheduleType.MONTHLY;

      } else if ( isWorkDaysInWeek() ) {
        return ScheduleType.DAILY;
      } else {
        return ScheduleType.WEEKLY;
      }
    } else if ( SIMPLE_JOB_TRIGGER.equals( getType() ) ) {
      return getSimpleScheduleType();
    } else {
      return ScheduleType.CRON; // cron trigger
    }
  }

  private ScheduleType getSimpleScheduleType() {
    if ( getRepeatInterval() == 0 ) {
      return ScheduleType.RUN_ONCE;
    } else if ( getRepeatInterval() % 604800 == 0 ) {
      return ScheduleType.WEEKLY;
    } else if ( getRepeatInterval() % 86400 == 0 ) {
      return ScheduleType.DAILY;
    } else if ( getRepeatInterval() % 3600 == 0 ) {
      return ScheduleType.HOURS;
    } else if ( getRepeatInterval() % 60 == 0 ) {
      return ScheduleType.MINUTES;
    } else if ( getRepeatInterval() > 0 ) {
      return ScheduleType.SECONDS;
    } else {
      return ScheduleType.RUN_ONCE;
    }
  }

  public final boolean isWorkDaysInWeek() {
    int[] daysOfWeek = getDayOfWeekRecurrences();
    if ( daysOfWeek == null || daysOfWeek.length != 5 ) {
      return false;
    } else {
      for ( int i = 0; i < 5; i++ ) {
        if ( daysOfWeek[ i ] != i + 2 ) {
          return false;
        }
      }
      return true;
    }
  }

  private String getRepeatsTimeZone( String defaultTimeZone ) {
    String timeZone = getTimeZone();
    if ( timeZone != null && !timeZone.trim().isEmpty() && !"undefined".equals( timeZone ) ) {
      return timeZone.trim();
    }
    return defaultTimeZone;
  }

  public final native Date getNextFireTime() /*-{ return this.nextFireTime; }-*/;

  public final native String getName() /*-{ return this.name; }-*/;

  public final native boolean getEnableSafeMode() /*-{ return this.enableSafeMode; }-*/;

  public final native void setEnableSafeMode( boolean enableSafeMode ) /*-{ this.enableSafeMode = enableSafeMode; }-*/;

  public final native boolean getGatherMetrics() /*-{ return this.gatherMetrics; }-*/;

  public final native void setGatherMetrics( boolean gatherMetrics ) /*-{ this.gatherMetrics = gatherMetrics; }-*/;

  public final native String getLogLevel() /*-{ return this.logLevel; }-*/;

  public final native void setLogLevel( boolean logLevel ) /*-{ this.logLevel = logLevel; }-*/;

  public final native String getTimeZone() /*-{ return this.timeZone; }-*/;

  public final native void setTimeZone( String timeZone ) /*-{ this.timeZone = timeZone; }-*/;

  public final native int getStartHour() /*-{ return parseInt(this.startHour); }-*/;

  public final native void setStartHour( int startHour ) /*-{ this.startHour = startHour; }-*/;

  public final native int getStartMin() /*-{ return parseInt(this.startMin); }-*/;

  public final native int getStartYear() /*-{ return parseInt(this.startYear); }-*/;

  public final native void setStartYear( int startYear ) /*-{ this.startYear = startYear; }-*/;

  public final native int getStartMonth() /*-{ return parseInt(this.startMonth); }-*/;

  public final native void setStartMonth( int startMonth ) /*-{ this.startMonth = startMonth; }-*/;

  public final native int getStartDay() /*-{  return parseInt(this.startDay); }-*/;

  public final native void setStartDay( int startDay ) /*-{ this.startDay = startDay; }-*/;

  public final native int getStartAmPm() /*-{ return parseInt(this.startAmPm); }-*/;

  public final native void setStartAmPm( int startAmPm ) /*-{ this.startAmPm = startstartAmPm; }-*/;
}
