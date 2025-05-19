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


package org.pentaho.platform.api.scheduler2;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The marker superclass for the various types of job triggers.
 * 
 * @author aphillips
 * 
 * @see SimpleJobTrigger
 * @see ComplexJobTrigger
 */
@JsonTypeInfo( use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type" )
@JsonSubTypes( {
  @JsonSubTypes.Type( value = SimpleJobTrigger.class, name = "simpleJobTrigger" ),
  @JsonSubTypes.Type( value = ComplexJobTrigger.class, name = "complexJobTrigger" ),
  @JsonSubTypes.Type(  value = CronJobTrigger.class, name = "cronJobTrigger")
})
@JsonInclude( JsonInclude.Include.NON_NULL )
public abstract class JobTrigger implements Serializable, IJobTrigger {
  /**
   * 
   */
  private static final long serialVersionUID = -2110414852036623140L;

  public static final SimpleJobTrigger ONCE_NOW = new SimpleJobTrigger( new Date(), null, 0, 0L );

  private Date startTime;

  private Date endTime;

  private String uiPassParam;

  private String cronString;

  private String cronDescription;

  private long duration = -1;

  private int startMin = -1;
  private int startHour = -1;
  private int startYear = -1;
  private int startMonth = -1;
  private int startDay = -1;
  private int startAmPm = -1;
  private String timeZone;

  public JobTrigger() {
  }

  public JobTrigger( Date startTime, Date endTime ) {
    this.startTime = startTime;
    this.endTime = endTime;
  }

  @Override
  @JsonFormat( shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ" )
  public Date getStartTime() {
    return startTime;
  }

  @Override
  public void setStartTime( Date startTime ) {
    this.startTime = startTime;
  }

  @Override
  @JsonFormat( shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ" )
  public Date getEndTime() {
    return endTime;
  }

  @Override
  public void setEndTime( Date endTime ) {
    this.endTime = endTime;
  }

  @Override
  public String getUiPassParam() {
    return uiPassParam;
  }

  @Override
  public void setUiPassParam( String uiPassParam ) {
    this.uiPassParam = uiPassParam;
  }

  @Override
  public String getCronString() {
    return cronString;
  }

  @Override
  public void setCronString( String cronString ) {
    this.cronString = cronString;
  }

  @Override
  public long getDuration() {
    return this.duration;
  }

  @JsonProperty( "duration" )
  public String getDurationAsString(){
    return String.valueOf( this.duration );
  }

  @Override
  public void setDuration( long duration ) {
    this.duration = duration;
  }


  public String getCronDescription() {
    return cronDescription;
  }

  public void setCronDescription(String cronDescription) {
    this.cronDescription = cronDescription;
  }

  @Override
  public int getStartHour() {
    return startHour;
  }

  @JsonProperty( "startHour" )
  public String getStartHourAsString(){
    return String.valueOf( this.startHour );
  }

  @Override
  public void setStartHour( int startHour ) {
    this.startHour = startHour;
  }

  @Override
  public int getStartMin() {
    return startMin;
  }

  @JsonProperty( "startMin" )
  public String getStartMinAsString(){
    return String.valueOf( this.startMin );
  }

  @Override
  public void setStartMin( int startMin ) {
    this.startMin = startMin;
  }

  @Override
  public int getStartYear() {
    return startYear;
  }

  @JsonProperty( "startYear" )
  public String getStartYearAsString(){
    return String.valueOf( this.startYear );
  }

  @Override
  public void setStartYear( int startYear ) {
    this.startYear = startYear;
  }

  @Override
  public int getStartMonth() {
    return startMonth;
  }

  @JsonProperty( "startMonth" )
  public String getStartMonthAsString(){
    return String.valueOf( this.startMonth );
  }

  @Override
  public void setStartMonth( int startMonth ) {
    this.startMonth = startMonth;
  }

  @Override
  public int getStartDay() {
    return startDay;
  }

  @JsonProperty( "startDay" )
  public String getStartDayAsString(){
    return String.valueOf( this.startDay );
  }

  @Override
  public void setStartDay( int startDay ) {
    this.startDay = startDay;
  }

  @Override
  public int getStartAmPm() {
    return startAmPm;
  }

  @JsonProperty( "startAmPm" )
  public String getStartAmPmAsString(){
    return String.valueOf( this.startAmPm );
  }

  @Override
  public void setStartAmPm( int startAmPm ) {
    this.startAmPm = startAmPm;
  }

  @Override
  public String getTimeZone() {
    return timeZone;
  }

  @Override
  public void setTimeZone( String timeZone ) {
    this.timeZone = timeZone;
  }
}
