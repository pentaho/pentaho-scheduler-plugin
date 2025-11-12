/*
 * ! ******************************************************************************
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pentaho.gwt.widgets.client.dialogs.IDialogCallback;
import org.pentaho.gwt.widgets.client.dialogs.MessageDialogBox;
import org.pentaho.gwt.widgets.client.table.BaseTable;
import org.pentaho.gwt.widgets.client.table.ColumnComparators.BaseColumnComparator;
import org.pentaho.gwt.widgets.client.table.ColumnComparators.ColumnComparatorTypes;
import org.pentaho.gwt.widgets.client.toolbar.Toolbar;
import org.pentaho.gwt.widgets.client.toolbar.ToolbarButton;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;
import org.pentaho.mantle.client.dialogs.scheduling.NewBlockoutScheduleDialog;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleHelper;
import org.pentaho.mantle.client.environment.EnvironmentHelper;
import org.pentaho.mantle.client.messages.Messages;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.gen2.table.client.SelectionGrid.SelectionPolicy;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import static org.pentaho.gwt.widgets.client.utils.ImageUtil.getThemeableImage;

public class BlockoutPanel extends SimplePanel {
  private static final String ICON_ZOOMABLE = "icon-zoomable";

  private BaseTable table;
  private final List<JsJob> list = new ArrayList<>();
  private final VerticalPanel widgets = new VerticalPanel();
  private Button blockoutButton;
  private Toolbar tableControls;
  private VerticalPanel tablePanel;
  private ToolbarButton editButton;
  private ToolbarButton removeButton;
  private String serverTzString;
  private TimeZone serverTimeZone;
  private Map<String, TimeZone> timeZoneById = new HashMap<>();
  private static final String BLANK_VALUE = "-";

  private final IDialogCallback refreshCallBack = new IDialogCallback() {
    public void okPressed() {
      refresh();
    }

    public void cancelPressed() {
      refresh();
    }
  };
  private Label headlineLabel;
  private Label blockoutHeading;
  private final boolean isAdmin;

  public BlockoutPanel( final boolean isAdmin ) {
    this.isAdmin = isAdmin;
    getTimeZoneData();
    createUI( isAdmin );
    refresh();
  }

  private void getTimeZoneData() {

    String url = EnvironmentHelper.getFullyQualifiedURL() + "api/system/timezones"; //$NON-NLS-1$
    RequestBuilder timeZonesRequest = new RequestBuilder( RequestBuilder.GET, url );
    timeZonesRequest.setHeader( "accept", "application/json" ); //$NON-NLS-1$ //$NON-NLS-2$
    timeZonesRequest.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    try {
      timeZonesRequest.sendRequest( null, new RequestCallback() {

        @Override
        public void onResponseReceived( Request request, Response response ) {
          try {
            String responseText = response.getText();
            JSONObject root = JSONParser.parseLenient( responseText ).isObject();

            // 1) Read server TZ id: "America/Chicago"
            JSONString serverTZIdString = root.get( "serverTzId" ).isString();
            serverTzString = serverTZIdString.stringValue();

            // 2) Find corresponding entry in timeZones.entry[]
            JSONObject timeZonesObj = root.get( "timeZones" ).isObject();
            JSONArray entries = timeZonesObj.get( "entry" ).isArray();

            for ( int i = 0; i < entries.size(); i++ ) {
              JSONObject entry = entries.get( i ).isObject();
              String key = entry.get( "key" ).isString().stringValue();
              String value = entry.get( "value" ).isString().stringValue();
              int offsetMinutes = parseUtcOffsetMinutes( value );

              timeZoneById.put( key, TimeZone.createTimeZone( offsetMinutes ) );

              if ( serverTzString.equals( key ) ) {
                serverTimeZone = TimeZone.createTimeZone( offsetMinutes );
              }
            }

            if ( serverTimeZone == null ) {
              serverTimeZone = TimeZone.createTimeZone( 0 ); // fallback: UTC
            }
          } catch ( Exception e ) {
            serverTimeZone = TimeZone.createTimeZone( 0 ); // fallback: UTC
          }
        }

        private int parseUtcOffsetMinutes( String display ) {
          // display example: "America/Chicago - Central Daylight Time (UTC-0600)"
          int idx = display.indexOf( "(UTC" );
          if ( idx == -1 ) {
            return 0; // fallback: UTC
          }

          int signIndex = idx + 4; // char at this position is '+' or '-'
          if ( signIndex >= display.length() ) {
            return 0;
          }

          char signChar = display.charAt( signIndex );
          int start = signIndex + 1;
          int end = start + 4; // e.g. "0600"
          if ( end > display.length() ) {
            return 0;
          }

          String hhmm = display.substring( start, end ); // e.g. "0600"
          int hours = Integer.parseInt( hhmm.substring( 0, 2 ) );
          int minutes = Integer.parseInt( hhmm.substring( 2, 4 ) );
          int totalMinutes = hours * 60 + minutes;

          // yes, the sign is backwards; this is how the gwt TimeZone offset works
          if ( signChar == '+' ) {
            totalMinutes = -totalMinutes;
          }

          return totalMinutes;
        }

        @Override
        public void onError( Request request, Throwable exception ) {
          // TODO Auto-generated method stub

        }

      } );
    } catch ( RequestException e ) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private String formatRunDateColumn( Date date, String jobTimeZone ) {
    if ( date == null ) {
      return BLANK_VALUE;
    }

    DateTimeFormat formatMedium = DateTimeFormat.getFormat( "EEE, MMM dd h:mm a" );
    DateTimeFormat format = DateTimeFormat.getFormat( formatMedium.getPattern() );

    if ( jobTimeZone != null && timeZoneById.containsKey( jobTimeZone ) ) {
      String jobFormatted = format.format( date, timeZoneById.get( jobTimeZone ) );
      return jobFormatted + " " + jobTimeZone;
    } else {
      // fallback: server TZ if serverTimeZone not resolved (error case)
      return format.format( date, serverTimeZone ) + " " + serverTzString;
    }
  }

  private void createUI( final boolean isAdmin ) {
    widgets.setWidth( "100%" );

    createBlockoutHeadingBar();
    createHeadlineBar();
    createControls( isAdmin );
    createTable();

    widgets.add( tablePanel );
    setWidget( widgets );
  }

  private void createBlockoutHeadingBar() {
    blockoutHeading = new Label( "" );
    blockoutHeading.setStyleName( "workspaceHeading" );
    widgets.add( blockoutHeading );
  }

  private void createHeadlineBar() {
    headlineLabel = new Label( "" );
    widgets.add( headlineLabel );
  }

  private void createControls( final boolean isAdmin ) {
    blockoutButton = new Button( Messages.getString( "createBlockoutTime" ) );
    tableControls = new Toolbar();
    tablePanel = new VerticalPanel();
    tablePanel.setVisible( false );

    if ( isAdmin ) {
      final ClickHandler newBlockoutHandler = clickEvent -> {
        DialogBox blockoutDialog = new NewBlockoutScheduleDialog( "", refreshCallBack, false, true );
        blockoutDialog.center();
      };

      createBlockoutButton( newBlockoutHandler );
      createTableControls( newBlockoutHandler );
    }
  }

  private void createBlockoutButton( final ClickHandler newBlockoutHandler ) {
    SimplePanel buttonPanel = new SimplePanel();
    buttonPanel.setStyleName( "schedulesButtonPanel" );

    blockoutButton.addClickHandler( newBlockoutHandler );
    blockoutButton.setStyleName( "pentaho-button" );
    buttonPanel.add( blockoutButton );

    widgets.add( buttonPanel );
  }

  private void createTableControls( final ClickHandler newBlockoutHandler ) {
    ToolbarButton addButton = new ToolbarButton( getThemeableImage( "pentaho-addbutton", ICON_ZOOMABLE ) );
    addButton.setCommand( () -> newBlockoutHandler.onClick( null ) );
    addButton.setToolTip( Messages.getString( "blockoutAdd" ) );

    editButton = new ToolbarButton( getThemeableImage( "pentaho-editbutton", ICON_ZOOMABLE ) );
    editButton.setEnabled( false );
    editButton.setCommand( () -> {
      Set<JsJob> jobs = getSelectedSet();
      final JsJob jsJob = jobs.iterator().next();

      IDialogCallback callback = new IDialogCallback() {
        public void okPressed() {
          refreshCallBack.okPressed();
        }

        public void cancelPressed() {
          refreshCallBack.cancelPressed();
        }
      };

      NewBlockoutScheduleDialog blockoutDialog = new NewBlockoutScheduleDialog( jsJob, callback, false, true );
      table.selectRow( list.indexOf( jsJob ) );
      blockoutDialog.setUpdateMode();
      blockoutDialog.center();
    } );
    editButton.setToolTip( Messages.getString( "blockoutEdit" ) );

    removeButton = new ToolbarButton( getThemeableImage( "pentaho-deletebutton", ICON_ZOOMABLE ) );
    removeButton.setEnabled( false );
    removeButton.setCommand( () -> {
      final Set<JsJob> selectedSet = getSelectedSet();

      final MessageDialogBox blockoutDeleteWarningDialogBox = new MessageDialogBox(
        Messages.getString( "delete" ),
        Messages.getString( "deleteBlockoutWarning", "" + selectedSet.size() ),
        false,
        Messages.getString( "yesDelete" ),
        Messages.getString( "no" ) );

      final IDialogCallback callback = new IDialogCallback() {

        public void cancelPressed() {
          blockoutDeleteWarningDialogBox.hide();
        }

        public void okPressed() {
          for ( JsJob jsJob : selectedSet ) {
            removeBlockout( jsJob );
            table.selectRow( list.indexOf( jsJob ) );
          }
        }
      };
      blockoutDeleteWarningDialogBox.setCallback( callback );
      blockoutDeleteWarningDialogBox.center();
    } );
    removeButton.setToolTip( Messages.getString( "blockoutDelete" ) );

    tableControls.add( editButton );
    tableControls.add( addButton );
    tableControls.add( removeButton );
    tableControls.add( Toolbar.GLUE );

    tablePanel.add( tableControls );
  }

  private void createTable() {
    int columnSize = 139;
    String[] tableHeaderNames =
      { Messages.getString( "blockoutColumnStarts" ), Messages.getString( "blockoutColumnEnds" ),
        Messages.getString( "blockoutColumnRepeats" ), Messages.getString( "blockoutColumnRepeatsEndBy" ) };
    int[] columnWidths = { columnSize, columnSize, columnSize, columnSize };
    BaseColumnComparator[] columnComparators =
      { BaseColumnComparator.getInstance( ColumnComparatorTypes.DATE ),
        BaseColumnComparator.getInstance( ColumnComparatorTypes.DATE ),
        BaseColumnComparator.getInstance( ColumnComparatorTypes.STRING_NOCASE ),
        BaseColumnComparator.getInstance( ColumnComparatorTypes.STRING_NOCASE ) };

    table = new BaseTable( tableHeaderNames, columnWidths, columnComparators, SelectionPolicy.MULTI_ROW );
    table.getElement().setId( "blockout-table" );
    table.setWidth( "auto" );
    table.setHeight( "328px" );
    table.fillWidth();
    table.addRowSelectionHandler( event -> {
      boolean isSelected = !event.getNewValue().isEmpty();
      boolean isSingleSelect = event.getNewValue().size() == 1;
      editButton.setEnabled( isSingleSelect );
      removeButton.setEnabled( isSelected );
    } );
    tablePanel.add( table );
  }

  private void removeBlockout( final JsJob jsJob ) {
    JSONObject jobRequest = new JSONObject();
    jobRequest.put( "jobId", new JSONString( jsJob.getJobId() ) );
    makeServiceCall( "removeJob", RequestBuilder.DELETE, jobRequest.toString(), "text/plain", new RequestCallback() {

      public void onError( Request request, Throwable exception ) {
        // noop
      }

      public void onResponseReceived( Request request, Response response ) {
        if ( response.getStatusCode() == Response.SC_OK ) {
          refresh();
        }
      }
    } );
  }

  public void refresh() {
    final MessageDialogBox errorDialog = new MessageDialogBox(
      Messages.getString( "error" ), Messages.getString( "noBlockoutViewPermission" ), false, false, true );
    makeServiceCall( "blockout/blockoutjobs", RequestBuilder.GET, null, "application/json", new RequestCallback() {

      public void onError( Request request, Throwable exception ) {
        errorDialog.center();
      }

      public void onResponseReceived( Request request, Response response ) {
        if ( response.getStatusCode() == Response.SC_OK ) {
          if ( "null".equals( response.getText() ) ) {
            showData( null );
          } else {
            showData( parseJson( JsonUtils.escapeJsonForEval( response.getText() ) ) );
          }
        } else {
          errorDialog.center();
        }
      }
    } );
  }

  private void makeServiceCall( final String urlSuffix, final RequestBuilder.Method httpMethod,
                                final String requestData, final String acceptHeader, final RequestCallback callback ) {
    final String url = ScheduleHelper.getPluginContextURL() + "api/scheduler/" + urlSuffix;
    RequestBuilder builder = new RequestBuilder( httpMethod, url );
    builder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    builder.setHeader( "Content-Type", "application/json" );
    if ( !StringUtils.isEmpty( acceptHeader ) ) {
      builder.setHeader( "accept", acceptHeader );
    }
    try {
      builder.sendRequest( requestData, callback );
    } catch ( RequestException e ) {
      final MessageDialogBox errorDialog = new MessageDialogBox(
        Messages.getString( "error" ), Messages.getString( "noBlockoutViewPermission" ), false, false, true );
      errorDialog.center();
    }
  }

  private void showData( final JsArray<JsJob> allBlocks ) {
    blockoutHeading.setText( Messages.getString( "blockoutTimes" ) );
    if ( allBlocks == null || allBlocks.length() == 0 ) {
      tablePanel.setVisible( false );
      blockoutButton.setVisible( true );
      headlineLabel.setText( Messages.getString( "blockoutNone" ) );

      if ( !isAdmin ) {
        blockoutHeading.setVisible( false );
        headlineLabel.setVisible( false );
      }

    } else {
      tablePanel.setVisible( true );
      blockoutButton.setVisible( false );
      blockoutHeading.setVisible( true );
      headlineLabel.setText( Messages.getString( "blockoutHeadline" ) );

      if ( !isAdmin ) {
        headlineLabel.setVisible( true );
      }

      List<JsJob> jobList = new ArrayList<>();
      for ( int i = 0; i < allBlocks.length(); i++ ) {
        JsJob job = allBlocks.get( i );
        jobList.add( job );
      }

      list.clear();
      list.addAll( jobList );

      int row = 0;
      Object[][] tableContent = new Object[ list.size() ][ 4 ];
      for ( JsJob block : list ) {
        tableContent[ row ][ 0 ] = getStartValue( block );
        tableContent[ row ][ 1 ] = getEndValue( block );
        tableContent[ row ][ 2 ] = getRepeatValue( block );
        tableContent[ row ][ 3 ] = getRepeatEndValue( block );
        row++;
      }
      table.populateTable( tableContent );
      table.setVisible( !jobList.isEmpty() );
    }
  }

  private native JsArray<JsJob> parseJson( String json ) /*-{
      var obj = JSON.parse(json);
      return obj.job;
  }-*/;

  private String convertDateToValue( Date date, String timeZone ) {
    if ( date != null ) {
      try {
        return formatRunDateColumn( date, timeZone );
      } catch ( Throwable t ) {
        //ignored
      }
    }
    return "-";
  }

  private String getStartValue( JsJob block ) {

    long now = System.currentTimeMillis();
    long duration = block.getJobTrigger().getBlockDuration();
    Date lastRun = block.getLastRun();
    String timeZone = block.getJobTrigger().getTimeZone();

    // if we have a last execution and we are still within the range of that, the
    // starts / ends need to still reflect this rather than the next execution
    if ( lastRun != null && now < lastRun.getTime() + duration && now > lastRun.getTime() ) {
      return convertDateToValue( lastRun, timeZone );
    }

    if ( block.getNextRun() != null ) {
      return convertDateToValue( block.getNextRun(), timeZone );
    } else if ( block.getJobTrigger() != null && block.getJobTrigger().getStartTime() != null ) {
      return convertDateToValue( block.getJobTrigger().getStartTime(), timeZone );
    } else if ( "COMPLETE".equals( block.getState() ) && block.getJobTrigger() != null ) {
      // if a job is complete, it will not have the date in the nextRun attribute
      return convertDateToValue( block.getJobTrigger().getStartTime(), timeZone );
    } else {
      return "-";
    }
  }

  private String getEndValue( JsJob block ) {

    long now = System.currentTimeMillis();
    long duration = block.getJobTrigger().getBlockDuration();
    Date lastRun = block.getLastRun();
    String timeZone = block.getJobTrigger().getTimeZone();

    // if we have a last execution and we are still within the range of that, the
    // starts / ends need to still reflect this rather than the next execution
    if ( lastRun != null && now < lastRun.getTime() + duration && now > lastRun.getTime() ) {
      return convertDateToValue( new Date( lastRun.getTime() + duration ), timeZone );
    }

    if ( block.getNextRun() instanceof Date ) {
      return convertDateToValue( new Date( block.getNextRun().getTime() + duration ), timeZone );
    } else if ( "COMPLETE".equals( block.getState() ) && block.getJobTrigger() != null
      && block.getJobTrigger().getStartTime() != null ) {
      // if a job is complete, it will not have the date in the nextRun attribute
      return convertDateToValue( new Date( block.getJobTrigger().getStartTime().getTime()
        + block.getJobTrigger().getBlockDuration() ), block.getJobTrigger().getTimeZone() );
    } else {
      return "-";
    }
  }

  private String getRepeatValue( JsJob block ) {
    try {
      return block.getJobTrigger().getDescription( serverTzString );
    } catch ( Throwable t ) {
      //ignored
    }
    return "-";
  }

  private String getRepeatEndValue( JsJob block ) {
    try {
      Date endTime = block.getJobTrigger().getEndTime();
      String timeZone = block.getJobTrigger().getTimeZone();
      if ( endTime == null ) {
        return Messages.getString( "never" );
      } else {
        return formatRunDateColumn( endTime, timeZone );
      }
    } catch ( Throwable t ) {
      //ignored
    }
    return "-";
  }

  private Set<JsJob> getSelectedSet() {
    Set<Integer> selected = table.getSelectedRows();
    Set<JsJob> selectedSet = new HashSet<>();
    for ( Integer selectedRow : selected ) {
      selectedSet.add( list.get( selectedRow ) );
    }
    return selectedSet;
  }
}
