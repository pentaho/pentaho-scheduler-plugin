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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JsonToStringListDeserializerTest {

  private ObjectMapper mapper;

  @Before
  public void setUp() {
    mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addDeserializer( List.class, new JsonToStringListDeserializer() );
    mapper.registerModule( module );
  }

  @Test
  public void testDeserializeStringValue() throws Exception {
    String json = "\"singleValue\"";

    List<String> result = mapper.readValue( json, new TypeReference<>() {} );

    assertNotNull(result);
    assertEquals( 1, result.size() );
    assertEquals( "singleValue", result.get( 0 ) );
  }

  @Test
  public void testDeserializeArrayValue() throws Exception {
    String json = "[\"one\", \"two\", \"three\"]";

    List<String> result = mapper.readValue( json, new TypeReference<>() {} );

    assertNotNull( result );
    assertEquals( 3, result.size() );
    assertEquals( List.of("one", "two", "three"), result );
  }

  @Test
  public void testDeserializeNumberValue() throws Exception {
    String json = "123";

    List<String> result = mapper.readValue( json, new TypeReference<>() {} );

    assertNotNull( result );
    assertEquals( 1, result.size() );
    assertEquals( List.of( "123" ), result );
  }

  @Test
  public void testDeserializeBooleanValue() throws Exception {
    String json = "false";

    List<String> result = mapper.readValue( json, new TypeReference<>() {} );

    assertNotNull( result );
    assertEquals( 1, result.size() );
    assertEquals( List.of( "false" ), result );
  }

  @Test
  public void testDeserializeNumberArrayValue() throws Exception {
    String json = "[ 123, 234, 345 ]";

    List<String> result = mapper.readValue( json, new TypeReference<>() {} );

    assertNotNull( result );
    assertEquals( 3, result.size() );
    assertEquals( List.of( "123", "234", "345" ), result );
  }
}
