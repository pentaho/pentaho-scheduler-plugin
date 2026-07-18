/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/



package org.pentaho.platform.scheduler2.ws;

import java.util.ArrayList;
import jakarta.xml.bind.annotation.XmlRootElement;

@SuppressWarnings( "serial" )
@XmlRootElement
public class ListParamValue extends ArrayList<String> implements ParamValue {
}
