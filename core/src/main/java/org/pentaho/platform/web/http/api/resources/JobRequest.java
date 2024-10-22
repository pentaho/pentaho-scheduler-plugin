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


package org.pentaho.platform.web.http.api.resources;

import org.pentaho.platform.api.scheduler2.IJobRequest;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class JobRequest implements Serializable, IJobRequest {

  private static final long serialVersionUID = 6111578259094385262L;

  private String jobId;

  public JobRequest() {
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId( String jobId ) {
    this.jobId = jobId;
  }

}
