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

package org.pentaho.mantle.client.workspace;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( GwtMockitoTestRunner.class )
public class SchedulerUiUtilTest {
  @Test( expected = IllegalArgumentException.class )
  public void getMapFromJSONResponseNullObject() {
    SchedulerUiUtil.getMapFromJSONResponse( null, "key" );
  }

  @Test( expected = IllegalArgumentException.class )
  public void getMapFromJSONResponseNullKey() {
    SchedulerUiUtil.getMapFromJSONResponse( new JSONObject(), null );
  }

  @Test( expected = IllegalArgumentException.class )
  public void getMapFromJSONResponseNullObjectAndKey() {
    SchedulerUiUtil.getMapFromJSONResponse( null, null );
  }

  @Test
  public void getMapFromJSONResponseNotFoundKeyEmptyObject() {
    Map<String, String> result = SchedulerUiUtil.getMapFromJSONResponse( new JSONObject(), "key" );
    assertNotNull( result );
    assertTrue( result.isEmpty() );
  }

  @Test( expected = IllegalArgumentException.class )
  public void getMapFromJSONResponseNotFoundKeyNormalObject() {
    SchedulerUiUtil.getMapFromJSONResponse( createJSONObject(), "key" );
  }

  @Test
  public void getMapFromJSONResponseNormalKeyNormalObject() {
    Map<String, String> result =
      SchedulerUiUtil.getMapFromJSONResponse( createJSONObject(), "changes" );
    assertNotNull( result );
    assertFalse( result.isEmpty() );
    assertEquals( 1, result.size() );
    assertEquals( "valueValue", result.get( "keyValue" ) );
  }

  @Test( expected = IllegalArgumentException.class )
  public void getMapFromJSONResponseNotFoundKeyArrayObject() {
    SchedulerUiUtil.getMapFromJSONResponse( createJSONArray(), "key" );
  }

  @Test
  public void getMapFromJSONResponseNormalKeyArrayObject() {
    Map<String, String> result = SchedulerUiUtil.getMapFromJSONResponse( createJSONArray(), "changes" );
    assertNotNull( result );
    assertFalse( result.isEmpty() );
    assertEquals( 3, result.size() );
    assertEquals( "valueValue1", result.get( "keyValue1" ) );
    assertEquals( "valueValue2", result.get( "keyValue2" ) );
    assertEquals( "valueValue3", result.get( "keyValue3" ) );
  }

  private JSONObject createJSONObject() {
    JSONObject entryObj = mock( JSONObject.class );
    when( entryObj.get( "key" ) ).thenReturn( new JSONString( "keyValue" ) );
    when( entryObj.get( "value" ) ).thenReturn( new JSONString( "valueValue" ) );

    JSONValue entryValue = mock( JSONValue.class );
    when( entryValue.isArray() ).thenReturn( null );
    when( entryValue.isObject() ).thenReturn( entryObj );

    JSONObject changesObj = mock( JSONObject.class );
    when( changesObj.get( "entry" ) ).thenReturn( entryValue );

    JSONValue changesValue = mock( JSONValue.class );
    when( changesValue.isObject() ).thenReturn( changesObj );

    JSONObject result = mock( JSONObject.class );
    when( result.get( "changes" ) ).thenReturn( changesValue );

    return result;
  }

  private JSONObject createJSONArray() {
    JSONObject entryObj1 = mock( JSONObject.class );
    when( entryObj1.get( "key" ) ).thenReturn( new JSONString( "keyValue1" ) );
    when( entryObj1.get( "value" ) ).thenReturn( new JSONString( "valueValue1" ) );

    JSONObject entryObj2 = mock( JSONObject.class );
    when( entryObj2.get( "key" ) ).thenReturn( new JSONString( "keyValue2" ) );
    when( entryObj2.get( "value" ) ).thenReturn( new JSONString( "valueValue2" ) );

    JSONObject entryObj3 = mock( JSONObject.class );
    when( entryObj3.get( "key" ) ).thenReturn( new JSONString( "keyValue3" ) );
    when( entryObj3.get( "value" ) ).thenReturn( new JSONString( "valueValue3" ) );

    JSONValue entryValue1 = mock( JSONValue.class );
    when( entryValue1.isArray() ).thenReturn( null );
    when( entryValue1.isObject() ).thenReturn( entryObj1 );

    JSONValue entryValue2 = mock( JSONValue.class );
    when( entryValue2.isArray() ).thenReturn( null );
    when( entryValue2.isObject() ).thenReturn( entryObj2 );

    JSONValue entryValue3 = mock( JSONValue.class );
    when( entryValue3.isArray() ).thenReturn( null );
    when( entryValue3.isObject() ).thenReturn( entryObj3 );

    JSONArray entryArray = mock( JSONArray.class );
    when( entryArray.size() ).thenReturn( 3 );
    when( entryArray.get( 0 ) ).thenReturn( entryValue1 );
    when( entryArray.get( 1 ) ).thenReturn( entryValue2 );
    when( entryArray.get( 2 ) ).thenReturn( entryValue3 );

    JSONValue entryValue = mock( JSONValue.class );
    when( entryValue.isArray() ).thenReturn( entryArray );
    when( entryValue.isObject() ).thenReturn( null );

    JSONObject changesObj = mock( JSONObject.class );
    when( changesObj.get( "entry" ) ).thenReturn( entryValue );

    JSONValue changesValue = mock( JSONValue.class );
    when( changesValue.isObject() ).thenReturn( changesObj );

    JSONObject result = mock( JSONObject.class );
    when( result.get( "changes" ) ).thenReturn( changesValue );

    return result;
  }
}
