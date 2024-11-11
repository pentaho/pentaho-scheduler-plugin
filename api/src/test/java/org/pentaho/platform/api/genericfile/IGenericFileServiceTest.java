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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.IGenericFileContentWrapper;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests the default methods of the {@link IGenericFileService} interface.
 */
class IGenericFileServiceTest {

  static class GenericFileServiceForTesting implements IGenericFileService {
    @Override
    public void clearTreeCache() {
    }

    @NonNull
    @Override
    public IGenericFileTree getTree( @NonNull GetTreeOptions options ) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean doesFolderExist( @NonNull GenericFilePath path ) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasAccess( @NonNull GenericFilePath path, @NonNull EnumSet<GenericFilePermission> permissions ) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean createFolder( @NonNull GenericFilePath path ) {
      throw new UnsupportedOperationException();
    }

    @Override public IGenericFileContentWrapper getFileContentWrapper( @NonNull GenericFilePath path ) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Tests for the {@link IGenericFileService#doesFolderExist(String)} method.
   */
  @Nested
  class DoesFileExistTests {
    @Test
    void testValidStringPathIsAccepted() throws OperationFailedException {
      GenericFileServiceForTesting service = spy( new GenericFileServiceForTesting() );

      ArgumentCaptor<GenericFilePath> pathCaptor = ArgumentCaptor.forClass( GenericFilePath.class );

      doReturn( true ).when( service ).doesFolderExist( any( GenericFilePath.class ) );

      assertTrue( service.doesFolderExist( "/foo" ) );

      verify( service, times( 1 ) ).doesFolderExist( pathCaptor.capture() );
      GenericFilePath path = pathCaptor.getValue();
      assertNotNull( path );
      assertEquals( "/foo", path.toString() );
    }

    @Test
    void testEmptyStringPathThrowsInvalidPathException() {
      GenericFileServiceForTesting service = new GenericFileServiceForTesting();

      assertThrows( InvalidPathException.class, () -> service.doesFolderExist( "" ) );
    }

    @Test
    void testInvalidStringPathThrowsInvalidPathException() {
      GenericFileServiceForTesting service = new GenericFileServiceForTesting();

      assertThrows( InvalidPathException.class, () -> service.doesFolderExist( "foo" ) );
    }
  }

  /**
   * Tests for the {@link IGenericFileService#createFolder(String)} method.
   */
  @Nested
  class CreateFolderTests {
    @Test
    void testValidStringPathIsAccepted() throws OperationFailedException {
      GenericFileServiceForTesting service = spy( new GenericFileServiceForTesting() );
      ArgumentCaptor<GenericFilePath> pathCaptor = ArgumentCaptor.forClass( GenericFilePath.class );

      doReturn( true ).when( service ).createFolder( any( GenericFilePath.class ) );

      assertTrue( service.createFolder( "/foo" ) );

      verify( service, times( 1 ) ).createFolder( pathCaptor.capture() );
      GenericFilePath path = pathCaptor.getValue();
      assertNotNull( path );
      assertEquals( "/foo", path.toString() );
    }

    @Test
    void testEmptyStringPathThrowsInvalidPathException() {
      GenericFileServiceForTesting service = new GenericFileServiceForTesting();

      assertThrows( InvalidPathException.class, () -> service.createFolder( "" ) );
    }

    @Test
    void testInvalidStringPathThrowsInvalidPathException() {
      GenericFileServiceForTesting service = new GenericFileServiceForTesting();

      assertThrows( InvalidPathException.class, () -> service.createFolder( "foo" ) );
    }
  }
}
