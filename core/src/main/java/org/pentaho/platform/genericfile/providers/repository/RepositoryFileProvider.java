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
 * Copyright (c) 2023-2024 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.genericfile.providers.repository;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.pentaho.platform.api.genericfile.GenericFilePath;
import org.pentaho.platform.api.genericfile.GetTreeOptions;
import org.pentaho.platform.api.genericfile.exception.AccessControlException;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;
import org.pentaho.platform.api.genericfile.exception.NotFoundException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.UnifiedRepositoryAccessDeniedException;
import org.pentaho.platform.api.repository2.unified.webservices.RepositoryFileDto;
import org.pentaho.platform.api.repository2.unified.webservices.RepositoryFileTreeDto;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.genericfile.BaseGenericFileProvider;
import org.pentaho.platform.genericfile.messages.Messages;
import org.pentaho.platform.genericfile.providers.repository.model.RepositoryFile;
import org.pentaho.platform.genericfile.providers.repository.model.RepositoryFileTree;
import org.pentaho.platform.genericfile.providers.repository.model.RepositoryFolder;
import org.pentaho.platform.genericfile.providers.repository.model.RepositoryObject;
import org.pentaho.platform.web.http.api.resources.services.FileService;

import java.util.ArrayList;
import java.util.Date;

import static org.pentaho.platform.util.RepositoryPathEncoder.encodeRepositoryPath;

public class RepositoryFileProvider extends BaseGenericFileProvider<RepositoryFile> {
  public static final String ROOT_PATH = "/";

  private static GenericFilePath ROOT_GENERIC_PATH;

  static {
    try {
      ROOT_GENERIC_PATH = GenericFilePath.parseRequired( ROOT_PATH );
    } catch ( InvalidPathException e ) {
      // Never happens.
    }
  }

  public static final String TYPE = "repository";

  private final IUnifiedRepository unifiedRepository;

  public RepositoryFileProvider() {
    unifiedRepository = PentahoSystem.get( IUnifiedRepository.class, PentahoSessionHolder.getSession() );
  }

  @NonNull
  @Override
  public Class<RepositoryFile> getFileClass() {
    return RepositoryFile.class;
  }

  @NonNull
  @Override
  public String getName() {
    return Messages.getString( "GenericFileRepository.REPOSITORY_FOLDER_DISPLAY" );
  }

  @NonNull
  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  protected boolean createFolderCore( @NonNull GenericFilePath path ) throws OperationFailedException {
    FileService fileService = new FileService();

    // When parent path is not found, its creation is attempted.
    try {
      return fileService.doCreateDirSafe( encodeRepositoryPath( path.toString() ) );
    } catch ( UnifiedRepositoryAccessDeniedException e ) {
      throw new AccessControlException( e );
    } catch ( FileService.InvalidNameException e ) {
      throw new InvalidPathException();
    }
  }

  @NonNull
  protected RepositoryFileTree getFileTreeCore( @NonNull GetTreeOptions options ) throws NotFoundException {

    // Get the whole tree under the provider root (VFS connections)?
    GenericFilePath basePath = options.getBasePath();
    if ( basePath == null ) {
      basePath = ROOT_GENERIC_PATH;
      assert basePath != null;
    } else if ( !owns( basePath ) ) {
      throw new NotFoundException( String.format( "Base path not found '%s'.", basePath ) );
    }

    FileService fileService = new FileService();

    String repositoryFilterString = options.getFilter().repositoryFilterString;

    RepositoryFileTreeDto nativeTree = fileService.doGetTree(
      encodeRepositoryPath( basePath.toString() ),
      options.getMaxDepth(),
      repositoryFilterString,
      true,
      false,
      false );

    // The parent path of base path.
    GenericFilePath parentPath = basePath.getParent();
    String parentPathString = parentPath != null ? parentPath.toString() : null;

    RepositoryFileTree tree = convertToTreeNode( nativeTree, parentPathString );

    RepositoryFolder repositoryFolder = (RepositoryFolder) tree.getFile();
    repositoryFolder.setName( Messages.getString( "GenericFileRepository.REPOSITORY_FOLDER_DISPLAY" ) );
    repositoryFolder.setCanAddChildren( false );
    repositoryFolder.setCanDelete( false );
    repositoryFolder.setCanEdit( false );

    return tree;
  }

  @Override
  public boolean doesFileExist( @NonNull GenericFilePath path ) {
    org.pentaho.platform.api.repository2.unified.RepositoryFile file = unifiedRepository.getFile( path.toString() );
    return file != null;
  }

  @NonNull
  private RepositoryObject convert( @NonNull RepositoryFileDto nativeFile, @Nullable String parentPath ) {

    RepositoryObject repositoryObject = nativeFile.isFolder() ? new RepositoryFolder() : new RepositoryFile();

    repositoryObject.setPath( nativeFile.getPath() );
    repositoryObject.setName( nativeFile.getName() );
    repositoryObject.setParentPath( parentPath );
    repositoryObject.setHidden( nativeFile.isHidden() );
    Date modifiedDate = ( nativeFile.getLastModifiedDate() != null && !nativeFile.getLastModifiedDate().isEmpty() )
      ? new Date( Long.parseLong( nativeFile.getLastModifiedDate() ) )
      : new Date( Long.parseLong( nativeFile.getCreatedDate() ) );
    repositoryObject.setModifiedDate( modifiedDate );
    repositoryObject.setObjectId( nativeFile.getId().toString() );
    repositoryObject.setCanEdit( true );
    repositoryObject.setTitle( nativeFile.getTitle() );
    repositoryObject.setDescription( nativeFile.getDescription() );
    if ( nativeFile.isFolder() ) {
      convertFolder( (RepositoryFolder) repositoryObject, nativeFile );
    }

    return repositoryObject;
  }

  private void convertFolder( @NonNull RepositoryFolder folder, RepositoryFileDto nativeFile ) {
    folder.setCanAddChildren( true );
  }

  @NonNull
  private RepositoryFileTree convertToTreeNode( @NonNull RepositoryFileTreeDto nativeTree,
                                                @Nullable String parentPath ) {

    RepositoryObject repositoryObject = convert( nativeTree.getFile(), parentPath );
    RepositoryFileTree repositoryTree = new RepositoryFileTree( repositoryObject );

    if ( nativeTree.getChildren() != null ) {
      // Ensure an empty list is reflected.
      repositoryTree.setChildren( new ArrayList<>() );

      String path = repositoryObject.getPath();

      for ( RepositoryFileTreeDto nativeChildTree : nativeTree.getChildren() ) {
        repositoryTree.addChild( convertToTreeNode( nativeChildTree, path ) );
      }
    }

    return repositoryTree;
  }

  @Override
  public boolean owns( @NonNull GenericFilePath path ) {
    return path.getFirstSegment().equals( ROOT_PATH );
  }
}
