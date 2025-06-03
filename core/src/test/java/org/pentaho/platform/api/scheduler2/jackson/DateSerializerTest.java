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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;


public class DateSerializerTest {

  private DateSerializer serializer = new DateSerializer();
  private DateDeserializer deserializer = new DateDeserializer();

  @Test
  public void serializeAndDeserializeWithZFormat() throws IOException, ParseException {
    JsonParser parser = Mockito.mock( JsonParser.class );
    DeserializationContext context = Mockito.mock( DeserializationContext.class );
    JsonGenerator generator = Mockito.mock( JsonGenerator.class );
    SerializerProvider serializerProvider = Mockito.mock( SerializerProvider.class );

    Mockito.when( parser.getText() ).thenReturn( "2023-10-02T12:00:00.000Z" );

    Date date = deserializer.deserialize( parser, context );

    Assert.assertNotNull( date );

    serializer.serialize( date, generator, serializerProvider );

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass( String.class );
    Mockito.verify( generator ).writeString( captor.capture() );
    Assert.assertTrue( captor.getValue().startsWith( "2023-10" ) );
  }

  @Test
  public void serializeAndDeserializeWithTimezoneFormat1() throws IOException, ParseException {
    JsonParser parser = Mockito.mock( JsonParser.class );
    DeserializationContext context = Mockito.mock( DeserializationContext.class );
    JsonGenerator generator = Mockito.mock( JsonGenerator.class );
    SerializerProvider serializerProvider = Mockito.mock( SerializerProvider.class );

    Mockito.when( parser.getText() ).thenReturn( "2023-10-02T12:00:00.000+0000" );

    Date date = deserializer.deserialize( parser, context );

    Assert.assertNotNull( date );

    serializer.serialize( date, generator, serializerProvider );

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass( String.class );
    Mockito.verify( generator ).writeString( captor.capture() );
    Assert.assertTrue( captor.getValue().startsWith( "2023-10" ) );
  }

  @Test
  public void serializeAndDeserializeWithTimezoneFormat2() throws IOException, ParseException {
    JsonParser parser = Mockito.mock( JsonParser.class );
    DeserializationContext context = Mockito.mock( DeserializationContext.class );
    JsonGenerator generator = Mockito.mock( JsonGenerator.class );
    SerializerProvider serializerProvider = Mockito.mock( SerializerProvider.class );

    Mockito.when( parser.getText() ).thenReturn( "2023-10-02T12:00:00.000+01:00" );

    Date date = deserializer.deserialize( parser, context );

    Assert.assertNotNull( date );
    serializer.serialize( date, generator, serializerProvider );

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass( String.class );
    Mockito.verify( generator ).writeString( captor.capture() );
    Assert.assertTrue( captor.getValue().startsWith( "2023-10" ) );
  }
}
