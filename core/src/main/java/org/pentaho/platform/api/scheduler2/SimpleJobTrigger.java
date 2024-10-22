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
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * A simple way of specifying a schedule on which a job will fire as opposed to {@link ComplexJobTrigger}. The
 * {@link SimpleJobTrigger} can meet your needs if you are looking for a way to have a job start, execute a set number
 * of times on a regular interval and then end either after a specified number of runs or at an end date.
 * 
 * @author aphillips
 */
@XmlRootElement
public class SimpleJobTrigger extends JobTrigger implements ISimpleJobTrigger {
  private static final long serialVersionUID = 7838270781497116177L;
  public static final int REPEAT_INDEFINITELY = -1;
  private int repeatCount = 0;
  private long repeatInterval = 0;

  public SimpleJobTrigger( Date startTime, Date endTime, int repeatCount, long repeatIntervalSeconds ) {
    super( startTime, endTime );
    this.repeatCount = repeatCount;
    this.repeatInterval = repeatIntervalSeconds;
  }

  public SimpleJobTrigger() {
  }

  public int getRepeatCount() {
    return repeatCount;
  }

  public void setRepeatCount( int repeatCount ) {
    this.repeatCount = repeatCount;
  }

  public long getRepeatInterval() {
    return repeatInterval;
  }

  public void setRepeatInterval( long repeatIntervalSeconds ) {
    this.repeatInterval = repeatIntervalSeconds;
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append( "repeatCount=" ); //$NON-NLS-1$
    b.append( repeatCount );
    b.append( ", " ); //$NON-NLS-1$
    b.append( "repeatInterval=" ); //$NON-NLS-1$
    b.append( repeatInterval );
    b.append( ", " ); //$NON-NLS-1$
    b.append( "startTime=" ); //$NON-NLS-1$
    b.append( super.getStartTime() );
    b.append( ", " ); //$NON-NLS-1$
    b.append( "endTime=" ); //$NON-NLS-1$
    b.append( super.getEndTime() );
    return b.toString();
  }

}
