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

package org.pentaho.platform.scheduler2.quartz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.pentaho.platform.api.action.IAction;
import org.pentaho.platform.api.action.IActionInvokeStatus;
import org.pentaho.platform.api.action.IActionInvoker;
import org.pentaho.platform.api.engine.IPentahoObjectFactory;
import org.pentaho.platform.api.engine.ISecurityHelper;
import org.pentaho.platform.api.scheduler2.IBlockoutManager;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.scheduler2.blockout.BlockoutAction;
import org.pentaho.platform.util.ActionUtil;
import org.pentaho.platform.workitem.WorkItemLifecycleEventUtil;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ActionAdapterQuartzJobTest {

  private static final String ACTION_ID = "testAction";
  private static final String ACTION_USER = "testUser";

  @Test( expected = JobExecutionException.class )
  public void invokeActionFailsWhenActionCannotBeCreated() throws Exception {
    IActionInvoker actionInvoker = mock( IActionInvoker.class );

    try ( StaticMocks staticMocks = new StaticMocks() ) {
      staticMocks.actionUtil.when( () -> ActionUtil.createActionBean( null, ACTION_ID ) ).thenReturn( null );

      testJob( actionInvoker ).invokeAction( null, ACTION_ID, ACTION_USER, context(), new HashMap<>() );
    }
  }

  @Test
  public void invokeActionReturnsWhenRemoteInvocationHasNoStatus() throws Exception {
    IAction action = mock( IAction.class );
    IActionInvoker actionInvoker = mock( IActionInvoker.class );
    when( actionInvoker.invokeAction( eq( action ), eq( ACTION_USER ), any() ) ).thenReturn( null );

    try ( StaticMocks staticMocks = new StaticMocks() ) {
      staticMocks.actionUtil.when( () -> ActionUtil.createActionBean( null, ACTION_ID ) ).thenReturn( action );

      testJob( actionInvoker ).invokeAction( null, ACTION_ID, ACTION_USER, context(), new HashMap<>() );
    }

    verify( actionInvoker ).invokeAction( eq( action ), eq( ACTION_USER ), any() );
  }

  @Test( expected = JobExecutionException.class )
  public void invokeActionFailsWhenInvocationReportsUnsuccessfulExecution() throws Exception {
    IAction action = mock( IAction.class );
    IActionInvoker actionInvoker = mock( IActionInvoker.class );
    IActionInvokeStatus status = mock( IActionInvokeStatus.class );
    when( status.isExecutionSuccessful() ).thenReturn( false );
    when( actionInvoker.invokeAction( eq( action ), eq( ACTION_USER ), any() ) ).thenReturn( status );

    try ( StaticMocks staticMocks = new StaticMocks() ) {
      staticMocks.actionUtil.when( () -> ActionUtil.createActionBean( null, ACTION_ID ) ).thenReturn( action );

      testJob( actionInvoker ).invokeAction( null, ACTION_ID, ACTION_USER, context(), new HashMap<>() );
    }
  }

  @Test
  public void invokeActionPassesScheduledFireTimeToBlockoutActions() throws Exception {
    Date scheduledFireTime = new Date();
    BlockoutAction action = mock( BlockoutAction.class );
    IActionInvoker actionInvoker = mock( IActionInvoker.class );
    when( actionInvoker.invokeAction( eq( action ), eq( ACTION_USER ), any() ) ).thenReturn( null );
    Map<String, Object> params = new HashMap<>();

    try ( StaticMocks staticMocks = new StaticMocks() ) {
      staticMocks.actionUtil.when( () -> ActionUtil.createActionBean( null, ACTION_ID ) ).thenReturn( action );

      testJob( actionInvoker ).invokeAction( null, ACTION_ID, ACTION_USER, context( scheduledFireTime ), params );
    }

    ArgumentCaptor<Map<String, Object>> paramsCaptor = paramsCaptor();
    verify( actionInvoker ).invokeAction( eq( action ), eq( ACTION_USER ), paramsCaptor.capture() );
    assertSame( scheduledFireTime, params.get( IBlockoutManager.SCHEDULED_FIRE_TIME ) );
    assertSame( scheduledFireTime, paramsCaptor.getValue().get( IBlockoutManager.SCHEDULED_FIRE_TIME ) );
  }

  @Test
  public void invokeActionRecreatesRunOnceJobWhenInvocationReportsThrowable() throws Exception {
    IAction action = mock( IAction.class );
    IActionInvoker actionInvoker = mock( IActionInvoker.class );
    statusWithThrowable( action, actionInvoker );
    IScheduler scheduler = mock( IScheduler.class );
    IPentahoObjectFactory objectFactory = mock( IPentahoObjectFactory.class );
    ISecurityHelper securityHelper = mock( ISecurityHelper.class );
    when( objectFactory.get( IScheduler.class, "IScheduler2", null ) ).thenReturn( scheduler );
    when( securityHelper.runAsUser( eq( ACTION_USER ), any() ) ).thenAnswer( invocation ->
      ( (java.util.concurrent.Callable<?>) invocation.getArgument( 1 ) ).call() );
    SecurityHelper.setMockInstance( securityHelper );

    try ( StaticMocks staticMocks = new StaticMocks() ) {
      staticMocks.actionUtil.when( () -> ActionUtil.createActionBean( null, ACTION_ID ) ).thenReturn( action );
      staticMocks.pentahoSystem.when( PentahoSystem::getObjectFactory ).thenReturn( objectFactory );

      try {
        testJob( actionInvoker ).invokeAction( null, ACTION_ID, ACTION_USER, context(), new HashMap<>() );
        fail( "Expected invocation failure to be reported to Quartz" );
      } catch ( JobExecutionException expected ) {
        // The initial invocation still fails after the one-time retry is scheduled.
      }
    } finally {
      SecurityHelper.setMockInstance( null );
    }

    ArgumentCaptor<Map<String, Object>> paramsCaptor = paramsCaptor();
    ArgumentCaptor<SimpleJobTrigger> triggerCaptor = ArgumentCaptor.forClass( SimpleJobTrigger.class );
    verify( scheduler ).createJob( eq( "testJob" ), eq( action.getClass() ), paramsCaptor.capture(),
      triggerCaptor.capture(), any() );
    assertEquals( Boolean.TRUE, paramsCaptor.getValue().get( QuartzScheduler.RESERVEDMAPKEY_RESTART_FLAG ) );
    assertEquals( "RUN_ONCE", triggerCaptor.getValue().getUiPassParam() );
    assertEquals( 0, triggerCaptor.getValue().getRepeatCount() );
    assertEquals( 1, triggerCaptor.getValue().getRepeatInterval() );
    assertNull( triggerCaptor.getValue().getEndTime() );
  }

  @Test
  public void invokeActionDoesNotRecreateJobWhenRestartFlagIsAlreadyPresent() throws Exception {
    IAction action = mock( IAction.class );
    IActionInvoker actionInvoker = mock( IActionInvoker.class );
    statusWithThrowable( action, actionInvoker );
    IScheduler scheduler = mock( IScheduler.class );
    IPentahoObjectFactory objectFactory = mock( IPentahoObjectFactory.class );
    when( objectFactory.get( IScheduler.class, "IScheduler2", null ) ).thenReturn( scheduler );
    Map<String, Object> params = new HashMap<>();
    params.put( QuartzScheduler.RESERVEDMAPKEY_RESTART_FLAG, Boolean.TRUE );

    try ( StaticMocks staticMocks = new StaticMocks() ) {
      staticMocks.actionUtil.when( () -> ActionUtil.createActionBean( null, ACTION_ID ) ).thenReturn( action );
      staticMocks.pentahoSystem.when( PentahoSystem::getObjectFactory ).thenReturn( objectFactory );

      try {
        testJob( actionInvoker ).invokeAction( null, ACTION_ID, ACTION_USER, context(), params );
        fail( "Expected invocation failure to be reported to Quartz" );
      } catch ( JobExecutionException expected ) {
        // Retry jobs must not schedule another retry.
      }
    }

    verify( scheduler, never() ).createJob( any(), Mockito.<Class<? extends IAction>>any(), any(), any(), any() );
  }

  private ActionAdapterQuartzJob testJob( final IActionInvoker actionInvoker ) {
    return new ActionAdapterQuartzJob() {
      @Override
      public IActionInvoker getActionInvoker() {
        return actionInvoker;
      }
    };
  }

  private JobExecutionContext context() throws Exception {
    return context( new Date() );
  }

  private JobExecutionContext context( Date scheduledFireTime ) throws Exception {
    JobExecutionContext context = mock( JobExecutionContext.class );
    JobDetail jobDetail = JobBuilder.newJob( ActionAdapterQuartzJob.class ).withIdentity(
      new QuartzJobKey( "testJob", ACTION_USER ).toString() ).build();
    when( context.getJobDetail() ).thenReturn( jobDetail );
    when( context.getScheduledFireTime() ).thenReturn( scheduledFireTime );
    return context;
  }

  private IActionInvokeStatus statusWithThrowable( IAction action, IActionInvoker actionInvoker ) throws Exception {
    IActionInvokeStatus status = mock( IActionInvokeStatus.class );
    when( status.isExecutionSuccessful() ).thenReturn( true );
    when( status.getThrowable() ).thenReturn( new IllegalStateException( "action failed" ) );
    when( actionInvoker.invokeAction( eq( action ), eq( ACTION_USER ), any() ) ).thenReturn( status );
    return status;
  }

  @SuppressWarnings( "unchecked" )
  private ArgumentCaptor<Map<String, Object>> paramsCaptor() {
    return ArgumentCaptor.forClass( Map.class );
  }

  private static class StaticMocks implements AutoCloseable {
    private final MockedStatic<ActionUtil> actionUtil = Mockito.mockStatic( ActionUtil.class );
    private final MockedStatic<PentahoSystem> pentahoSystem = Mockito.mockStatic( PentahoSystem.class );
    private final MockedStatic<WorkItemLifecycleEventUtil> workItemEvents =
      Mockito.mockStatic( WorkItemLifecycleEventUtil.class );

    private StaticMocks() {
      actionUtil.when( () -> ActionUtil.extractName( any() ) ).thenReturn( "testWorkItem" );
      pentahoSystem.when( () -> PentahoSystem.get( IActionInvoker.class ) ).thenReturn( null );
    }

    @Override
    public void close() {
      workItemEvents.close();
      pentahoSystem.close();
      actionUtil.close();
    }
  }
}
