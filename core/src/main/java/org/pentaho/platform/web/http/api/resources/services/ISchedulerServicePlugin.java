/*!
 * HITACHI VANTARA PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2023 Hitachi Vantara. All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Hitachi Vantara and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Hitachi Vantara and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Hitachi Vantara is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Hitachi Vantara,
 * explicitly covering such access.
 */

package org.pentaho.platform.web.http.api.resources.services;

import org.pentaho.platform.api.repository2.unified.webservices.RepositoryFileDto;
import org.pentaho.platform.api.scheduler2.IJob;
import org.pentaho.platform.api.scheduler2.IJobTrigger;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.JobState;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.web.http.api.proxies.BlockStatusProxy;
import org.pentaho.platform.web.http.api.resources.JobRequest;
import org.pentaho.platform.web.http.api.resources.JobScheduleRequest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Interface for pentaho platform scheduling service with focus
 * on servicing RESTful Web Services and real implementation of classes.
 * Ideally a more generic interface moved to pentaho-platform/scheduler that
 * only has interfaces for method arguments and return objects.
 */
/*
 * TODO look into refactoring SchedulerService to use *only* interfaces for arguments/return objects like:
 *  - pentaho-platform/scheduler/src/main/java/org/pentaho/platform/api/scheduler2/IJob.java
 * so this class can be moved to:
 *  - pentaho-platform/scheduler/src/main/java/org/pentaho/platform/api/scheduler2/ISchedulerService.java
 */
public interface ISchedulerServicePlugin {
  /*
   * TODO I don't think createJob actually throws IOException. look into changing IOException, SchedulerException,
   *  IllegalAccessException, ->  SchedulerException,
   */
  Job createJob( JobScheduleRequest jobScheduleRequest ) throws IOException, SchedulerException, IllegalAccessException;

  Job updateJob( JobScheduleRequest jobScheduleRequest ) throws IllegalAccessException, IOException, SchedulerException;

  Job triggerNow( String jobId ) throws SchedulerException;

  Job getContentCleanerJob() throws SchedulerException;

  List<IJob> getJobs() throws SchedulerException, IllegalAccessException;

  boolean isScheduleAllowed( String id );

  String doGetCanSchedule();

  String doGetCanExecuteSchedule();

  String getState() throws SchedulerException;

  String start() throws SchedulerException;

  String pause() throws SchedulerException;

  String shutdown() throws SchedulerException;

  JobState getJobState( JobRequest jobRequest ) throws SchedulerException;

  JobState pauseJob( String jobId ) throws SchedulerException;

  JobState resumeJob( String jobId ) throws SchedulerException;

  boolean removeJob( String jobId ) throws SchedulerException;

  IJob getJob( String jobId ) throws SchedulerException;

  IJob getJobInfo( String jobId ) throws SchedulerException;

  List<IJob> getBlockOutJobs() throws IllegalAccessException;

  JobScheduleRequest getJobInfo();

  boolean hasBlockouts();

  IJob addBlockout( JobScheduleRequest jobScheduleRequest )
    throws IOException, IllegalAccessException, SchedulerException;

  IJob updateBlockout( String jobId, JobScheduleRequest jobScheduleRequest )
    throws IllegalAccessException, SchedulerException, IOException;

  boolean willFire( IJobTrigger jobTrigger );

  boolean shouldFireNow();

  BlockStatusProxy getBlockStatus( JobScheduleRequest jobScheduleRequest ) throws SchedulerException;

  List<RepositoryFileDto> doGetGeneratedContentForSchedule( String lineageId ) throws FileNotFoundException;

  IScheduler getScheduler();
}
