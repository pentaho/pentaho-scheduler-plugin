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

package org.pentaho.platform.api.genericfile;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the {@link GetTreeOptions} class.
 */
class GetTreeOptionsTest {
  /**
   * Tests the {@link GetTreeOptions#GetTreeOptions(GetTreeOptions)} constructor.
   */
  @Nested
  class CopyConstructorTests {
    @Test
    void testCopiesAllProperties() throws InvalidPathException {
      GetTreeOptions options1 = new GetTreeOptions();
      options1.setBasePath( "/A" );
      options1.setExpandedPath( "/B" );
      options1.setMaxDepth( 3 );
      options1.setExpandedMaxDepth( 5 );
      options1.setFilter( GetTreeOptions.TreeFilter.FILES );
      options1.setBypassCache( true );
      options1.setIncludeHidden( true );

      // ---

      GetTreeOptions options2 = new GetTreeOptions( options1 );

      assertEquals( options1.getBasePath(), options2.getBasePath() );
      assertEquals( options1.getExpandedPath(), options2.getExpandedPath() );
      assertEquals( options1.getMaxDepth(), options2.getMaxDepth() );
      assertEquals( options1.getExpandedMaxDepth(), options2.getExpandedMaxDepth() );
      assertEquals( options1.getFilter(), options2.getFilter() );
      assertEquals( options1.isBypassCache(), options2.isBypassCache() );
      assertEquals( options1.isIncludeHidden(), options2.isIncludeHidden() );
    }

    @Test
    void testAcceptsPathSetAsGenericPath() throws InvalidPathException {
      GetTreeOptions options = new GetTreeOptions();
      GenericFilePath path = GenericFilePath.parseRequired( "/" );

      options.setBasePath( path );

      assertSame( path, options.getBasePath() );
    }

    @Test
    void testAcceptsNullPathAsGenericPath() throws InvalidPathException {
      GetTreeOptions options = new GetTreeOptions();

      GenericFilePath initialPath = GenericFilePath.parseRequired( "/" );
      options.setBasePath( initialPath );

      options.setBasePath( (GenericFilePath) null );

      assertNull( options.getBasePath() );
    }

    @Test
    void testAcceptsPathSetAsString() throws InvalidPathException {
      GetTreeOptions options = new GetTreeOptions();

      options.setBasePath( "/" );

      GenericFilePath path = options.getBasePath();
      assertNotNull( path );
      assertEquals( "/", path.toString() );
    }

    @Test
    void testAcceptsNullPathSetAsString() throws InvalidPathException {
      GetTreeOptions options = new GetTreeOptions();

      GenericFilePath initialPath = GenericFilePath.parseRequired( "/" );
      options.setBasePath( initialPath );

      options.setBasePath( (String) null );

      assertNull( options.getBasePath() );
    }

    @Test
    void testThrowsInvalidPathExceptionIfInvalidPathSetAsString() {
      GetTreeOptions options = new GetTreeOptions();

      assertThrows( InvalidPathException.class, () -> options.setBasePath( "foo" ) );
    }
  }

  /**
   * Tests the {@link GetTreeOptions#getBasePath()}, {@link GetTreeOptions#setBasePath(String)} and
   * {@link GetTreeOptions#setBasePath(GenericFilePath)} methods.
   */
  @Nested
  class BasePathTests {
    @Test
    void testDefaultsToNull() {
      GetTreeOptions options = new GetTreeOptions();
      assertNull( options.getBasePath() );
    }

    @Test
    void testAcceptsPathSetAsGenericPath() throws InvalidPathException {
      GetTreeOptions options = new GetTreeOptions();
      GenericFilePath path = GenericFilePath.parseRequired( "/" );

      options.setBasePath( path );

      assertSame( path, options.getBasePath() );
    }

    @Test
    void testAcceptsNullPathAsGenericPath() throws InvalidPathException {
      GetTreeOptions options = new GetTreeOptions();

      GenericFilePath initialPath = GenericFilePath.parseRequired( "/" );
      options.setBasePath( initialPath );

      options.setBasePath( (GenericFilePath) null );

      assertNull( options.getBasePath() );
    }

