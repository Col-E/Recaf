package software.coley.recaf.ui.window;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import software.coley.recaf.services.attach.AttachManager;
import software.coley.recaf.services.attach.AttachManagerConfig;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.ui.pane.RemoteVirtualMachinesPane;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.ThreadUtil;

/**
 * Window wrapper for {@link RemoteVirtualMachinesPane}.
 *
 * @author Matt Coley
 * @see RemoteVirtualMachinesPane
 */
@Dependent
public class RemoteVirtualMachinesWindow extends AbstractIdentifiableStage {
	@Inject
	public RemoteVirtualMachinesWindow(@Nonnull RemoteVirtualMachinesPane remoteVirtualMachinesPane,
									   @Nonnull AttachManager attachManager,
									   @Nonnull AttachManagerConfig attachManagerConfig) {
		super(WindowManager.WIN_REMOTE_VMS);

		// Bind attach manager scanning state to the visibility of this window.
		showingProperty().addListener((ob, old, showing) -> {
			// When showing run a scan immediately.
			// We are already registered as a scan listener, so we can update the display after it finishes.
			if (showing)
				ThreadUtil.run(attachManager::scan);

			// Bind scanning to only run when the UI is displayed.
			attachManagerConfig.getPassiveScanning().setValue(showing);
		});

		// Layout
		titleProperty().bind(Lang.getBinding("menu.file.attach"));
		setMinWidth(750);
		setMinHeight(450);
		setScene(new RecafScene(remoteVirtualMachinesPane, 750, 450));
	}
}
