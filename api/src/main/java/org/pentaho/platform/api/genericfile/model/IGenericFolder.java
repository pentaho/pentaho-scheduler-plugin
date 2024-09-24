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

public interface IGenericFolder extends IGenericFile {
  @Override
  default String getType() {
    return TYPE_FOLDER;
  }

  boolean isCanAddChildren();

  default boolean isHasChildren() {
    return true;
  }
}
