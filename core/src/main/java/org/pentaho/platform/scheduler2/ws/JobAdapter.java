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

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.JobState;
import org.pentaho.platform.api.scheduler2.JobTrigger;
import org.pentaho.platform.scheduler2.ws.JaxBSafeMap.JaxBSafeEntry;

/**
 * Handles the sending of {@link Job} objects over JAXWS webservices by utilizing {@link JaxbSafeJob} as a transport
 * type.
 * 
 * @author aphillips
 */
public class JobAdapter extends XmlAdapter<JobAdapter.JaxbSafeJob, Job> {

  private static final Log logger = LogFactory.getLog( JobAdapter.class );

  public JaxbSafeJob marshal( Job job ) throws Exception {
    if ( job == null ) {
      return null;
    }

    JaxbSafeJob jaxbSafeJob = new JaxbSafeJob();
    try {
      if ( job.getJobTrigger() == null ) {
        throw new IllegalArgumentException();

      }
      jaxbSafeJob.jobTrigger = (JobTrigger) job.getJobTrigger();
      jaxbSafeJob.jobParams = new JaxBSafeMap( toParamValueMap( job.getJobParams() ) );
      jaxbSafeJob.lastRun = job.getLastRun();
      jaxbSafeJob.nextRun = job.getNextRun();
      jaxbSafeJob.schedulableClass = job.getSchedulableClass();
      jaxbSafeJob.jobId = job.getJobId();
      jaxbSafeJob.userName = job.getUserName();
      jaxbSafeJob.jobName = job.getJobName();
      jaxbSafeJob.state = job.getState();
    } catch ( Throwable t ) {
      // no message bundle since this is a development error case
      logger.error( "Error marshalling job", t ); //$NON-NLS-1$
      return null;
    }
    return jaxbSafeJob;
  }

  @SuppressWarnings( "unchecked" )
  private Map<String, ParamValue> toParamValueMap( Map<String, Object> unsafeMap ) {
    Map<String, ParamValue> paramValueMap = new HashMap<String, ParamValue>();
    for ( Map.Entry<String, Object> entry : unsafeMap.entrySet() ) {
      if ( entry.getValue() instanceof Map ) {
        // convert the inner map
        MapParamValue map = new MapParamValue();
        Map innerMap = (Map) entry.getValue();
        Set<Map.Entry> entrySet = innerMap.entrySet();
        for ( Map.Entry innerEntry : entrySet ) {
          map.put( innerEntry.getKey().toString(), ( innerEntry.getValue() == null ) ? null : innerEntry.getValue()
              .toString() );
        }
        // add the converted map the the top-level map
        paramValueMap.put( entry.getKey(), map );
      } else if ( entry.getValue() instanceof List ) {
        ListParamValue list = new ListParamValue();
        List innerList = (List) entry.getValue();
        list.addAll( innerList );
        paramValueMap.put( entry.getKey(), list );
      } else {
        paramValueMap.put( entry.getKey(), new StringParamValue( ( entry.getValue() == null ) ? null : entry.getValue()
            .toString() ) );
      }
    }
    return paramValueMap;
  }

  private Map<String, Object> toProperMap( JaxBSafeMap safeMap ) {
    Map<String, Object> unsafeMap = new HashMap<>();
    for ( JaxBSafeEntry safeEntry : safeMap.entry ) {
      if ( safeEntry.getStringValue() != null ) {
        unsafeMap.put( safeEntry.key, ( safeEntry.getStringValue() == null ) ? null : safeEntry.getStringValue()
            .toString() );
        continue;
      }
      if ( safeEntry.getListValue() != null ) {
        unsafeMap.put( safeEntry.key, safeEntry.getListValue() );
        continue;
      }
      if ( safeEntry.getMapValue() != null ) {
        unsafeMap.put( safeEntry.key, safeEntry.getMapValue() );
        continue;
      }
    }
    return unsafeMap;
  }

  public Job unmarshal( JaxbSafeJob jaxbSafeJob ) throws Exception {
    if ( jaxbSafeJob == null ) {
      return null;
    }

    Job job = new Job();
    try {
      job.setJobTrigger( jaxbSafeJob.jobTrigger );
      job.setJobParams( toProperMap( jaxbSafeJob.jobParams ) );
      job.setLastRun( jaxbSafeJob.lastRun );
      job.setNextRun( jaxbSafeJob.nextRun );
      job.setSchedulableClass( jaxbSafeJob.schedulableClass );
      job.setJobId( jaxbSafeJob.jobId );
      job.setUserName( jaxbSafeJob.userName );
      job.setJobName( jaxbSafeJob.jobName );
      job.setState( jaxbSafeJob.state );
    } catch ( Throwable t ) {
      // no message bundle since this is a development error case
      logger.error( "Error unmarshalling job", t ); //$NON-NLS-1$
      return null;
    }
    return job;

  }

  @XmlRootElement
  public static class JaxbSafeJob {
    public JobTrigger jobTrigger;

    public JaxBSafeMap jobParams;

    public Date lastRun;

    public Date nextRun;

    public String schedulableClass;

    public String jobId;

    public String userName;

    public String jobName;

    public JobState state;
  }

}
