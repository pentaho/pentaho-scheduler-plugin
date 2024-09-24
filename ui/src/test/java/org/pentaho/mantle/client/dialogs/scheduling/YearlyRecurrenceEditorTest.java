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

import static org.mockito.Mockito.*;

@RunWith( GwtMockitoTestRunner.class )
public class YearlyRecurrenceEditorTest {
  private RecurrenceEditor.YearlyRecurrenceEditor yearlyRecurrenceEditor;

  @Before
  public void setUp() throws Exception {
    yearlyRecurrenceEditor = mock( RecurrenceEditor.YearlyRecurrenceEditor.class );
  }

  @Test
  public void testReset() throws Exception {
    doCallRealMethod().when( yearlyRecurrenceEditor ).reset();

    yearlyRecurrenceEditor.reset();
    verify( yearlyRecurrenceEditor ).setEveryMonthOnNthDay();
    verify( yearlyRecurrenceEditor ).setMonthOfYear0( TimeUtil.MonthOfYear.JAN );
    verify( yearlyRecurrenceEditor ).setDayOfMonth( "" );
    verify( yearlyRecurrenceEditor ).setWeekOfMonth( TimeUtil.WeekOfMonth.FIRST );
    verify( yearlyRecurrenceEditor ).setDayOfWeek( TimeUtil.DayOfWeek.SUN );
    verify( yearlyRecurrenceEditor ).setMonthOfYear1( TimeUtil.MonthOfYear.JAN );
  }

  @Test
  @SuppressWarnings( "deprecation" )
  public void testSetEveryMonthOnNthDay() throws Exception {
    doCallRealMethod().when( yearlyRecurrenceEditor ).setEveryMonthOnNthDay();

    yearlyRecurrenceEditor.everyMonthOnNthDayRb = mock( RadioButton.class );
    yearlyRecurrenceEditor.nthDayNameOfMonthNameRb = mock( RadioButton.class );

    yearlyRecurrenceEditor.setEveryMonthOnNthDay();
    verify( yearlyRecurrenceEditor.everyMonthOnNthDayRb ).setChecked( true );
    verify( yearlyRecurrenceEditor.nthDayNameOfMonthNameRb ).setChecked( false );
  }

  @Test
  @SuppressWarnings( "deprecation" )
  public void testSetNthDayNameOfMonthName() throws Exception {
    doCallRealMethod().when( yearlyRecurrenceEditor ).setNthDayNameOfMonthName();

    yearlyRecurrenceEditor.everyMonthOnNthDayRb = mock( RadioButton.class );
    yearlyRecurrenceEditor.nthDayNameOfMonthNameRb = mock( RadioButton.class );

    yearlyRecurrenceEditor.setNthDayNameOfMonthName();
    verify( yearlyRecurrenceEditor.everyMonthOnNthDayRb ).setChecked( false );
    verify( yearlyRecurrenceEditor.nthDayNameOfMonthNameRb ).setChecked( true );
  }
}
