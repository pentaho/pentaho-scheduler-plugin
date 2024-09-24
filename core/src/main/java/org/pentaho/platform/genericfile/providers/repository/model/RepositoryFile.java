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
