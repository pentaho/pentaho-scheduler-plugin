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
 * Copyright (c) 2002-2023 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.mantle.client.external.services.definitions;

import com.google.gwt.json.client.JSONObject;

public interface IRunInBackgroundCommandUtils {

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.createScheduleOutputLocationDialog
  */
  void createScheduleOutputLocationDialog( String solutionPath, Boolean feedback );

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.createScheduleOutputLocationDialog
  */
  void createScheduleOutputLocationDialog( String solutionPath, String outputLocationPath, Boolean feedback );

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.setOkButtonText
  */
  void setOkButtonText();

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.centerScheduleOutputLocationDialog
  */
  void centerScheduleOutputLocationDialog();

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.createScheduleParamsDialog
  */
  void createScheduleParamsDialog( String filePath, JSONObject scheduleRequest, Boolean isEmailConfigValid,
                                   Boolean isSchedulesPerspectiveActive );

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.createScheduleEmailDialog
  */
  void createScheduleEmailDialog( String filePath, JSONObject scheduleRequest );

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.getScheduleParams
  */
  String getScheduleParams( JSONObject scheduleRequest );
}