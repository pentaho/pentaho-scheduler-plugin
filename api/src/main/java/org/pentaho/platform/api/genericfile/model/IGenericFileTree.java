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
 * Copyright (c) 2023-2024 Hitachi Vantara. All rights reserved.
 */
package org.pentaho.platform.api.genericfile.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.util.List;

/**
 * The {@code IGenericFileTree} interface unites a file with its child files, if any.
 */
public interface IGenericFileTree extends IProviderable {
  /**
   * Gets the file of the generic file tree.
   * <p>
   * For folders, this file must be the parent of the files of the
   * {@link #getChildren() child trees}. This applies to all files except the provider root folders, which, despite
   * being part of the top-most root folder's children collection, have a {@code null} <i>parent path</i>.
   */
  @NonNull
  IGenericFile getFile();

  /**
   * Gets the child trees list.
   * <p>
   * When the {@link #getFile() tree's file} is a {@link IGenericFile#TYPE_FILE file proper}, then this property should
   * be an empty list.
   * Otherwise, when the tree's file is a folder, then this property can be either {@code null} or a possibly empty
   * list.
   * When {@code null}, then the tree's file may or may not have child files. This will typically happen when a depth
   * cut-off is reached during a tree loading operation, deferring getting the children of a folder for a later request.
   */
  @Nullable
  List<IGenericFileTree> getChildren();
}
