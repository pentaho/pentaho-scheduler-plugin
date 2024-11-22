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


package org.pentaho.platform.api.genericfile.exception;

public class InvalidOperationException extends OperationFailedException {

  public InvalidOperationException() {
    super();
  }

  public InvalidOperationException( String message ) {
    super( message );
  }

  public InvalidOperationException( Throwable cause ) {
    super( cause );
  }
}
