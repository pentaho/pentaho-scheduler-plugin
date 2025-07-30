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

package org.pentaho.platform.api.scheduler2;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement( name = "jobs" )
@XmlAccessorType( XmlAccessType.FIELD )
public class JobWrapper {

  @XmlElement( name = "job" )
  private List<Job> jobs;

  public JobWrapper() {

  }

  public JobWrapper( List<Job> jobs ) {
    this.jobs = jobs;
  }

  public List<Job> getJobs() {
    return jobs;
  }

  public void setJobs( List<Job> jobs ) {
    this.jobs = jobs;
  }
}
