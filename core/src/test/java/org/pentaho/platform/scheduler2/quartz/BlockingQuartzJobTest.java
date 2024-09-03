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


package org.pentaho.platform.scheduler2.quartz;

import static org.junit.Assert.assertEquals;

import org.apache.commons.logging.Log;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.scheduler2.IBlockoutManager;
import org.pentaho.platform.engine.core.audit.MDCUtil;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.impl.JobDetailImpl;

/**
 * Unit tests for BlockingQuartzJob
 * 
 * @author kwalker
 */
public class BlockingQuartzJobTest {
  private Job underlyingJob;

  private JobExecutionContext context;

  private IBlockoutManager blockoutManager;

  private Mockery mockery;

  private Log logger;

  private SchedulerException schedulerException = new SchedulerException( "something bad happened" );

  @Before
  public void setUp() throws Exception {
    mockery = new Mockery() {
      {
        setImposteriser( ClassImposteriser.INSTANCE );
      }
    };
    underlyingJob = mockery.mock( Job.class );
    context = mockery.mock( JobExecutionContext.class );
    blockoutManager = mockery.mock( IBlockoutManager.class );
    logger = mockery.mock( Log.class );
  }

  @After
  public void tearDown() throws Exception {
    mockery.assertIsSatisfied();
  }

  /**
   * Verify that MDC context contains the sessionName, sessionId and instanceId for scheduler jobs.
   * 
   * @throws JobExecutionException
   */
  @Test
  public void testMDCContext() throws JobExecutionException {
    PentahoSessionHolder.setSession( new StandaloneSession( "test user", "SESSION_ID_123" ) );

    JobDetail jobDetail = new JobDetailImpl( "myjob", BlockingQuartzJob.class );
    jobDetail.getJobDataMap().put( QuartzScheduler.RESERVEDMAPKEY_ACTIONUSER, "test user" );
    jobDetail.getJobDataMap().put( "lineage-id", "INSTANCE_ID_123" );

    // Verify we start with empty MDC context
    MDCUtil mdc = new MDCUtil();
    mdc.getContextMap().clear();
    Assert.assertTrue( mdc.getContextMap().isEmpty() );

    BlockingQuartzJob blockingJob = createTestBlockingJob( false );
    mockery.checking( new Expectations() {
      {
        one( blockoutManager ).shouldFireNow();
        will( returnValue( false ) );
        one( logger ).warn( "Job 'myjob' attempted to run during a blockout period.  This job was not executed" );
        allowing( context ).getJobDetail();
        will( returnValue( jobDetail ) );
      }
    } );
    blockingJob.execute( context );

    // Verify MDC context is now populated
    mdc = new MDCUtil();
    assertEquals( "SESSION_ID_123", mdc.getContextMap().get( MDCUtil.SESSION_ID ) );
    assertEquals( "INSTANCE_ID_123", mdc.getContextMap().get( MDCUtil.INSTANCE_ID ) );
    assertEquals( "test user", mdc.getContextMap().get( MDCUtil.SESSION_NAME ) );
  }

  @Test
  public void testJobIsBlockedDuringABlockout() throws JobExecutionException {
    BlockingQuartzJob blockingJob = createTestBlockingJob( false );
    mockery.checking( new Expectations() {
      {
        one( blockoutManager ).shouldFireNow();
        will( returnValue( false ) );
        one( logger ).warn( "Job 'myjob' attempted to run during a blockout period.  This job was not executed" );
        allowing( context ).getJobDetail();
        will( returnValue( new JobDetailImpl( "myjob", BlockingQuartzJob.class ) ) );
      }
    } );
    blockingJob.execute( context );
  }

  @Test
  public void testJobIsRunWhenNoBlockout() throws JobExecutionException {
    BlockingQuartzJob blockingJob = createTestBlockingJob( false );
    try {
      mockery.checking( new Expectations() {
        {
          oneOf( context ).getJobDetail();
          will( returnValue( new JobDetailImpl( "somejob", BlockingQuartzJob.class ) ) );
          oneOf( context ).getJobDetail();
          will( returnValue( new JobDetailImpl( "somejob", BlockingQuartzJob.class ) ) );
          oneOf( context ).getJobDetail();
          will( returnValue( new JobDetailImpl( "somejob", BlockingQuartzJob.class ) ) );
          one( blockoutManager ).shouldFireNow();
          will( returnValue( true ) );
          one( underlyingJob ).execute( with( same( context ) ) );
        }
      } );
    } catch ( SchedulerException e ) {
      throw new RuntimeException( e );
    }
    blockingJob.execute( context );
  }

  @Test
  public void testJobIsRunWhenThereIsAnExceptionRetrievingTheBlockoutManager() throws JobExecutionException {
    BlockingQuartzJob blockingJob = createTestBlockingJob( true );
    mockery.checking( new Expectations() {
      {
        oneOf( context ).getJobDetail();
        will( returnValue( new JobDetailImpl( "somejob", BlockingQuartzJob.class ) ) );
        oneOf( context ).getJobDetail();
        will( returnValue( new JobDetailImpl( "somejob", BlockingQuartzJob.class ) ) );
        oneOf( context ).getJobDetail();
        will( returnValue( new JobDetailImpl( "somejob", BlockingQuartzJob.class ) ) );
        one( underlyingJob ).execute( with( same( context ) ) );
        one( context ).getJobDetail();
        will( returnValue( new JobDetailImpl( "somejob", BlockingQuartzJob.class ) ) );
        one( logger ).warn( "Got Exception retrieving the Blockout Manager for job 'somejob'."
            + " Executing the underlying job anyway", schedulerException );
      }
    } );
    blockingJob.execute( context );
  }

  private BlockingQuartzJob createTestBlockingJob( final boolean throwSchedulerException ) {
    return new BlockingQuartzJob() {
      @Override
      Job createUnderlyingJob() {
        return underlyingJob;
      }

      @Override
      IBlockoutManager getBlockoutManager() throws SchedulerException {

        if ( throwSchedulerException ) {
          throw schedulerException;
        } else {
          return blockoutManager;
        }
      }

      @Override
      Log getLogger() {
        return logger;
      }

      @Override
      protected void makeAuditRecord( float time, String messageType, JobExecutionContext jobExecutionContext ) {

      }
    };
  }

}
