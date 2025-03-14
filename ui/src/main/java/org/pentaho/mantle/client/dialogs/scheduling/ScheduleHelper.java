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

import com.google.gwt.json.client.JSONObject;
import org.pentaho.gwt.widgets.client.dialogs.IDialogCallback;
import org.pentaho.gwt.widgets.client.dialogs.MessageDialogBox;
import org.pentaho.gwt.widgets.client.filechooser.RepositoryFile;
import org.pentaho.mantle.client.commands.AbstractCommand;
import org.pentaho.mantle.client.events.EventBusUtil;
import org.pentaho.mantle.client.events.SolutionFileActionEvent;
import org.pentaho.mantle.client.messages.Messages;
import org.pentaho.mantle.client.environment.EnvironmentHelper;
import org.pentaho.mantle.client.solutionbrowser.ScheduleCallback;
import org.pentaho.mantle.client.workspace.JsJob;
import org.pentaho.mantle.login.client.MantleLoginDialog;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class ScheduleHelper {

  static {
    setupNativeHooks( new ScheduleHelper() );
  }

  public static String JOB_SCHEDULER_URL = "api/scheduler/job";
  public static String UPDATE_JOB_SCHEDULER_URL = "api/scheduler/job/update";
  public static String SCHEDULER_INTERNAL_VARIABLE_URL = "api/scheduler/hideInternalVariable";


  private static native void setupNativeHooks( ScheduleHelper scheduleHelper )
  /*-{
    $wnd.mantle_confirmBackgroundExecutionDialog = function(url) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      @org.pentaho.mantle.client.dialogs.scheduling.ScheduleHelper::confirmBackgroundExecutionDialog(Ljava/lang/String;)(url);
    }

    $wnd.pho.createSchedule = function(repositoryFileId, repositoryFilePath) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      @org.pentaho.mantle.client.dialogs.scheduling.ScheduleHelper::createSchedule(Ljava/lang/String;Ljava/lang/String;)(repositoryFileId, repositoryFilePath);
    }

    $wnd.pho.getSchedulerPluginContextURL = function() {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      return @org.pentaho.mantle.client.dialogs.scheduling.ScheduleHelper::getPluginContextURL()();
    }
  }-*/;

  public static void showScheduleDialog( final String fileNameWithPath, final IDialogCallback callback ) {
    final SolutionFileActionEvent event = new SolutionFileActionEvent();
    event.setAction( ScheduleHelper.class.getName() );
    try {

      final String url = EnvironmentHelper.getFullyQualifiedURL() + "api/mantle/isAuthenticated"; //$NON-NLS-1$
      RequestBuilder requestBuilder = new RequestBuilder( RequestBuilder.GET, url );
      requestBuilder.setHeader( "accept", "text/plain" );
      requestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
      requestBuilder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable caught ) {
          MantleLoginDialog.performLogin( new AsyncCallback<Boolean>() {

            public void onFailure( Throwable caught ) {
            }

            public void onSuccess( Boolean result ) {
              showScheduleDialog( fileNameWithPath, callback );
            }

          } );
        }

        public void onResponseReceived( Request request, Response response ) {
          RequestBuilder emailValidRequest =
              new RequestBuilder( RequestBuilder.GET, EnvironmentHelper.getFullyQualifiedURL() + "api/emailconfig/isValid" );
          emailValidRequest.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
          emailValidRequest.setHeader( "accept", "text/plain" );
          try {
            emailValidRequest.sendRequest( null, new RequestCallback() {

              public void onError( Request request, Throwable exception ) {
                MessageDialogBox dialogBox =
                    new MessageDialogBox( Messages.getString( "error" ), exception.toString(), false, false, true ); //$NON-NLS-1$
                dialogBox.center();
                event.setMessage( exception.getLocalizedMessage() );
                EventBusUtil.EVENT_BUS.fireEvent( event );
              }

              public void onResponseReceived( Request request, Response response ) {
                if ( response.getStatusCode() == Response.SC_OK ) {
                  final boolean isEmailConfValid = Boolean.parseBoolean( response.getText() );

                  NewScheduleDialog dialog = ScheduleFactory.getInstance()
                    .createNewScheduleDialog( fileNameWithPath, callback, isEmailConfValid );
                  dialog.center();

                  event.setMessage( "Open" );
                  EventBusUtil.EVENT_BUS.fireEvent( event );
                }
              }
            } );
          } catch ( RequestException e ) {
            MessageDialogBox dialogBox =
                new MessageDialogBox( Messages.getString( "error" ), e.toString(), false, false, true ); //$NON-NLS-1$
            dialogBox.center();
            event.setMessage( e.getLocalizedMessage() );
            EventBusUtil.EVENT_BUS.fireEvent( event );
          }
        }

      } );
    } catch ( RequestException e ) {
      Window.alert( e.getMessage() );
      event.setMessage( e.getLocalizedMessage() );
      EventBusUtil.EVENT_BUS.fireEvent( event );
    }
  }

  public static void createSchedule( final RepositoryFile repositoryFile ) {
    createSchedule( repositoryFile.getId(), repositoryFile.getPath() );
  }

  public static void createSchedule( final String repositoryFileId,  final String repositoryFilePath ) {
    ScheduleCallback callback = new ScheduleCallback( repositoryFilePath );

    AbstractCommand scheduleCommand = new AbstractCommand() {

      protected void performOperation() {

        // hit the server and check: isScheduleAllowed
        final String url = getPluginContextURL() + "api/scheduler/isScheduleAllowed?id=" + repositoryFileId; //$NON-NLS-1$
        RequestBuilder requestBuilder = new RequestBuilder( RequestBuilder.GET, url );
        requestBuilder.setHeader( "accept", "text/plain" );
        requestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
        final MessageDialogBox errorDialog =
            new MessageDialogBox(
                Messages.getString( "error" ), Messages.getString( "noSchedulePermission" ), false, false, true ); //$NON-NLS-1$ //$NON-NLS-2$
        try {
          requestBuilder.sendRequest( null, new RequestCallback() {

            public void onError( Request request, Throwable caught ) {
              errorDialog.center();
            }

            public void onResponseReceived( Request request, Response response ) {
              if ( "true".equalsIgnoreCase( response.getText() ) ) {
                callback.okPressed();
              } else {
                errorDialog.center();
              }
            }
          } );
        } catch ( RequestException re ) {
          errorDialog.center();
        }

      }

      protected void performOperation( boolean feedback ) {
        performOperation();
      }

    };
    scheduleCommand.execute();

  }

  /**
   * The passed in URL has all the parameters set for background execution. We simply call GET on the URL and
   * handle the response object. If the response object contains a particular string then we display success
   * message box.
   *
   * @param url
   *          Complete url with all the parameters set for scheduling a job in the background.
   */
  private static void runInBackground( final String url ) {

    RequestBuilder builder = new RequestBuilder( RequestBuilder.GET, url );
    try {
      builder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
      builder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
          MessageDialogBox dialogBox =
              new MessageDialogBox(
                  Messages.getString( "error" ), Messages.getString( "couldNotBackgroundExecute" ), false, false, true ); //$NON-NLS-1$ //$NON-NLS-2$
          dialogBox.center();
        }

        public void onResponseReceived( Request request, Response response ) {
          /*
           * We are checking for this specific string because if the job was scheduled successfully by
           * QuartzBackgroundExecutionHelper then the response is an html that contains the specific string. We
           * have coded this way because we did not want to touch the old way.
           */
          if ( "true".equals( response.getHeader( "background_execution" ) ) ) {
            MessageDialogBox dialogBox =
                new MessageDialogBox(
                    Messages.getString( "info" ), Messages.getString( "backgroundJobScheduled" ), false, false, true ); //$NON-NLS-1$ //$NON-NLS-2$
            dialogBox.center();
          }
        }
      } );
    } catch ( RequestException e ) {
      MessageDialogBox dialogBox = new MessageDialogBox( Messages.getString( "error" ), //$NON-NLS-1$
          Messages.getString( "couldNotBackgroundExecute" ), false, false, true ); //$NON-NLS-1$
      dialogBox.center();
    }
  }

  public static void confirmBackgroundExecutionDialog( final String url ) {

    final MessageDialogBox scheduleInBackground = new MessageDialogBox(
      Messages.getString( "confirm" ),
      Messages.getString( "userParamBackgroundWarning" ),
      false,
      Messages.getString( "yes" ),
      Messages.getString( "no" ) );

    final IDialogCallback callback = new IDialogCallback() {
      public void cancelPressed() {
        scheduleInBackground.hide();
      }

      public void okPressed() {
        runInBackground( url );
      }
    };
    scheduleInBackground.setCallback( callback );
    scheduleInBackground.center();
  }

  public static RequestBuilder buildRequestForJob( JsJob editJob, JSONObject requestPayload ) {

    RequestBuilder scheduleFileRequestBuilder = null;

    if ( editJob == null || editJob.getJobId() == null ) {
      scheduleFileRequestBuilder =
          new RequestBuilder( RequestBuilder.POST, getPluginContextURL() + JOB_SCHEDULER_URL );
    } else {
      scheduleFileRequestBuilder =
        new RequestBuilder( RequestBuilder.POST, getPluginContextURL() + UPDATE_JOB_SCHEDULER_URL );
      if ( null != requestPayload ) {
        requestPayload.put( "jobId", new JSONString( editJob.getJobId() ) );
      }
    }

    scheduleFileRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    scheduleFileRequestBuilder.setHeader( "Content-Type", "application/json" ); //$NON-NLS-1$//$NON-NLS-2$

    return scheduleFileRequestBuilder;
  }

  public static RequestBuilder buildRequestForRetrieveHideInternalVariable() {

    RequestBuilder scheduleFileRequestBuilder =
            new RequestBuilder( RequestBuilder.GET, getPluginContextURL() + SCHEDULER_INTERNAL_VARIABLE_URL );

    scheduleFileRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    scheduleFileRequestBuilder.setHeader( "Content-Type", "application/json" ); //$NON-NLS-1$//$NON-NLS-2$

    return scheduleFileRequestBuilder;
  }

  public static String getPluginContextURL() {
    String fullyQualifiedURL = EnvironmentHelper.getFullyQualifiedURL();
    return fullyQualifiedURL + "plugin/scheduler-plugin/";
  }

  /**
   * Gets the fully qualified base URL of the Pentaho server environment.
   *
   * This version does not support embedding scenarios.
   * Additionally, it is tied to the Pentaho scheduler.
   *
   * @deprecated Use {@link EnvironmentHelper#getFullyQualifiedURL()} instead.
   */
  @Deprecated
  public static native String getFullyQualifiedURL()
  /*-{
    return $wnd.location.protocol + "//" + $wnd.location.host + $wnd.CONTEXT_PATH
  }-*/;

}
