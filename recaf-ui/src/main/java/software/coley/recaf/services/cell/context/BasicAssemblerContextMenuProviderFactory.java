package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.util.BlwOpcodes;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.AssemblerPathData;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.assembler.resolve.AssemblyResolution;
import software.coley.recaf.util.assembler.resolve.ClassAnnotationResolution;
import software.coley.recaf.util.assembler.resolve.ClassExtends;
import software.coley.recaf.util.assembler.resolve.ClassImplements;
import software.coley.recaf.util.assembler.resolve.FieldAnnotationResolution;
import software.coley.recaf.util.assembler.resolve.FieldResolution;
import software.coley.recaf.util.assembler.resolve.IndependentAnnotationResolution;
import software.coley.recaf.util.assembler.resolve.InnerClassResolution;
import software.coley.recaf.util.assembler.resolve.InstructionResolution;
import software.coley.recaf.util.assembler.resolve.LabelDeclarationResolution;
import software.coley.recaf.util.assembler.resolve.LabelReferenceResolution;
import software.coley.recaf.util.assembler.resolve.MethodAnnotationResolution;
import software.coley.recaf.util.assembler.resolve.MethodResolution;
import software.coley.recaf.util.assembler.resolve.TypeReferenceResolution;
import software.coley.recaf.util.assembler.resolve.VariableDeclarationResolution;
import software.coley.recaf.util.assembler.JasmUtils;
import software.coley.recaf.util.SVG;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.ARROW_RIGHT;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.HEALTH_CROSS;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.LIST_BOXES;
import static org.objectweb.asm.Opcodes.*;
import static software.coley.collections.Unchecked.runnable;
import static software.coley.recaf.util.Menus.action;
import static software.coley.recaf.util.Menus.menu;

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
			ActionMenuItem gotoAction = action("menu.goto.class", ARROW_RIGHT, () -> {
				try {
					provider.actions.gotoDeclaration(Objects.requireNonNull(classPath));
				} catch (IncompletePathException ex) {
					logger.error("Cannot go to class due to incomplete path", ex);
				}
			});
			ActionMenuItem overrideAction = action("menu.edit.override.method", HEALTH_CROSS, () -> {
				if (classPath == null)
					return;
				WorkspaceResource resource = classPath.getValueOfType(WorkspaceResource.class);
				JvmClassBundle bundle = classPath.getValueOfType(JvmClassBundle.class);
				JvmClassInfo cls = classPath.getValue().asJvmClass();
				provider.actions.overrideClassMethod(workspace, resource, bundle, cls);
			});
			if (classPath == null) {
				gotoAction.setDisable(true);
				overrideAction.setDisable(true);
			}
			menu.getItems().addAll(gotoAction, overrideAction);
		});
		register(ClassExtends.class, (provider, menu, editor, workspace, resolution) -> {
			ClassPathNode classPath = workspace.findClass(resolution.superName().literal());
			ActionMenuItem gotoAction = action("menu.goto.class", ARROW_RIGHT, () -> {
				try {
					provider.actions.gotoDeclaration(Objects.requireNonNull(classPath));
				} catch (IncompletePathException ex) {
					logger.error("Cannot go to class due to incomplete path", ex);
				}
			});
			ActionMenuItem overrideAction = action("menu.edit.override.method", HEALTH_CROSS, () -> {
				if (classPath == null)
					return;
				WorkspaceResource resource = classPath.getValueOfType(WorkspaceResource.class);
				JvmClassBundle bundle = classPath.getValueOfType(JvmClassBundle.class);
				JvmClassInfo cls = classPath.getValue().asJvmClass();
				provider.actions.overrideClassMethod(workspace, resource, bundle, cls);
			});
			if (classPath == null) {
				gotoAction.setDisable(true);
				overrideAction.setDisable(true);
			}
			menu.getItems().addAll(gotoAction, overrideAction);
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
			ASTInstruction instruction = resolution.instruction();
			ASTIdentifier identifier = instruction.identifier();
			if (identifier != null && identifier.content() != null) {
				int opcode = BlwOpcodes.opcode(identifier.content());
				if (opcode >= IFEQ && opcode <= JSR && !instruction.arguments().isEmpty()) {
					String labelName = instruction.arguments().getLast().content();
					if (labelName != null)
						menu.getItems().add(action("menu.goto.label", ARROW_RIGHT, () -> {
							CodeArea area = editor.getCodeArea();
							gotoLabelDeclaration(resolution.method(), editor.getCodeArea(), labelName);
						}));
				} else if (opcode == NEW || opcode == CHECKCAST || opcode == INSTANCEOF) {
					if (instruction.arguments().getLast() instanceof ASTIdentifier typeIdentifier) {
						String typeName = typeIdentifier.content();
						if (typeName != null) {
							ClassPathNode typePath = workspace.findClass(typeName);
							if (typePath != null) {
								// TODO: We want to show the type that is targeted with this, similar to how we have in
								//  other context menus. This code is also messy and there's probably some abstractions
								//  we can make to reduce the indentation hell.
								menu.getItems().add(action("menu.goto.class", ARROW_RIGHT, runnable(() -> provider.actions.gotoDeclaration(typePath))));
							}
						}
					}
				}
			}
		});
		register(VariableDeclarationResolution.class, (provider, menu, editor, workspace, resolution) -> {
			List<UsageTarget> usages = JasmUtils.collectVariableReferences(resolution.method(), resolution.variableName().literal()).stream()
					.map(reference -> new UsageTarget(
							formatUsageDescription(reference.element(), switch (reference.kind()) {
								case READ -> "read";
								case WRITE -> "write";
								case INCREMENT -> "increment";
							}),
							reference.element(),
							switch (reference.kind()) {
								case READ -> SVG.REF_READ;
								case WRITE, INCREMENT -> SVG.REF_WRITE;
							}
					))
					.toList();
			addUsageMenu(menu, editor, usages);
		});
		register(LabelDeclarationResolution.class, (provider, menu, editor, workspace, resolution) -> {
			List<UsageTarget> usages = JasmUtils.collectLabelReferences(resolution.method(), resolution.label().identifier().literal()).stream()
					.map(reference -> new UsageTarget(
							formatUsageDescription(reference.element(), switch (reference.kind()) {
								case FLOW -> Objects.requireNonNullElse(reference.context(), "flow");
								case SWITCH_DEFAULT -> "default";
								case SWITCH_CASE -> "case " + reference.context();
								case TRY_START -> "try start";
								case TRY_END -> "try end";
								case HANDLER -> "handler";
							}),
							reference.element(),
							null
					))
					.toList();
			addUsageMenu(menu, editor, usages);
		});
		register(LabelReferenceResolution.class, (provider, menu, editor, workspace, resolution) -> {
			String target = resolution.labelName().content();
			if (target != null)
				menu.getItems().add(action("menu.goto.label", ARROW_RIGHT, () -> {
					CodeArea area = editor.getCodeArea();
					gotoLabelDeclaration(resolution.method(), area, target);
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

	private static void addUsageMenu(@Nonnull ContextMenu menu, @Nonnull Editor editor, @Nonnull List<UsageTarget> usages) {
		if (usages.isEmpty())
			return;

		Menu usageMenu = menu("assembler.variables.usage", LIST_BOXES);
		CodeArea area = editor.getCodeArea();
		for (UsageTarget usage : usages) {
			usageMenu.getItems().add(new ActionMenuItem(
					usage.description(),
					usage.iconPath() == null ? new FontIconView(ARROW_RIGHT) : SVG.ofIconFile(usage.iconPath()),
					() -> gotoAstElement(area, usage.element())
			));
		}
		menu.getItems().add(usageMenu);
	}

	@Nonnull
	private static String formatUsageDescription(@Nonnull ASTElement element, @Nonnull String context) {
		Location location = element.location();
		if (location == null)
			return context;
		return "Line %d (%s)".formatted(location.line(), context);
	}

	private static void gotoAstElement(@Nonnull CodeArea area, @Nonnull ASTElement element) {
		Range range = element.range();
		Location location = element.location();
		if (location == null)
			return;
		area.selectRange(range.start(), range.end());
		area.showParagraphAtCenter(location.line());
	}

	private static void gotoLabelDeclaration(@Nonnull ASTMethod method, @Nonnull CodeArea area, @Nonnull String target) {
		ASTLabel label = JasmUtils.getLabelDeclaration(method, target);
		if (label != null)
			gotoAstElement(area, label);
	}

	private record UsageTarget(@Nonnull String description, @Nonnull ASTElement element, @Nullable String iconPath) {}

	/**
	 * @param <R>
	 * 		Resolution impl type.
	 */
	private interface ResolutionMenuFiller<R extends AssemblyResolution> {
		void accept(@Nonnull AbstractContextMenuProviderFactory provider, @Nonnull ContextMenu menu, @Nonnull Editor editor,
		            @Nonnull Workspace workspace, @Nonnull R resolution);
	}
}
