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
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.query.AssemblyQueries;
import me.darknet.assembler.query.AssemblyUtils;
import me.darknet.assembler.query.LabelQueryResult;
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
import me.darknet.assembler.util.JvmOpcodes;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import org.fxmisc.richtext.CodeArea;
import org.objectweb.asm.Type;
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
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.JSR;
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
			ResolutionMenuFiller<Resolution> filler = fillers.get(resolution.getClass());
			if (filler != null) filler.accept(this, menu, editor, workspace, resolution);
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
		register(InstructionResolution.class, (provider, menu, editor, workspace, resolution) -> {
			ASTInstruction instruction = resolution.instruction();
			ASTIdentifier identifier = instruction.identifier();
			String instructionName = identifier == null ? null : identifier.content();
			if (instructionName == null)
				return;

			CodeArea area = editor.getCodeArea();
			ASTElement selectedElement = instruction.pick(area.getCaretPosition());
			addIntegerConversionActions(menu, area, selectedElement);
			addMemberReferenceAction(provider, menu, workspace, instructionName, instruction);
			addTypeReferenceAction(provider, menu, workspace, instructionName, instruction);
			addFlowLabelAction(menu, area, resolution.method(), instructionName, instruction);
			addSwitchTargetActions(menu, area, resolution.method(), instructionName, instruction, selectedElement);
		});
		register(VariableDeclarationResolution.class, (provider, menu, editor, workspace, resolution) ->
				addUsageMenu(menu, editor, variableUsageTargets(resolution.method(), resolution.variable().identity().name())));
		register(VariableReferenceResolution.class, (provider, menu, editor, workspace, resolution) ->
				addUsageMenu(menu, editor, variableUsageTargets(resolution.method(), resolution.usage().name())));
		register(LabelDeclarationResolution.class, (provider, menu, editor, workspace, resolution) ->
				addUsageMenu(menu, editor, labelUsageTargets(resolution.method(), resolution.label().name())));
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

	private static void addFlowLabelAction(@Nonnull ContextMenu menu, @Nonnull CodeArea area, @Nonnull ASTMethod method,
	                                       @Nonnull String instructionName, @Nonnull ASTInstruction instruction) {
		int opcode = JvmOpcodes.opcode(instructionName);
		if (opcode < IFEQ || opcode > JSR || instruction.arguments().isEmpty())
			return;

		String labelName = instruction.arguments().getLast().content();
		if (labelName != null) {
			menu.getItems().add(action("menu.goto.label", ARROW_RIGHT, () ->
					gotoLabelDeclaration(method, area, labelName)));
		}
	}

	private static void addTypeReferenceAction(@Nonnull AbstractContextMenuProviderFactory provider, @Nonnull ContextMenu menu,
	                                           @Nonnull Workspace workspace, @Nonnull String instructionName,
	                                           @Nonnull ASTInstruction instruction) {
		if (!AssemblyUtils.isTypeReferenceInstruction(BytecodeFormat.JVM, instructionName))
			return;

		int argumentIndex = AssemblyUtils.typeReferenceArgumentIndex(BytecodeFormat.JVM, instructionName);
		if (argumentIndex < 0 || argumentIndex >= instruction.arguments().size())
			return;

		String typeName = normalizeTypeReference(instruction.arguments().get(argumentIndex).content());
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
	                                             @Nonnull Workspace workspace, @Nonnull String instructionName,
	                                             @Nonnull ASTInstruction instruction) {
		MemberReference memberReference = parseMemberReference(instructionName, instruction);
		if (memberReference == null)
			return;

		ClassPathNode ownerPath = workspace.findClass(memberReference.owner());
		ClassMemberPathNode memberPath = ownerPath == null ? null : ownerPath.child(memberReference.name(), memberReference.descriptor());
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
	                                           @Nonnull String instructionName, @Nonnull ASTInstruction instruction,
	                                           @Nullable ASTElement selectedElement) {
		if (!"lookupswitch".equals(instructionName) && !"tableswitch".equals(instructionName))
			return;

		List<SwitchTarget> targets = parseSwitchTargets(instructionName, instruction);
		if (targets.isEmpty())
			return;

		if (selectedElement != null) {
			for (SwitchTarget target : targets) {
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
		for (SwitchTarget target : targets) {
			switchMenu.getItems().add(actionLiteral(target.description(), ARROW_RIGHT, () ->
					gotoLabelDeclaration(method, area, target.labelName())));
		}
		menu.getItems().add(switchMenu);
	}

	@Nonnull
	private static List<UsageTarget> variableUsageTargets(@Nonnull ASTMethod method, @Nonnull String variableName) {
		VariableQueryResult queryResult = AssemblyQueries.variables(method);
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
	private static List<UsageTarget> labelUsageTargets(@Nonnull ASTMethod method, @Nonnull String labelName) {
		LabelQueryResult queryResult = AssemblyQueries.labels(method);
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

	// TODO: Surely some of this can be pulled up into JASM upstream...

	private static void replaceAstText(@Nonnull CodeArea area, @Nonnull ASTElement element, @Nonnull String replacement) {
		Range range = element.range();
		area.replaceText(range.start(), range.end(), replacement);
	}

	@Nullable
	private static String normalizeTypeReference(@Nullable String typeReference) {
		if (typeReference == null || typeReference.isBlank())
			return null;
		if (typeReference.charAt(0) != '[' && typeReference.charAt(0) != 'L')
			return typeReference;

		Type type = Type.getType(typeReference);
		while (type.getSort() == Type.ARRAY) type = type.getElementType();
		return type.getSort() == Type.OBJECT ? type.getInternalName() : null;
	}

	@Nullable
	private static MemberReference parseMemberReference(@Nonnull String instructionName, @Nonnull ASTInstruction instruction) {
		if ((!isFieldReferenceInstruction(instructionName) && !isMethodReferenceInstruction(instructionName)) ||
				instruction.arguments().size() < 2)
			return null;

		String ownerAndName = instruction.arguments().get(0).content();
		String descriptor = instruction.arguments().get(1).content();
		if (ownerAndName == null || descriptor == null)
			return null;

		int split = ownerAndName.lastIndexOf('.');
		if (split <= 0 || split >= ownerAndName.length() - 1)
			return null;

		return new MemberReference(
				ownerAndName.substring(0, split),
				ownerAndName.substring(split + 1),
				descriptor,
				isMethodReferenceInstruction(instructionName)
		);
	}

	private static boolean isFieldReferenceInstruction(@Nonnull String instructionName) {
		return "getfield".equals(instructionName) || "getstatic".equals(instructionName) ||
				"putfield".equals(instructionName) || "putstatic".equals(instructionName);
	}

	private static boolean isMethodReferenceInstruction(@Nonnull String instructionName) {
		return instructionName.startsWith("invoke") && !"invokedynamic".equals(instructionName);
	}

	@Nonnull
	private static List<SwitchTarget> parseSwitchTargets(@Nonnull String instructionName, @Nonnull ASTInstruction instruction) {
		if (instruction.arguments().isEmpty())
			return List.of();

		ASTObject switchObject = instruction.argumentObject(instruction.arguments().size() - 1);
		if (switchObject == null)
			return List.of();

		return "lookupswitch".equals(instructionName) ? parseLookupSwitchTargets(switchObject) : parseTableSwitchTargets(switchObject);
	}

	@Nonnull
	private static List<SwitchTarget> parseLookupSwitchTargets(@Nonnull ASTObject switchObject) {
		List<SwitchTarget> targets = new ArrayList<>();
		for (int i = 0; i < switchObject.values().size(); i++) {
			ASTIdentifier key = switchObject.values().key(i);
			ASTElement value = switchObject.values().get(i);
			String context = key == null ? null : key.literal();
			String labelName = value == null ? null : value.content();
			if (context == null || labelName == null)
				continue;

			targets.add(new SwitchTarget(
					context,
					labelName,
					"default".equals(context) ? "Default -> " + labelName : "Case " + context + " -> " + labelName,
					key,
					value
			));
		}
		return targets;
	}

	@Nonnull
	private static List<SwitchTarget> parseTableSwitchTargets(@Nonnull ASTObject switchObject) {
		List<SwitchTarget> targets = new ArrayList<>();
		ASTElement defaultValue = switchObject.value("default");
		String defaultLabel = defaultValue == null ? null : defaultValue.content();
		if (defaultLabel != null)
			targets.add(new SwitchTarget("default", defaultLabel, "Default -> " + defaultLabel,
					switchObject.values().key("default"), defaultValue));

		long min = 0;
		ASTElement minValue = switchObject.value("min");
		if (minValue instanceof ASTNumber number)
			min = number.asLong();


		ASTArray cases = switchObject.value("cases");
		if (cases == null)
			return targets;

		long currentValue = min;
		for (ASTElement caseTarget : cases.values()) {
			String labelName = caseTarget.content();
			if (labelName == null || labelName.isBlank())
				continue;

			String caseContext = Long.toString(currentValue++);
			targets.add(new SwitchTarget(caseContext, labelName, "Case " + caseContext + " -> " + labelName,
					caseTarget, caseTarget));
		}
		return targets;
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

	private record UsageTarget(@Nonnull String description, @Nonnull ASTElement element, @Nullable String iconPath) {}

	private record MemberReference(@Nonnull String owner, @Nonnull String name, @Nonnull String descriptor,
	                               boolean isMethod) {}

	private record SwitchTarget(@Nonnull String context, @Nonnull String labelName, @Nonnull String description,
	                            @Nullable ASTElement contextElement, @Nullable ASTElement labelElement) {
		private boolean matches(@Nonnull ASTElement element) {
			return element == contextElement || element == labelElement ||
					context.equals(element.content()) || labelName.equals(element.content());
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
