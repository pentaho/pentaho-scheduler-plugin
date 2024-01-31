/*!
 *
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
 *
 * Copyright (c) 2023 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.genericfile.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class BaseGenericFileTree implements IGenericFileTree {

  @NonNull
  protected final BaseGenericFile file;

  @Nullable
  protected List<IGenericFileTree> children;

  protected BaseGenericFileTree( @NonNull BaseGenericFile file ) {
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

  @Override
  public void addChild( @NonNull IGenericFileTree childTree ) {

    if ( !( childTree instanceof BaseGenericFileTree ) ) {
      throw new IllegalArgumentException(
        String.format( "The argument 'childTree' is not an instance of '%s'.", BaseGenericFileTree.class.getName() ) );
    }

    if ( children == null ) {
      children = new ArrayList<>();
    }

    children.add( childTree );

    BaseGenericFile childFile = ( (BaseGenericFileTree) childTree ).getFile();
    childFile.setParentPath( file.getPath() );
  }
}
