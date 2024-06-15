package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.AssemblerPathData;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.pane.editing.assembler.resolve.*;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.ARROW_RIGHT;
import static software.coley.recaf.util.Menus.action;

/**
 * Basic implementation for {@link AssemblerContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicAssemblerContextMenuProviderFactory extends AbstractContextMenuProviderFactory implements AssemblerContextMenuProviderFactory {
	private static final Logger logger = Logging.get(BasicAssemblerContextMenuProviderFactory.class);
	private static final Map<Class<? extends AssemblyResolution>, ResolutionMenuFiller<AssemblyResolution>> fillers = new IdentityHashMap<>();

	@Inject
	public BasicAssemblerContextMenuProviderFactory(@Nonnull TextProviderService textService,
													@Nonnull IconProviderService iconService,
													@Nonnull Actions actions) {
		super(textService, iconService, actions);
	}

	@Nonnull
	@Override
	public ContextMenuProvider getAssemblerMenuProvider(@Nonnull ContextSource source,
														@Nonnull Workspace workspace,
														@Nonnull WorkspaceResource resource,
														@Nonnull ClassBundle<? extends ClassInfo> bundle,
														@Nonnull ClassInfo declaringClass,
														@Nonnull AssemblerPathData assemblerData) {
		return () -> {
			ContextMenu menu = new ContextMenu();
			AssemblyResolution resolution = assemblerData.resolution();
			Editor editor = assemblerData.editor();
			ResolutionMenuFiller<AssemblyResolution> filler = fillers.get(resolution.getClass());
			if (filler != null) filler.accept(this, menu, editor, workspace, resolution);
			if (menu.getItems().isEmpty()) return null;
			return menu;
		};
	}

	/**
	 * Helper to register {@link ResolutionMenuFiller} instances.
	 *
	 * @param type
	 * 		Subtype of {@link AssemblyResolution}.
	 * @param filler
	 * 		Filler for the given subtype.
	 * @param <T>
	 * 		Subtype of {@link AssemblyResolution}.
	 */
	private static <T extends AssemblyResolution> void register(@Nonnull Class<T> type, @Nonnull ResolutionMenuFiller<T> filler) {
		fillers.put(type, Unchecked.cast(filler));
	}

	static {
		register(ClassImplements.class, (provider, menu, editor, workspace, resolution) -> {
			ClassPathNode classPath = workspace.findClass(resolution.implemented().literal());
			ActionMenuItem action = action("menu.goto.class", ARROW_RIGHT, () -> {
				try {
					provider.actions.gotoDeclaration(Objects.requireNonNull(classPath));
				} catch (IncompletePathException ex) {
					logger.error("Cannot go to class due to incomplete path", ex);
				}
			});
			if (classPath == null) action.setDisable(true);
			menu.getItems().add(action);

			// TODO:
			//  - Implement methods (for methods not already present in the ASTClass)
		});
		register(ClassExtends.class, (provider, menu, editor, workspace, resolution) -> {
			ClassPathNode classPath = workspace.findClass(resolution.superName().literal());
			ActionMenuItem action = action("menu.goto.class", ARROW_RIGHT, () -> {
				try {
					provider.actions.gotoDeclaration(Objects.requireNonNull(classPath));
				} catch (IncompletePathException ex) {
					logger.error("Cannot go to class due to incomplete path", ex);
				}
			});
			if (classPath == null) action.setDisable(true);
			menu.getItems().add(action);

			// TODO:
			//  - Override methods (for methods not already present in the ASTClass)
		});
		register(ClassAnnotationResolution.class, (provider, menu, editor, workspace, resolution) -> {
			// No items
		});
		register(FieldAnnotationResolution.class, (provider, menu, editor, workspace, resolution) -> {
			// No items
		});
		register(MethodAnnotationResolution.class, (provider, menu, editor, workspace, resolution) -> {
			// No items
		});
		register(IndependentAnnotationResolution.class, (provider, menu, editor, workspace, resolution) -> {
			// No items
		});
		register(InnerClassResolution.class, (provider, menu, editor, workspace, resolution) -> {
			// No items
		});
		register(FieldResolution.class, (provider, menu, editor, workspace, resolution) -> {
			// No items
		});
		register(MethodResolution.class, (provider, menu, editor, workspace, resolution) -> {
			// No items
		});
		register(InstructionResolution.class, (provider, menu, editor, workspace, resolution) -> {
			// TODO:
			//  - Different actions for different instructions (only differentiable by instruction name string atm)
			//  - Convert integer representations (Hex, binary, decimal)
			//  - Goto declaration for type/field/method references
			//  - Goto declaration for jump instructions (with case hint)
		});
		register(VariableDeclarationResolution.class, (provider, menu, editor, workspace, resolution) -> {
			// TODO:
			//  - Goto usage(s)
		});
		register(LabelDeclarationResolution.class, (provider, menu, editor, workspace, resolution) -> {
			// TODO:
			//  - Goto usage(s)
		});
		register(LabelReferenceResolution.class, (provider, menu, editor, workspace, resolution) -> {
			menu.getItems().add(action("menu.goto.label", ARROW_RIGHT, () -> {
				CodeArea area = editor.getCodeArea();
				String target = resolution.labelName().content();
				List<ASTInstruction> instructions = resolution.method().code().instructions();
				for (ASTInstruction instruction : instructions) {
					if (instruction instanceof ASTLabel label && Objects.equals(label.identifier().content(), target)) {
						Range range = Objects.requireNonNull(label.range());
						Location location = Objects.requireNonNull(label.location());
						area.selectRange(range.start(), range.end());
						area.showParagraphAtCenter(location.line());
						break;
					}
				}
			}));
		});
		register(TypeReferenceResolution.class, (provider, menu, editor, workspace, resolution) -> {
			ClassPathNode classPath = workspace.findClass(resolution.typeName().literal());
			ActionMenuItem action = action("menu.goto.class", ARROW_RIGHT, () -> {
				try {
					provider.actions.gotoDeclaration(Objects.requireNonNull(classPath));
				} catch (IncompletePathException ex) {
					logger.error("Cannot go to class due to incomplete path", ex);
				}
			});
			if (classPath == null) action.setDisable(true);
			menu.getItems().add(action);
		});
	}

	/**
	 * @param <R>
	 * 		Resolution impl type.
	 */
	private interface ResolutionMenuFiller<R extends AssemblyResolution> {
		void accept(@Nonnull AbstractContextMenuProviderFactory provider, @Nonnull ContextMenu menu, @Nonnull Editor editor,
					@Nonnull Workspace workspace, @Nonnull R resolution);
	}
}
