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

import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class JobsResponse implements Serializable {
  private static final long serialVersionUID = -5254494051376468510L;
  JobsResponseEntries changes;

  public JobsResponseEntries getChanges() {
    return changes;
  }

  public void setChanges( JobsResponseEntries changes ) {
    this.changes = changes;
  }

  public void addChanges( String jobId, String state ) {
    if( changes == null ) {
      changes = new JobsResponseEntries();
    }
    validateJobId( jobId );
    validateState( state );

    changes.put( jobId, state );
  }

  private void validateJobId( String jobId ) {
    if ( jobId == null || jobId.isEmpty() ) {
      throw new IllegalArgumentException( "Invalid job id!" );
    }
  }

  private void validateState( String state ) {
    if ( state == null || state.isEmpty() ) {
      throw new IllegalArgumentException( "Invalid state!" );
    }
  }
  @XmlRootElement
  public static class JobsResponseEntries {
    private List<JobsResponseEntry> entry = new ArrayList<>();

    public JobsResponseEntries() {
    }

    public JobsResponseEntries( List<JobsResponseEntry> entry ) {
      this.entry = entry;
    }

    public List<JobsResponseEntry> getEntry() {
      return entry;
    }

    public void setEntry( List<JobsResponseEntry> entry ) {
      this.entry = entry;
    }

    public void put( String jobId, String state ) {
      entry.add( new JobsResponseEntry( jobId, state ) );
    }
  }
}
