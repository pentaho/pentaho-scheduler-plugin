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
    this.scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", null ); //$NON-NLS-1$
  }

  public IJobTrigger getBlockOut( String blockOutJobId ) {
    try {
      IJob blockOutJob = this.scheduler.getJob( blockOutJobId );
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
      List<IJob> jobs = scheduler.getJobs( new IJobFilter() {
        @Override public boolean accept( IJob job ) {
          if ( BLOCK_OUT_JOB_NAME.equals( job.getJobName() ) ) {
            job.getJobTrigger().setDuration( ( (Number) job.getJobParams().get( DURATION_PARAM ) ).longValue() );
            return true;
          }
          return false;
        }
      } );
      return jobs;
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  @Override
  public boolean willFire( IJobTrigger scheduleTrigger ) {

    return BlockoutManagerUtil.willFire( scheduleTrigger, getBlockOutJobTriggers(), this.scheduler );
  }

  @Override
  public boolean shouldFireNow() {
    return BlockoutManagerUtil.shouldFireNow( getBlockOutJobTriggers(), this.scheduler );
  }

  public List<IJobTrigger> willBlockSchedules( IJobTrigger testBlockOutJobTrigger ) {
    List<IJobTrigger> blockedSchedules = new ArrayList<IJobTrigger>();

    List<IJob> scheduledJobs = new ArrayList<>();
    try {
      scheduledJobs = this.scheduler.getJobs( new IJobFilter() {

        @Override
        public boolean accept( IJob job ) {
          return !BLOCK_OUT_JOB_NAME.equals( job.getJobName() );
        }
      } );
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }

    // Loop over trigger group names
    for ( IJob scheduledJob : scheduledJobs ) {

      // Add schedule to list if block out conflicts at all
      if ( BlockoutManagerUtil.willBlockSchedule( scheduledJob.getJobTrigger(),
              testBlockOutJobTrigger, this.scheduler ) ) {
        blockedSchedules.add( scheduledJob.getJobTrigger() );
      }
    }

    return blockedSchedules;
  }

  @Override
  public boolean isPartiallyBlocked( IJobTrigger scheduleJobTrigger ) {
    return BlockoutManagerUtil.isPartiallyBlocked( scheduleJobTrigger, getBlockOutJobTriggers(), this.scheduler );
  }

  private List<IJobTrigger> getBlockOutJobTriggers() {
    List<IJobTrigger> blockOutJobTriggers = new ArrayList<IJobTrigger>();

    for ( IJob blockOutJob : getBlockOutJobs() ) {
      blockOutJobTriggers.add( blockOutJob.getJobTrigger() );
    }

    return blockOutJobTriggers;
  }

}
