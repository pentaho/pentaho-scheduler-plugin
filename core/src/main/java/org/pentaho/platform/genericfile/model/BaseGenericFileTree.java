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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BaseGenericFileTree implements IGenericFileTree {

  @NonNull
  protected final BaseGenericFile file;

  @Nullable
  protected List<IGenericFileTree> children;

  public BaseGenericFileTree( @NonNull BaseGenericFile file ) {
    this.file = Objects.requireNonNull( file );
  }

  @Override
  @NonNull
  public BaseGenericFile getFile() {
    return file;
  }

  @Override
  @Nullable
  public List<IGenericFileTree> getChildren() {
    return children;
  }

  public void setChildren( @Nullable List<IGenericFileTree> children ) {
    this.children = children;
  }

  /**
   * Adds a child tree to this file tree.
   * <p>
   * If this file tree has a {@code null} {@link #getChildren() child trees list} before this call, one will be
   * instantiated to hold the new child tree.
   *
   * @param childTree The child tree.
   */
  public void addChild( @NonNull IGenericFileTree childTree ) {
    Objects.requireNonNull( childTree );

    if ( children == null ) {
      children = new ArrayList<>();
    }

    children.add( childTree );
  }
}
