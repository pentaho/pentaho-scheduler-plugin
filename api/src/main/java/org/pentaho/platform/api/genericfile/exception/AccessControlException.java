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
 * The exception class thrown when the user of the current session does not have correct role-based access security
 * in API methods from {@link org.pentaho.platform.api.genericfile.IGenericFileService IGenericFileService interface}.
 */
public class AccessControlException extends OperationFailedException {

  public AccessControlException() {
    super();
  }

  public AccessControlException( String message ) {
    super( message );
  }

  public AccessControlException( Throwable cause ) {
    super( cause );
  }
}
