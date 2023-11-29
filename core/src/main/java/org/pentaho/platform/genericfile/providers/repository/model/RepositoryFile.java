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

package org.pentaho.platform.genericfile.providers.repository.model;

import org.pentaho.platform.api.genericfile.model.IGenericFile;

import java.util.Objects;

public class RepositoryFile extends RepositoryObject implements IGenericFile {

  private String username;

  public RepositoryFile() {
    // Necessary for JSON marshalling
  }

  public String getUsername() {
    return username;
  }

  public void setUsername( String username ) {
    this.username = username;
  }

  @Override
  public int hashCode() {
    return Objects.hash( getProvider(), getPath() );
  }

  @Override
  public boolean equals( Object obj ) {
    // If the object is compared with itself then return true
    if ( obj == this ) {
      return true;
    }

    if ( !( obj instanceof RepositoryFile ) ) {
      return false;
    }

    RepositoryFile compare = (RepositoryFile) obj;
    return compare.getProvider().equals( getProvider() )
      && ( ( compare.getPath() == null && getPath() == null ) || compare.getPath().equals( getPath() ) );
  }
}
