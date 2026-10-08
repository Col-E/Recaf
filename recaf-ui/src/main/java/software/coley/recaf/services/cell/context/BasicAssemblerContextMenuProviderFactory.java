package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.query.AssemblyQueries;
import me.darknet.assembler.query.AssemblyUtils;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.target.TargetContext;
import me.darknet.assembler.descriptor.Descriptors;
import me.darknet.assembler.query.LabelQueryResult;
import me.darknet.assembler.query.MemberReference;
import me.darknet.assembler.query.SwitchTarget;
import me.darknet.assembler.query.VariableQueryResult;
import me.darknet.assembler.query.resolution.ClassAnnotationResolution;
import me.darknet.assembler.query.resolution.ClassExtends;
import me.darknet.assembler.query.resolution.ClassImplements;
import me.darknet.assembler.query.resolution.FieldAnnotationResolution;
import me.darknet.assembler.query.resolution.FieldResolution;
import me.darknet.assembler.query.resolution.IndependentAnnotationResolution;
import me.darknet.assembler.query.resolution.InnerClassResolution;
import me.darknet.assembler.query.resolution.InstructionResolution;
import me.darknet.assembler.query.resolution.LabelDeclarationResolution;
import me.darknet.assembler.query.resolution.LabelReferenceResolution;
import me.darknet.assembler.query.resolution.MethodAnnotationResolution;
import me.darknet.assembler.query.resolution.MethodResolution;
import me.darknet.assembler.query.resolution.Resolution;
import me.darknet.assembler.query.resolution.TypeReferenceResolution;
import me.darknet.assembler.query.resolution.VariableDeclarationResolution;
import me.darknet.assembler.query.resolution.VariableReferenceResolution;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.AssemblerPathData;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.SVG;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.ARROW_RIGHT;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.HEALTH_CROSS;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.LIST_BOXES;
import static software.coley.recaf.util.Menus.*;

