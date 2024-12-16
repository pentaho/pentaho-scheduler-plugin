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

package org.pentaho.platform.genericfile;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.pentaho.platform.api.genericfile.GenericFilePath;
import org.pentaho.platform.api.genericfile.GenericFilePermission;
import org.pentaho.platform.api.genericfile.GetTreeOptions;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.api.genericfile.model.IGenericFileContentWrapper;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;
import org.pentaho.platform.genericfile.model.BaseGenericFile;
import org.pentaho.platform.genericfile.model.BaseGenericFileTree;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.pentaho.platform.api.genericfile.model.IGenericFile.TYPE_FOLDER;

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
    protected List<BaseGenericFileTree> getRootTreesCore( @NonNull GetTreeOptions options )
      throws OperationFailedException {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasAccess( @NonNull GenericFilePath path, EnumSet<GenericFilePermission> permissions ) {
      throw new UnsupportedOperationException();
    }

    @Override public IGenericFileContentWrapper getFileContentWrapper( @NonNull GenericFilePath path ) {
      throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public IGenericFile getFile( @NonNull GenericFilePath path ) {
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
    public boolean doesFolderExist( @NonNull GenericFilePath path ) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean owns( @NonNull GenericFilePath path ) {
      return false;
    }
  }

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

  BaseGenericFileTree getSampleRepositoryAdminTreeOfDepth1() {
    BaseGenericFileTree adminTree = createSampleFileTree( "/home/admin", "admin" );
    adminTree.addChild( createSampleFileTree( "/home/admin/folder1", "folder1" ) );
    adminTree.addChild( createSampleFileTree( "/home/admin/folder2", "folder2" ) );
    return adminTree;
  }

  BaseGenericFileTree getSampleRepositoryAdminTreeOfDepth2() {
    BaseGenericFileTree adminTree = createSampleFileTree( "/home/admin", "admin" );
    adminTree.addChild( getSampleRepositoryAdminFolder1TreeOfDepth1() );
    adminTree.addChild( getSampleRepositoryAdminFolder2TreeOfDepth1() );
    return adminTree;
  }

  BaseGenericFileTree getSampleRepositoryAdminFolder1TreeOfDepth1() {
    BaseGenericFileTree folder1Tree = createSampleFileTree( "/home/admin/folder1", "folder1" );
    folder1Tree.addChild( createSampleFileTree( "/home/admin/folder1/subfolder1", "subfolder1" ) );
    return folder1Tree;
  }

  BaseGenericFileTree getSampleRepositoryAdminFolder2TreeOfDepth1() {
    BaseGenericFileTree folder2Tree = createSampleFileTree( "/home/admin/folder2", "folder2" );
    folder2Tree.addChild( createSampleFileTree( "/home/admin/folder2/subfolder1", "subfolder1" ) );
    return folder2Tree;
  }

  BaseGenericFileTree getSampleVfsTreeOfDepth1() {
    BaseGenericFileTree rootTree = createSampleFileTree( "pvfs://", "pvfs://" );
    rootTree.addChild( createSampleFileTree( "pvfs://demo1", "demo1" ) );
    rootTree.addChild( createSampleFileTree( "pvfs://demo2", "demo2" ) );
    return rootTree;
  }

  BaseGenericFileTree getSamplePvfsDemo1TreeOfDepth1() {
    BaseGenericFileTree demo1Tree = createSampleFileTree( "pvfs://demo1", "demo1" );
    demo1Tree.addChild( createSampleFileTree( "pvfs://demo1/foo", "foo" ) );
    demo1Tree.addChild( createSampleFileTree( "pvfs://demo1/bar", "bar" ) );
    return demo1Tree;
  }

  BaseGenericFileTree getSamplePvfsDemo1FooTreeOfDepth1() {
    BaseGenericFileTree fooTree = createSampleFileTree( "pvfs://demo1/foo", "foo" );
    fooTree.setChildren( List.of() );
    return fooTree;
  }

  BaseGenericFileTree getSamplePvfsDemo2TreeOfDepth1() {
    BaseGenericFileTree demo2Tree = createSampleFileTree( "pvfs://demo2", "demo2" );
    demo2Tree.addChild( createSampleFileTree( "pvfs://demo2/duu", "duu" ) );
    demo2Tree.addChild( createSampleFileTree( "pvfs://demo2/gaa", "gaa" ) );
    return demo2Tree;
  }

  BaseGenericFileTree createSampleFileTree( String path, String name ) {
    BaseGenericFile file = new BaseGenericFile();
    file.setName( name );
    file.setPath( path );
    file.setType( TYPE_FOLDER );
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

  void assertRepositoryAdminDepth1Structure( IGenericFileTree adminTree ) {

    // /admin

    assertNotNull( adminTree );

    assertNotNull( adminTree.getFile() );
    assertEquals( "/home/admin", adminTree.getFile().getPath() );

    List<IGenericFileTree> adminChildren = adminTree.getChildren();
    assertNotNull( adminChildren );
    assertEquals( 2, adminChildren.size() );

    IGenericFileTree adminFolder1Tree = adminChildren.get( 0 );
    assertNotNull( adminFolder1Tree );
    assertNotNull( adminFolder1Tree.getFile() );
    assertEquals( "/home/admin/folder1", adminFolder1Tree.getFile().getPath() );

    IGenericFileTree adminFolder2Tree = adminChildren.get( 1 );
    assertNotNull( adminFolder2Tree );
    assertNotNull( adminFolder2Tree.getFile() );
    assertEquals( "/home/admin/folder2", adminFolder2Tree.getFile().getPath() );
  }

  void assertRepositoryAdminFolder1Depth1Structure( IGenericFileTree folder1Tree ) {

    // /admin/folder1

    assertNotNull( folder1Tree );

    assertNotNull( folder1Tree.getFile() );
    assertEquals( "/home/admin/folder1", folder1Tree.getFile().getPath() );

    List<IGenericFileTree> folder1TreeChildren = folder1Tree.getChildren();
    assertNotNull( folder1TreeChildren );
    assertEquals( 1, folder1TreeChildren.size() );

    IGenericFileTree subfolder1Tree = folder1TreeChildren.get( 0 );
    assertNotNull( subfolder1Tree );
    assertNotNull( subfolder1Tree.getFile() );
    assertEquals( "/home/admin/folder1/subfolder1", subfolder1Tree.getFile().getPath() );
  }

  void assertRepositoryAdminFolder2Depth1Structure( IGenericFileTree folder2Tree ) {

    // /admin/folder1

    assertNotNull( folder2Tree );

    assertNotNull( folder2Tree.getFile() );
    assertEquals( "/home/admin/folder2", folder2Tree.getFile().getPath() );

    List<IGenericFileTree> folder2TreeChildren = folder2Tree.getChildren();
    assertNotNull( folder2TreeChildren );
    assertEquals( 1, folder2TreeChildren.size() );

    IGenericFileTree subfolder1Tree = folder2TreeChildren.get( 0 );
    assertNotNull( subfolder1Tree );
    assertNotNull( subfolder1Tree.getFile() );
    assertEquals( "/home/admin/folder2/subfolder1", subfolder1Tree.getFile().getPath() );
  }

  // endregion

  // region getTree

  // region Caching
  // Opting to use the actual GetTreeOptions class, to be sure that the caching is properly functioning, including
  // integrated with the GetTreeOptions class, regarding its hash code function.
  @Test
  public void testGetTreeUsesCacheWhenGivenEqualOptions() throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );
    BaseGenericFileTree tree = mock( BaseGenericFileTree.class );
    BaseGenericFile file = mock( BaseGenericFile.class );

    doReturn( file ).when( tree ).getFile();
    doReturn( "/A" ).when( file ).getPath();

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

    assertSame( tree.getFile(), result2.getFile() );
    assertEquals( tree.getChildren(), result2.getChildren() );
    // No additional calls made to getTreeCore proves cache was used.
    verify( provider, times( 1 ) ).getTreeCore( any( GetTreeOptions.class ) );
  }

  @Test
  public void testGetTreeDoesNotUseCacheWhenGivenEqualOptionsAndBypassCache() throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    BaseGenericFileTree tree1 = mock( BaseGenericFileTree.class );
    BaseGenericFileTree tree2 = mock( BaseGenericFileTree.class );

    BaseGenericFile file1 = mock( BaseGenericFile.class );
    doReturn( file1 ).when( tree1 ).getFile();
    doReturn( "/A" ).when( file1 ).getPath();

    BaseGenericFile file2 = mock( BaseGenericFile.class );
    doReturn( file2 ).when( tree2 ).getFile();
    doReturn( "/B" ).when( file2 ).getPath();

    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );
    doReturn( tree1, tree2 ).when( provider ).getTreeCore( any( GetTreeOptions.class ) );

    // Call #1 - firstly, no cache bypass, so that it surely stores in cache.

    GetTreeOptions options1 = new GetTreeOptions();
    options1.setBasePath( "/A" );
    options1.setExpandedPath( "/B" );
    options1.setMaxDepth( 1 );
    options1.setFilter( GetTreeOptions.TreeFilter.FOLDERS );
    options1.setBypassCache( false );

    IGenericFileTree result1 = provider.getTree( options1 );

    assertSame( tree1, result1 );
    verify( provider, times( 1 ) ).getTreeCore( any( GetTreeOptions.class ) );

    // Call #2, with not same, but equal options, and bypass cache

    GetTreeOptions options2 = new GetTreeOptions();
    options2.setBasePath( "/A" );
    options2.setExpandedPath( "/B" );
    options2.setMaxDepth( 1 );
    options2.setFilter( GetTreeOptions.TreeFilter.FOLDERS );
    options2.setBypassCache( true );

    IGenericFileTree result2 = provider.getTree( options2 );

    assertSame( tree2, result2 );
    verify( provider, times( 2 ) ).getTreeCore( any( GetTreeOptions.class ) );
  }


  @Test
  public void testGetTreeCachesResultEvenWhenGivenBypassCache() throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    BaseGenericFileTree tree1 = mock( BaseGenericFileTree.class );
    BaseGenericFileTree tree2 = mock( BaseGenericFileTree.class );

    BaseGenericFile file1 = mock( BaseGenericFile.class );
    doReturn( file1 ).when( tree1 ).getFile();
    doReturn( "/A" ).when( file1 ).getPath();

    BaseGenericFile file2 = mock( BaseGenericFile.class );
    doReturn( file2 ).when( tree2 ).getFile();
    doReturn( "/B" ).when( file2 ).getPath();

    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );
    doReturn( tree1, tree2 ).when( provider ).getTreeCore( any( GetTreeOptions.class ) );

    // Call #1 - firstly, no cache bypass, so that it surely stores in cache.

    GetTreeOptions options1 = new GetTreeOptions();
    options1.setBasePath( "/A" );
    options1.setExpandedPath( "/B" );
    options1.setMaxDepth( 1 );
    options1.setFilter( GetTreeOptions.TreeFilter.FOLDERS );
    options1.setBypassCache( true );

    IGenericFileTree result1 = provider.getTree( options1 );

    assertSame( tree1, result1 );
    verify( provider, times( 1 ) ).getTreeCore( any( GetTreeOptions.class ) );

    // Call #2, with not same, but equal options, and bypass cache

    GetTreeOptions options2 = new GetTreeOptions();
    options2.setBasePath( "/A" );
    options2.setExpandedPath( "/B" );
    options2.setMaxDepth( 1 );
    options2.setFilter( GetTreeOptions.TreeFilter.FOLDERS );
    options2.setBypassCache( false );

    IGenericFileTree result2 = provider.getTree( options2 );

    assertSame( tree1.getFile(), result2.getFile() );
    assertEquals( tree1.getChildren(), result2.getChildren() );

    // No additional calls made to getTreeCore proves cache was used.
    verify( provider, times( 1 ) ).getTreeCore( any( GetTreeOptions.class ) );
  }

  @Test
  public void testGetTreeDoesNotUseCacheWhenGivenDifferentOptions() throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );
    BaseGenericFileTree tree1 = mock( BaseGenericFileTree.class );
    BaseGenericFileTree tree2 = mock( BaseGenericFileTree.class );

    BaseGenericFile file1 = mock( BaseGenericFile.class );
    doReturn( file1 ).when( tree1 ).getFile();
    doReturn( "/A" ).when( file1 ).getPath();

    BaseGenericFile file2 = mock( BaseGenericFile.class );
    doReturn( file2 ).when( tree2 ).getFile();
    doReturn( "/B" ).when( file2 ).getPath();

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

  void assertNestedExpandPathGetTreeOptions( @NonNull String expectedBasePath, Integer expectedMaxDepth,
                                             GetTreeOptions options ) {
    assertNotNull( options );
    assertNull( options.getExpandedPaths() );
    assertSame( expectedMaxDepth, options.getMaxDepth() );
    assertNotNull( options.getBasePath() );
    assertEquals( expectedBasePath, options.getBasePath().toString() );
  }

  void assertNestedExpandPathGetTreeOptions( @NonNull String expectedBasePath, GetTreeOptions options ) {
    assertNestedExpandPathGetTreeOptions( expectedBasePath, 1, options );
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
    doReturn( null ).when( options ).getExpandedPaths();
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
    doReturn( List.of( mock( GenericFilePath.class ) ) ).when( options ).getExpandedPaths();
    doReturn( null ).when( options ).getMaxDepth();

    // Even if expanded max depth were not null!
    doReturn( 1 ).when( options ).getExpandedMaxDepth();

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
    doReturn( List.of( expandedPathMock ) ).when( options ).getExpandedPaths();

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
  public void testGetTreeDoesExpandPathForItsChildrenIfExpandedPathWithinMaxDepthAndNullExpandedMaxDepth()
    throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );

    BaseGenericFileTree tree = getSampleRepositoryTreeOfDepth1();
    doReturn( tree ).when( provider ).getTreeCore( any( GetTreeOptions.class ) );

    // ---

    GetTreeOptions options = new GetTreeOptions();
    options.setBasePath( "/" );
    // Expanded path is 1 level below base path, and thus within max depth of 1 (last element)
    // Will still need to request its children with the same max depth of 1.
    options.setExpandedPath( "/home" );
    options.setMaxDepth( 1 );

    // ---

    IGenericFileTree result = provider.getTree( options );

    assertSame( tree, result );

    // Expand path recursively calls getTree to expand each level.
    // There should be a call corresponding to the above call and another for the expanded path children.
    verify( provider, times( 2 ) ).getTree( any( GetTreeOptions.class ) );
  }

  @Test
  public void testGetTreeDoesNotExpandPathForItsChildrenIfExpandedPathWithinMaxDepthAndWithExpandedMaxDepthOfZero()
    throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );

    BaseGenericFileTree tree = getSampleRepositoryTreeOfDepth1();
    doReturn( tree ).when( provider ).getTreeCore( any( GetTreeOptions.class ) );

    // ---

    GetTreeOptions options = new GetTreeOptions();
    options.setBasePath( "/" );
    // Expanded path is 1 level below base path, and thus within max depth of 1 (last element)
    // Will still need to request its children with the same max depth of 1.
    options.setExpandedPath( "/home" );
    options.setMaxDepth( 1 );
    options.setExpandedMaxDepth( 0 );

    // ---

    IGenericFileTree result = provider.getTree( options );

    assertSame( tree, result );

    // Expand path recursively calls getTree to expand each level.
    // There should be a call corresponding to the above call and none for the expanded path children.
    verify( provider, times( 1 ) ).getTree( any( GetTreeOptions.class ) );
  }

  @Test
  public void testGetTreeExpandsPathIfExpandedPathIsDeeperAndWithExpandedMaxDepthOfTwo()
    throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );

    BaseGenericFileTree rootTree = getSampleRepositoryTreeOfDepth1();
    BaseGenericFileTree homeTree = getSampleRepositoryHomeTreeOfDepth1();
    BaseGenericFileTree adminTree = getSampleRepositoryAdminTreeOfDepth2();

    doReturn( rootTree, homeTree, adminTree )
      .when( provider )
      .getTreeCore( any( GetTreeOptions.class ) );

    // ---

    GetTreeOptions options = new GetTreeOptions();
    options.setBasePath( "/" );

    // Expanded path is 2 levels below base path, and thus deeper than the max depth of 1.
    options.setExpandedPath( "/home/admin" );
    options.setMaxDepth( 1 );
    options.setExpandedMaxDepth( 2 );

    // ---

    IGenericFileTree result = provider.getTree( options );

    // ---

    assertSame( rootTree, result );

    // ---

    // Expand path recursively calls getTree to expand each extra level below max depth.
    // That's four calls. One for the top-level getTree call,
    // another for the extra level of admin (the children of /home),
    // and another two requesting the admin children.

    ArgumentCaptor<GetTreeOptions> getTreeArgumentCaptor = ArgumentCaptor.forClass( GetTreeOptions.class );
    verify( provider, times( 3 ) ).getTree( getTreeArgumentCaptor.capture() );

    List<GetTreeOptions> optionsList = getTreeArgumentCaptor.getAllValues();
    assertEquals( 3, optionsList.size() );
    assertSame( options, optionsList.get( 0 ) );
    assertNestedExpandPathGetTreeOptions( "/home", optionsList.get( 1 ) );
    assertNestedExpandPathGetTreeOptions( "/home/admin", 2, optionsList.get( 2 ) );

    // ---

    IGenericFileTree rootHomeTree = assertRepositoryDepth1Structure( rootTree );
    IGenericFileTree homeAdminTree = assertRepositoryHomeDepth1Structure( rootHomeTree );
    assertRepositoryAdminDepth1Structure( homeAdminTree );

    assertRepositoryAdminFolder1Depth1Structure( homeAdminTree.getChildren().get( 0 ) );
    assertRepositoryAdminFolder2Depth1Structure( homeAdminTree.getChildren().get( 1 ) );
  }

  @Test
  public void testGetTreeExpandsPathIfExpandedPathIsDeeperThanMaxDepth() throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );

    BaseGenericFileTree rootTree = getSampleRepositoryTreeOfDepth1();
    BaseGenericFileTree homeTree = getSampleRepositoryHomeTreeOfDepth1();
    BaseGenericFileTree adminTree = getSampleRepositoryAdminTreeOfDepth1();

    doReturn( rootTree, homeTree, adminTree ).when( provider ).getTreeCore( any( GetTreeOptions.class ) );

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
    // That's three calls. One for the top-level getTree call,
    // another for the extra level of admin (the children of /home),
    // and another one requesting the admin children (depth is applied to children of the expandedPath - /home/admin).

    ArgumentCaptor<GetTreeOptions> getTreeArgumentCaptor = ArgumentCaptor.forClass( GetTreeOptions.class );
    verify( provider, times( 3 ) ).getTree( getTreeArgumentCaptor.capture() );

    List<GetTreeOptions> optionsList = getTreeArgumentCaptor.getAllValues();
    assertEquals( 3, optionsList.size() );
    assertSame( options, optionsList.get( 0 ) );
    assertNestedExpandPathGetTreeOptions( "/home", optionsList.get( 1 ) );
    assertNestedExpandPathGetTreeOptions( "/home/admin", optionsList.get( 2 ) );

    // ---

    IGenericFileTree rootHomeTree = assertRepositoryDepth1Structure( rootTree );
    IGenericFileTree homeAdminTree = assertRepositoryHomeDepth1Structure( rootHomeTree );
    assertRepositoryAdminDepth1Structure( homeAdminTree );
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

  @Test
  public void testGetTreeExpandsPathAndExpandsItsChildrenWithSameMaxDepth() throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );

    BaseGenericFileTree rootTree = getSampleRepositoryTreeOfDepth1();
    BaseGenericFileTree homeTree = getSampleRepositoryHomeTreeOfDepth1();
    BaseGenericFileTree adminTree = getSampleRepositoryAdminTreeOfDepth1();

    doReturn( rootTree, homeTree, adminTree ).when( provider ).getTreeCore( any( GetTreeOptions.class ) );

    // ---

    GetTreeOptions options = new GetTreeOptions();
    options.setBasePath( "/" );

    // Expanded path is 2 levels below base path, and thus deeper than the max depth of 1.
    // Will need to request 'getTree' to get children of /home and then another to get children of /home/admin to
    // expand the children of the expanded path with same max depth of 1.
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
    verify( provider, times( 3 ) ).getTree( getTreeArgumentCaptor.capture() );

    List<GetTreeOptions> optionsList = getTreeArgumentCaptor.getAllValues();
    assertEquals( 3, optionsList.size() );
    assertSame( options, optionsList.get( 0 ) );
    assertNestedExpandPathGetTreeOptions( "/home/admin", optionsList.get( 2 ) );

    // ---

    IGenericFileTree rootHomeTree = assertRepositoryDepth1Structure( rootTree );
    IGenericFileTree homeAdminTree = assertRepositoryHomeDepth1Structure( rootHomeTree );
    assertRepositoryAdminDepth1Structure( homeAdminTree );
  }
  // endregion

  // endregion

  // region getRootTrees

  // region Caching
  // Opting to use the actual GetTreeOptions class, to be sure that the caching is properly functioning, including
  // integrated with the GetTreeOptions class, regarding its hash code function.
  @Test
  public void testGetRootTreesIgnoresCache() throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    BaseGenericFileTree tree1 = mock( BaseGenericFileTree.class );
    BaseGenericFileTree tree2 = mock( BaseGenericFileTree.class );
    BaseGenericFileTree tree3 = mock( BaseGenericFileTree.class );
    BaseGenericFileTree tree4 = mock( BaseGenericFileTree.class );

    BaseGenericFile file1 = mock( BaseGenericFile.class );
    doReturn( file1 ).when( tree1 ).getFile();
    doReturn( "/A" ).when( file1 ).getPath();

    BaseGenericFile file2 = mock( BaseGenericFile.class );
    doReturn( file2 ).when( tree2 ).getFile();
    doReturn( "/B" ).when( file2 ).getPath();

    BaseGenericFile file3 = mock( BaseGenericFile.class );
    doReturn( file3 ).when( tree3 ).getFile();
    doReturn( "/A" ).when( file3 ).getPath();

    BaseGenericFile file4 = mock( BaseGenericFile.class );
    doReturn( file4 ).when( tree4 ).getFile();
    doReturn( "/B" ).when( file4 ).getPath();

    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );
    doReturn( List.of( tree1, tree2 ), List.of( tree3, tree4 ) ).when( provider )
      .getRootTreesCore( any( GetTreeOptions.class ) );

    // Call #1

    GetTreeOptions options1 = new GetTreeOptions();
    options1.setBasePath( "/A" );
    options1.setExpandedPath( "/B" );
    options1.setMaxDepth( 1 );
    options1.setFilter( GetTreeOptions.TreeFilter.FOLDERS );
    options1.setBypassCache( false );

    List<IGenericFileTree> rootTrees1 = provider.getRootTrees( options1 );

    assertNotNull( rootTrees1 );
    assertEquals( 2, rootTrees1.size() );
    assertSame( tree1, rootTrees1.get( 0 ) );
    assertSame( tree2, rootTrees1.get( 1 ) );

    verify( provider, times( 1 ) ).getRootTreesCore( any( GetTreeOptions.class ) );

    // Call #2, with equal options

    GetTreeOptions options2 = new GetTreeOptions();
    options2.setBasePath( "/A" );
    options2.setExpandedPath( "/B" );
    options2.setMaxDepth( 1 );
    options2.setFilter( GetTreeOptions.TreeFilter.FOLDERS );
    options2.setBypassCache( false );

    List<IGenericFileTree> rootTrees2 = provider.getRootTrees( options2 );

    assertNotNull( rootTrees2 );
    assertEquals( 2, rootTrees2.size() );
    assertSame( tree3, rootTrees2.get( 0 ) );
    assertSame( tree4, rootTrees2.get( 1 ) );

    verify( provider, times( 2 ) ).getRootTreesCore( any( GetTreeOptions.class ) );
  }
  // endregion

  // region Expanded Path
  @Test
  public void testGetRootTreesDoesNotExpandPathIfExpandedPathIsNull() throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    BaseGenericFileTree tree1 = mock( BaseGenericFileTree.class );
    BaseGenericFileTree tree2 = mock( BaseGenericFileTree.class );

    BaseGenericFile file1 = mock( BaseGenericFile.class );
    doReturn( file1 ).when( tree1 ).getFile();
    doReturn( "/A" ).when( file1 ).getPath();

    BaseGenericFile file2 = mock( BaseGenericFile.class );
    doReturn( file2 ).when( tree2 ).getFile();
    doReturn( "/B" ).when( file2 ).getPath();

    // Owns the base path and the expanded path!
    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );
    doReturn( List.of( tree1, tree2 ) ).when( provider )
      .getRootTreesCore( any( GetTreeOptions.class ) );

    GetTreeOptions options = mock( GetTreeOptions.class );
    doReturn( mock( GenericFilePath.class ) ).when( options ).getBasePath();
    doReturn( null ).when( options ).getExpandedPaths();
    doReturn( 1 ).when( options ).getMaxDepth();

    List<IGenericFileTree> rootTrees = provider.getRootTrees( options );

    assertNotNull( rootTrees );
    assertEquals( 2, rootTrees.size() );
    assertSame( tree1, rootTrees.get( 0 ) );
    assertSame( tree2, rootTrees.get( 1 ) );

    // Expand path recursively calls getTree to expand each level.
    // There should be a single call corresponding to the above call.
    verify( provider, never() ).getTree( any( GetTreeOptions.class ) );
  }

  @Test
  public void testGetRootTreesDoesNotExpandPathIfMaxDepthIsNull() throws OperationFailedException {
    // All folders should already be included, so no need to expand anything.
    // Also, expanded path should be a descendant or self of the base path.

    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    BaseGenericFileTree tree1 = mock( BaseGenericFileTree.class );
    BaseGenericFileTree tree2 = mock( BaseGenericFileTree.class );

    BaseGenericFile file1 = mock( BaseGenericFile.class );
    doReturn( file1 ).when( tree1 ).getFile();
    doReturn( "/A" ).when( file1 ).getPath();

    BaseGenericFile file2 = mock( BaseGenericFile.class );
    doReturn( file2 ).when( tree2 ).getFile();
    doReturn( "/B" ).when( file2 ).getPath();

    // Owns the base path and the expanded path!
    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );
    doReturn( List.of( tree1, tree2 ) ).when( provider )
      .getRootTreesCore( any( GetTreeOptions.class ) );

    GetTreeOptions options = mock( GetTreeOptions.class );
    doReturn( mock( GenericFilePath.class ) ).when( options ).getBasePath();
    doReturn( List.of( mock( GenericFilePath.class ) ) ).when( options ).getExpandedPaths();
    doReturn( null ).when( options ).getMaxDepth();

    // Even if expanded max depth were not null!
    doReturn( 1 ).when( options ).getExpandedMaxDepth();

    List<IGenericFileTree> rootTrees = provider.getRootTrees( options );

    assertNotNull( rootTrees );
    assertEquals( 2, rootTrees.size() );
    assertSame( tree1, rootTrees.get( 0 ) );
    assertSame( tree2, rootTrees.get( 1 ) );

    // Expand path recursively calls getTree to expand each level.
    // There should be a single call corresponding to the above call.
    verify( provider, never() ).getTree( any( GetTreeOptions.class ) );
  }

  @Test
  public void testGetRootTreesDoesNotExpandPathIfExpandedPathNotOwned() throws OperationFailedException {
    // Expanded path should be a descendant or self of the base path, or is ignored.

    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    BaseGenericFileTree tree1 = mock( BaseGenericFileTree.class );
    BaseGenericFileTree tree2 = mock( BaseGenericFileTree.class );

    BaseGenericFile file1 = mock( BaseGenericFile.class );
    doReturn( file1 ).when( tree1 ).getFile();
    doReturn( "/A" ).when( file1 ).getPath();

    BaseGenericFile file2 = mock( BaseGenericFile.class );
    doReturn( file2 ).when( tree2 ).getFile();
    doReturn( "/B" ).when( file2 ).getPath();

    // Owns the base path and the expanded path!
    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );
    doReturn( List.of( tree1, tree2 ) ).when( provider )
      .getRootTreesCore( any( GetTreeOptions.class ) );

    // ---

    GetTreeOptions options = mock( GetTreeOptions.class );

    GenericFilePath basePathMock = mock( GenericFilePath.class );
    doReturn( basePathMock ).when( options ).getBasePath();

    GenericFilePath expandedPathMock = mock( GenericFilePath.class );
    doReturn( List.of( expandedPathMock ) ).when( options ).getExpandedPaths();

    // Owns the base path, but not the expanded path.
    doReturn( true ).when( provider ).owns( basePathMock );
    doReturn( false ).when( provider ).owns( expandedPathMock );

    doReturn( 1 ).when( options ).getMaxDepth();

    // ---

    List<IGenericFileTree> rootTrees = provider.getRootTrees( options );

    assertNotNull( rootTrees );
    assertEquals( 2, rootTrees.size() );
    assertSame( tree1, rootTrees.get( 0 ) );
    assertSame( tree2, rootTrees.get( 1 ) );

    // Expand path recursively calls getTree to expand each level.
    // There should be a single call corresponding to the above call.
    verify( provider, never() ).getTree( any( GetTreeOptions.class ) );
  }

  @Test
  public void testGetRootTreesDoesExpandPathIfExpandedPathIsDeeperThanMaxDepthAndZeroExpandedMaxDepth()
    throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    BaseGenericFileTree pvfsTree = getSampleVfsTreeOfDepth1();

    BaseGenericFileTree repoTree = getSampleRepositoryTreeOfDepth1();
    BaseGenericFileTree homeTree = getSampleRepositoryHomeTreeOfDepth1();

    // Owns the base path and the expanded path!
    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );

    assertNotNull( pvfsTree.getChildren() );
    doReturn( List.of(
      pvfsTree.getChildren().get( 0),
      pvfsTree.getChildren().get( 1),
      repoTree
    ) )
      .when( provider )
      .getRootTreesCore( any( GetTreeOptions.class ) );

    doReturn( homeTree )
      .when( provider )
      .getTreeCore( any( GetTreeOptions.class ) );

    // ---

    GetTreeOptions options = new GetTreeOptions();
    // Expanded path is at depth 1 below the / root, and thus within max depth of 1 (last element).
    // Will still need to request its children with the same max depth of 1.
    options.setExpandedPath( "/home" );
    options.setMaxDepth( 1 );
    options.setExpandedMaxDepth( 1 );

    // ---

    List<IGenericFileTree> rootTrees = provider.getRootTrees( options );

    assertNotNull( rootTrees );
    assertEquals( 3, rootTrees.size() );
    assertSame( pvfsTree.getChildren().get( 0 ), rootTrees.get( 0 ) );
    assertSame( pvfsTree.getChildren().get( 1 ), rootTrees.get( 1 ) );
    assertSame( repoTree, rootTrees.get( 2 ) );

    // Check was expanded.
    assertNotNull( repoTree.getChildren() );
    assertEquals( 2, repoTree.getChildren().size() );

    IGenericFileTree homeTree1 = repoTree.getChildren().get( 0 );
    assertEquals( homeTree.getFile().getPath(), homeTree1.getFile().getPath() );

    // Children were reused from the partial getTree of homeTree.
    assertNotNull( homeTree1.getChildren() );
    assertSame( homeTree.getChildren(), homeTree1.getChildren() );

    // Expand path recursively calls getTree to expand each level.
    // There should be a call corresponding to getting the children of /home.
    verify( provider, times( 1 ) ).getTree( any( GetTreeOptions.class ) );
  }

  @Test
  public void testGetRootTreesDoesExpandMultiplePaths() throws OperationFailedException {
    GenericFileProviderForTesting<IGenericFile> provider = spy( new GenericFileProviderForTesting<>() );

    BaseGenericFileTree pvfsTree = getSampleVfsTreeOfDepth1();
    BaseGenericFileTree demo1Tree = getSamplePvfsDemo1TreeOfDepth1();
    BaseGenericFileTree fooTree = getSamplePvfsDemo1FooTreeOfDepth1();

    BaseGenericFileTree repoTree = getSampleRepositoryTreeOfDepth1();
    BaseGenericFileTree homeTree = getSampleRepositoryHomeTreeOfDepth1();

    // Owns the base path and the expanded path!
    doReturn( true ).when( provider ).owns( any( GenericFilePath.class ) );

    assertNotNull( pvfsTree.getChildren() );
    doReturn( List.of(
      pvfsTree.getChildren().get( 0),
      pvfsTree.getChildren().get( 1),
      repoTree
    ) )
      .when( provider )
      .getRootTreesCore( any( GetTreeOptions.class ) );

    doReturn( homeTree, demo1Tree, fooTree )
      .when( provider )
      .getTreeCore( any( GetTreeOptions.class ) );

    // ---

    GetTreeOptions options = new GetTreeOptions();
    // Expanded path is at depth 1 below the / root, and thus within max depth of 1 (last element).
    // Will still need to request its children with the same max depth of 1.
    options.setExpandedPaths( GenericFilePath.parseManyRequired( List.of(
      "/home",
      "pvfs://demo1/foo"
    ) ) );
    options.setMaxDepth( 1 );
    options.setExpandedMaxDepth( 1 );

    // ---

    List<IGenericFileTree> rootTrees = provider.getRootTrees( options );

    assertNotNull( rootTrees );
    assertEquals( 3, rootTrees.size() );
    assertSame( pvfsTree.getChildren().get( 0 ), rootTrees.get( 0 ) );
    assertSame( pvfsTree.getChildren().get( 1 ), rootTrees.get( 1 ) );
    assertSame( repoTree, rootTrees.get( 2 ) );

    // Check /home was expanded.
    assertNotNull( repoTree.getChildren() );
    assertEquals( 2, repoTree.getChildren().size() );

    IGenericFileTree homeTree1 = repoTree.getChildren().get( 0 );
    assertEquals( homeTree.getFile().getPath(), homeTree1.getFile().getPath() );

    // Children were reused from the partial getTree of homeTree.
    assertNotNull( homeTree1.getChildren() );

    // Check pvfs://demo1/foo was expanded.
    IGenericFileTree demo1Tree1 = pvfsTree.getChildren().get( 0 );
    assertEquals( demo1Tree.getFile().getPath(), demo1Tree1.getFile().getPath() );

    // Children were reused from the partial getTree of demo1Tree.
    assertNotNull( demo1Tree1.getChildren() );
    assertSame( demo1Tree.getChildren(), demo1Tree1.getChildren() );

    IGenericFileTree fooTree1 = demo1Tree1.getChildren().get( 0 );
    assertNotNull( fooTree1 );
    assertEquals( fooTree.getFile().getPath(), fooTree1.getFile().getPath() );

    // Expand path recursively calls getTree to expand each level.
    // There should be calls corresponding to getting the children of:
    // - pvfs://demo1
    // - pvfs://demo1/foo
    // - /home
    verify( provider, times( 3 ) ).getTree( any( GetTreeOptions.class ) );
  }

  // endregion

  // endregion
}
