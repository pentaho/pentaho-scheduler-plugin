package org.pentaho.mantle.client.workspace.dialogs;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import org.pentaho.gwt.widgets.client.ui.Banner;
import org.pentaho.gwt.widgets.client.ui.Card;
import org.pentaho.gwt.widgets.client.dialogs.PromptDialogBox;
import org.pentaho.gwt.widgets.client.panel.HorizontalFlexPanel;
import org.pentaho.gwt.widgets.client.panel.VerticalFlexPanel;
import org.pentaho.gwt.widgets.client.text.TextCopyToClipboard;
import org.pentaho.mantle.client.messages.Messages;
import org.pentaho.mantle.client.workspace.JsJob;

import java.util.Set;
import java.util.stream.Collectors;

public class ExecuteAvailable extends PromptDialogBox {
  public static final String STYLE_NAME = "execute-available-dialog";

  private static final String TITLE = Messages.getString(  "dialog.executeAvailable.title" );
  private static final String CANCEL = Messages.getString(  "cancel" );

  private final Set<JsJob> jobs;
  private final Set<JsJob> noPermissionJobs;

  public ExecuteAvailable( Set<JsJob> jobs, Set<JsJob> noPermissionJobs ) {
    super( TITLE, Messages.getString( "dialog.executeAvailable.run", String.valueOf( jobs.size() ) ), CANCEL,
      false, true );

    this.jobs = jobs;
    this.noPermissionJobs = noPermissionJobs;

    createUI();

    setResponsive( true );
    setSizingMode( DialogSizingMode.FILL_VIEWPORT_WIDTH );
    setWidthCategory( DialogWidthCategory.SMALL );
    addStyleName( STYLE_NAME );
  }

  private void createUI() {
    VerticalFlexPanel content = new VerticalFlexPanel();

    content.add( new Banner( "warning", Messages.getString( "dialog.executeAvailable.banner" ) ) );
    content.add( createCardsUI() );

    HTML description = new HTML( Messages.getString( "dialog.resolveLackPermission" ) );
    description.setStyleName( "typography typography-body dialog-description" );
    content.add( description );

    String noPermissionsValue = this.noPermissionJobs
      .stream()
      .map( JsJob::getFullResourceName )
      .collect( Collectors.joining( "\n" ) );
    content.add( new TextCopyToClipboard( noPermissionsValue ) );

    setContent( content );
    setWidth( "530px" );
  }

  private HorizontalPanel createCardsUI() {
    HorizontalFlexPanel panel = new HorizontalFlexPanel();
    panel.addStyleName( "cards-container flex-column-sm" );
    panel.setWidth( "100%" );

    Label successCount = new Label( String.valueOf( jobs.size() ) );
    successCount.setStyleName( "typography typography-title-2" );
    panel.add( new Card( "success", Messages.getString( "dialog.executeAvailable.successCardTitle" ),
      null, successCount ) );

    Label errorCount = new Label( String.valueOf( noPermissionJobs.size() ) );
    errorCount.setStyleName( "typography typography-title-2" );
    panel.add( new Card( "error", Messages.getString( "dialog.executeAvailable.errorCardTitle" ),
      Messages.getString( "dialog.executeAvailable.errorCardSubtitle" ), errorCount ) );

    return panel;
  }
}
