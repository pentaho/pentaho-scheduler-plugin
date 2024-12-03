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

package org.pentaho.platform.api.genericfile;

import edu.umd.cs.findbugs.annotations.Nullable;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;

import java.util.Objects;

/**
 * This class contains the options for retrieving a subtree of generic files.
 */
public class GetTreeOptions {
  @Nullable
  private GenericFilePath basePath;

  @Nullable
  private Integer maxDepth;

  @Nullable
  private GenericFilePath expandedPath;

  private boolean showHiddenFiles;

  /**
   * Enum to represent the three filters that can be applied to trees.
   * Technically, in our model, everything in the tree is a file; folders are just files that have children. We will
   * call these FileObjects.
   * To a user, a folder is a folder and a file is a file. These filters already exist in our repository
   * implementation, so I have perpetuated them in the enum values.
   */
  public enum TreeFilter {
    /**
     * "Folders" make up the branches of the tree, they have children.
     */
    FOLDERS,
    /**
     * "Files" are the leaves of the trees, they do not have children.
     */
    FILES,
    /**
     * All FileObjects in the tree, "Folders" and "Files".
     */
    ALL;

    /**
     * Returns true if the file type passes the filter
     * @param isFolder
     * @return
     */
    public boolean passesFilter( boolean isFolder ) {
      switch ( this ) {
        case FOLDERS:
          return isFolder;
        case FILES:
          return !isFolder;
        case ALL:
          return true;
        default:
          throw new IllegalArgumentException( "This filter type has not been accounted for." );
      }
    }
  }

  /**
   * The filter used to narrow down the tree.
   */
  private TreeFilter filter = TreeFilter.ALL;

  /**
   * Gets the base path of the subtree to retrieve.
   * <p>
   * When the base path is {@code null}, the whole tree, for the context of the operation, is retrieved.
   * Otherwise, the resulting tree is rooted at the specified base path.
   * <p>
   * Defaults to {@code null}.
   *
   * @return The base path.
   */
  @Nullable
  public GenericFilePath getBasePath() {
    return basePath;
  }

  /**
   * Sets the base path of the subtree to retrieve from a string.
   * <p>
   * The specified value is parsed by {@link GenericFilePath#parse(String)}.
   *
   * @param basePath The base path as a string.
   */
  public void setBasePath( @Nullable String basePath ) throws InvalidPathException {
    this.setBasePath( GenericFilePath.parse( basePath ) );
  }

  /**
   * Sets the base path of the subtree to retrieve.
   * <p>
   * When the base path is {@code null}, the whole tree is retrieved.
   * <p>
   * Defaults to {@code null}.
   *
   * @param basePath The base path.
   */
  public void setBasePath( @Nullable GenericFilePath basePath ) {
    this.basePath = basePath;
  }

  /**
   * Gets the maximum depth of the subtree to retrieve.
   *
   * @return The maximum depth.
   */
  @Nullable
  public Integer getMaxDepth() {
    return maxDepth;
  }

  /**
   * Sets the maximum depth of the subtree to retrieve.
   * <p>
   * When set to {@code null}, there is no limit to the depth of the subtree to retrieve.
   * <p>
   * Setting to a number less than one results in setting to a {@code null} value.
   * <p>
   * When {@link #getBasePath() base path} is specified, a depth of {@code 1} corresponds to its children.
   * When base path is not specified, a depth of {@code 1} corresponds to the children of the root folder of each
   * generic file provider.
   *
   * @param maxDepth The maximum depth.
   */
  public void setMaxDepth( @Nullable Integer maxDepth ) {
    this.maxDepth = maxDepth != null && maxDepth >= 1 ? maxDepth : null;
  }

  /**
   * Gets the expanded path.
   *
   * @return The expanded path.
   */
  @Nullable
  public GenericFilePath getExpandedPath() {
    return expandedPath;
  }

  /**
   * Sets the expanded path, given as a string.
   * <p>
   * This property is ignored if {@link #getMaxDepth() maximum depth} is {@code null}.
   * <p>
   * When {@link #getBasePath() base path} is non-{@code null}, the expanded path must be equal to, or a descendant of
   * it. Otherwise, it is ignored.
   * <p>
   * The expanded path is included in the returned subtree even if not covered by the specified
   * {@link #getMaxDepth() maximum depth}.
   * All the ancestors of the expanded path, up to a non-{@code null} {@link #getBasePath() base path}, will be
   * included, along with their direct children.
   *
   * @param expandedPath The expanded path as a string.
   */
  public void setExpandedPath( @Nullable String expandedPath ) throws InvalidPathException {
    this.setExpandedPath( GenericFilePath.parse( expandedPath ) );
  }

  /**
   * Sets the expanded path.
   * <p>
   * This property is ignored if {@link #getMaxDepth() maximum depth} is {@code null}.
   * <p>
   * When {@link #getBasePath() base path} is non-{@code null}, the expanded path must be equal to, or a descendant of
   * it. Otherwise, it is ignored.
   * <p>
   * The expanded path is included in the returned subtree even if not covered by the specified
   * {@link #getMaxDepth() maximum depth}.
   * All the ancestors of the expanded path, up to a non-{@code null} {@link #getBasePath() base path}, will be
   * included, along with their direct children.
   *
   * @param expandedPath The expanded path.
   */
  public void setExpandedPath( @Nullable GenericFilePath expandedPath ) {
    this.expandedPath = expandedPath;
  }

  /**
   * Gets the tree filter.
   *
   * @return the tree filter.
   */
  public TreeFilter getFilter() {
    return filter;
  }

  /**
   * Sets the tree filter.
   * If a null value is passed, "ALL" filter will be used.
   *
   * @param filter
   */
  public void setFilter( TreeFilter filter ) {
    this.filter = ( filter == null ) ? TreeFilter.ALL : filter;
  }

  /**
   * Sets the tree filter via parsing string value.
   *
   * @param treeFilterString
   */
  public void setFilter( String treeFilterString ) throws IllegalArgumentException {
    if ( treeFilterString == null ) {
      setFilter( TreeFilter.ALL );
    } else {
      setFilter( TreeFilter.valueOf( treeFilterString ) );
    }
  }

  /**
   * Gets the show hidden files value.
   * <p>
   * Defaults to {@code false}.
   * @return the show hidden files value.
   */
  public boolean getShowHiddenFiles() {
    return showHiddenFiles;
  }

  /**
   * Sets the show hidden files value.
   *
   * @param showHiddenFiles
   */
  public void setShowHiddenFiles( boolean showHiddenFiles ) {
    this.showHiddenFiles = showHiddenFiles;
  }

  @Override
  public boolean equals( Object other ) {
    if ( this == other ) {
      return true;
    }

    if ( other == null || getClass() != other.getClass() ) {
      return false;
    }

    GetTreeOptions that = (GetTreeOptions) other;
    return Objects.equals( basePath, that.basePath )
      && Objects.equals( maxDepth, that.maxDepth )
      && Objects.equals( expandedPath, that.expandedPath )
      && Objects.equals( filter, that.filter );
  }

  @Override
  public int hashCode() {
    return Objects.hash( basePath, maxDepth, expandedPath, filter );
  }
}
