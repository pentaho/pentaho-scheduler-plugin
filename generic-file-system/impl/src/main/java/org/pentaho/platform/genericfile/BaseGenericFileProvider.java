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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

  // region getRootTrees
  @NonNull
  @Override
  public List<IGenericFileTree> getRootTrees( @NonNull GetTreeOptions options ) throws OperationFailedException {
    Objects.requireNonNull( options );

    List<BaseGenericFileTree> rootTrees = getRootTreesCore( options );

    if ( shouldProcessExpandedPath( options ) ) {
      expandPathsInTrees( rootTrees, options );
    }

    return rootTrees.stream()
      .map( tree -> (IGenericFileTree) tree )
      .collect( Collectors.toList() );
  }

  @NonNull
  protected abstract List<BaseGenericFileTree> getRootTreesCore( @NonNull GetTreeOptions options )
    throws OperationFailedException;
  // endregion

  @Override
  @NonNull
  public IGenericFileTree getTree( @NonNull GetTreeOptions options ) throws OperationFailedException {

    Objects.requireNonNull( options );

    // (Sonar) Cannot use computeIfAbsent because a checked exception needs to be thrown from the mapping function.
    BaseGenericFileTree tree = null;
    if ( !options.isBypassCache() ) {
      tree = loadFromTreeCache( options );
    }

    if ( tree == null ) {
      tree = getTreeCore( options );

      List<GenericFilePath> effectiveExpandedPaths = null;
      if ( shouldProcessExpandedPath( options ) ) {
        effectiveExpandedPaths = expandPathsInTrees( List.of( tree ), options );
      }

      storeInTreeCache( tree, options, effectiveExpandedPaths );
    }

    return tree;
  }

  @NonNull
  protected abstract BaseGenericFileTree getTreeCore( @NonNull GetTreeOptions options )
    throws OperationFailedException;

  // region Expanded Path
  private int getEffectiveExpandedMaxDepth( @NonNull GetTreeOptions options ) {
    assert options.getMaxDepth() != null;

    return options.getExpandedMaxDepth() != null
      ? options.getExpandedMaxDepth()
      : options.getMaxDepth();
  }

  private boolean shouldProcessExpandedPath( @NonNull GetTreeOptions options ) {
    return options.getFilter() != GetTreeOptions.TreeFilter.FILES
      && options.getExpandedPaths() != null
      && options.getMaxDepth() != null;
  }

  private List<GenericFilePath> expandPathsInTrees( @NonNull List<BaseGenericFileTree> baseTrees,
                                                    @NonNull GetTreeOptions options )
    throws OperationFailedException {

    assert options.getExpandedPaths() != null;

    List<GenericFilePath> effectiveExpandedPaths = null;

    for ( GenericFilePath expandedPath : options.getExpandedPaths() ) {
      if ( owns( expandedPath ) && expandOwnPathInTrees( baseTrees, options, expandedPath ) ) {
        if ( effectiveExpandedPaths == null ) {
          effectiveExpandedPaths = new ArrayList<>();
        }

        effectiveExpandedPaths.add( expandedPath );
      }
    }

    return effectiveExpandedPaths;
  }

  private boolean expandOwnPathInTrees( @NonNull List<BaseGenericFileTree> baseTrees,
                                        @NonNull GetTreeOptions options,
                                        @NonNull GenericFilePath expandedPath )
    throws OperationFailedException {

    for ( BaseGenericFileTree baseTree : baseTrees ) {
      if ( expandOwnPathInTree( baseTree, options, expandedPath ) ) {
        return true;
      }
    }

    return false;
  }

  private boolean expandOwnPathInTree( @NonNull BaseGenericFileTree tree,
                                       @NonNull GetTreeOptions options,
                                       @NonNull GenericFilePath expandedPath )
    throws OperationFailedException {

    GenericFilePath path = GenericFilePath.parseRequired( tree.getFile().getPath() );

    // If expanded path is not within the tree's root, then ignore it.
    List<String> segments = expandedPath.relativeSegments( path );
    if ( segments == null ) {
      return false;
    }

    List<IGenericFileTree> childTrees = tree.getChildren();

    for ( String segment : segments ) {
      // Find the child tree whose file name is segment.

      if ( childTrees == null ) {
        // Children were cut / not included due to max depth.
        // Get the children of this tree as well.
        childTrees = getChildTrees( path, options, 1 );

        tree.setChildren( childTrees );
      }

      BaseGenericFileTree childTree = (BaseGenericFileTree) findChildTreeByName( childTrees, segment );
      if ( childTree == null ) {
        // Child does not exist (anymore?).
        return true;
      }

      tree = childTree;
      childTrees = tree.getChildren();
      path = path.child( segment );
    }

    // At this point, tree is the tree that represents the last segment of the expanded path.
    // Now, we need to add the children of this last segment ensuring the specified expanded max depth.
    addChildrenToExpandedPath( tree, options, getEffectiveExpandedMaxDepth( options ) );
    return true;
  }

  @NonNull
  private List<IGenericFileTree> getChildTrees( @NonNull GenericFilePath basePath,
                                                @NonNull GetTreeOptions baseOptions,
                                                int maxDepth )
    throws OperationFailedException {

    assert maxDepth >= 1;

    // Will use same bypassCache option.
    GetTreeOptions options = new GetTreeOptions( baseOptions );
    options.setBasePath( basePath );
    options.setMaxDepth( maxDepth );
    options.setExpandedPaths( null );
    options.setExpandedMaxDepth( null );

    BaseGenericFileTree treeWithChildren = (BaseGenericFileTree) getTree( options );

    assert treeWithChildren.getChildren() != null;

    return treeWithChildren.getChildren();
  }

  /**
   * Recursively adds children to the given tree up to a given depth. If expandedMaxDepth is less than 1 or the tree root is
   * not a folder, then nothing is done.
   *
   * @param tree             The tree to which children will be added.
   * @param options          The get tree options.
   * @param expandedMaxDepth The max depth to which children will be added.
   * @throws OperationFailedException If an error occurs while getting the children.
   */
  private void addChildrenToExpandedPath( @NonNull BaseGenericFileTree tree,
                                          @NonNull GetTreeOptions options,
                                          int expandedMaxDepth ) throws OperationFailedException {

    if ( expandedMaxDepth < 1 || !tree.getFile().isFolder() ) {
      return;
    }

    List<IGenericFileTree> childTrees = tree.getChildren();

    // If tree has no children yet, try to get them (and even the children of its children) based on the max depth.
    if ( childTrees == null ) {
      childTrees = getChildTrees(
        GenericFilePath.parseRequired( tree.getFile().getPath() ),
        options,
        expandedMaxDepth );

      tree.setChildren( childTrees );
      return;
    }

    int nextDepth = expandedMaxDepth - 1;

    // If children was previously fetched, then we want to recursively look for its children, controlling the max depth.
    for ( IGenericFileTree child : childTrees ) {
      addChildrenToExpandedPath( (BaseGenericFileTree) child, options, nextDepth );
    }
  }

  @Nullable
  private IGenericFileTree findChildTreeByName( @NonNull List<IGenericFileTree> childTrees, @NonNull String name ) {
    return childTrees.stream()
      .filter( childTree -> name.equals( childTree.getFile().getName() ) )
      .findFirst().orElse( null );
  }
  // endregion

  // region Tree Cache
  @Override
  public void clearTreeCache() {
    cachedTrees.clear();
  }

  @Nullable
  private BaseGenericFileTree loadFromTreeCache( @NonNull GetTreeOptions options ) {
    BaseGenericFileTree cachedTree = cachedTrees.get( options );
    if ( cachedTree == null ) {
      return null;
    }

    // Prevent cache pollution due to reuse across multiple expanded levels.
    BaseGenericFileTree copyTree = new BaseGenericFileTree( cachedTree.getFile() );
    if ( cachedTree.getChildren() != null ) {
      copyTree.setChildren( List.copyOf( cachedTree.getChildren() ) );
    }

    return copyTree;
  }

  private void storeInTreeCache( @NonNull BaseGenericFileTree tree,
                                 @NonNull GetTreeOptions options,
                                 @Nullable List<GenericFilePath> effectiveExpandedPaths ) {
    // Take the chance to store/update the cache, even if bypassing cache.
    // However, must create a clone and change bypassCache to false...
    // Also, store in cache for both the specified expanded paths and for the existing ones.
    GetTreeOptions cacheOptions = new GetTreeOptions( options );
    cacheOptions.setBypassCache( false );
    cachedTrees.put( cacheOptions, tree );

    cacheOptions = new GetTreeOptions( options );
    cacheOptions.setBypassCache( false );
    cacheOptions.setExpandedPaths( effectiveExpandedPaths );
    cachedTrees.put( cacheOptions, tree );
  }
  // endregion

  @Override
  public abstract IGenericFileContentWrapper getFileContentWrapper( @NonNull GenericFilePath path )
    throws OperationFailedException;

  @NonNull
  @Override
  public abstract IGenericFile getFile( @NonNull GenericFilePath path ) throws OperationFailedException;
}
