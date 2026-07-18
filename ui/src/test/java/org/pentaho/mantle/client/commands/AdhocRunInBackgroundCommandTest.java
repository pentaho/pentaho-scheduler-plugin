/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/



package org.pentaho.mantle.client.commands;

import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith( GwtMockitoTestRunner.class )
public class AdhocRunInBackgroundCommandTest {

  private static final String OUTPUT_NAME = "myReport";
  private static final String FORMATTED_DATE = "_20260703";

  private AdhocRunInBackgroundCommand command;

  @Before
  public void setUp() {
    // Override the date-formatting seam so the file-name composition is deterministic and independent of the
    // current time and GWT date formatting.
    command = new AdhocRunInBackgroundCommand() {
      @Override
      protected String formatDate( String dateFormat ) {
        return FORMATTED_DATE;
      }
    };
    command.setOutputName( OUTPUT_NAME );
  }

  @Test
  public void appendsTimestampWhenDateFormatSelected() {
    command.setDateFormat( "yyyyMMdd" );

    assertEquals( OUTPUT_NAME + FORMATTED_DATE, command.getOutputFileName() );
  }

  @Test
  public void returnsPlainNameWhenDateFormatEmpty() {
    command.setDateFormat( "" );

    assertEquals( OUTPUT_NAME, command.getOutputFileName() );
  }

  @Test
  public void returnsPlainNameWhenDateFormatNull() {
    command.setDateFormat( null );

    assertEquals( OUTPUT_NAME, command.getOutputFileName() );
  }
}
