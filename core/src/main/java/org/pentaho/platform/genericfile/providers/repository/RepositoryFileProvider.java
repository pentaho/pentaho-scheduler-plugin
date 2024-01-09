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

package org.pentaho.platform.genericfile.providers.repository;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.pentaho.platform.api.genericfile.IGenericFileProvider;
import org.pentaho.platform.api.genericfile.exception.AccessControlException;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.UnifiedRepositoryAccessDeniedException;
import org.pentaho.platform.api.repository2.unified.webservices.RepositoryFileDto;
import org.pentaho.platform.api.repository2.unified.webservices.RepositoryFileTreeDto;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.genericfile.messages.Messages;
import org.pentaho.platform.genericfile.providers.repository.model.RepositoryFile;
import org.pentaho.platform.genericfile.providers.repository.model.RepositoryFileTree;
import org.pentaho.platform.genericfile.providers.repository.model.RepositoryFolder;
import org.pentaho.platform.genericfile.providers.repository.model.RepositoryObject;
import org.pentaho.platform.web.http.api.resources.services.FileService;

import java.util.Date;

import static org.pentaho.platform.util.RepositoryPathEncoder.encodeRepositoryPath;

public class RepositoryFileProvider implements IGenericFileProvider<RepositoryFile> {
  public static String REPOSITORY_PREFIX = "/";
  private IUnifiedRepository unifiedRepository;

  @Override
  public Class<RepositoryFile> getFileClass() {
    return RepositoryFile.class;
  }

  private RepositoryFileTree tree;
  public static final String TYPE = "repository";

  public RepositoryFileProvider() {
    unifiedRepository = PentahoSystem.get( IUnifiedRepository.class, PentahoSessionHolder.getSession() );
  }

  @Override
  public String getName() {
    return Messages.getString( "GenericFileRepository.REPOSITORY_FOLDER_DISPLAY" );
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean createFolder( @NonNull String path ) throws OperationFailedException {
    FileService fileService = new FileService();

    // When parent path is not found, its creation is attempted.

    boolean folderCreated;
    try {
      folderCreated = fileService.doCreateDirSafe( encodeRepositoryPath( path ) );
    } catch ( UnifiedRepositoryAccessDeniedException e ) {
      throw new AccessControlException( e );
    } catch ( FileService.InvalidNameException e ) {
      throw new InvalidPathException();
    }

    if ( folderCreated ) {
      clearFolderCache();
    }

    return folderCreated;
  }

  @Override
  @NonNull
  public RepositoryFileTree getFolders( @Nullable Integer depth ) {
    if ( tree != null ) {
      return tree;
    }

    FileService fileService = new FileService();
    RepositoryFileTreeDto nativeTree =
      fileService.doGetTree( "/", depth, "*|FOLDERS", true, false, false );


    tree = convertToTreeNode( nativeTree, null );

    RepositoryFolder repositoryFolder = (RepositoryFolder) tree.getFile();
    repositoryFolder.setName( Messages.getString( "GenericFileRepository.REPOSITORY_FOLDER_DISPLAY" ) );
    repositoryFolder.setCanAddChildren( false );
    repositoryFolder.setCanDelete( false );
    repositoryFolder.setCanEdit( false );

    return tree;
  }

  @Override
  public void clearFolderCache() {
    tree = null;
  }

  @Override
  public boolean doesFolderExist( @NonNull String path ) {
    org.pentaho.platform.api.repository2.unified.RepositoryFile file = unifiedRepository.getFile( path );
    return file != null;
  }

  private RepositoryObject convert(
    @NonNull RepositoryFileDto nativeFile,
    @Nullable RepositoryFolder parentRepositoryFolder ) {

    RepositoryObject repositoryObject = nativeFile.isFolder() ? new RepositoryFolder() : new RepositoryFile();

    repositoryObject.setPath( nativeFile.getPath() );
    repositoryObject.setName( nativeFile.getName() );
    repositoryObject.setParent( parentRepositoryFolder != null ? parentRepositoryFolder.getPath() : null );
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

  private void convertFolder( @NonNull RepositoryFolder folder,
                              RepositoryFileDto nativeFile ) {
    folder.setCanAddChildren( true );
  }

  @NonNull
  private RepositoryFileTree convertToTreeNode(
    @NonNull RepositoryFileTreeDto nativeTree,
    @Nullable RepositoryFolder parentRepositoryFolder ) {

    RepositoryObject repositoryObject = convert( nativeTree.getFile(), parentRepositoryFolder );
    RepositoryFileTree repositoryTree = new RepositoryFileTree( repositoryObject );
    if ( nativeTree.getChildren() != null ) {
      for ( RepositoryFileTreeDto nativeChildTree : nativeTree.getChildren() ) {
        repositoryTree.addChild( convertToTreeNode( nativeChildTree, (RepositoryFolder) repositoryObject ) );
      }
    }

    return repositoryTree;
  }

  @Override
  public boolean isAvailable() {
    return unifiedRepository != null;
  }

  @Override
  public boolean owns( String path ) {
    return path.startsWith( REPOSITORY_PREFIX );
  }
}
