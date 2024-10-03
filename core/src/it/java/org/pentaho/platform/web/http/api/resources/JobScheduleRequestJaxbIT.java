///*! ******************************************************************************
// *
// * Pentaho
// *
// * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
// *
// * Use of this software is governed by the Business Source License included
// * in the LICENSE.TXT file.
// *
// * Change Date: 2029-07-20
// ******************************************************************************/
//
//
//package org.pentaho.platform.web.http.api.resources;
//
//import com.sun.jersey.api.json.JSONJAXBContext;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.pentaho.platform.JaxbContextResolver; // TODO update to pentaho-platform-extension class org.pentaho.platform.web.http.api.resources.JaxbContextResolver
//import org.pentaho.platform.api.scheduler2.IJobScheduleParam;
//
//import javax.xml.bind.JAXBContext;
//import javax.xml.bind.Unmarshaller;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.URL;
//import java.util.List;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
///**
// * Integration Test to verify JAXB annotations for unmarshalling
// * {@link org.pentaho.platform.web.http.api.resources.JobScheduleRequest}
// */
//public class JobScheduleRequestJaxbIT {
//
//  protected static JaxbContextResolver jaxbContextResolver;
//
//  @BeforeClass
//  public static void setup() throws Exception {
//    jaxbContextResolver = new JaxbContextResolver();
//  }
//
//  @Test
//  public void testJaxbJson() throws Exception {
//
//    //Overloaded methods to unmarshal from different xml sources
//    String jsonFileName = "jaxb/JobScheduleRequest_update.json";
//    URL url = getClass().getClassLoader().getResource( jsonFileName );
//    JobScheduleRequest jobScheduleRequest = readFrom( JobScheduleRequest.class, url.openStream() );
//
//    //asserts
//    assertEquals( "Top Customers (report)-manual-edit-", jobScheduleRequest.getJobName() );
//    assertEquals("YEARLY", jobScheduleRequest.getComplexJobTrigger().getUiPassParam());
//    assertEquals( "/public/Steel Wheels/Top Customers (report).prpt", jobScheduleRequest.getInputFile() );
//    assertEquals( "/home/admin", jobScheduleRequest.getOutputFile() );
//    assertEquals( "America/New_York", jobScheduleRequest.getTimeZone() );
//    assertEquals( "false", jobScheduleRequest.getRunSafeMode() );
//    assertEquals( "true", jobScheduleRequest.getGatheringMetrics() );
//    assertEquals( "Basic", jobScheduleRequest.getLogLevel() );
//    assertEquals( 14, jobScheduleRequest.getJobParameters().size() );
//
//    assertContains( new String[]{ "sLine", "[Product].[All Products].[Classic Cars]", "string"},
//      jobScheduleRequest.getJobParameters() );
//
//    assertContains( new String[]{ "sMarket", "[Markets].[All Markets].[NA]", "string"},
//      jobScheduleRequest.getJobParameters() );
//
//    assertContains( new String[]{ "sYear", "[Time].[All Years].[2003]", "string"},
//      jobScheduleRequest.getJobParameters() );
//
//    assertContains( new String[]{ "TopCount", "3", "number"},
//      jobScheduleRequest.getJobParameters() );
//
//    assertContains( new String[]{ "output-target", "table/html;page-mode=page", "string"},
//      jobScheduleRequest.getJobParameters() );
//
//    assertContains( new String[]{ "accepted-page", "0", "number"},
//      jobScheduleRequest.getJobParameters() );
//
//    assertContains( new String[]{ "::session", "09b24716-84a6-11ee-8f4b-66860150dd1e", "string"},
//      jobScheduleRequest.getJobParameters() );
//
//    assertContains( new String[]{ "showParameters", "true", "string"},
//      jobScheduleRequest.getJobParameters() );
//
//    assertContains( new String[]{ "renderMode", "PARAMETER", "string"},
//      jobScheduleRequest.getJobParameters() );
//
//    assertContains( new String[]{ "htmlProportionalWidth", "false", "string"},
//      jobScheduleRequest.getJobParameters() );
//
//    assertContains( new String[]{ "query-limit-ui-enabled", "true", "string"},
//      jobScheduleRequest.getJobParameters() );
//
//    assertContains( new String[]{ "query-limit", "0", "number"},
//      jobScheduleRequest.getJobParameters() );
//
//    assertContains( new String[]{ "maximum-query-limit", "0", "number"},
//      jobScheduleRequest.getJobParameters() );
//
//    assertTrue( jobScheduleRequest.getJobId().contains( "admin" )
//      && jobScheduleRequest.getJobId().contains( "Top Customers (report)-manual-edit-" )
//      && jobScheduleRequest.getJobId().contains( "007cf280-84a6-11ee-8f4b-66860150dd1e" )
//    );
//
//  }
//
//  /**
//   * Verify parameter exists within a list.
//   * @param parameter
//   * @param jobScheduleParams
//   */
//  public void assertContains( String[] parameter, List<IJobScheduleParam> jobScheduleParams ) {
//    int NAME_INDEX=0;
//    int STRING_VALUE_INDEX=1;
//    int TYPE_INDEX=2;
//    IJobScheduleParam param = jobScheduleParams.stream()
//      .filter( jsp -> parameter[NAME_INDEX].equals( jsp.getName() ) ).findFirst().get();
//    assertEquals( parameter[TYPE_INDEX], param.getType() );
//    /*
//     NOTE: doing assertTrue here, there is internal logic for adding "[" and "]" to strings, testing could result in
//     brittle test. Unit test for the corresponding class should handle that precise verification.
//     */
//    assertTrue( param.getStringValue().toString().contains( parameter[STRING_VALUE_INDEX] ) );
//  }
//
//  /**
//   * Unmarshall json string to Object. Simulates current technology stack unmarshalling process.
//   * Code is taken from current technology stack of spring,jersey,jackson and jaxb.
//   * Slightly modified this function @see <a href="https://github.com/javaee/jersey-1.x/blob/1.19.1/jersey-json/src/main/java/com/sun/jersey/json/impl/provider/entity/JSONRootElementProvider.java#L148-L158">JSONRootElementProvider#readFrom</a>
//   * <p/>
//   * code snippet:
//   * <pre>
//   *     protected final Object readFrom(Class<Object> type, MediaType mediaType,
//   *             Unmarshaller u, InputStream entityStream)
//   *             throws JAXBException {
//   *         final Charset c = getCharset(mediaType);
//   *
//   *
//   *         try {
//   *             return JSONJAXBContext.getJSONUnmarshaller(u, getJAXBContext(type)).
//   *                     unmarshalFromJSON(new InputStreamReader(entityStream, c), type);
//   *         } catch (JsonFormatException e) {
//   *             throw new WebApplicationException(e, Status.BAD_REQUEST);
//   *         }
//   * </pre>
//   * @param type
//   * @param entityStream
//   * @return
//   * @param <T>
//   * @throws Exception
//   */
//  protected final <T extends Object> T readFrom( Class<T> type, InputStream entityStream ) throws Exception {
//    JAXBContext jaxbContext = jaxbContextResolver.getContext( type );
//    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
//
//    return JSONJAXBContext.getJSONUnmarshaller(unmarshaller , jaxbContext).unmarshalFromJSON(new InputStreamReader(entityStream), type);
//  }
//}
