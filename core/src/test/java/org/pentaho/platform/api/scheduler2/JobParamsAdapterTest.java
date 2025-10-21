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


package org.pentaho.platform.api.scheduler2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JobParamsAdapterTest {
  @Test
  public void testMarshal() throws Exception {
    JobParamsAdapter adapter = new JobParamsAdapter();

    Map<String, Object> dataMap = new HashMap<String, Object>();
    dataMap.put( "a", "A" );
    dataMap.put( "bb", "[B]" );
    dataMap.put( "ccc", "[C].[CCC]" );
    dataMap.put( "dddd", "[D].[DDD,ddd]" );
    dataMap.put( "eeeee", null );
    dataMap.put( null, "FFFFFF" );
    JobParams expectedJobParams = createJobParams( new JobParam[]{
      createJobParam( "a", "A" ),
      createJobParam( "bb", "[B]" ),
      createJobParam( "ccc", "[C].[CCC]" ),
      createJobParam( "dddd", "[D].[DDD,ddd]" )
    } );

    final JobParams resultJobParams = adapter.marshal( dataMap );

    assertNotNull( resultJobParams );
    assertNotNull( resultJobParams.jobParams );
    java.util.Arrays.sort( resultJobParams.jobParams, new JobParamWholeComparator() );
    assertJobParamArrayEquals( "", expectedJobParams.jobParams, resultJobParams.jobParams );
  }

  @Test
  public void testMarshalMultiValue() throws Exception {
    JobParamsAdapter adapter = new JobParamsAdapter();

    Map<String, Object> dataMap = new HashMap<String, Object>();
    dataMap.put( "a", "A" );
    dataMap.put( "bb", "[B]" );
    ArrayList<String> cValue = castAsArrayList( new String[] { "[C].[CCC]", "[D].[DDD,ddd]" } );
    dataMap.put( "ccc", cValue );
    ArrayList<String> eValue = castAsArrayList( new String[] { null, "FFFFFF" } );
    dataMap.put( "eeeee", eValue );
    dataMap.put( "ffffff", new String[] { "val1", "val2", "val3" } );
    JobParams expectedJobParams = createJobParams( new JobParam[]{
      createJobParam( "a", "A" ),
      createJobParam( "bb", "[B]" ),
      createJobParam( "ccc", "[C].[CCC]" ),
      createJobParam( "ccc", "[D].[DDD,ddd]" ),
      createJobParam( "eeeee", "FFFFFF" ),
      createJobParam( "ffffff", "val1" ),
      createJobParam( "ffffff", "val2" ),
      createJobParam( "ffffff", "val3" )
    } );

    final JobParams resultJobParams = adapter.marshal( dataMap );

    assertNotNull( resultJobParams );
    assertNotNull( resultJobParams.jobParams );
    Arrays.sort( resultJobParams.jobParams, new JobParamWholeComparator() );
    assertJobParamArrayEquals( "", expectedJobParams.jobParams, resultJobParams.jobParams );
  }

  @Test
  public void testMarshalRemovesVariableDuplicate() throws Exception {
    JobParamsAdapter adapter = new JobParamsAdapter();

    Map<String, Object> dataMap = new HashMap<String, Object>();
    HashMap<String, String> variables = new HashMap<>();
    variables.put( "test1", "val1" );
    variables.put( "test2", "val2" );
    HashMap<String, String> parameters = new HashMap<>();
    parameters.put( "test2", "val2Updated" );

    dataMap.put( "variables", variables );
    dataMap.put( "parameters", parameters );
    dataMap.put( "test3", "val3" );


    JobParams expectedJobParams = createJobParams( new JobParam[]{
      createJobParam( "test1", "val1" ),
      createJobParam( "test2", "val2Updated" ),
      createJobParam( "test3", "val3" )
    } );

    final JobParams resultJobParams = adapter.marshal( dataMap );

    assertNotNull( resultJobParams );
    assertNotNull( resultJobParams.jobParams );

    Arrays.sort( resultJobParams.jobParams, new JobParamWholeComparator() );
    assertJobParamArrayEquals( "", expectedJobParams.jobParams, resultJobParams.jobParams );
  }

  private void assertJobParamArrayEquals( String msg, JobParam[] expected, final JobParam[] actual ) {
    Assert.assertNotNull( msg + " null", actual );
    Assert.assertEquals( msg + " length", expected.length, actual.length );
    for ( int i = 0; i < expected.length; i++ ) {
      assertJobParamEquals( msg + " [" + i + "]", expected[i], actual[i] );
    }
  }

  private void assertJobParamEquals( String msg, final JobParam expected, final JobParam actual ) {
    Assert.assertEquals( msg + " .name", expected.name, actual.name );
    Assert.assertEquals( msg + " .value", expected.value, actual.value );
  }

  @Test
  public void testUnmarshal() throws Exception {
    JobParamsAdapter adapter = new JobParamsAdapter();

    Map<String, Object> expectedDataMap = new HashMap<String, Object>();
    expectedDataMap.put( "a", "A" );
    expectedDataMap.put( "bb", "[B]" );
    expectedDataMap.put( "ccc", "[C].[CCC]" );
    expectedDataMap.put( "dddd", "[D].[DDD,ddd]" );
    JobParams dataJobParams = createJobParams( new JobParam[]{
      createJobParam( "a", "A" ),
      createJobParam( "bb", "[B]" ),
      createJobParam( "ccc", "[C].[CCC]" ),
      createJobParam( "dddd", "[D].[DDD,ddd]" )
    } );

    Map<String, Object> resultMap = adapter.unmarshal( dataJobParams );

    Assert.assertNotNull( "resultMap", resultMap );
    Assert.assertEquals( "resultMap.size", expectedDataMap.size(), resultMap.size() );
    for ( String key : expectedDataMap.keySet() ) {
      assertEquals( "resultMap[" + key + "]", expectedDataMap.get( key ), resultMap.get( key ) );
    }
  }

  @Test
  public void testUnmarshalMultiValue() throws Exception {
    JobParamsAdapter adapter = new JobParamsAdapter();

    Map<String, Object> expectedDataMap = new HashMap<String, Object>();
    expectedDataMap.put( "a", "A" );
    expectedDataMap.put( "bb", "[B]" );
    final ArrayList<String> cValue = castAsArrayList( new String[] { "[C].[CCC]", "[D].[DDD,ddd]" } );
    expectedDataMap.put( "ccc", cValue );
    JobParams dataJobParams = createJobParams( new JobParam[]{
      createJobParam( "a", "A" ),
      createJobParam( "bb", "[B]" ),
      createJobParam( "ccc", "[C].[CCC]" ),
      createJobParam( "ccc", "[D].[DDD,ddd]" )
    } );

    Map<String, Object> resultMap = adapter.unmarshal( dataJobParams );

    Assert.assertNotNull( "resultMap", resultMap );
    Assert.assertEquals( "resultMap.size", expectedDataMap.size(), resultMap.size() );
    for ( String key : new String[] { "a", "bb" } ) {
      assertEquals( "resultMap[" + key + "]", expectedDataMap.get( key ), resultMap.get( key ) );
    }

    String key = "ccc";
    assertTrue( "resultMap[" + key + "] is collection", resultMap.get( key ) instanceof Collection );
    Collection<?> actualCValue = (Collection<?>) resultMap.get( key );
    assertEquals( "resultMap[" + key + "].size", cValue.size(), actualCValue.size() );
    assertTrue( "resultMap[" + key + "] all expected values", cValue.containsAll( actualCValue ) );

  }

  JobParam createJobParam( String n, String v ) {
    JobParam r = new JobParam();
    r.name = n;
    r.value = v;
    return r;
  }

  JobParams createJobParams( JobParam[] v ) {
    JobParams r = new JobParams();
    r.jobParams = v;
    return r;
  }

  static class JobParamWholeComparator implements Comparator<JobParam> {

    @Override
    public int compare( JobParam arg0, JobParam arg1 ) {
      int r = arg0.name.compareTo( arg1.name );
      if ( r != 0 ) {
        return r;
      }
      return arg0.value.compareTo( arg1.value );
    }

  }
  ArrayList<String> castAsArrayList( String[] values ) {
    if ( values == null ) {
      return null;
    }
    ArrayList<String> list = new ArrayList<String>( values.length );
    for ( String v: values ) {
      list.add( v );
    }
    return list;
  }

}
