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
 * Copyright (c) 2023 - 2024 Hitachi Vantara. All rights reserved.
 */

package org.pentaho.platform.api.genericfile;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.pentaho.platform.api.genericfile.exception.AccessControlException;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;
import org.pentaho.platform.api.genericfile.exception.NotFoundException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;

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
   * @throws AccessControlException   If the user of the current session does not have permission to browse the
   *                                  specified files.
   * @throws NotFoundException        If the specified {@link GetTreeOptions#getBasePath() base path option} is not
   *                                  {@code null} and is not owned by this provider.
   * @throws OperationFailedException If the operation fails for any other (checked) reason.
   */
  @NonNull
  IGenericFileTree getTree( @NonNull GetTreeOptions options )
    throws OperationFailedException;

  /**
   * Clears the cache of trees, for the current user session.
   *
   * @throws OperationFailedException If the operation fails for some (checked) reason.
   * @see #getTree(GetTreeOptions)
   * @see #createFolder(GenericFilePath)
   */
  void clearTreeCache() throws OperationFailedException;


  /**
   * Checks whether a generic file exists, given its path.
   *
   * @param path The path of the generic file.
   * @return {@code true}, if the generic file exists; {@code false}, otherwise.
   * @throws AccessControlException   If the user of the current session does not have permission to check the existence
   *                                  of the specified file.
   * @throws OperationFailedException If the operation fails for any other (checked) reason.
   */
  boolean doesFileExist( @NonNull GenericFilePath path ) throws OperationFailedException;

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
   * @throws AccessControlException   If the user of the current session does not have permission to create the folder.
   * @throws InvalidPathException If the folder's path is not valid.
   * @throws OperationFailedException If the operation fails for any other (checked) reason.
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
}
