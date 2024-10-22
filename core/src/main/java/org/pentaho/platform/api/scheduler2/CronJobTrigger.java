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


package org.pentaho.platform.api.scheduler2;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class CronJobTrigger extends JobTrigger implements ICronJobTrigger {
  private static final long serialVersionUID = 2460248678333124471L;
  String cronString;

  public CronJobTrigger() {
  }

  protected CronJobTrigger( String cronString ) {
    this.cronString = cronString;
  }

  public String getCronString() {
    return cronString;
  }

  public void setCronString( String crongString ) {
    this.cronString = crongString;
  }

  public String toString() {
    return cronString;
  }

}
