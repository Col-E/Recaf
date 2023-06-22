package software.coley.recaf.ui.pane.editing.android;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.source.AstResolveResult;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.control.richtext.source.JavaContextActionSupport;
import software.coley.recaf.ui.pane.editing.AbstractDecompilePane;
import software.coley.recaf.ui.pane.editing.AbstractDecompilerPaneConfigurator;
import software.coley.recaf.ui.pane.editing.jvm.DecompilerPaneConfig;

/**
 * Displays an {@link AndroidClassInfo} via a configured {@link Editor} as decompiled by {@link DecompilerManager}.
 *
 * @author Matt Coley
 */
@Dependent
public class AndroidDecompilerPane extends AbstractDecompilePane {
	@Inject
	public AndroidDecompilerPane(@Nonnull DecompilerPaneConfig config,
								 @Nonnull KeybindingConfig keys,
								 @Nonnull SearchBar searchBar,
								 @Nonnull JavaContextActionSupport contextActionSupport,
								 @Nonnull DecompilerManager decompilerManager,
								 @Nonnull Actions actions) {
		super(config, searchBar, contextActionSupport, decompilerManager);

		// Install configurator popup
		AbstractDecompilerPaneConfigurator configurator = new AndroidDecompilerPaneConfigurator(config, decompiler, decompilerManager);
		configurator.install(editor);

		// Setup keybindings
		setOnKeyPressed(e -> {
			if (keys.getRename().match(e)) {
				// Resolve what the caret position has, then handle renaming on the generic result.
				AstResolveResult result = contextActionSupport.resolvePosition(editor.getCodeArea().getCaretPosition());
				if (result != null)
					actions.rename(result.path());
			}
		});
	}
}
