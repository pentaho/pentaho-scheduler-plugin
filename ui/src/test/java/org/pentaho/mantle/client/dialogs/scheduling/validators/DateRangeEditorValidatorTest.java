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

import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pentaho.gwt.widgets.client.controls.DateRangeEditor;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith( GwtMockitoTestRunner.class )
public class DateRangeEditorValidatorTest {

  @Test
  public void testIsValid() throws Exception {
    final DateRangeEditor dateRangeEditor = mock( DateRangeEditor.class );
    final DateRangeEditorValidator validator = new DateRangeEditorValidator( dateRangeEditor );

    when( dateRangeEditor.getStartDate() ).thenReturn( null );
    assertFalse( validator.isValid() );

    when( dateRangeEditor.getStartDate() ).thenReturn( new Date() );
    when( dateRangeEditor.isEndBy() ).thenReturn( true );
    when( dateRangeEditor.getEndDate() ).thenReturn( null );
    assertFalse( validator.isValid() );

    when( dateRangeEditor.getEndDate() ).thenReturn( new Date() );
    assertTrue( validator.isValid() );

    when( dateRangeEditor.isEndBy() ).thenReturn( false );
    when( dateRangeEditor.getEndDate() ).thenReturn( null );
    assertTrue( validator.isValid() );
  }

  @Test
  public void testClear() throws Exception {
    final DateRangeEditor dateRangeEditor = mock( DateRangeEditor.class );
    final DateRangeEditorValidator validator = new DateRangeEditorValidator( dateRangeEditor );

    validator.clear();
    verify( dateRangeEditor ).setStartDateError( null );
    verify( dateRangeEditor ).setEndByError( null );
  }
}
