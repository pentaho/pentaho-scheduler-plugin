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
 * The exception class thrown when a generic file path is determined to be invalid.
 */
public class InvalidPathException extends OperationFailedException {
  public InvalidPathException() {
    super();
  }

  public InvalidPathException( String message ) {
    super( message );
  }

  public InvalidPathException( Throwable cause ) {
    super( cause );
  }
}
