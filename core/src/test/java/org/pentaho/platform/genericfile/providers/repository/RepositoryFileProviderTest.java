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


package org.pentaho.platform.genericfile.providers.repository;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.Test;
import org.pentaho.platform.api.genericfile.GenericFilePath;
import org.pentaho.platform.api.genericfile.GetTreeOptions;
import org.pentaho.platform.api.genericfile.exception.NotFoundException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;
import org.pentaho.platform.api.genericfile.model.IGenericFolder;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.webservices.RepositoryFileDto;
import org.pentaho.platform.api.repository2.unified.webservices.RepositoryFileTreeDto;
import org.pentaho.platform.genericfile.messages.Messages;
import org.pentaho.platform.genericfile.providers.repository.model.RepositoryObject;
import org.pentaho.platform.util.RepositoryPathEncoder;
import org.pentaho.platform.web.http.api.resources.services.FileService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.pentaho.platform.genericfile.providers.repository.RepositoryFileProvider.ROOT_PATH;

/**
 * Tests for the {@link RepositoryFileProvider} class.
 */
public class RepositoryFileProviderTest {

  static final String ENCODED_ROOT_PATH = RepositoryPathEncoder.encodeRepositoryPath( ROOT_PATH );

  static final String ALL_FILTER = "*";

  // region Helpers and Sample Structures

  /**
   * A sample native repository tree structure.
   * <p>
   * /
   * /home
   * /public
   * /public/testFile1
   * /public/testFolder2
   */
  static class NativeDtoRepositoryScenario {
    @NonNull
    public final RepositoryFileTreeDto rootTree;
    @NonNull
    public final RepositoryFileDto rootFolder;
    @NonNull
    public final RepositoryFileTreeDto homeTree;
    @NonNull
    public final RepositoryFileDto homeFolder;
    @NonNull
    public final RepositoryFileTreeDto publicTree;
    @NonNull
    public final RepositoryFileDto publicFolder;
    @NonNull
    public final RepositoryFileDto testFile1;
    @NonNull
    public final RepositoryFileDto testFolder2;

    public NativeDtoRepositoryScenario() {
      rootFolder = createNativeFileDto( ROOT_PATH, "", true );
      rootTree = createNativeTreeDto( rootFolder );

      // ---
      // /home
      homeFolder = createNativeFileDto( "/home", "home", true );
      homeTree = createNativeTreeDto( homeFolder );

      // ---
      // /public
      publicFolder = createNativeFileDto( "/public", "public", true );
      publicTree = createNativeTreeDto( publicFolder );

      testFile1 = createSampleTestFile1();
      testFolder2 = createSampleTestFolder2();

      publicTree.setChildren( Arrays.asList(
        createNativeTreeDto( testFile1 ),
        createNativeTreeDto( testFolder2 )
      ) );

      rootTree.setChildren( Arrays.asList( homeTree, publicTree ) );
    }

    @NonNull
    private static RepositoryFileDto createSampleTestFile1() {
      RepositoryFileDto testFile1 = createNativeFileDto( "/public/testFile1", "testFile1", false );
      testFile1.setHidden( true );

      String ellapsedMilliseconds = "100";
      testFile1.setLastModifiedDate( ellapsedMilliseconds );

      testFile1.setId( "Test File 1 Id" );
      testFile1.setTitle( "Test File 1 title" );
      testFile1.setDescription( "Test File 1 description" );

      return testFile1;
    }

    @NonNull
    private static RepositoryFileDto createSampleTestFolder2() {
      RepositoryFileDto testFolder2 = createNativeFileDto( "/public/testFolder2", "testFolder2", true );

      String ellapsedMilliseconds = "200";
      testFolder2.setLastModifiedDate( ellapsedMilliseconds );

      testFolder2.setId( "Test Folder 2 Id" );
      testFolder2.setTitle( "Test Folder 2 title" );
      testFolder2.setDescription( "Test Folder 2 description" );

      return testFolder2;
    }
  }

