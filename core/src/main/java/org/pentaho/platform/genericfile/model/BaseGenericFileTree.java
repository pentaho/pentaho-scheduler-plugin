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

import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseGenericFileTree implements IGenericFileTree {

  protected IGenericFile file;
  protected List<IGenericFileTree> children = new ArrayList<>();

  protected BaseGenericFileTree( IGenericFile file ) {
    this.file = file;
  }

  @Override
  public IGenericFile getFile() {
    return file;
  }

  public void setFile( IGenericFile file ) {
    this.file = file;
  }

  public List<IGenericFileTree> getChildren() {
    return children;
  }

  public void setChildren( List<IGenericFileTree> children ) {
    this.children = children;
  }

  @Override
  public void addChild( IGenericFileTree tree ) {
    children.add( tree );
  }
}
