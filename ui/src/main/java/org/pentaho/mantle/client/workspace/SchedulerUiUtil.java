/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2024 Hitachi Vantara. All rights reserved.
 */
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
    "accepted-page",
    "ActionAdapterQuartzJob-ActionId",
    "ActionAdapterQuartzJob-ActionUser",
    "ActionAdapterQuartzJob-StreamProvider-InputFile",
    "ActionAdapterQuartzJob-StreamProvider",
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
    "query-limit-ui-enabled",
    "query-limit",
    "renderMode",
    "repositoryName",
    "runSafeMode",
    "scheduleRecurrence",
    "scheduleType",
    "showParameters",
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
