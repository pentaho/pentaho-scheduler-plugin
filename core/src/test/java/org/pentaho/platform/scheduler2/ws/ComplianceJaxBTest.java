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


package org.pentaho.platform.scheduler2.ws;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import jakarta.xml.bind.JAXBException;

import org.junit.Test;
import org.pentaho.platform.api.scheduler2.JobState;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.scheduler2.ws.JaxBSafeMap.JaxBSafeEntry;

import junit.framework.Assert;

@SuppressWarnings( "nls" )
public class ComplianceJaxBTest {

  @Test
  public void testJaxbSafeMap() throws JAXBException {
    HashMap<String, ParamValue> params = new HashMap<String, ParamValue>();
    ListParamValue listValue = new ListParamValue();
    listValue.add( "testListVal0" );
    listValue.add( "testListVal1" );
    MapParamValue mapValue = new MapParamValue();
    mapValue.put( "testMapValueKey", "testMapVal" );

    params.put( "testStringkey", new StringParamValue( "testStringVal" ) );
    params.put( "testListKey", listValue );
    params.put( "testMapKey", mapValue );

    // TreeMap implements java.util.SortedMap; applies a natural ordering of its keys;
    Map<String, ParamValue> sortedParams = new TreeMap( params );
    List paramsList = java.util.Arrays.asList( sortedParams.keySet().toArray() );

    JaxBSafeMap map = new JaxBSafeMap( sortedParams );

    JaxBSafeMap unmarshalled = JaxBUtil.outin( map, JaxBSafeMap.class, JaxBSafeEntry.class, StringParamValue.class );

    Assert.assertEquals( "testStringVal", unmarshalled.entry.get( paramsList.indexOf( "testStringkey" ) ).getStringValue().toString() );
    Assert.assertEquals( "testListVal0", unmarshalled.entry.get( paramsList.indexOf( "testListKey" ) ).getListValue().get( 0 ) );
    Assert.assertEquals( "testMapVal", unmarshalled.entry.get( paramsList.indexOf( "testMapKey" ) ).getMapValue().get( "testMapValueKey" ) );
  }

  @Test
  public void testParamValue() throws JAXBException {
    ParamValue val = new StringParamValue( "testval" );

    ParamValue unmarshalled = JaxBUtil.outin( val, StringParamValue.class );
    Assert.assertEquals( "testval", unmarshalled.toString() );
  }

  @Test
  public void testJaxbSafeJob() throws JAXBException {
    JobAdapter.JaxbSafeJob job = new JobAdapter.JaxbSafeJob();
    final Date NOW = new Date();
    job.lastRun = NOW;
    job.nextRun = NOW;
    job.jobName = "testName";
    job.jobId = "testId";
    job.schedulableClass = "test.schedulable.class";
    job.state = JobState.COMPLETE;
    job.userName = "testUsername";

    HashMap<String, ParamValue> params = new HashMap<String, ParamValue>();
    params.put( "testStringkey", new StringParamValue( "testStringVal" ) );

    JaxBSafeMap safeMap = new JaxBSafeMap( params );
    job.jobParams = safeMap;

    JobAdapter.JaxbSafeJob unmarshalledJob = JaxBUtil.outin( job, JobAdapter.JaxbSafeJob.class );
    Assert.assertEquals( job.lastRun, unmarshalledJob.lastRun );
    Assert.assertEquals( job.nextRun, unmarshalledJob.nextRun );
    Assert.assertEquals( job.jobName, unmarshalledJob.jobName );
    Assert.assertEquals( job.jobId, unmarshalledJob.jobId );
    Assert.assertEquals( job.schedulableClass, unmarshalledJob.schedulableClass );
    Assert.assertEquals( job.userName, unmarshalledJob.userName );
    Assert.assertEquals( job.state, unmarshalledJob.state );
    Assert.assertEquals( job.jobTrigger, unmarshalledJob.jobTrigger );
    Assert.assertTrue( "testStringkey".equals( unmarshalledJob.jobParams.entry.get( 0 ).key ) );
    Assert.assertTrue( "testStringVal".equals( unmarshalledJob.jobParams.entry.get( 0 ).getStringValue().toString() ) );
  }

  @Test
  public void testSimpleTrigger() throws JAXBException {
    SimpleJobTrigger orig = new SimpleJobTrigger();
    Date STARTTIME = new Date();
    orig.setStartTime( STARTTIME );

    SimpleJobTrigger unmarshalled = JaxBUtil.outin( orig, SimpleJobTrigger.class );

    Assert.assertEquals( orig.getStartTime(), unmarshalled.getStartTime() );
  }
}