    @Test
    void testAcceptsPathSetAsString() throws InvalidPathException {
      GetTreeOptions options = new GetTreeOptions();

      options.setBasePath( "/" );

      GenericFilePath path = options.getBasePath();
      assertNotNull( path );
      assertEquals( "/", path.toString() );
    }

    @Test
    void testAcceptsNullPathSetAsString() throws InvalidPathException {
      GetTreeOptions options = new GetTreeOptions();

      GenericFilePath initialPath = GenericFilePath.parseRequired( "/" );
      options.setBasePath( initialPath );

      options.setBasePath( (String) null );

      assertNull( options.getBasePath() );
    }

    @Test
    void testThrowsInvalidPathExceptionIfInvalidPathSetAsString() {
      GetTreeOptions options = new GetTreeOptions();

      assertThrows( InvalidPathException.class, () -> options.setBasePath( "foo" ) );
    }
  }

  /**
   * Tests the {@link GetTreeOptions#getExpandedPath()}, {@link GetTreeOptions#setExpandedPath(String)} and
   * {@link GetTreeOptions#setExpandedPath(GenericFilePath)} methods.
   */
  @Nested
  class ExpandedPathTests {
    @Test
    void testDefaultsToNull() {
      GetTreeOptions options = new GetTreeOptions();
      assertNull( options.getExpandedPath() );
    }

    @Test
    void testAcceptsPathSetAsGenericPath() throws InvalidPathException {
      GetTreeOptions options = new GetTreeOptions();
      GenericFilePath path = GenericFilePath.parseRequired( "/" );

      options.setExpandedPath( path );

      assertSame( path, options.getExpandedPath() );
    }

    @Test
    void testAcceptsNullPathAsGenericPath() throws InvalidPathException {
      GetTreeOptions options = new GetTreeOptions();

      GenericFilePath initialPath = GenericFilePath.parseRequired( "/" );
      options.setExpandedPath( initialPath );

      options.setExpandedPath( (GenericFilePath) null );

      assertNull( options.getExpandedPath() );
    }

    @Test
    void testAcceptsPathSetAsString() throws InvalidPathException {
      GetTreeOptions options = new GetTreeOptions();

      options.setExpandedPath( "/" );

      GenericFilePath path = options.getExpandedPath();
      assertNotNull( path );
      assertEquals( "/", path.toString() );
    }

    @Test
    void testAcceptsNullPathSetAsString() throws InvalidPathException {
      GetTreeOptions options = new GetTreeOptions();

      GenericFilePath initialPath = GenericFilePath.parseRequired( "/" );
      options.setExpandedPath( initialPath );

      options.setExpandedPath( (String) null );

      assertNull( options.getExpandedPath() );
    }

    @Test
    void testThrowsInvalidPathExceptionIfInvalidPathSetAsString() {
      GetTreeOptions options = new GetTreeOptions();

      assertThrows( InvalidPathException.class, () -> options.setExpandedPath( "foo" ) );
    }
  }

  /**
   * Tests the {@link GetTreeOptions#getMaxDepth()} and {@link GetTreeOptions#setMaxDepth(Integer)} methods.
   */
  @Nested
  class MaxDepthTests {
    @Test
    void testDefaultsToNull() {
      GetTreeOptions options = new GetTreeOptions();
      assertNull( options.getMaxDepth() );
    }

    @Test
    void testAcceptsBeingSetToZero() {
      GetTreeOptions options = new GetTreeOptions();

      options.setMaxDepth( 0 );

      assertEquals( 0, options.getMaxDepth() );
    }

    @Test
    void testAcceptsBeingSetToOne() {
      GetTreeOptions options = new GetTreeOptions();

      options.setMaxDepth( 1 );

      assertEquals( 1, options.getMaxDepth() );
    }

