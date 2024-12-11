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

/**
 * The {@code IGenericFileProvider} interface contains operations to access and modify generic files
 * owned by a specific generic file provider.
 * <p>
 * Generic file providers are identified by their {@link #getType() type string}.
 * The {@link #owns(GenericFilePath)} method determines if a provider owns a path.
 *
 * @param <T> The specific {@link IGenericFile generic file} class used by the provider.
 * @see IGenericFileService
 */
public interface IGenericFileProvider<T extends IGenericFile> {
  /**
   * Gets specific {@link IGenericFile generic file} class used by the provider, matching the type parameter, {@code T}.
   */
  @NonNull
  Class<T> getFileClass();

  /**
   * Gets the name of the provider.
   * <p>
   * The name of a provider is a localized, human-readable name of the provider. Ideally, it should be unique.
   *
   * @see #getType()
   */
  @NonNull
  String getName();

  /**
   * Gets the unique identifier of the provider.
   *
   * @see #getName()
   */
  @NonNull
  String getType();

  /**
   * Gets a tree of files.
   * <p>
   * The results of this method are cached. To ensure fresh results, set {@link GetTreeOptions#setBypassCache(boolean)}
   * to {@code true} or call {@link #clearTreeCache()} beforehand.
   * beforehand.
   *
   * @param options The operation options.
   * @return The file tree.
   * @throws NotFoundException If the specified base file does not exist, is not a folder, or the current user is not
   *                           allowed to read it.
   * @throws AccessControlException If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   * @see IGenericFileService#getTree(GetTreeOptions)
   */
  @NonNull
  IGenericFileTree getTree( @NonNull GetTreeOptions options ) throws OperationFailedException;

  /**
   * Gets a list of the real root trees that this provider provides to the generic file system.
   * <p>
   * The returned root tree folders are considered to have a depth of {@code 0}.
   * <p>
   * The results of this method are not cached, and so {@link GetTreeOptions#isBypassCache()} is ignored.
   *
   * @param options The operation options.
   * @return A list of the real root trees provided by this provider.
   * @throws AccessControlException If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   * @see IGenericFileService#getRootTrees(GetTreeOptions)
   */
  @NonNull
  List<IGenericFileTree> getRootTrees( @NonNull GetTreeOptions options ) throws OperationFailedException;

  /**
   * Clears the cache of trees, for the current user session.
   *
   * @throws AccessControlException If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   * @see #getTree(GetTreeOptions)
   * @see #createFolder(GenericFilePath)
   */
  void clearTreeCache() throws OperationFailedException;

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
   * Creates a folder given its path.
   * <p>
   * This method ensures that each ancestor folder of the specified folder exists,
   * creating it if necessary, and allowed.
   * <p>
   * When the operation is successful, the folder tree session cache is automatically cleared.
   *
   * @param path The folder's generic file path.
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
  boolean createFolder( @NonNull GenericFilePath path ) throws OperationFailedException;

  /**
   * Determines if the provider owns a given path.
   * <p>
   * The {@link GenericFilePath#getFirstSegment() provider root segment} of a generic file path is exclusive of each
   * provider and is used to determine if a path is owned by a provider.
   *
   * @param path The generic file path to check.
   * @return {@code true}, if the provider owns the specified generic file path; {@code false}, otherwise.
   */
  boolean owns( @NonNull GenericFilePath path );

  /**
   * Checks whether a generic file exists and the current user has the specified permissions on it.
   *
   * @param path The string representation of the path of the generic folder to create.
   * @param permissions Set of permissions needed for any operation like READ/WRITE/DELETE
   * @return {@code true}, if the conditions are; {@code false}, otherwise.
   * @throws AccessControlException If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   */
  boolean hasAccess( @NonNull GenericFilePath path, @NonNull EnumSet<GenericFilePermission> permissions )
    throws OperationFailedException;

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
