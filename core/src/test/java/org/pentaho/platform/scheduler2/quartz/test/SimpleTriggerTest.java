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


package org.pentaho.platform.scheduler2.quartz.test;

import java.util.Calendar;

import org.junit.Assert;
import org.junit.Test;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;

@SuppressWarnings( "nls" )
public class SimpleTriggerTest {

  @Test
  public void defaultValidTest() {
    SimpleJobTrigger trigger = new SimpleJobTrigger();
    Assert.assertNull( trigger.getStartTime() );
    Assert.assertNull( trigger.getEndTime() );
    Assert.assertFalse( trigger.getRepeatCount() != 0 && trigger.getRepeatInterval() < 1 );
  }

  @Test
  public void defaultParamsNoDatesTest() {
    SimpleJobTrigger trigger = new SimpleJobTrigger();
    Assert.assertEquals( trigger.toString(), "repeatCount=0, repeatInterval=0, startTime=null, endTime=null" );
  }

  @Test
  public void defaultParamsDatesTest() {
    Calendar now = Calendar.getInstance();
    Calendar nextMonth = Calendar.getInstance();
    nextMonth.add( Calendar.MONTH, 1 );
    SimpleJobTrigger trigger = new SimpleJobTrigger( now.getTime(), nextMonth.getTime(), 1, 1000 );
    Assert.assertEquals( trigger.toString(), "repeatCount=1, repeatInterval=1000, startTime="
        + now.getTime().toString() + ", endTime=" + nextMonth.getTime().toString() );
  }

}
