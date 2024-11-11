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


package org.pentaho.mantle.client.commands;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

import org.pentaho.gwt.widgets.client.dialogs.MessageDialogBox;
import org.pentaho.gwt.widgets.client.utils.NameUtils;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleEmailDialog;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleFactory;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleHelper;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleOutputLocationDialog;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleParamsDialog;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleParamsHelper;
import org.pentaho.mantle.client.messages.Messages;
import org.pentaho.mantle.client.solutionbrowser.ScheduleCreateStatusDialog;
import org.pentaho.mantle.client.environment.EnvironmentHelper;

import java.util.Date;

/**
 * Run In Background Command
 *
 * Note that the run in background command functionality is similar to schedule functionality. While a lot of code is
 * duplicated over the two commands, quite a bit of screen flow / controller logic is embedded in the view dialogs.
 * Combining these would make more sense once the control flow is removed from the views and abstracted into controllers.
 */
public class RunInBackgroundCommand extends AbstractCommand {

  static {
    setupNativeHooks( new RunInBackgroundCommand() );
  }

  String contextURL = EnvironmentHelper.getFullyQualifiedURL();

  private String repositoryFileId;
  private String repositoryFilePath;

  public RunInBackgroundCommand() {
  }

  private String solutionPath = null;
  private String solutionTitle = null;
  private String outputLocationPath = null;
  private String outputName = null;
  private String overwriteFile;
  private String dateFormat;

  public String getSolutionTitle() {
    return solutionTitle;
  }

  public void setSolutionTitle( String solutionTitle ) {
    this.solutionTitle = solutionTitle;
  }

  public String getSolutionPath() {
    return solutionPath;
  }

  public void setSolutionPath( String solutionPath ) {
    this.solutionPath = solutionPath;
  }

  public String getOutputLocationPath() {
    return outputLocationPath;
  }

  public void setOutputLocationPath( String outputLocationPath ) {
    this.outputLocationPath = outputLocationPath;
  }

  public String getOutputName() {
    return outputName;
  }

  public void setOutputName( String outputName ) {
    this.outputName = outputName;
  }

  /**
   * Get Date Format
   *
   * @return a string representation of a date format
   */
  public String getDateFormat() {
    return dateFormat;
  }

  /**
   * Set Date Format
   *
   * @param dateFormat a string representation of a date format
   */
  public void setDateFormat( String dateFormat ) {
    this.dateFormat = dateFormat;
  }

  /**
   * Get Overwrite File
   *
   * @return the string "true" if the file should be overwritten, otherwise "false"
   */
  public String getOverwriteFile() {
    return overwriteFile;
  }

  /**
   * Set Overwrite File
   *
   * @param overwriteFile the string "true" if the file should be overwritten, otherwise "false"
   */
  public void setOverwriteFile( String overwriteFile ) {
    this.overwriteFile = overwriteFile;
  }

  protected void performOperation() {
    if ( this.getSolutionPath() != null ) {
      handleRepositoryFileSelection( this.getSolutionPath(), false );
    } else {
      performOperation( true );
    }
  }

  protected native void handleRepositoryFileSelection( String solutionPath, boolean isAdhoc )/*-{
   $wnd.mantle.handleRepositoryFileSelection( solutionPath, isAdhoc );
  }-*/;

  @SuppressWarnings ( "deprecation" )
  protected JSONObject getJsonSimpleTrigger( int repeatCount, int interval, Date startDate, Date endDate ) {
    JSONObject trigger = new JSONObject();
    trigger.put( "repeatInterval", new JSONNumber( interval ) ); //$NON-NLS-1$
    trigger.put( "repeatCount", new JSONNumber( repeatCount ) ); //$NON-NLS-1$
    trigger
      .put(
        "startTime", startDate != null ? new JSONString( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( startDate ) ) : JSONNull.getInstance() ); //$NON-NLS-1$
    if ( endDate != null ) {
      endDate.setHours( 23 );
      endDate.setMinutes( 59 );
      endDate.setSeconds( 59 );
    }
    trigger
      .put(
        "endTime", endDate == null ? JSONNull.getInstance() : new JSONString( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( endDate ) ) ); //$NON-NLS-1$
    return trigger;
  }

