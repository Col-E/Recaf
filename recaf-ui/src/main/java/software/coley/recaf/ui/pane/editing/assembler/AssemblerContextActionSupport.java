package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.CodeArea;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.path.AssemblerPathData;
import software.coley.recaf.path.AssemblerPathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.context.ContextMenuProviderService;
import software.coley.recaf.services.cell.context.ContextSource;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.source.JavaContextActionSupport;
import software.coley.recaf.ui.pane.editing.assembler.resolve.AssemblyResolution;
import software.coley.recaf.ui.pane.editing.assembler.resolve.AssemblyResolver;

/**
 * Enables context actions on an {@link Editor} within an {@link AssemblerPane}.
 * The AST of the last successful parse from the assembler is used to query for menus offered by {@link ContextMenuProviderService}.
 *
 * @author Matt Coley
 * @see JavaContextActionSupport Alternative for context actions on Java source.
 */
@Dependent
public class AssemblerContextActionSupport extends AstBuildConsumerComponent {
	private static final DebuggingLogger logger = Logging.get(AssemblerContextActionSupport.class);
	private final AssemblyResolver resolver = new AssemblyResolver();
	private final CellConfigurationService cellConfigurationService;
	private ContextMenu menu;

	@Inject
	public AssemblerContextActionSupport(@Nonnull CellConfigurationService cellConfigurationService) {
		this.cellConfigurationService = cellConfigurationService;
	}

	@Override
	public void install(@Nonnull Editor editor) {
		super.install(editor);
		CodeArea area = editor.getCodeArea();
		area.setOnContextMenuRequested(e -> {
			if (menu != null) {
				menu.hide();
				menu = null;
			}

			// Check AST model has been generated
			if (astElements == null) {
				logger.warn("Could not request context menu, AST model not available");
				return;
			}

			// Convert the event position to line/column
			CharacterHit hit = area.hit(e.getX(), e.getY());

			// Sync caret
			area.moveTo(hit.getInsertionIndex());

			// Create menu
			AssemblyResolution resolution = resolver.resolveAt(hit.getInsertionIndex());
			AssemblerPathData data = new AssemblerPathData(editor, resolution);
			menu = cellConfigurationService.contextMenuOf(ContextSource.DECLARATION, new AssemblerPathNode(path, data));

			// Show menu
			if (menu != null && !menu.getItems().isEmpty()) {
				menu.setAutoHide(true);
				menu.setHideOnEscape(true);
				menu.show(area.getScene().getWindow(), e.getScreenX(), e.getScreenY());
				menu.requestFocus();
			}
		});
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		super.uninstall(editor);
		editor.getCodeArea().setOnContextMenuRequested(null);
	}

	@Override
	protected void onPipelineOutputUpdate() {
		resolver.setAst(astElements);
	}
}
