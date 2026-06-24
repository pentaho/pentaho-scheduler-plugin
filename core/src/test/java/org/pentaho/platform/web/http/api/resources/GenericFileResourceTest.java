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

import jakarta.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.exception.AccessControlException;
import org.pentaho.platform.api.genericfile.exception.ConflictException;
import org.pentaho.platform.api.genericfile.exception.InvalidOperationException;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.CreateFileOptions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

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
    doNothing().when( mockFileService ).createFile( eq( path ), eq( content ), any( CreateFileOptions.class ) );

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
    doThrow( new ConflictException( "File already exists. Choose Replace Files to Overwrite it." ) ).when(
      mockFileService ).createFile( eq( path ), eq( content ), any( CreateFileOptions.class ) );

    // Act & Assert - Expect WebApplicationException to be thrown
    try {
      genericFileResource.createFile( path, false, content );
      Assert.fail( "Expected WebApplicationException to be thrown" );
    } catch ( jakarta.ws.rs.WebApplicationException e ) {
      assertEquals( Response.Status.CONFLICT.getStatusCode(), e.getResponse().getStatus() );
      // The exception should be the cause
      assertEquals( "File already exists. Choose Replace Files to Overwrite it.", e.getCause().getMessage() );
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
    assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus() );
    assertEquals( "Invalid path", response.getEntity() );
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
    assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus() );
    assertEquals( "Invalid operation", response.getEntity() );
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
    doThrow( new OperationFailedException( "Operation failed" ) ).when( mockFileService )
      .createFile( eq( path ), eq( content ), any( CreateFileOptions.class ) );

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

}
