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

import org.pentaho.gwt.widgets.client.dialogs.IDialogCallback;
import org.pentaho.gwt.widgets.client.dialogs.MessageDialogBox;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;
import org.pentaho.gwt.widgets.client.wizards.AbstractWizardDialog;
import org.pentaho.gwt.widgets.client.wizards.IWizardPanel;
import org.pentaho.mantle.client.messages.Messages;
import org.pentaho.mantle.client.workspace.JsJob;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;

public class ScheduleEmailDialog extends AbstractWizardDialog {
  IDialogCallback callback;

  ScheduleEmailWizardPanel scheduleEmailWizardPanel;
  AbstractWizardDialog parentDialog;
  String filePath;
  JSONObject jobSchedule;

  JSONArray scheduleParams;
  JsJob editJob;

  Boolean done = false;

  private boolean newSchedule = true;

  public ScheduleEmailDialog( AbstractWizardDialog parentDialog, String filePath, JSONObject jobSchedule,
      JSONArray scheduleParams, JsJob editJob ) {
    super( ScheduleDialogType.SCHEDULER, Messages.getString( editJob == null ? "newSchedule" : "editSchedule" ),
      null, false, true );

    this.parentDialog = parentDialog;
    this.filePath = filePath;
    this.jobSchedule = jobSchedule;
    this.scheduleParams = scheduleParams;
    this.editJob = editJob;

    initDialog();
  }

  public void initDialog() {
    scheduleEmailWizardPanel = ScheduleFactory.getInstance().createScheduleEmailWizardPanel( filePath, jobSchedule, editJob, scheduleParams );
    IWizardPanel[] wizardPanels = { scheduleEmailWizardPanel };
    this.setWizardPanels( wizardPanels );
    setPixelSize( 635, 375 );
    wizardDeckPanel.setHeight( "100%" );
    setSize( "650px", "450px" );
    addStyleName( "schedule-email-dialog" );
    setResponsive( true );
    setSizingMode( DialogSizingMode.FILL_VIEWPORT );
    setWidthCategory( DialogWidthCategory.SMALL );
  }

  public boolean onKeyDownPreview( char key, int modifiers ) {
    if ( key == KeyCodes.KEY_ESCAPE ) {
      hide();
    }
    return true;
  }

  protected JSONArray getFinishScheduleParams() {
    JSONArray params = scheduleParams != null ? scheduleParams : new JSONArray();

    JsArray<JsSchedulingParameter> emailParams = scheduleEmailWizardPanel.getEmailParams();
    if ( emailParams != null ) {
      int index = params.size();
      for ( int i = 0; i < emailParams.length(); i++ ) {
        params.set( index++, new JSONObject( emailParams.get( i ) ) );
      }
    }

    if ( editJob != null ) {
      params.set( params.size(), generateLineageId() );
    }

    return params;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.gwt.widgets.client.wizards.AbstractWizardDialog#finish()
   */
  @Override
  protected boolean onFinish() {
    final JSONObject scheduleRequest = parseStrictScheduleJob();

    scheduleParams = getFinishScheduleParams();
    scheduleRequest.put( ScheduleParamsHelper.JOB_PARAMETERS_KEY, scheduleParams );

    RequestBuilder scheduleFileRequestBuilder = ScheduleHelper.buildRequestForJob( editJob, scheduleRequest );

    try {
      scheduleFileRequestBuilder.sendRequest( scheduleRequest.toString(), new RequestCallback() {

        @Override
        public void onError( Request request, Throwable exception ) {
          MessageDialogBox dialogBox =
              new MessageDialogBox( Messages.getString( "error" ), exception.toString(), false, false, true );
          dialogBox.center();
          setDone( false );
        }

        @Override
        public void onResponseReceived( Request request, Response response ) {
          if ( response.getStatusCode() == 200 ) {
            setDone( true );
            ScheduleEmailDialog.this.hide();
            if ( callback != null ) {
              callback.okPressed();
            }
          } else {
            String message = response.getText();
            if ( StringUtils.isEmpty( message ) ) {
              message = Messages.getString( "serverErrorColon" ) + " " + response.getStatusCode();
            }

            MessageDialogBox dialogBox = new MessageDialogBox( Messages.getString( "error" ), message,
              false, false, true );

            dialogBox.center();
            setDone( false );
          }
        }
      } );
    } catch ( RequestException e ) {
      MessageDialogBox dialogBox = new MessageDialogBox( Messages.getString( "error" ), e.toString(),
          false, false, true );
      dialogBox.center();
      setDone( false );
    }
    setDone( true );
    return true;
  }

  /* Visible for testing */
  JSONObject parseStrictScheduleJob() {
    return (JSONObject) JSONParser.parseStrict( jobSchedule.toString() );
  }

  /* Visible for testing */
  JSONObject generateLineageId() {
    return ScheduleParamsHelper.generateLineageId( editJob );
  }

  public Boolean getDone() {
    return done;
  }

  public void setDone( Boolean done ) {
    this.done = done;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.pentaho.gwt.widgets.client.wizards.AbstractWizardDialog#onNext(org.pentaho.gwt.widgets.client.wizards.
   * IWizardPanel, org.pentaho.gwt.widgets.client.wizards.IWizardPanel)
   */
  @Override
  protected boolean onNext( IWizardPanel nextPanel, IWizardPanel previousPanel ) {
    // TODO Auto-generated method stub
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.pentaho.gwt.widgets.client.wizards.AbstractWizardDialog#onPrevious(org.pentaho.gwt.widgets.client.wizards
   * .IWizardPanel, org.pentaho.gwt.widgets.client.wizards.IWizardPanel)
   */
  @Override
  protected void backClicked() {
    parentDialog.center();
    hide();
  }

  @Override
  public void center() {
    super.center();
    scheduleEmailWizardPanel.setFocus();
  }

  @Override
  protected boolean onPrevious( IWizardPanel previousPanel, IWizardPanel currentPanel ) {
    return true;
  }

  @Override
  protected boolean showBack( int index ) {
    return parentDialog != null;
  }

  @Override
  protected boolean showFinish( int index ) {
    return true;
  }

  @Override
  protected boolean showNext( int index ) {
    return false;
  }

  @Override
  protected boolean enableBack( int index ) {
    return true;
  }

  public void setCallback( IDialogCallback callback ) {
    this.callback = callback;
  }

  public IDialogCallback getCallback() {
    return callback;
  }

  public AbstractWizardDialog getParentDialog() {
    return parentDialog;
  }

  public void setParentDialog( AbstractWizardDialog parentDialog ) {
    this.parentDialog = parentDialog;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath( String filePath ) {
    this.filePath = filePath;
  }

  public JSONObject getJobSchedule() {
    return jobSchedule;
  }

  public void setJobSchedule( JSONObject jobSchedule ) {
    this.jobSchedule = jobSchedule;
  }

  public JSONArray getScheduleParams() {
    return scheduleParams;
  }

  public void setScheduleParams( JSONArray scheduleParams ) {
    this.scheduleParams = scheduleParams;
    this.scheduleEmailWizardPanel.setScheduleParams( scheduleParams );
    this.scheduleEmailWizardPanel.panelWidgetChanged( this );
  }

  public JsJob getEditJob() {
    return editJob;
  }

  public void setEditJob( JsJob editJob ) {
    this.editJob = editJob;
  }

  public void setNewSchedule( boolean newSchedule ) {
    this.newSchedule = newSchedule;
  }
}
