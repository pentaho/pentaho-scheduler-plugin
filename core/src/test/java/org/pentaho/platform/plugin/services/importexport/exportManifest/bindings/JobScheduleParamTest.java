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

package org.pentaho.platform.plugin.services.importexport.exportManifest.bindings;

import org.junit.Ignore;
import org.junit.Test;
import org.pentaho.platform.api.scheduler.JobScheduleParam;

import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSettersExcluding;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * Created by rfellows on 10/26/15.
 */
public class JobScheduleParamTest {

  @Test
  @Ignore
  public void testGettersAndSetters() throws Exception {
    String[] excludes = new String[] {
      "stringValue"
    };
    assertThat( JobScheduleParam.class, hasValidGettersAndSettersExcluding( excludes ) );
  }

  @Test
  public void testGetStringValue() throws Exception {
    JobScheduleParam jsp = new JobScheduleParam();
    assertNotNull( jsp.getStringValue() );
    assertEquals( 0, jsp.getStringValue().size() );
  }
}
