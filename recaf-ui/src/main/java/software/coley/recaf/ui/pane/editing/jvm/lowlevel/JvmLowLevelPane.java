package software.coley.recaf.ui.pane.editing.jvm.lowlevel;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.cafedude.InvalidClassException;
import software.coley.cafedude.classfile.ClassFile;
import software.coley.cafedude.classfile.Field;
import software.coley.cafedude.classfile.Method;
import software.coley.cafedude.classfile.annotation.Annotation;
import software.coley.cafedude.classfile.annotation.AnnotationElementValue;
import software.coley.cafedude.classfile.annotation.ArrayElementValue;
import software.coley.cafedude.classfile.annotation.ClassElementValue;
import software.coley.cafedude.classfile.annotation.ElementValue;
import software.coley.cafedude.classfile.annotation.EnumElementValue;
import software.coley.cafedude.classfile.annotation.PrimitiveElementValue;
import software.coley.cafedude.classfile.annotation.Utf8ElementValue;
import software.coley.cafedude.classfile.attribute.AnnotationDefaultAttribute;
import software.coley.cafedude.classfile.attribute.AnnotationsAttribute;
import software.coley.cafedude.classfile.attribute.Attribute;
import software.coley.cafedude.classfile.attribute.BootstrapMethodsAttribute;
import software.coley.cafedude.classfile.attribute.CharacterRangeTableAttribute;
import software.coley.cafedude.classfile.attribute.CodeAttribute;
import software.coley.cafedude.classfile.attribute.CompilationIdAttribute;
import software.coley.cafedude.classfile.attribute.ConstantValueAttribute;
import software.coley.cafedude.classfile.attribute.DefaultAttribute;
import software.coley.cafedude.classfile.attribute.DeprecatedAttribute;
import software.coley.cafedude.classfile.attribute.EnclosingMethodAttribute;
import software.coley.cafedude.classfile.attribute.ExceptionsAttribute;
import software.coley.cafedude.classfile.attribute.InnerClassesAttribute;
import software.coley.cafedude.classfile.attribute.LineNumberTableAttribute;
import software.coley.cafedude.classfile.attribute.LocalVariableTableAttribute;
import software.coley.cafedude.classfile.attribute.LocalVariableTypeTableAttribute;
import software.coley.cafedude.classfile.attribute.MethodParametersAttribute;
import software.coley.cafedude.classfile.attribute.ModuleAttribute;
import software.coley.cafedude.classfile.attribute.ModuleHashesAttribute;
import software.coley.cafedude.classfile.attribute.ModuleMainClassAttribute;
import software.coley.cafedude.classfile.attribute.ModulePackagesAttribute;
import software.coley.cafedude.classfile.attribute.ModuleResolutionAttribute;
import software.coley.cafedude.classfile.attribute.ModuleTargetAttribute;
import software.coley.cafedude.classfile.attribute.NestHostAttribute;
import software.coley.cafedude.classfile.attribute.NestMembersAttribute;
import software.coley.cafedude.classfile.attribute.ParameterAnnotationsAttribute;
import software.coley.cafedude.classfile.attribute.PermittedClassesAttribute;
import software.coley.cafedude.classfile.attribute.RecordAttribute;
import software.coley.cafedude.classfile.attribute.SignatureAttribute;
import software.coley.cafedude.classfile.attribute.SourceDebugExtensionAttribute;
import software.coley.cafedude.classfile.attribute.SourceFileAttribute;
import software.coley.cafedude.classfile.attribute.SourceIdAttribute;
import software.coley.cafedude.classfile.attribute.StackMapTableAttribute;
import software.coley.cafedude.classfile.attribute.SyntheticAttribute;
import software.coley.cafedude.classfile.behavior.AttributeHolder;
import software.coley.cafedude.classfile.constant.ConstDynamic;
import software.coley.cafedude.classfile.constant.ConstRef;
import software.coley.cafedude.classfile.constant.CpClass;
import software.coley.cafedude.classfile.constant.CpDouble;
import software.coley.cafedude.classfile.constant.CpEntry;
import software.coley.cafedude.classfile.constant.CpFloat;
import software.coley.cafedude.classfile.constant.CpInt;
import software.coley.cafedude.classfile.constant.CpInternal;
import software.coley.cafedude.classfile.constant.CpLong;
import software.coley.cafedude.classfile.constant.CpMethodHandle;
import software.coley.cafedude.classfile.constant.CpMethodType;
import software.coley.cafedude.classfile.constant.CpModule;
import software.coley.cafedude.classfile.constant.CpNameType;
import software.coley.cafedude.classfile.constant.CpPackage;
import software.coley.cafedude.classfile.constant.CpString;
import software.coley.cafedude.classfile.constant.CpUtf8;
import software.coley.cafedude.classfile.instruction.BasicInstruction;
import software.coley.cafedude.classfile.instruction.CpRefInstruction;
import software.coley.cafedude.classfile.instruction.IincInstruction;
import software.coley.cafedude.classfile.instruction.Instruction;
import software.coley.cafedude.classfile.instruction.IntOperandInstruction;
import software.coley.cafedude.classfile.instruction.LookupSwitchInstruction;
import software.coley.cafedude.classfile.instruction.MultiANewArrayInstruction;
import software.coley.cafedude.classfile.instruction.Opcodes;
import software.coley.cafedude.classfile.instruction.TableSwitchInstruction;
import software.coley.cafedude.classfile.instruction.WideInstruction;
import software.coley.cafedude.io.ClassFileReader;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.ClassNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.config.MemberDisplayFormatConfig;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.tree.TreeItems;
import software.coley.recaf.ui.pane.editing.binary.hex.HexUtil;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.SVG;
import software.coley.recaf.util.Types;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Function;

import static software.coley.cafedude.classfile.constant.CpMethodHandle.*;
import static software.coley.recaf.util.AccessFlag.sortAndToString;

/**
 * Displays a {@link JvmClassInfo} as a {@link TreeView} that closely aligns to the class file specification.
 *
 * @author Matt Coley
 */
