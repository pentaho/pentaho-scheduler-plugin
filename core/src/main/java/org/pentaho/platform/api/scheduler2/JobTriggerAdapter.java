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

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.scheduler2.quartz.QuartzScheduler;

public class JobTriggerAdapter extends XmlAdapter<JobTrigger, JobTrigger> {

  public JobTrigger marshal( JobTrigger v ) throws Exception {
    return v instanceof ComplexJobTrigger ? new CronJobTrigger( v.toString() ) : v;
  }

  public JobTrigger unmarshal( JobTrigger v ) throws Exception {
    IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", null );
    return v instanceof CronJobTrigger ? (JobTrigger) scheduler.createComplexTrigger( v.toString() ) : v;
  }

}
