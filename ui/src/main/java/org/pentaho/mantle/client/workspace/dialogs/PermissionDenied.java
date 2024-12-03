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
package org.pentaho.mantle.client.workspace.dialogs;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import org.pentaho.gwt.widgets.client.dialogs.PromptDialogBox;
import org.pentaho.gwt.widgets.client.panel.VerticalFlexPanel;
import org.pentaho.gwt.widgets.client.text.TextCopyToClipboard;
import org.pentaho.mantle.client.messages.Messages;
import org.pentaho.mantle.client.workspace.JsJob;

import java.util.Set;
import java.util.stream.Collectors;

public class PermissionDenied extends PromptDialogBox {
  public static final String STYLE_NAME = "permission-denied-dialog";

  private static final String TITLE = Messages.getString(  "dialog.permissionDenied.title" );
  private static final String OK = Messages.getString(  "ok" );

  private final Set<JsJob> noPermissionJobs;

  public PermissionDenied( Set<JsJob> noPermissionJobs ) {
    super( TITLE, OK, null, false, true );

    this.noPermissionJobs = noPermissionJobs;

    createUI();

    setResponsive( true );
    setSizingMode( DialogSizingMode.FILL_VIEWPORT_WIDTH );
    setWidthCategory( DialogWidthCategory.SMALL );

    addStyleName( STYLE_NAME );
  }

  private void createUI() {
    VerticalFlexPanel content = new VerticalFlexPanel();
    content.getElement().getStyle().setPropertyPx( "gap", 16 );

    Label permissionDeniedDescription = new Label( Messages.getString( "dialog.lackPermissionDescription" ) );
    permissionDeniedDescription.setStyleName( "typography typography-body dialog-description" );
    content.add( permissionDeniedDescription );

    HTML description = new HTML( Messages.getString( "dialog.resolveLackPermission" ) );
    description.setStyleName( "typography typography-body dialog-description" );
    content.add( description );

    String noPermissionsValue = this.noPermissionJobs
      .stream()
      .map( JsJob::getInputFilePath )
      .collect( Collectors.joining( "\n" ) );
    content.add( new TextCopyToClipboard( noPermissionsValue ) );

    setContent( content );
    setWidth( "530px" );
  }
}