@Dependent
public class JvmLowLevelPane extends BorderPane implements ClassNavigable, UpdatableNavigable {
	private static final Logger logger = Logging.get(JvmLowLevelPane.class);
	private final MemberDisplayFormatConfig memberDisplay;
	private final CellConfigurationService configurationService;
	private final Workspace workspace;
	private ClassPathNode path;

	@Inject
	public JvmLowLevelPane(@Nonnull WorkspaceManager workspaceManager,
	                       @Nonnull MemberDisplayFormatConfig memberDisplay,
	                       @Nonnull CellConfigurationService configurationService) {
		this.workspace = workspaceManager.getCurrent();
		this.memberDisplay = memberDisplay;
		this.configurationService = configurationService;
	}

	private void updateDisplay(@Nonnull JvmClassInfo info) throws InvalidClassException {
		ClassFileReader reader = new ClassFileReader();
		ClassFile klass = reader.read(info.getBytecode());

		// Build tree
		ClassItem root = buildRoot(klass);
		TreeView<ClassElement> tree = new TreeView<>();
		tree.setCellFactory(_ -> new TreeCell<>() {
			@Override
			protected void updateItem(ClassElement item, boolean empty) {
				super.updateItem(item, empty);

				if (empty || item == null) {
					setText(null);
					setGraphic(null);
					setOnContextMenuRequested(null);
				} else {
					item.configureDisplay(this);
				}
			}
		});
		tree.getStyleClass().addAll(Tweaks.EDGE_TO_EDGE, Styles.DENSE);
		tree.setShowRoot(false);
		tree.setRoot(root);

		// TODO: Add search bar (like workspace filter pane) (prefix + text)
		setCenter(tree);
	}

	@Nonnull
	private ClassItem buildRoot(@Nonnull ClassFile klass) {
		ClassItem root = item("Root", klass,
				i -> "",
				i -> null,
				i -> null);
		root.item("Major version", klass,
				i -> String.valueOf(i.getVersionMajor()),
				i -> new FontIconView(CarbonIcons.NUMBER_0),
				i -> null);
		root.item("Minor version", klass,
				i -> String.valueOf(i.getVersionMinor()),
				i -> new FontIconView(CarbonIcons.NUMBER_SMALL_0),
				i -> null);
		root.item("Access", klass,
				i -> sortAndToString(AccessFlag.Type.CLASS, i.getAccess()),
				i -> Icons.getVisibilityIcon(i.getAccess()),
				i -> null);
		root.item("This", klass,
				i -> i.getName(),
				i -> classGraphic(i.getName()),
				i -> null);
		root.item("Super", klass,
				i -> i.getSuperName(),
				i -> classGraphic(Objects.requireNonNullElse(i.getSuperName(), "java/lang/Object")),
				i -> null);
		ClassItem interfaces = root.item("Interfaces", klass.getInterfaceClasses(),
				i -> "[" + i.size() + "]",
				i -> Icons.getIconView(Icons.ARRAY),
				i -> null
		);
		for (CpClass interfaceClass : klass.getInterfaceClasses())
			interfaces.item("Interface", interfaceClass,
					i -> i.getName().getText(),
					i -> classGraphic(i),
					i -> null);
		ClassItem pool = root.item("Constant pool", klass.getPool(),
				i -> "[" + i.size() + "]",
				i -> Icons.getIconView(Icons.ARRAY),
				i -> null
		);
		for (CpEntry entry : klass.getPool())
			pool.item("[" + entry.getIndex() + "] " + entry.getClass().getSimpleName(), entry,
					i -> cpToString(i),
					i -> cpToGraphic(i),
					i -> null);
		ClassItem fields = root.item("Fields", klass.getFields(),
				i -> "[" + i.size() + "]",
				i -> memberGraphic(null, false),
				i -> null
		);
		for (Field field : klass.getFields()) {
			ClassItem fieldItem = fields.item("Field", field,
					i -> {
						String access = sortAndToString(AccessFlag.Type.FIELD, i.getAccess());
						if (!access.isEmpty())
							access += ' ';
						String fieldName = i.getName().getText();
						String fieldType = i.getType().getText();
						String nameType = memberDisplay.getFieldDisplay(fieldName, fieldType);
						return access + nameType;
					},
					i -> {
						String fieldName = i.getName().getText();
						String fieldType = i.getType().getText();
						return memberGraphic(path.child(fieldName, fieldType), false);
					},
					i -> null
			);
			fieldItem.item("Access", field,
					i -> sortAndToString(AccessFlag.Type.FIELD, i.getAccess()),
					i -> Icons.getVisibilityIcon(i.getAccess()),
					i -> null);
			addAttributes(fieldItem, field);
		}
		ClassItem methods = root.item("Methods", klass.getMethods(),
				i -> "[" + i.size() + "]",
				i -> memberGraphic(null, true),
				i -> null
		);
		for (Method method : klass.getMethods()) {
			ClassItem methodItem = methods.item("Method", method,
					i -> {
						String access = sortAndToString(AccessFlag.Type.METHOD, i.getAccess());
						if (!access.isEmpty())
							access += ' ';
						String fieldName = i.getName().getText();
						String fieldType = i.getType().getText();
						String nameType = memberDisplay.getFieldDisplay(fieldName, fieldType);
						return access + nameType;
					},
					i -> {
						String fieldName = i.getName().getText();
						String fieldType = i.getType().getText();
						return memberGraphic(path.child(fieldName, fieldType), true);
					},
					i -> null
			);
			methodItem.item("Access", method,
					i -> sortAndToString(AccessFlag.Type.METHOD, i.getAccess()),
					i -> Icons.getVisibilityIcon(i.getAccess()),
					i -> null);
			addAttributes(methodItem, method);
		}
		addAttributes(root, klass);
		return root;
	}

	@Nonnull
	private ClassItem addAttributes(@Nonnull ClassItem parent, @Nonnull AttributeHolder holder) {
		ClassItem child = parent.item("Attributes", holder,
				i -> "[" + holder.getAttributes().size() + "]",
				i -> Icons.getIconView(Icons.ARRAY),
				i -> null);
		for (Attribute attribute : holder.getAttributes())
			addAttribute(child, holder, attribute);
		return child;
	}

