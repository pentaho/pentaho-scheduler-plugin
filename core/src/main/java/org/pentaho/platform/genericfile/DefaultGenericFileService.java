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
import org.pentaho.platform.api.genericfile.IGenericFileProvider;
import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.exception.InvalidGenericFileProviderException;
import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;
import org.pentaho.platform.genericfile.model.BaseGenericFile;
import org.pentaho.platform.genericfile.model.BaseGenericFileTree;
import org.pentaho.platform.genericfile.providers.repository.RepositoryFileProvider;

import java.util.List;
import java.util.Objects;

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

  public void clearCache() {
    for ( IGenericFileProvider<?> fileProvider : fileProviders ) {
      fileProvider.clearCache();
    }
  }

  public IGenericFileTree getFolders( Integer depth ) {
    if ( fileProviders.size() > 1 ) {
      BaseGenericFile entity = new BaseGenericFile();
      entity.setName( "root" );
      entity.setProvider( "combined" );
      entity.setType( IGenericFile.TYPE_FOLDER );

      BaseGenericFileTree rootTree = new BaseGenericFileTree( entity ) {
        @Override
        public String getProvider() {
          return "combined";
        }
      };

      for ( IGenericFileProvider<?> fileProvider : fileProviders ) {
        if ( fileProvider.isAvailable() ) {
          rootTree.addChild( fileProvider.getFolders( depth ) );
        }
      }

      return rootTree;
    } else {
      return fileProviders.get( 0 ).getFolders( depth );
    }
  }

  public boolean validate( String path ) {
    if ( path != null && path.length() > 0 ) {

      // TODO: This implementation is quite off...
      for ( IGenericFileProvider<?> fileProvider : fileProviders ) {
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
      // TODO: This implementation is quite off...
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
