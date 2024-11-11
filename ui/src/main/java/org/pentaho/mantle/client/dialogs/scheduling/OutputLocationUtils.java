/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/


package org.pentaho.mantle.client.dialogs.scheduling;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Command;
import org.pentaho.gwt.widgets.client.genericfile.GenericFileNameUtils;
import org.pentaho.gwt.widgets.client.utils.NameUtils;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;
import org.pentaho.mantle.client.environment.EnvironmentHelper;

/**
 * @author Rowell Belen
 */
public class OutputLocationUtils {

  private OutputLocationUtils() {
  }

  public static void validateOutputLocation( final String outputLocation, final Command successCallback,
                                             final Command errorCallback ) {

    if ( StringUtils.isEmpty( outputLocation ) ) {
      return;
    }

    final String url = EnvironmentHelper.getFullyQualifiedURL()
      + "plugin/scheduler-plugin/api/generic-files/folders/"
      + NameUtils.URLEncode( GenericFileNameUtils.encodePath( outputLocation ) );

    RequestBuilder builder = new RequestBuilder( RequestBuilder.HEAD, url );
    // This header is required to force Internet Explorer to not cache values from the GET response.
    builder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    try {
      builder.sendRequest( null, new RequestCallback() {
        public void onError( Request request, Throwable exception ) {
          if ( errorCallback != null ) {
            errorCallback.execute();
          }
        }

        public void onResponseReceived( Request request, Response response ) {
          if ( response.getStatusCode() == Response.SC_NO_CONTENT ) {
            if ( successCallback != null ) {
              successCallback.execute();
            }
          } else {
            if ( errorCallback != null ) {
              errorCallback.execute();
            }
          }
        }
      } );
    } catch ( RequestException e ) {
      if ( errorCallback != null ) {
        errorCallback.execute();
      }
    }
  }

  public static String getPreviousLocationPath( String path ) {
    return GenericFileNameUtils.getParentPath( path );
  }
}