	@Nonnull
	private ClassItem addAttribute(@Nonnull ClassItem parent, @Nonnull AttributeHolder holder, @Nonnull Attribute attribute) {
		ClassItem child = parent.item(attribute.getName().getText(), attribute,
				i -> switch (i) {
					case AnnotationDefaultAttribute attr -> elementValueToString(attr.getElementValue());
					case AnnotationsAttribute attr -> "[" + attr.getAnnotations().size() + "]";
					case BootstrapMethodsAttribute attr -> "[" + attr.getBootstrapMethods().size() + "]";
					case CharacterRangeTableAttribute attr -> "[" + attr.getCharacterRangeTable().size() + "]";
					case CodeAttribute attr -> "maxLocals=" + attr.getMaxLocals() + ", maxStack=" + attr.getMaxStack()
							+ ", instructions[" + attr.getInstructions().size() + "]";
					case CompilationIdAttribute attr -> attr.getCompilationId().getText();
					case ConstantValueAttribute attr -> cpToString(attr.getConstantValue());
					case DefaultAttribute attr -> {
						byte[] slice = Arrays.copyOf(attr.getData(), Math.min(16, attr.getData().length));
						StringBuilder sb = new StringBuilder();
						for (byte b : slice)
							sb.append(HexUtil.strFormat00(b)).append(' ');
						yield sb.toString().trim();
					}
					case DeprecatedAttribute attr -> "";
					case EnclosingMethodAttribute attr -> {
						CpClass classEntry = attr.getClassEntry();
						CpNameType methodEntry = attr.getMethodEntry();
						String owner = classEntry.getName().getText();
						if (methodEntry != null) {
							String methodName = methodEntry.getName().getText();
							String methodType = methodEntry.getType().getText();
							yield owner + "." + memberDisplay.getMethodDisplay(methodName, methodType);
						}
						yield owner;
					}
					case ExceptionsAttribute attr -> "[" + attr.getExceptionTable().size() + "]";
					case InnerClassesAttribute attr -> "[" + attr.getInnerClasses().size() + "]";
					case LineNumberTableAttribute attr -> "[" + attr.getEntries().size() + "]";
					case LocalVariableTableAttribute attr -> "[" + attr.getEntries().size() + "]";
					case LocalVariableTypeTableAttribute attr -> "[" + attr.getEntries().size() + "]";
					case MethodParametersAttribute attr -> "[" + attr.getParameters().size() + "]";
					case ModuleAttribute attr -> attr.getModule().getName().getText();
					case ModuleHashesAttribute attr -> attr.getAlgorithmName().getText();
					case ModuleMainClassAttribute attr -> attr.getMainClass().getName().getText();
					case ModulePackagesAttribute attr -> "[" + attr.getPackages().size() + "]";
					case ModuleResolutionAttribute attr -> "mask=" + Integer.toBinaryString(attr.getFlags());
					case ModuleTargetAttribute attr -> attr.getPlatformName().getText();
					case NestHostAttribute attr -> attr.getHostClass().getName().getText();
					case NestMembersAttribute attr -> "[" + attr.getMemberClasses().size() + "]";
					case ParameterAnnotationsAttribute attr -> "[" + attr.getParameterAnnotations().size() + "]";
					case PermittedClassesAttribute attr -> "[" + attr.getClasses().size() + "]";
					case RecordAttribute attr -> "[" + attr.getComponents().size() + "]";
					case SignatureAttribute attr -> attr.getSignature().getText();
					case SourceDebugExtensionAttribute attr -> {
						byte[] slice = Arrays.copyOf(attr.getDebugExtension(), Math.min(16, attr.getDebugExtension().length));
						StringBuilder sb = new StringBuilder();
						for (byte b : slice)
							sb.append(HexUtil.strFormat00(b)).append(' ');
						yield sb.toString().trim();
					}
					case SourceFileAttribute attr -> attr.getSourceFilename().getText();
					case SourceIdAttribute attr -> attr.getSourceId().getText();
					case StackMapTableAttribute attr -> "[" + attr.getFrames().size() + "]";
					case SyntheticAttribute attr -> "";
				},
				i -> switch (i) {
					case AnnotationDefaultAttribute attr -> Icons.getIconView(Icons.ANNOTATION);
					case AnnotationsAttribute attr -> Icons.getIconView(Icons.ANNOTATION);
					case BootstrapMethodsAttribute attr -> new FontIconView(CarbonIcons.CODE);
					case CharacterRangeTableAttribute attr -> new FontIconView(CarbonIcons.QUERY_QUEUE);
					case CodeAttribute attr -> new FontIconView(CarbonIcons.CODE);
					case CompilationIdAttribute attr -> new FontIconView(CarbonIcons.LICENSE_MAINTENANCE);
					case ConstantValueAttribute attr -> new FontIconView(CarbonIcons.OPERATION);
					case DefaultAttribute attr -> new FontIconView(CarbonIcons.UNKNOWN_FILLED);
					case DeprecatedAttribute attr -> new FontIconView(CarbonIcons.WARNING_ALT_FILLED, Color.YELLOW);
					case EnclosingMethodAttribute attr -> {
						CpClass classEntry = attr.getClassEntry();
						CpNameType methodEntry = attr.getMethodEntry();
						String owner = classEntry.getName().getText();
						if (methodEntry != null) {
							String methodName = methodEntry.getName().getText();
							String methodType = methodEntry.getType().getText();
							yield memberGraphic(owner, methodName, methodType);
						} else {
							yield classGraphic(owner);
						}
					}
					case ExceptionsAttribute attr -> new FontIconView(CarbonIcons.ERROR_FILLED, Color.RED);
					case InnerClassesAttribute attr -> new FontIconView(CarbonIcons.COPY);
					case LineNumberTableAttribute attr -> new FontIconView(CarbonIcons.SPINE_LABEL);
					case LocalVariableTableAttribute attr -> new FontIconView(CarbonIcons.SIGMA);
					case LocalVariableTypeTableAttribute attr -> new FontIconView(CarbonIcons.SIGMA);
					case MethodParametersAttribute attr -> new FontIconView(CarbonIcons.LETTER_PP);
					case ModuleAttribute attr -> new FontIconView(CarbonIcons.CATEGORIES);
					case ModuleHashesAttribute attr -> new FontIconView(CarbonIcons.LOCKED);
					case ModuleMainClassAttribute attr -> classGraphic(attr.getMainClass().getName().getText());
					case ModulePackagesAttribute attr -> Icons.getIconView(Icons.FOLDER_PACKAGE);
					case ModuleResolutionAttribute attr -> new FontIconView(CarbonIcons.SEARCH);
					case ModuleTargetAttribute attr -> new FontIconView(CarbonIcons.LAPTOP);
					case NestHostAttribute attr -> classGraphic(attr.getHostClass().getName().getText());
					case NestMembersAttribute attr -> new FontIconView(CarbonIcons.CATEGORIES);
					case ParameterAnnotationsAttribute attr -> Icons.getIconView(Icons.ANNOTATION);
					case PermittedClassesAttribute attr -> new FontIconView(CarbonIcons.CATEGORIES);
					case RecordAttribute attr -> new FontIconView(CarbonIcons.LIST_BOXES);
					case SignatureAttribute attr -> {
						String text = attr.getSignature().getText();
						yield memberGraphic(null, !text.isEmpty() && text.charAt(0) == '(');
					}
					case SourceDebugExtensionAttribute attr -> new FontIconView(CarbonIcons.DEBUG);
					case SourceFileAttribute attr -> new FontIconView(CarbonIcons.CHAT);
					case SourceIdAttribute attr -> new FontIconView(CarbonIcons.LICENSE_MAINTENANCE);
					case StackMapTableAttribute attr -> new FontIconView(CarbonIcons.CHART_STACKED);
					case SyntheticAttribute attr -> new FontIconView(CarbonIcons.SETTINGS);
				},
				i -> null);

		switch (attribute) {
			case AnnotationDefaultAttribute attr -> {
				addElementValue(child, "Default value", attr.getElementValue());
			}
			case AnnotationsAttribute attr -> {
				List<Annotation> annotations = attr.getAnnotations();
				for (int i = 0; i < annotations.size(); i++) {
					Annotation annotation = annotations.get(i);
					addAnnotation(child, "[" + i + "]", annotation);
				}
			}
			case BootstrapMethodsAttribute attr -> {
				List<BootstrapMethodsAttribute.BootstrapMethod> bootstrapMethods = attr.getBootstrapMethods();
				for (int j = 0; j < bootstrapMethods.size(); j++) {
					BootstrapMethodsAttribute.BootstrapMethod bsm = bootstrapMethods.get(j);
					ClassItem bsmItem = child.item("[" + j + "]", bsm,
							i -> cpToString(i.getBsmMethodRef()) + " args[" + i.getArgs().size() + "]",
							i -> cpToGraphic(i.getBsmMethodRef()),
							i -> null);
					bsmItem.item("Method reference", bsm.getBsmMethodRef(),
							i -> cpToString(i),
							i -> cpToGraphic(i),
							i -> null);
					List<CpEntry> args = bsm.getArgs();
					ClassItem arguments = bsmItem.item("Arguments", args,
							i -> "[" + i + "]",
							i -> Icons.getIconView(Icons.ARRAY),
							i -> null);
					for (int k = 0; k < args.size(); k++) {
						CpEntry arg = args.get(k);
						arguments.item("[" + k + "]", arg,
								i -> cpToString(i),
								i -> cpToGraphic(i),
								i -> null);
					}
				}
			}
			case CharacterRangeTableAttribute attr -> {
				List<CharacterRangeTableAttribute.CharacterRangeInfo> rangeInfos = attr.getCharacterRangeTable();
				for (int j = 0; j < rangeInfos.size(); j++) {
					child.item("[" + j + "]", rangeInfos.get(j),
							i -> "charRange=[" + i.getCharacterRangeStart() + "-" + i.getCharacterRangeEnd() + "] " +
									"codeRange=[" + i.getStartPc() + "-" + i.getEndPc() + "] " +
									"flags=" + Integer.toBinaryString(i.getFlags()),
							i -> new FontIconView(CarbonIcons.NUMBER_0),
							i -> null);
				}
			}
			case CodeAttribute attr -> {
				List<Instruction> instructions = attr.getInstructions();
				ClassItem instructionsItem = child.item("Instructions", instructions,
						i -> "[" + i.size() + "]",
						i -> Icons.getIconView(Icons.ARRAY),
						i -> null);
				for (int j = 0; j < instructions.size(); j++) {
					Instruction instruction = instructions.get(j);
					String insnName = AsmInsnUtil.getInsnName(instruction.getOpcode());
					instructionsItem.item("[" + j + "]", instruction,
							i -> switch (i) {
								case BasicInstruction insn -> insnName;
								case CpRefInstruction insn -> insnName + " " + cpToString(insn.getEntry());
								case IincInstruction insn ->
										insnName + " " + insn.getVar() + " += " + insn.getIncrement();
								case IntOperandInstruction insn -> insnName + " " + insn.getOperand();
								case MultiANewArrayInstruction insn ->
										insnName + " " + cpToString(insn.getDescriptor()) + " x" + insn.getDimensions();
								case LookupSwitchInstruction insn -> insnName; // TODO: Flesh out
								case TableSwitchInstruction insn -> insnName;
								case WideInstruction insn -> insnName;
							},
							i -> switch (i) {
								case BasicInstruction insn -> {
									int op = i.getOpcode();
									if (op == Opcodes.NOP)
										yield new FontIconView(CarbonIcons.SMOOTHING);

									// Constant numbers
									if (op <= Opcodes.SIPUSH)
										yield new FontIconView(CarbonIcons.STRING_INTEGER);

									// Var/array loads
									if (op <= Opcodes.ALOAD_3)
										yield SVG.ofIconFile(SVG.REF_READ);
									if (op <= Opcodes.SALOAD)
										yield Icons.getIconView(Icons.ARRAY);

									// Var/array stores
									if (op <= Opcodes.ASTORE_3)
										yield SVG.ofIconFile(SVG.REF_WRITE);
									if (op <= Opcodes.SASTORE)
										yield Icons.getIconView(Icons.ARRAY);

									// Stack
									if (op <= Opcodes.SWAP)
										yield new FontIconView(CarbonIcons.STACKED_SCROLLING_1);

									// Math operations
									if (op <= Opcodes.LXOR)
										yield new FontIconView(CarbonIcons.CALCULATOR);

									// Primitive conversions
									if (op <= Opcodes.I2S)
										yield new FontIconView(CarbonIcons.DATA_SHARE);

									// Stack value comparisons
									if (op <= Opcodes.DCMPG)
										yield new FontIconView(CarbonIcons.CALCULATOR);

									// Return
									if (op <= Opcodes.RETURN)
										yield new FontIconView(CarbonIcons.EXIT, Color.STEELBLUE);

									// Exception
									if (op == Opcodes.ATHROW)
										yield Icons.getIconView(Icons.CLASS_EXCEPTION);
									if (op == Opcodes.ARRAYLENGTH)
										yield Icons.getIconView(Icons.ARRAY);

									// Monitor
									yield new FontIconView(CarbonIcons.MAGNIFY); // No eye icon?
								}
								case CpRefInstruction insn -> cpToGraphic(insn.getEntry());
								case IincInstruction insn -> new FontIconView(CarbonIcons.ADD);
								case IntOperandInstruction insn -> {
									int op = i.getOpcode();

									// Constant numbers
									if (op <= Opcodes.SIPUSH)
										yield new FontIconView(CarbonIcons.STRING_INTEGER);

									// Var loads/stores
									if (op <= Opcodes.ALOAD_3)
										yield SVG.ofIconFile(SVG.REF_READ);
									if (op <= Opcodes.ASTORE_3)
										yield SVG.ofIconFile(SVG.REF_WRITE);

									// Control flow
									if (op <= Opcodes.RET)
										yield new FontIconView(CarbonIcons.BRANCH);

									// Array
									if (op == Opcodes.NEWARRAY)
										yield Icons.getIconView(Icons.ARRAY);

									// Remaining control flow
									yield new FontIconView(CarbonIcons.BRANCH);
								}
								case MultiANewArrayInstruction insn -> Icons.getIconView(Icons.ARRAY);
								case LookupSwitchInstruction insn -> new FontIconView(CarbonIcons.BRANCH);
								case TableSwitchInstruction insn -> new FontIconView(CarbonIcons.BRANCH);
								case WideInstruction insn -> new FontIconView(CarbonIcons.DRAG_HORIZONTAL);
							},
							i -> null);
				}
				List<CodeAttribute.ExceptionTableEntry> exceptionTable = attr.getExceptionTable();
				ClassItem exceptionTableItem = child.item("Exceptions", exceptionTable,
						i -> "[" + i.size() + "]",
						i -> Icons.getIconView(Icons.ARRAY),
						i -> null);
				for (int j = 0; j < exceptionTable.size(); j++) {
					CodeAttribute.ExceptionTableEntry exception = exceptionTable.get(j);
					exceptionTableItem.item("[" + j + "]", exception,
							i -> {
								String owner = i.getCatchType() == null ?
										"java/lang/Throwable" :
										i.getCatchType().getName().getText();
								return owner + "[" + i.getStartPc() + ":" + i.getEndPc() +
										"] handler[" + i.getHandlerPc() + "]";
							},
							i -> Icons.getIconView(Icons.CLASS_EXCEPTION),
							i -> null);
				}
			}
			case CompilationIdAttribute attr -> { /* single value */ }
			case ConstantValueAttribute attr -> { /* single value */ }
			case DefaultAttribute attr -> { /* unknown */ }
			case DeprecatedAttribute attr -> { /* empty */ }
			case EnclosingMethodAttribute attr -> {
				CpClass classEntry = attr.getClassEntry();
				CpNameType methodEntry = attr.getMethodEntry();
				child.item("Class", classEntry,
						i -> cpToString(i),
						i -> cpToGraphic(i),
						i -> null);
				if (methodEntry != null)
					child.item("Method", methodEntry,
							i -> cpToString(i),
							i -> cpToGraphic(i),
							i -> null);
			}
			case ExceptionsAttribute attr -> {
				List<CpClass> exceptionTable = attr.getExceptionTable();
				for (int j = 0; j < exceptionTable.size(); j++) {
					child.item("[" + j + "]", exceptionTable.get(j),
							i -> cpToString(i),
							i -> cpToGraphic(i),
							i -> null);
				}
			}
			case InnerClassesAttribute attr -> {
				List<InnerClassesAttribute.InnerClass> innerClasses = attr.getInnerClasses();
				for (int j = 0; j < innerClasses.size(); j++) {
					// Technically more data to show, but eh...
					child.item("[" + j + "]", innerClasses.get(j),
							i -> cpToString(i.getInnerClassInfo()),
							i -> cpToGraphic(i.getInnerClassInfo()),
							i -> null);
				}
			}
			case LineNumberTableAttribute attr -> {
				List<LineNumberTableAttribute.LineEntry> entries = attr.getEntries();
				for (int j = 0; j < entries.size(); j++) {
					child.item("[" + j + "]", entries.get(j),
							i -> "Line " + i.getLine() + " : offset=" + i.getStartPc(),
							i -> new FontIconView(CarbonIcons.NUMBER_0),
							i -> null);
				}
			}
			case LocalVariableTableAttribute attr -> {
				List<LocalVariableTableAttribute.VarEntry> entries = attr.getEntries();
				for (int j = 0; j < entries.size(); j++) {
					// TODO: Children for:
					//  - index
					//  - name
					//  - desc
					//  - range (start + length)
					child.item("[" + j + "]", entries.get(j),
							i -> {
								String name = i.getName().getText();
								String desc = i.getDesc().getText();
								if (!Types.isValidDesc(desc))
									desc = Types.OBJECT_TYPE.getDescriptor();
								return i.getIndex() + " : " + memberDisplay.getFieldDisplay(name, desc);
							},
							i -> {
								String desc = i.getDesc().getText();
								if (desc.isEmpty())
									return Icons.getIconView(Icons.PRIMITIVE);
								return switch (desc.charAt(0)) {
									case '[' -> Icons.getIconView(Icons.ARRAY);
									case 'L' -> Icons.getIconView(Icons.CLASS);
									default -> Icons.getIconView(Icons.PRIMITIVE);
								};
							},
							i -> null);
				}
			}
			case LocalVariableTypeTableAttribute attr -> {
				List<LocalVariableTypeTableAttribute.VarTypeEntry> entries = attr.getEntries();
				for (int j = 0; j < entries.size(); j++) {
					// TODO: Children for:
					//  - index
					//  - name
					//  - desc
					//  - range (start + length)
					child.item("[" + j + "]", entries.get(j),
							i -> {
								String name = i.getName().getText();
								String signature = i.getSignature().getText();
								if (!Types.isValidFieldSignature(signature))
									signature = Types.OBJECT_TYPE.getDescriptor();
								return i.getIndex() + " : " + memberDisplay.getFieldDisplay(name, signature);
							},
							i -> {
								String signature = i.getSignature().getText();
								if (signature.isEmpty())
									return Icons.getIconView(Icons.PRIMITIVE);
								return switch (signature.charAt(0)) {
									case '[' -> Icons.getIconView(Icons.ARRAY);
									case 'L' -> Icons.getIconView(Icons.CLASS);
									default -> Icons.getIconView(Icons.PRIMITIVE);
								};
							},
							i -> null);
				}
			}
			case MethodParametersAttribute attr -> {
				List<MethodParametersAttribute.Parameter> parameters = attr.getParameters();
				for (int j = 0; j < parameters.size(); j++) {
					child.item("[" + j + "]", parameters.get(j),
							i -> i.getName().getText(),
							i -> null,
							i -> null);
				}
			}
			case ModuleAttribute attr -> {}  // TODO: Load of crap
			case ModuleHashesAttribute attr -> {}  // TODO: Display algo + bytes
			case ModuleMainClassAttribute attr -> { /* single value */ }
			case ModulePackagesAttribute attr -> {
				List<CpPackage> packages = attr.getPackages();
				for (int j = 0; j < packages.size(); j++) {
					child.item("[" + j + "]", packages.get(j),
							i -> cpToString(i.getPackageName()),
							i -> Icons.getIconView(Icons.FOLDER_PACKAGE),
							i -> null);
				}
			}
			case ModuleResolutionAttribute attr -> { /* single value */ }
			case ModuleTargetAttribute attr -> { /* single value */ }
			case NestHostAttribute attr -> { /* single value */ }
			case NestMembersAttribute attr -> {
				List<CpClass> memberClasses = attr.getMemberClasses();
				for (int j = 0; j < memberClasses.size(); j++) {
					child.item("[" + j + "]", memberClasses.get(j),
							i -> cpToString(i.getName()),
							i -> classGraphic(i.getName().getText()),
							i -> null);
				}
			}
			case ParameterAnnotationsAttribute attr -> {
				attr.getParameterAnnotations().forEach((paramIndex, annotations) -> {
					ClassItem param = child.item("[" + paramIndex + "]", paramIndex,
							i -> "Parameter " + i + " [" + annotations.size() + "]",
							i -> null,
							i -> null);
					for (int i = 0; i < annotations.size(); i++) {
						Annotation annotation = annotations.get(i);
						addAnnotation(param, "[" + i + "]", annotation);
					}
				});
			}
			case PermittedClassesAttribute attr -> {
				List<CpClass> permittedClasses = attr.getClasses();
				for (int j = 0; j < permittedClasses.size(); j++) {
					child.item("[" + j + "]", permittedClasses.get(j),
							i -> cpToString(i.getName()),
							i -> classGraphic(i.getName().getText()),
							i -> null);
				}
			}
			case RecordAttribute attr -> {
				List<RecordAttribute.RecordComponent> components = attr.getComponents();
				for (int j = 0; j < components.size(); j++) {
					child.item("[" + j + "]", components.get(j),
							i -> cpToString(i.getName()),
							i -> memberGraphic(path.child(i.getName().getText(), i.getDesc().getText()), false),
							i -> null);
				}
			}
			case SignatureAttribute attr -> { /* single value */ }
			case SourceDebugExtensionAttribute attr -> { /* single value */ }
			case SourceFileAttribute attr -> { /* single value */ }
			case SourceIdAttribute attr -> { /* single value */ }
			case StackMapTableAttribute attr -> {
				List<StackMapTableAttribute.StackMapFrame> frames = attr.getFrames();
				for (int j = 0; j < frames.size(); j++) {
					child.item("[" + j + "]", frames.get(j),
							i -> switch (i) {
								case StackMapTableAttribute.AppendFrame appendFrame -> "APPEND: " + i.getFrameType();
								case StackMapTableAttribute.ChopFrame chopFrame -> "CHOP: " + i.getFrameType();
								case StackMapTableAttribute.FullFrame fullFrame -> "FULL: " + i.getFrameType();
								case StackMapTableAttribute.SameFrame sameFrame -> "SAME: " + i.getFrameType();
								case StackMapTableAttribute.SameFrameExtended sameFrameExtended ->
										"SAME_EXTENDED: " + i.getFrameType();
								case StackMapTableAttribute.SameLocalsOneStackItem sameLocalsOneStackItem ->
										"SAME_LOCALS_ONE_STACK: " + i.getFrameType();
								case StackMapTableAttribute.SameLocalsOneStackItemExtended sameLocalsOneStackItemExtended ->
										"SAME_LOCALS_ONE_STACK_EXTENDED: " + i.getFrameType();
							},
							i -> switch (i) {
								case StackMapTableAttribute.AppendFrame appendFrame ->
										new FontIconView(CarbonIcons.SUB_VOLUME);
								case StackMapTableAttribute.ChopFrame chopFrame ->
										new FontIconView(CarbonIcons.CUT_IN_HALF);
								case StackMapTableAttribute.FullFrame fullFrame -> new FontIconView(CarbonIcons.STOP);
								case StackMapTableAttribute.SameFrame sameFrame -> new FontIconView(CarbonIcons.COPY);
								case StackMapTableAttribute.SameFrameExtended sameFrameExtended ->
										new FontIconView(CarbonIcons.COPY);
								case StackMapTableAttribute.SameLocalsOneStackItem sameLocalsOneStackItem ->
										new FontIconView(CarbonIcons.COPY);
								case StackMapTableAttribute.SameLocalsOneStackItemExtended sameLocalsOneStackItemExtended ->
										new FontIconView(CarbonIcons.COPY);
							},
							i -> null);
				}
			}
			case SyntheticAttribute attr -> { /* empty */ }
		}

		if (attribute instanceof AttributeHolder nestedHolder)
			addAttributes(child, nestedHolder);

		return child;
	}

