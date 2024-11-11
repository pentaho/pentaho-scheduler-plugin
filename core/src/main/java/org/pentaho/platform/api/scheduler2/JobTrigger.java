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


package org.pentaho.platform.api.scheduler2;

import java.io.Serializable;
import java.util.Date;

import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * The marker superclass for the various types of job triggers.
 * 
 * @author aphillips
 * 
 * @see SimpleJobTrigger
 * @see ComplexJobTrigger
 */
@XmlSeeAlso( { SimpleJobTrigger.class, ComplexJobTrigger.class, CronJobTrigger.class } )
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

  public JobTrigger() {
  }

  public JobTrigger( Date startTime, Date endTime ) {
    this.startTime = startTime;
    this.endTime = endTime;
  }

  @Override
  public Date getStartTime() {
    return startTime;
  }

  @Override
  public void setStartTime( Date startTime ) {
    this.startTime = startTime;
  }

  @Override
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
}
