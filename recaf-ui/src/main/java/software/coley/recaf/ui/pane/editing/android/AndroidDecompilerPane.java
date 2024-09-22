package software.coley.recaf.ui.pane.editing.android;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.source.AstResolveResult;
import software.coley.recaf.services.info.association.FileTypeSyntaxAssociationService;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.control.richtext.source.JavaContextActionSupport;
import software.coley.recaf.ui.pane.editing.AbstractDecompilePane;
import software.coley.recaf.ui.pane.editing.ToolsContainerComponent;
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
								 @Nonnull ToolsContainerComponent toolsContainer,
								 @Nonnull JavaContextActionSupport contextActionSupport,
								 @Nonnull FileTypeSyntaxAssociationService languageAssociation,
								 @Nonnull DecompilerManager decompilerManager,
								 @Nonnull Actions actions) {
		super(config, searchBar, contextActionSupport, languageAssociation, decompilerManager);

		// Install tools container with configurator
		new AndroidDecompilerPaneConfigurator(toolsContainer, config, decompiler, decompilerManager);
		new AndroidClassInfoProvider(toolsContainer, this);
		installToolsContainer(toolsContainer);

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
