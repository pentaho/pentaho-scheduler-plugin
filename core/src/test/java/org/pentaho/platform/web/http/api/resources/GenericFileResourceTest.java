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

package org.pentaho.platform.web.http.api.resources;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.exception.AccessControlException;
import org.pentaho.platform.api.genericfile.exception.InvalidOperationException;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.CreateFileOptions;
import org.pentaho.platform.scheduler2.messsages.Messages;

import jakarta.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
      genericFileResource.decodePath( ENCODED_SAMPLE_PATH ) );
  }

  // Tests for createFile endpoint

  @Test
  public void testCreateFileSuccess() throws Exception {
    // Arrange
    String path = "/test/file.txt";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    when( mockFileService.createFile( eq( path ), eq( content ), any( CreateFileOptions.class ) ) ).thenReturn( true );

    // Act
    Response response = genericFileResource.createFile( path, false, content );

    // Assert
    assertEquals( Response.Status.CREATED.getStatusCode(), response.getStatus() );
  }

  @Test
  public void testCreateFileAlreadyExists() throws Exception {
    // Arrange
    String path = "/test/existing-file.txt";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    when( mockFileService.createFile( eq( path ), eq( content ), any( CreateFileOptions.class ) ) ).thenReturn( false );

    // Mock Messages static method
    try ( MockedStatic<Messages> messagesStatic = mockStatic( Messages.class ) ) {
      messagesStatic.when( () -> Messages.getString( "GenericFileResource.FILE_EXISTS_OVERWRITE" ) )
        .thenReturn( "File already exists. Choose Replace Files to Overwrite it." );

      // Act & Assert - Expect WebApplicationException to be thrown
      try {
        genericFileResource.createFile( path, false, content );
        Assert.fail( "Expected WebApplicationException to be thrown" );
      } catch ( jakarta.ws.rs.WebApplicationException e ) {
        assertEquals( Response.Status.CONFLICT.getStatusCode(), e.getResponse().getStatus() );
        assertEquals( "File already exists. Choose Replace Files to Overwrite it.", e.getMessage() );
      }
    }
  }

  @Test
  public void testCreateFileInvalidPath() throws Exception {
    // Arrange
    String path = "invalid/path";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    when( mockFileService.createFile( eq( path ), eq( content ), any( CreateFileOptions.class ) ) )
      .thenThrow( new InvalidPathException( "Invalid path" ) );

    // Act
    Response response = genericFileResource.createFile( path, false, content );

    // Assert
    assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus() );
    assertEquals( "Invalid path", response.getEntity() );
  }

  @Test
  public void testCreateFileInvalidOperation() throws Exception {
    // Arrange
    String path = "test/file.txt";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    when( mockFileService.createFile( eq( path ), eq( content ), any( CreateFileOptions.class ) ) )
      .thenThrow( new InvalidOperationException( "Invalid operation" ) );

    // Act
    Response response = genericFileResource.createFile( path, false, content );

    // Assert
    assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus() );
    assertEquals( "Invalid operation", response.getEntity() );
  }

  @Test
  public void testCreateFileAccessDenied() throws Exception {
    // Arrange
    String path = "/restricted/file.txt";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    when( mockFileService.createFile( eq( path ), eq( content ), any( CreateFileOptions.class ) ) )
      .thenThrow( new AccessControlException( "Access denied" ) );

    // Act & Assert - Expect WebApplicationException to be thrown
    try {
      genericFileResource.createFile( path, false, content );
      Assert.fail( "Expected WebApplicationException to be thrown" );
    } catch ( jakarta.ws.rs.WebApplicationException e ) {
      assertEquals( Response.Status.FORBIDDEN.getStatusCode(), e.getResponse().getStatus() );
      // The exception should be the cause
      assertEquals( "Access denied", e.getCause().getMessage() );
    }
  }

  @Test
  public void testCreateFileOperationFailed() throws Exception {
    // Arrange
    String path = "/test/file.txt";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    when( mockFileService.createFile( eq( path ), eq( content ), any( CreateFileOptions.class ) ) )
      .thenThrow( new OperationFailedException( "Operation failed" ) );

    // Act & Assert - Expect WebApplicationException to be thrown
    try {
      genericFileResource.createFile( path, false, content );
      Assert.fail( "Expected WebApplicationException to be thrown" );
    } catch ( jakarta.ws.rs.WebApplicationException e ) {
      assertEquals( Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getResponse().getStatus() );
      // The exception should be the cause
      assertEquals( "Operation failed", e.getCause().getMessage() );
    }
  }

  // Tests for @Encoded + URI-based percent-decode behavior in createFile

  @Test
  public void testCreateFile_PercentEncodedPathIsDecoded() throws Exception {
    // Simulate a percent-encoded path as it arrives with @Encoded (JAX-RS skips auto-decoding)
    String encodedPath = "%3Ahome%3Aadmin%3Amy%20file.ktr";
    // decodePath converts internal format: : -> /, ~ -> :, \t -> ~
    String expectedNaturalPath = "/home/admin/my file.ktr";

    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    when( mockFileService.createFile( eq( expectedNaturalPath ), eq( content ), any( CreateFileOptions.class ) ) )
      .thenReturn( true );

    Response response = genericFileResource.createFile( encodedPath, false, content );

    assertEquals( Response.Status.CREATED.getStatusCode(), response.getStatus() );
  }

  @Test
  public void testCreateFile_MalformedPercentEncoding_Returns400() {
    // A bare '%' not followed by two hex digits triggers URISyntaxException from URI parsing
    String malformedPath = "test%ZZinvalid";
    InputStream content = new ByteArrayInputStream( "test content".getBytes() );

    Response response = genericFileResource.createFile( malformedPath, false, content );

    assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus() );
    String entity = (String) response.getEntity();
    Assert.assertTrue( entity.startsWith( "Invalid file path encoding:" ) );
  }

  @Test
  public void testCreateFile_LiteralPlusPreservedInPath() throws Exception {
    // java.net.URI.getPath() does NOT convert '+' to space (unlike URLDecoder).
    // '+' is a valid literal character in URL path segments and file names.
    String pathWithPlus = "%3Ahome%3Aadmin%3Amy+file.ktr";
    String expectedNaturalPath = "/home/admin/my+file.ktr";

    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    when( mockFileService.createFile( eq( expectedNaturalPath ), eq( content ), any( CreateFileOptions.class ) ) )
      .thenReturn( true );

    Response response = genericFileResource.createFile( pathWithPlus, false, content );

    assertEquals( Response.Status.CREATED.getStatusCode(), response.getStatus() );
  }

  @Test
  public void testCreateFile_EncodedSpecialCharsInFileName() throws Exception {
    // File name with special chars: "report (final).ktr" → parentheses encoded as %28 %29
    String encodedPath = "%3Ahome%3Aadmin%3Areport%20%28final%29.ktr";
    String expectedNaturalPath = "/home/admin/report (final).ktr";

    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    when( mockFileService.createFile( eq( expectedNaturalPath ), eq( content ), any( CreateFileOptions.class ) ) )
      .thenReturn( true );

    Response response = genericFileResource.createFile( encodedPath, false, content );

    assertEquals( Response.Status.CREATED.getStatusCode(), response.getStatus() );
  }

  @Test
  public void testCreateFile_VfsEncodedPath() throws Exception {
    // VFS internal path: pvfs~::Local:folder:file.ktr → percent-encoded by the client
    String encodedPath = "pvfs~%3A%3ALocal%3Afolder%3Afile.ktr";
    // After URI.getPath() decode: pvfs~::Local:folder:file.ktr
    // After decodePath: pvfs://Local/folder/file.ktr  (~ → :, : → /)
    String decodedInternalPath = "pvfs~::Local:folder:file.ktr";
    String expectedNaturalPath = genericFileResource.decodePath( decodedInternalPath );

    InputStream content = new ByteArrayInputStream( "test content".getBytes() );
    when( mockFileService.createFile( eq( expectedNaturalPath ), eq( content ), any( CreateFileOptions.class ) ) )
      .thenReturn( true );

    Response response = genericFileResource.createFile( encodedPath, false, content );

    assertEquals( Response.Status.CREATED.getStatusCode(), response.getStatus() );
  }

}
