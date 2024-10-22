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


package org.pentaho.platform.scheduler2.ws;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class StringParamValue implements ParamValue {
  private String stringValue = null;

  public StringParamValue() {
  }

  public StringParamValue( String value ) {
    this.stringValue = value;
  }

  public void setStringValue( String value ) {
    this.stringValue = value;
  }

  public String getStringValue() {
    return stringValue;
  }

  public String toString() {
    return stringValue;
  }
}
