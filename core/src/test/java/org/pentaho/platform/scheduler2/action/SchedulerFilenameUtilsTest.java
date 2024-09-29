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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SchedulerFilenameUtilsTest {
  
  @Test
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

    // TEST pathToAdd is many folders + filename
    assertEquals( "/home/randomUser/folderA/folderB/folderC/folderD/folderE/simple_file_name.pdf",
      SchedulerFilenameUtils.concat( "/home/randomUser/folderA/folderB/",
        "/folderC/folderD/folderE/simple_file_name.pdf" )
    );

    assertEquals( "/home/randomUser/folderA/folderB/folderC/folderD/folderE/folderF",
      SchedulerFilenameUtils.concat( "/home/randomUser/folderA/folderB/",
        "/folderC/folderD/folderE/folderF" )
    );
  }

  @Test
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

    // TEST pathToAdd is many folders + filename
    assertEquals( "ascheme://somBucket/folderA/folderB/folderC/folderD/folderE/simple_file_name.pdf",
      SchedulerFilenameUtils.concat( "ascheme://somBucket/folderA/folderB/",
        "/folderC/folderD/folderE/simple_file_name.pdf" )
    );

    assertEquals( "ascheme://somBucket/folderA/folderB/folderC/folderD/folderE/folderF",
      SchedulerFilenameUtils.concat( "ascheme://somBucket/folderA/folderB/",
        "/folderC/folderD/folderE/folderF" )
    );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testConcat_exceptionThrown_windowsSeparator_basePath() {
    SchedulerFilenameUtils.concat( "\\some\\windows\\folder\\", "aFile.txt" );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testConcat_exceptionThrown_windowsSeparator_pathToAdd() {
    SchedulerFilenameUtils.concat( "/some/folder/", "\\windows\\relativePath\\aFile.txt" );
  }

  @Test
  public void testGetNoEndSeparator() {
    assertNull( SchedulerFilenameUtils.getNoEndSeparator( (StringBuilder) null ) );
    assertEquals( "", SchedulerFilenameUtils.getNoEndSeparator( toSb( "" ) ).toString() );
    assertEquals( "", SchedulerFilenameUtils.getNoEndSeparator( toSb( "/" ) ).toString() );
    assertEquals( "/foo", SchedulerFilenameUtils.getNoEndSeparator( toSb(  "/foo" ) ).toString() );
    assertEquals( "/bar", SchedulerFilenameUtils.getNoEndSeparator(  toSb(  "/bar/" ) ).toString() );
    assertEquals( "/a/b/c", SchedulerFilenameUtils.getNoEndSeparator(  toSb( "/a/b/c/" ) ).toString() );
    assertEquals( "/a/b/c/d", SchedulerFilenameUtils.getNoEndSeparator( toSb( "/a/b/c/d" ) ).toString() );
    assertEquals( "/a/bee/c/duck", SchedulerFilenameUtils.getNoEndSeparator( toSb( "/a/bee/c/duck" ) ).toString() );
  }

  @Test
  public void testIndexOfLastSeparator() {
    assertEquals( -1, SchedulerFilenameUtils.indexOfLastSeparator( null ) );
    assertEquals( -1, SchedulerFilenameUtils.indexOfLastSeparator( toSb( "noseparator.inthispath" ) ) );
    assertEquals( -1, SchedulerFilenameUtils.indexOfLastSeparator( toSb( "\\windows\\seperator\\not\\supported" ) ) );
    assertEquals( 4, SchedulerFilenameUtils.indexOfLastSeparator( toSb( "/x/y/z" ) ) );
    assertEquals( 6, SchedulerFilenameUtils.indexOfLastSeparator( toSb( "/x/y/z/" ) ) );
    assertEquals( 0, SchedulerFilenameUtils.indexOfLastSeparator( toSb( "/nop" ) ) );
    assertEquals( 4, SchedulerFilenameUtils.indexOfLastSeparator( toSb( "/pqr/stuv" ) ) );
    assertEquals( 3, SchedulerFilenameUtils.indexOfLastSeparator( toSb( "a/b/c" ) ) );
    assertEquals( 5, SchedulerFilenameUtils.indexOfLastSeparator( toSb( "a/b/c/" ) ) );
    assertEquals( 1, SchedulerFilenameUtils.indexOfLastSeparator( toSb( "d/ef" ) ) );
    assertEquals( 3, SchedulerFilenameUtils.indexOfLastSeparator( toSb( "ghi/jklm" ) ) );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testCheckPath_exceptionThrown_windowsSeparator_begginng() {
    SchedulerFilenameUtils.checkPath( "\\windows\\relativePath\\aFile.txt" );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testCheckPath_exceptionThrown_windowsSeparator_middle() {
    SchedulerFilenameUtils.checkPath( "windows\\relativePath\\aFile.txt" );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testCheckPath_exceptionThrown_windowsSeparator_end() {
    SchedulerFilenameUtils.checkPath( "relativePath\\" );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testCheckPath_exceptionThrown_windowsSeparator_mix() {
    SchedulerFilenameUtils.checkPath( "windows/relativePath\\aFile.txt" );
  }

  public StringBuilder toSb( String str ) {
    return new StringBuilder( str );
  }
}
