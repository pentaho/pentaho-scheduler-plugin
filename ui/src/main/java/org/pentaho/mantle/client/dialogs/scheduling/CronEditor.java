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

import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.pentaho.gwt.widgets.client.controls.DateRangeEditor;
import org.pentaho.gwt.widgets.client.controls.ErrorLabel;
import org.pentaho.gwt.widgets.client.ui.ICallback;
import org.pentaho.gwt.widgets.client.ui.IChangeHandler;
import org.pentaho.gwt.widgets.client.utils.TimeUtil;
import org.pentaho.mantle.client.messages.Messages;

import java.util.Date;

/**
 * @author Steven Barkdull
 *
 */
@SuppressWarnings( "deprecation" )
public class CronEditor extends VerticalPanel implements IChangeHandler {
  private static final String CRON_LABEL = "cron-label"; //$NON-NLS-1$

  protected TextBox cronTb = new TextBox();
  protected DateRangeEditor dateRangeEditor = null;
  protected ErrorLabel cronLabel = null;
  private ICallback<IChangeHandler> onChangeHandler;

  private ErrorLabel detailLabel = null;

  private AdditionalDetailsPanel detailsPanel;

  public CronEditor() {
    super();
    setWidth( "100%" ); //$NON-NLS-1$

    Label l = new Label( Messages.getString( "schedule.cronLabel" ) );
    l.setStylePrimaryName( CRON_LABEL );
    cronLabel = new ErrorLabel( l );
    cronTb.getElement().setId( "cron-tb" );

    add( cronLabel );
    add( cronTb );

    dateRangeEditor = new DateRangeEditor( new Date() );
    add( dateRangeEditor );
    detailsPanel = new AdditionalDetailsPanel();

    /* BISERVER-15179
    add( detailsPanel );
    */

    configureOnChangeHandler();
  }

  public void reset( Date d ) {
    cronTb.setText( "" ); //$NON-NLS-1$
    dateRangeEditor.reset( d );
  }

  public String getCronString() {
    return cronTb.getText();
  }

  public void setCronString( String cronStr ) {
    this.cronTb.setText( cronStr );
  }

  public Date getStartDate() {
    return dateRangeEditor.getStartDate();
  }

  public void setStartDate( Date d ) {
    dateRangeEditor.setStartDate( d );
  }

  public Date getEndDate() {
    return dateRangeEditor.getEndDate();
  }

  public void setEndDate( Date d ) {
    dateRangeEditor.setEndDate( d );
  }

  public void setNoEndDate() {
    dateRangeEditor.setNoEndDate();
  }

  public boolean isEndBy() {
    return dateRangeEditor.isEndBy();
  }

  public void setEndBy() {
    dateRangeEditor.setEndBy();
  }

  public boolean isNoEndDate() {
    return dateRangeEditor.isNoEndDate();
  }

  public String getStartTime() {
    // No time picker, assume midnight and use 00 to ensure correct translation to java Date notation
    return "00:00:00";
  }

  public boolean getEnableSafeMode() {
    return detailsPanel.getEnableSafeMode();
  }

  public void setEnableSafeMode( boolean enableSafeMode ) {
    detailsPanel.setEnableSafeMode( enableSafeMode );
  }

  public boolean getGatherMetrics() {
    return detailsPanel.getGatherMetrics();
  }

  public void setGatherMetrics( boolean gatherMetrics ) {
    detailsPanel.setGatherMetrics( gatherMetrics );
  }

  public String getLogLevel() {
    return detailsPanel.getLogLevel();
  }

  public void setLogLevel( String logLevel ) {
    detailsPanel.setLogLevel( logLevel );
  }


  /**
   * NOTE: should only ever be used by validators. This is a backdoor into this class that shouldn't be here, do
   * not use this method unless you are validating.
   *
   * @return DateRangeEditor
   */
  public DateRangeEditor getDateRangeEditor() {
    return dateRangeEditor;
  }

  public void setCronError( String errorMsg ) {
    cronLabel.setErrorMsg( errorMsg );
  }

  public void setOnChangeHandler( ICallback<IChangeHandler> handler ) {
    this.onChangeHandler = handler;
  }

  private void changeHandler() {
    if ( null != onChangeHandler ) {
      onChangeHandler.onHandle( this );
    }
  }

  private void configureOnChangeHandler() {
    final CronEditor localThis = this;
    KeyboardListener keyboardListener = new KeyboardListener() {
      public void onKeyDown( Widget sender, char keyCode, int modifiers ) {
      }

      public void onKeyPress( Widget sender, char keyCode, int modifiers ) {
      }

      public void onKeyUp( Widget sender, char keyCode, int modifiers ) {
        localThis.changeHandler();
      }
    };
    ICallback<IChangeHandler> handler = new ICallback<IChangeHandler>() {
      public void onHandle( IChangeHandler o ) {
        localThis.changeHandler();
      }
    };
    cronTb.addKeyboardListener( keyboardListener );
    dateRangeEditor.setOnChangeHandler( handler );
  }
}
