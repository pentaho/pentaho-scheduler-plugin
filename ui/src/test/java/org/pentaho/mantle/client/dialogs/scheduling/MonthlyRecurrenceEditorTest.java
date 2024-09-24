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

package org.pentaho.mantle.client.dialogs.scheduling;

import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pentaho.gwt.widgets.client.utils.TimeUtil;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith( GwtMockitoTestRunner.class )
public class MonthlyRecurrenceEditorTest {
  private RecurrenceEditor.MonthlyRecurrenceEditor monthlyRecurrenceEditor;

  @Before
  public void setUp() throws Exception {
    monthlyRecurrenceEditor = mock( RecurrenceEditor.MonthlyRecurrenceEditor.class );
  }

  @Test
  public void testReset() throws Exception {
    doCallRealMethod().when( monthlyRecurrenceEditor ).reset();

    monthlyRecurrenceEditor.reset();
    verify( monthlyRecurrenceEditor ).setDayNOfMonth();
    verify( monthlyRecurrenceEditor ).setDayOfMonth( "" );
    verify( monthlyRecurrenceEditor ).setWeekOfMonth( TimeUtil.WeekOfMonth.FIRST );
    verify( monthlyRecurrenceEditor ).setDayOfWeek( TimeUtil.DayOfWeek.SUN );
  }

  @Test
  @SuppressWarnings( "deprecation" )
  public void testSetDayNOfMonth() throws Exception {
    doCallRealMethod().when( monthlyRecurrenceEditor ).setDayNOfMonth();

    monthlyRecurrenceEditor.dayNOfMonthRb = mock( RadioButton.class );
    monthlyRecurrenceEditor.nthDayNameOfMonthRb = mock( RadioButton.class );

    monthlyRecurrenceEditor.setDayNOfMonth();
    verify( monthlyRecurrenceEditor.dayNOfMonthRb ).setChecked( true );
    verify( monthlyRecurrenceEditor.nthDayNameOfMonthRb ).setChecked( false );
  }

  @Test
  @SuppressWarnings( "deprecation" )
  public void testSetNthDayNameOfMonth() throws Exception {
    doCallRealMethod().when( monthlyRecurrenceEditor ).setNthDayNameOfMonth();

    monthlyRecurrenceEditor.dayNOfMonthRb = mock( RadioButton.class );
    monthlyRecurrenceEditor.nthDayNameOfMonthRb = mock( RadioButton.class );

    monthlyRecurrenceEditor.setNthDayNameOfMonth();
    verify( monthlyRecurrenceEditor.dayNOfMonthRb ).setChecked( false );
    verify( monthlyRecurrenceEditor.nthDayNameOfMonthRb ).setChecked( true );
  }
}
