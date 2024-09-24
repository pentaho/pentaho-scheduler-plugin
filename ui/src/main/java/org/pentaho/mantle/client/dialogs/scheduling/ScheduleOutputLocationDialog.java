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
 * Copyright (c) 2002-2023 Hitachi Vantara. All rights reserved.
 */

package org.pentaho.mantle.client.dialogs.scheduling;

import java.util.Date;

import com.google.gwt.core.client.JsArray;
import org.pentaho.gwt.widgets.client.dialogs.IDialogCallback;
import org.pentaho.gwt.widgets.client.dialogs.MessageDialogBox;
import org.pentaho.gwt.widgets.client.dialogs.PromptDialogBox;
import org.pentaho.gwt.widgets.client.formatter.JSDateTextFormatter;
import org.pentaho.gwt.widgets.client.panel.HorizontalFlexPanel;
import org.pentaho.gwt.widgets.client.panel.VerticalFlexPanel;
import org.pentaho.gwt.widgets.client.utils.NameUtils;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;
import org.pentaho.gwt.widgets.client.wizards.AbstractWizardDialog;
import org.pentaho.mantle.client.dialogs.folderchooser.SelectFolderDialog;
import org.pentaho.mantle.client.messages.Messages;

import com.google.gwt.dom.client.Style.VerticalAlign;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

public abstract class ScheduleOutputLocationDialog extends PromptDialogBox {
  private String filePath;

  private final TextBox scheduleNameTextBox = new TextBox();
  private final Label scheduleNameLabel = new Label();
  private final static TextBox scheduleLocationTextBox = new TextBox();
  private final CheckBox appendTimeChk = new CheckBox();
  private final ListBox timestampLB = new ListBox();
  private final CaptionPanel previewCaptionPanel = new CaptionPanel( Messages.getString( "preview" ) );
  private final Label scheduleNamePreviewLabel = new Label();
  private final CheckBox overrideExistingChk = new CheckBox();

  private static HandlerRegistration changeHandlerReg = null;
  private static HandlerRegistration keyHandlerReg = null;

  static {
    setScheduleLocation( getDefaultSaveLocation() );
  }

  private static native String getDefaultSaveLocation()
  /*-{
      return window.top.HOME_FOLDER;
  }-*/;

  private static native void delete( JsArray<?> array, int index, int count )
  /*-{
      array.splice(index, count);
  }-*/;

