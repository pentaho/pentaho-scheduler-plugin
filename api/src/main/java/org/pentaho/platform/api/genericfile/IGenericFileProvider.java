/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/

package org.pentaho.platform.api.genericfile;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.pentaho.platform.api.genericfile.exception.AccessControlException;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;
import org.pentaho.platform.api.genericfile.exception.InvalidOperationException;
import org.pentaho.platform.api.genericfile.exception.NotFoundException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.api.genericfile.model.IGenericFileContentWrapper;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;

import java.util.EnumSet;

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
   * The results of this method are cached. To ensure fresh results, the {@link #clearTreeCache()} should be called
   * beforehand.
   *
   * @param options The operation options. These control, for example, whether to return the full tree,
   *                a subtree of a given base path, as well as the depth of the returned file tree,
   *                amongst other options.
   *                <p>
   *                When the {@link GetTreeOptions#getBasePath() base path option} is {@code null},
   *                then the returned tree should be rooted at the provider's root path. Otherwise, the base path must
   *                be owned by this provider, or an exception is thrown.
   *
   * @return The file tree.
   * @throws NotFoundException If the specified base file does not exist, is not a folder, or the current user is not
   *                           allowed to read it.
   * @throws AccessControlException If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   */
  @NonNull
  IGenericFileTree getTree( @NonNull GetTreeOptions options )
    throws OperationFailedException;

  /**
   * Clears the cache of trees, for the current user session.
   *
   * @see #getTree(GetTreeOptions)
   * @see #createFolder(GenericFilePath)
   * @throws AccessControlException If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
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
   * @throws InvalidOperationException If the path, or one of its prefixes, does not exist and cannot be created using this service (e.g. connections, buckets);
   *                                   if the path or its longest existing prefix does not reference a folder;
   *                                   if the path does not exist and the current user is not allowed to create folders on
   *                                   the folder denoted by its longest existing prefix.
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
   * @param path The string representation of the path of the generic folder to create.
   * @param permissions Set of permissions needed for any operation like READ/WRITE/DELETE
   * @return {@code true}, if the conditions are; {@code false}, otherwise.
   * @throws AccessControlException If the current user cannot perform this operation.
   * @throws OperationFailedException If the operation fails for some other (checked) reason.
   */
  boolean hasAccess( @NonNull GenericFilePath path, @NonNull EnumSet<GenericFilePermission> permissions );

  /**
   * Gets a wrapper object containing the target file's content, name, and MIME type.
   * @param path The string representation of the path of the generic file whose content we wish to access.
   * @return The generic file's content as an InputStream, wrapped with the associated file name and MIME type.
   * @throws OperationFailedException If the operation fails for some (checked) reason.
   */
  IGenericFileContentWrapper getFileContentWrapper(@NonNull GenericFilePath path ) throws OperationFailedException;
}
