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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the {@link GenericFilePath} class.
 */
class GenericFilePathTest {
  void assertParent( @NonNull String[] expectedSegments, @Nullable GenericFilePath path ) {
    assert expectedSegments.length >= 1;

    assertNotNull( path );

    GenericFilePath parentPath = path.getParent();
    if ( expectedSegments.length == 1 ) {
      assertNull( parentPath );
    } else {
      assertNotNull( parentPath );

      String[] expectedParentSegments = Arrays.copyOfRange( expectedSegments, 0, expectedSegments.length - 1 );
      assertArrayEquals( expectedParentSegments, parentPath.getSegments().toArray() );
    }
  }

  void assertSegments( @NonNull String[] expectedSegments, @Nullable GenericFilePath path ) {
    assert expectedSegments.length >= 1;

    assertNotNull( path );

    // getSegments()
    assertArrayEquals( expectedSegments, path.getSegments().toArray() );

    // getFirstSegment()
    assertEquals( expectedSegments[ 0 ], path.getFirstSegment() );

    // getRestSegment()
    String[] expectedRestSegments = Arrays.copyOfRange( expectedSegments, 1, expectedSegments.length );
    assertArrayEquals( expectedRestSegments, path.getRestSegments().toArray() );

    assertParent( expectedSegments, path );
  }

  void assertPathStringEquals( @NonNull String pathString, @Nullable GenericFilePath path ) {
    assertNotNull( path );
    assertEquals( pathString, path.toString() );
  }

  /**
   * Tests {@link GenericFilePath#parse(String)}, focusing on special cases not covered by the <i>per use case</i> tests
   * of {@link RepositoryPathTests} and {@link URLPathTests}.
   */
  @Nested
  class ParseTests {
    @Test
    void testReturnsNullWhenGivenNull() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( null );
      assertNull( path );
    }

