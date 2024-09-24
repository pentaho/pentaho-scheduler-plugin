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

package org.pentaho.platform.genericfile.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.pentaho.platform.api.genericfile.model.IGenericFile;

import java.util.Date;

public class BaseGenericFile implements IGenericFile {
  private String provider;
  private String name;
  private String path;
  private String parentPath;
  private String type;
  private Date modifiedDate;
  private boolean canEdit;
  private boolean canDelete;
  private String title;
  private String description;

  @NonNull
  @Override
  public String getProvider() {
    return provider;
  }

  public void setProvider( String provider ) {
    this.provider = provider;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  @Override
  public String getPath() {
    return path;
  }

  public void setPath( String path ) {
    this.path = path;
  }

  @Override
  public String getParentPath() {
    return parentPath;
  }

  public void setParentPath( String parentPath ) {
    this.parentPath = parentPath;
  }

  @Override
  public String getType() {
    return type;
  }

  public void setType( String type ) {
    this.type = type;
  }

  @Override
  public Date getModifiedDate() {
    return modifiedDate;
  }

  public void setModifiedDate( Date modifiedDate ) {
    this.modifiedDate = modifiedDate;
  }

  @Override
  public boolean isCanEdit() {
    return canEdit;
  }

  public void setCanEdit( boolean canEdit ) {
    this.canEdit = canEdit;
  }

  @Override
  public boolean isCanDelete() {
    return canDelete;
  }

  public void setCanDelete( boolean canDelete ) {
    this.canDelete = canDelete;
  }

  @Override
  public String getTitle() {
    return title;
  }

  public void setTitle( String title ) {
    this.title = title;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }
}
