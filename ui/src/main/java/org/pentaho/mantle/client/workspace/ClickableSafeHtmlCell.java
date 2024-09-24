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

package org.pentaho.mantle.client.workspace;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * @author Rowell Belen
 */
public class ClickableSafeHtmlCell extends AbstractCell<SafeHtml> {

  public ClickableSafeHtmlCell() {
    super( "click" );
  }

  @Override
  public void onBrowserEvent( Context context, Element parent, SafeHtml value, NativeEvent event,
      ValueUpdater<SafeHtml> valueUpdater ) {

    // use default implementation for all events other than the click event
    super.onBrowserEvent( context, parent, value, event, valueUpdater );

    if ( "click".equals( event.getType() ) ) {

      // Ignore clicks that occur outside of the outermost element.
      EventTarget eventTarget = event.getEventTarget();
      if ( parent.getFirstChildElement() != null && parent.getFirstChildElement().isOrHasChild( Element.as( eventTarget ) ) ) {
        onEnterKeyDown( context, parent, value, event, valueUpdater );
      }
    }
  }

  @Override
  protected void onEnterKeyDown( Context context, Element parent, SafeHtml value, NativeEvent event,
      ValueUpdater<SafeHtml> valueUpdater ) {
    if ( valueUpdater != null ) {
      valueUpdater.update( value );
    }
  }

  @Override
  public void render( Context context, SafeHtml value, SafeHtmlBuilder sb ) {
    if ( value != null ) {
      sb.append( value );
    }
  }
}
