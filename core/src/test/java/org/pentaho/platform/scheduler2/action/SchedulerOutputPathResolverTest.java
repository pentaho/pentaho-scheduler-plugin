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

package org.pentaho.platform.scheduler2.action;

import junit.framework.TestCase;

public class SchedulerOutputPathResolverTest extends TestCase {

  public void testConcat() throws Exception {

    SchedulerOutputPathResolver testInstance = new SchedulerOutputPathResolver( );

    // TEST 1 - directory path does not have separator
    assertNotNull( testInstance );

    String argDirectoryNoEndSeparator = "/home/admin";
    String argFilename2 = "simple_report_name2.prpt";
    String expectedFullPathRepoHomeAdmdin = "/home/admin/simple_report_name2.prpt";

    String actualFullPath1 = testInstance.concat( argDirectoryNoEndSeparator, argFilename2 );
    assertEquals( expectedFullPathRepoHomeAdmdin, actualFullPath1 );

    // TEST 2 - directory path does not have separator
    String argDirectoryWithSeparator = "/public/";
    String argFilename3 = "simple_report_name3.prpt";
    String expectedFullPathRepoHome = "/public/simple_report_name3.prpt";

    String actualFullPath2 = testInstance.concat( argDirectoryWithSeparator, argFilename3 );
    assertEquals( expectedFullPathRepoHome, actualFullPath2 );
  }

}
