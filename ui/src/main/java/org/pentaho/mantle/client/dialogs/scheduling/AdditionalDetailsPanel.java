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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import org.pentaho.gwt.widgets.client.controls.ErrorLabel;
import org.pentaho.gwt.widgets.client.panel.HorizontalFlexPanel;
import org.pentaho.gwt.widgets.client.panel.VerticalFlexPanel;
import org.pentaho.gwt.widgets.client.ui.ICallback;
import org.pentaho.gwt.widgets.client.ui.IChangeHandler;
import org.pentaho.gwt.widgets.client.utils.TimeUtil;
import org.pentaho.mantle.client.messages.Messages;

public class AdditionalDetailsPanel extends Composite  implements IChangeHandler {

  protected ListBox logLevel = createLogLevelListBox();
  private ICallback<IChangeHandler> onChangeHandler = null;
  protected ErrorLabel detailLabel = null;
  protected CheckBox enableSafeMode;
  protected CheckBox gatherMetrics;

  private static final String SCHEDULER_ADDITIONAL_PANEL = "schedule-editor-additional-panel";
  private static final String SCHEDULE_DETAILS_INPUT = "schedule-details-input";

  public AdditionalDetailsPanel() {
    CaptionPanel detailPanel = new CaptionPanel( Messages.getString( "schedule.detail" ) );
    detailPanel.setStyleName( SCHEDULER_ADDITIONAL_PANEL );
    detailPanel.getElement().setId( SCHEDULE_DETAILS_INPUT );

    VerticalFlexPanel additionalDetailVP = new VerticalFlexPanel();
    enableSafeMode = new CheckBox();
    enableSafeMode.setText( Messages.getString( "schedule.enableSafeMode" ) );
    enableSafeMode.setStyleName( "enableSafeModeCheckbox" );
    enableSafeMode.addClickHandler(new ClickHandler() {
      @Override
      public void onClick( ClickEvent event ) {
        boolean checked = ( (CheckBox) event.getSource() ).getValue().booleanValue();
        setEnableSafeMode( checked );
      }
    } );
    additionalDetailVP.add( enableSafeMode );

    gatherMetrics = new CheckBox();
    gatherMetrics.setText( Messages.getString( "schedule.performanceMetrics" ) );
    gatherMetrics.setStyleName( "gatherPerfmnceMetricsCheckbox" );
    gatherMetrics.setValue( true );
    gatherMetrics.addClickHandler(new ClickHandler() {
      @Override
      public void onClick( ClickEvent event ) {
        boolean checked = ( (CheckBox) event.getSource() ).getValue().booleanValue();
        setGatherMetrics( checked );
      }
    } );
    additionalDetailVP.add( gatherMetrics );

    HorizontalFlexPanel logLevelHP = new HorizontalFlexPanel();
    logLevel.getElement().setId( "log-level-lb" );
    logLevelHP.add( logLevel );
    Label logLabel = new Label( Messages.getString( "schedule.logLevel" ) );
    logLabel.setStyleName( "endLabel" );
    logLevelHP.add( logLabel );
    additionalDetailVP.add( logLevelHP );
    detailPanel.add( additionalDetailVP );

    detailLabel = new ErrorLabel( detailPanel );
    VerticalFlexPanel detailVP = new VerticalFlexPanel();
    detailVP.add( detailLabel );
    initWidget( detailVP );
  }

  private ListBox createLogLevelListBox() {
    ListBox l = new ListBox();
    for ( int i = 0; i < TimeUtil.LogLevel.length(); ++i ) {
      TimeUtil.LogLevel log = TimeUtil.LogLevel.get( i );
      l.addItem( Messages.getString( log.toString() ) );
    }
    return l;
  }

  @Override
  public void setOnChangeHandler( ICallback<IChangeHandler> handler ) {
    this.onChangeHandler = handler;
  }

  public boolean getEnableSafeMode() {
    return enableSafeMode.getValue();
  }

  public void setEnableSafeMode( boolean enableSafeMode ) {
    this.enableSafeMode.setValue( enableSafeMode );
  }

  public boolean getGatherMetrics() {
    return gatherMetrics.getValue();
  }

  public void setGatherMetrics( boolean gatherMetrics ) {
    this.gatherMetrics.setValue( gatherMetrics );
  }

  public String getLogLevel() {
    return logLevel.getSelectedItemText();
  }

  public void setLogLevel( String logLevel ) {
    for ( int i = 0; i < TimeUtil.LogLevel.length(); ++i ) {
      TimeUtil.LogLevel log = TimeUtil.LogLevel.get( i );
      if ( log.toString().equals( logLevel ) ) {
        this.logLevel.setSelectedIndex( i );
      }
    }
  }
}
