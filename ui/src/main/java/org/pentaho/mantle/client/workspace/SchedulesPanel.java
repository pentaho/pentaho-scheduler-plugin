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
 * Copyright (c) 2002-2024 Hitachi Vantara. All rights reserved.
 */
package org.pentaho.mantle.client.workspace;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestBuilder.Method;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.AbstractHeaderOrFooterBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.Range;
import org.pentaho.gwt.widgets.client.dialogs.IDialogCallback;
import org.pentaho.gwt.widgets.client.dialogs.MessageDialogBox;
import org.pentaho.gwt.widgets.client.genericfile.GenericFileNameUtils;
import org.pentaho.gwt.widgets.client.toolbar.Toolbar;
import org.pentaho.gwt.widgets.client.toolbar.ToolbarButton;
import org.pentaho.gwt.widgets.client.utils.NameUtils;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;
import org.pentaho.mantle.client.commands.RefreshSchedulesCommand;
import org.pentaho.mantle.client.csrf.CsrfRequestBuilder;
import org.pentaho.mantle.client.dialogs.scheduling.NewScheduleDialog;
import org.pentaho.mantle.client.dialogs.scheduling.OutputLocationUtils;
import org.pentaho.mantle.client.dialogs.scheduling.ParameterPreviewSidebar;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleFactory;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleHelper;
import org.pentaho.mantle.client.environment.EnvironmentHelper;
import org.pentaho.mantle.client.messages.Messages;
import org.pentaho.mantle.client.ui.column.HtmlColumn;
import org.pentaho.mantle.client.workspace.SchedulesPerspectivePanel.CellTableResources;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.pentaho.gwt.widgets.client.utils.ImageUtil.getThemeableImage;
import static org.pentaho.mantle.client.workspace.SchedulesPerspectivePanel.PAGE_SIZE;

@SuppressWarnings( { "java:S110", "java:S1192", "java:S3776" } )
public class SchedulesPanel extends SimplePanel {
  private static final String JOB_STATE_REMOVED = "REMOVED";
  private static final String JOB_STATE_NORMAL = "NORMAL";
  private static final String SCHEDULER_STATE_RUNNING = "RUNNING";

  public static final String ACCEPT = "Accept";
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String APPLICATION_JSON = "application/json";
  public static final String TEXT_PLAIN = "text/plain";
  public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
  public static final String IF_MODIFIED_SINCE_DATE = "01 Jan 1970 00:00:00 GMT";

  private static final String ICON_SMALL_STYLE = "icon-small";
  private static final String ICON_RUN_STYLE = "icon-run";
  private static final String ICON_ZOOMABLE = "icon-zoomable";

  private static final String BLANK_VALUE = "-";

  private static final int READ_PERMISSION = 0;

  private static final int OUTPUT_PATH_COLUMN = 3;

  private final ToolbarButton controlScheduleButton = new ToolbarButton( getThemeableImage(
    ICON_SMALL_STYLE, ICON_RUN_STYLE, ICON_ZOOMABLE ) );
  private final ToolbarButton editButton = new ToolbarButton( getThemeableImage(
    "pentaho-editbutton", ICON_ZOOMABLE ) );
  private final ToolbarButton triggerNowButton = new ToolbarButton( getThemeableImage(
    ICON_SMALL_STYLE, "icon-execute", ICON_ZOOMABLE ) );
  private final ToolbarButton scheduleRemoveButton = new ToolbarButton( getThemeableImage(
    "pentaho-deletebutton", ICON_ZOOMABLE ) );
  private final ToolbarButton filterButton = new ToolbarButton( getThemeableImage(
    ICON_SMALL_STYLE, "icon-filter-add", ICON_ZOOMABLE ) );
  private final ToolbarButton filterRemoveButton = new ToolbarButton( getThemeableImage(
    ICON_SMALL_STYLE, "icon-filter-remove", ICON_ZOOMABLE ) );

  private Header<Boolean> selectAllHeader;

  private JsArray<JsJob> allJobs;

  private final ArrayList<IJobFilter> filters = new ArrayList<>();

  private final CellTable<JsJob> table =
    new CellTable<>( PAGE_SIZE, (CellTableResources) GWT.create( CellTableResources.class ) );

  private final ListDataProvider<JsJob> dataProvider = new ListDataProvider<>();

  private SimplePager pager;

  private FilterDialog filterDialog;

  private final IDialogCallback filterDialogCallback = new IDialogCallback() {
    public void okPressed() {
      filters.clear();

      // create filters
      if ( filterDialog.getAfterDate() != null ) {
        filters.add( job -> job.getNextRun().after( filterDialog.getAfterDate() ) );
      }

      if ( filterDialog.getBeforeDate() != null ) {
        filters.add( job -> job.getNextRun().before( filterDialog.getBeforeDate() ) );
      }

      if ( !StringUtils.isEmpty( filterDialog.getResourceName() ) ) {
        filters.add(
          job -> job.getShortResourceName().toLowerCase().contains( filterDialog.getResourceName().toLowerCase() ) );
      }

      final String showAll = Messages.getString( "showAll" );

      if ( !StringUtils.isEmpty( filterDialog.getUserFilter() ) && !filterDialog.getUserFilter().equals( showAll ) ) {
        filters.add( job -> job.getUserName().equalsIgnoreCase( filterDialog.getUserFilter() ) );
      }

      if ( !StringUtils.isEmpty( filterDialog.getStateFilter() ) && !filterDialog.getStateFilter().equals( showAll ) ) {
        filters.add( job -> job.getState().toLowerCase().equalsIgnoreCase( filterDialog.getStateFilter() ) );
      }

      if ( !StringUtils.isEmpty( filterDialog.getTypeFilter() ) && !filterDialog.getTypeFilter().equals( showAll ) ) {
        filters.add( job -> job.getJobTrigger().getScheduleType().equalsIgnoreCase( filterDialog.getTypeFilter() ) );
      }

      filterRemoveButton.setEnabled( !filters.isEmpty() );
      filterAndShowData();
    }

    public void cancelPressed() {
      /* noop */
    }
  };