  protected void showDialog( final boolean feedback ) {
    final ScheduleOutputLocationDialog outputLocationDialog = new ScheduleOutputLocationDialog( solutionPath ) {
      @Override
      protected void onSelect( final String name, final String outputLocationPath, final boolean overwriteFile, final String dateFormat ) {
        setOutputName( name );
        setOutputLocationPath( outputLocationPath );
        setOverwriteFile( String.valueOf( overwriteFile ) );
        setDateFormat( dateFormat );
        performOperation( feedback );
      }
    };
    final String filePath = solutionPath;
    String urlPath = NameUtils.URLEncode( NameUtils.encodeRepositoryPath( filePath ) );

    RequestBuilder scheduleFileRequestBuilder = createParametersChecker( urlPath );
    final boolean isXAction = isXAction( urlPath );

    try {
      scheduleFileRequestBuilder.sendRequest( null, new RequestCallback() {
        public void onError( Request request, Throwable exception ) {
          MessageDialogBox dialogBox =
            new MessageDialogBox( Messages.getString( "error" ), exception.toString(), false, false, true ); //$NON-NLS-1$
          dialogBox.center();
        }

        public void onResponseReceived( Request request, Response response ) {
          if ( response.getStatusCode() == Response.SC_OK ) {
            String responseMessage = response.getText();
            boolean hasParams = hasParameters( responseMessage, isXAction );
            if ( !hasParams ) {
              outputLocationDialog.setOkButtonText( Messages.getString( "ok" ) );
            }
            outputLocationDialog.center();
          } else {
            MessageDialogBox dialogBox =
              new MessageDialogBox(
                Messages.getString( "error" ), Messages.getString( "serverErrorColon" ) + " " + response.getStatusCode(), false, false, true ); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
            dialogBox.center();
          }
        }
      } );
    } catch ( RequestException e ) {
      MessageDialogBox dialogBox =
        new MessageDialogBox( Messages.getString( "error" ), e.toString(), false, false, true ); //$NON-NLS-1$
      dialogBox.center();
    }
  }

  private boolean hasParameters( String responseMessage, boolean isXAction ) {
    if ( isXAction ) {
      int numOfInputs = StringUtils.countMatches( responseMessage, "<input" );
      int numOfHiddenInputs = StringUtils.countMatches( responseMessage, "type=\"hidden\"" );
      return numOfInputs - numOfHiddenInputs > 0 ? true : false;
    } else {
      return Boolean.parseBoolean( responseMessage );
    }
  }

  private boolean isXAction( String urlPath ) {
    if ( ( urlPath != null ) && ( urlPath.endsWith( "xaction" ) ) ) {
      return true;
    } else {
      return false;
    }
  }

  private RequestBuilder createParametersChecker( String urlPath ) {
    RequestBuilder scheduleFileRequestBuilder = null;
    if ( ( urlPath != null ) && ( urlPath.endsWith( "xaction" ) ) ) {
      scheduleFileRequestBuilder = new RequestBuilder( RequestBuilder.GET, contextURL + "api/repos/" + urlPath
        + "/parameterUi" );
    } else {
      scheduleFileRequestBuilder = new RequestBuilder( RequestBuilder.GET, contextURL + "api/repo/files/" + urlPath
        + "/parameterizable" );
    }
    scheduleFileRequestBuilder.setHeader( "accept", "text/plain" ); //$NON-NLS-1$ //$NON-NLS-2$
    scheduleFileRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    return scheduleFileRequestBuilder;
  }

