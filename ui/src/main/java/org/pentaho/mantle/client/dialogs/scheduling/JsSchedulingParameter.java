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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class JsSchedulingParameter extends JavaScriptObject implements ISchedulingParameter {

  protected JsSchedulingParameter() {
  }

  public final native String getName() /*-{
                                       return this.name;
                                       }-*/;

  public final native JsArrayString getStringValue() /*-{
                                                     return this.stringValue;
                                                     }-*/;

  public final native void setName( String name ) /*-{
                                                  this.name = name;
                                                  }-*/;

  public final native void setStringValue( JsArrayString value ) /*-{
                                                                 return this.stringValue = value;
                                                                 }-*/;

  public final native String getType() /*-{
                                       return this.type;
                                       }-*/;

  public final native void setType( String type ) /*-{
                                                  this.type = type;
                                                  }-*/;
}