    @Test
    void testAcceptsBeingSetToTwo() {
      GetTreeOptions options = new GetTreeOptions();

      options.setMaxDepth( 2 );

      assertEquals( 2, options.getMaxDepth() );
    }

    @Test
    void testAcceptsBeingResetToNullInteger() {
      GetTreeOptions options = new GetTreeOptions();

      options.setMaxDepth( 1 );

      options.setMaxDepth( null );

      assertNull( options.getMaxDepth() );
    }

    @Test
    void testNormalizesNegativeToNull() {
      GetTreeOptions options = new GetTreeOptions();

      options.setMaxDepth( -1 );

      assertNull( options.getMaxDepth() );
    }
  }

  /**
   * Tests the {@link GetTreeOptions#getExpandedMaxDepth()} and {@link GetTreeOptions#setExpandedMaxDepth(Integer)} methods.
   */
  @Nested
  class ExpandedMaxDepthTests {
    @Test
    void testDefaultsToNull() {
      GetTreeOptions options = new GetTreeOptions();
      assertNull( options.getExpandedMaxDepth() );
    }

    @Test
    void testAcceptsBeingSetToZero() {
      GetTreeOptions options = new GetTreeOptions();

      options.setExpandedMaxDepth( 0 );

      assertEquals( 0, options.getExpandedMaxDepth() );
    }

    @Test
    void testAcceptsBeingSetToOne() {
      GetTreeOptions options = new GetTreeOptions();

      options.setExpandedMaxDepth( 1 );

      assertEquals( 1, options.getExpandedMaxDepth() );
    }

    @Test
    void testAcceptsBeingSetToTwo() {
      GetTreeOptions options = new GetTreeOptions();

      options.setExpandedMaxDepth( 2 );

      assertEquals( 2, options.getExpandedMaxDepth() );
    }

    @Test
    void testAcceptsBeingResetToNullInteger() {
      GetTreeOptions options = new GetTreeOptions();

      options.setExpandedMaxDepth( 1 );

      options.setExpandedMaxDepth( null );

      assertNull( options.getExpandedMaxDepth() );
    }

    @Test
    void testNormalizesNegativeToNull() {
      GetTreeOptions options = new GetTreeOptions();

      options.setExpandedMaxDepth( -1 );

      assertNull( options.getExpandedMaxDepth() );
    }
  }

  /**
   * Tests the {@link GetTreeOptions#isBypassCache()} and {@link GetTreeOptions#setBypassCache(boolean)} methods.
   */
  @Nested
  class BypassCacheTests {
    @Test
    void testDefaultsToFalse() {
      GetTreeOptions options = new GetTreeOptions();
      assertFalse( options.isBypassCache() );
    }

    @Test
    void testAcceptsBeingSetToTrue() {
      GetTreeOptions options = new GetTreeOptions();

      options.setBypassCache( true );

      assertTrue( options.isBypassCache() );
    }

    @Test
    void testAcceptsBeingSetToFalse() {
      GetTreeOptions options = new GetTreeOptions();

      options.setBypassCache( false );

      assertFalse( options.isBypassCache() );
    }
  }

  /**
   * Tests the {@link GetTreeOptions#isIncludeHidden()} and {@link GetTreeOptions#setIncludeHidden(boolean)} methods.
   */
  @Nested
  class IncludeHiddenTests {
    @Test
    void testDefaultsToFalse() {
      GetTreeOptions options = new GetTreeOptions();
      assertFalse( options.isIncludeHidden() );
    }

    @Test
    void testAcceptsBeingSetToTrue() {
      GetTreeOptions options = new GetTreeOptions();

      options.setIncludeHidden( true );

      assertTrue( options.isIncludeHidden() );
    }

    @Test
    void testAcceptsBeingSetToFalse() {
      GetTreeOptions options = new GetTreeOptions();

      options.setIncludeHidden( false );

      assertFalse( options.isIncludeHidden() );
    }
  }

