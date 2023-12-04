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
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryRequest;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.genericfile.providers.repository.model.RepositoryFile;
import org.pentaho.platform.genericfile.providers.repository.model.RepositoryFileTree;
import org.pentaho.platform.genericfile.providers.repository.model.RepositoryFolder;
import org.pentaho.platform.genericfile.providers.repository.model.RepositoryObject;
import org.pentaho.platform.web.http.api.resources.services.FileService;

public class RepositoryFileProvider implements IGenericFileProvider<RepositoryFile> {
  public static String REPOSITORY_PREFIX = ":";
  private IUnifiedRepository unifiedRepository;

  @Override public Class<RepositoryFile> getFileClass() {
    return RepositoryFile.class;
  }

  private RepositoryFileTree tree;

  public static final String NAME = "Pentaho Repository";
  public static final String TYPE = "repository";

  public RepositoryFileProvider() {
    unifiedRepository = PentahoSystem.get( IUnifiedRepository.class, PentahoSessionHolder.getSession() );
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  /**
   * @param path
   * @return
   */
  @Override public boolean add( String path ) {
    try {
      FileService fileService = new FileService();
      return fileService.doCreateDirSafe( path );
    } catch ( FileService.InvalidNameException e ) {
      return false;
    }
  }

  @Override
  public RepositoryFileTree getFolders( Integer depth ) {
    if ( tree != null ) {
      return tree;
    }

    RepositoryRequest repoRequest = new RepositoryRequest();
    repoRequest.setDepth( depth );
    repoRequest.setIncludeAcls( false );
    repoRequest.setChildNodeFilter( "*" );
    repoRequest.setIncludeSystemFolders( false );
    repoRequest.setTypes( RepositoryRequest.FILES_TYPE_FILTER.FOLDERS );
    repoRequest.setPath( "/" );
    repoRequest.setShowHidden( true );
    org.pentaho.platform.api.repository2.unified.RepositoryFileTree nativeTree =
      unifiedRepository.getTree( repoRequest );

    tree = convertToTreeNode( nativeTree, null );

    ( (RepositoryFolder) tree.getFile() ).setName( RepositoryFileProvider.NAME );

    return tree;
  }

  @Override
  public void clearCache() {
    tree = null;
  }

  @Override
  public boolean validate( String pathId ) {
    String path = pathId.replace( ":", "/" ).replace( "~", ":" );
    org.pentaho.platform.api.repository2.unified.RepositoryFile file = unifiedRepository.getFile( path );
    return file != null;
  }

  private RepositoryObject convert(
    @NonNull org.pentaho.platform.api.repository2.unified.RepositoryFile nativeFile,
    @Nullable RepositoryFolder parentRepositoryFolder ) {

    RepositoryObject repositoryObject = nativeFile.isFolder() ? new RepositoryFolder() : new RepositoryFile();

    repositoryObject.setPath( nativeFile.getPath() );
    repositoryObject.setName( nativeFile.getName() );
    repositoryObject.setParent( parentRepositoryFolder != null ? parentRepositoryFolder.getPath() : null );
    repositoryObject.setHidden( nativeFile.isHidden() );
    repositoryObject.setModifiedDate( nativeFile.getLastModifiedDate() != null
      ? nativeFile.getLastModifiedDate()
      : nativeFile.getCreatedDate() );
    repositoryObject.setObjectId( nativeFile.getId().toString() );
    repositoryObject.setRoot( RepositoryFileProvider.NAME );
    repositoryObject.setCanEdit( true );

    repositoryObject.setTitle( nativeFile.getTitle() );
    repositoryObject.setDescription( nativeFile.getDescription() );
    if ( nativeFile.isFolder() ) {
      convertFolder( (RepositoryFolder) repositoryObject, nativeFile );
    }

    return repositoryObject;
  }

  private void convertFolder( @NonNull RepositoryFolder folder,
                              org.pentaho.platform.api.repository2.unified.RepositoryFile nativeFile ) {
    folder.setCanAddChildren( true );
  }

  @NonNull
  private RepositoryFileTree convertToTreeNode(
    @NonNull org.pentaho.platform.api.repository2.unified.RepositoryFileTree nativeTree,
    @Nullable RepositoryFolder parentRepositoryFolder ) {

    RepositoryObject repositoryObject = convert( nativeTree.getFile(), parentRepositoryFolder );
    RepositoryFileTree repositoryTree = new RepositoryFileTree( repositoryObject );
    if ( nativeTree.getChildren() != null ) {
      for ( org.pentaho.platform.api.repository2.unified.RepositoryFileTree nativeChildTree : nativeTree.getChildren() ) {
        repositoryTree.addChild( convertToTreeNode( nativeChildTree, (RepositoryFolder) repositoryObject ) );
      }
    }

    return repositoryTree;
  }

  @Override
  public boolean isAvailable() {
    return unifiedRepository != null;
  }
}
