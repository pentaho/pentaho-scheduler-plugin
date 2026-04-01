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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class JobParamsAdapter extends XmlAdapter<JobParams, Map<String, Object>> {

  private static final String VARIABLES = "variables";
  private static final String PARAMETERS = "parameters";

  public JobParams marshal( Map<String, Object> v ) throws Exception {
    ArrayList<JobParam> params = new ArrayList<>();
    Set<String> emittedNames = new HashSet<>();

    // Pass 1: root-level single values — highest priority.
    addRootScalarValues( v, params, emittedNames );
    // Pass 2: root-level collections, maps, and arrays.
    addRootCollectionMapAndArrayValues( v, params, emittedNames );
    // Pass 3: "parameters" map — lower priority than any root-level value.
    addNamedMapValues( v.get( PARAMETERS ), params, emittedNames );
    // Pass 4: "variables" map — lowest priority.
    addNamedMapValues( v.get( VARIABLES ), params, emittedNames );

    JobParams jobParams = new JobParams();
    jobParams.jobParams = params.toArray( new JobParam[0] );
    return jobParams;
  }

  private void addRootScalarValues( Map<String, Object> source, Collection<JobParam> params, Set<String> emittedNames ) {
    // Emit root-level scalar values first so they win over parameters and variables.
    rootEntries( source )
        .filter( entry -> !isDeferredPriorityMap( entry ) )
        .filter( entry -> !isCollectionMapOrArray( entry.getValue() ) )
        .forEach( entry -> {
          emittedNames.add( entry.getKey() );
          params.add( toJobParam( entry.getKey(), entry.getValue() ) );
        } );
  }

  @SuppressWarnings( "unchecked" )
  private void addRootCollectionMapAndArrayValues( Map<String, Object> source, Collection<JobParam> params,
                                                    Set<String> emittedNames ) {
    // Emit root-level multi-value structures after scalar roots, but before parameters and variables.
    rootEntries( source )
        .filter( entry -> !isDeferredPriorityMap( entry ) )
        .filter( entry -> isCollectionMapOrArray( entry.getValue() ) )
        .forEach( entry ->
          // Track and guard by the actual JobParam names produced from this entry,
          // so that later passes do not re-emit the same logical parameters.
          flattenEntryValues( entry ).forEach( jobParam -> {
            if ( jobParam != null ) {
              String name = jobParam.name;
              if ( name == null || !emittedNames.contains( name ) ) {
                params.add( jobParam );
                if ( name != null ) {
                  emittedNames.add( name );
                }
              }
            }
          } );
        );
  }

  @SuppressWarnings( "unchecked" )
  private void addNamedMapValues( Object mapObject, Collection<JobParam> params, Set<String> emittedNames ) {
    if ( !( mapObject instanceof Map ) ) {
      return;
    }

    // Emit named map entries only if a higher-priority pass has not already produced the same key.
    ( (Map<String, Object>) mapObject ).forEach( ( key, value ) -> {
      if ( key != null && value != null && !emittedNames.contains( key ) ) {
        params.add( toJobParam( key, value ) );
        emittedNames.add( key );
      }
    } );
  }

  private Stream<Map.Entry<String, Object>> rootEntries( Map<String, Object> source ) {
    return source.entrySet().stream()
        .filter( Objects::nonNull )
        .filter( entry -> entry.getKey() != null && entry.getValue() != null );
  }

  @SuppressWarnings( "unchecked" )
  private Stream<JobParam> flattenEntryValues( Map.Entry<String, Object> entry ) {
    if ( entry.getValue() instanceof Map ) {
      return ( (Map<String, Object>) entry.getValue() ).entrySet().stream()
          .filter( mapEntry -> mapEntry.getKey() != null && mapEntry.getValue() != null )
          .map( mapEntry -> toJobParam( mapEntry.getKey(), mapEntry.getValue() ) );
    }
    if ( entry.getValue() instanceof Collection ) {
      return ( (Collection<?>) entry.getValue() ).stream()
          .filter( Objects::nonNull )
          .map( value -> toJobParam( entry.getKey(), value ) );
    }
    return Arrays.stream( (Object[]) entry.getValue() )
        .filter( Objects::nonNull )
        .map( value -> toJobParam( entry.getKey(), value ) );
  }

  private boolean isDeferredPriorityMap( Map.Entry<String, Object> entry ) {
    return entry.getValue() instanceof Map
        && ( PARAMETERS.equals( entry.getKey() ) || VARIABLES.equals( entry.getKey() ) );
  }

  private boolean isCollectionMapOrArray( Object value ) {
    return value instanceof Map || value instanceof Collection || value.getClass().isArray();
  }

  private JobParam toJobParam( String name, Object value ) {
    JobParam jobParam = new JobParam();
    jobParam.name = name;
    jobParam.value = value.toString();
    return jobParam;
  }

  public Map<String, Object> unmarshal( JobParams v ) throws Exception {
    HashMap<String, ArrayList<Serializable>> draftParamMap = new HashMap<>();
    for ( JobParam jobParam : v.jobParams ) {
      ArrayList<Serializable> p = draftParamMap.get( jobParam.name );
      if ( p == null ) {
        p = new ArrayList<>();
        draftParamMap.put( jobParam.name, p );
      }
      p.add( jobParam.value );
    }
    HashMap<String, Object> paramMap = new HashMap<>();
    for ( Map.Entry<String, ArrayList<Serializable>> entry : draftParamMap.entrySet() ) {
      ArrayList<Serializable> values = entry.getValue();
      paramMap.put( entry.getKey(), values.size() == 1 ? values.get( 0 ) : values );
    }
    return paramMap;
  }

}