  @NonNull
  static RepositoryFileTreeDto createNativeTreeDto( @NonNull RepositoryFileDto nativeFile ) {
    RepositoryFileTreeDto nativeTree = new RepositoryFileTreeDto();
    nativeTree.setFile( nativeFile );
    return nativeTree;
  }

  @NonNull
  private static RepositoryFileDto createNativeFileDto( String path, String name, boolean isFolder ) {
    RepositoryFileDto nativeFile = new RepositoryFileDto();
    nativeFile.setName( name );
    nativeFile.setPath( path );
    nativeFile.setFolder( isFolder );

    String numberOfMilliseconds = "0";
    nativeFile.setCreatedDate( numberOfMilliseconds );

    return nativeFile;
  }

  @NonNull
  private static org.pentaho.platform.api.repository2.unified.RepositoryFile createNativeFile( String path,
                                                                                               String name,
                                                                                               boolean isFolder ) {
    Date createdDate = new Date( 100 );
    Date lastModeDate = new Date( 200 );
    Date lockDate = new Date();
    return new org.pentaho.platform.api.repository2.unified.RepositoryFile(
      "12345", name, isFolder, false, false, false, "versionId", path, createdDate,
      lastModeDate,
      false, "lockOwner", "lockMessage", lockDate, "en_US", name + " title", name + " description",
      null, null, 4096, name + "creatorId", null
    );
  }

  /**
   * Represents the result structure of a root tree operation for {@link NativeDtoRepositoryScenario}.
   */
  static class RepositoryValidatedScenario {
    @NonNull
    public final IGenericFileTree rootTree;
    @NonNull
    public final IGenericFolder rootFolder;
    @NonNull
    public final IGenericFileTree homeTree;
    @NonNull
    public final IGenericFolder homeFolder;
    @NonNull
    public final IGenericFileTree publicTree;
    @NonNull
    public final IGenericFolder publicFolder;
    @NonNull
    public final IGenericFile testFile1;
    @NonNull
    public final IGenericFolder testFolder2;

    public RepositoryValidatedScenario( @NonNull IGenericFileTree tree ) {
      assertNotNull( tree );
      rootTree = tree;
      rootFolder = assertRootFolder( tree.getFile() );

      assertEquals( ROOT_PATH, rootFolder.getPath() );

      // Check that the children of the home subtree are now part of the root tree.
      List<IGenericFileTree> rootChildren = tree.getChildren();
      assertNotNull( rootChildren );
      assertEquals( 2, rootChildren.size() );

      // ---
      // /home
      assertNotNull( rootChildren.get( 0 ) );
      homeTree = rootChildren.get( 0 );
      homeFolder = assertGenericFolder( homeTree.getFile() );
      assertEquals( "/home", homeFolder.getPath() );
      assertEquals( "home", homeFolder.getName() );
      assertEquals( ROOT_PATH, homeFolder.getParentPath() );

      // ---
      // /public

      assertNotNull( rootChildren.get( 1 ) );
      publicTree = rootChildren.get( 1 );
      publicFolder = assertPublicTree( publicTree );

      List<IGenericFileTree> publicChildren = publicTree.getChildren();
      assertNotNull( publicChildren );
      assertEquals( 2, publicChildren.size() );

      IGenericFileTree testTree1 = publicChildren.get( 0 );
      assertNotNull( testTree1 );
      assertNotNull( testTree1.getFile() );
      testFile1 = testTree1.getFile();

      IGenericFileTree testTree2 = publicChildren.get( 1 );
      assertNotNull( testTree2 );
      assertNotNull( testTree2.getFile() );
      testFolder2 = assertGenericFolder( testTree2.getFile() );
    }
  }

  @NonNull
  RepositoryValidatedScenario assertRepositoryTree( IGenericFileTree tree ) {
    return new RepositoryValidatedScenario( tree );
  }

  private static @NonNull IGenericFolder assertPublicTree( IGenericFileTree publicTree ) {
    IGenericFolder publicFolder = assertGenericFolder( publicTree.getFile() );

    assertEquals( "/public", publicFolder.getPath() );
    assertEquals( "public", publicFolder.getName() );
    assertEquals( ROOT_PATH, publicFolder.getParentPath() );
    assertRegularCapabilities( publicFolder );

    return publicFolder;
  }

