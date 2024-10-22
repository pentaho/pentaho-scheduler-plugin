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
import java.util.HashMap;
import java.util.Map;

@XmlRootElement
public class JobsResponse implements Serializable {
  private static final long serialVersionUID = -5254494051376468510L;
  private Map<String, String> changes = new HashMap<>();

  public Map<String, String> getChanges() {
    return changes;
  }

  public void setChanges( Map<String, String> changes ) {
    this.changes = changes;
  }

  public void addChanges( String jobId, String state ) {
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
}
