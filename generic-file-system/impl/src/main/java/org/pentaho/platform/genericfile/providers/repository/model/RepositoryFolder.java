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


package org.pentaho.platform.genericfile.providers.repository.model;

import org.pentaho.platform.api.genericfile.model.IGenericFolder;

public class RepositoryFolder extends RepositoryFile implements IGenericFolder {
  private boolean canAddChildren;
  private boolean hasChildren;

  public RepositoryFolder() {
    this.setType( TYPE_FOLDER );
    this.setHasChildren( true );
  }

  @Override
  public boolean isCanAddChildren() {
    return canAddChildren;
  }

  public void setCanAddChildren( boolean canAddChildren ) {
    this.canAddChildren = canAddChildren;
  }

  @Override
  public boolean isHasChildren() {
    return hasChildren;
  }

  public void setHasChildren( boolean hasChildren ) {
    this.hasChildren = hasChildren;
  }
}
