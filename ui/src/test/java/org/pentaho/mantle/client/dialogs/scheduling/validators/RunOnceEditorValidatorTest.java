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

package org.pentaho.mantle.client.dialogs.scheduling.validators;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pentaho.mantle.client.dialogs.scheduling.RunOnceEditor;

import java.util.Calendar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( GwtMockitoTestRunner.class )
public class RunOnceEditorValidatorTest {

  @Test
  public void testIsValid() throws Exception {
    final RunOnceEditor runOnceEditor = mock( RunOnceEditor.class );
    final RunOnceEditorValidator validator = new RunOnceEditorValidator( runOnceEditor );

    when( runOnceEditor.getStartDate() ).thenReturn( null );
    assertFalse( validator.isValid() );

    Calendar calendar = Calendar.getInstance();
    calendar.add( Calendar.SECOND, -1 );
    when( runOnceEditor.getStartDate() ).thenReturn( calendar.getTime() );
    String startTime = DateTimeFormat.getFormat( "hh:mm:ss a" ).
      format( calendar.getTime() ).toLowerCase();
    when( runOnceEditor.getStartTime() ).thenReturn( startTime );
    assertFalse( validator.isValid() );

    calendar.add( Calendar.MINUTE, 1 );
    when( runOnceEditor.getStartDate() ).thenReturn( calendar.getTime() );
    when( runOnceEditor.getStartTime() ).thenReturn( DateTimeFormat.getFormat( "hh:mm:ss a" ).
      format( calendar.getTime() ).toLowerCase() );
    assertTrue( validator.isValid() );
  }
}