	@Nonnull
	private ClassItem addElementValue(@Nonnull ClassItem parentItem, @Nonnull String prefix, @Nonnull ElementValue value) {
		ClassItem child = parentItem.item(prefix, value,
				i -> elementValueToString(i),
				i -> switch (i) {
					case AnnotationElementValue subValue -> Icons.getIconView(Icons.ANNOTATION);
					case ArrayElementValue subValue -> Icons.getIconView(Icons.ARRAY);
					case ClassElementValue subValue -> classGraphic(subValue.getClassEntry().getText());
					case EnumElementValue subValue -> Icons.getIconView(Icons.ENUM);
					case PrimitiveElementValue subValue -> cpToGraphic(subValue.getValue());
					case Utf8ElementValue subValue -> cpToGraphic(subValue.getValue());
				},
				i -> null);

		if (value instanceof AnnotationElementValue subValue) {
			Annotation annotation = subValue.getAnnotation();
			addAnnotation(child, "Annotation", annotation);
		} else if (value instanceof ArrayElementValue subValue) {
			List<ElementValue> array = subValue.getArray();
			for (int i = 0; i < array.size(); i++) {
				ElementValue arrayValue = array.get(i);
				addElementValue(child, "Array [" + i + "]", arrayValue);
			}
		}

		return child;
	}