  @NonNull
  static IGenericFolder assertGenericFolder( IGenericFile file ) {
    assertNotNull( file );
    assertTrue( file.isFolder() );
    assertTrue( file instanceof IGenericFolder );
    return (IGenericFolder) file;
  }

  @NonNull
  static IGenericFolder assertRootFolder( IGenericFile file ) {
    IGenericFolder folder = assertGenericFolder( file );

    assertEquals( ROOT_PATH, file.getPath() );
    assertNull( file.getParentPath() );
    assertEquals( Messages.getString( "GenericFileRepository.REPOSITORY_FOLDER_DISPLAY" ), file.getName() );

    assertFalse( folder.isCanDelete() );
    assertFalse( folder.isCanEdit() );
    assertFalse( folder.isCanAddChildren() );
    return folder;
  }

  @NonNull
  static void assertRegularCapabilities( IGenericFile file ) {
    assertTrue( file.isCanDelete() );
    assertTrue( file.isCanEdit() );
    if ( file.isFolder() ) {
      IGenericFolder folder = (IGenericFolder) file;
      assertTrue( folder.isCanAddChildren() );
    }
  }
  // endregion

  // region getTree
  @Test( expected = NotFoundException.class )
  public void testGetTreeThrowsNotFoundExceptionIfBasePathNotOwned() throws OperationFailedException {
    RepositoryFileProvider repositoryProvider =
      new RepositoryFileProvider( mock( IUnifiedRepository.class ), mock( FileService.class ) );

    GetTreeOptions options = new GetTreeOptions();
    options.setBasePath( "scheme://path" );
    options.setMaxDepth( 1 );

    repositoryProvider.getTree( options );
  }

  @Test
  public void testGetTreeDelegatesToFileServiceDoGetTree() throws OperationFailedException {

    NativeDtoRepositoryScenario scenario = new NativeDtoRepositoryScenario();

    IUnifiedRepository repositoryMock = mock( IUnifiedRepository.class );

    FileService fileServiceMock = mock( FileService.class );
    doReturn( scenario.rootTree )
      .when( fileServiceMock )
      .doGetTree( any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean() );

    RepositoryFileProvider repositoryProvider = new RepositoryFileProvider( repositoryMock, fileServiceMock );

    GetTreeOptions options = new GetTreeOptions();
    options.setBasePath( ROOT_PATH );
    options.setMaxDepth( 2 );

    IGenericFileTree tree = repositoryProvider.getTree( options );

    assertRepositoryTree( tree );

    verify( fileServiceMock, times( 1 ) )
      .doGetTree( ENCODED_ROOT_PATH, 2, ALL_FILTER, false, false, false );
  }

  @Test
  public void testGetTreeDefaultsBasePathToRepositoryRoot() throws OperationFailedException {
    NativeDtoRepositoryScenario scenario = new NativeDtoRepositoryScenario();

    FileService fileServiceMock = mock( FileService.class );
    doReturn( scenario.rootTree )
      .when( fileServiceMock )
      .doGetTree( any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean() );

    RepositoryFileProvider repositoryProvider =
      new RepositoryFileProvider( mock( IUnifiedRepository.class ), fileServiceMock );

    GetTreeOptions options = new GetTreeOptions();
    options.setBasePath( (GenericFilePath) null );
    options.setMaxDepth( 1 );

    repositoryProvider.getTree( options );

    verify( fileServiceMock, times( 1 ) )
      .doGetTree( eq( ENCODED_ROOT_PATH ), anyInt(), anyString(), anyBoolean(), anyBoolean(), anyBoolean() );
  }

