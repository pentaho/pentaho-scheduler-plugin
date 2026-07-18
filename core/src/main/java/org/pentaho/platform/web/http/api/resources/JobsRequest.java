/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/



package org.pentaho.platform.web.http.api.resources;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class JobsRequest implements Serializable {
  private static final long serialVersionUID = -2183312426347688394L;
  private List<String> jobIds = new ArrayList<>();

  public List<String> getJobIds() {
    return jobIds;
  }

  public void setJobIds( List<String> jobIds ) {
    this.jobIds = jobIds;
  }
}
