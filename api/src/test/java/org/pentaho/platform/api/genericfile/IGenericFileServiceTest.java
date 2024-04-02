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
package org.pentaho.platform.api.genericfile;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;

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
    public boolean doesFileExist( @NonNull GenericFilePath path ) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean createFolder( @NonNull GenericFilePath path ) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Tests for the {@link IGenericFileService#doesFileExist(String)} method.
   */
  @Nested
  class DoesFileExistTests {
    @Test
    void testValidStringPathIsAccepted() throws OperationFailedException {
      GenericFileServiceForTesting service = spy( new GenericFileServiceForTesting() );

      ArgumentCaptor<GenericFilePath> pathCaptor = ArgumentCaptor.forClass( GenericFilePath.class );

      doReturn( true ).when( service ).doesFileExist( any( GenericFilePath.class ) );

      assertTrue( service.doesFileExist( "/foo" ) );

      verify( service, times( 1 ) ).doesFileExist( pathCaptor.capture() );
      GenericFilePath path = pathCaptor.getValue();
      assertNotNull( path );
      assertEquals( "/foo", path.toString() );
    }

    @Test
    void testEmptyStringPathThrowsInvalidPathException() {
      GenericFileServiceForTesting service = new GenericFileServiceForTesting();

      assertThrows( InvalidPathException.class, () -> service.doesFileExist( "" ) );
    }

    @Test
    void testInvalidStringPathThrowsInvalidPathException() {
      GenericFileServiceForTesting service = new GenericFileServiceForTesting();

      assertThrows( InvalidPathException.class, () -> service.doesFileExist( "foo" ) );
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
