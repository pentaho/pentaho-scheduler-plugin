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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by rfellows on 11/10/15.
 */
public class JobRequestTest {

  @Test
  public void testConstructor() throws Exception {
    JobRequest jr = new JobRequest();
    jr.setJobId( "jobId" );
    assertEquals( "jobId", jr.getJobId() );
  }
}
