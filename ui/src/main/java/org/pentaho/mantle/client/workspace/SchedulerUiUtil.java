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

package org.pentaho.mantle.client.workspace;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class SchedulerUiUtil {
  protected static final List<String> INJECTED_JOB_PARAM_NAMES = Arrays.asList(
    "::session",
    "ActionAdapterQuartzJob-ActionId",
    "ActionAdapterQuartzJob-ActionUser",
    "ActionAdapterQuartzJob-StreamProvider",
    "ActionAdapterQuartzJob-StreamProvider-InputFile",
    "accepted-page",
    "appendDateFormat",
    "autoCreateUniqueFilename",
    "autoSubmit",
    "autoSubmitUI",
    "clearLog",
    "directory",
    "gatheringMetrics",
    "htmlProportionalWidth",
    "job",
    "jobName",
    "lineage-id",
    "logLevel",
    "maximum-query-limit",
    "previousTriggerNow",
    "query-limit",
    "query-limit-ui-enabled",
    "renderMode",
    "repositoryName",
    "runSafeMode",
    "scheduleRecurrence",
    "scheduleType",
    "showParameters",
    "startTime",
    "timezone",
    "transformation",
    "uiPassParam",
    "user_locale",
    "versionRequestFlags"
  );

  private SchedulerUiUtil() {
  }

  /*
  Filter out the values we inject into scheduled job params for internal scheduler use,
  so that only the user-supplied are returned. The object we get back from the API also
  includes duplicated values, so we remove those too. Also converts the params array into
  a more convenient list type.
   */
  public static List<JsJobParam> getFilteredJobParams( JsJob job, final boolean hideInternalVariables ) {
    JsArray<JsJobParam> paramsRaw = job.getJobParams();
    ArrayList<JsJobParam> params = new ArrayList<>();

    // JsArrays aren't Collections and equals() is a little different for the JavaScriptObjects, so we'll iterate,
    // filter(), and distinct() the old-fashioned way...
    HashSet<String> toFilter = new HashSet<>( INJECTED_JOB_PARAM_NAMES );
    HashSet<String> visitedParamNames = new HashSet<>();

    for ( int i = 0; i < paramsRaw.length(); i++ ) {
      JsJobParam param = paramsRaw.get( i );

      if ( !toFilter.contains( param.getName() ) && !visitedParamNames.contains( param.getName() ) ) {
        if( hideInternalVariables ){
          if( !paramsRaw.get(i).getName().contains("Internal.") ) {
            params.add(paramsRaw.get(i));
          }
        } else {
          params.add(paramsRaw.get(i));
        }
        visitedParamNames.add(param.getName());
      }
    }

    return params;
  }

  @SuppressWarnings( "SameParameterValue" )
  public static Map<String, String> getMapFromJSONResponse( JSONObject obj, String objKey ) {
    try {
      Map<String, String> result = new HashMap<>();
      JSONValue entry = obj.get( objKey ).isObject().get( "entry" );
      JSONArray values = entry.isArray();

      if ( values != null ) {
        for ( int i = 0; i < values.size(); i++ ) {
          JSONValue value = values.get( i );
          result.put( getStringParamFromJSONValue( value, "key" ), getStringParamFromJSONValue( value, "value" ) );
        }
      } else {
        result.put( getStringParamFromJSONValue( entry, "key" ), getStringParamFromJSONValue( entry, "value" ) );
      }

      return result;
    } catch ( Exception e ) {
      throw new IllegalArgumentException( "Invalid JSON Map." );
    }
  }

  private static String getStringParamFromJSONValue( JSONValue value, String param ) {
    try {
      return value.isObject().get( param ).isString().stringValue();
    } catch ( Exception e ) {
      throw new IllegalArgumentException( "Invalid JSONValue param." );
    }
  }
}