  /**
   * Tests the {@link GetTreeOptions#equals(Object)} and {@link GetTreeOptions#hashCode()} methods.
   */
  @Nested
  class EqualsTests {
    @Test
    void testEqualsItself() {
      GetTreeOptions options = new GetTreeOptions();

      // Sonar issue: Want to directly test the #equals(.) method.
      assertTrue( options.equals( options ) );
    }

    @Test
    void testDoesNotEqualNull() {
      GetTreeOptions options = new GetTreeOptions();
      assertFalse( options.equals( null ) );
    }

    @Test
    void testEqualsAnotherBothWithAllNullProperties() {
      GetTreeOptions options1 = new GetTreeOptions();
      GetTreeOptions options2 = new GetTreeOptions();

      assertTrue( options1.equals( options2 ) );
      assertEquals( options1.hashCode(), options2.hashCode() );
    }

    @Test
    void testEqualsAnotherWithAllEqualProperties() throws InvalidPathException {
      GetTreeOptions options1 = new GetTreeOptions();
      options1.setBasePath( "/" );
      options1.setMaxDepth( 1 );
      options1.setExpandedPath( "scheme://my/folder" );
      options1.setExpandedMaxDepth( 2 );
      options1.setFilter( GetTreeOptions.TreeFilter.FILES );
      options1.setIncludeHidden( true );
      options1.setBypassCache( true );

      GetTreeOptions options2 = new GetTreeOptions();
      options2.setBasePath( "/" );
      options2.setMaxDepth( 1 );
      options2.setExpandedPath( "scheme://my/folder" );
      options2.setExpandedMaxDepth( 2 );
      options2.setFilter( GetTreeOptions.TreeFilter.FILES );
      options2.setIncludeHidden( true );
      options2.setBypassCache( true );

      assertTrue( options1.equals( options2 ) );
      assertEquals( options1.hashCode(), options2.hashCode() );
    }

    @Test
    void testDoesNotEqualAnotherWithDifferentBasePath() throws InvalidPathException {
      GetTreeOptions options1 = new GetTreeOptions();
      options1.setBasePath( "/" );
      options1.setMaxDepth( 1 );
      options1.setExpandedPath( "scheme://my/folder" );

      GetTreeOptions options2 = new GetTreeOptions();
      options2.setBasePath( "/a" );
      options2.setMaxDepth( 1 );
      options2.setExpandedPath( "scheme://my/folder" );

      assertFalse( options1.equals( options2 ) );
      assertNotEquals( options1.hashCode(), options2.hashCode() );
    }

    @Test
    void testDoesNotEqualAnotherWithDifferentMaxDepth() throws InvalidPathException {
      GetTreeOptions options1 = new GetTreeOptions();
      options1.setBasePath( "/" );
      options1.setMaxDepth( 1 );
      options1.setExpandedPath( "scheme://my/folder" );

      GetTreeOptions options2 = new GetTreeOptions();
      options2.setBasePath( "/" );
      options2.setMaxDepth( 2 );
      options2.setExpandedPath( "scheme://my/folder" );

      assertFalse( options1.equals( options2 ) );
      assertNotEquals( options1.hashCode(), options2.hashCode() );
    }

    @Test
    void testDoesNotEqualAnotherWithDifferentExpandedPath() throws InvalidPathException {
      GetTreeOptions options1 = new GetTreeOptions();
      options1.setBasePath( "/" );
      options1.setMaxDepth( 1 );
      options1.setExpandedPath( "scheme://my/folder" );

      GetTreeOptions options2 = new GetTreeOptions();
      options2.setBasePath( "/" );
      options2.setMaxDepth( 1 );
      options2.setExpandedPath( "scheme://my/folder/a" );

      assertFalse( options1.equals( options2 ) );
      assertNotEquals( options1.hashCode(), options2.hashCode() );
    }

    @Test
    void testDoesNotEqualObjectsOfOtherClasses() {
      GetTreeOptions options = new GetTreeOptions();
      Object other = new Object();

      assertFalse( options.equals( other ) );
    }
  }
}
