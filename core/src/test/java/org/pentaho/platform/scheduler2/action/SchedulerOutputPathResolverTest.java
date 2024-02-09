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
