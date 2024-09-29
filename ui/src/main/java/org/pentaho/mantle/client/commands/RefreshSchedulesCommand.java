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


package org.pentaho.mantle.client.commands;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
import org.pentaho.mantle.client.environment.EnvironmentHelper;
import org.pentaho.mantle.client.workspace.SchedulesPerspectivePanel;

public class RefreshSchedulesCommand extends AbstractCommand {

  public RefreshSchedulesCommand() {
  }

  protected void performOperation() {
    performOperation( true );
  }

  protected void performOperation( boolean feedback ) {
    try {
      final String url = EnvironmentHelper.getFullyQualifiedURL() + "api/repo/files/canAdminister"; //$NON-NLS-1$
      RequestBuilder requestBuilder = new RequestBuilder( RequestBuilder.GET, url );
      requestBuilder.setHeader( "accept", "text/plain" );
      requestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
      requestBuilder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable caught ) {
          GWT.runAsync( new RunAsyncCallback() {

            public void onSuccess() {
              SchedulesPerspectivePanel.getInstance().refresh();
            }

            public void onFailure( Throwable reason ) {
            }
          } );
        }

        public void onResponseReceived( Request request, final Response response ) {
          GWT.runAsync( new RunAsyncCallback() {

            public void onSuccess() {
              SchedulesPerspectivePanel.getInstance().refresh();
            }

            public void onFailure( Throwable reason ) {
            }
          } );
        }

      } );
    } catch ( RequestException e ) {
      Window.alert( e.getMessage() );
    }
  }

}
