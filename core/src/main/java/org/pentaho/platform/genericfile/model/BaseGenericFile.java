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

import java.util.Date;

public class BaseGenericFile implements IGenericFile {
  private String provider;
  private String name;
  private String path;
  private String parent;
  private String type;
  private Date modifiedDate;
  private boolean canEdit;
  private boolean canDelete;
  private String title;
  private String description;

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
  public String getParent() {
    return parent;
  }

  public void setParent( String parent ) {
    this.parent = parent;
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
