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

import java.util.Date;

/**
 * The {@code IGenericFile} interface contains basic information about a generic file.
 * <p>
 * To know whether a generic file is a file proper, or a folder, use {@link #isFolder()} or {@link #getType()}.
 * Folder generic file instances do not directly contain their children. Children of a folder generic file are
 * represented as part of a {@link IGenericFileTree} instance.
 * <p>
 * To know the name or the path of a file, use {@link #getName()} and {@link #getPath()}, respectively.
 */
public interface IGenericFile extends IProviderable {
  /**
   * The {@link #getType() type} value for a folder generic file.
   */
  String TYPE_FOLDER = "folder";

  /**
   * The {@link #getType() type} value for a file proper generic file.
   */
  String TYPE_FILE = "file";

  /**
   * Gets the physical name of the file.
   * <p>
   * Generally, the physical name of a file is the last segment of its {@link #getPath() path}.
   * <p>
   * A valid generic file instance must have a non-null name.
   *
   * @see #getNameDecoded()
   * @see #getTitle()
   */
  String getName();

  /**
   * Gets the physical, non-encoded name of the file.
   * <p>
   * The same as {@link #getName()} but without any encoding.
   * <p>
   * The default implementation simply returns {@link #getName()}.
   */
  default String getNameDecoded() {
    return getName();
  }

  /**
   * Gets the path of the file, as a string.
   * <p>
   * A valid generic file instance must have a non-null path.
   *
   * @see #getName()
   * @see org.pentaho.platform.api.genericfile.GenericFilePath
   */
  String getPath();

  /**
   * Gets the path of the parent folder, as a string, if any.
   * <p>
   * The provider root folders do not have a parent.
   * Otherwise, a valid generic file instance must have a non-null parent path.
   *
   * @see #getPath()
   */
  String getParentPath();

  /**
   * Gets the type of generic file, one of: {@link #TYPE_FOLDER} or {@link #TYPE_FILE}.
   *
   * @see #isFolder()
   */
  String getType();

  /**
   * Determines if a generic file is a folder.
   * <p>
   * The default implementation checks if the value of {@link #getType()} is equal to {@link #TYPE_FOLDER}.
   *
   * @return {@code true}, if the generic file is a folder; {@code false}, otherwise.
   */
  default boolean isFolder() {
    return TYPE_FOLDER.equals( getType() );
  }

  /**
   * Gets the modified date of the generic file.
   */
  Date getModifiedDate();

  /**
   * Gets whether the generic file can be edited.
   */
  boolean isCanEdit();

  /**
   * Gets whether the generic file can be deleted.
   */
  boolean isCanDelete();

  /**
   * Gets the title of the file.
   * <p>
   * The title of a file is a localized, human-readable version of its {@link #getNameDecoded()} non-encoded name}.
   * <p>
   * Unlike the name of a file, the title may not be unique amongst siblings.
   * <p>
   * When title of a file is unspecified, the name of a file can be used in its place.
   *
   * @see #getName()
   * @see #getNameDecoded()
   * @see #getDescription()
   */
  String getTitle();

  /**
   * Gets the description of the file.
   * <p>
   * The description of a file is a localized, human-readable description of a file. Typically, displayed in a tooltip
   * in a user interface.
   *
   * @see #getName()
   * @see #getTitle()
   */
  String getDescription();
}
