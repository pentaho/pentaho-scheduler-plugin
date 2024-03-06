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
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;

/**
 * The {@code IGenericFileProvider} interface contains operations to access and modify generic files
 * owned by a specific generic file provider.
 * <p>
 * Generic file provider are of two types: the <i>Repository</i> provider, and
 *
 * @see IGenericFileService
 */
public interface IGenericFileProvider<T extends IGenericFile> {

  @NonNull
  Class<T> getFileClass();

  @NonNull
  String getName();

  @NonNull
  String getType();

  @NonNull
  IGenericFileTree getFolderTree( @NonNull GetTreeOptions options ) throws OperationFailedException;

  void clearFolderCache() throws OperationFailedException;

  boolean doesFileExist( @NonNull GenericFilePath path ) throws OperationFailedException;

  boolean createFolder( @NonNull GenericFilePath path ) throws OperationFailedException;

  boolean owns( @NonNull GenericFilePath path );
}
