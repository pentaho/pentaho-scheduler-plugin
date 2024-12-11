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
import org.pentaho.platform.api.genericfile.exception.AccessControlException;
import org.pentaho.platform.api.genericfile.exception.InvalidOperationException;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;
import org.pentaho.platform.api.genericfile.exception.NotFoundException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.api.genericfile.model.IGenericFileContentWrapper;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;

import java.util.EnumSet;
import java.util.List;

import org.pentaho.platform.api.genericfile.model.IGenericFileContentWrapper;

/**
 * The {@code IGenericFileService} interface contains operations to access and modify generic files.
 *
 * @see GenericFilePath
 * @see IGenericFileProvider
 * @see IGenericFileTree
 */
public interface IGenericFileService {
  /**
   * Clears the cache of file trees, for all generic file providers, for the current user session.
   *
   * @see #getTree(GetTreeOptions)
   * @see #createFolder(GenericFilePath)
   * @throws AccessControlException If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   */
  void clearTreeCache() throws OperationFailedException;

  /**
   * Gets a tree of files.
   *
   * <p>
   * The results of this method are cached. To ensure fresh results, set {@link GetTreeOptions#setBypassCache(boolean)}
   * to {@code true} or call {@link #clearTreeCache()} beforehand.
   *
   * <h3>Obtaining the root tree</h3>
   * When called with a {@code null} {@link GetTreeOptions#getBasePath() base path option}, this method provides an
   * umbrella view of the generic file system, by using an <i>abstract</i> root tree folder. It is abstract in the sense
   * that the folder cannot be addressed for any actual file operations and serves only a grouping purpose. Moreover,
   * the children of this root folder may themselves be <i>umbrella</i> root tree folders, one per generic file system
   * provider backing the service. The exact structure of these provider umbrella trees is determined by the providers
   * themselves. While some providers may represent and expose a single, addressable file system, and opt to directly
   * return that file system's real root tree folder, others may represent several file systems, in which case these
   * are exposed under an abstract, umbrella provider root tree folder.
   * <p>
   * Implementations may directly return the tree result of a provider, when there is only one registered provider,
   * bypassing the abstract root tree folder.
   * <p>
   * In any case, the root tree folder returned by a provider is considered to have a depth of {@code 0}.
   * <p>
   * For a more regular and predictable interface to obtaining actually addressable generic file system root folders,
   * see {@link #getRootTrees(GetTreeOptions)}.
   *
   * <h3>Obtaining a subtree</h3>
   * When {@link GetTreeOptions#getBasePath() base path} is specified, the returned tree is rooted at the specified
   * <i>base</i> folder. The <i>base</i> folder is considered to have a depth of {@code 0}.
   *
   * @param options The operation options.
   * @return A file tree.
   * @throws NotFoundException If the specified base file does not exist, is not a folder, or the current user is not
   *                           allowed to read it.
   * @throws AccessControlException If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   */
  @NonNull
  IGenericFileTree getTree( @NonNull GetTreeOptions options ) throws OperationFailedException;

  /**
   * Gets a list of the real root trees of the generic file system.
   * <p>
   * This method returns the real root trees that compose the generic file system.
   * Contrast with the method {@link #getTree(GetTreeOptions)}, when called with a {@code null}
   * {@link GetTreeOptions#getBasePath() base path option}, which instead offers an umbrella view of the generic file
   * system.
   * <p>
   * Each returned root folder is considered to have a depth of {@code 0}.
   * <p>
   * The results of this method are not cached, and so {@link GetTreeOptions#isBypassCache()} is ignored.
   *
   * @param options The operation options. The {@link GetTreeOptions#getBasePath() base path option} is ignored.
   * @return A list of the real root trees.
   * @throws AccessControlException If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   */
  @NonNull
  List<IGenericFileTree> getRootTrees( @NonNull GetTreeOptions options ) throws OperationFailedException;

  /**
   * Checks whether a generic file exists, is a folder and the current user can read it, given its path.
   *
   * @param path The path of the generic file.
   * @return {@code true}, if the conditions are met; {@code false}, otherwise.
   * @throws AccessControlException If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   */
  boolean doesFolderExist( @NonNull GenericFilePath path ) throws OperationFailedException;

  /**
   * Checks whether a generic file exists and is a folder, given its path's string representation.
   * <p>
   * The default implementation of this method parses the given path's string representation using
   * {@link GenericFilePath#parseRequired(String)} and then calls {@link #doesFolderExist(GenericFilePath)} with the result.
   *
   * @param path The string representation of the path of the generic file.
   * @return {@code true}, if the generic file exists; {@code false}, otherwise.
   * @throws InvalidPathException     If the specified path's string representation is not valid, according to
   *                                  {@link GenericFilePath#parseRequired(String)}.
   * @throws AccessControlException If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   */
  default boolean doesFolderExist( @NonNull String path ) throws OperationFailedException {
    return doesFolderExist( GenericFilePath.parseRequired( path ) );
  }

