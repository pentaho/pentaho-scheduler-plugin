/*!
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
 * Copyright (c) 2024 Hitachi Vantara. All rights reserved.
 */
package org.pentaho.platform.genericfile;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.pentaho.platform.api.genericfile.GenericFilePath;
import org.pentaho.platform.api.genericfile.GetTreeOptions;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;
import org.pentaho.platform.genericfile.model.BaseGenericFile;
import org.pentaho.platform.genericfile.model.BaseGenericFileTree;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for the {@link BaseGenericFileProvider} class.
 */
public class BaseGenericFileProviderTest {
  static class GenericFileProviderForTesting<T extends IGenericFile> extends BaseGenericFileProvider<T> {
    @Override
    protected boolean createFolderCore( @NonNull GenericFilePath path ) {
      throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public BaseGenericFileTree getTreeCore( @NonNull GetTreeOptions options ) {
      throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public Class<T> getFileClass() {
      throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public String getName() {
      throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public String getType() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean doesFileExist( @NonNull GenericFilePath path ) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean owns( @NonNull GenericFilePath path ) {
      return false;
    }
  }

  // region getTree

  // region Sample Use Cases

  /**
   * Gets a sample tree of depth 1.
   *
   * <p>
   * /
   * /home
   * /public
   * <p>
   * Need real file trees, to properly support the children collection handling.
   */
  BaseGenericFileTree getSampleRepositoryTreeOfDepth1() {
    BaseGenericFileTree rootTree = createSampleFileTree( "/", "" );
    rootTree.addChild( createSampleFileTree( "/home", "home" ) );
    rootTree.addChild( createSampleFileTree( "/public", "public" ) );
    return rootTree;
  }

  /**
   * Gets a sample subtree of depth 1 under /home.
   *
   * <p>
   * /home
   * /home/admin
   * /home/suzy
   * <p>
   * Need real file trees, to properly support the children collection handling.
   */
  BaseGenericFileTree getSampleRepositoryHomeTreeOfDepth1() {
    BaseGenericFileTree homeTree = createSampleFileTree( "/home", "home" );
    homeTree.addChild( createSampleFileTree( "/home/admin", "admin" ) );
    homeTree.addChild( createSampleFileTree( "/home/suzy", "suzy" ) );
    return homeTree;
  }

  BaseGenericFileTree createSampleFileTree( String path, String name ) {
    BaseGenericFile file = new BaseGenericFile();
    file.setName( name );
    file.setPath( path );
    return new BaseGenericFileTree( file );
  }

  @NonNull
  IGenericFileTree assertRepositoryDepth1Structure( IGenericFileTree rootTree ) {

    assertNotNull( rootTree );

    // Check that the children of the home subtree are now part of the root tree.
    List<IGenericFileTree> rootChildren = rootTree.getChildren();
    assertNotNull( rootChildren );
    assertEquals( 2, rootChildren.size() );

    // ---
    // /home
    IGenericFileTree homeTree = rootChildren.get( 0 );
    assertNotNull( homeTree.getFile() );
    assertEquals( "/home", homeTree.getFile().getPath() );

    // ---
    // /public

    IGenericFileTree publicTree = rootChildren.get( 1 );
    assertNotNull( publicTree.getFile() );
    assertEquals( "/public", publicTree.getFile().getPath() );

    return homeTree;
  }

  @NonNull
  IGenericFileTree assertRepositoryHomeDepth1Structure( IGenericFileTree homeTree ) {

    // /home

    assertNotNull( homeTree );

    assertNotNull( homeTree.getFile() );
    assertEquals( "/home", homeTree.getFile().getPath() );

    List<IGenericFileTree> homeChildren = homeTree.getChildren();
    assertNotNull( homeChildren );
    assertEquals( 2, homeChildren.size() );

    IGenericFileTree homeAdminTree = homeChildren.get( 0 );
    assertNotNull( homeAdminTree );
    assertNotNull( homeAdminTree.getFile() );
    assertEquals( "/home/admin", homeAdminTree.getFile().getPath() );

    IGenericFileTree homeSuzyTree = homeChildren.get( 1 );
    assertNotNull( homeSuzyTree );
    assertNotNull( homeSuzyTree.getFile() );
    assertEquals( "/home/suzy", homeSuzyTree.getFile().getPath() );

    return homeAdminTree;
  }
  // endregion

  // region Caching
  // Opting to use the actual GetTreeOptions class, to be sure that the caching is properly functioning, including
  // integrated with the GetTreeOptions class, regarding its hash code function.
  @Test
  public void testGetTreeUsesCacheWhenGivenEqualOptions() throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );
    BaseGenericFileTree tree = mock( BaseGenericFileTree.class );

    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );
    doReturn( tree ).when( provider ).getTreeCore( any( GetTreeOptions.class ) );

    // Call #1

    GetTreeOptions options1 = new GetTreeOptions();
    options1.setBasePath( "/A" );
    options1.setExpandedPath( "/B" );
    options1.setMaxDepth( 1 );
    options1.setFilter( GetTreeOptions.TreeFilter.FOLDERS );

    IGenericFileTree result1 = provider.getTree( options1 );

    assertSame( tree, result1 );
    verify( provider, times( 1 ) ).getTreeCore( any( GetTreeOptions.class ) );

    // Call #2, with not same but equal options

    GetTreeOptions options2 = new GetTreeOptions();
    options2.setBasePath( "/A" );
    options2.setExpandedPath( "/B" );
    options2.setMaxDepth( 1 );
    options2.setFilter( GetTreeOptions.TreeFilter.FOLDERS );

    IGenericFileTree result2 = provider.getTree( options2 );

    assertSame( tree, result2 );
    // No additional calls made to getTreeCore proves cache was used.
    verify( provider, times( 1 ) ).getTreeCore( any( GetTreeOptions.class ) );
  }

