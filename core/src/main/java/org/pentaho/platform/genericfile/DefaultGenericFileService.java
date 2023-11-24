/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2023 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.platform.genericfile;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.pentaho.platform.api.genericfile.IGenericFileProvider;
import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.exception.InvalidGenericFileProviderException;
import org.pentaho.platform.api.genericfile.model.BaseEntity;
import org.pentaho.platform.api.genericfile.model.BaseFileTree;
import org.pentaho.platform.api.genericfile.model.IGenericTree;
import org.pentaho.platform.genericfile.providers.repository.RepositoryFileProvider;

import java.util.List;
import java.util.Objects;

public class DefaultGenericFileService implements IGenericFileService {

  private final List<IGenericFileProvider> fileProviders;

  public DefaultGenericFileService( @NonNull List<IGenericFileProvider> fileProviders ) throws InvalidGenericFileProviderException {
    Objects.requireNonNull( fileProviders );
    if ( fileProviders.isEmpty() ) {
      throw new InvalidGenericFileProviderException();
    }
    this.fileProviders = fileProviders;
  }

  public void clearCache() {
    for ( IGenericFileProvider fileProvider: fileProviders ) {
      fileProvider.clearCache();
    }
  }

  public IGenericTree loadFoldersOnly( Integer depth ) {
    if ( fileProviders.size() > 1 ) {
      BaseEntity entity = new BaseEntity();
      entity.setName( "root" );
      BaseFileTree rootTree = new BaseFileTree( entity ) {

        @Override
        public String getProvider() {
          return "combined";
        }
      };
      // If there are no filters or default filter, use default list of providers. Else load only providers found in
      // filter
      for ( IGenericFileProvider fileProvider : fileProviders ) {
        if ( fileProvider.isAvailable() ) {
          rootTree.addChild( fileProvider.getTreeFoldersOnly( depth ) );
        }
      }
      return rootTree;
    } else {
      return fileProviders.get( 0 ).getTreeFoldersOnly( depth );
    }
  }

  public boolean validate( String path ) {
    if ( path != null && path.length() > 0 ) {
      for ( IGenericFileProvider fileProvider : fileProviders ) {
        if ( path.startsWith( ":" ) ) {
          try {
            return this.get( RepositoryFileProvider.TYPE ).validate( path );
          } catch ( InvalidGenericFileProviderException e ) {
            return false;
          }
        } else if ( path.startsWith( "pvfs~::" ) ) {
          try {
            return this.get( "vfs" ).validate( path );
          } catch ( InvalidGenericFileProviderException e ) {
            return false;
          }
        }
      }
    }
    return false;
  }


  public boolean add( String path ) {
    if ( path != null && path.length() > 0 ) {
      for ( IGenericFileProvider fileProvider : fileProviders ) {
        if ( path.startsWith( ":" ) ) {
          try {
            return this.get( RepositoryFileProvider.TYPE ).add( path );
          } catch ( InvalidGenericFileProviderException e ) {
            return false;
          }
        } else if ( path.startsWith( "pvfs~::" ) ) {
          try {
            return this.get( "vfs" ).validate( path );
          } catch ( InvalidGenericFileProviderException e ) {
            return false;
          }
        }
      }
    }
    return false;
  }

  public IGenericFileProvider get( String provider ) throws InvalidGenericFileProviderException {
    return fileProviders.stream().filter( fileProvider1 ->
        fileProvider1.getType().equalsIgnoreCase( provider ) && fileProvider1.isAvailable() )
      .findFirst()
      .orElseThrow( InvalidGenericFileProviderException::new );
  }
}
