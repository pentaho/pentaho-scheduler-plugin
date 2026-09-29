/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/

package org.pentaho.platform.scheduler2.blockout;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;

import org.junit.Test;
import org.pentaho.platform.api.scheduler2.ComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.CronJobTrigger;
import org.pentaho.platform.api.scheduler2.IJobTrigger;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.scheduler2.quartz.QuartzScheduler;

public class BlockoutManagerUtilTest {

  private static final long SECOND = 1000L;

  @Test
  public void willFireReturnsTrueWhenThereAreNoBlockouts() {
    assertTrue( BlockoutManagerUtil.willFire( simpleTrigger( 0, null, 10, 60 ), Collections.emptyList(), null ) );
  }

  @Test
  public void willFireReturnsFalseWhenAnIdenticalSimpleBlockoutBlocksEveryOccurrence() {
    Date start = new Date( System.currentTimeMillis() - SECOND );
    Date end = new Date( start.getTime() + 4 * 60 * 60 * SECOND );
    SimpleJobTrigger schedule = new SimpleJobTrigger( start, end, SimpleJobTrigger.REPEAT_INDEFINITELY, 1 );
    SimpleJobTrigger blockout = new SimpleJobTrigger( start, end, SimpleJobTrigger.REPEAT_INDEFINITELY, 1 );
    blockout.setDuration( SECOND );

    assertFalse( BlockoutManagerUtil.willFire( schedule, Collections.<IJobTrigger>singletonList( blockout ),
      quartzScheduler() ) );
  }

  @Test
  public void willBlockScheduleReturnsTrueWhenSimpleTriggersMeetAtBlockoutBoundary() {
    SimpleJobTrigger schedule = simpleTrigger( 10, null, 1, 10 );
    SimpleJobTrigger blockout = simpleTrigger( 20, null, 1, 10 );
    blockout.setDuration( 0 );

    assertTrue( BlockoutManagerUtil.willBlockSchedule( schedule, blockout, null ) );
  }

  @Test
  public void willBlockScheduleReturnsFalseWhenFiniteSimpleTriggersDoNotOverlap() {
    SimpleJobTrigger schedule = simpleTrigger( 0, new Date( 20 * SECOND ), 1, 10 );
    SimpleJobTrigger blockout = simpleTrigger( 100, new Date( 120 * SECOND ), 1, 10 );
    blockout.setDuration( SECOND );

    assertFalse( BlockoutManagerUtil.willBlockSchedule( schedule, blockout, null ) );
  }

  @Test
  public void willBlockScheduleReturnsFalseWhenSimpleScheduleStartsAfterComplexBlockoutEnds() {
    ComplexJobTrigger blockout = new ComplexJobTrigger();
    blockout.setStartTime( new Date( 0 ) );
    blockout.setEndTime( new Date( 10 * SECOND ) );
    blockout.setDuration( SECOND );
    SimpleJobTrigger schedule = simpleTrigger( 20, null, 1, 10 );

    assertFalse( BlockoutManagerUtil.willBlockSchedule( schedule, blockout, quartzScheduler() ) );
  }

  @Test
  public void shouldFireNowReturnsFalseWhenCurrentTimeFallsInsideSimpleBlockout() {
    Date now = new Date();
    SimpleJobTrigger blockout = new SimpleJobTrigger( new Date( now.getTime() - SECOND ), null,
      SimpleJobTrigger.REPEAT_INDEFINITELY, 60 * 60 );
    blockout.setDuration( 60 * SECOND );

    assertFalse( BlockoutManagerUtil.shouldFireNow( Collections.<IJobTrigger>singletonList( blockout ), null ) );
  }

  @Test
  public void shouldFireNowReturnsTrueWhenNoBlockoutContainsCurrentTime() {
    SimpleJobTrigger futureBlockout = simpleTrigger( System.currentTimeMillis() / SECOND + 60, null,
      SimpleJobTrigger.REPEAT_INDEFINITELY, 60 * 60 );
    futureBlockout.setDuration( 60 * SECOND );

    assertTrue( BlockoutManagerUtil.shouldFireNow( Collections.<IJobTrigger>singletonList( futureBlockout ), null ) );
  }

  @Test
  public void isPartiallyBlockedReflectsWhetherAnyBlockoutOverlapsTheSchedule() {
    SimpleJobTrigger schedule = simpleTrigger( 10, null, 1, 10 );
    SimpleJobTrigger overlappingBlockout = simpleTrigger( 20, null, 1, 10 );
    overlappingBlockout.setDuration( 0 );

    assertFalse( BlockoutManagerUtil.isPartiallyBlocked( schedule, Collections.emptyList(), null ) );
    assertTrue( BlockoutManagerUtil.isPartiallyBlocked( schedule,
      Collections.<IJobTrigger>singletonList( overlappingBlockout ), null ) );
  }

  @Test
  public void isComplexTriggerIdentifiesComplexAndCronTriggersOnly() {
    assertTrue( BlockoutManagerUtil.isComplexTrigger( new ComplexJobTrigger() ) );
    assertTrue( BlockoutManagerUtil.isComplexTrigger( new CronJobTrigger() ) );
    assertFalse( BlockoutManagerUtil.isComplexTrigger( simpleTrigger( 0, null, 1, 1 ) ) );
  }

  private SimpleJobTrigger simpleTrigger( long startSeconds, Date end, int repeatCount, long repeatIntervalSeconds ) {
    return new SimpleJobTrigger( new Date( startSeconds * SECOND ), end, repeatCount, repeatIntervalSeconds );
  }

  private QuartzScheduler quartzScheduler() {
    return new QuartzScheduler();
  }
}
