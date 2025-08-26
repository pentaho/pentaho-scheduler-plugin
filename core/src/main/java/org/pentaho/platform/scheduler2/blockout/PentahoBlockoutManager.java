/*
 * ! ******************************************************************************
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


package org.pentaho.platform.scheduler2.blockout;

import java.util.ArrayList;
import java.util.List;

import org.pentaho.platform.api.scheduler2.IBlockoutManager;
import org.pentaho.platform.api.scheduler2.IJob;
import org.pentaho.platform.api.scheduler2.IJobFilter;
import org.pentaho.platform.api.scheduler2.IJobTrigger;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.scheduler2.quartz.BlockingQuartzJob;

public class PentahoBlockoutManager implements IBlockoutManager {

  private IScheduler scheduler;

  public PentahoBlockoutManager() {

  }

  protected IScheduler getScheduler() {
    if ( scheduler == null ) {
      scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", null ); //$NON-NLS-1$
    }
    return scheduler;
  }

  public IJobTrigger getBlockOut( String blockOutJobId ) {
    try {
      IJob blockOutJob = getScheduler().getJob( blockOutJobId );
      IJobTrigger blockOutJobTrigger = blockOutJob.getJobTrigger();
      blockOutJobTrigger.setDuration( ( (Number) blockOutJob.getJobParams().get( DURATION_PARAM ) ).longValue() );
      return blockOutJobTrigger;
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  @Override
  public List<IJob> getBlockOutJobs() {
    try {
      return getScheduler().getJobs( ( IJob j ) -> {
        if ( BLOCK_OUT_JOB_NAME.equals( j.getJobName() ) ) {
          j.getJobTrigger().setDuration( ( (Number) j.getJobParams().get( DURATION_PARAM ) ).longValue() );
          return true;
        }
        return false;
      } );

    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  @Override
  public boolean willFire( IJobTrigger scheduleTrigger ) {

    return BlockoutManagerUtil.willFire( scheduleTrigger, getBlockOutJobTriggers(), getScheduler() );
  }

  @Override
  public boolean shouldFireNow() {
    return BlockoutManagerUtil.shouldFireNow( getBlockOutJobTriggers(), getScheduler() );
  }

  public List<IJobTrigger> willBlockSchedules( IJobTrigger testBlockOutJobTrigger ) {
    List<IJobTrigger> blockedSchedules = new ArrayList<IJobTrigger>();

    List<IJob> scheduledJobs = new ArrayList<>();
    try {
      scheduledJobs = getScheduler().getJobs( ( IJob job ) -> !BLOCK_OUT_JOB_NAME.equals( job.getJobName() ) );
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }

    // Loop over trigger group names
    for ( IJob scheduledJob : scheduledJobs ) {

      // Add schedule to list if block out conflicts at all
      if ( BlockoutManagerUtil.willBlockSchedule( scheduledJob.getJobTrigger(),
        testBlockOutJobTrigger, getScheduler() ) ) {
        blockedSchedules.add( scheduledJob.getJobTrigger() );
      }
    }

    return blockedSchedules;
  }

  @Override
  public boolean isPartiallyBlocked( IJobTrigger scheduleJobTrigger ) {
    return BlockoutManagerUtil.isPartiallyBlocked( scheduleJobTrigger, getBlockOutJobTriggers(), getScheduler() );
  }

  private List<IJobTrigger> getBlockOutJobTriggers() {
    List<IJobTrigger> blockOutJobTriggers = new ArrayList<IJobTrigger>();

    for ( IJob blockOutJob : getBlockOutJobs() ) {
      blockOutJobTriggers.add( blockOutJob.getJobTrigger() );
    }

    return blockOutJobTriggers;
  }

}