	@Nonnull
	private ClassItem addAnnotation(@Nonnull ClassItem parentItem, @Nonnull String prefix, @Nonnull Annotation annotation) {
		ClassItem item = parentItem.item(prefix, annotation,
				i -> i.getType().getText(),
				i -> Icons.getIconView(Icons.ANNOTATION),
				i -> null);
		annotation.getValues().forEach((key, value) -> {
			// We *could* allow changing annotation value-pair names if we tweak this a little bit...
			addElementValue(item, key.getText(), value);
		});
		return item;
	}

	@Override
	public void requestFocus(@Nonnull ClassMember member) {
		if (getCenter() instanceof TreeView<?> tv) {
			TreeItem<ClassElement> root = Unchecked.cast(tv.getRoot());
			Queue<TreeItem<ClassElement>> queue = new ArrayDeque<>();
			queue.add(root);
			while (!queue.isEmpty()) {
				TreeItem<ClassElement> item = queue.remove();
				ClassElement element = item.getValue();
				if (element instanceof LazyClassElement<?> lazyElement
						&& lazyElement.getElement() instanceof software.coley.cafedude.classfile.ClassMember itemMember) {
					if (member.getName().equals(itemMember.getName().getText())
							&& member.getDescriptor().equals(itemMember.getType().getText())) {
						TreeItems.expandParents(item);
						tv.getSelectionModel().select(Unchecked.cast(item));
						tv.scrollTo(tv.getRow(Unchecked.cast(item)));
						return;
					}
					continue;
				}
				queue.addAll(item.getChildren());
			}
		}
	}