  public ScheduleOutputLocationDialog( final String filePath ) {
    super(
      Messages.getString( "runInBackground" ), Messages.getString( "next" ), Messages.getString( "cancel" ), false, true ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    this.filePath = filePath;
    createUI();
    setupCallbacks();
    setResponsive( true );
    setSizingMode( DialogSizingMode.FILL_VIEWPORT );
    setWidthCategory( DialogWidthCategory.SMALL );
  }

  private void createUI() {
    addStyleName( "schedule-output-location-dialog" );
    VerticalFlexPanel content = new VerticalFlexPanel();

    HorizontalFlexPanel scheduleNameLabelPanel = new HorizontalFlexPanel();

    scheduleNameLabel.setText( Messages.getString( "generatedContentName" ) );
    scheduleNameLabel.setHorizontalAlignment( HasHorizontalAlignment.ALIGN_LEFT );
    scheduleNameLabel.setStyleName( ScheduleEditor.SCHEDULE_LABEL );
    scheduleNameLabel.addStyleName( "schedule-name" );
    scheduleNameLabelPanel.add( scheduleNameLabel );

    String defaultName = filePath.substring( filePath.lastIndexOf( "/" ) + 1, filePath.lastIndexOf( "." ) );
    scheduleNameTextBox.getElement().setId( "schedule-name-input" );
    scheduleNameTextBox.setText( defaultName );

    content.add( scheduleNameLabelPanel );

    timestampLB.addStyleName( "schedule-timestamp-listbox" );

    timestampLB.addItem( "yyyy-MM-dd" );
    timestampLB.addItem( "yyyyMMdd" );
    timestampLB.addItem( "yyyyMMddHHmmss" );
    timestampLB.addItem( "MM-dd-yyyy" );
    timestampLB.addItem( "MM-dd-yy" );
    timestampLB.addItem( "dd-MM-yyyy" );

    timestampLB.addClickHandler( event -> {
      int index = ( (ListBox) event.getSource() ).getSelectedIndex();
      scheduleNamePreviewLabel.setText( getPreviewName( index ) );
    } );

    timestampLB.setVisible( false );

    HorizontalFlexPanel scheduleNamePanel = new HorizontalFlexPanel();
    scheduleNamePanel.addStyleName( "schedule-name-panel" );
    scheduleNamePanel.add( scheduleNameTextBox );
    scheduleNamePanel.setCellVerticalAlignment( scheduleNameTextBox, HasVerticalAlignment.ALIGN_MIDDLE );
    scheduleNamePanel.add( timestampLB );

    content.add( scheduleNamePanel );

    appendTimeChk.setText( Messages.getString( "appendTimeToName" ) );
    appendTimeChk.addClickHandler( event -> {
      boolean checked = ( (CheckBox) event.getSource() ).getValue();
      refreshAppendedTimestamp( checked );
    } );
    content.add( appendTimeChk );

    previewCaptionPanel.setStyleName( "schedule-caption-panel" );
    previewCaptionPanel.setVisible( false );
    content.add( previewCaptionPanel );

    scheduleNamePreviewLabel.setText( getPreviewName( timestampLB.getSelectedIndex() ) );
    scheduleNamePreviewLabel.addStyleName( "schedule-name-preview" );
    previewCaptionPanel.add( scheduleNamePreviewLabel );

    Label scheduleLocationLabel = new Label( Messages.getString( "generatedContentLocation" ) );
    scheduleLocationLabel.setStyleName( ScheduleEditor.SCHEDULE_LABEL );
    content.add( scheduleLocationLabel );

    Button browseButton = new Button( Messages.getString( "select" ) );
    browseButton.addClickHandler( event -> {
      String selectedPath = scheduleLocationTextBox.getText();

      final SelectFolderDialog selectFolder = new SelectFolderDialog( selectedPath );
      selectFolder.setCallback( new IDialogCallback() {
        public void okPressed() {
          setScheduleLocation( selectFolder.getSelectedPath() );
        }

        public void cancelPressed() { /* noop */ }
      } );

      selectFolder.center();
    } );

    browseButton.setStyleName( AbstractWizardDialog.PENTAHO_BUTTON );
    browseButton.addStyleName( ScheduleEditor.SCHEDULE_BUTTON );

    ChangeHandler changeHandler = event -> {
      scheduleNamePreviewLabel.setText( getPreviewName( timestampLB.getSelectedIndex() ) );
      updateButtonState();
    };
    KeyUpHandler keyUpHandler = event -> {
      scheduleNamePreviewLabel.setText( getPreviewName( timestampLB.getSelectedIndex() ) );
      updateButtonState();
    };

    if ( keyHandlerReg != null ) {
      keyHandlerReg.removeHandler();
    }
    if ( changeHandlerReg != null ) {
      changeHandlerReg.removeHandler();
    }
    keyHandlerReg = scheduleNameTextBox.addKeyUpHandler( keyUpHandler );
    changeHandlerReg = scheduleNameTextBox.addChangeHandler( changeHandler );


    HorizontalFlexPanel locationPanel = new HorizontalFlexPanel();
    locationPanel.addStyleName( "schedule-dialog-location-panel" );
    locationPanel.setCellVerticalAlignment( scheduleLocationTextBox, HasVerticalAlignment.ALIGN_MIDDLE );

    scheduleLocationTextBox.addStyleName( ScheduleEditor.SCHEDULE_INPUT );
    scheduleLocationTextBox.addChangeHandler( changeHandler );
    scheduleLocationTextBox.setEnabled( false );
    locationPanel.add( scheduleLocationTextBox );

    locationPanel.add( browseButton );

    content.add( locationPanel );

    content.add( overrideExistingChk );

    refreshAppendedTimestamp( appendTimeChk.getValue() );

    setContent( content );
    content.getElement().getStyle().clearHeight();
    content.getElement().getParentElement().getStyle().setVerticalAlign( VerticalAlign.TOP );
    content.getParent().setHeight( "100%" );

    updateButtonState();
    setSize( "650px", "450px" );

    validateScheduleLocationTextBox();
  }

  private void setupCallbacks() {
    setValidatorCallback( () -> {
      String name;
      if ( appendTimeChk.getValue() ) {
        name = getPreviewName( timestampLB.getSelectedIndex() );
      } else {
        //trim the name if there is no timestamp appended
        scheduleNameTextBox.setText( scheduleNameTextBox.getText().trim() );

        name = scheduleNameTextBox.getText();
      }

      boolean isValid = NameUtils.isValidFileName( name );
      if ( !isValid ) {
        String message = Messages.getString(
          "prohibitedNameSymbols", name, NameUtils.reservedCharListForDisplay( " " ) );

        MessageDialogBox errorDialog = new MessageDialogBox( Messages.getString( "error" ),
          message, false, false, true );
        errorDialog.center();
      }

      return isValid;
    });

    setCallback( new IDialogCallback() {
      @Override
      public void okPressed() {
        boolean overwriteFile = false;
        if ( overrideExistingChk != null ) {
          overwriteFile = overrideExistingChk.getValue();
        }

        String dateFormat = "";
        if ( appendTimeChk != null ) {
          if ( appendTimeChk.getValue() ) {
            dateFormat = timestampLB.getValue( timestampLB.getSelectedIndex() );
          }
        }

        onSelect( scheduleNameTextBox.getText(), scheduleLocationTextBox.getText(), overwriteFile, dateFormat );
      }

      @Override
      public void cancelPressed() {
      }
    } );
  }

  private void updateButtonState() {
    boolean hasLocation = !StringUtils.isEmpty( scheduleLocationTextBox.getText() );
    boolean hasName = !StringUtils.isEmpty( scheduleNameTextBox.getText() );

    okButton.setEnabled( hasLocation && hasName );
  }

  public String getPreviewName( int index ) {
    JSDateTextFormatter formatter = new JSDateTextFormatter( timestampLB.getValue( index ) );
    Date date = new Date();
    return scheduleNameTextBox.getText() + formatter.format( String.valueOf( date.getTime() ) );
  }

  private void validateScheduleLocationTextBox() {
    final Command errorCallback = new Command() {
      @Override
      public void execute() {
        String previousPath = OutputLocationUtils.getPreviousLocationPath( scheduleLocationTextBox.getText() );
        if ( previousPath != null && !previousPath.isEmpty() ) {
          setScheduleLocation( previousPath );
          validateScheduleLocationTextBox();
        } else {
          setScheduleLocation( getDefaultSaveLocation() ); // restore default location
        }
      }
    };
    OutputLocationUtils.validateOutputLocation( scheduleLocationTextBox.getText(), null, errorCallback );
  }

  private static void setScheduleLocation( String location ) {
    scheduleLocationTextBox.setText( location );
    scheduleLocationTextBox.setTitle( location );
  }

  /**
   * Refresh Appended Timestamp
   *
   * Refresh the New Schedule UI to update multiple components that change based on whether the timestamp is appended
   * to the schedule name.
   *
   * @param value - true if the timestamp should be appended, otherwise false
   */
  private void refreshAppendedTimestamp( boolean value ) {
    previewCaptionPanel.setVisible( value );
    timestampLB.setVisible( value );
    if ( value ) {
      overrideExistingChk.setText( Messages.getString( "overrideExistingFileAndTime" ) ); //$NON-NLS-1$

      //Update the preview text
      scheduleNamePreviewLabel.setText( getPreviewName( timestampLB.getSelectedIndex() ) );
    } else {
      overrideExistingChk.setText( Messages.getString( "overrideExistingFile" ) ); //$NON-NLS-1$
    }
  }

  protected abstract void onSelect( String name, String outputLocationPath, boolean overwriteFile, String dateFormat );

  public void setOkButtonText( String text ) {
    okButton.setText( text );
  }

  public void setScheduleNameText( String text ) {
    scheduleNameLabel.setText( text );
  }

}
