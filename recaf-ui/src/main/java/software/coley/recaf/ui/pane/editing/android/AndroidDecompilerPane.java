package software.coley.recaf.ui.pane.editing.android;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.input.MouseButton;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.info.association.FileTypeSyntaxAssociationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.source.AstResolveResult;
import software.coley.recaf.services.source.AstService;
import software.coley.recaf.services.tutorial.TutorialConfig;
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
	private static final Logger logger = Logging.get(AndroidDecompilerPane.class);

	@Inject
	public AndroidDecompilerPane(@Nonnull DecompilerPaneConfig decompilerConfig,
								 @Nonnull TutorialConfig tutorialConfig,
	                             @Nonnull KeybindingConfig keys,
	                             @Nonnull SearchBar searchBar,
	                             @Nonnull ToolsContainerComponent toolsContainer,
	                             @Nonnull AstService astService,
	                             @Nonnull JavaContextActionSupport contextActionSupport,
	                             @Nonnull FileTypeSyntaxAssociationService languageAssociation,
	                             @Nonnull DecompilerManager decompilerManager,
	                             @Nonnull Actions actions) {
		super(decompilerConfig, tutorialConfig, searchBar, astService, contextActionSupport, languageAssociation, decompilerManager);

		// Install tools container with configurator
		new AndroidDecompilerPaneConfigurator(toolsContainer, decompilerConfig, decompiler, decompilerManager);
		new AndroidClassInfoProvider(toolsContainer, this);
		installToolsContainer(toolsContainer);

		// Setup keybindings
		setOnKeyPressed(e -> {
			if (keys.getRename().match(e)) {
				// Resolve what the caret position has, then handle renaming on the generic result.
				AstResolveResult result = contextActionSupport.resolvePosition(editor.getCodeArea().getCaretPosition());
				if (result != null)
					actions.rename(result.path());
			} else if (keys.getGoto().match(e)) {
				// Resolve what the caret position has, then handle navigating to the resulting path.
				AstResolveResult result = contextActionSupport.resolvePosition(editor.getCodeArea().getCaretPosition());
				if (result != null) {
					try {
						actions.gotoDeclaration(result.path());
					} catch (IncompletePathException ex) {
						// Should realistically never happen
						logger.warn("Cannot goto location, path incomplete", ex);
					}
				}
			}
		});
		setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.PRIMARY && e.isControlDown()) {
				// Resolve what the caret position has, then handle navigating to the resulting path.
				AstResolveResult result = contextActionSupport.resolvePosition(editor.getCodeArea().getCaretPosition());
				if (result != null) {
					try {
						actions.gotoDeclaration(result.path());
					} catch (IncompletePathException ex) {
						// Should realistically never happen
						logger.warn("Cannot goto location, path incomplete", ex);
					}
				}
			}
		});
	}
}