  @Test
  public void testGetTreeRespectsNullChildrenList() throws OperationFailedException {
    NativeDtoRepositoryScenario scenario = new NativeDtoRepositoryScenario();

    // Check initial structure has null children list for /home.
    assertNull( scenario.homeTree.getChildren() );

    FileService fileServiceMock = mock( FileService.class );
    doReturn( scenario.rootTree )
      .when( fileServiceMock )
      .doGetTree( any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean() );

    RepositoryFileProvider repositoryProvider =
      new RepositoryFileProvider( mock( IUnifiedRepository.class ), fileServiceMock );

    GetTreeOptions options = new GetTreeOptions();
    options.setBasePath( ROOT_PATH );
    options.setMaxDepth( 1 );

    IGenericFileTree tree = repositoryProvider.getTree( options );

    RepositoryValidatedScenario validatedScenario = assertRepositoryTree( tree );
    assertNull( validatedScenario.homeTree.getChildren() );
  }

  @Test
  public void testGetTreeRespectsEmptyChildrenList() throws OperationFailedException {
    NativeDtoRepositoryScenario scenario = new NativeDtoRepositoryScenario();

    // Set empty list to children of /home.
    scenario.homeTree.setChildren( Collections.emptyList() );

    FileService fileServiceMock = mock( FileService.class );
    doReturn( scenario.rootTree )
      .when( fileServiceMock )
      .doGetTree( any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean() );

    RepositoryFileProvider repositoryProvider =
      new RepositoryFileProvider( mock( IUnifiedRepository.class ), fileServiceMock );

    GetTreeOptions options = new GetTreeOptions();
    options.setBasePath( ROOT_PATH );
    options.setMaxDepth( 1 );

    IGenericFileTree tree = repositoryProvider.getTree( options );

    RepositoryValidatedScenario validatedScenario = assertRepositoryTree( tree );
    assertNotNull( validatedScenario.homeTree.getChildren() );
    assertTrue( validatedScenario.homeTree.getChildren().isEmpty() );
  }

  @Test
  public void testGetTreeTestFile1HasExpectedProperties() throws OperationFailedException {
    NativeDtoRepositoryScenario scenario = new NativeDtoRepositoryScenario();

    FileService fileServiceMock = mock( FileService.class );
    doReturn( scenario.rootTree )
      .when( fileServiceMock )
      .doGetTree( any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean() );

    RepositoryFileProvider repositoryProvider =
      new RepositoryFileProvider( mock( IUnifiedRepository.class ), fileServiceMock );

    GetTreeOptions options = new GetTreeOptions();
    options.setBasePath( ROOT_PATH );
    options.setMaxDepth( 1 );

    IGenericFileTree tree = repositoryProvider.getTree( options );

    RepositoryValidatedScenario validatedScenario = assertRepositoryTree( tree );

    RepositoryObject testFile1 = (RepositoryObject) validatedScenario.testFile1;
    assertEquals( "/public/testFile1", testFile1.getPath() );
    assertEquals( "/public", testFile1.getParentPath() );
    assertEquals( "testFile1", testFile1.getName() );
    assertEquals( "Test File 1 Id", testFile1.getObjectId() );
    assertEquals( "Test File 1 title", testFile1.getTitle() );
    assertEquals( "Test File 1 description", testFile1.getDescription() );
    assertTrue( testFile1.isHidden() );
    assertEquals( new Date( 100 ), testFile1.getModifiedDate() );
  }

  @Test
  public void testGetTreeTestFolder2HasExpectedProperties() throws OperationFailedException {
    NativeDtoRepositoryScenario scenario = new NativeDtoRepositoryScenario();

    FileService fileServiceMock = mock( FileService.class );
    doReturn( scenario.rootTree )
      .when( fileServiceMock )
      .doGetTree( any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean() );

    RepositoryFileProvider repositoryProvider =
      new RepositoryFileProvider( mock( IUnifiedRepository.class ), fileServiceMock );

    GetTreeOptions options = new GetTreeOptions();
    options.setBasePath( ROOT_PATH );
    options.setMaxDepth( 1 );

    IGenericFileTree tree = repositoryProvider.getTree( options );

    RepositoryValidatedScenario validatedScenario = assertRepositoryTree( tree );

    RepositoryObject testFolder2 = (RepositoryObject) validatedScenario.testFolder2;
    assertEquals( "/public/testFolder2", testFolder2.getPath() );
    assertEquals( "/public", testFolder2.getParentPath() );
    assertEquals( "testFolder2", testFolder2.getName() );
    assertEquals( "Test Folder 2 Id", testFolder2.getObjectId() );
    assertEquals( "Test Folder 2 title", testFolder2.getTitle() );
    assertEquals( "Test Folder 2 description", testFolder2.getDescription() );
    assertFalse( testFolder2.isHidden() );
    assertEquals( new Date( 200 ), testFolder2.getModifiedDate() );
  }

