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


package org.pentaho.mantle.client.external.services.definitions;

public interface IContentCleanerPanelUtils {

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.processScheduleTextBoxValue
  */
  String processScheduleTextBoxValue( String jsonJobString );

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.processScheduleTextBoxChangeHandler
  */
  void processScheduleTextBoxChangeHandler( String scheduleTextBoxValue );

  /*
  Should be externalized via JSNI through a function with a name
  $wnd.pho.createScheduleRecurrenceDialog
  */
  void createScheduleRecurrenceDialog( String scheduleValue, String olderThanLabel, String daysLabel );

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
