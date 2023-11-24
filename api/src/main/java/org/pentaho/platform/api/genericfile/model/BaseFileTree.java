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

package org.pentaho.platform.api.genericfile.model;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseFileTree implements IGenericTree {

  protected IGenericFile file;
  protected List<IGenericTree> children = new ArrayList<>();

  public BaseFileTree( IGenericFile file ) {
    this.file = file;
  }

  @Override
  public IGenericFile getFile() {
    return file;
  }

  public void setFile( IGenericFile file ) {
    this.file = file;
  }

  public List<IGenericTree> getChildren() {
    return children;
  }

  public void setChildren( List<IGenericTree> children ) {
    this.children = children;
  }

  @Override
  public void addChild( IGenericTree tree ) {
    children.add( tree );
  }
}
