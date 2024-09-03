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

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;
import org.pentaho.gwt.widgets.client.controls.DatePickerEx;
import org.pentaho.gwt.widgets.client.controls.ErrorLabel;
import org.pentaho.gwt.widgets.client.controls.TimePicker;
import org.pentaho.gwt.widgets.client.dialogs.MessageDialogBox;
import org.pentaho.gwt.widgets.client.panel.VerticalFlexPanel;
import org.pentaho.gwt.widgets.client.ui.ICallback;
import org.pentaho.gwt.widgets.client.ui.IChangeHandler;
import org.pentaho.gwt.widgets.client.utils.TimeUtil;
import org.pentaho.mantle.client.messages.Messages;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;

/**
 * @author Steven Barkdull
 */

public class RunOnceEditor extends VerticalPanel implements IChangeHandler {

  private static final String SCHEDULER_CAPTION_PANEL = "schedule-editor-caption-panel"; //$NON-NLS-1$
  private static final String SCHEDULE_START_DATE_INPUT = "schedule-start-date-input";

  protected TimePicker startTimePicker = null;
  protected ListBox timeZonePicker = null;
  private DefaultFormat format = new DefaultFormat( DateTimeFormat.getFormat( PredefinedFormat.DATE_SHORT ) );
  protected DatePickerEx startDatePicker = new DatePickerEx( format );
  private ICallback<IChangeHandler> onChangeHandler = null;

  private ErrorLabel startLabel = null;
  private ErrorLabel detailLabel = null;

  private AdditionalDetailsPanel detailsPanel;

  private final MessageDialogBox errorBox =
    new MessageDialogBox( Messages.getString( "error" ), "", false, false, true );

  public RunOnceEditor( final TimePicker startTimePicker, final ListBox timeZonePicker ) {
    setWidth( "100%" ); //$NON-NLS-1$

    VerticalFlexPanel outerVP = new VerticalFlexPanel();
    add( outerVP );
    detailsPanel = new AdditionalDetailsPanel();

    CaptionPanel startDateCaptionPanel = new CaptionPanel( Messages.getString( "schedule.startDate" ) );
    startDateCaptionPanel.setStyleName( SCHEDULER_CAPTION_PANEL );
    startDateCaptionPanel.getElement().setId( SCHEDULE_START_DATE_INPUT );
    startDateCaptionPanel.add( startDatePicker.getDatePicker() );
    startLabel = new ErrorLabel( startDateCaptionPanel );
    outerVP.add( startLabel );

    detailLabel = new ErrorLabel( detailsPanel );
    outerVP.add( detailLabel );

    this.startTimePicker = startTimePicker;
    this.timeZonePicker = timeZonePicker;
    configureOnChangeHandler();
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

  public Date getStartDate() {
    return startDatePicker.getSelectedDate();
  }

  public void setStartDate( Date d ) {
    startDatePicker.getDatePicker().setValue( d );
  }

  public String getStartTime() {
    return startTimePicker.getTime();
  }

  public void setStartHour( int hour ) {
    startTimePicker.setHour( hour );
  }

  public void setStartMinute( int min ) {
    startTimePicker.setMinute( min );
  }

  public void setStartTimeOfDay( int amPm ) {
    startTimePicker.setTimeOfDay( TimeUtil.TimeOfDay.get( amPm ) );
  }

  public String getStartTimeOfDay() {
    return startTimePicker.getTimeOfDay().toString();
  }

  public String getStartHour() {
    return startTimePicker.getHour();
  }

  public String getStartMinute() {
    return startTimePicker.getMinute();
  }

  public void setStartTime( String strTime ) {
    startTimePicker.setTime( strTime );
  }

  @SuppressWarnings( "deprecation" )
  public void reset( Date d ) {
    startTimePicker.setTimeOfDay( TimeUtil.getTimeOfDayBy0To23Hour( d.getHours() ) );
    startTimePicker.setHour( TimeUtil.to12HourClock( d.getHours() ) );
    startTimePicker.setMinute( d.getMinutes() );
    startDatePicker.getDatePicker().setValue( d );
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
    final RunOnceEditor localThis = this;

    ICallback<IChangeHandler> handler = new ICallback<IChangeHandler>() {
      public void onHandle( IChangeHandler o ) {
        localThis.changeHandler();
      }
    };
    startTimePicker.setOnChangeHandler( handler );
    startDatePicker.setOnChangeHandler( handler );
    timeZonePicker.addChangeHandler( event -> localThis.changeHandler() );
  }
}
