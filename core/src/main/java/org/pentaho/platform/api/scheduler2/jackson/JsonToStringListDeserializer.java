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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JsonToStringListDeserializer extends JsonDeserializer<List<String>> {

  @Override
  public List<String> deserialize( JsonParser jsonParser, DeserializationContext deserializationContext ) throws IOException {

    JsonToken currentToken = jsonParser.getCurrentToken();

    if ( currentToken == JsonToken.START_ARRAY ) {
      List<String> list = new ArrayList<>();
      while ( jsonParser.nextToken() != JsonToken.END_ARRAY ) {
        list.add( jsonParser.getValueAsString() );
      }
      return list;
    } else if ( currentToken == JsonToken.VALUE_STRING
            || currentToken == JsonToken.VALUE_NUMBER_FLOAT || currentToken == JsonToken.VALUE_NUMBER_INT
            || currentToken == JsonToken.VALUE_TRUE || currentToken == JsonToken.VALUE_FALSE ) {
      String value = jsonParser.getValueAsString();
      return Collections.singletonList( value );
    }

    throw JsonMappingException.from( jsonParser, "Expected string or array for field" );
  }
}