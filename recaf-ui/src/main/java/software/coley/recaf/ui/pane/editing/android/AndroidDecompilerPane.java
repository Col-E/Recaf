package software.coley.recaf.ui.pane.editing.android;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.info.association.FileTypeSyntaxAssociationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.navigation.NavigationHistoryService;
import software.coley.recaf.services.source.AstService;
import software.coley.recaf.services.tutorial.TutorialConfig;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.control.richtext.source.JavaContextActionSupport;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;
import software.coley.recaf.ui.control.richtext.suggest.java.typeindex.JavaTypeIndexService;
import software.coley.recaf.ui.pane.editing.AbstractDecompilePane;
import software.coley.recaf.ui.pane.editing.ToolsContainerComponent;
import software.coley.recaf.ui.pane.editing.jvm.DecompilerPaneConfig;
import software.coley.recaf.ui.pane.editing.text.TextConfig;

/**
 * Displays an {@link AndroidClassInfo} via a configured {@link Editor} as decompiled by {@link DecompilerManager}.
 *
 * @author Matt Coley
 */
@Dependent
public class AndroidDecompilerPane extends AbstractDecompilePane {

	@Inject
	public AndroidDecompilerPane(@Nonnull DecompilerPaneConfig decompilerConfig,
	                             @Nonnull TutorialConfig tutorialConfig,
	                             @Nonnull KeybindingConfig keys,
	                             @Nonnull SearchBar searchBar,
	                             @Nonnull ToolsContainerComponent toolsContainer,
	                             @Nonnull AstService astService,
	                             @Nonnull JavaContextActionSupport contextActionSupport,
	                             @Nonnull NavigationHistoryService navigationHistoryService,
	                             @Nonnull CellConfigurationService cellConfigurationService,
	                             @Nonnull FileTypeSyntaxAssociationService languageAssociation,
	                             @Nonnull DecompilerManager decompilerManager,
	                             @Nonnull JavaTypeIndexService javaTypeIndexService,
	                             @Nonnull TabCompletionConfig tabCompletionConfig,
	                             @Nonnull TextConfig textConfig,
	                             @Nonnull Actions actions) {
		super(decompilerConfig, tutorialConfig, keys, searchBar, astService, contextActionSupport, navigationHistoryService, cellConfigurationService,
				languageAssociation, decompilerManager, javaTypeIndexService, tabCompletionConfig, textConfig, actions);

		// Install tools container with configurator
		new AndroidDecompilerPaneConfigurator(toolsContainer, decompilerConfig, decompiler, decompilerManager);
		new AndroidClassInfoProvider(toolsContainer, this);
		installToolsContainer(toolsContainer);
	}
}
