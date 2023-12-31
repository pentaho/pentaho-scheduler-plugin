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
 * Copyright (c) 2002-2018 Hitachi Vantara. All rights reserved.
 *
 */

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