    @Test
    void testReturnsNullWhenGivenEmptyString() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "" );
      assertNull( path );
    }

    @Test
    void testReturnsNullWhenGivenAllSpacesString() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "   " );
      assertNull( path );
    }

    @Test
    void testThrowsInvalidPathExceptionIfPathNotRepositoryOrSchemeURL() {
      assertThrows( InvalidPathException.class, () -> GenericFilePath.parse( "foo" ) );
      assertThrows( InvalidPathException.class, () -> GenericFilePath.parse( "foo/bar" ) );
      assertThrows( InvalidPathException.class, () -> GenericFilePath.parse( "foo//bar" ) );
      assertThrows( InvalidPathException.class, () -> GenericFilePath.parse( "foo:/bar" ) );
    }
  }

  /**
   * Tests {@link GenericFilePath#parseRequired(String)}, focusing on special cases not covered by the
   * <i>per use case</i> tests of {@link RepositoryPathTests} and {@link URLPathTests}.
   */
  @Nested
  class ParseRequiredTests {
    @Test
    void testThrowsInvalidPathExceptionWhenGivenNull() {
      assertThrows( InvalidPathException.class, () -> GenericFilePath.parseRequired( null ) );
    }

    @Test
    void testReturnsNullWhenGivenEmptyString() {
      assertThrows( InvalidPathException.class, () -> GenericFilePath.parseRequired( "" ) );
    }

    @Test
    void testHandlesRepositoryPath() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parseRequired( "/home" );
      assertNotNull( path );
      assertArrayEquals( new String[] { "/", "home" }, path.getSegments().toArray() );
      assertEquals( path.toString(), "/home" );
    }

    @Test
    void testHandlesURLPath() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parseRequired( "scheme://my/folder" );
      assertNotNull( path );
      assertArrayEquals( new String[] { "scheme://", "my", "folder" }, path.getSegments().toArray() );
      assertEquals( path.toString(), "scheme://my/folder" );
    }
  }

  /**
   * Tests {@link GenericFilePath#getSegments()}, focusing on special cases not covered by the <i>per use case</i> tests
   * of {@link RepositoryPathTests} and {@link URLPathTests}.
   */
  @Nested
  class GetSegmentsTests {
    @Test
    void testReturnsImmutableList() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "/home" );
      assertNotNull( path );

      List<String> segments = path.getSegments();

      assertNotNull( segments );
      assertThrows( UnsupportedOperationException.class, () -> segments.add( "" ) );
    }
  }

  /**
   * <i>Per use case</i> tests for repository paths, such as {@code "/"} or {@code "/home/admin"}.
   * <p>
   * The following tests each cover a use case, and make assertions about expected results of multiple methods,
   * such as {@link GenericFilePath#parse(String)}, {@link GenericFilePath#getSegments()},
   * {@link GenericFilePath#getFirstSegment()}, {@link GenericFilePath#getRestSegments()}, or
   * {@link GenericFilePath#toString()}.
   */
  @Nested
  class RepositoryPathTests {
    void assertIsRepositoryPath( @Nullable GenericFilePath path ) {
      assertNotNull( path );
      assertFalse( path.hasScheme() );
      assertNull( path.getScheme() );
    }

    @ParameterizedTest( name = "{index} - {1}: `{0}`" )
    @CsvSource( value = {
      "'/', 'Root'",
      "'  /  ', 'Normalizes Root With Leading and Trailing Spaces'"
    } )
    void testRootPathCases( @NonNull String pathString, @NonNull String testTitle ) throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( pathString );
      assertIsRepositoryPath( path );
      assertSegments( new String[] { "/" }, path );
      assertPathStringEquals( "/", path );
    }

    @ParameterizedTest( name = "{index} - {1}: `{0}`" )
    @CsvSource( value = {
      "'/home', 'Path With Two Segments'",
      "'/home/', 'Removes Trailing Slash'",
      "'/  home', 'Normalizes Segments With Leading Spaces'",
      "'/home  ', 'Normalizes Segments With Trailing Spaces'",
      "'/  home  ', 'Normalizes Segments With Leading and Trailing Spaces'",

      "'/home//', 'Removes One Trailing Empty Segment'",
      "'/home///', 'Removes Two Consecutive Trailing Empty Segments'",
      "'/home/ / / /', 'Removes Three Consecutive Trailing Blank Segments'"
    } )
    void testTwoSegmentPathCases( @NonNull String pathString, @NonNull String testTitle ) throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( pathString );
      assertIsRepositoryPath( path );
      assertSegments( new String[] { "/", "home" }, path );
      assertPathStringEquals( "/home", path );
    }

    @ParameterizedTest( name = "{index} - {1}: `{0}`" )
    @CsvSource( value = {
      "'/home/admin', 'Path with Three Segments'",
      "'/home//admin', 'Removes One Middle Empty Segment'",
      "'/home///admin', 'Removes Two Consecutive Middle Empty Segments'",
      "'/home/ / / /admin', 'Removes Three Consecutive Middle Blank Segments'"
    } )
    void testThreeSegmentPathCases( @NonNull String pathString, @NonNull String testTitle )
      throws InvalidPathException {

      GenericFilePath path = GenericFilePath.parse( pathString );
      assertIsRepositoryPath( path );
      assertSegments( new String[] { "/", "home", "admin" }, path );
      assertPathStringEquals( "/home/admin", path );
    }

    @Test
    void testPreservesInnerSegmentSpaces() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "/home/adm in" );
      assertIsRepositoryPath( path );
      assertSegments( new String[] { "/", "home", "adm in" }, path );
      assertPathStringEquals( "/home/adm in", path );
    }
  }

  /**
   * <i>Per use case</i> tests for URL paths, such as {@code "scheme://"} or {@code "scheme://my/folder"}.
   * <p>
   * The following tests each cover a use case, and make assertions about expected results of multiple methods,
   * such as {@link GenericFilePath#parse(String)}, {@link GenericFilePath#getSegments()},
   * {@link GenericFilePath#getFirstSegment()}, {@link GenericFilePath#getRestSegments()}, or
   * {@link GenericFilePath#toString()}.
   */
  @Nested
  class URLPathTests {
    final String SAMPLE_SCHEME = "scheme";

    void assertScheme( @Nullable String scheme, @Nullable GenericFilePath path ) {
      assertNotNull( path );
      assertTrue( path.hasScheme() );
      assertEquals( scheme, path.getScheme() );
    }

    @ParameterizedTest( name = "{index} - {1}: `{0}`" )
    @CsvSource( value = {
      "'scheme://', 'Root'",
      "'  scheme://  ', 'Normalizes Root With Leading and Trailing Spaces'"
    } )
    void testRootPathCases( @NonNull String pathString, @NonNull String testTitle ) throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( pathString );
      assertScheme( SAMPLE_SCHEME, path );
      assertSegments( new String[] { "scheme://" }, path );
      assertPathStringEquals( "scheme://", path );
    }

    @Test
    void testAnotherScheme() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "other://" );
      assertScheme( "other", path );
      assertSegments( new String[] { "other://" }, path );
      assertPathStringEquals( "other://", path );
    }

    @ParameterizedTest( name = "{index} - {1}: `{0}`" )
    @CsvSource( value = {
      "'scheme://my', 'Path With Two Segments'",
      "'scheme://my/', 'Removes Trailing Slash'",
      "'scheme://  my', 'Normalizes Segments With Leading Spaces'",
      "'scheme://my  ', 'Normalizes Segments With Trailing Spaces'",
      "'scheme://  my  ', 'Normalizes Segments With Leading and Trailing Spaces'",

      "'scheme://my//', 'Removes One Trailing Empty Segment'",
      "'scheme://my///', 'Removes Two Consecutive Trailing Empty Segments'",
      "'scheme://my/ / / /', 'Removes Three Consecutive Trailing Blank Segments'"
    } )
    void testTwoSegmentPathCases( @NonNull String pathString, @NonNull String testTitle ) throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( pathString );
      assertScheme( SAMPLE_SCHEME, path );
      assertSegments( new String[] { "scheme://", "my" }, path );
      assertPathStringEquals( "scheme://my", path );
    }

    @ParameterizedTest( name = "{index} - {1}: `{0}`" )
    @CsvSource( value = {
      "'scheme://my/folder', 'Path with Three Segments'",
      "'scheme://my//folder', 'Removes One Middle Empty Segment'",
      "'scheme://my///folder', 'Removes Two Consecutive Middle Empty Segments'",
      "'scheme://my/ / / /folder', 'Removes Three Consecutive Middle Blank Segments'"
    } )
    void testThreeSegmentPathCases( @NonNull String pathString, @NonNull String testTitle )
      throws InvalidPathException {

      GenericFilePath path = GenericFilePath.parse( pathString );
      assertScheme( SAMPLE_SCHEME, path );
      assertSegments( new String[] { "scheme://", "my", "folder" }, path );
      assertPathStringEquals( "scheme://my/folder", path );
    }

    @Test
    void testPreservesInnerSegmentSpaces() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "scheme://my/fol der" );
      assertScheme( SAMPLE_SCHEME, path );
      assertSegments( new String[] { "scheme://", "my", "fol der" }, path );
      assertPathStringEquals( "scheme://my/fol der", path );
    }
  }

  /**
   * Tests the {@link GenericFilePath#equals(Object)} and {@link GenericFilePath#hashCode()} methods.
   */
  @Nested
  class EqualsTests {
    @Test
    void testEqualsItself() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "scheme://my/folder" );
      assertNotNull( path );
      // Sonar issue: Want to directly test the #equals(.) method.
      assertTrue( path.equals( path ) );
    }

    @Test
    void testNotEqualsNull() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "scheme://my/folder" );
      assertNotNull( path );
      assertFalse( path.equals( null ) );
    }

    @Test
    void testEqualsPathsWithSameSegments() throws InvalidPathException {
      GenericFilePath path1 = GenericFilePath.parse( "scheme://my/folder" );
      GenericFilePath path2 = GenericFilePath.parse( "scheme://my/folder" );
      assertNotNull( path1 );
      assertTrue( path1.equals( path2 ) );
      assertEquals( path1.hashCode(), path2.hashCode() );
    }

    @Test
    void testEqualsPathsWithSameNormalizedSegments() throws InvalidPathException {
      GenericFilePath path1 = GenericFilePath.parse( "scheme://my/  folder" );
      GenericFilePath path2 = GenericFilePath.parse( "scheme://my//folder  " );
      assertNotNull( path1 );
      assertTrue( path1.equals( path2 ) );
      assertEquals( path1.hashCode(), path2.hashCode() );
    }

    @Test
    void testDoesNotEqualObjectsOfOtherClasses() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "scheme://my/folder" );
      Object other = new Object();
      assertNotNull( path );
      assertFalse( path.equals( other ) );
    }
  }

  /**
   * Tests the {@link GenericFilePath#relativeSegments(GenericFilePath)} method.
   */
  @Nested
  class RelativeSegmentsTests {
    @Test
    void testReturnsSameSegmentsRelativeToNull() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "scheme://my/folder" );
      assertNotNull( path );

      List<String> relativeSegments = path.relativeSegments( null );

      assertNotNull( relativeSegments );
      assertEquals( path.getSegments(), relativeSegments );
    }

    @Test
    void testReturnsNullWhenRelativeToASibling() throws InvalidPathException {
      GenericFilePath path1 = GenericFilePath.parse( "scheme://my/folder1" );
      GenericFilePath path2 = GenericFilePath.parse( "scheme://my/folder2" );
      assertNotNull( path1 );
      assertNotNull( path2 );

      List<String> relativeSegments = path1.relativeSegments( path2 );

      assertNull( relativeSegments );
    }

    @Test
    void testReturnsNullWhenRelativeToAnAunt() throws InvalidPathException {
      GenericFilePath path1 = GenericFilePath.parse( "scheme://my1/folder1" );
      GenericFilePath path2 = GenericFilePath.parse( "scheme://my2" );
      assertNotNull( path1 );
      assertNotNull( path2 );

      List<String> relativeSegments = path1.relativeSegments( path2 );

      assertNull( relativeSegments );
    }

    @Test
    void testReturnsEmptySegmentsWhenRelativeToItSelf() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "scheme://my/folder" );
      assertNotNull( path );

      List<String> relativeSegments = path.relativeSegments( path );

      assertNotNull( relativeSegments );
      assertArrayEquals( new String[] {}, relativeSegments.toArray() );
    }

    @Test
    void testReturnsEmptySegmentsWhenRelativeToEqualSegmentsPath() throws InvalidPathException {
      GenericFilePath path1 = GenericFilePath.parse( "scheme://my/folder" );
      GenericFilePath path2 = GenericFilePath.parse( "scheme://my/folder" );
      assertNotNull( path1 );
      assertNotNull( path2 );

      List<String> relativeSegments = path1.relativeSegments( path2 );

      assertNotNull( relativeSegments );
      assertArrayEquals( new String[] {}, relativeSegments.toArray() );
    }

    @Test
    void testReturnsExcessSegmentsWhenRelativeToAnAncestor() throws InvalidPathException {
      GenericFilePath path1 = GenericFilePath.parse( "scheme://my/folder/a/b" );
      GenericFilePath path2 = GenericFilePath.parse( "scheme://my/folder" );
      assertNotNull( path1 );
      assertNotNull( path2 );

      List<String> relativeSegments = path1.relativeSegments( path2 );

      assertNotNull( relativeSegments );
      assertArrayEquals( new String[] { "a", "b" }, relativeSegments.toArray() );
    }
  }

  /**
   * Tests the {@link GenericFilePath#contains(GenericFilePath)} method.
   */
  @Nested
  class ContainsTests {
    @ParameterizedTest( name = "{index} - `{0}` {2}, `{1}`" )
    @CsvSource( value = {
      "'scheme://my/folder1', 'scheme://my/folder2', 'Does Not Contain a Sibling'",
      "'scheme://my1/folder', 'scheme://my2', 'Does Not Contain an Aunt'",
      "'scheme://my1', 'scheme://my2/folder', 'Does Not Contain a Cousin'"
    } )
    void testDoesNotContainVariousCases( @NonNull String path1String,
                                         @NonNull String path2String,
                                         @NonNull String testTitle )
      throws InvalidPathException {

      GenericFilePath path1 = GenericFilePath.parse( path1String );
      GenericFilePath path2 = GenericFilePath.parse( path2String );
      assertNotNull( path1 );
      assertNotNull( path2 );

      assertFalse( path1.contains( path2 ) );
    }

    @ParameterizedTest( name = "{index} - `{0}` {2}, `{1}`" )
    @CsvSource( value = {
      "'scheme://my/folder', 'scheme://my/folder', 'Contains Path With Same Segments'",
      "'scheme://my/folder', 'scheme://my/folder/a', 'Contains A Child'",
      "'scheme://my/folder', 'scheme://my/folder/a/b', 'Contains A Grandchild'"
    } )
    void testContainsVariousCases( @NonNull String path1String,
                                   @NonNull String path2String,
                                   @NonNull String testTitle )
      throws InvalidPathException {

      GenericFilePath path1 = GenericFilePath.parse( path1String );
      GenericFilePath path2 = GenericFilePath.parse( path2String );
      assertNotNull( path1 );
      assertNotNull( path2 );

      assertTrue( path1.contains( path2 ) );
    }

    @Test
    void testContainsItSelf() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "scheme://my/folder" );
      assertNotNull( path );

      assertTrue( path.contains( path ) );
    }
  }

  /**
   * Tests the {@link GenericFilePath#child(String)} method.
   */
  @Nested
  class ChildTests {
    @Test
    void testThrowsIllegalArgumentExceptionOnEmptySegment() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "scheme://my/folder" );
      assertNotNull( path );

      assertThrows( IllegalArgumentException.class, () -> path.child( "" ) );
    }

    @Test
    void testThrowsIllegalArgumentExceptionOnBlankSegment() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "scheme://my/folder" );
      assertNotNull( path );

      assertThrows( IllegalArgumentException.class, () -> path.child( "   " ) );
    }

    @Test
    void testHandlesChildOfNonRootURLPath() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "scheme://my/folder" );
      assertNotNull( path );

      GenericFilePath childPath = path.child( "a" );
      assertArrayEquals( new String[] { "scheme://", "my", "folder", "a" }, childPath.getSegments().toArray() );
      assertEquals( "scheme://my/folder/a", childPath.toString() );
    }

    @Test
    void testHandlesChildOfRootURLPath() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "scheme://" );
      assertNotNull( path );

      GenericFilePath childPath = path.child( "a" );
      assertArrayEquals( new String[] { "scheme://", "a" }, childPath.getSegments().toArray() );
      assertEquals( "scheme://a", childPath.toString() );
    }

    @Test
    void testHandlesChildOfNonRootRepositoryPath() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "/home" );
      assertNotNull( path );

      GenericFilePath childPath = path.child( "admin" );
      assertArrayEquals( new String[] { "/", "home", "admin" }, childPath.getSegments().toArray() );
      assertEquals( "/home/admin", childPath.toString() );
    }

    @Test
    void testHandlesChildOfRootRepositoryPath() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "/" );
      assertNotNull( path );

      GenericFilePath childPath = path.child( "home" );
      assertArrayEquals( new String[] { "/", "home" }, childPath.getSegments().toArray() );
      assertEquals( "/home", childPath.toString() );
    }

    @Test
    void testHandlesChildWithInnerSpacesRepositoryPath() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "/" );
      assertNotNull( path );

      GenericFilePath childPath = path.child( "ho  me" );
      assertArrayEquals( new String[] { "/", "ho  me" }, childPath.getSegments().toArray() );
      assertEquals( "/ho  me", childPath.toString() );
    }

    @Test
    void testThrowsInvalidPathExceptionIfChildHasSlashSeparator() throws InvalidPathException {
      GenericFilePath path = GenericFilePath.parse( "/" );
      assertNotNull( path );

      assertThrows( InvalidPathException.class, () -> path.child( "home/admin" ) );
    }
  }
}
