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

package org.pentaho.platform.api.scheduler2.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateSerializer extends JsonSerializer<Date> {

  @Override
  public void serialize( Date date, JsonGenerator jsonGenerator, SerializerProvider serializerProvider ) throws IOException {
    String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    SimpleDateFormat sdf = new SimpleDateFormat( pattern );
    String formattedDate = sdf.format( date );

    jsonGenerator.writeString( formattedDate );
  }
}
