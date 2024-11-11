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


package org.pentaho.mantle.client.dialogs.scheduling;

import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pentaho.gwt.widgets.client.controls.DatePickerEx;
import org.pentaho.gwt.widgets.client.controls.TimePicker;
import org.pentaho.gwt.widgets.client.utils.TimeUtil;

import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith( GwtMockitoTestRunner.class )
public class RunOnceEditorTest {
  private RunOnceEditor runOnceEditor;

  @Before
  public void setUp() throws Exception {
    runOnceEditor = mock( RunOnceEditor.class );
  }

  @Test
  @SuppressWarnings( "deprecation" )
  public void testReset() throws Exception {
    doCallRealMethod().when( runOnceEditor ).reset( any( Date.class ) );

    runOnceEditor.startTimePicker = mock( TimePicker.class );
    runOnceEditor.startDatePicker = mock( DatePickerEx.class );
    final DateBox dateBox = mock( DateBox.class );
    when( runOnceEditor.startDatePicker.getDatePicker() ).thenReturn( dateBox );

    final Date date = new Date();
    runOnceEditor.reset( date );
    verify( runOnceEditor.startTimePicker ).setTimeOfDay( TimeUtil.getTimeOfDayBy0To23Hour( date.getHours() ) );
    verify( runOnceEditor.startTimePicker ).setHour( TimeUtil.to12HourClock( date.getHours() ) );
    verify( runOnceEditor.startTimePicker ).setMinute( date.getMinutes() );
    verify( dateBox ).setValue( date );
  }
}
