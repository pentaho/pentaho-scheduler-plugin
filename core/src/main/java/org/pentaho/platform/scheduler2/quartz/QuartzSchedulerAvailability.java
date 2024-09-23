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


package org.pentaho.platform.scheduler2.quartz;

import java.util.Date;

import org.quartz.impl.calendar.BaseCalendar;

/**
 * Implementation of a Quartz calendar. Note that unlike typical Quartz calendars in which you specify when the trigger
 * is not allowed to fire, when constructing this calendar you specify when the trigger is allowed to fire.
 * 
 * @author arodriguez
 */
public class QuartzSchedulerAvailability extends BaseCalendar {
  private static final long serialVersionUID = 8419843512264409846L;
  Date startTime;
  Date endTime;

  /**
   * Default constructor for QuartzSchedulerAvailability.
   * Initializes the start time to the current time and the end time to 24 hours from the current time.
   */
  public QuartzSchedulerAvailability() {
    this.startTime = new Date();
    this.endTime = new Date( System.currentTimeMillis() + 86400000 );
  }

  /**
   * Creates a quartz calender which is used to indicate when a trigger is allowed to fire. The trigger will be allowed
   * to fire between the start date and end date.
   * 
   * @param startTime
   *          the earliest time at which the trigger may fire. If null the trigger may fire immediately.
   * @param endTime
   *          the last date at which the trigger may fire. If null the trigger may fire indefinitely.
   */
  public QuartzSchedulerAvailability( Date startTime, Date endTime ) {
    this.startTime = startTime;
    this.endTime = endTime;
  }

  /** {@inheritDoc} */
  public long getNextIncludedTime( long arg0 ) {
    long nextIncludedDate = 0;
    Date date = new Date( arg0 );
    if ( ( startTime != null ) && ( endTime != null ) ) {
      if ( !date.before( startTime ) && date.before( endTime ) ) {
        nextIncludedDate = arg0 + 1;
      } else if ( date.before( startTime ) ) {
        nextIncludedDate = startTime.getTime();
      }
    } else if ( startTime != null ) {
      if ( date.before( startTime ) ) {
        nextIncludedDate = startTime.getTime();
      } else {
        nextIncludedDate = arg0 + 1;
      }
    } else if ( endTime != null ) {
      if ( date.before( endTime ) ) {
        nextIncludedDate = arg0 + 1;
      }
    }
    return nextIncludedDate;
  }

  /** {@inheritDoc} */
  public boolean isTimeIncluded( long arg0 ) {
    boolean isIncluded = false;
    Date date = new Date( arg0 );
    if ( ( startTime != null ) && ( endTime != null ) ) {
      isIncluded = !date.before( startTime ) && !date.after( endTime );
    } else if ( startTime != null ) {
      isIncluded = !date.before( startTime );
    } else if ( endTime != null ) {
      isIncluded = !date.after( endTime );
    }
    return isIncluded;
  }

  public Date getStartTime() {
    return startTime;
  }

  public Date getEndTime() {
    return endTime;
  }
}
