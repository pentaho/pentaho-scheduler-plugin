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
import edu.umd.cs.findbugs.annotations.Nullable;
import org.pentaho.platform.api.genericfile.IGenericFileProvider;
import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.exception.InvalidGenericFileProviderException;
import org.pentaho.platform.api.genericfile.exception.NotFoundException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;
import org.pentaho.platform.genericfile.model.BaseGenericFile;
import org.pentaho.platform.genericfile.model.BaseGenericFileTree;
import org.pentaho.platform.util.StringUtil;

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

    this.fileProviders = fileProviders;
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
  public IGenericFileTree getFolders( @Nullable Integer depth ) throws OperationFailedException {
    if ( fileProviders.size() <= 1 ) {
      return fileProviders.get( 0 ).getFolders( depth );
    }

    return getFoldersMultipleProviders( depth );
  }

  @NonNull
  private BaseGenericFileTree getFoldersMultipleProviders( @Nullable Integer depth ) throws OperationFailedException {
    BaseGenericFileTree rootTree = createMultipleProviderTreeRoot();

    OperationFailedException firstProviderException = null;
    for ( IGenericFileProvider<?> fileProvider : fileProviders ) {
      if ( fileProvider.isAvailable() ) {
        try {
          rootTree.addChild( fileProvider.getFolders( depth ) );
        } catch ( OperationFailedException e ) {
          if ( firstProviderException == null ) {
            firstProviderException = e;
          }

          // Continue, collecting providers that work. But still log failed ones, JIC.
          e.printStackTrace();
        }
      }
    }

    if ( firstProviderException != null && rootTree.getChildren().isEmpty() ) {
      // All providers failed. Opting to throw the error of the first failed one to the caller.
      throw firstProviderException;
    }

    return rootTree;
  }

  @NonNull
  private static BaseGenericFileTree createMultipleProviderTreeRoot() {
    BaseGenericFile entity = new BaseGenericFile();
    entity.setName( "root" );
    entity.setProvider( "combined" );
    entity.setType( IGenericFile.TYPE_FOLDER );

    return new BaseGenericFileTree( entity ) {
      @Override
      public String getProvider() {
        return "combined";
      }
    };
  }

  public boolean doesFolderExist( @NonNull String path ) throws OperationFailedException {
    Optional<IGenericFileProvider<?>> fileProvider = getOwnerFileProvider( path );

    return fileProvider.isPresent() && fileProvider.get().doesFolderExist( path );
  }

  public boolean createFolder( @NonNull String path ) throws OperationFailedException {
    Objects.requireNonNull( path );

    return getOwnerFileProvider( path )
      .orElseThrow( NotFoundException::new )
      .createFolder( path );
  }

  private Optional<IGenericFileProvider<?>> getOwnerFileProvider( @Nullable String path ) {
    if ( StringUtil.isEmpty( path ) ) {
      return Optional.empty();
    }

    return fileProviders.stream().filter( fileProvider -> fileProvider.owns( path ) ).findFirst();
  }
}
