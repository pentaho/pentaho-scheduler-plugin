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
 * Copyright (c) 2024 Hitachi Vantara. All rights reserved.
 *
 */

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
  void setParams( Map<String, Object> params );

  /**
   * Set streamProvider, will be used to write the destination output.
   * @param streamProvider
   */
  void setStreamProvider( IBackgroundExecutionStreamProvider streamProvider );

}
