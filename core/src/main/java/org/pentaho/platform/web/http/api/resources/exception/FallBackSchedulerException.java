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

package org.pentaho.platform.web.http.api.resources.exception;

import org.pentaho.platform.api.scheduler2.SchedulerException;

/**
 * Exception thrown when there is an issue with the fallback mechanism in the scheduler.
 */
public class FallBackSchedulerException extends SchedulerException {

  /**
   * Constructs a new FallBackSchedulerException with the specified detail message.
   *
   * @param msg the detail message.
   */
  public FallBackSchedulerException( String msg ) {
    super( msg );
  }

  /**
   * Constructs a new FallBackSchedulerException with the specified cause.
   *
   * @param t the cause.
   */
  public FallBackSchedulerException( Throwable t ) {
    super( t );
  }

  /**
   * Constructs a new FallBackSchedulerException with the specified detail message and cause.
   *
   * @param msg the detail message.
   * @param t the cause.
   */
  public FallBackSchedulerException( String msg, Throwable t ) {
    super( msg, t );
  }
}
