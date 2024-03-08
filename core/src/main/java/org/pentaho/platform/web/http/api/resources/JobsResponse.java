/*!
 *
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 *
 * Copyright (c) 2024 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.web.http.api.resources;

import javax.xml.bind.annotation.XmlRootElement;
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
