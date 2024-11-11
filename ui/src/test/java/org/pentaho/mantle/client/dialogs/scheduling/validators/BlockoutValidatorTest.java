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


package org.pentaho.mantle.client.dialogs.scheduling.validators;

import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pentaho.gwt.widgets.client.controls.TimePicker;
import org.pentaho.gwt.widgets.client.utils.TimeUtil;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleEditor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( GwtMockitoTestRunner.class )
public class BlockoutValidatorTest {

  @Test
  public void testIsValid() throws Exception {
    final ScheduleEditor scheduleEditor = mock( ScheduleEditor.class );
    final BlockoutValidator validator = new BlockoutValidator( scheduleEditor );

    when( scheduleEditor.getBlockoutEndsType() ).thenReturn( ScheduleEditor.ENDS_TYPE.DURATION );
    ScheduleEditor.DurationValues durationValues = new ScheduleEditor.DurationValues();
    when( scheduleEditor.getDurationValues() ).thenReturn( durationValues );
    assertFalse( validator.isValid() );

    durationValues.days = 1;
    assertTrue( validator.isValid() );

    when( scheduleEditor.getBlockoutEndsType() ).thenReturn( ScheduleEditor.ENDS_TYPE.TIME );
    TimePicker startTimePicker = mock( TimePicker.class );
    when( startTimePicker.getHour() ).thenReturn( "8" );
    when( startTimePicker.getMinute() ).thenReturn( "8" );
    TimeUtil.TimeOfDay timeOfDayPM = TimeUtil.TimeOfDay.PM;
    when( startTimePicker.getTimeOfDay() ).thenReturn( timeOfDayPM );
    when( scheduleEditor.getStartTimePicker() ).thenReturn( startTimePicker );

    TimePicker endTimePicker = mock( TimePicker.class );
    when( endTimePicker.getHour() ).thenReturn( "8" );
    when( endTimePicker.getMinute() ).thenReturn( "8" );
    when( endTimePicker.getTimeOfDay() ).thenReturn( timeOfDayPM );
    when( scheduleEditor.getBlockoutEndTimePicker() ).thenReturn( endTimePicker );
    assertFalse( validator.isValid() ); // equal start/end

    when( endTimePicker.getMinute() ).thenReturn( "7" );
    assertFalse( validator.isValid() ); // start after end

    when( endTimePicker.getMinute() ).thenReturn( "9" );
    assertTrue( validator.isValid() ); // end after start
  }
}
