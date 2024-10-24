/*!
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
 * Copyright (c) 2024 Hitachi Vantara. All rights reserved.
 */

package org.pentaho.platform.genericfile;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.pentaho.platform.api.genericfile.GenericFilePath;
import org.pentaho.platform.api.genericfile.GetTreeOptions;
import org.pentaho.platform.api.genericfile.IGenericFileProvider;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.api.genericfile.model.IGenericFileContentWrapper;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;
import org.pentaho.platform.genericfile.model.BaseGenericFileTree;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseGenericFileProvider<T extends IGenericFile> implements IGenericFileProvider<T> {

  @NonNull
  private final Map<GetTreeOptions, BaseGenericFileTree> cachedTrees;

  protected BaseGenericFileProvider() {
    cachedTrees = new ConcurrentHashMap<>();
  }

  @Override
  public boolean createFolder( @NonNull GenericFilePath path ) throws OperationFailedException {

    Objects.requireNonNull( path );

    boolean folderCreated = createFolderCore( path );
    if ( folderCreated ) {
      clearTreeCache();
    }

    return folderCreated;
  }

  protected abstract boolean createFolderCore( @NonNull GenericFilePath path ) throws OperationFailedException;

  @Override
  @NonNull
  public IGenericFileTree getTree( @NonNull GetTreeOptions options ) throws OperationFailedException {

    Objects.requireNonNull( options );

    // (Sonar) Cannot use computeIfAbsent because a checked exception needs to be thrown from the mapping function.
    BaseGenericFileTree tree = cachedTrees.get( options );
    if ( tree == null ) {
      tree = getTreeCore( options );

      processExpandedPath( tree, options );

      cachedTrees.put( options, tree );
    }

    return tree;
  }

  @NonNull
  protected abstract BaseGenericFileTree getTreeCore( @NonNull GetTreeOptions options )
    throws OperationFailedException;

  // region Expanded Path
  private void processExpandedPath( @NonNull BaseGenericFileTree tree, @NonNull GetTreeOptions options )
    throws OperationFailedException {

    // If max depth and expandedPath are specified and the expanded path is owned by this provider.
    if ( options.getExpandedPath() != null && options.getMaxDepth() != null && owns( options.getExpandedPath() ) ) {

      // When base path is not null, it can be used directly.
      // Otherwise, take it from the given tree (which should be that of the provider's root path).
      // This is a matter of efficiency, to avoid parsing the path when it is already available parsed in
      // options.getBasePath(). These must be identical, in that case.
      GenericFilePath basePath = options.getBasePath() != null
        ? options.getBasePath()
        : GenericFilePath.parseRequired( tree.getFile().getPath() );

      expandPathInTree( tree, basePath, options.getExpandedPath(), options.getMaxDepth(),
        options.getShowHiddenFiles(), options.getFilter() );
    }
  }

  private void expandPathInTree( @NonNull BaseGenericFileTree tree,
                                 @NonNull GenericFilePath basePath,
                                 @NonNull GenericFilePath expandedPath,
                                 int maxDepth,
                                 boolean showHiddenFiles,
                                 GetTreeOptions.TreeFilter filter )
    throws OperationFailedException {

    // If expanded path is not within the tree's root, then ignore it.
    List<String> relativeSegments = expandedPath.relativeSegments( basePath );
    if ( relativeSegments != null && !relativeSegments.isEmpty() ) {
      expandSegmentsInTree( tree, basePath, relativeSegments, showHiddenFiles, maxDepth, filter );
    }
  }

  private void expandSegmentsInTree( @NonNull BaseGenericFileTree tree,
                                     @NonNull GenericFilePath path,
                                     @NonNull List<String> segments,
                                     boolean showHiddenFiles,
                                     int maxDepth,
                                     GetTreeOptions.TreeFilter filter )
    throws OperationFailedException {

    for ( String segment : segments ) {
      // Find the child tree whose file name is segment.

      List<IGenericFileTree> childTrees = tree.getChildren();
      if ( childTrees == null ) {
        // Children were cut / not included due to max depth.
        // Get the children of this tree as well.
        GetTreeOptions options = new GetTreeOptions();
        options.setBasePath( path );
        options.setMaxDepth( 1 );
        options.setShowHiddenFiles( showHiddenFiles );
        options.setFilter( filter );

        BaseGenericFileTree treeWithChildren = (BaseGenericFileTree) getTree( options );

        // Steal the children.
        childTrees = treeWithChildren.getChildren();

        assert childTrees != null;

        tree.setChildren( childTrees );
      }

      BaseGenericFileTree childTree = (BaseGenericFileTree) getChildTreeByName( childTrees, segment );
      if ( childTree == null ) {
        // Child does not exist (anymore?).
        return;
      }

      tree = childTree;
      path = path.child( segment );
    }

    // At this point, tree is the tree that represents the last segment of the expanded path.
    // Now, we need to add the children of this last segment with the same max depth.
    addChildrenToExpandedPath( tree, showHiddenFiles, maxDepth, filter );
  }

  /**
   * Recursively adds children to the given tree up to a given depth.
   *
   * @param tree            The tree to which children will be added.
   * @param showHiddenFiles Whether hidden files should be shown.
   * @param depth           The depth to which children will be added.
   * @throws OperationFailedException If an error occurs while getting the children.
   */
  private void addChildrenToExpandedPath( @NonNull BaseGenericFileTree tree, boolean showHiddenFiles,
                                          int depth,
                                          GetTreeOptions.TreeFilter filter ) throws OperationFailedException {

    List<IGenericFileTree> childTrees = tree.getChildren();
    if ( childTrees == null ) {
      GetTreeOptions options = new GetTreeOptions();
      options.setBasePath( tree.getFile().getPath() );
      options.setMaxDepth( 1 );
      options.setShowHiddenFiles( showHiddenFiles );
      options.setFilter( filter );

      childTrees = getTree( options ).getChildren();

      assert childTrees != null;

      tree.setChildren( childTrees );
    }

    if ( --depth == 0 ) {
      return;
    }

    for ( IGenericFileTree child : childTrees ) {
      // Only folders can be expanded.
      if ( child.getFile().isFolder() ) {
        addChildrenToExpandedPath( (BaseGenericFileTree) child, showHiddenFiles, depth, filter );
      }
    }
  }

  @Nullable
  private IGenericFileTree getChildTreeByName( @NonNull List<IGenericFileTree> childTrees, @NonNull String name ) {
    return childTrees.stream()
      .filter( childTree -> name.equals( childTree.getFile().getName() ) )
      .findFirst().orElse( null );
  }
  // endregion

  @Override
  public void clearTreeCache() {
    cachedTrees.clear();
  }

  @Override
  public abstract IGenericFileContentWrapper getFileContentWrapper( @NonNull GenericFilePath path )
    throws OperationFailedException;
}