	@Nonnull
	@Override
	public ClassPathNode getClassPath() {
		return path;
	}

	@Nullable
	@Override
	public ClassPathNode getPath() {
		return getClassPath();
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		if (path instanceof ClassPathNode classPath) {
			this.path = classPath;
			try {
				updateDisplay(classPath.getValue().asJvmClass());
			} catch (InvalidClassException e) {
				logger.error("Failed to create low-level display for invalid class", e);
				// TODO: This shouldn't happen, but better error handling/feedback would be nice just in case
				setCenter(new Label("Invalid class"));
			}
		}
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		setDisable(true);
	}

	@Nonnull
	private Node classGraphic(@Nonnull CpClass className) {
		return classGraphic(className.getName().getText());
	}

	@Nonnull
	private Node classGraphic(@Nonnull String className) {
		ClassPathNode path = workspace.findClass(className);
		if (path == null)
			return Icons.getIconView(Icons.CLASS);
		return configurationService.graphicOf(path);
	}

	@Nonnull
	private Node memberGraphic(@Nonnull String owner, @Nonnull String name, @Nonnull String type) {
		boolean isMethod = !type.isEmpty() && type.charAt(0) == '(';
		ClassPathNode path = workspace.findClass(owner);
		return memberGraphic(path == null ? null : path.child(name, type), isMethod);
	}

