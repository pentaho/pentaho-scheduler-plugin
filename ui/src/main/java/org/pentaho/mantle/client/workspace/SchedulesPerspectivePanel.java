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

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleHelper;
import org.pentaho.mantle.client.environment.EnvironmentHelper;
import org.pentaho.mantle.client.messages.Messages;

import static org.pentaho.mantle.client.workspace.SchedulesPanel.ACCEPT;
import static org.pentaho.mantle.client.workspace.SchedulesPanel.APPLICATION_JSON;
import static org.pentaho.mantle.client.workspace.SchedulesPanel.IF_MODIFIED_SINCE;
import static org.pentaho.mantle.client.workspace.SchedulesPanel.IF_MODIFIED_SINCE_DATE;
import static org.pentaho.mantle.client.workspace.SchedulesPanel.TEXT_PLAIN;

public class SchedulesPerspectivePanel extends SimplePanel {
  static final int PAGE_SIZE = 25;
  private static final SchedulesPerspectivePanel instance = new SchedulesPerspectivePanel();
  private SchedulesPanel schedulesPanel;
  private BlockoutPanel blockoutPanel;
  private boolean isScheduler;
  private boolean isAdmin;
  private boolean canExecuteSchedules;
  private boolean hideInternalVariables;

  public static SchedulesPerspectivePanel getInstance() {
    return instance;
  }

  public SchedulesPerspectivePanel() {
    try {
      final String url = EnvironmentHelper.getFullyQualifiedURL() + "api/repo/files/canAdminister";
      RequestBuilder requestBuilder = new RequestBuilder( RequestBuilder.GET, url );
      requestBuilder.setHeader( ACCEPT, TEXT_PLAIN );
      requestBuilder.setHeader( IF_MODIFIED_SINCE, IF_MODIFIED_SINCE_DATE );
      requestBuilder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable caught ) {
          isAdmin = false;
          isScheduler = false;
          canExecuteSchedules = false;
        }

        public void onResponseReceived( Request request, Response response ) {
          isAdmin = "true".equalsIgnoreCase( response.getText() );
          canSchedule();
        }
      } );
    } catch ( RequestException e ) {
      Window.alert( e.getMessage() );
    }
  }

  private void canSchedule() {
    try {
      final String url = ScheduleHelper.getPluginContextURL() + "api/scheduler/canSchedule";
      RequestBuilder requestBuilder = new RequestBuilder( RequestBuilder.GET, url );
      requestBuilder.setHeader( ACCEPT, APPLICATION_JSON );
      requestBuilder.setHeader( IF_MODIFIED_SINCE, IF_MODIFIED_SINCE_DATE );
      requestBuilder.sendRequest( null, new RequestCallback() {
        public void onError( Request request, Throwable caught ) {
          isScheduler = false;
          canExecuteSchedules();
        }

        public void onResponseReceived( Request request, Response response ) {
          isScheduler = "true".equalsIgnoreCase( response.getText() );
          canExecuteSchedules();
        }
      } );
    } catch ( RequestException e ) {
      Window.alert( e.getMessage() );
    }
  }

  private void canExecuteSchedules() {
    try {
      final String url = ScheduleHelper.getPluginContextURL() + "api/scheduler/canExecuteSchedules";
      RequestBuilder requestBuilder = new RequestBuilder( RequestBuilder.GET, url );
      requestBuilder.setHeader( ACCEPT, APPLICATION_JSON );
      requestBuilder.setHeader( IF_MODIFIED_SINCE, IF_MODIFIED_SINCE_DATE );
      requestBuilder.sendRequest( null, new RequestCallback() {
        public void onError( Request request, Throwable caught ) {
          canExecuteSchedules = false;
          fetchHideInternalVariable();
        }

        public void onResponseReceived( Request request, Response response ) {
          canExecuteSchedules = "true".equalsIgnoreCase( response.getText() );
          fetchHideInternalVariable();
        }
      } );
    } catch ( RequestException e ) {
      Window.alert( e.getMessage() );
    }
  }

  protected void fetchHideInternalVariable(){
    RequestBuilder builder = ScheduleHelper.buildRequestForRetrieveHideInternalVariable();
    try {
      builder.setHeader( IF_MODIFIED_SINCE, IF_MODIFIED_SINCE_DATE );
      builder.sendRequest( null, new RequestCallback() {

        public void onError(Request request, Throwable exception ) {
          hideInternalVariables = false;
          createUI();
        }

        public void onResponseReceived( Request request, Response response ) {
          if ( response.getStatusCode() == Response.SC_OK ){
            String res = response.getText();
            res = res.replaceAll("\"", "");
            hideInternalVariables = "Y".equalsIgnoreCase( res );
            createUI();
          }
        }
      } );
    } catch ( RequestException e ) {
      Window.alert( e.getMessage() );
    }
  }

  private void createUI() {
    this.setStyleName( "schedulerPerspective" );
    this.addStyleName( "responsive" );

    VerticalPanel wrapperPanel = new VerticalPanel();

    String schedulesLabelStr = Messages.getString( "mySchedules" );

    if ( isAdmin ) {
      schedulesLabelStr = Messages.getString( "manageSchedules" );
    }

    Label schedulesLabel = new Label( schedulesLabelStr );
    schedulesLabel.setStyleName( "workspaceHeading" );
    wrapperPanel.add( schedulesLabel );

    schedulesPanel = new SchedulesPanel( isAdmin, isScheduler, canExecuteSchedules, hideInternalVariables );
    schedulesPanel.setStyleName( "schedulesPanel" );
    schedulesPanel.addStyleName( "schedules-panel-wrapper" );
    wrapperPanel.add( schedulesPanel );

    blockoutPanel = new BlockoutPanel( isAdmin );
    blockoutPanel.setStyleName( "schedulesPanel" );
    blockoutPanel.addStyleName( "blockout-schedules-panel-wrapper" );
    wrapperPanel.add( blockoutPanel );

    SimplePanel sPanel = new SimplePanel();
    sPanel.add( wrapperPanel );
    sPanel.setStylePrimaryName( "schedulerPerspective-wrapper" );
    add( sPanel );
  }

  public void refresh() {
    schedulesPanel.refresh();
    blockoutPanel.refresh();
    schedulesPanel.getServerTime();
  }

  public interface CellTableResources extends CellTable.Resources {
    @Override
    ImageResource cellTableSortAscending();

    @Override
    ImageResource cellTableSortDescending();

    /**
     * The styles used in this widget.
     */
    @Source( "org/pentaho/mantle/client/workspace/CellTable.css" )
    CellTable.Style cellTableStyle();
  }
}
