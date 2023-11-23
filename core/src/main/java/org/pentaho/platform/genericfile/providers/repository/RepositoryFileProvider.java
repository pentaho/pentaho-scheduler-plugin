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

package org.pentaho.platform.genericfile.providers.repository;

import org.pentaho.platform.api.genericfile.IGenericFileProvider;
import org.pentaho.platform.api.genericfile.model.IGenericTree;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFileTree;
import org.pentaho.platform.api.repository2.unified.RepositoryRequest;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.genericfile.providers.repository.model.RepositoryFile;
import org.pentaho.platform.genericfile.providers.repository.model.RepositoryTree;
import org.pentaho.platform.web.http.api.resources.services.FileService;

public class RepositoryFileProvider implements IGenericFileProvider<RepositoryFile> {
  public static String REPOSITORY_PREFIX = ":";
  private IUnifiedRepository unifiedRepository;

  @Override public Class<RepositoryFile> getFileClass() {
    return RepositoryFile.class;
  }

  private IGenericTree tree;

  public static final String NAME = "Pentaho Repository";
  public static final String TYPE = "repository";

  public RepositoryFileProvider() {
    unifiedRepository = PentahoSystem.get(IUnifiedRepository.class, PentahoSessionHolder.getSession());
  }
  @Override public String getName() {
    return NAME;
  }

  @Override public String getType() {
    return TYPE;
  }

  /**
   * @param path
   *
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
  public IGenericTree getTreeFoldersOnly( Integer depth ) {
    if ( tree != null ) {
      return tree;
    }

    RepositoryRequest repoRequest = new RepositoryRequest();
    repoRequest.setDepth( depth );
    repoRequest.setIncludeAcls( false );
    repoRequest.setChildNodeFilter( "*" );
    repoRequest.setIncludeSystemFolders(false);
    repoRequest.setTypes( RepositoryRequest.FILES_TYPE_FILTER.FOLDERS );
    repoRequest.setPath( "/" );
    repoRequest.setShowHidden( true );
    RepositoryFileTree tree = unifiedRepository.getTree( repoRequest );
    this.tree = convertToTreeNode( tree );
    return this.tree;
  }

  @Override
  public void clearCache() {
    tree = null;
  }

  @Override
  public boolean validate(String pathId) {
    String path = pathId.replace( ":", "/" ).replace( "~", ":" );
    org.pentaho.platform.api.repository2.unified.RepositoryFile file = unifiedRepository.getFile( path );
    return file != null;
  }

  private RepositoryFile convert(org.pentaho.platform.api.repository2.unified.RepositoryFile file ) {
    RepositoryFile repositoryFile = new RepositoryFile();
    repositoryFile.setPath( file.getPath() );
    repositoryFile.setName(file.getName() );
    repositoryFile.setProvider("repository");
    repositoryFile.setParent(file.getOriginalParentFolderPath() );
    repositoryFile.setObjectId(file.getId().toString() );
    return repositoryFile;
  }
  private IGenericTree convertToTreeNode( RepositoryFileTree tree ) {
    RepositoryTree repositoryTree = new RepositoryTree(convert( tree.getFile() ) );
    for ( RepositoryFileTree fileTree: tree.getChildren() ) {
      repositoryTree.addChild( convertToTreeNode( fileTree ) );
    }
    return repositoryTree;
  }


  @Override public boolean isAvailable() {
    return unifiedRepository != null;
  }


}
