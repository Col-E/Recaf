package software.coley.recaf.ui.pane.editing.android;

import jakarta.annotation.Nonnull;
import software.coley.observables.ObservableObject;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.decompile.JvmDecompiler;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.pane.editing.AbstractDecompilerPaneConfigurator;
import software.coley.recaf.ui.pane.editing.ToolsContainerComponent;
import software.coley.recaf.ui.pane.editing.jvm.DecompilerPaneConfig;

/**
 * Overlay component for {@link Editor} that allows quick configuration of properties of a {@link AndroidDecompilerPane}.
 *
 * @author Matt Coley
 */
public class AndroidDecompilerPaneConfigurator extends AbstractDecompilerPaneConfigurator {
	/**
	 * @param toolsContainer
	 * 		Container to house tool buttons for display in the {@link Editor}.
	 * @param config
	 * 		Containing {@link AndroidDecompilerPane} config singleton.
	 * @param decompiler
	 * 		Local decompiler implementation.
	 * @param decompilerManager
	 * 		Manager to pull available {@link JvmDecompiler} instances from.
	 */
	public AndroidDecompilerPaneConfigurator(@Nonnull ToolsContainerComponent toolsContainer,
											 @Nonnull DecompilerPaneConfig config,
											 @Nonnull ObservableObject<JvmDecompiler> decompiler,
											 @Nonnull DecompilerManager decompilerManager) {
		super(toolsContainer, config, decompiler, decompilerManager);
	}
}
