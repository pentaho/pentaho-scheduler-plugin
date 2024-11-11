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

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith( GwtMockitoTestRunner.class )
public class DailyRecurrenceEditorTest {
  private RecurrenceEditor.DailyRecurrenceEditor dailyRecurrenceEditor;

  @Before
  public void setUp() throws Exception {
    dailyRecurrenceEditor = mock( RecurrenceEditor.DailyRecurrenceEditor.class );
  }

  @Test
  public void testReset() throws Exception {
    doCallRealMethod().when( dailyRecurrenceEditor ).reset();

    dailyRecurrenceEditor.reset();
    verify( dailyRecurrenceEditor ).setDailyRepeatValue( "" );
    verify( dailyRecurrenceEditor ).setEveryNDays();
  }

  @Test
   @SuppressWarnings( "deprecation" )
   public void testSetEveryNDays() throws Exception {
    doCallRealMethod().when( dailyRecurrenceEditor ).setEveryNDays();

    dailyRecurrenceEditor.everyNDaysRb = mock( RadioButton.class );
    dailyRecurrenceEditor.everyWeekdayRb = mock( RadioButton.class );
    dailyRecurrenceEditor.ignoreDTSCb = mock( CheckBox.class );

    dailyRecurrenceEditor.setEveryNDays();
    verify( dailyRecurrenceEditor.everyNDaysRb ).setChecked( true );
    verify( dailyRecurrenceEditor.everyWeekdayRb ).setChecked( false );
  }

  @Test
  @SuppressWarnings( "deprecation" )
  public void testSetEveryWeekday() throws Exception {
    doCallRealMethod().when( dailyRecurrenceEditor ).setEveryWeekday();

    dailyRecurrenceEditor.everyNDaysRb = mock( RadioButton.class );
    dailyRecurrenceEditor.everyWeekdayRb = mock( RadioButton.class );
    dailyRecurrenceEditor.ignoreDTSCb = mock( CheckBox.class );
    dailyRecurrenceEditor.setEveryWeekday();
    verify( dailyRecurrenceEditor.everyNDaysRb ).setChecked( false );
    verify( dailyRecurrenceEditor.everyWeekdayRb ).setChecked( true );
  }
}
