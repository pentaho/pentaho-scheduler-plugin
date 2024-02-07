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

public class SchedulerFilenameUtilsTest extends TestCase {


  public void testConcat_Repository() {

    // TEST null arguments
    assertNull( SchedulerFilenameUtils.concat( null,
        "/simple_report_name8.prpt" )
    );

    assertNull( SchedulerFilenameUtils.concat( "/home/randomUser",
        null )
    );

    assertNull( SchedulerFilenameUtils.concat( null,
      null )
    );

    // TEST - basePath and pathToAdd BOTH DON'T have separator
    assertEquals( "/home/randomUser/simple_report_name8.prpt",
      SchedulerFilenameUtils.concat( "/home/randomUser/",
        "simple_report_name8.prpt" )
    );

    assertEquals( "/home/randomUser/afolder",
      SchedulerFilenameUtils.concat( "/home/randomUser/",
        "afolder" )
    );

    // TEST - basePath has end separator
    assertEquals( "/home/randomUser/simple_report_name8.prpt",
      SchedulerFilenameUtils.concat( "/home/randomUser/",
        "simple_report_name8.prpt" )
    );

    assertEquals( "/home/randomUser/afolder",
      SchedulerFilenameUtils.concat( "/home/randomUser/",
        "afolder" )
    );

    // TEST - pathToAdd has end separator
    assertEquals( "/home/randomUser/simple_report_name8.prpt",
      SchedulerFilenameUtils.concat( "/home/randomUser",
        "/simple_report_name8.prpt" )
    );

    assertEquals( "/home/randomUser/afolder",
      SchedulerFilenameUtils.concat( "/home/randomUser",
        "/afolder" )
    );

    // TEST - basePath and pathToAdd BOTH have separator
    assertEquals( "/home/randomUser/simple_report_name8.prpt",
      SchedulerFilenameUtils.concat( "/home/randomUser",
        "/simple_report_name8.prpt" )
    );

    assertEquals( "/home/randomUser/afolder",
      SchedulerFilenameUtils.concat( "/home/randomUser",
        "/afolder" )
    );
  }

  public void testConcat_Scheme() {

    // TEST null arguments
    assertNull( SchedulerFilenameUtils.concat( null,
        "simple_file_name.pdf" )
    );

    assertNull( SchedulerFilenameUtils.concat( "ascheme://somBucket/someFolder/simple_file_name.pdf",
      null )
    );

    assertNull( SchedulerFilenameUtils.concat( null,
      null )
    );

    // TEST - basePath and pathToAdd BOTH DON'T have separator
    assertEquals( "ascheme://somBucket/someFolder/simple_file_name.pdf",
      SchedulerFilenameUtils.concat( "ascheme://somBucket/someFolder",
        "simple_file_name.pdf" )
    );

    assertEquals( "ascheme://somBucket/someFolder/mysteryFolder",
      SchedulerFilenameUtils.concat( "ascheme://somBucket/someFolder",
        "mysteryFolder" )
    );

    // TEST - basePath has end separator
    assertEquals( "ascheme://somBucket/someFolder/simple_file_name.pdf",
      SchedulerFilenameUtils.concat( "ascheme://somBucket/someFolder/",
        "simple_file_name.pdf" )
    );

    assertEquals( "ascheme://somBucket/someFolder/mysteryFolder",
      SchedulerFilenameUtils.concat( "ascheme://somBucket/someFolder/",
        "mysteryFolder" )
    );

    // TEST - pathToAdd has end separator
    assertEquals( "ascheme://somBucket/someFolder/simple_file_name.pdf",
      SchedulerFilenameUtils.concat( "ascheme://somBucket/someFolder",
        "/simple_file_name.pdf" )
    );

    assertEquals( "ascheme://somBucket/someFolder/mysteryFolder",
      SchedulerFilenameUtils.concat( "ascheme://somBucket/someFolder/",
        "/mysteryFolder" )
    );

    // TEST - basePath and pathToAdd BOTH have separator
    assertEquals( "ascheme://somBucket/someFolder/simple_file_name.pdf",
      SchedulerFilenameUtils.concat( "ascheme://somBucket/someFolder",
        "simple_file_name.pdf" )
    );

    assertEquals( "ascheme://somBucket/someFolder/mysteryFolder",
      SchedulerFilenameUtils.concat( "ascheme://somBucket/someFolder/",
        "/mysteryFolder" )
    );
  }

  public void testGetNoEndSeparator() {
    assertNull( SchedulerFilenameUtils.getNoEndSeparator( null ) );
    assertEquals( "", SchedulerFilenameUtils.getNoEndSeparator( "" ) );
    assertEquals( "", SchedulerFilenameUtils.getNoEndSeparator( "/" ) );
    assertEquals( "/foo", SchedulerFilenameUtils.getNoEndSeparator( "/foo" ) );
    assertEquals( "/bar", SchedulerFilenameUtils.getNoEndSeparator( "/bar/" ) );
    assertEquals( "/a/b/c", SchedulerFilenameUtils.getNoEndSeparator( "/a/b/c/" ) );
    assertEquals( "/a/b/c/d", SchedulerFilenameUtils.getNoEndSeparator( "/a/b/c/d" ) );
    assertEquals( "/a/bee/c/duck", SchedulerFilenameUtils.getNoEndSeparator( "/a/bee/c/duck" ) );
  }
}
