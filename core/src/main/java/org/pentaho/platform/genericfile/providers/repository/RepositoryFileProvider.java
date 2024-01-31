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
import org.pentaho.platform.genericfile.BaseGenericFileProvider;
import org.pentaho.platform.api.genericfile.GenericFilePath;
import org.pentaho.platform.api.genericfile.GetTreeOptions;
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

import java.util.ArrayList;
import java.util.Date;

import static org.pentaho.platform.util.RepositoryPathEncoder.encodeRepositoryPath;

public class RepositoryFileProvider extends BaseGenericFileProvider<RepositoryFile> {
  public static final String REPOSITORY_PREFIX = "/";
  private IUnifiedRepository unifiedRepository;

  @NonNull
  @Override
  public Class<RepositoryFile> getFileClass() {
    return RepositoryFile.class;
  }

  public static final String TYPE = "repository";

  public RepositoryFileProvider() {
    unifiedRepository = PentahoSystem.get( IUnifiedRepository.class, PentahoSessionHolder.getSession() );
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
  protected RepositoryFileTree getFolderTreeCore( @NonNull GetTreeOptions options ) {
    FileService fileService = new FileService();

    // #doGetTree supports either paths or pathIds, so no need to explicitly convert rootPath to a pathId,
    // like in other methods.
    RepositoryFileTreeDto nativeTree = fileService.doGetTree(
      encodeRepositoryPath( options.getBasePath().toString() ),
      options.getMaxDepth(),
      "*|FOLDERS",
      true,
      false,
      false );


    RepositoryFileTree tree = convertToTreeNode( nativeTree, null );

    RepositoryFolder repositoryFolder = (RepositoryFolder) tree.getFile();
    repositoryFolder.setName( Messages.getString( "GenericFileRepository.REPOSITORY_FOLDER_DISPLAY" ) );
    repositoryFolder.setCanAddChildren( false );
    repositoryFolder.setCanDelete( false );
    repositoryFolder.setCanEdit( false );

    return tree;
  }

  @Override
  public boolean doesFolderExist( @NonNull GenericFilePath path ) {
    org.pentaho.platform.api.repository2.unified.RepositoryFile file = unifiedRepository.getFile( path.toString() );
    return file != null;
  }

  private RepositoryObject convert( @NonNull RepositoryFileDto nativeFile,
                                    @Nullable RepositoryFolder parentRepositoryFolder ) {

    RepositoryObject repositoryObject = nativeFile.isFolder() ? new RepositoryFolder() : new RepositoryFile();

    repositoryObject.setPath( nativeFile.getPath() );
    repositoryObject.setName( nativeFile.getName() );
    repositoryObject.setParentPath( parentRepositoryFolder != null ? parentRepositoryFolder.getPath() : null );
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
                                                @Nullable RepositoryFolder parentRepositoryFolder ) {

    RepositoryObject repositoryObject = convert( nativeTree.getFile(), parentRepositoryFolder );
    RepositoryFileTree repositoryTree = new RepositoryFileTree( repositoryObject );

    if ( nativeTree.getChildren() != null ) {
      // Ensure an empty list is reflected.
      repositoryTree.setChildren( new ArrayList<>() );

      for ( RepositoryFileTreeDto nativeChildTree : nativeTree.getChildren() ) {
        repositoryTree.addChild( convertToTreeNode( nativeChildTree, (RepositoryFolder) repositoryObject ) );
      }
    }

    return repositoryTree;
  }

  @Override
  public boolean owns( @NonNull GenericFilePath path ) {
    return path.getRoot().equals( REPOSITORY_PREFIX );
  }
}