	@Nonnull
	private Node memberGraphic(@Nonnull CpClass owner, @Nonnull CpNameType nameType) {
		ClassPathNode ownerPath = workspace.findClass(owner.getName().getText());
		String name = nameType.getName().getText();
		String type = nameType.getType().getText();
		boolean isMethod = !type.isEmpty() && type.charAt(0) == '(';
		return memberGraphic(path == null ? null : path.child(name, type), isMethod);
	}

	@Nonnull
	private Node memberGraphic(@Nullable ClassMemberPathNode member, boolean isMethod) {
		if (member == null)
			return isMethod ? Icons.getIconView(Icons.METHOD) : Icons.getIconView(Icons.FIELD);
		return configurationService.graphicOf(member);
	}

	@Nonnull
	private String cpToString(@Nonnull CpEntry entry) {
		return switch (entry) {
			case ConstDynamic cp -> {
				CpNameType nameType = cp.getNameType();
				int bsmIndex = cp.getBsmIndex();
				String name = nameType.getName().getText();
				String desc = nameType.getType().getText();
				boolean isMethod = !desc.isEmpty() && desc.charAt(0) == '(';
				String ntSuffix = "nameType=" + (isMethod ?
						memberDisplay.getMethodDisplay(name, desc) :
						memberDisplay.getFieldDisplay(name, desc));
				yield "bootstrapMethodIndex=" + bsmIndex + " " + ntSuffix;
			}
			case ConstRef cp -> {
				CpNameType nameType = cp.getNameType();
				String owner = cp.getClassRef().getName().getText();
				String name = nameType.getName().getText();
				String desc = nameType.getType().getText();
				yield owner + "." + memberDisplay.getFieldDisplay(name, desc);
			}
			case CpClass cp -> cp.getName().getText();
			case CpDouble cp -> String.valueOf(cp.getValue());
			case CpFloat cp -> String.valueOf(cp.getValue());
			case CpInt cp -> String.valueOf(cp.getValue());
			case CpLong cp -> String.valueOf(cp.getValue());
			case CpMethodHandle cp -> {
				String kind = switch (cp.getKind()) {
					case REF_GET_FIELD -> "getField";
					case REF_GET_STATIC -> "getStatic";
					case REF_PUT_FIELD -> "putField";
					case REF_PUT_STATIC -> "putStatic";
					case REF_INVOKE_VIRTUAL -> "invokeVirtual";
					case REF_INVOKE_STATIC -> "invokeStatic";
					case REF_INVOKE_SPECIAL -> "invokeSpecial";
					case REF_NEW_INVOKE_SPECIAL -> "newInvokeSpecial";
					case REF_INVOKE_INTERFACE -> "invokeInterface";
					default -> "unknown";
				};
				CpNameType nameType = cp.getReference().getNameType();
				String owner = cp.getReference().getClassRef().getName().getText();
				String name = nameType.getName().getText();
				String desc = nameType.getType().getText();
				yield kind + "(" + owner + "." + memberDisplay.getFieldDisplay(name, desc) + ")";
			}
			case CpMethodType cp -> {
				String desc = cp.getDescriptor().getText();
				yield memberDisplay.getDescriptorDisplay(desc);
			}
			case CpModule cp -> cp.getName().getText();
			case CpNameType cp -> {
				String name = cp.getName().getText();
				String desc = cp.getType().getText();
				boolean isMethod = !desc.isEmpty() && desc.charAt(0) == '(';
				yield name + (isMethod ? "" : ".") + memberDisplay.getDescriptorDisplay(desc);
			}
			case CpPackage cp -> cp.getPackageName().getText();
			case CpString cp -> cp.getString().getText();
			case CpUtf8 cp -> cp.getText();
			case CpInternal cp -> "<internal>";
		};
	}

