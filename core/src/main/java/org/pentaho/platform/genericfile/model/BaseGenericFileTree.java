/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2023 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

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
