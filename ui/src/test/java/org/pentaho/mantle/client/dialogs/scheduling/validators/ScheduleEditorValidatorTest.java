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
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleEditor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith( GwtMockitoTestRunner.class )
public class ScheduleEditorValidatorTest {

  @Test
  public void testIsValid() throws Exception {
    final ScheduleEditorValidator scheduleEditorValidator = mock( ScheduleEditorValidator.class );
    scheduleEditorValidator.scheduleEditor = mock( ScheduleEditor.class );
    scheduleEditorValidator.recurrenceEditorValidator = mock( RecurrenceEditorValidator.class );
    scheduleEditorValidator.runOnceEditorValidator = mock( RunOnceEditorValidator.class );
    scheduleEditorValidator.cronEditorValidator = mock( CronEditorValidator.class );
    scheduleEditorValidator.blockoutValidator = mock( BlockoutValidator.class );
    doCallRealMethod().when( scheduleEditorValidator ).isValid();

    testRecurrenceEditorValidator( scheduleEditorValidator, scheduleEditorValidator.runOnceEditorValidator,
        ScheduleEditor.ScheduleType.RUN_ONCE );
    testRecurrenceEditorValidator( scheduleEditorValidator, scheduleEditorValidator.recurrenceEditorValidator,
        ScheduleEditor.ScheduleType.SECONDS );
    testRecurrenceEditorValidator( scheduleEditorValidator, scheduleEditorValidator.recurrenceEditorValidator,
        ScheduleEditor.ScheduleType.MINUTES );
    testRecurrenceEditorValidator( scheduleEditorValidator, scheduleEditorValidator.recurrenceEditorValidator,
        ScheduleEditor.ScheduleType.HOURS );
    testRecurrenceEditorValidator( scheduleEditorValidator, scheduleEditorValidator.recurrenceEditorValidator,
        ScheduleEditor.ScheduleType.DAILY );
    testRecurrenceEditorValidator( scheduleEditorValidator, scheduleEditorValidator.recurrenceEditorValidator,
        ScheduleEditor.ScheduleType.WEEKLY );
    testRecurrenceEditorValidator( scheduleEditorValidator, scheduleEditorValidator.recurrenceEditorValidator,
        ScheduleEditor.ScheduleType.MONTHLY );
    testRecurrenceEditorValidator( scheduleEditorValidator, scheduleEditorValidator.recurrenceEditorValidator,
        ScheduleEditor.ScheduleType.YEARLY );
    testRecurrenceEditorValidator( scheduleEditorValidator, scheduleEditorValidator.cronEditorValidator,
        ScheduleEditor.ScheduleType.CRON );
  }

  private void testRecurrenceEditorValidator( ScheduleEditorValidator scheduleEditorValidator, IUiValidator validator,
      ScheduleEditor.ScheduleType type ) {
    when( scheduleEditorValidator.scheduleEditor.isBlockoutDialog() ).thenReturn( false );
    when( scheduleEditorValidator.scheduleEditor.getScheduleType() ).thenReturn( type );
    when( validator.isValid() ).thenReturn( false );
    assertFalse( scheduleEditorValidator.isValid() );

    when( validator.isValid() ).thenReturn( true );
    assertTrue( scheduleEditorValidator.isValid() );

    when( scheduleEditorValidator.scheduleEditor.isBlockoutDialog() ).thenReturn( true );
    assertFalse( scheduleEditorValidator.isValid() );
  }

  @Test
  public void testClear() throws Exception {
    final ScheduleEditorValidator scheduleEditorValidator = mock( ScheduleEditorValidator.class );
    scheduleEditorValidator.recurrenceEditorValidator = mock( RecurrenceEditorValidator.class );
    scheduleEditorValidator.runOnceEditorValidator = mock( RunOnceEditorValidator.class );
    scheduleEditorValidator.cronEditorValidator = mock( CronEditorValidator.class );

    doCallRealMethod().when( scheduleEditorValidator ).clear();

    scheduleEditorValidator.clear();

    verify( scheduleEditorValidator.recurrenceEditorValidator ).clear();
    verify( scheduleEditorValidator.runOnceEditorValidator ).clear();
    verify( scheduleEditorValidator.cronEditorValidator ).clear();
  }
}