  @Test
  public void testGetSubTreeRootNodeHasExpectedProperties() throws OperationFailedException {

    NativeDtoRepositoryScenario scenario = new NativeDtoRepositoryScenario();

    IUnifiedRepository repositoryMock = mock( IUnifiedRepository.class );

    FileService fileServiceMock = mock( FileService.class );
    doReturn( scenario.publicTree )
      .when( fileServiceMock )
      .doGetTree( any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean() );

    RepositoryFileProvider repositoryProvider = new RepositoryFileProvider( repositoryMock, fileServiceMock );

    GetTreeOptions options = new GetTreeOptions();
    options.setBasePath( scenario.publicFolder.getPath() );
    options.setMaxDepth( 1 );

    IGenericFileTree tree = repositoryProvider.getTree( options );

    assertPublicTree( tree );
  }
  // endregion

  // region getFile
  @Test( expected = NotFoundException.class )
  public void testGetFileThrowsNotFoundExceptionIfPathNotOwned() throws OperationFailedException {
    RepositoryFileProvider repositoryProvider =
      new RepositoryFileProvider( mock( IUnifiedRepository.class ), mock( FileService.class ) );

    repositoryProvider.getFile( GenericFilePath.parse( "scheme://path" ) );
  }

  @Test( expected = NotFoundException.class )
  public void testGetFileThrowsNotFoundExceptionIfPathNotFound() throws OperationFailedException {

    IUnifiedRepository repositoryMock = mock( IUnifiedRepository.class );
    doReturn( null )
      .when( repositoryMock )
      .getFile( "/path" );

    RepositoryFileProvider repositoryProvider =
      new RepositoryFileProvider( repositoryMock, mock( FileService.class ) );

    repositoryProvider.getFile( GenericFilePath.parse( "/path" ) );
  }

  @Test
  public void testGetFileRootHasExpectedProperties() throws OperationFailedException {

    org.pentaho.platform.api.repository2.unified.RepositoryFile nativeFile = createNativeFile( ROOT_PATH, "", true );

    IUnifiedRepository repositoryMock = mock( IUnifiedRepository.class );
    doReturn( nativeFile )
      .when( repositoryMock )
      .getFile( ROOT_PATH );

    RepositoryFileProvider repositoryProvider = new RepositoryFileProvider( repositoryMock, mock( FileService.class ) );

    IGenericFile file = repositoryProvider.getFile( GenericFilePath.parse( ROOT_PATH ) );

    assertRootFolder( file );
  }

  @Test
  public void testGetFileRegularHasExpectedProperties() throws OperationFailedException {

    org.pentaho.platform.api.repository2.unified.RepositoryFile nativeFile =
      createNativeFile( "/public/testFile1", "testFile1", false );

    IUnifiedRepository repositoryMock = mock( IUnifiedRepository.class );
    doReturn( nativeFile )
      .when( repositoryMock )
      .getFile( nativeFile.getPath() );

    RepositoryFileProvider repositoryProvider = new RepositoryFileProvider( repositoryMock, mock( FileService.class ) );

    IGenericFile file = repositoryProvider.getFile( GenericFilePath.parse( nativeFile.getPath() ) );

    assertEquals( "/public/testFile1", file.getPath() );
    assertEquals( "/public", file.getParentPath() );
    assertEquals( "testFile1", file.getName() );
    assertEquals( "testFile1 title", file.getTitle() );
    assertEquals( "testFile1 description", file.getDescription() );

    assertEquals( new Date( 200 ), file.getModifiedDate() );

    assertRegularCapabilities( file );
  }
  // endregion
}
