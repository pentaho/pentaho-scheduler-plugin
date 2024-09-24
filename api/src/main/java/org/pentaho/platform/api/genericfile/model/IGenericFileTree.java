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
package org.pentaho.platform.api.genericfile.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.util.List;

/**
 * The {@code IGenericFileTree} interface unites a file with its child files, if any.
 */
public interface IGenericFileTree {
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
