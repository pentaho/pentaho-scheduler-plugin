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

package org.pentaho.platform.scheduler2.action;

import org.pentaho.platform.api.action.IAction;
import org.pentaho.platform.api.scheduler2.IBackgroundExecutionStreamProvider;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Executes an {@link IAction} as a given user.
 */
public interface IActionRunner extends Callable<Boolean> {
  /**
   * Set action, contains the main logic.
   * @param action
   */
  void setAction( IAction action );

  /**
   * Set the user and execute check security and permissions with this identity.
   * @param actionUser
   */
  void setActionUser( String actionUser );

  /**
   * Set parameters. Used to pass objects and other information needed.
   * @param params
   */
  void setParams( Map<String, Serializable> params );

  /**
   * Set streamProvider, will be used to write the destination output.
   * @param streamProvider
   */
  void setStreamProvider( IBackgroundExecutionStreamProvider streamProvider );

}
