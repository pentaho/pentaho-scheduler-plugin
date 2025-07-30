/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2025 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/

package org.pentaho.platform.web.http.api.resources;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class JobsResponseEntry {

  String key;
  String value;

  public JobsResponseEntry() {
  }

  public JobsResponseEntry( String key, String value ) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public void setKey( String key ) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue( String value ) {
    this.value=value;
  }

}
