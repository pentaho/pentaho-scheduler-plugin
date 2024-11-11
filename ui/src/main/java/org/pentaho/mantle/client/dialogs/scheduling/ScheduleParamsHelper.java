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


package org.pentaho.mantle.client.dialogs.scheduling;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import org.pentaho.mantle.client.workspace.JsJob;
import org.pentaho.mantle.client.workspace.JsJobParam;

import java.util.ArrayList;
import java.util.List;

public class ScheduleParamsHelper {

  public static final String AUTO_CREATE_UNIQUE_FILENAME_KEY = "autoCreateUniqueFilename";
  public static final String APPEND_DATE_FORMAT_KEY = "appendDateFormat";
  public static final String OVERWRITE_FILE_KEY = "overwriteFile";
  public static final String ACTION_USER_KEY = "ActionAdapterQuartzJob-ActionUser";
  public static final String JOB_PARAMETERS_KEY = "jobParameters";

  private ScheduleParamsHelper() { }

  public static JSONObject buildScheduleParam( String name, String value, String type ) {
    JsArrayString paramValue = JavaScriptObject.createArray().cast();
    paramValue.push( value );

    JsSchedulingParameter param = JavaScriptObject.createObject().cast();
    param.setName( name );
    param.setType( type );
    param.setStringValue( paramValue );

    return new JSONObject( param );
  }

  public static JsJobParam buildJobParam( String name, String value ) {
    JsJobParam param = JavaScriptObject.createObject().cast();
    param.setName( name );
    param.setValue( value );

    return param;
  }

  public static JSONArray getScheduleParams( JSONObject jobSchedule ) {
    List<JSONObject> schedulingParams = new ArrayList<>();
    JSONArray jobParameters = (JSONArray) jobSchedule.get( JOB_PARAMETERS_KEY );
    if ( jobParameters != null ) {
      for ( int i = 0; i < jobParameters.size(); i++ ) {
        schedulingParams.add( (JSONObject) jobParameters.get( i ) );
      }
    }

    return getScheduleParams( jobSchedule, schedulingParams );
  }

  public static JSONArray getScheduleParams( JSONObject jobSchedule, List<JSONObject> schedulingParams ) {
    JSONArray params = new JSONArray();

    for ( int i = 0; i < schedulingParams.size(); i++ ) {
      params.set( i, schedulingParams.get( i ) );
    }

    JSONArray jobParams = (JSONArray) jobSchedule.get( JOB_PARAMETERS_KEY );
    if ( jobParams != null ) {
      int size = params.size();

      for ( int i = 0; i < jobParams.size(); i++ ) {
        params.set( size + i, jobParams.get( i ) );
      }
    }

    if ( jobSchedule.get( APPEND_DATE_FORMAT_KEY ) != null ) {
      String dateFormat = jobSchedule.get( APPEND_DATE_FORMAT_KEY ).toString();
      dateFormat = dateFormat.substring( 1, dateFormat.length() - 1 ); // get rid of ""

      params.set( params.size(), buildScheduleParam( APPEND_DATE_FORMAT_KEY, dateFormat, "string" ) );
    }

    if ( jobSchedule.get( OVERWRITE_FILE_KEY ) != null ) {
      String overwriteFile = jobSchedule.get( OVERWRITE_FILE_KEY ).toString();
      overwriteFile = overwriteFile.substring( 1, overwriteFile.length() - 1 );

      boolean overwrite = Boolean.parseBoolean( overwriteFile );
      if ( overwrite ) {
        params.set( params.size(), buildScheduleParam( AUTO_CREATE_UNIQUE_FILENAME_KEY, "false",  "boolean" ) );
      }
    }

    return params;
  }

  public static JSONObject generateLineageId( JsJob job ) {
    return buildScheduleParam( "lineage-id", job.getJobParamValue( "lineage-id" ), "string" );
  }
}