  /**
   * Creates a folder given its path.
   * <p>
   * This method ensures that each ancestor folder of the specified folder exists,
   * creating it if necessary, and allowed.
   * <p>
   * When the operation is successful, the folder session cache for the generic file provider owning the folder is
   * automatically cleared.
   *
   * @param path The path of the generic folder to create.
   * @return {@code true}, if the folder did not exist and was created; {@code false}, if the folder already existed.
   * @throws AccessControlException    If the current user cannot perform this operation.
   * @throws InvalidPathException      If the folder path is not valid.
   * @throws InvalidOperationException If the path, or one of its prefixes, does not exist and cannot be created using this service (e.g. connections, buckets);
   *                                   if the path or its longest existing prefix does not reference a folder;
   *                                   if the path does not exist and the current user is not allowed to create folders on
   *                                   the folder denoted by its longest existing prefix.
   * @throws OperationFailedException  If the operation fails for some other (checked) reason.
   * @see #clearTreeCache()
   */
  boolean createFolder( @NonNull GenericFilePath path ) throws OperationFailedException;

  /**
   * Creates a folder given its path's string representation.
   * <p>
   * This method ensures that each ancestor folder of the specified folder exists,
   * creating it if necessary, and allowed.
   * <p>
   * The default implementation of this method parses the given path's string representation using
   * {@link GenericFilePath#parseRequired(String)} and then calls {@link #createFolder(GenericFilePath)} with the result.
   * <p>
   * When the operation is successful, the folder tree session cache for the generic file provider owning the folder is
   * automatically cleared.
   *
   * @param path The string representation of the path of the generic folder to create.
   * @return {@code true}, if the folder did not exist and was created; {@code false}, if the folder already existed.
   * @throws AccessControlException    If the current user cannot perform this operation.
   * @throws InvalidPathException      If the folder path is not valid.
   * @throws InvalidOperationException If the path, or one of its prefixes, does not exist and cannot be created using
   *                                   this service (e.g. connections, buckets);
   *                                   if the path or its longest existing prefix does not reference a folder;
   *                                   if the path does not exist and the current user is not allowed to create folders
   *                                   on the folder denoted by its longest existing prefix.
   * @throws OperationFailedException  If the operation fails for some other (checked) reason.
   * @see #clearTreeCache()
   */
  default boolean createFolder( @Nullable String path ) throws OperationFailedException {
    return createFolder( GenericFilePath.parseRequired( path ) );
  }

  /**
   * Checks whether a generic file exists and the current user has the specified permissions on it, given its path's
   * string representation.
   * <p>
   * The default implementation of this method parses the given path's string representation using
   * {@link GenericFilePath#parseRequired(String)} and then calls {@link #hasAccess(GenericFilePath, EnumSet)} with the
   * result.
   *
   * @param path The string representation of the path of the generic file.
   * @param permissions Set of permissions needed for any operation like READ/WRITE/DELETE
   * @return {@code true}, if the conditions are; {@code false}, otherwise.
   * @throws AccessControlException If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   */
  default boolean hasAccess( @NonNull String path, @NonNull EnumSet<GenericFilePermission> permissions )
    throws OperationFailedException {
    return hasAccess( GenericFilePath.parseRequired( path ), permissions );
  }

  /**
   * Checks whether a generic file exists and the current user has the specified permissions on it.
   * @param path The path of the generic file.
   * @param permissions Set of permissions needed for any operation like READ/WRITE/DELETE
   * @return {@code true}, if the conditions are; {@code false}, otherwise.
   * @throws AccessControlException If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   */
  boolean hasAccess( @NonNull GenericFilePath path, @NonNull EnumSet<GenericFilePermission> permissions )
    throws OperationFailedException;

  /**
   * Gets the content of a file, given its path's string representation.
   * <p>
   * The default implementation of this method parses the given path's string representation using
   * {@link GenericFilePath#parseRequired(String)} and then calls {@link #getFileContentWrapper(GenericFilePath)} with
   * the result.
   *
   * @param path The string representation of the path of the file.
   * @return The file's content wrapper.
   * @throws NotFoundException        If the specified file does not exist, or the current user is not allowed to read
   *                                  it.
   * @throws AccessControlException   If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   */
  default IGenericFileContentWrapper getFileContentWrapper( @NonNull String path )
    throws OperationFailedException {
    return getFileContentWrapper( GenericFilePath.parseRequired( path ) );
  }

  /**
   * Gets the content of a file, given its path.
   *
   * @param path The path of the file.
   * @return The file's content wrapper.
   * @throws NotFoundException        If the specified file does not exist, or the current user is not allowed to read
   *                                  it.
   * @throws AccessControlException   If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   */
  @NonNull
  IGenericFileContentWrapper getFileContentWrapper( @NonNull GenericFilePath path ) throws OperationFailedException;

  /**
   * Gets a file given its path's string representation.
   * <p>
   * The default implementation of this method parses the given path's string representation using
   * {@link GenericFilePath#parseRequired(String)} and then calls {@link #getFile(GenericFilePath)} with the result.
   *
   * @param path The string representation of the path of the file.
   * @return The file.
   * @throws NotFoundException        If the specified file does not exist, or the current user is not allowed to
   *                                  read it.
   * @throws AccessControlException   If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   */
  @NonNull
  default IGenericFile getFile( @NonNull String path ) throws OperationFailedException {
    return getFile( GenericFilePath.parseRequired( path ) );
  }

  /**
   * Gets a file given its path.
   *
   * @param path The path of the file.
   * @return The file.
   * @throws NotFoundException        If the specified file does not exist, or the current user is not allowed to
   *                                  read it.
   * @throws AccessControlException   If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   */
  @NonNull
  IGenericFile getFile( @NonNull GenericFilePath path ) throws OperationFailedException;
}
