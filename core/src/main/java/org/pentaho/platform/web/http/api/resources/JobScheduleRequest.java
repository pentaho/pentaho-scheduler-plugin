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


package org.pentaho.platform.web.http.api.resources;

import org.pentaho.platform.api.scheduler2.CronJobTrigger;
import org.pentaho.platform.api.scheduler2.ICronJobTrigger;
import org.pentaho.platform.api.scheduler2.IJobScheduleParam;
import org.pentaho.platform.api.scheduler2.IJobScheduleRequest;
import org.pentaho.platform.api.scheduler2.ISimpleJobTrigger;
import org.pentaho.platform.api.scheduler2.JobState;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@XmlRootElement
public class JobScheduleRequest implements Serializable, IJobScheduleRequest {

  private static final long serialVersionUID = -485489832281790257L;

  public static final int SUNDAY = 0;

  public static final int MONDAY = 1;

  public static final int TUESDAY = 2;

  public static final int WEDNESDAY = 3;

  public static final int THURSDAY = 4;

  public static final int FRIDAY = 5;

  public static final int SATURDAY = 6;

  public static final int JANUARY = 0;

  public static final int FEBRUARY = 1;

  public static final int MARCH = 2;

  public static final int APRIL = 3;

  public static final int MAY = 4;

  public static final int JUNE = 5;

  public static final int JULY = 6;

  public static final int AUGUST = 7;

  public static final int SEPTEMBER = 8;

  public static final int OCTOBER = 9;

  public static final int NOVEMBER = 10;

  public static final int DECEMBER = 11;

  public static final int LAST_WEEK_OF_MONTH = 4;

  String jobName;

  String jobId;

  JobState jobState;

  String inputFile;

  String outputFile;

  String actionClass;

  CronJobTrigger cronJobTrigger;

  ComplexJobTriggerProxy complexJobTrigger;

  SimpleJobTrigger simpleJobTrigger;

  List<IJobScheduleParam> jobParameters = new ArrayList<>();

  Map<String, String> pdiParameters;

  long duration;

  String timeZone;

  protected String runSafeMode;
  protected String gatheringMetrics;
  protected String logLevel;

  public String getInputFile() {
    return inputFile;
  }

  public void setInputFile( String file ) {
    this.inputFile = file;
  }

  public String getOutputFile() {
    return outputFile;
  }

  public void setOutputFile( String file ) {
    this.outputFile = file;
  }

  @Override public void setPdiParameters( Map<String, String> stringStringHashMap ) {
    this.pdiParameters = stringStringHashMap;
  }

  public CronJobTrigger getCronJobTrigger() {
    return cronJobTrigger;
  }

  public void setCronJobTrigger( CronJobTrigger jobTrigger ) {
    if ( jobTrigger != null ) {
      setComplexJobTrigger( null );
      setSimpleJobTrigger( null );
    }
    this.cronJobTrigger = jobTrigger;
  }

  public ComplexJobTriggerProxy getComplexJobTrigger() {
    return complexJobTrigger;
  }

  public void setComplexJobTrigger( ComplexJobTriggerProxy jobTrigger ) {
    if ( jobTrigger != null ) {
      setCronJobTrigger( null );
      setSimpleJobTrigger( null );
    }
    this.complexJobTrigger = jobTrigger;
  }

  public SimpleJobTrigger getSimpleJobTrigger() {
    return simpleJobTrigger;
  }

  public void setSimpleJobTrigger( SimpleJobTrigger jobTrigger ) {
    if ( jobTrigger != null ) {
      setCronJobTrigger( null );
      setComplexJobTrigger( null );
    }
    this.simpleJobTrigger = jobTrigger;
  }

  @XmlElement( type=JobScheduleParam.class )
  public List<IJobScheduleParam> getJobParameters() {
    return jobParameters;
  }

  public void setJobParameters( List<IJobScheduleParam> jobParameters ) {
    if ( jobParameters !=  this.jobParameters ) {
      this.jobParameters.clear();
      if ( jobParameters != null ) {
        this.jobParameters.addAll( jobParameters );
      }
    }
  }

  public Map<String, String> getPdiParameters() {
    return pdiParameters;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName( String jobName ) {
    this.jobName = jobName;
  }

  public JobState getJobState() {
    return jobState;
  }

  public void setJobState( JobState jobState ) {
    this.jobState = jobState;
  }

  public String getActionClass() {
    return actionClass;
  }

  public void setActionClass( String actionClass ) {
    this.actionClass = actionClass;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration( long duration ) {
    this.duration = duration;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public void setTimeZone( String timeZone ) {
    this.timeZone = timeZone;
  }

  @Override public void setSimpleJobTrigger( ISimpleJobTrigger jobTrigger ) {
    simpleJobTrigger = (SimpleJobTrigger) jobTrigger;
  }

  @Override public void setCronJobTrigger( ICronJobTrigger cron ) {
    cronJobTrigger = (CronJobTrigger) cron;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId( String jobId ) {
    this.jobId = jobId;
  }

  public String getRunSafeMode() {
    return runSafeMode;
  }

  public void setRunSafeMode( String runSafeMode ) {
    this.runSafeMode = runSafeMode;
  }

  public String getGatheringMetrics() {
    return gatheringMetrics;
  }

  public void setGatheringMetrics( String gatheringMetrics ) {
    this.gatheringMetrics = gatheringMetrics;
  }

  public String getLogLevel() {
    return logLevel;
  }

  public void setLogLevel( String logLevel ) {
    this.logLevel = logLevel;
  }
}
