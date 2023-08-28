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

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import org.pentaho.gwt.widgets.client.dialogs.IDialogCallback;

public interface IContentCleanerPanelUtils {

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.processScheduleTextBox
  */
  void processScheduleTextBox( String jsonJobString, TextBox scheduleTextBox );

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.createScheduleRecurrenceDialog
  */
  void createScheduleRecurrenceDialog( HorizontalPanel scheduleLabelPanel, IDialogCallback callback );

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.getJobId
  */
  String getJobId();

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.isFakeJob
  */
  boolean isFakeJob();

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.getJobDescription();
  */
  String getJobDescription();
}
