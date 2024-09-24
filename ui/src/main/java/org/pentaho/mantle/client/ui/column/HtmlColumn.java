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

package org.pentaho.mantle.client.ui.column;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.safehtml.shared.OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;

public abstract class HtmlColumn<T> extends Column<T, SafeHtml> {
  
  public HtmlColumn() {
    super( new SafeHtmlCell() );
  }
  
  public HtmlColumn( Cell<SafeHtml> cell ) {
    super( cell );
  }

  @Override
  public SafeHtml getValue( T t ) {
    return new OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml( getStringValue( t ) );
  }

  public abstract String getStringValue( T t );
}