  protected void checkSchedulePermissionAndDialog( String repositoryFileId, String repositoryFilePath ) {
    this.repositoryFileId = repositoryFileId;
    this.repositoryFilePath = repositoryFilePath;
    final String url = ScheduleHelper.getPluginContextURL() + "api/scheduler/isScheduleAllowed?id=" + this.repositoryFileId; //$NON-NLS-1$
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
            showDialog( true );
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

    final String filePath = ( this.getSolutionPath() != null ) ? this.getSolutionPath() : this.repositoryFilePath;
    String urlPath = NameUtils.URLEncode( NameUtils.encodeRepositoryPath( filePath ) );

    RequestBuilder scheduleFileRequestBuilder = createParametersChecker( urlPath );
    final boolean isXAction = isXAction( urlPath );

    try {
      scheduleFileRequestBuilder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
          MessageDialogBox dialogBox =
            new MessageDialogBox( Messages.getString( "error" ), exception.toString(), false, false, true ); //$NON-NLS-1$
          dialogBox.center();
        }

        public void onResponseReceived( Request request, Response response ) {
          if ( response.getStatusCode() == Response.SC_OK ) {
            final JSONObject scheduleRequest = new JSONObject();
            scheduleRequest.put( "inputFile", new JSONString( filePath ) ); //$NON-NLS-1$

            //Set date format to append to filename
            if ( StringUtils.isEmpty( getDateFormat() ) ) {
              scheduleRequest.put( "appendDateFormat", JSONNull.getInstance() ); //$NON-NLS-1$
            } else {
              scheduleRequest.put( "appendDateFormat", new JSONString( getDateFormat() ) ); //$NON-NLS-1$
            }

            //Set whether to overwrite the file
            if ( StringUtils.isEmpty( getOverwriteFile() ) ) {
              scheduleRequest.put( "overwriteFile", JSONNull.getInstance() ); //$NON-NLS-1$
            } else {
              scheduleRequest.put( "overwriteFile", new JSONString( getOverwriteFile() ) ); //$NON-NLS-1$
            }

            // Set job name
            if ( StringUtils.isEmpty( getOutputName() ) ) {
              scheduleRequest.put( "jobName", JSONNull.getInstance() ); //$NON-NLS-1$
            } else {
              scheduleRequest.put( "jobName", new JSONString( getOutputName() ) ); //$NON-NLS-1$
            }

            // Set output path location
            if ( StringUtils.isEmpty( getOutputLocationPath() ) ) {
              scheduleRequest.put( "outputFile", JSONNull.getInstance() ); //$NON-NLS-1$
            } else {
              scheduleRequest.put( "outputFile", new JSONString( getOutputLocationPath() ) ); //$NON-NLS-1$
            }

            // BISERVER-9321
            scheduleRequest.put( "runInBackground", JSONBoolean.getInstance( true ) );

            String responseMessage = response.getText();
            final boolean hasParams = hasParameters( responseMessage, isXAction );

            RequestBuilder emailValidRequest =
              new RequestBuilder( RequestBuilder.GET, contextURL + "api/emailconfig/isValid" ); //$NON-NLS-1$
            emailValidRequest.setHeader( "accept", "text/plain" ); //$NON-NLS-1$ //$NON-NLS-2$
            emailValidRequest.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
            try {
              emailValidRequest.sendRequest( null, new RequestCallback() {

                public void onError( Request request, Throwable exception ) {
                  MessageDialogBox dialogBox =
                    new MessageDialogBox( Messages.getString( "error" ), exception.toString(), false, false, true ); //$NON-NLS-1$
                  dialogBox.center();
                }

                public void onResponseReceived( Request request, Response response ) {
                  if ( response.getStatusCode() == Response.SC_OK ) {
                    // final boolean isEmailConfValid = Boolean.parseBoolean(response.getText());
                    // force false for now, I have a feeling PM is going to want this, making it easy to turn back
                    // on
                    final boolean isEmailConfValid = false;
                    if ( hasParams ) {
                      ScheduleParamsDialog dialog =
                        ScheduleFactory.getInstance().createScheduleParamsDialog( filePath, scheduleRequest, isEmailConfValid );
                      dialog.center();
                      dialog.setAfterResponseCallback( scheduleParamsDialogCallback );
                    } else if ( isEmailConfValid ) {
                      ScheduleEmailDialog scheduleEmailDialog =
                        ScheduleFactory.getInstance().createScheduleEmailDialog( null, filePath, scheduleRequest, null, null );
                      scheduleEmailDialog.center();
                    } else {
                      // Handle Schedule Parameters
                      String jsonStringScheduleParams = ScheduleParamsHelper.getScheduleParams( scheduleRequest ).toString();
                      JSONValue scheduleParams = JSONParser.parseStrict( jsonStringScheduleParams );
                      scheduleRequest.put( "jobParameters", scheduleParams );

                      // just run it
                      RequestBuilder scheduleFileRequestBuilder =
                        new RequestBuilder( RequestBuilder.POST, ScheduleHelper.getPluginContextURL() + "api/scheduler/job" ); //$NON-NLS-1$
                      scheduleFileRequestBuilder.setHeader( "Content-Type", "application/json" ); //$NON-NLS-1$//$NON-NLS-2$
                      scheduleFileRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );

                      try {
                        scheduleFileRequestBuilder.sendRequest( scheduleRequest.toString(), new RequestCallback() {

                          @Override
                          public void onError( Request request, Throwable exception ) {
                            MessageDialogBox dialogBox =
                              new MessageDialogBox(
                                Messages.getString( "error" ), exception.toString(), false, false, true ); //$NON-NLS-1$
                            dialogBox.center();
                          }

                          @Override
                          public void onResponseReceived( Request request, Response response ) {
                            if ( response.getStatusCode() == 200 ) {
                              MessageDialogBox dialogBox =
                                new MessageDialogBox(
                                  Messages.getString( "runInBackground" ), Messages.getString( "backgroundExecutionStarted" ), //$NON-NLS-1$ //$NON-NLS-2$
                                  false, false, true );
                              dialogBox.center();
                            } else {
                              MessageDialogBox dialogBox =
                                new MessageDialogBox(
                                  Messages.getString( "error" ), Messages.getString( "serverErrorColon" ) + " " + response.getStatusCode(), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-2$ //$NON-NLS-3$
                                  false, false, true );
                              dialogBox.center();
                            }
                          }

                        } );
                      } catch ( RequestException e ) {
                        MessageDialogBox dialogBox = new MessageDialogBox( Messages.getString( "error" ), e.toString(), //$NON-NLS-1$
                          false, false, true );
                        dialogBox.center();
                      }
                    }

                  }
                }
              } );
            } catch ( RequestException e ) {
              MessageDialogBox dialogBox =
                new MessageDialogBox( Messages.getString( "error" ), e.toString(), false, false, true ); //$NON-NLS-1$
              dialogBox.center();
            }

          } else {
            MessageDialogBox dialogBox =
              new MessageDialogBox(
                Messages.getString( "error" ), Messages.getString( "serverErrorColon" ) + " " + response.getStatusCode(), false, false, true ); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
            dialogBox.center();
          }
        }

      } );
    } catch ( RequestException e ) {
      MessageDialogBox dialogBox =
        new MessageDialogBox( Messages.getString( "error" ), e.toString(), false, false, true ); //$NON-NLS-1$
      dialogBox.center();
    }
  }

  ScheduleParamsDialog.IAfterResponse scheduleParamsDialogCallback = new ScheduleParamsDialog.IAfterResponse() {
    @Override
    public void onResponse( JSONValue rib ) {
      if ( rib != null && rib.isBoolean() != null && rib.isBoolean().booleanValue() ) {
        MessageDialogBox dialogBox =
          new MessageDialogBox(
            Messages.getString( "runInBackground" ), Messages.getString( "backgroundExecutionStarted" ), //$NON-NLS-1$ //$NON-NLS-2$
            false, false, true );
        dialogBox.center();
      } else if ( checkSelectedPerspective() ) {
        ScheduleCreateStatusDialog successDialog = new ScheduleCreateStatusDialog();
        successDialog.center();
      } else {
        MessageDialogBox dialogBox =
          new MessageDialogBox(
            Messages.getString( "scheduleUpdatedTitle" ), Messages.getString( "scheduleUpdatedMessage" ), //$NON-NLS-1$ //$NON-NLS-2$
            false, false, true );
        dialogBox.center();
      }
    }
  };

  private void runInBackgroundCommand( String repositoryFileId, String repositoryFilePath ) {
    this.repositoryFileId = repositoryFileId;
    this.repositoryFilePath = repositoryFilePath;
    setSolutionPath( repositoryFilePath );
    execute();
  }

  private native Boolean checkSelectedPerspective()/*-{
  return $wnd.mantle.checkSelectedPerspective()
  }-*/;

  private static native void setupNativeHooks( RunInBackgroundCommand cmd )
   /*-{
    $wnd.pho.checkSchedulePermissionAndDialog = function(repositoryFileId, repositoryFilePath) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      cmd.@org.pentaho.mantle.client.commands.RunInBackgroundCommand::checkSchedulePermissionAndDialog(Ljava/lang/String;Ljava/lang/String;)(repositoryFileId, repositoryFilePath);
    }

    $wnd.pho.runInBackground = function(repositoryFileId, repositoryFilePath) {
      //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
      cmd.@org.pentaho.mantle.client.commands.RunInBackgroundCommand::runInBackgroundCommand(Ljava/lang/String;Ljava/lang/String;)(repositoryFileId, repositoryFilePath);
    }
  }-*/;
}