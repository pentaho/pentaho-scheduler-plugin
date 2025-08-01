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


package org.pentaho.platform.scheduler2.ws;

import java.util.Map;
import jakarta.jws.WebService;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.pentaho.platform.api.scheduler2.ComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;

/**
 * A service interface used for exposing scheduling capabilities as JAXWS or other endpoints.
 * <p>
 * <strong>WARNING: If you change this interface or any of the objects related to this API, you are effectively changing
 * the WSDL which is generated by JAXWS from this interface. Changing the WSDL means you are changing the contract by
 * which clients talk to the scheduling service.</strong>
 * <p>
 * Currently {@link SimpleJobTrigger} and {@link ComplexJobTrigger} are natively JAXB marshallable. Each trigger class
 * contains JAXB annotations to allow us to use the real trigger object model and not to use a XmlJavaTypeAdapter class.
 * For the {@link Job} object, we are using the {@link JobAdapter} to send the {@link Job} over the wire. We realize
 * that two different approaches were taken here and perhaps a unified approach is best, but each object model was taken
 * on and the best approach to JAXB compatibility assessed based on their own merits. In any case, the JAXB compliance
 * unit tests in this project will provide assurance that each type transported over the WS remains functional.
 * 
 * @author aphillips
 */
@WebService
public interface ISchedulerService {
  /** @see IScheduler#createJob(String, Class, Map, org.pentaho.platform.api.scheduler2.JobTrigger) */
  public String createSimpleJob( String jobName,
      @XmlJavaTypeAdapter( JobParamsAdapter.class ) Map<String, ParamValue> jobParams, SimpleJobTrigger trigger )
    throws SchedulerException;

  /** @see IScheduler#createJob(String, Class, Map, org.pentaho.platform.api.scheduler2.JobTrigger) */
  public String createComplexJob( String jobName,
      @XmlJavaTypeAdapter( JobParamsAdapter.class ) Map<String, ParamValue> jobParams, ComplexJobTrigger trigger )
    throws SchedulerException;

  /** @see IScheduler#updateJob(String, Map, org.pentaho.platform.api.scheduler2.JobTrigger) */
  public void updateJobToUseSimpleTrigger( String jobId,
      @XmlJavaTypeAdapter( JobParamsAdapter.class ) Map<String, ParamValue> jobParams, SimpleJobTrigger trigger )
    throws SchedulerException;

  /** @see IScheduler#updateJob(String, String, Map, org.pentaho.platform.api.scheduler2.JobTrigger) */
  public String updateJobSimpleTriggerWithJobName( String jobName, String jobId,
      @XmlJavaTypeAdapter( JobParamsAdapter.class ) Map<String, ParamValue> jobParams, SimpleJobTrigger trigger )
          throws SchedulerException;

  /** @see IScheduler#updateJob(String, Map, org.pentaho.platform.api.scheduler2.JobTrigger) */
  public void updateJobToUseComplexTrigger( String jobId,
      @XmlJavaTypeAdapter( JobParamsAdapter.class ) Map<String, ParamValue> jobParams, ComplexJobTrigger trigger )
    throws SchedulerException;

  /** @see IScheduler#updateJob(String, String, Map, org.pentaho.platform.api.scheduler2.JobTrigger) */
  public String updateJobComplexTriggerWithJobName( String jobName, String jobId,
      @XmlJavaTypeAdapter( JobParamsAdapter.class ) Map<String, ParamValue> jobParams, ComplexJobTrigger trigger )
          throws SchedulerException;

  /** @see IScheduler#removeJob(String) */
  public void removeJob( String jobId ) throws SchedulerException;

  /** @see IScheduler#pauseJob(String) */
  public void pauseJob( String jobId ) throws SchedulerException;

  /** @see IScheduler#resumeJob(String) */
  public void resumeJob( String jobId ) throws SchedulerException;

  /** @see IScheduler#pause() */
  public void pause() throws SchedulerException;

  /** @see IScheduler#start() */
  public void start() throws SchedulerException;

  /** @see IScheduler#getJobs(org.pentaho.platform.api.scheduler2.IJobFilter) */
  @XmlJavaTypeAdapter( JobAdapter.class )
  public Job[] getJobs() throws SchedulerException;

  /**
   * Returns the scheduler status.
   * 
   * @return the ordinal value of the current scheduler state
   * @see IScheduler#getStatus()
   */
  public int getSchedulerStatus() throws SchedulerException;

  default public boolean canStopScheduler() {
    return true;
  }

  /**
   * Checks if the logged user has the permission to manage its schedules.
   *
   * @return true if it can, false otherwise.
   */
  default boolean canSchedule() {
    return true;
  }
}
