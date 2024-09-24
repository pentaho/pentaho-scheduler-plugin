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
 * Copyright (c) 2002-2024 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.mantle.client.workspace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import org.pentaho.gwt.widgets.client.genericfile.GenericFileNameUtils;

import java.util.Date;

public class JsJob extends JavaScriptObject {

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
    String resource = getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" );
    return ( resource != null && !resource.isEmpty() );
  }

  public final String getFullResourceName() {
    String resource = getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" );
    if ( resource == null || resource.isEmpty() ) {
      return getJobName();
    }

    int outputFileIndex = resource.indexOf( ":outputFile = /" );
    return resource.substring( resource.indexOf( "/" ),
      ( outputFileIndex != -1 ) ? outputFileIndex : resource.indexOf( ":" ) );
  }

  public final String getScheduledExtn() {
    // Jobs scheduled in PUC have a param with an input path on its own
    String inputPath = getJobParamValue( "ActionAdapterQuartzJob-StreamProvider-InputFile" );

    // Jobs scheduled in PDI only have a single field with combined input/output paths
    // in this format "input file = /home/admin/myTransformation.ktr:outputFile = /home/admin/myTransformation.*"
    String inputOutputPaths = getJobParamValue("ActionAdapterQuartzJob-StreamProvider");

    String fileExtension = "";
    if ( inputPath != null && !inputPath.trim().isEmpty() ) {
      fileExtension = inputPath.substring( inputPath.lastIndexOf( '.' ) + 1 );
    } else if ( inputOutputPaths != null && !inputOutputPaths.trim().isEmpty() ) {
      String splitInputFilePath = inputOutputPaths.substring(0, inputOutputPaths.indexOf(":outputFile = "));
      fileExtension = splitInputFilePath.substring(splitInputFilePath.lastIndexOf('.') + 1);
    }

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

  public final String getOutputPath() {
    // Sample values
    // "input file = /home/admin/report.prpt:outputFile = pvfs://MyS3/folder/report.*"
    // "input file = /home/admin/report.prpt:outputFile = /home/admin/folder/report.*"
    String resource = getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" );
    if ( resource == null || resource.isEmpty() ) {
      return "";
    }

    // Extract outputFile value. Match on the full separator to avoid issues for paths with colons
    String outputPath = resource.substring( resource.indexOf( ":outputFile = " ) + 14 ).trim();

    // Remove file name pattern in last segment, to get the output folder.
    return GenericFileNameUtils.getParentPath( outputPath );
  }

  public final void setOutputPath( String outputPath, String outputFileName ) {
    // Sample value
    // input file = /public/Inventory.prpt:outputFile = /public/TEST.*
    JsJobParam resource = getJobParam( "ActionAdapterQuartzJob-StreamProvider" );
    resource.setValue( "input file = " + getFullResourceName() + ":outputFile = " + outputPath + "/" + outputFileName
      + ".*" );
  }

  public final String getShortResourceName() {
    String resource = getFullResourceName();
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
