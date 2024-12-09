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

package org.pentaho.mantle.client.dialogs.scheduling;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.pentaho.gwt.widgets.client.dialogs.PromptDialogBox;
import org.pentaho.mantle.client.messages.Messages;
import org.pentaho.mantle.client.workspace.JsJob;
import org.pentaho.mantle.client.workspace.JsJobParam;
import org.pentaho.mantle.client.workspace.SchedulerUiUtil;

public class ParameterPreviewSidebar extends PromptDialogBox {
  private static final int SIDEBAR_WIDTH = 400;
  private static final int FOOTER_HEIGHT = 175;
  private static final int PADDING = 20;
  private final JsJob job;
  private VerticalPanel paramsPanel;

  public ParameterPreviewSidebar( JsJob job ) {
    super( Messages.getString( "paramsDialogTitle" ), Messages.getString( "cancel" ), null, null, false, true );
    this.job = job;
  }

  protected void layout() {
    setSizingMode( DialogSizingMode.FILL_VIEWPORT );
    VerticalPanel wrapperPanel = new VerticalPanel();
    paramsPanel = new VerticalPanel();

    wrapperPanel.add( paramsPanel );
    setContent( wrapperPanel );

    wrapperPanel.getElement().getStyle().setWidth( SIDEBAR_WIDTH - ( 2 * PADDING ), Style.Unit.PX );
    paramsPanel.getElement().getStyle().setWidth( 100, Style.Unit.PCT );
    paramsPanel.getElement().getStyle().setProperty( "height", "calc(100vh - " + FOOTER_HEIGHT + "px)" );
    paramsPanel.getElement().getStyle().setDisplay( Style.Display.BLOCK );
    paramsPanel.getElement().getStyle().setOverflow( Style.Overflow.AUTO );

    getElement().getStyle().setHeight( 100, Style.Unit.PCT );
    getElement().getStyle().setWidth( SIDEBAR_WIDTH, Style.Unit.PX );
    getElement().getStyle().clearLeft();
    getElement().getStyle().setTop( 0, Style.Unit.PX );
    getElement().getStyle().setRight( 0, Style.Unit.PX );
    getElement().getStyle().setProperty( "maxHeight", "100%" );
  }

  private void addJobs( Panel panel, JsJob job, String filterString ) {
    panel.clear();

    JsJobParam[] params = SchedulerUiUtil.getFilteredJobParams( job )
      .stream()
      .filter( param -> param.getName().toLowerCase().contains( filterString ) || param.getValue().toLowerCase().contains( filterString ) )
      .toArray( JsJobParam[]::new );

    for ( JsJobParam param : params ) {
      Label name = new Label( param.getName() );
      Label value = new Label( param.getValue() );
      name.getElement().getStyle().setFontWeight( Style.FontWeight.BOLD );
      value.getElement().getStyle().setPaddingBottom( 1, Style.Unit.EM );
      panel.add( name );
      panel.add( value );
    }

    if ( params.length == 0 && filterString.isEmpty() ) {
      panel.add( new Label( Messages.getString( "paramsDialogEmpty" ) ) );
    }
    if ( params.length == 0 && !filterString.isEmpty() ) {
      panel.add( new Label( Messages.getString( "paramsDialogNoMatches" ) ) );
    }
  }

  @Override public void show() {
    super.show();
    layout();
    addJobs( paramsPanel, job, "" );
  }

  protected void onResize( ResizeEvent event ) {
    // No-op to avoid the parent class's centering behavior
  }

  @Override protected void beginDragging( MouseDownEvent event ) {
    // No-op to prevent the sidebar from being draggable like other dialog boxes
  }

  @Override protected void continueDragging( MouseMoveEvent event ) {
    // No-op to prevent the sidebar from being draggable like other dialog boxes
  }

  @Override protected void endDragging( MouseUpEvent event ) {
    // No-op to prevent the sidebar from being draggable like other dialog boxes
  }
}
