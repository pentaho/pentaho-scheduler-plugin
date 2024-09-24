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

package org.pentaho.platform.api.genericfile.exception;

/**
 * The exception class thrown when a generic file assumed to exist, does not or the user does not have READ/WRITE
 * access needed to perform some operation of the
 * {@link org.pentaho.platform.api.genericfile.IGenericFileService IGenericFileService interface},
 * to not allow a user without read permissions to even know if a file exists.
 */
public class NotFoundException extends OperationFailedException {
  public NotFoundException() {
    super();
  }

  public NotFoundException( String message ) {
    super( message );
  }

  public NotFoundException( Throwable cause ) {
    super( cause );
  }
}
