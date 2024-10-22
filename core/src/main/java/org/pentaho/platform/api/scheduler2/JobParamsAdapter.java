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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class JobParamsAdapter extends XmlAdapter<JobParams, Map<String, Object>> {

  private static final String VARIABLES = "variables";
  private static final String PARAMETERS = "parameters";

  public JobParams marshal( Map<String, Object> v ) throws Exception {
    Object variables = v.get( VARIABLES );
    Object parameters = v.get( PARAMETERS );
    if ( parameters != null && parameters instanceof Map
            && variables != null && variables instanceof Map ) {
      Map<String, String> paramMap = (Map) parameters;
      Map<String, String> variableMap = (Map) variables;
      if ( !paramMap.isEmpty() && !variableMap.isEmpty() ) {
        for ( Map.Entry<String, String> paramEntry : paramMap.entrySet() ) {
          if ( variableMap.containsKey( paramEntry.getKey() ) && paramEntry.getValue() != null ) {
            variableMap.remove( paramEntry.getKey() );
          }
        }
      }
    }

    ArrayList<JobParam> params = new ArrayList<JobParam>();
    for ( Map.Entry<String, Object> entry : v.entrySet() ) {
      if ( entry != null && entry.getKey() != null && entry.getValue() != null ) {
        if ( entry.getValue() instanceof Collection ) {
          for ( Object iValue : (Collection<?>) entry.getValue() ) {
            if ( iValue != null ) {
              JobParam jobParam = new JobParam();
              jobParam.name = entry.getKey();
              jobParam.value = iValue.toString();
              params.add( jobParam );
            }
          }
        } else if ( entry.getValue() instanceof Map ) {
          ( (Map<String, Object>) entry.getValue() ).forEach( ( key, value ) -> {
            if ( value != null ) {
              JobParam jobParam = new JobParam();
              jobParam.name = key;
              jobParam.value = value.toString();
              params.add( jobParam );
            }
          } );
        } else {
          JobParam jobParam = new JobParam();
          jobParam.name = entry.getKey();
          jobParam.value = entry.getValue().toString();
          params.add( jobParam );
        }
      }
    }
    JobParams jobParams = new JobParams();
    jobParams.jobParams = params.toArray( new JobParam[0] );
    return jobParams;
  }

  public Map<String, Object> unmarshal( JobParams v ) throws Exception {
    HashMap<String, ArrayList<Serializable>> draftParamMap = new HashMap<String, ArrayList<Serializable>>();
    for ( JobParam jobParam : v.jobParams ) {
      ArrayList<Serializable> p = draftParamMap.get( jobParam.name );
      if ( p == null ) {
        p = new ArrayList<Serializable>();
        draftParamMap.put( jobParam.name, p );
      }
      p.add( jobParam.value );
    }
    HashMap<String, Object> paramMap = new HashMap<String, Object>();
    for ( String paramName : draftParamMap.keySet() ) {
      ArrayList<Serializable> p = draftParamMap.get( paramName );
      if ( p.size() == 1 ) {
        paramMap.put( paramName, p.get( 0 ) );
      } else {
        paramMap.put( paramName, p );
      }
    }
    return paramMap;
  }

}
