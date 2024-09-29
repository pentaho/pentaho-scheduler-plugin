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

import com.google.gwt.core.client.JavaScriptObject;

public class JsJobParam extends JavaScriptObject {

  // Overlay types always have protected, zero argument constructors.
  protected JsJobParam() {
  }

  // JSNI methods to get job data.
  public final native String getName() /*-{ return this.name; }-*/; //

  public final native String getValue() /*-{ return this.value; }-*/; //

  public final native void setName( String name ) /*-{ this.name = name; }-*/;

  public final native void setValue( String value ) /*-{ this.value = value; }-*/;
}
