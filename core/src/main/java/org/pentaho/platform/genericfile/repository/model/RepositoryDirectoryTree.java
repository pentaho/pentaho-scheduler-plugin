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

package org.pentaho.platform.genericfile.repository.model;

import org.pentaho.platform.api.genericfile.model.IGenericTreeNode;

import java.util.ArrayList;
import java.util.List;

public class RepositoryDirectoryTree implements IGenericTreeNode<RepositoryFile> {
    private RepositoryFile folder;
    private List<IGenericTreeNode<RepositoryFile>> children;
    public RepositoryDirectoryTree( RepositoryFile folder ) {
        super();
        this.folder = folder;
        this.children = new ArrayList<>();
    }
    @Override
    public List<IGenericTreeNode<RepositoryFile>> getChildren() {
        return children;
    }

    @Override
    public void setChildren(List<IGenericTreeNode<RepositoryFile>> children) {
        this.children.addAll( children );
    }

    @Override
    public void addChild(IGenericTreeNode<RepositoryFile> child) {
        this.children.add( child );
    }

    @Override
    public RepositoryFile getTreeNodeValue() {
        return folder;
    }

    @Override
    public void setTreeNodeValue(RepositoryFile value) {
        this.folder = value;
    }
}
