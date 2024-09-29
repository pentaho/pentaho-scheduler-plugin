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


package org.pentaho.platform.api.scheduler2;

import javax.xml.bind.annotation.XmlElement;

public class JobParam {
  @XmlElement
  String name;
  @XmlElement
  String value;
}
