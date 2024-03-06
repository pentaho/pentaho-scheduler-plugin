/*!
 *
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
 *
 * Copyright (c) 2023 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.genericfile;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.pentaho.platform.api.genericfile.GenericFilePath;
import org.pentaho.platform.api.genericfile.GetTreeOptions;
import org.pentaho.platform.api.genericfile.IGenericFileProvider;
import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.exception.InvalidGenericFileProviderException;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;
import org.pentaho.platform.api.genericfile.exception.NotFoundException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;
import org.pentaho.platform.genericfile.model.BaseGenericFile;
import org.pentaho.platform.genericfile.model.BaseGenericFileTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DefaultGenericFileService implements IGenericFileService {

  private final List<IGenericFileProvider<?>> fileProviders;

  public DefaultGenericFileService( @NonNull List<IGenericFileProvider<?>> fileProviders )
    throws InvalidGenericFileProviderException {

    Objects.requireNonNull( fileProviders );

    if ( fileProviders.isEmpty() ) {
      throw new InvalidGenericFileProviderException();
    }

    // Create defensive copy to disallow external modification (and be sure there's always >= 1 provider).
    this.fileProviders = new ArrayList<>( fileProviders );
  }

  public void clearFolderCache() {
    for ( IGenericFileProvider<?> fileProvider : fileProviders ) {
      try {
        fileProvider.clearFolderCache();
      } catch ( OperationFailedException e ) {
        // Clear as many as possible. Still, log each failure.
        e.printStackTrace();
      }
    }
  }

  @NonNull
  public IGenericFileTree getFolderTree( @NonNull GetTreeOptions options ) throws OperationFailedException {

    Objects.requireNonNull( options );

    if ( isSingleProviderMode() ) {
      return fileProviders.get( 0 ).getFolderTree( options );
    }

    return options.getBasePath().isNull()
      ? getFolderTreeFromRoot( options )
      : getFolderSubTree( options.getBasePath(), options );
  }

  private boolean isSingleProviderMode() {
    return fileProviders.size() == 1;
  }

  @NonNull
  private IGenericFileTree getFolderTreeFromRoot( @NonNull GetTreeOptions options )
    throws OperationFailedException {

    BaseGenericFileTree rootTree = createMultipleProviderTreeRoot();

    OperationFailedException firstProviderException = null;
    for ( IGenericFileProvider<?> fileProvider : fileProviders ) {
      try {
        rootTree.addChild( fileProvider.getFolderTree( options ) );
      } catch ( OperationFailedException e ) {
        if ( firstProviderException == null ) {
          firstProviderException = e;
        }

        // Continue, collecting providers that work. But still log failed ones, JIC.
        e.printStackTrace();
      }
    }

    if ( firstProviderException != null
      && ( rootTree.getChildren() == null || rootTree.getChildren().isEmpty() ) ) {
      // All providers failed. Opting to throw the error of the first failed one to the caller.
      throw firstProviderException;
    }

    return rootTree;
  }

  @NonNull
  private static BaseGenericFileTree createMultipleProviderTreeRoot() {
    // Note that the absolute root has a null path.
    BaseGenericFile entity = new BaseGenericFile();
    entity.setName( "root" );
    entity.setProvider( "combined" );
    entity.setType( IGenericFile.TYPE_FOLDER );

    return new BaseGenericFileTree( entity ) {
      @NonNull
      @Override
      public String getProvider() {
        return "combined";
      }
    };
  }

  @NonNull
  private IGenericFileTree getFolderSubTree( @NonNull GenericFilePath basePath, @NonNull GetTreeOptions options )
    throws OperationFailedException {

    return getOwnerFileProvider( basePath )
      .orElseThrow( () -> new NotFoundException( String.format( "Base path not found '%s'.", basePath ) ) )
      .getFolderTree( options );
  }

  public boolean doesFileExist( @NonNull GenericFilePath path ) throws OperationFailedException {
    if ( path.isNull() ) {
      // Only multiple provider mode has the null / uber path.
      if ( isSingleProviderMode() ) {
        throw new InvalidPathException();
      }

      return true;
    }

    Optional<IGenericFileProvider<?>> fileProvider = getOwnerFileProvider( path );

    return fileProvider.isPresent() && fileProvider.get().doesFileExist( path );
  }

  public boolean createFolder( @NonNull GenericFilePath path ) throws OperationFailedException {
    if ( path.isNull() ) {
      // Only multiple provider mode has the null / uber path.
      if ( isSingleProviderMode() ) {
        throw new InvalidPathException();
      }

      // Already exists.
      return false;
    }

    return getOwnerFileProvider( path )
      .orElseThrow( NotFoundException::new )
      .createFolder( path );
  }

  private Optional<IGenericFileProvider<?>> getOwnerFileProvider( @NonNull GenericFilePath path ) {
    if ( path.isNull() ) {
      return Optional.empty();
    }

    return fileProviders.stream()
      .filter( fileProvider -> fileProvider.owns( path ) )
      .findFirst();
  }
}
