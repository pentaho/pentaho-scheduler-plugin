/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.platform.web.http.api.proxies;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author wseyler
 * 
 */
@XmlRootElement
public class BlockStatusProxy {
  Boolean totallyBlocked;
  Boolean partiallyBlocked;

  public BlockStatusProxy() {
    this( false, false );
  }

  public BlockStatusProxy( Boolean totallyBlocked, Boolean partiallyBlocked ) {
    super();
    this.totallyBlocked = totallyBlocked;
    this.partiallyBlocked = partiallyBlocked;
  }

  public Boolean getTotallyBlocked() {
    return totallyBlocked;
  }

  public void setTotallyBlocked( Boolean totallyBlocked ) {
    this.totallyBlocked = totallyBlocked;
  }

  public Boolean getPartiallyBlocked() {
    return partiallyBlocked;
  }

  public void setPartiallyBlocked( Boolean partiallyBlocked ) {
    this.partiallyBlocked = partiallyBlocked;
  }

  @Override public boolean equals( Object o ) {
    if ( this == o ) {
      return true;
    }

    if ( o == null || getClass() != o.getClass() ) {
      return false;
    }

    BlockStatusProxy that = (BlockStatusProxy) o;

    return new EqualsBuilder()
      .append( totallyBlocked, that.totallyBlocked )
      .append( partiallyBlocked, that.partiallyBlocked )
      .isEquals();
  }

  @Override public int hashCode() {
    return new HashCodeBuilder( 17, 37 )
      .append( totallyBlocked )
      .append( partiallyBlocked )
      .toHashCode();
  }

  @Override public String toString() {
    return new ToStringBuilder( this )
      .append( "partiallyBlocked", partiallyBlocked )
      .append( "totallyBlocked", totallyBlocked )
      .toString();
  }
}
