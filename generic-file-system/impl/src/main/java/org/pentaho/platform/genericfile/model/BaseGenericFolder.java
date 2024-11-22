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


package org.pentaho.platform.genericfile.model;

import org.pentaho.platform.api.genericfile.model.IGenericFolder;

public abstract class BaseGenericFolder extends BaseGenericFile implements IGenericFolder {
  private boolean hasChildren;
  private boolean canAddChildren;

  protected BaseGenericFolder() {
    setType( TYPE_FOLDER );
  }

  public boolean isHasChildren() {
    return hasChildren;
  }

  public void setHasChildren( boolean hasChildren ) {
    this.hasChildren = hasChildren;
  }


  @Override
  public boolean isCanAddChildren() {
    return canAddChildren;
  }

  public void setCanAddChildren( boolean canAddChildren ) {
    this.canAddChildren = canAddChildren;
  }
}
