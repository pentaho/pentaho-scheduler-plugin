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

package org.pentaho.platform.scheduler2.blockout;

import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.action.IVarArgsAction;
import org.pentaho.platform.api.scheduler2.IBlockoutManager;

/**
 * @author wseyler This is the job that executes when the a block out trigger fires. This job essentially does nothing
 *         more than logging the firing of the trigger.
 */
public class BlockoutAction implements IVarArgsAction {

  private static final Log logger = LogFactory.getLog( BlockoutAction.class );

  long duration;
  Date scheduledFireTime;

  @Override
  public void execute() throws Exception {
    Date startDate = new Date();
    long effectiveDuration = duration - ( startDate.getTime() - scheduledFireTime.getTime() );
    if ( effectiveDuration < 0 ) {
      logger.warn( "Blocking Scheduled for " + scheduledFireTime + " for " + this.duration
          + " milliseconds has already expired" );
    } else {
      logger.warn( "Blocking Started at: " + startDate + " and will last: " + effectiveDuration + " milliseconds" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      Thread.sleep( effectiveDuration );
      logger.warn( "Blockout that started at: " + startDate + " has ended at: " + new Date() ); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  @Override
  public void setVarArgs( Map<String, Object> args ) {
    if ( args.containsKey( IBlockoutManager.DURATION_PARAM ) ) {
      this.duration = ( (Number) args.get( IBlockoutManager.DURATION_PARAM ) ).longValue();
    }
    if ( args.containsKey( IBlockoutManager.SCHEDULED_FIRE_TIME ) ) {
      this.scheduledFireTime = ( (Date) args.get( IBlockoutManager.SCHEDULED_FIRE_TIME ) );
    }
  }

}