  @SuppressWarnings( "unchecked" )
  private Set<JsJob> getSelectedJobs() {
    return ( (MultiSelectionModel<JsJob>) table.getSelectionModel() ).getSelectedSet();
  }

  @SuppressWarnings( "unchecked" )
  private void clearJobsSelection() {
    ( (MultiSelectionModel<JsJob>) table.getSelectionModel() ).clear();
  }

  private final IDialogCallback scheduleDialogCallback = new IDialogCallback() {
    public void okPressed() {
      refresh();

      MessageDialogBox dialogBox = new MessageDialogBox( Messages.getString( "scheduleUpdatedTitle" ),
        Messages.getString( "scheduleUpdatedMessage" ), false, false, true );

      dialogBox.center();
    }

    public void cancelPressed() {
      /* noop */
    }
  };

  public SchedulesPanel( final boolean isAdmin, final boolean isScheduler ) {
    createUI( isAdmin, isScheduler );
    refresh();
  }

  public void refresh() {
    final String apiEndpoint = "api/scheduler/getJobs";

    RequestBuilder executableTypesRequestBuilder =
      createRequestBuilder( RequestBuilder.GET, ScheduleHelper.getPluginContextURL(), apiEndpoint );
    executableTypesRequestBuilder.setHeader( ACCEPT, APPLICATION_JSON );
    final MessageDialogBox errorDialog =
      new MessageDialogBox(
        Messages.getString( "error" ), Messages.getString( "noScheduleViewPermission" ), false, false, true );

    try {
      executableTypesRequestBuilder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
          errorDialog.center();
        }

        public void onResponseReceived( Request request, Response response ) {
          if ( response.getStatusCode() == Response.SC_OK ) {
            allJobs = parseJson( JsonUtils.escapeJsonForEval( response.getText() ) );
            filterAndShowData();
          } else {
            errorDialog.center();
          }
        }
      } );
    } catch ( RequestException e ) {
      errorDialog.center();
    }
  }

  private void filterAndShowData() {
    filters.add( job -> !job.getFullResourceName().equals( "GeneratedContentCleaner" ) );

    ArrayList<JsJob> filteredList = new ArrayList<>();

    for ( int i = 0; i < allJobs.length(); i++ ) {
      filteredList.add( allJobs.get( i ) );

      // filter if needed
      for ( IJobFilter filter : filters ) {
        if ( !filter.accept( allJobs.get( i ) ) ) {
          filteredList.remove( allJobs.get( i ) );
        }
      }
    }

    if ( filteredList.isEmpty() ) {
      selectAllHeader.setHeaderStyleNames( "cellTableSelectAllHeader" );
    } else {
      selectAllHeader.setHeaderStyleNames( "" );
    }

    List<JsJob> list = dataProvider.getList();
    list.clear();
    list.addAll( filteredList );
    pager.setVisible( filteredList.size() > PAGE_SIZE );

    clearJobsSelection();

    editButton.setEnabled( false );
    controlScheduleButton.setEnabled( false );
    scheduleRemoveButton.setEnabled( false );
    triggerNowButton.setEnabled( false );
    table.setPageStart( 0 );

    table.setKeyboardSelectedRow( 0, false );
    table.setKeyboardSelectedColumn( 0, false );

    table.redraw();
  }

  private void updateControlSchedulerButtonState( final ToolbarButton controlSchedulerButton,
                                                  final boolean isScheduler ) {
    RequestBuilder builder =
      createRequestBuilder( RequestBuilder.GET, ScheduleHelper.getPluginContextURL(), "api/scheduler/state" );

    try {
      builder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
          // showError(exception);
        }

        public void onResponseReceived( Request request, Response response ) {
          updateControlSchedulerButtonStyle( controlSchedulerButton, response.getText() );

          controlSchedulerButton.setEnabled( isScheduler );
        }
      } );
    } catch ( RequestException e ) {
      // showError(e);
    }
  }

  private void updateControlSchedulerButtonStyle( ToolbarButton controlSchedulerButton, String state ) {
    boolean isRunning = SCHEDULER_STATE_RUNNING.equalsIgnoreCase( state );

    final String tooltip = isRunning ? Messages.getString( "stopScheduler" ) : Messages.getString( "startScheduler" );
    controlSchedulerButton.setToolTip( tooltip );

    final String buttonIconCss = isRunning ? "icon-stop-scheduler" : "icon-start-scheduler";
    controlSchedulerButton.setImage( getThemeableImage( ICON_SMALL_STYLE, buttonIconCss ) );
  }

  private void updateJobScheduleButtonStyle( String state ) {
    boolean isRunning = JOB_STATE_NORMAL.equalsIgnoreCase( state );

    String controlButtonCss = isRunning ? "icon-stop" : ICON_RUN_STYLE;
    controlScheduleButton.setImage( getThemeableImage( ICON_SMALL_STYLE, controlButtonCss, ICON_ZOOMABLE ) );

    String controlButtonTooltip = isRunning ? Messages.getString( "stop" ) : Messages.getString( "start" );
    controlScheduleButton.setToolTip( controlButtonTooltip );
  }

  private void toggleSchedulerOnOff( final ToolbarButton controlSchedulerButton, final boolean isScheduler ) {
    RequestBuilder builder =
      createRequestBuilder( RequestBuilder.GET, ScheduleHelper.getPluginContextURL(), "api/scheduler/state" );

    try {
      builder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
          // showError(exception);
        }

        public void onResponseReceived( Request request, Response response ) {
          boolean isRunning = SCHEDULER_STATE_RUNNING.equalsIgnoreCase( response.getText() );

          final String action = isRunning ? "pause" : "start";
          controlScheduler( controlSchedulerButton, action, isScheduler );
        }
      } );
    } catch ( RequestException e ) {
      // showError(e);
    }
  }

  private void createUI( boolean isAdmin, final boolean isScheduler ) {
    table.getElement().setId( "schedule-table" );
    table.setStylePrimaryName( "pentaho-table" );
    table.setWidth( "100%", true );

    // BISERVER-9331 Column sort indicators should be to the right of header text in the Manage Schedules table.
    if ( table.getHeaderBuilder() instanceof AbstractHeaderOrFooterBuilder ) {
      ( (AbstractHeaderOrFooterBuilder<JsJob>) table.getHeaderBuilder() ).setSortIconStartOfLine( false );
    }

    final MultiSelectionModel<JsJob> selectionModel = new MultiSelectionModel<>( JsJob::getJobId );
    table.setSelectionModel( selectionModel, DefaultSelectionEventManager.createCheckboxManager() );

    Label noDataLabel = new Label( Messages.getString( "noSchedules" ) );
    noDataLabel.setStyleName( "noDataForScheduleTable" );
    table.setEmptyTableWidget( noDataLabel );

    TextColumn<JsJob> idColumn = new TextColumn<JsJob>() {
      public String getValue( JsJob job ) {
        return job.getJobId();
      }
    };
    idColumn.setSortable( true );

    Column<JsJob, Boolean> checkColumn = new Column<JsJob, Boolean>( new CheckboxCell( true, false ) ) {
      @Override
      public Boolean getValue( JsJob object ) {
        return selectionModel.isSelected( object );
      }
    };

    selectAllHeader = new Header<Boolean>( new CheckboxCell( true, true ) ) {
      @Override
      public Boolean getValue() {
        int selectedCount = selectionModel.getSelectedSet().size();

        return selectedCount != 0 && selectedCount == dataProvider.getList().size();
      }
    };

    selectAllHeader.setUpdater( value -> {
      for ( JsJob item : dataProvider.getList() ) {
        selectionModel.setSelected( item, value );
      }
    } );

    TextColumn<JsJob> nameColumn = new TextColumn<JsJob>() {
      public String getValue( JsJob job ) {
        return job.getJobName();
      }
    };
    nameColumn.setSortable( true );

    TextColumn<JsJob> type = new TextColumn<JsJob>() {
      public String getValue( JsJob job ) {
        return job.getScheduledExtn();
      }
    };
    type.setSortable( true );

    HtmlColumn<JsJob> resourceColumn = new HtmlColumn<JsJob>() {
      @Override
      public String getStringValue( JsJob job ) {
        String fullName = job.getFullResourceName();
        if ( null == fullName || fullName.isEmpty() ) {
          return "";
        }
        int lastDotIndex = fullName.lastIndexOf( "." );
        String name = ( lastDotIndex > 0 ) ? fullName.substring( 0, lastDotIndex ) : fullName;
        return name.replace( "/", "/<wbr/>" );
      }
    };
    resourceColumn.setSortable( true );

    HtmlColumn<JsJob> outputPathColumn = new HtmlColumn<JsJob>( new ClickableSafeHtmlCell() ) {
      @Override
      public String getStringValue( JsJob jsJob ) {
        try {
          String outputPath = jsJob.getOutputPath();
          if ( StringUtils.isEmpty( outputPath ) ) {
            return BLANK_VALUE;
          }

          if ( !GenericFileNameUtils.isRepositoryPath( outputPath ) ) {
            return outputPath;
          }

          outputPath = new SafeHtmlBuilder().appendEscaped( outputPath ).toSafeHtml().asString();

          return MessageFormat.format( "<span class='workspace-resource-link' title='{0}'>{0}</span>", outputPath );
        } catch ( Exception e ) {
          return BLANK_VALUE;
        }
      }
    };
    HtmlColumn<JsJob> parametersColumn = new HtmlColumn<JsJob>( new ClickableSafeHtmlCell() ) {
      @Override
      public String getStringValue( JsJob job ) {
        int numParams = SchedulerUiUtil.getFilteredJobParams( job ).size();
        if ( numParams == 0 ) {
          return "-";
        } else {
          return MessageFormat.format( "<span class='workspace-resource-link' title='{0}'>{0}</span>",
            String.valueOf( numParams ) );
        }
      }
    };
    parametersColumn.setFieldUpdater( ( i, job, safeHtml ) -> {
      if ( !SchedulerUiUtil.getFilteredJobParams( job ).isEmpty() ) {
        new ParameterPreviewSidebar( job ).show();
      }
    } );
    parametersColumn.setSortable( true );

    outputPathColumn.setFieldUpdater( ( index, jsJob, value ) -> {
      if ( value != null && !BLANK_VALUE.equals( value.asString() ) ) {
        final Command errorCallback = this::showValidateOutputLocationError;
        final Command successCallback = () -> openOutputLocation( jsJob.getOutputPath() );
        OutputLocationUtils.validateOutputLocation( jsJob.getOutputPath(), successCallback, errorCallback );
      }
    } );

    outputPathColumn.setSortable( true );

    TextColumn<JsJob> scheduleColumn = new TextColumn<JsJob>() {
      public String getValue( JsJob job ) {
        try {
          return job.getJobTrigger().getDescription();
        } catch ( Exception e ) {
          return BLANK_VALUE;
        }
      }
    };
    scheduleColumn.setSortable( true );

    TextColumn<JsJob> userNameColumn = new TextColumn<JsJob>() {
      public String getValue( JsJob job ) {
        try {
          return job.getUserName();
        } catch ( Exception e ) {
          return BLANK_VALUE;
        }
      }
    };
    userNameColumn.setSortable( true );

    TextColumn<JsJob> stateColumn = new TextColumn<JsJob>() {
      public String getValue( JsJob job ) {
        try {
          // BISERVER-9965
          final String jobState = "COMPLETE".equalsIgnoreCase( job.getState() ) ? "FINISHED" : job.getState();
          // not css text-transform because tooltip will use pure text from the cell
          return jobState.substring( 0, 1 ).toUpperCase() + jobState.substring( 1 ).toLowerCase();
        } catch ( Exception e ) {
          return BLANK_VALUE;
        }
      }
    };
    stateColumn.setSortable( true );

    TextColumn<JsJob> nextFireColumn = new TextColumn<JsJob>() {
      public String getValue( JsJob job ) {
        try {
          Date date = job.getNextRun();
          if ( date == null ) {
            return BLANK_VALUE;
          }

          DateTimeFormat format = DateTimeFormat.getFormat( PredefinedFormat.DATE_TIME_MEDIUM );

          return format.format( date );
        } catch ( Exception e ) {
          return BLANK_VALUE;
        }
      }
    };
    nextFireColumn.setSortable( true );

    TextColumn<JsJob> lastFireColumn = new TextColumn<JsJob>() {
      public String getValue( JsJob job ) {
        try {
          Date date = job.getLastRun();
          if ( date == null ) {
            return BLANK_VALUE;
          }

          DateTimeFormat format = DateTimeFormat.getFormat( PredefinedFormat.DATE_TIME_MEDIUM );
          return format.format( date );
        } catch ( Exception e ) {
          return BLANK_VALUE;
        }
      }
    };
    lastFireColumn.setSortable( true );

    table.addColumn( checkColumn, selectAllHeader );
    table.addColumn( nameColumn, Messages.getString( "scheduleName" ) );
    table.addColumn( scheduleColumn, Messages.getString( "recurrence" ) );
    table.addColumn( type, Messages.getString( "Type" ) );
    table.addColumn( resourceColumn, Messages.getString( "sourceFile" ) );
    table.addColumn( outputPathColumn, Messages.getString( "outputPath" ) );
    table.addColumn( parametersColumn, Messages.getString( "parameters" ) );
    table.addColumn( lastFireColumn, Messages.getString( "lastFire" ) );
    table.addColumn( nextFireColumn, Messages.getString( "nextFire" ) );

    if ( isAdmin ) {
      table.addColumn( userNameColumn, Messages.getString( "user" ) );
    }

    table.addColumn( stateColumn, Messages.getString( "state" ) );

    table.addColumnStyleName( 0, "backgroundContentHeaderTableCell" );
    table.addColumnStyleName( 1, "backgroundContentHeaderTableCell" );
    table.addColumnStyleName( 2, "backgroundContentHeaderTableCell" );
    table.addColumnStyleName( 3, "backgroundContentHeaderTableCell" );
    table.addColumnStyleName( 4, "backgroundContentHeaderTableCell" );
    table.addColumnStyleName( 5, "backgroundContentHeaderTableCell" );
    table.addColumnStyleName( 6, "backgroundContentHeaderTableCell" );
    table.addColumnStyleName( 7, "backgroundContentHeaderTableCell" );

    if ( isAdmin ) {
      table.addColumnStyleName( 8, "backgroundContentHeaderTableCell" );
    }

    table.addColumnStyleName( ( isAdmin ) ? 9 : 8, "backgroundContentHeaderTableCell" );

    table.setColumnWidth( checkColumn, 40, Unit.PX );
    table.setColumnWidth( nameColumn, 160, Unit.PX );
    table.setColumnWidth( resourceColumn, 200, Unit.PX );
    table.setColumnWidth( outputPathColumn, 180, Unit.PX );
    table.setColumnWidth( scheduleColumn, 170, Unit.PX );
    table.setColumnWidth( lastFireColumn, 120, Unit.PX );
    table.setColumnWidth( nextFireColumn, 120, Unit.PX );
    table.setColumnWidth( type, 120, Unit.PX );
    table.setColumnWidth( parametersColumn, 120, Unit.PX );

    if ( isAdmin ) {
      table.setColumnWidth( userNameColumn, 100, Unit.PX );
    }

    table.setColumnWidth( stateColumn, 90, Unit.PX );

    dataProvider.addDataDisplay( table );
    List<JsJob> list = dataProvider.getList();

    ListHandler<JsJob> columnSortHandler = new ListHandler<>( list );

    columnSortHandler.setComparator( idColumn, ( o1, o2 ) -> {
      if ( o1 == o2 ) {
        return 0;
      }

      if ( o1 != null ) {
        return ( o2 != null ) ? o1.getJobId().compareTo( o2.getJobId() ) : 1;
      }
      return -1;
    } );

    columnSortHandler.setComparator( nameColumn, ( o1, o2 ) -> {
      if ( o1 == o2 ) {
        return 0;
      }

      if ( o1 != null ) {
        return ( o2 != null ) ? o1.getJobName().compareTo( o2.getJobName() ) : 1;
      }
      return -1;
    } );

    columnSortHandler.setComparator( resourceColumn, ( o1, o2 ) -> {
      if ( o1 == o2 ) {
        return 0;
      }

      if ( o1 != null ) {
        String r1 = o1.getShortResourceName();
        String r2 = null;

        if ( o2 != null ) {
          r2 = o2.getShortResourceName();
        }

        return ( o2 != null ) ? r1.compareTo( r2 ) : 1;
      }
      return -1;
    } );

    columnSortHandler.setComparator( outputPathColumn, ( o1, o2 ) -> {
      if ( o1 == o2 ) {
        return 0;
      }

      if ( o1 != null ) {
        String r1 = o1.getOutputPath();
        String r2 = null;

        if ( o2 != null ) {
          r2 = o2.getOutputPath();
        }

        return ( o2 != null ) ? r1.compareTo( r2 ) : 1;
      }
      return -1;
    } );

    columnSortHandler.setComparator( scheduleColumn, ( o1, o2 ) -> {
      String s1 = o1.getJobTrigger().getDescription();
      String s2 = o2.getJobTrigger().getDescription();
      return s1.compareTo( s2 );
    } );

    columnSortHandler.setComparator( userNameColumn, ( o1, o2 ) -> {
      if ( o1 == o2 ) {
        return 0;
      }

      if ( o1 != null ) {
        return ( o2 != null ) ? o1.getUserName().compareTo( o2.getUserName() ) : 1;
      }
      return -1;
    } );

    columnSortHandler.setComparator( stateColumn, ( o1, o2 ) -> {
      if ( o1 == o2 ) {
        return 0;
      }

      if ( o1 != null ) {
        return ( o2 != null ) ? o1.getState().compareTo( o2.getState() ) : 1;
      }
      return -1;
    } );

    columnSortHandler.setComparator( nextFireColumn, ( o1, o2 ) -> {
      if ( o1 == o2 ) {
        return 0;
      }

      if ( o1 == null || o1.getNextRun() == null ) {
        return -1;
      }

      if ( o2 == null || o2.getNextRun() == null ) {
        return 1;
      }

      if ( o1.getNextRun() == o2.getNextRun() ) {
        return 0;
      }

      return o1.getNextRun().compareTo( o2.getNextRun() );
    } );

    columnSortHandler.setComparator( lastFireColumn, ( o1, o2 ) -> {
      if ( o1 == o2 ) {
        return 0;
      }

      if ( o1 == null || o1.getLastRun() == null ) {
        return -1;
      }
      if ( o2 == null || o2.getLastRun() == null ) {
        return 1;
      }

      if ( o1.getLastRun() == o2.getLastRun() ) {
        return 0;
      }

      return o1.getLastRun().compareTo( o2.getLastRun() );
    } );

    table.addColumnSortHandler( columnSortHandler );

    table.getColumnSortList().push( idColumn );
    table.getColumnSortList().push( resourceColumn );
    table.getColumnSortList().push( outputPathColumn );
    table.getColumnSortList().push( nameColumn );

    table.getSelectionModel().addSelectionChangeHandler( event -> {
      Set<JsJob> selectedJobs = getSelectedJobs();

      if ( !selectedJobs.isEmpty() ) {
        final JsJob job = selectedJobs.toArray( new JsJob[ 0 ] )[ 0 ];
        updateJobScheduleButtonStyle( job.getState() );

        editButton.setEnabled( isScheduler );
        controlScheduleButton.setEnabled( isScheduler );
        scheduleRemoveButton.setEnabled( isScheduler );
        triggerNowButton.setEnabled( isScheduler );
      } else {
        editButton.setEnabled( false );
        controlScheduleButton.setEnabled( false );
        scheduleRemoveButton.setEnabled( false );
        triggerNowButton.setEnabled( false );
      }
    } );

    // BISERVER-9965
    table.addCellPreviewHandler( event -> {
      if ( "mouseover".equals( event.getNativeEvent().getType() ) ) {
        final TableCellElement cell =
          table.getRowElement( event.getIndex() - table.getPageStart() ).getCells().getItem( event.getColumn() );
        cell.setTitle( cell.getInnerText() );
      }

      if ( BrowserEvents.CLICK.equals( event.getNativeEvent().getType() ) ) {
        // Get the selected line
        JsJob selectedData = table.getVisibleItem( event.getIndex() - table.getPageStart() );
        if ( selectedData != null && event.getColumn() != 0 ) {
          selectionModel.setSelected( selectedData, !selectionModel.isSelected( selectedData ) );
        }
      }

    } );

    /*
     * For Left & Right, the below code is there to override the base class implementation i.e.,
     * navigates focus only amongst interactive cells, while our implementation navigates through all cells.
     *
     * For Enter, goes to the output path.
     */
    table.setKeyboardSelectionHandler( new AbstractCellTable.CellTableKeyboardSelectionHandler<JsJob>( table ) {
      @Override
      public void onCellPreview( CellPreviewEvent<JsJob> event ) {
        int keyboardSelectedColumn = table.getKeyboardSelectedColumn();

        if ( BrowserEvents.KEYDOWN.equals( event.getNativeEvent().getType() ) ) {
          if ( event.getNativeEvent().getKeyCode() == KeyCodes.KEY_RIGHT ) {
            table.setKeyboardSelectedColumn( Math.min( keyboardSelectedColumn + 1, table.getColumnCount() - 1 ) );
            handledEvent( event );
          } else if ( event.getNativeEvent().getKeyCode() == KeyCodes.KEY_LEFT ) {
            table.setKeyboardSelectedColumn( Math.max( 0, keyboardSelectedColumn - 1 ) );
            handledEvent( event );
          } else if ( event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER
            && keyboardSelectedColumn == OUTPUT_PATH_COLUMN ) {
            int index = event.getIndex();
            JsJob job = table.getVisibleItem( event.getIndex() );
            outputPathColumn.getFieldUpdater().update( index, job, outputPathColumn.getValue( job ) );
          } else {
            super.onCellPreview( event );
          }
        } else {
          super.onCellPreview( event );
        }
      }

      private void handledEvent( CellPreviewEvent<?> event ) {
        event.setCanceled( true );
        event.getNativeEvent().preventDefault();
      }
    } );

    SimplePager.Resources pagerResources = GWT.create( SimplePager.Resources.class );
    pager = new SimplePager( TextLocation.CENTER, pagerResources, false, 0, true ) {
      @Override
      public void setPageStart( int index ) {
        if ( getDisplay() != null ) {
          Range range = getDisplay().getVisibleRange();
          int pageSize = range.getLength();

          index = Math.max( 0, index );
          if ( index != range.getStart() ) {
            getDisplay().setVisibleRange( index, pageSize );
          }
        }
      }
    };
    pager.setDisplay( table );

    VerticalPanel tableAndPager = new VerticalPanel();
    tableAndPager.setHorizontalAlignment( HasHorizontalAlignment.ALIGN_CENTER );

    tableAndPager.add( createToolbarUI( isAdmin, isScheduler ) );
    tableAndPager.add( table );
    tableAndPager.add( pager );

    // Add it to the root panel.
    setWidget( tableAndPager );
  }

  private Toolbar createToolbarUI( boolean isAdmin, final boolean isScheduler ) {
    Toolbar bar = new Toolbar();

    // Add refresh button
    ToolbarButton refresh = new ToolbarButton( getThemeableImage( ICON_SMALL_STYLE, "icon-refresh", ICON_ZOOMABLE ) );
    refresh.setToolTip( Messages.getString( "refreshTooltip" ) );
    refresh.setCommand( () -> {
      RefreshSchedulesCommand cmd = new RefreshSchedulesCommand();
      cmd.execute();
    } );
    bar.add( refresh );


    // Add control schedule button
    controlScheduleButton.setCommand( () -> {
      Set<JsJob> selectedJobs = getSelectedJobs();

      if ( !selectedJobs.isEmpty() ) {
        final JsJob job = selectedJobs.toArray( new JsJob[ 0 ] )[ 0 ];

        boolean isRunning = JOB_STATE_NORMAL.equalsIgnoreCase( job.getState() );

        final String action = isRunning ? "pauseJob" : "resumeJob";
        controlJobs( selectedJobs, action, RequestBuilder.POST, false );
      }
    } );
    controlScheduleButton.setEnabled( false );
    bar.add( controlScheduleButton );

    bar.addSpacer( 20 );

    // Add execute now button
    triggerNowButton.setToolTip( Messages.getString( "executeNow" ) );
    triggerNowButton.setCommand( () -> {
      Set<JsJob> selectedJobs = getSelectedJobs();
      if ( !selectedJobs.isEmpty() ) {
        triggerExecuteNow( selectedJobs );
      }
    } );
    triggerNowButton.setEnabled( false );
    bar.add( triggerNowButton );

    // Add control scheduler button
    if ( isAdmin ) {
      final ToolbarButton controlSchedulerButton = new ToolbarButton( getThemeableImage(
        ICON_SMALL_STYLE, "icon-start-scheduler", ICON_ZOOMABLE ) );

      controlSchedulerButton.setCommand( () -> toggleSchedulerOnOff( controlSchedulerButton, isScheduler ) );
      updateControlSchedulerButtonState( controlSchedulerButton, isScheduler );

      bar.add( controlSchedulerButton );
    }

    bar.addSpacer( 20 );

    // Add filter button
    filterButton.setCommand( () -> {
      if ( filterDialog == null ) {
        filterDialog = new FilterDialog( allJobs, filterDialogCallback );
      } else {
        filterDialog.initUI( allJobs );
      }

      filterDialog.center();
    } );

    filterButton.setToolTip( Messages.getString( "filterSchedules" ) );

    if ( isAdmin ) {
      bar.add( filterButton );
    }

    // Add remove filters button
    filterRemoveButton.setCommand( () -> {
      filterDialog = null;
      filters.clear();
      filterAndShowData();
      filterRemoveButton.setEnabled( false );
      filterButton.setImage( getThemeableImage( ICON_SMALL_STYLE, "icon-filter-add", ICON_ZOOMABLE ) );
    } );

    filterRemoveButton.setToolTip( Messages.getString( "removeFilters" ) );
    filterRemoveButton.setEnabled( !filters.isEmpty() );

    if ( isAdmin ) {
      bar.add( filterRemoveButton );
    }

    bar.addSpacer( 20 );

    // Add edit button
    editButton.setCommand( () -> {
      Set<JsJob> selectedJobs = getSelectedJobs();

      if ( !selectedJobs.isEmpty() ) {
        final JsJob editJob = selectedJobs.toArray( new JsJob[ 0 ] )[ 0 ];

        canAccessJobRequest( editJob, new RequestCallback() {
          public void onError( Request request, Throwable exception ) {
            promptForScheduleResourceError( Collections.singleton( editJob ) );
          }

          public void onResponseReceived( Request request, Response response ) {
            boolean canEditJob = "true".equalsIgnoreCase( response.getText() );

            if ( !canEditJob ) {
              promptForScheduleResourceError( Collections.singleton( editJob ) );
              return;
            }

            editJob( editJob );
          }
        } );
      }
    } );
    editButton.setEnabled( false );
    editButton.setToolTip( Messages.getString( "editTooltip" ) );
    bar.add( editButton );

    // Add remove button
    scheduleRemoveButton.setCommand( () -> {
      final Set<JsJob> selectedJobs = getSelectedJobs();
      int selectionSize = selectedJobs.size();

      if ( selectionSize > 0 ) {
        final MessageDialogBox prompt = new MessageDialogBox(
          Messages.getString( "warning" ),
          Messages.getString( "deleteConfirmSchedules", "" + selectionSize ),
          false,
          Messages.getString( "yes" ),
          Messages.getString( "no" ) );

        prompt.setCallback( new IDialogCallback() {
          public void okPressed() {
            removeJobs( selectedJobs );
            prompt.hide();
          }

          public void cancelPressed() {
            prompt.hide();
          }
        } );

        prompt.center();
      }
    } );

    scheduleRemoveButton.setToolTip( Messages.getString( "remove" ) );
    scheduleRemoveButton.setEnabled( false );
    bar.add( scheduleRemoveButton );

    bar.add( Toolbar.GLUE );

    return bar;
  }

  private void editJob( final JsJob editJob ) {
    final String jobId = editJob.getJobId();
    final String apiEndpoint = "api/scheduler/jobinfo?jobId=" + URL.encodeQueryString( jobId );

    RequestBuilder executableTypesRequestBuilder =
      createRequestBuilder( RequestBuilder.GET, ScheduleHelper.getPluginContextURL(), apiEndpoint );
    executableTypesRequestBuilder.setHeader( ACCEPT, APPLICATION_JSON );

    try {
      executableTypesRequestBuilder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
          // showError(exception);
        }

        public void onResponseReceived( Request request, Response response ) {
          if ( response.getStatusCode() == Response.SC_OK ) {
            final JsJob jsJob = parseJsonJob( JsonUtils.escapeJsonForEval( response.getText() ) );

            // check email is set up
            final String checkEmailEndpoint = "api/emailconfig/isValid";
            RequestBuilder emailValidRequest =
              createRequestBuilder( RequestBuilder.GET, EnvironmentHelper.getFullyQualifiedURL(), checkEmailEndpoint );

            emailValidRequest.setHeader( "accept", "text/plain" );

            try {
              emailValidRequest.sendRequest( null, new RequestCallback() {

                public void onError( Request request, Throwable exception ) {
                  MessageDialogBox dialogBox =
                    new MessageDialogBox( Messages.getString( "error" ), exception.toString(), false, false, true );

                  dialogBox.center();
                }

                public void onResponseReceived( Request request, Response response ) {
                  if ( response.getStatusCode() == Response.SC_OK ) {
                    final boolean isEmailConfValid = Boolean.parseBoolean( response.getText() );
                    final NewScheduleDialog scheduleDialog = ScheduleFactory.getInstance()
                      .createNewScheduleDialog( jsJob, scheduleDialogCallback, isEmailConfValid );

                    scheduleDialog.center();
                  }
                }
              } );
            } catch ( RequestException e ) {
              // showError(e);
            }

          } else {
            String message = Messages.getString( "serverErrorColon" ) + " " + response.getStatusCode();
            MessageDialogBox dialogBox =
              new MessageDialogBox( Messages.getString( "error" ), message, false, false, true );

            dialogBox.center();
          }
        }
      } );
    } catch ( RequestException e ) {
      // showError(e);
    }
  }

  private void triggerExecuteNow( final Set<JsJob> jobs ) {
    final Map<String, List<JsJob>> candidateJobs = new HashMap<>( jobs.size() );

    for ( JsJob job : jobs ) {
      List<JsJob> jobList = candidateJobs.computeIfAbsent( job.getFullResourceName(), k -> new ArrayList<>() );
      jobList.add( job );
    }

    canAccessJobListRequest( jobs, new RequestCallback() {
      public void onError( Request request, Throwable exception ) {
        promptForScheduleResourceError( jobs );
      }

      public void onResponseReceived( Request request, Response response ) {
        final Set<JsJob> executeList = getExecutableJobs( candidateJobs, response );

        // execute job schedules that can be executed
        if ( !executeList.isEmpty() ) {
          executeJobs( executeList );
        }

        final Set<JsJob> removeList = new HashSet<>();

        for ( JsJob job : jobs ) {
          if ( !executeList.contains( job ) ) {
            removeList.add( job );
          }
        }

        // remove job schedules that no longer can be executed
        if ( !removeList.isEmpty() ) {
          promptForScheduleResourceError( removeList );
        }
      }
    } );
  }

  private void executeJobs( Set<JsJob> jobs ) {
    final String title = Messages.getString( "executeNow" );
    final String message = Messages.getString( "executeNowStarted" + ( jobs.size() > 1 ? "Multiple" : "" ) );

    MessageDialogBox messageDialog = new MessageDialogBox( title, message, false, true, true );
    messageDialog.center();

    controlJobs( jobs, "triggerNow", RequestBuilder.POST, true );
  }

  private Set<JsJob> getExecutableJobs( Map<String, List<JsJob>> candidateJobs, Response response ) {
    final Set<JsJob> executeList = new HashSet<>();

    try {
      final List<String> readableFiles = parseJsonAccessList( response.getText() ).getReadableFiles();

      for ( String resourceName : readableFiles ) {
        executeList.addAll( candidateJobs.get( resourceName ) );
      }
    } catch ( Exception e ) {
      // noop
    }

    return executeList;
  }

  private void promptForScheduleResourceError( final Set<JsJob> jobs ) {
    final String promptContent =
      Messages.getString( "editScheduleResourceDoesNotExist" + ( jobs.size() > 1 ? "Multiple" : "" ) );
    final MessageDialogBox prompt = new MessageDialogBox(
      Messages.getString( "fileUnavailable" ),
      promptContent,
      true,
      Messages.getString( "yesDelete" ),
      Messages.getString( "no" ) );

    prompt.setCallback( new IDialogCallback() {
      public void okPressed() {
        controlJobs( jobs, "removeJob", RequestBuilder.DELETE, true );
        prompt.hide();
      }

      public void cancelPressed() {
        prompt.hide();
      }
    } );

    prompt.setWidth( "530px" );
    prompt.center();
  }

  private void controlJobs( final Set<JsJob> jobs, String function, final Method method, final boolean refreshData ) {
    for ( final JsJob job : jobs ) {
      RequestBuilder builder =
        createRequestBuilder( method, ScheduleHelper.getPluginContextURL(), "api/scheduler/" + function );

      builder.setHeader( CONTENT_TYPE, APPLICATION_JSON );

      JSONObject startJobRequest = new JSONObject();
      startJobRequest.put( "jobId", new JSONString( job.getJobId() ) );

      try {
        builder.sendRequest( startJobRequest.toString(), new RequestCallback() {

          public void onError( Request request, Throwable exception ) {
            // showError(exception);
          }

          public void onResponseReceived( Request request, Response response ) {
            final String jobState = response.getText();

            job.setState( jobState );
            table.redraw();

            updateJobScheduleButtonStyle( jobState );

            if ( refreshData ) {
              refresh();
            }
          }
        } );
      } catch ( RequestException e ) {
        // showError(e);
      }
    }
  }

  private JSONArray getIds( final Set<JsJob> jobs ) {
    JSONArray result = new JSONArray();

    for ( JsJob job : jobs ) {
      result.set( result.size(), new JSONString( job.getJobId() ) );
    }

    return result;
  }

  @SuppressWarnings( { "java:S112" } )
  private void removeJobs( final Set<JsJob> jobs ) {
    RequestBuilder builder =
      createRequestBuilder( RequestBuilder.POST, ScheduleHelper.getPluginContextURL(), "api/scheduler/removeJobs" );
    builder.setHeader( CONTENT_TYPE, APPLICATION_JSON );
    builder.setHeader( ACCEPT, APPLICATION_JSON );

    JSONObject requestData = new JSONObject();
    requestData.put( "jobIds", getIds( jobs ) );

    try {
      builder.sendRequest( requestData.toString(), new RequestCallback() {
        public void onError( Request request, Throwable exception ) {
          showHTMLMessage( Messages.getString( "error" ), exception.toString() );
        }

        public void onResponseReceived( Request request, Response response ) {
          try {
            if ( response.getStatusCode() == Response.SC_OK ) {
              JSONObject responseObj = new JSONObject( JsonUtils.safeEval( response.getText() ) );
              Map<String, String> changes = SchedulerUiUtil.getMapFromJSONResponse( responseObj, "changes" );

              if ( !changes.isEmpty() ) {
                List<JsJob> errorJobs = new ArrayList<>();

                jobs.forEach( job -> {
                  String jobId = job.getJobId();
                  String newState = changes.get( jobId );

                  if ( JOB_STATE_REMOVED.equals( newState ) ) {
                    job.setState( newState );
                    updateJobScheduleButtonStyle( newState );
                  } else {
                    errorJobs.add( job );
                  }
                } );

                if ( errorJobs.isEmpty() ) {
                  showHTMLMessage( Messages.getString( "success" ), Messages.getString( "bulkDeleteSuccess" ) );
                } else {
                  throw new RuntimeException( Messages.getString( "bulkDeleteErrors",
                    errorJobs.stream().map( JsJob::getJobName ).collect( Collectors.joining( "<br/>" ) ) ) );
                }
              } else {
                throw new RuntimeException( Messages.getString( "bulkDeleteResponseError" ) );
              }
            } else {
              throw new RuntimeException( Messages.getString( "serverErrorColon" ) + " " + response.getStatusCode() );
            }
          } catch ( Exception e ) {
            showHTMLMessage( Messages.getString( "error" ), e.toString() );
          } finally {
            table.redraw();
            refresh();
          }
        }
      } );
    } catch ( RequestException e ) {
      showHTMLMessage( Messages.getString( "error" ), e.toString() );
    }
  }

  private void showHTMLMessage( String title, String body ) {
    MessageDialogBox dialogBox = new MessageDialogBox( title, body, true, false, true );
    dialogBox.center();
  }

  private void controlScheduler( final ToolbarButton controlSchedulerButton, final String function,
                                 final boolean isScheduler ) {
    final RequestBuilder builder =
      createRequestBuilder( RequestBuilder.POST, ScheduleHelper.getPluginContextURL(), "api/scheduler/" + function );

    try {
      builder.sendRequest( null, new RequestCallback() {
        public void onError( Request request, Throwable exception ) {
          // showError(exception);
        }

        public void onResponseReceived( Request request, Response response ) {
          updateControlSchedulerButtonStyle( controlSchedulerButton, response.getText() );

          controlSchedulerButton.setEnabled( isScheduler );
        }
      } );
    } catch ( RequestException e ) {
      // showError(e);
    }
  }

  private void openOutputLocation( final String outputLocation ) {
    setBrowserPerspective();
    String url = EnvironmentHelper.getFullyQualifiedURL() + "api/mantle/session-variable?key=scheduler_folder&value="
      + outputLocation;
    RequestBuilder executableTypesRequestBuilder = new CsrfRequestBuilder( RequestBuilder.POST, url );

    try {
      executableTypesRequestBuilder.sendRequest( null, new RequestCallback() {
        @Override
        public void onResponseReceived( Request request, Response response ) {
          // fire and forget
        }

        @Override
        public void onError( Request request, Throwable exception ) {
          // fire and forget
        }
      } );
    } catch ( RequestException e ) {
      // IGNORE
    }

    fireRefreshFolderEvent( outputLocation );
  }

  private void showValidateOutputLocationError() {
    String title = Messages.getString( "outputLocationErrorTitle" );
    String message = Messages.getString( "outputLocationErrorMessage" );
    String okText = Messages.getString( "close" );

    MessageDialogBox dialogBox = new MessageDialogBox( title, message, false, false, true, okText, null, null );

    dialogBox.addStyleName( "pentaho-dialog-small" );
    dialogBox.center();
  }

  @SuppressWarnings( "nls" )
  public static String pathToId( String path ) {
    String id = NameUtils.encodeRepositoryPath( path );
    return NameUtils.URLEncode( id );
  }

  private void canAccessJobRequest( final JsJob job, RequestCallback callback ) {
    final String jobId = pathToId( job.getFullResourceName() );

    final String apiEndpoint =
      "api/repo/files/" + jobId + "/canAccess?cb=" + System.currentTimeMillis() + "&permissions=" + READ_PERMISSION;

    final RequestBuilder accessBuilder =
      createRequestBuilder( RequestBuilder.GET, EnvironmentHelper.getFullyQualifiedURL(), apiEndpoint );

    try {
      accessBuilder.sendRequest( null, callback );
    } catch ( RequestException re ) {
      // noop
    }
  }

  private void canAccessJobListRequest( final Set<JsJob> jobs, RequestCallback callback ) {
    final JSONArray jobNameList = new JSONArray();

    int idx = 0;

    for ( JsJob job : jobs ) {
      jobNameList.set( idx++, new JSONString( job.getFullResourceName() ) );
    }

    final JSONObject payload = new JSONObject();
    payload.put( "strings", jobNameList );

    final String accessListEndpoint = "api/repo/files/pathsAccessList?cb=" + System.currentTimeMillis();
    RequestBuilder accessListBuilder =
      createRequestBuilder( RequestBuilder.POST, EnvironmentHelper.getFullyQualifiedURL(), accessListEndpoint );

    accessListBuilder.setHeader( CONTENT_TYPE, APPLICATION_JSON );
    accessListBuilder.setHeader( ACCEPT, APPLICATION_JSON );

    try {
      accessListBuilder.sendRequest( payload.toString(), callback );
    } catch ( RequestException re ) {
      // noop
    }
  }

  private RequestBuilder createRequestBuilder( Method method, String context, String apiEndpoint ) {
    final String url = context + apiEndpoint;

    RequestBuilder builder = new RequestBuilder( method, url );
    builder.setHeader( IF_MODIFIED_SINCE, IF_MODIFIED_SINCE_DATE );

    return builder;
  }

  private native JsArray<JsJob> parseJson( String json ) /*-{
    var obj = JSON.parse(json);

    if (obj != null && obj.hasOwnProperty("job")) {
      return obj.job;
    }

    return [];
  }-*/;

  private native JsJob parseJsonJob( String json ) /*-{
    return JSON.parse(json);
  }-*/;

  private native JsPermissionsList parseJsonAccessList( String json ) /*-{
    return JSON.parse(json);
  }-*/;

  public native void setBrowserPerspective() /*-{
    $wnd.mantle.setBrowserPerspective();
  }-*/;

  public native void fireRefreshFolderEvent( String outputLocation ) /*-{
    $wnd.mantle.fireRefreshFolderEvent(outputLocation);
  }-*/;
}
