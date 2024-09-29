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


package org.pentaho.platform.genericfile.providers.repository.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.pentaho.platform.genericfile.model.BaseGenericFileTree;

public class RepositoryFileTree extends BaseGenericFileTree {

  public RepositoryFileTree( RepositoryObject file ) {
    super( file );
  }

  @NonNull
  @Override
  public RepositoryObject getFile() {
    return (RepositoryObject) super.getFile();
  }
}
