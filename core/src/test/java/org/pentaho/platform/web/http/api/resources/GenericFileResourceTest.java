/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/


package org.pentaho.platform.web.http.api.resources;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.genericfile.GetTreeOptions;
import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.model.IGenericFileContent;
import org.pentaho.platform.api.genericfile.exception.AccessControlException;
import org.pentaho.platform.api.genericfile.exception.ConflictException;
import org.pentaho.platform.api.genericfile.exception.InvalidOperationException;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.CreateFileOptions;
import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;

import jakarta.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GenericFileResourceTest {

  GenericFileResource genericFileResource;
  IGenericFileService mockFileService;

  private static final String ENCODED_SAMPLE_PATH = ":home:Ab`\t!@#$%^&()_+{}<>?'=-yZ~";
  private static final String EXPECTED_DECODED_SAMPLE_PATH = "/home/Ab`~!@#$%^&()_+{}<>?'=-yZ:";

  @Before
  public void setUp() {
    mockFileService = mock( IGenericFileService.class );
    genericFileResource = new GenericFileResource( mockFileService );
  }

  @Test
  public void testDecodePath() {
    Assert.assertEquals( "Unexpected path decoding:", EXPECTED_DECODED_SAMPLE_PATH,
      GenericFileResource.decodePath( ENCODED_SAMPLE_PATH ) );
  }

  @Test
  public void testDecodeRequestPathPreservesLiteralPlus() {
    Assert.assertEquals( "/home/admin/plus+name.txt",
      GenericFileResource.decodeRequestPath( ":home:admin:plus+name.txt" ) );
  }

  @Test
  public void testDecodeRequestPathDecodesEncodedPlus() {
    Assert.assertEquals( "/home/admin/plus+name.txt",
      GenericFileResource.decodeRequestPath( ":home:admin:plus%2Bname.txt" ) );
  }

  @Test
  public void testDecodeRequestPathDecodesEncodedSpace() {
    Assert.assertEquals( "/home/admin/space name.txt",
      GenericFileResource.decodeRequestPath( ":home:admin:space%20name.txt" ) );
  }

  @Test
  public void testDecodeRequestPathDecodesEncodedSpaceAndPreservesLiteralPlus() {
    Assert.assertEquals( "/home/admin/space and+plus.txt",
      GenericFileResource.decodeRequestPath( ":home:admin:space%20and+plus.txt" ) );
  }

  @Test
  public void testDecodeRequestPathDecodesEncodedSpaceAndEncodedPlus() {
    Assert.assertEquals( "/home/admin/space and+plus.txt",
      GenericFileResource.decodeRequestPath( ":home:admin:space%20and%2Bplus.txt" ) );
  }

  @Test
  public void testGetRootFileTreesPassesProvidersToService() throws Exception {
    IGenericFileTree repositoryTree = mockConnectionRootTree( "Repository" );
    IGenericFileTree localTree = mockConnectionRootTree( "Local" );

    when( mockFileService.getRootTrees( argThat( options -> options.includesProviderType( "vfs" )
      && !options.includesProviderType( "repository" ) ) ) )
      .thenReturn( Arrays.asList( repositoryTree, localTree ) );

    List<IGenericFileTree> result = genericFileResource.getRootFileTrees( 1, null, null, "FOLDERS", true,
      List.of( "VFS" ) );

    Assert.assertEquals( 2, result.size() );
    verify( mockFileService ).getRootTrees( argThat( options -> options.includesProviderType( "vfs" )
      && !options.includesProviderType( "repository" ) ) );
  }

  @Test
  public void testGetRootFileTreesDefaultsProvidersToAll() throws Exception {
    IGenericFileTree repositoryTree = mockConnectionRootTree( "Repository" );
    IGenericFileTree localTree = mockConnectionRootTree( "Local" );

    when( mockFileService.getRootTrees( argThat( GetTreeOptions::includesAllProviders ) ) )
      .thenReturn( Arrays.asList( repositoryTree, localTree ) );

    List<IGenericFileTree> result = genericFileResource.getRootFileTrees( 1, null, null, "FOLDERS", true, null );

    Assert.assertEquals( 2, result.size() );
    verify( mockFileService ).getRootTrees( argThat( GetTreeOptions::includesAllProviders ) );
  }

  @Test
  public void testGetFileTreePassesProvidersToService() throws Exception {
    IGenericFileTree repositoryTree = mockConnectionRootTree( "Repository", "pvfs://" );
    IGenericFileTree localTree = mockConnectionRootTree( "Local", "pvfs://" );
    IGenericFileTree pvfsTree = mockConnectionRootTree( "pvfs://" );
    IGenericFileTree rootTree = mock( IGenericFileTree.class );

    when( rootTree.getChildren() ).thenReturn( new ArrayList<>( Arrays.asList( pvfsTree ) ) );
    when( pvfsTree.getChildren() ).thenReturn( new ArrayList<>( Arrays.asList( repositoryTree, localTree ) ) );
    when( mockFileService.getTree( argThat( options -> options.includesProviderType( "vfs" )
      && !options.includesProviderType( "repository" ) ) ) ).thenReturn( rootTree );

    IGenericFileTree result = genericFileResource.getFileTree( 1, null, null, "FOLDERS", true, false,
      List.of( "VFS" ) );

    Assert.assertSame( rootTree, result );
    verify( mockFileService ).getTree( argThat( options -> options.includesProviderType( "vfs" )
      && !options.includesProviderType( "repository" ) ) );
  }

  @Test
  public void testGetFileTreeDefaultsProvidersToAll() throws Exception {
    IGenericFileTree repositoryTree = mockConnectionRootTree( "Repository", "pvfs://" );
    IGenericFileTree localTree = mockConnectionRootTree( "Local", "pvfs://" );
    IGenericFileTree pvfsTree = mockConnectionRootTree( "pvfs://" );
    IGenericFileTree rootTree = mock( IGenericFileTree.class );

    when( rootTree.getChildren() ).thenReturn( new ArrayList<>( Arrays.asList( pvfsTree ) ) );
    when( pvfsTree.getChildren() ).thenReturn( new ArrayList<>( Arrays.asList( repositoryTree, localTree ) ) );
    when( mockFileService.getTree( argThat( GetTreeOptions::includesAllProviders ) ) ).thenReturn( rootTree );

    IGenericFileTree result = genericFileResource.getFileTree( 1, null, null, "FOLDERS", true, false, null );

    Assert.assertSame( rootTree, result );
    verify( mockFileService ).getTree( argThat( GetTreeOptions::includesAllProviders ) );
  }

  // Tests for createFile endpoint

  @Test
  public void testCreateFileSuccess() throws Exception {
    // Arrange
    String path = "/test/file.txt";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    doNothing().when( mockFileService ).createFile( eq( path ), eq( content ), any( CreateFileOptions.class ) );

    // Act
    Response response = genericFileResource.createFile( path, false, content );

    // Assert
    Assert.assertEquals( Response.Status.CREATED.getStatusCode(), response.getStatus() );
  }

  @Test
  public void testCreateFilePreservesEncodedPlusInPath() throws Exception {
    String path = ":home:admin:plus%2Bname.txt";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    doNothing().when( mockFileService )
      .createFile( eq( "/home/admin/plus+name.txt" ), eq( content ), any( CreateFileOptions.class ) );

    Response response = genericFileResource.createFile( path, false, content );

    Assert.assertEquals( Response.Status.CREATED.getStatusCode(), response.getStatus() );
  }

  @Test
  public void testCreateFileDecodesEncodedSpaceAndPreservesPlusInPath() throws Exception {
    String path = ":home:admin:space%20and%2Bplus.txt";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    doNothing().when( mockFileService )
      .createFile( eq( "/home/admin/space and+plus.txt" ), eq( content ), any( CreateFileOptions.class ) );

    Response response = genericFileResource.createFile( path, false, content );

    Assert.assertEquals( Response.Status.CREATED.getStatusCode(), response.getStatus() );
  }

  @Test
  public void testDoesFolderExistSupportsRepositoryPath() throws Exception {
    String decodedPath = "/pvfs://Repository/home";
    when( mockFileService.doesFolderExist( decodedPath ) ).thenReturn( true );

    genericFileResource.doesFolderExist( encodeRequestPath( decodedPath ) );

    verify( mockFileService ).doesFolderExist( decodedPath );
  }

  @Test
  public void testCreateFolderSupportsRepositoryPath() throws Exception {
    String decodedPath = "/pvfs://Repository/home/new-folder";
    when( mockFileService.createFolder( decodedPath ) ).thenReturn( true );

    Response response = genericFileResource.createFolder( encodeRequestPath( decodedPath ) );

    Assert.assertEquals( Response.Status.CREATED.getStatusCode(), response.getStatus() );
    verify( mockFileService ).createFolder( decodedPath );
  }

  @Test
  public void testCreateFileSupportsRepositoryPath() throws Exception {
    String decodedPath = "/pvfs://Repository/home/new-file.txt";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    doNothing().when( mockFileService ).createFile( eq( decodedPath ), eq( content ), any( CreateFileOptions.class ) );

    Response response =
      genericFileResource.createFile( encodeRequestPath( decodedPath ), false, content );

    Assert.assertEquals( Response.Status.CREATED.getStatusCode(), response.getStatus() );
    verify( mockFileService ).createFile( eq( decodedPath ), eq( content ), any( CreateFileOptions.class ) );
  }

  @Test
  public void testGetFileContentSupportsRepositoryPath() throws Exception {
    String decodedPath = "/pvfs://Repository/home/report.txt";
    IGenericFileContent content = mock( IGenericFileContent.class );
    InputStream inputStream = new ByteArrayInputStream( "report".getBytes() );

    when( content.getInputStream() ).thenReturn( inputStream );
    when( content.getFileName() ).thenReturn( "report.txt" );
    when( content.getMimeType() ).thenReturn( "text/plain" );
    when( mockFileService.getFileContent( decodedPath, false ) ).thenReturn( content );

    Response response = genericFileResource.getFileContent( encodeRequestPath( decodedPath ) );

    Assert.assertEquals( Response.Status.OK.getStatusCode(), response.getStatus() );
    verify( mockFileService ).getFileContent( decodedPath, false );
  }

  @Test
  public void testGetFileSupportsRepositoryPath() throws Exception {
    String decodedPath = "/pvfs://Repository/home/report.txt";
    IGenericFile file = mock( IGenericFile.class );

    when( mockFileService.getFile( decodedPath ) ).thenReturn( file );

    IGenericFile result = genericFileResource.getFile( encodeRequestPath( decodedPath ) );

    Assert.assertSame( file, result );
    verify( mockFileService ).getFile( decodedPath );
  }

  @Test
  public void testCreateFileAlreadyExists() throws Exception {
    // Arrange
    String path = "/test/existing-file.txt";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    doThrow( new ConflictException( "File already exists. Choose Replace Files to Overwrite it." ) ).when(
      mockFileService ).createFile( eq( path ), eq( content ), any( CreateFileOptions.class ) );

    // Act & Assert - Expect WebApplicationException to be thrown
    try {
      genericFileResource.createFile( path, false, content );
      Assert.fail( "Expected WebApplicationException to be thrown" );
    } catch ( jakarta.ws.rs.WebApplicationException e ) {
      Assert.assertEquals( Response.Status.CONFLICT.getStatusCode(), e.getResponse().getStatus() );
      // The exception should be the cause
      Assert.assertEquals( "File already exists. Choose Replace Files to Overwrite it.", e.getCause().getMessage() );
    }
  }

  @Test
  public void testCreateFileInvalidPath() throws Exception {
    // Arrange
    String path = "invalid/path";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    doThrow( new InvalidPathException( "Invalid path" ) ).when( mockFileService )
      .createFile( eq( path ), eq( content ), any( CreateFileOptions.class ) );

    // Act
    Response response = genericFileResource.createFile( path, false, content );

    // Assert
    Assert.assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus() );
    Assert.assertEquals( "Invalid path", response.getEntity() );
  }

  @Test
  public void testCreateFileInvalidOperation() throws Exception {
    // Arrange
    String path = "test/file.txt";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    doThrow( new InvalidOperationException( "Invalid operation" ) ).when( mockFileService )
      .createFile( eq( path ), eq( content ), any( CreateFileOptions.class ) );

    // Act
    Response response = genericFileResource.createFile( path, false, content );

    // Assert
    Assert.assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus() );
    Assert.assertEquals( "Invalid operation", response.getEntity() );
  }

  @Test
  public void testCreateFileAccessDenied() throws Exception {
    // Arrange
    String path = "/restricted/file.txt";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    doThrow( new AccessControlException( "Access denied" ) ).when( mockFileService )
      .createFile( eq( path ), eq( content ), any( CreateFileOptions.class ) );

    // Act & Assert - Expect WebApplicationException to be thrown
    try {
      genericFileResource.createFile( path, false, content );
      Assert.fail( "Expected WebApplicationException to be thrown" );
    } catch ( jakarta.ws.rs.WebApplicationException e ) {
      Assert.assertEquals( Response.Status.FORBIDDEN.getStatusCode(), e.getResponse().getStatus() );
      // The exception should be the cause
      Assert.assertEquals( "Access denied", e.getCause().getMessage() );
    }
  }

  @Test
  public void testCreateFileOperationFailed() throws Exception {
    // Arrange
    String path = "/test/file.txt";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    doThrow( new OperationFailedException( "Operation failed" ) ).when( mockFileService )
      .createFile( eq( path ), eq( content ), any( CreateFileOptions.class ) );

    // Act & Assert - Expect WebApplicationException to be thrown
    try {
      genericFileResource.createFile( path, false, content );
      Assert.fail( "Expected WebApplicationException to be thrown" );
    } catch ( jakarta.ws.rs.WebApplicationException e ) {
      Assert.assertEquals( Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getResponse().getStatus() );
      // The exception should be the cause
      Assert.assertEquals( "Operation failed", e.getCause().getMessage() );
    }
  }

  private IGenericFileTree mockConnectionRootTree( String name ) {
    return mockConnectionRootTree( name, null );
  }

  private IGenericFileTree mockConnectionRootTree( String name, String parentPath ) {
    IGenericFileTree tree = mock( IGenericFileTree.class );
    IGenericFile file = mock( IGenericFile.class );

    when( tree.getFile() ).thenReturn( file );
    when( file.getName() ).thenReturn( name );
    when( file.getParentPath() ).thenReturn( parentPath );

    return tree;
  }

  private static String encodeRequestPath( String decodedPath ) {
    return decodedPath
      .replace( "~", "\u0000" )
      .replace( ":", "~" )
      .replace( "/", ":" )
      .replace( "\u0000", "\t" );
  }

}
