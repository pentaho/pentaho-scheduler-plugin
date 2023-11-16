package org.pentaho.platform.api.genericfile.model;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseFileTree implements IGenericTree {

    protected IGenericFile file;
    protected List<IGenericTree> children = new ArrayList<>();

    public BaseFileTree( IGenericFile file ) {
        this.file = file;
    }

    @Override public IGenericFile getFile() {
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
