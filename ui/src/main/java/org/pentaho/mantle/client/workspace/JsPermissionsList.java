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
import com.google.gwt.core.client.JsArray;

import java.util.ArrayList;
import java.util.List;

public class JsPermissionsList extends JavaScriptObject {

  // Overlay types always have protected, zero argument constructors.
  protected JsPermissionsList() {
  }

  public final native JsArray<JsJobParam> getList() /*-{ return this.setting; }-*/;
  public final native void setList( JsArray<JsJobParam> settingList ) /*-{ this.setting = settingList; }-*/;

  public final List<String> getReadableFiles() {
    final List<String> files = new ArrayList<String>();

    final JsArray<JsJobParam> permissionList = getList();

    for ( int idx = 0; idx < permissionList.length(); idx++ ) {
      final JsJobParam param = permissionList.get( idx );

      final String name = param.getName();

      boolean isReadable = "0".equals( param.getValue() );
      if ( isReadable && !files.contains( name ) ) {
        files.add( name );
      }
    }

    return files;
  }
}
