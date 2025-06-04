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

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateDeserializer extends JsonDeserializer<Date> {

    @Override
    public Date deserialize( JsonParser jsonParser, DeserializationContext deserializationContext ) throws IOException, JacksonException {
        String dateString = jsonParser.getText().trim();

        // Define patterns
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", // format with +hh:mm or -hh:mm
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",   // format with +0000
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"  // format with Z
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat( pattern );
                return sdf.parse( dateString );
            } catch ( Exception e ) {
                // Ignore and try the next pattern
            }
        }

        throw new IOException( "Unable to parse date: " + dateString ); // Throw if none of the formats work
    }
}
