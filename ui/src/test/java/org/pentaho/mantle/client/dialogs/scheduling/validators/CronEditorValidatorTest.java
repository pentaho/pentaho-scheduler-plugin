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
import org.pentaho.mantle.client.dialogs.scheduling.CronEditor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith( GwtMockitoTestRunner.class )
public class CronEditorValidatorTest {

  @Test
  public void testIsValid() throws Exception {
    final CronEditor editor = mock( CronEditor.class );
    CronEditorValidator cronEditorValidator = new CronEditorValidator( editor );
    cronEditorValidator.dateRangeEditorValidator = mock( DateRangeEditorValidator.class );

    when( editor.getCronString() ).thenReturn( "" );
    when( cronEditorValidator.dateRangeEditorValidator.isValid() ).thenReturn( true );
    assertFalse( cronEditorValidator.isValid() );

    when( editor.getCronString() ).thenReturn( "0 33 6 ? * 1" );
    when( cronEditorValidator.dateRangeEditorValidator.isValid() ).thenReturn( false );
    assertFalse( cronEditorValidator.isValid() );

    when( cronEditorValidator.dateRangeEditorValidator.isValid() ).thenReturn( true );
    assertTrue( cronEditorValidator.isValid() );
  }

  @Test
  public void testClear() throws Exception {
    final CronEditor editor = mock( CronEditor.class );
    CronEditorValidator cronEditorValidator = new CronEditorValidator( editor );
    cronEditorValidator.dateRangeEditorValidator = mock( DateRangeEditorValidator.class );

    cronEditorValidator.clear();
    verify( editor ).setCronError( null );
    verify( cronEditorValidator.dateRangeEditorValidator ).clear();
  }
}
