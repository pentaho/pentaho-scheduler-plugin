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

import java.util.HashMap;
import java.util.Map;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.scheduler2.ws.JaxBSafeMap.JaxBSafeEntry;

/**
 * Converts a Map<String, ParamValue> used to pass {@link ISchedulerService} job parameters to a JAXB marshallable type
 * and back. See <a href="https://jaxb.dev.java.net/guide/Mapping_your_favorite_class.html">this JAXB reference</a>
 * 
 * @author aphillips
 * 
 */
public class JobParamsAdapter extends XmlAdapter<JaxBSafeMap, Map<String, ParamValue>> {

  private static final Log logger = LogFactory.getLog( JobParamsAdapter.class );

  public JaxBSafeMap marshal( Map<String, ParamValue> unsafeMap ) throws Exception {
    try {
      JaxBSafeMap safeMap = new JaxBSafeMap( unsafeMap );
      return safeMap;
    } catch ( Throwable t ) {
      logger.error( t );
    }
    return null;
  }

  public Map<String, ParamValue> unmarshal( JaxBSafeMap safeMap ) throws Exception {
    Map<String, ParamValue> unsafeMap = null;
    try {
      unsafeMap = new HashMap<String, ParamValue>();
      for ( JaxBSafeEntry safeEntry : safeMap.entry ) {
        ParamValue v = safeEntry.getStringValue();
        if ( v == null ) {
          v = safeEntry.getListValue();
        }
        if ( v == null ) {
          v = safeEntry.getMapValue();
        }
        unsafeMap.put( safeEntry.key, v );
      }
      return unsafeMap;
    } catch ( Throwable t ) {
      logger.error( t );
    }
    return null;
  }
}
