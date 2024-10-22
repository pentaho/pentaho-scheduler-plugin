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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

public class JaxBUtil {

  /**
   * Returns an object that should match the <code>in</code> object value-wise, but is actually created by JAXB. JAXB
   * will marshall the <code>in</code> object to XML, print it to stdout, and hydrate an object of the same type from
   * the XML.
   * 
   * @param <T>
   *          the type to marshall and unmarshall to and from XML
   * @param in
   *          the object to marshall to XML
   * @param classes
   *          additional classes referred to by the <code>in</code> object
   * @return a JAXB-created object that should be value-wise equal to the <code>in</code> object
   * @throws JAXBException
   *           if something goes wrong
   */
  @SuppressWarnings( "unchecked" )
  public static <T> T outin( T in, Class<?>... classes ) throws JAXBException {
    //
    // marshal and unmarshall back into a new object
    //
    JAXBContext jc = JAXBContext.newInstance( classes );
    Marshaller m = jc.createMarshaller();
    m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    m.marshal( in, bos );
    System.out.println( new String( bos.toByteArray() ) );
    Unmarshaller u = jc.createUnmarshaller();
    T unmarshalled = (T) u.unmarshal( new ByteArrayInputStream( bos.toByteArray() ) );
    return unmarshalled;
  }

  @SuppressWarnings( "unchecked" )
  /**
   * @param wrapInRootElement <code>false</code> means the class is not required to be annotated with @XmlRootElement
   */
  public static <T> void out( boolean wrapInRootElement, T in, Class<?>... classes ) throws JAXBException {
    JAXBContext jc = JAXBContext.newInstance( classes );
    Marshaller m = jc.createMarshaller();
    m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    if ( wrapInRootElement ) {
      m.marshal( new JAXBElement( new QName( in.getClass().getSimpleName() ), in.getClass(), in ), bos );
    }
    System.out.println( new String( bos.toByteArray() ) );
  }
}
