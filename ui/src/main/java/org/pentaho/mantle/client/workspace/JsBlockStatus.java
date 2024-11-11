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


package org.pentaho.mantle.client.workspace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;

/**
 * Wraps the JSON response for a blockoutStatus REST response. We get back a string that represents something like:
 * {"partiallyBlocked":"false", "totallyBlocked":"false"}
 */
public class JsBlockStatus extends JavaScriptObject {

  // Overlay types always have protected, zero argument constructors.
  protected JsBlockStatus() {
  }

  // JSNI methods to get job data.
  public final native String getPartiallyBlocked() /*-{ return this.partiallyBlocked; }-*/; //

  public final native String getTotallyBlocked() /*-{ return this.totallyBlocked; }-*/; //

  public final String getJSONString() {
    return new JSONObject( this ).toString();
  }
}
