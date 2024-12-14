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

import edu.umd.cs.findbugs.annotations.NonNull;
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

  @Nullable
  private Integer expandedMaxDepth;

  private boolean includeHidden;

  private boolean bypassCache;

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
  @NonNull
  private TreeFilter filter = TreeFilter.ALL;

  public GetTreeOptions() {
  }

  /**
   * Copy constructor.
   * @param other The options instance from which to initialize this instance.
   */
  public GetTreeOptions( @NonNull GetTreeOptions other ) {
    Objects.requireNonNull( other );

    this.basePath = other.basePath;
    this.maxDepth = other.maxDepth;
    this.expandedPath = other.expandedPath;
    this.expandedMaxDepth = other.expandedMaxDepth;
    this.includeHidden = other.includeHidden;
    this.bypassCache = other.bypassCache;
    this.filter = other.filter;
  }

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
   * This option may also affect the depth below a specified expanded path, {@link #setExpandedPath(String)}.
   * <p>
   * When set to {@code null}, there is no limit to the depth of the subtree to retrieve.
   * <p>
   * Setting to a number less than zero results in setting to a {@code null} value.
   * <p>
   * The exact semantics of this parameter depends on the operation being called. For more information, please see the
   * documentation for {@link IGenericFileService#getTree(GetTreeOptions)} and
   * {@link IGenericFileService#getRootTrees(GetTreeOptions)}.
   *
   * @param maxDepth The maximum depth.
   * @see #setExpandedPath(String)
   * @see #setExpandedMaxDepth(Integer)
   */
  public void setMaxDepth( @Nullable Integer maxDepth ) {
    this.maxDepth = maxDepth != null && maxDepth >= 0 ? maxDepth : null;
  }

  /**
   * Gets the maximum depth of the subtree rooted at the expanded path.
   *
   * @return The maximum depth of the expanded path.
   */
  @Nullable
  public Integer getExpandedMaxDepth() {
    return expandedMaxDepth;
  }

  /**
   * Sets the maximum depth of the subtree rooted at the expanded path.
   * <p>
   * This property is ignored if either the {@link #getExpandedPath() expanded path} or the
   * {@link #getMaxDepth() maximum depth} options are {@code null}.
   * <p>
   * Setting to a number less than zero results in setting to a {@code null} value.
   * <p>
   * The <b>effective expanded path's maximum depth</b> is the value of this property, when specified, or the value of
   * {@link #getMaxDepth()}, otherwise.
   *
   * @param expandedMaxDepth The maximum expanded path depth.
   * @see #setExpandedPath(String)
   * @see #setMaxDepth(Integer)
   */
  public void setExpandedMaxDepth( @Nullable Integer expandedMaxDepth ) {
    this.expandedMaxDepth = expandedMaxDepth != null && expandedMaxDepth >= 0 ? expandedMaxDepth : null;
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
   * <p>
   * Moreover, if the {@link #getExpandedMaxDepth() effective expanded path maximum depth} not {@code null}, the result
   * will include a subtree of that maximum depth, rooted at the expanded path.
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
  @NonNull
  public TreeFilter getFilter() {
    return filter;
  }

  /**
   * Sets the tree filter.
   * If a null value is passed, "ALL" filter will be used.
   *
   * @param filter
   */
  public void setFilter( @Nullable TreeFilter filter ) {
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
   * Gets a value that indicates whether hidden files are included in the result.
   * <p>
   * Defaults to {@code false}.
   * @return {@code true} to include hidden files; {@code false}, otherwise.
   */
  public boolean isIncludeHidden() {
    return includeHidden;
  }

  /**
   * Sets the show hidden files value.
   *
   * @param includeHidden {@code true} to include hidden files; {@code false}, otherwise.
   */
  public void setIncludeHidden( boolean includeHidden ) {
    this.includeHidden = includeHidden;
  }

  /**
   * Gets a value that indicates if the cache is to be bypassed.
   *
   * @return {@code true} to bypass the cache; {@code false}, otherwise.
   */
  public boolean isBypassCache() {
    return bypassCache;
  }

  /**
   * Sets a value that indicates if the cache is to be bypassed.
   * <p>
   * Defaults to {@code false}.
   *
   * @param bypassCache {@code true} to bypass the cache; {@code false}, otherwise.
   */
  public void setBypassCache( boolean bypassCache ) {
    this.bypassCache = bypassCache;
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
      && Objects.equals( filter, that.filter )
      && Objects.equals( includeHidden, that.includeHidden )
      && Objects.equals( bypassCache, that.bypassCache );
  }

  @Override
  public int hashCode() {
    return Objects.hash( basePath, maxDepth, expandedPath, filter, includeHidden, bypassCache );
  }
}