	@Nonnull
	private Node cpToGraphic(@Nonnull CpEntry entry) {
		return switch (entry) {
			case ConstDynamic cp -> memberGraphic(null, cp.getNameType().getType().getText().startsWith("("));
			case ConstRef cp -> memberGraphic(cp.getClassRef(), cp.getNameType());
			case CpClass cp -> classGraphic(cp);
			case CpDouble cp -> new FontIconView(CarbonIcons.STRING_INTEGER);
			case CpFloat cp -> new FontIconView(CarbonIcons.STRING_INTEGER);
			case CpInt cp -> new FontIconView(CarbonIcons.STRING_INTEGER);
			case CpLong cp -> new FontIconView(CarbonIcons.STRING_INTEGER);
			case CpMethodHandle cp -> memberGraphic(cp.getReference().getClassRef(), cp.getReference().getNameType());
			case CpMethodType cp -> memberGraphic(null, true);
			case CpModule cp -> new FontIconView(CarbonIcons.CATEGORIES);
			case CpNameType cp -> memberGraphic(null, cp.getType().getText().startsWith("("));
			case CpPackage cp -> Icons.getIconView(Icons.FOLDER_PACKAGE);
			case CpString cp -> new FontIconView(CarbonIcons.QUOTES);
			case CpUtf8 cp -> new FontIconView(CarbonIcons.STRING_TEXT);
			case CpInternal cp -> new Label("<internal>");
		};
	}

	@Nonnull
	private String elementValueToString(@Nonnull ElementValue value) {
		return switch (value) {
			case AnnotationElementValue subValue -> {
				Annotation annotation = subValue.getAnnotation();
				yield memberDisplay.getDescriptorDisplay(annotation.getType().getText());
			}
			case ArrayElementValue subValue -> "[" + subValue.getArray().size() + "]";
			case ClassElementValue subValue -> subValue.getClassEntry().getText();
			case EnumElementValue subValue -> {
				String ownerDesc = subValue.getType().getText();
				String owner = ownerDesc.length() > 2 ? ownerDesc.substring(1, ownerDesc.length() - 1) : ownerDesc;
				String name = subValue.getName().getText();
				yield owner + "." + memberDisplay.getFieldDisplay(name, ownerDesc);
			}
			case PrimitiveElementValue subValue -> cpToString(subValue.getValue());
			case Utf8ElementValue subValue -> cpToString(subValue.getValue());
		};
	}

	private static <E> ClassItem item(@Nonnull String prefix, @Nonnull E element,
	                                  @Nonnull Function<E, String> stringMapper,
	                                  @Nonnull Function<E, Node> graphicMapper,
	                                  @Nonnull Function<E, ContextMenu> menuMapper) {
		return new ClassItem(new LazyClassElement<>(prefix, element, stringMapper, graphicMapper, menuMapper));
	}
}
