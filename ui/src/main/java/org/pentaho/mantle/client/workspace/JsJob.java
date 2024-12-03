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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import org.pentaho.gwt.widgets.client.genericfile.GenericFileNameUtils;

import java.util.Date;

public class JsJob extends JavaScriptObject {

  private static final String ACTION_ADAPTER_QUARTZ_JOB_STREAM_PROVIDER = "ActionAdapterQuartzJob-StreamProvider";
  private static final String ACTION_ADAPTER_QUARTZ_JOB_STREAM_PROVIDER_INPUT_FILE = "ActionAdapterQuartzJob-StreamProvider-InputFile";

  public static final String OUTPUT_FILE_SEPARATOR = ":output file\\s*=|:outputFile\\s*=";
  public static final String INPUT_FILE_SEPARATOR = "input file =";

  // Overlay types always have protected, zero argument constructors.
  protected JsJob() {
  }

  // JSNI methods to get job data.
  public final native String getJobId() /*-{ return this.jobId; }-*/; //

  public final native String getJobName() /*-{ return this.jobName; }-*/; //

  public final native String getUserName() /*-{ return this.userName; }-*/; //

  private final native String getNativeNextRun() /*-{ return this.nextRun; }-*/; //

  private final native String getNativeLastRun() /*-{ return this.lastRun; }-*/; //

  public final native JsArray<JsJobParam> getJobParams() /*-{ return this.jobParams.jobParams; }-*/; //

  public final native JsJobTrigger getJobTrigger() /*-{ return this.jobTrigger; }-*/; //

  public final native String getState() /*-{ return this.state; }-*/; //

  public final native void setState( String newState ) /*-{ this.state = newState; }-*/; //

  public final String getJobParamValue( String name ) {
    if ( hasJobParams() ) {
      JsArray<JsJobParam> params = getJobParams();
      for ( int i = 0; i < params.length(); i++ ) {
        JsJobParam param = params.get( i );
        if ( param.getName().equals( name ) ) {
          return param.getValue();
        }
      }
    }
    return null;
  }

  public final JsJobParam getJobParam( String name ) {
    if ( hasJobParams() ) {
      JsArray<JsJobParam> params = getJobParams();
      for ( int i = 0; i < params.length(); i++ ) {
        JsJobParam param = params.get( i );
        if ( param.getName().equals( name ) ) {
          return param;
        }
      }
    }
    return null;
  }

  private final native boolean hasJobParams() /*-{ return this.jobParams != null; }-*/; //

  public final boolean hasResourceName() {
    String resource = getJobParamValue( ACTION_ADAPTER_QUARTZ_JOB_STREAM_PROVIDER );
    return ( resource != null && !resource.isEmpty() );
  }

  public final String getScheduledExtn() {
    String fileExtension = "";

    // Jobs scheduled in PUC have a param with an input path on its own
    String inputPath = getJobParamValue( ACTION_ADAPTER_QUARTZ_JOB_STREAM_PROVIDER_INPUT_FILE );
    if ( inputPath != null && !inputPath.isEmpty() ) {
      System.out.println( "inputPath: " + inputPath );
      fileExtension = inputPath.substring( inputPath.lastIndexOf( '.' ) + 1 );
    } else {
      // Jobs scheduled in PDI only have a single field with combined input/output paths
      // in this format "input file = /home/admin/myTransformation.ktr:outputFile = /home/admin/myTransformation.*"
      // BISERVER-15173 the format "input file = /home/admin/myTransformation.ktr:output file=/home/admin/myTransformation.*"
      // also needs to be supported for backward compatibility
      String inputFilePath = getInputFilePath();
      System.out.println( "inputFilePath: " + inputFilePath );
      if ( inputFilePath != null && !inputFilePath.isEmpty() ) {
        fileExtension = inputFilePath.substring( inputFilePath.lastIndexOf( '.' ) + 1 );
      }
    }
    System.out.println( "fileExtension: " + fileExtension );
    switch ( fileExtension ) {
      case "ktr":
        return "transformation";
      case "kjb":
        return "job";
      case "prpt":
      case "prpti":
        return "report";
      default:
        return "-";
    }
  }

  public final String getInputFilePath() {
    String resource = getJobParamValue( ACTION_ADAPTER_QUARTZ_JOB_STREAM_PROVIDER );
    System.out.println( resource );
    if ( resource == null || resource.isEmpty() ) {
      return getJobName();
    }

    String inputPart = resource.split( OUTPUT_FILE_SEPARATOR )[0];

    int inputStart = inputPart.indexOf( INPUT_FILE_SEPARATOR );
    if ( inputStart == -1 ) {
      return getJobName();
    }

    return inputPart.substring( inputStart + INPUT_FILE_SEPARATOR.length() ).trim();
  }

  public final String getOutputPath() {
    String resource = getJobParamValue( ACTION_ADAPTER_QUARTZ_JOB_STREAM_PROVIDER );
    if ( resource == null || resource.isEmpty() ) {
      return "";
    }

    // Match either ":output file" or ":outputFile" format, allowing spaces around the equals
    String[] splitByOutputFile = resource.split( OUTPUT_FILE_SEPARATOR );
    if ( splitByOutputFile.length < 2 ) {
      return "";
    }

    return GenericFileNameUtils.getParentPath( splitByOutputFile[1].trim() );
  }

  public final void setOutputPath( String outputPath, String outputFileName ) {
    // Sample value
    // input file = /public/Inventory.prpt:outputFile = /public/TEST.*
    // BISERVER-15173 the format "input file = /home/admin/myTransformation.ktr:output file=/home/admin/myTransformation.*"
    // also needs to be supported for backward compatibility
    JsJobParam resource = getJobParam( ACTION_ADAPTER_QUARTZ_JOB_STREAM_PROVIDER );
    assert resource != null;
    resource.setValue( "input file = " + getInputFilePath()
       + ":outputFile = " + outputPath + "/" + outputFileName + ".*" );
  }

  public final String getShortResourceName() {
    String resource = getInputFilePath();
    if ( resource.contains( "/" ) ) {
      resource = resource.substring( resource.lastIndexOf( "/" ) + 1 );
    }
    return resource;
  }


  public final Date getLastRun() {
    return formatDate( getNativeLastRun() );
  }

  public final Date getNextRun() {
    return formatDate( getNativeNextRun() );
  }

  public static Date formatDate( String dateStr ) {
    try {
      DateTimeFormat format = DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 );
      return format.parse( dateStr );
    } catch ( Throwable t ) {
      //ignored
    }

    try {
      DateTimeFormat format = DateTimeFormat.getFormat( "yyyy-MM-dd'T'HH:mm:ssZZZ" );
      return format.parse( dateStr );
    } catch ( Throwable t ) {
      //ignored
    }

    return null;
  }

  public final native void setJobTrigger( JsJobTrigger trigger ) /*-{ this.jobTrigger = trigger; }-*/;

  public final native String setJobName( String name ) /*-{ this.jobName = name; }-*/; //

}
