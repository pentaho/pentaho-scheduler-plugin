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


package org.pentaho.platform.scheduler2.quartz;

import java.text.MessageFormat;

import org.apache.commons.lang.StringUtils;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.scheduler2.messsages.Messages;
import org.pentaho.platform.util.UUIDUtil;

/**
 * This class is the key by which we identify a quartz job. It provides the means to create a new key or derive a key
 * from an existing job. This should be the only place in the Quartz scheduler that knows exactly how a jobId is
 * constructed.
 * 
 * @author aphillips
 */
public class QuartzJobKey {
  private String userName;
  private String jobName;
  // BACKLOG-22942, changing timInMillis from long to randomUUID
  private String randomUuid;

  /**
   * Use this constructor when you wish to create a new unique job key.
   * 
   * @param jobName
   *          the user-provided job name
   * @param username
   *          the user who is executing this job
   * @throws SchedulerException
   */
  public QuartzJobKey( String jobName, String username ) throws SchedulerException {
    if ( StringUtils.isEmpty( jobName ) ) {
      throw new SchedulerException( Messages.getInstance().getErrorString( "QuartzJobKey.ERROR_0000" ) ); //$NON-NLS-1$
    }
    if ( StringUtils.isEmpty( username ) ) {
      throw new SchedulerException( Messages.getInstance().getErrorString( "QuartzJobKey.ERROR_0001" ) ); //$NON-NLS-1$
    }
    userName = username;
    this.jobName = jobName;
    randomUuid = UUIDUtil.getUUIDAsString();
  }

  private QuartzJobKey() {
  }

  /**
   * Parses an existing jobId into a {@link QuartzJobKey}
   * 
   * @param jobId
   *          an existing jobId
   * @return a quartz job key
   * @throws SchedulerException
   */
  public static QuartzJobKey parse( String jobId ) throws SchedulerException {
    String delimiter = jobId.contains( "\t" ) || jobId.isEmpty() ? "\t" : ":";
    String[] elements = jobId.split( delimiter ); //$NON-NLS-1$
    if ( elements == null || elements.length < 3 ) {
      throw new SchedulerException( MessageFormat.format( Messages.getInstance().getErrorString(
          "QuartzJobKey.ERROR_0002" ), jobId ) ); //$NON-NLS-1$
    }
    QuartzJobKey key = new QuartzJobKey();
    key.userName = elements[0];
    key.jobName = elements[1];
    key.randomUuid = elements[2];

    return key;
  }

  public String getUserName() {
    return userName;
  }

  public String getJobName() {
    return jobName;
  }

  @Override
  public String toString() {
    return userName + "\t" + jobName + "\t" + randomUuid; //$NON-NLS-1$ //$NON-NLS-2$
  }
}