/**
 * Basic implementation for {@link AssemblerContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicAssemblerContextMenuProviderFactory extends AbstractContextMenuProviderFactory implements AssemblerContextMenuProviderFactory {
	private static final Logger logger = Logging.get(BasicAssemblerContextMenuProviderFactory.class);
	private static final Map<Class<? extends Resolution>, ResolutionMenuFiller<Resolution>> fillers = new IdentityHashMap<>();

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
			Resolution resolution = assemblerData.resolution();
			Editor editor = assemblerData.editor();
			TargetContext targetContext = declaringClass.isAndroidClass()
					? DalvikTargetContext.INSTANCE
					: JvmTargetContext.INSTANCE;
			if (resolution instanceof InstructionResolution instructionResolution) {
				addInstructionContextMenu(this, menu, editor, workspace, targetContext, instructionResolution);
			} else if (resolution instanceof VariableDeclarationResolution declaration) {
				addUsageMenu(menu, editor, variableUsageTargets(declaration.method(), targetContext, declaration.variable().identity().name()));
			} else if (resolution instanceof VariableReferenceResolution reference) {
				addUsageMenu(menu, editor, variableUsageTargets(reference.method(), targetContext, reference.usage().name()));
			} else if (resolution instanceof LabelDeclarationResolution declaration) {
				addUsageMenu(menu, editor, labelUsageTargets(declaration.method(), targetContext, declaration.label().name()));
			} else {
				ResolutionMenuFiller<Resolution> filler = fillers.get(resolution.getClass());
				if (filler != null) filler.accept(this, menu, editor, workspace, resolution);
			}
			if (menu.getItems().isEmpty()) return null;
			return menu;
		};
	}

	/**
	 * Helper to register {@link ResolutionMenuFiller} instances.
	 *
	 * @param type
	 * 		Subtype of {@link Resolution}.
	 * @param filler
	 * 		Filler for the given subtype.
	 * @param <T>
	 * 		Subtype of {@link Resolution}.
	 */
	private static <T extends Resolution> void register(@Nonnull Class<T> type, @Nonnull ResolutionMenuFiller<T> filler) {
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
		register(LabelReferenceResolution.class, (provider, menu, editor, workspace, resolution) -> {
			String target = resolution.reference().content();
			if (target != null)
				menu.getItems().add(action("menu.goto.label", ARROW_RIGHT, () ->
						gotoLabelDeclaration(resolution.method(), editor.getCodeArea(), target)));
		});
		register(TypeReferenceResolution.class, (provider, menu, editor, workspace, resolution) -> {
			ClassPathNode classPath = workspace.findClass(resolution.type().literal());
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

	private static void addInstructionContextMenu(@Nonnull AbstractContextMenuProviderFactory provider,
	                                              @Nonnull ContextMenu menu,
	                                              @Nonnull Editor editor,
	                                              @Nonnull Workspace workspace,
	                                              @Nonnull TargetContext targetContext,
	                                              @Nonnull InstructionResolution resolution) {
		ASTInstruction instruction = resolution.instruction();
		ASTIdentifier identifier = instruction.identifier();
		String instructionName = identifier == null ? null : identifier.content();
		if (instructionName == null)
			return;

		CodeArea area = editor.getCodeArea();
		ASTElement selectedElement = instruction.pick(area.getCaretPosition());
		addIntegerConversionActions(menu, area, selectedElement);
		addMemberReferenceAction(provider, menu, workspace, targetContext, instruction);
		addTypeReferenceAction(provider, menu, workspace, targetContext, instructionName, instruction);
		addFlowLabelAction(menu, area, resolution.method(), targetContext, instructionName, instruction);
		addSwitchTargetActions(menu, area, resolution.method(), targetContext, instruction, selectedElement);
	}

	private static void addFlowLabelAction(@Nonnull ContextMenu menu, @Nonnull CodeArea area, @Nonnull ASTMethod method,
	                                       @Nonnull TargetContext targetContext, @Nonnull String instructionName,
	                                       @Nonnull ASTInstruction instruction) {
		if (!AssemblyUtils.isBranchInstruction(targetContext, instructionName) || instruction.arguments().isEmpty())
			return;

		String labelName = instruction.arguments().getLast().content();
		if (labelName != null) {
			menu.getItems().add(action("menu.goto.label", ARROW_RIGHT, () ->
					gotoLabelDeclaration(method, area, labelName)));
		}
	}

	private static void addTypeReferenceAction(@Nonnull AbstractContextMenuProviderFactory provider, @Nonnull ContextMenu menu,
	                                           @Nonnull Workspace workspace, @Nonnull TargetContext targetContext,
	                                           @Nonnull String instructionName, @Nonnull ASTInstruction instruction) {
		if (!AssemblyUtils.isTypeReferenceInstruction(targetContext, instructionName))
			return;

		Instruction<?> definition = targetContext.instructions().get(instructionName);
		if (definition == null)
			return;
		int argumentIndex = definition.typeReferenceOperandIndex();
		if (argumentIndex < 0 || argumentIndex >= instruction.arguments().size())
			return;

		String typeName = Descriptors.internalName(instruction.arguments().get(argumentIndex).content());
		if (typeName == null)
			return;

		ClassPathNode typePath = workspace.findClass(typeName);
		ActionMenuItem gotoAction = action("menu.goto.class", ARROW_RIGHT, () -> {
			try {
				provider.actions.gotoDeclaration(Objects.requireNonNull(typePath));
			} catch (IncompletePathException ex) {
				logger.error("Cannot go to class due to incomplete path", ex);
			}
		});
		if (typePath == null) gotoAction.setDisable(true);
		menu.getItems().add(gotoAction);
	}

	private static void addMemberReferenceAction(@Nonnull AbstractContextMenuProviderFactory provider, @Nonnull ContextMenu menu,
	                                             @Nonnull Workspace workspace, @Nonnull TargetContext targetContext,
	                                             @Nonnull ASTInstruction instruction) {
		// TODO: Fix "Go to method" for invoke-polymorphic.
		if (instruction.identifier() != null && instruction.identifier().content().startsWith("invoke-polymorphic"))
			return;

		MemberReference memberReference = AssemblyUtils.resolveMemberReference(targetContext, instruction);
		if (memberReference == null)
			return;

		ClassMemberPathNode memberPath = resolveMemberPath(workspace, memberReference);
		String key = memberReference.isMethod() ? "menu.goto.method" : "menu.goto.field";
		ActionMenuItem gotoAction = action(key, ARROW_RIGHT, () -> {
			try {
				provider.actions.gotoDeclaration(Objects.requireNonNull(memberPath));
			} catch (IncompletePathException ex) {
				logger.error("Cannot go to member due to incomplete path", ex);
			}
		});
		if (memberPath == null) gotoAction.setDisable(true);
		menu.getItems().add(gotoAction);
	}

	private static void addIntegerConversionActions(@Nonnull ContextMenu menu, @Nonnull CodeArea area,
	                                                @Nullable ASTElement selectedElement) {
		if (!(selectedElement instanceof ASTNumber number) || number.isFloatingPoint())
			return;

		String content = number.content();
		Number value = number.number();
		if (content == null || value == null)
			return;

		long longValue = value.longValue();
		boolean wide = content.endsWith("L") || content.endsWith("l") || value instanceof Long;
		int currentRadix = integerLiteralRadix(content);
		if (currentRadix != 10)
			menu.getItems().add(actionLiteral("Convert to decimal (%s)".formatted(formatIntegerLiteral(longValue, wide, 10)),
					ARROW_RIGHT, () -> replaceAstText(area, number, formatIntegerLiteral(longValue, wide, 10))));
		if (currentRadix != 16)
			menu.getItems().add(actionLiteral("Convert to hex (%s)".formatted(formatIntegerLiteral(longValue, wide, 16)),
					ARROW_RIGHT, () -> replaceAstText(area, number, formatIntegerLiteral(longValue, wide, 16))));
		if (currentRadix != 2)
			menu.getItems().add(actionLiteral("Convert to binary (%s)".formatted(formatIntegerLiteral(longValue, wide, 2)),
					ARROW_RIGHT, () -> replaceAstText(area, number, formatIntegerLiteral(longValue, wide, 2))));
	}

	private static void addSwitchTargetActions(@Nonnull ContextMenu menu, @Nonnull CodeArea area, @Nonnull ASTMethod method,
	                                           @Nonnull TargetContext targetContext, @Nonnull ASTInstruction instruction,
	                                           @Nullable ASTElement selectedElement) {
		List<MenuSwitchTarget> targets = AssemblyUtils.resolveSwitchTargets(targetContext, instruction).stream()
				.map(target -> new MenuSwitchTarget(target, describeSwitchTarget(target)))
				.toList();
		if (targets.isEmpty())
			return;

		if (selectedElement != null) {
			for (MenuSwitchTarget target : targets) {
				if (target.matches(selectedElement)) {
					menu.getItems().add(action("menu.goto.label", ARROW_RIGHT, () ->
							gotoLabelDeclaration(method, area, target.labelName())));
					break;
				}
			}
		}

		Menu switchMenu = new Menu();
		switchMenu.textProperty().bind(Lang.getBinding("menu.goto.switch.title"));
		switchMenu.setGraphic(new FontIconView(ARROW_RIGHT));
		for (MenuSwitchTarget target : targets) {
			switchMenu.getItems().add(actionLiteral(target.description(), ARROW_RIGHT, () ->
					gotoLabelDeclaration(method, area, target.labelName())));
		}
		menu.getItems().add(switchMenu);
	}

	@Nonnull
	private static List<UsageTarget> variableUsageTargets(@Nonnull ASTMethod method, @Nonnull TargetContext targetContext,
	                                                      @Nonnull String variableName) {
		VariableQueryResult queryResult = AssemblyQueries.variables(method, targetContext);
		return queryResult.usagesOf(variableName).stream()
				.sorted(Comparator.comparing(usage -> usage.reference().location()))
				.map(usage -> new UsageTarget(
						formatUsageDescription(usage.reference(), switch (usage.kind()) {
							case READ -> "read";
							case WRITE -> "write";
							case INCREMENT -> "increment";
						}),
						usage.reference(),
						switch (usage.kind()) {
							case READ -> SVG.REF_READ;
							case WRITE, INCREMENT -> SVG.REF_WRITE;
						}
				))
				.toList();
	}

	@Nonnull
	private static List<UsageTarget> labelUsageTargets(@Nonnull ASTMethod method, @Nonnull TargetContext targetContext,
	                                                   @Nonnull String labelName) {
		LabelQueryResult queryResult = AssemblyQueries.labels(method, targetContext);
		return queryResult.usagesOf(labelName).stream()
				.sorted(Comparator.comparing(usage -> usage.reference().location()))
				.map(usage -> new UsageTarget(
						formatUsageDescription(usage.reference(), switch (usage.kind()) {
							case FLOW -> Objects.requireNonNullElse(usage.context(), "flow");
							case SWITCH_DEFAULT -> "default";
							case SWITCH_CASE -> "case " + usage.context();
							case TRY_START -> "try start";
							case TRY_END -> "try end";
							case HANDLER -> "handler";
						}),
						usage.reference(),
						null
				))
				.toList();
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
		ASTLabel label = AssemblyUtils.findLabelDeclaration(method, target);
		if (label != null)
			gotoAstElement(area, label);
	}

	private static void replaceAstText(@Nonnull CodeArea area, @Nonnull ASTElement element, @Nonnull String replacement) {
		Range range = element.range();
		area.replaceText(range.start(), range.end(), replacement);
	}

	@Nullable
	static ClassMemberPathNode resolveMemberPath(@Nonnull Workspace workspace, @Nonnull MemberReference reference) {
		ClassPathNode ownerPath = workspace.findClass(reference.owner());
		return ownerPath == null ? null : ownerPath.child(reference.name(), reference.descriptor());
	}

	@Nonnull
	private static String describeSwitchTarget(@Nonnull SwitchTarget target) {
		return "default".equals(target.context())
				? "Default -> " + target.labelName()
				: "Case " + target.context() + " -> " + target.labelName();
	}

	private static int integerLiteralRadix(@Nonnull String content) {
		int offset = content.startsWith("-") || content.startsWith("+") ? 1 : 0;
		if (content.regionMatches(true, offset, "0x", 0, 2))
			return 16;
		if (content.regionMatches(true, offset, "0b", 0, 2))
			return 2;
		return 10;
	}

	@Nonnull
	private static String formatIntegerLiteral(long value, boolean wide, int radix) {
		BigInteger magnitude = BigInteger.valueOf(value).abs();
		String sign = value < 0 ? "-" : "";
		String text = switch (radix) {
			case 16 -> sign + "0x" + magnitude.toString(16).toUpperCase(Locale.ROOT);
			case 2 -> sign + "0b" + magnitude.toString(2);
			default -> Long.toString(value);
		};
		return wide ? text + 'L' : text;
	}

	/**
	 * A usage of a variable or label.
	 *
	 * @param description
	 * 		Text to show in the menu for the usage.
	 * @param element
	 * 		Element that is the usage.
	 * @param iconPath
	 * 		Optional icon path for the usage type.
	 */
	private record UsageTarget(@Nonnull String description, @Nonnull ASTElement element, @Nullable String iconPath) {}

	/**
	 * A resolved switch branch paired with the text its menu item shows.
	 *
	 * @param target
	 * 		Branch resolved by JASM.
	 * @param description
	 * 		Menu text for the branch.
	 */
	private record MenuSwitchTarget(@Nonnull SwitchTarget target, @Nonnull String description) {
		@Nonnull
		String labelName() {
			return target.labelName();
		}

		@Nonnull
		String context() {
			return target.context();
		}

		private boolean matches(@Nonnull ASTElement element) {
			return element == target.contextElement() || element == target.labelElement() ||
					context().equals(element.content()) || labelName().equals(element.content());
		}
	}

	/**
	 * @param <R>
	 * 		Resolution impl type.
	 */
	private interface ResolutionMenuFiller<R extends Resolution> {
		void accept(@Nonnull AbstractContextMenuProviderFactory provider, @Nonnull ContextMenu menu, @Nonnull Editor editor,
		            @Nonnull Workspace workspace, @Nonnull R resolution);
	}
}