  @Test
  public void testGetTreeDoesNotUseCacheWhenGivenDifferentOptions() throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );
    BaseGenericFileTree tree1 = mock( BaseGenericFileTree.class );
    BaseGenericFileTree tree2 = mock( BaseGenericFileTree.class );

    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );
    doReturn( tree1, tree2 ).when( provider ).getTreeCore( any( GetTreeOptions.class ) );

    // Call #1

    GetTreeOptions options1 = new GetTreeOptions();
    options1.setBasePath( "/A" );
    options1.setExpandedPath( "/B" );
    options1.setMaxDepth( 1 );
    options1.setFilter( GetTreeOptions.TreeFilter.FOLDERS );

    IGenericFileTree result1 = provider.getTree( options1 );

    assertSame( tree1, result1 );
    verify( provider, times( 1 ) ).getTreeCore( any( GetTreeOptions.class ) );

    // Call #2, with different options

    GetTreeOptions options2 = new GetTreeOptions();
    options2.setBasePath( "/B" );
    options2.setExpandedPath( "/C" );
    options2.setMaxDepth( 2 );
    options2.setFilter( GetTreeOptions.TreeFilter.FILES );

    IGenericFileTree result2 = provider.getTree( options2 );

    assertSame( tree2, result2 );
    verify( provider, times( 2 ) ).getTreeCore( any( GetTreeOptions.class ) );
  }
  // endregion

  // region Expanded Path

  void assertNestedExpandPathGetTreeOptions( @NonNull String expectedBasePath, GetTreeOptions options ) {
    assertNotNull( options );
    assertNull( options.getExpandedPath() );
    assertSame( 1, options.getMaxDepth() );
    assertNotNull( options.getBasePath() );
    assertEquals( expectedBasePath, options.getBasePath().toString() );
  }

  @Test
  public void testGetTreeDoesNotExpandPathIfExpandedPathIsNull() throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );
    BaseGenericFileTree tree = getSampleRepositoryTreeOfDepth1();

    // Owns the base path and the expanded path!
    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );
    doReturn( tree ).when( provider ).getTreeCore( any( GetTreeOptions.class ) );

    GetTreeOptions options = mock( GetTreeOptions.class );
    doReturn( mock( GenericFilePath.class ) ).when( options ).getBasePath();
    doReturn( null ).when( options ).getExpandedPath();
    doReturn( 1 ).when( options ).getMaxDepth();

    IGenericFileTree result = provider.getTree( options );

    assertSame( tree, result );

    // Expand path recursively calls getTree to expand each level.
    // There should be a single call corresponding to the above call.
    verify( provider, times( 1 ) ).getTree( any( GetTreeOptions.class ) );
  }

  @Test
  public void testGetTreeDoesNotExpandPathIfMaxDepthIsNull() throws OperationFailedException {
    // All folders should already be included, so no need to expand anything.
    // Also, expanded path should be a descendant or self of the base path.

    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );
    BaseGenericFileTree tree = getSampleRepositoryTreeOfDepth1();

    // Owns the base path and the expanded path!
    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );
    doReturn( tree ).when( provider ).getTreeCore( any( GetTreeOptions.class ) );

    GetTreeOptions options = mock( GetTreeOptions.class );
    doReturn( mock( GenericFilePath.class ) ).when( options ).getBasePath();
    doReturn( mock( GenericFilePath.class ) ).when( options ).getExpandedPath();
    doReturn( null ).when( options ).getMaxDepth();

    IGenericFileTree result = provider.getTree( options );

    assertSame( tree, result );

    // Expand path recursively calls getTree to expand each level.
    // There should be a single call corresponding to the above call.
    verify( provider, times( 1 ) ).getTree( any( GetTreeOptions.class ) );
  }

  @Test
  public void testGetTreeDoesNotExpandPathIfExpandedPathNotOwned() throws OperationFailedException {
    // Expanded path should be a descendant or self of the base path, or is ignored.

    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );
    BaseGenericFileTree tree = getSampleRepositoryTreeOfDepth1();

    doReturn( tree ).when( provider ).getTreeCore( any( GetTreeOptions.class ) );

    // ---

    GetTreeOptions options = mock( GetTreeOptions.class );

    GenericFilePath basePathMock = mock( GenericFilePath.class );
    doReturn( basePathMock ).when( options ).getBasePath();

    GenericFilePath expandedPathMock = mock( GenericFilePath.class );
    doReturn( expandedPathMock ).when( options ).getExpandedPath();

    // Owns the base path, but not the expanded path.
    doReturn( true ).when( provider ).owns( basePathMock );
    doReturn( false ).when( provider ).owns( expandedPathMock );

    doReturn( 1 ).when( options ).getMaxDepth();

    // ---

    IGenericFileTree result = provider.getTree( options );

    assertSame( tree, result );

    // Expand path recursively calls getTree to expand each level.
    // There should be a single call corresponding to the above call.
    verify( provider, times( 1 ) ).getTree( any( GetTreeOptions.class ) );
  }

  @Test
  public void testGetTreeDoesNotExpandPathIfExpandedPathWithinMaxDepth() throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );

    BaseGenericFileTree tree = getSampleRepositoryTreeOfDepth1();
    doReturn( tree ).when( provider ).getTreeCore( any( GetTreeOptions.class ) );

    // ---

    GetTreeOptions options = new GetTreeOptions();
    options.setBasePath( "/" );
    // Expanded path is 1 level below base path, and thus within max depth of 1.
    options.setExpandedPath( "/home" );
    options.setMaxDepth( 1 );

    // ---

    IGenericFileTree result = provider.getTree( options );

    assertSame( tree, result );

    // Expand path recursively calls getTree to expand each level.
    // There should be a single call corresponding to the above call.
    verify( provider, times( 1 ) ).getTree( any( GetTreeOptions.class ) );
  }

  @Test
  public void testGetTreeExpandsPathIfExpandedPathIsDeeperThanMaxDepth() throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );

    BaseGenericFileTree rootTree = getSampleRepositoryTreeOfDepth1();
    BaseGenericFileTree homeTree = getSampleRepositoryHomeTreeOfDepth1();

    doReturn( rootTree, homeTree ).when( provider ).getTreeCore( any( GetTreeOptions.class ) );

    // ---

    GetTreeOptions options = new GetTreeOptions();
    options.setBasePath( "/" );

    // Expanded path is 2 levels below base path, and thus deeper than the max depth of 1.
    options.setExpandedPath( "/home/admin" );
    options.setMaxDepth( 1 );

    // ---

    IGenericFileTree result = provider.getTree( options );

    // ---

    assertSame( rootTree, result );

    // ---

    // Expand path recursively calls getTree to expand each extra level below max depth.
    // That's two calls. One for the top-level getTree call,
    // and another for the extra level of admin (the children of /home).
    ArgumentCaptor<GetTreeOptions> getTreeArgumentCaptor = ArgumentCaptor.forClass( GetTreeOptions.class );
    verify( provider, times( 2 ) ).getTree( getTreeArgumentCaptor.capture() );

    List<GetTreeOptions> optionsList = getTreeArgumentCaptor.getAllValues();
    assertEquals( 2, optionsList.size() );
    assertSame( options, optionsList.get( 0 ) );
    assertNestedExpandPathGetTreeOptions( "/home", optionsList.get( 1 ) );

    // ---

    IGenericFileTree rootHomeTree = assertRepositoryDepth1Structure( rootTree );
    assertRepositoryHomeDepth1Structure( rootHomeTree );
  }

  @Test
  public void testGetTreeExpandsPathAndStopsIfExpandedPathSegmentDoesNotExistInParent()
    throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );

    BaseGenericFileTree rootTree = getSampleRepositoryTreeOfDepth1();
    BaseGenericFileTree homeTree = getSampleRepositoryHomeTreeOfDepth1();

    BaseGenericFileTree homeAdminTree = createSampleFileTree( "/home/admin", "admin" );
    homeAdminTree.setChildren( Collections.emptyList() );

    doReturn( rootTree, homeTree, homeAdminTree ).when( provider ).getTreeCore( any( GetTreeOptions.class ) );

    // ---

    GetTreeOptions options = new GetTreeOptions();
    options.setBasePath( "/" );

    // Expanded path is 4 levels below base path, and thus deeper than the max depth of 1.
    options.setExpandedPath( "/home/admin/missingFolder/anotherMissingFolder" );
    options.setMaxDepth( 1 );

    // ---

    IGenericFileTree resultRootTree = provider.getTree( options );

    // ---

    assertSame( rootTree, resultRootTree );

    // ---

    // Expand path recursively calls getTree to expand each extra level below max depth.
    // That's three calls. One for the top-level getTree call,
    // another for the extra level of admin (the children of /home),
    // and another for the extra level of missingFolder (the children of /home/admin).
    ArgumentCaptor<GetTreeOptions> getTreeArgumentCaptor = ArgumentCaptor.forClass( GetTreeOptions.class );
    verify( provider, times( 3 ) ).getTree( getTreeArgumentCaptor.capture() );

    List<GetTreeOptions> optionsList = getTreeArgumentCaptor.getAllValues();
    assertEquals( 3, optionsList.size() );
    assertSame( options, optionsList.get( 0 ) );
    assertNestedExpandPathGetTreeOptions( "/home", optionsList.get( 1 ) );
    assertNestedExpandPathGetTreeOptions( "/home/admin", optionsList.get( 2 ) );

    // ---

    // Check that the children of the home subtree are now part of the root tree.
    IGenericFileTree resultHomeTree = assertRepositoryDepth1Structure( resultRootTree );
    IGenericFileTree resultAdminTree = assertRepositoryHomeDepth1Structure( resultHomeTree );

    // Check that empty children were returned for /home/admin.
    List<IGenericFileTree> adminChildren = resultAdminTree.getChildren();
    assertNotNull( adminChildren );
    assertTrue( adminChildren.isEmpty() );
  }
  // endregion

  // endregion
}
