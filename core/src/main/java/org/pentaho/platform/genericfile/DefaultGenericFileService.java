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


package org.pentaho.platform.genericfile;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.pentaho.platform.api.genericfile.GenericFilePath;
import org.pentaho.platform.api.genericfile.GenericFilePermission;
import org.pentaho.platform.api.genericfile.GetTreeOptions;
import org.pentaho.platform.api.genericfile.IGenericFileProvider;
import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.exception.InvalidGenericFileProviderException;
import org.pentaho.platform.api.genericfile.exception.NotFoundException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.api.genericfile.model.IGenericFileContentWrapper;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;
import org.pentaho.platform.genericfile.model.BaseGenericFile;
import org.pentaho.platform.genericfile.model.BaseGenericFileTree;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DefaultGenericFileService implements IGenericFileService {

  @VisibleForTesting
  static final String MULTIPLE_PROVIDER_ROOT_PROVIDER = "combined";
  @VisibleForTesting
  static final String MULTIPLE_PROVIDER_ROOT_NAME = "root";

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

  public void clearTreeCache() {
    for ( IGenericFileProvider<?> fileProvider : fileProviders ) {
      try {
        fileProvider.clearTreeCache();
      } catch ( OperationFailedException e ) {
        // Clear as many as possible. Still, log each failure.
        e.printStackTrace();
      }
    }
  }

  @NonNull
  public IGenericFileTree getTree( @NonNull GetTreeOptions options ) throws OperationFailedException {

    Objects.requireNonNull( options );

    if ( isSingleProviderMode() ) {
      return fileProviders.get( 0 ).getTree( options );
    }

    return options.getBasePath() == null
      ? getTreeFromRoot( options )
      : getSubTree( options.getBasePath(), options );
  }

  @VisibleForTesting
  boolean isSingleProviderMode() {
    return fileProviders.size() == 1;
  }

  @NonNull
  private IGenericFileTree getTreeFromRoot( @NonNull GetTreeOptions options )
    throws OperationFailedException {

    BaseGenericFileTree rootTree = createMultipleProviderTreeRoot();

    OperationFailedException firstProviderException = null;
    for ( IGenericFileProvider<?> fileProvider : fileProviders ) {
      try {
        rootTree.addChild( fileProvider.getTree( options ) );
      } catch ( OperationFailedException e ) {
        if ( firstProviderException == null ) {
          firstProviderException = e;
        }

        // Continue, collecting providers that work. But still log failed ones, JIC.
        e.printStackTrace();
      }
    }

    if ( firstProviderException != null && rootTree.getChildren() == null ) {
      // All providers failed. Opting to throw the error of the first failed one to the caller.
      throw firstProviderException;
    }

    return rootTree;
  }

  @NonNull
  private static BaseGenericFileTree createMultipleProviderTreeRoot() {
    // Note that the absolute root has a null path.
    BaseGenericFile entity = new BaseGenericFile();
    entity.setName( MULTIPLE_PROVIDER_ROOT_NAME );
    entity.setProvider( MULTIPLE_PROVIDER_ROOT_PROVIDER );
    entity.setType( IGenericFile.TYPE_FOLDER );

    return new BaseGenericFileTree( entity );
  }

  @NonNull
  private IGenericFileTree getSubTree( @NonNull GenericFilePath basePath, @NonNull GetTreeOptions options )
    throws OperationFailedException {

    // In multi-provider mode, and fetching a subtree based on basePath, the parent path is the parent path of basePath.
    return getOwnerFileProvider( basePath )
      .orElseThrow( () -> new NotFoundException( String.format( "Base path not found '%s'.", basePath ) ) )
      .getTree( options );
  }

  public boolean doesFolderExist( @NonNull GenericFilePath path ) throws OperationFailedException {
    Optional<IGenericFileProvider<?>> fileProvider = getOwnerFileProvider( path );

    return fileProvider.isPresent() && fileProvider.get().doesFolderExist( path );
  }

  public boolean createFolder( @NonNull GenericFilePath path ) throws OperationFailedException {
    return getOwnerFileProvider( path )
      .orElseThrow( NotFoundException::new )
      .createFolder( path );
  }

  @Override
  @NonNull
  public IGenericFileContentWrapper getFileContentWrapper( @NonNull GenericFilePath path )
    throws OperationFailedException {
    return getOwnerFileProvider( path ).orElseThrow( NotFoundException::new ).getFileContentWrapper( path );
  }

  @Override
  @NonNull
  public IGenericFile getFile( @NonNull GenericFilePath path )
    throws OperationFailedException {
    return getOwnerFileProvider( path ).orElseThrow( NotFoundException::new ).getFile( path );
  }

  private Optional<IGenericFileProvider<?>> getOwnerFileProvider( @NonNull GenericFilePath path ) {
    return fileProviders.stream()
      .filter( fileProvider -> fileProvider.owns( path ) )
      .findFirst();
  }

  @Override
  public boolean hasAccess( @NonNull GenericFilePath path, @NonNull EnumSet<GenericFilePermission> permissions )
    throws OperationFailedException {
    Optional<IGenericFileProvider<?>> fileProvider = getOwnerFileProvider( path );

    return fileProvider.isPresent() && fileProvider.get().hasAccess( path, permissions );
  }
}
