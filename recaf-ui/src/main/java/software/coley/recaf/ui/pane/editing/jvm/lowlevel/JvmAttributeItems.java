package software.coley.recaf.ui.pane.editing.jvm.lowlevel;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
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
import software.coley.cafedude.classfile.annotation.Annotation;
import software.coley.cafedude.classfile.behavior.AttributeHolder;
import software.coley.cafedude.classfile.constant.CpEntry;
import software.coley.cafedude.classfile.constant.CpUtf8;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.pane.editing.binary.hex.HexUtil;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Registers class-file attributes and their nested values for low-level trees.
 *
 * @author Matt Coley
 * @see JvmLowLevelTreeBuilder
 */
public final class JvmAttributeItems {
	private JvmAttributeItems() {}

	/**
	 * Registers attribute model handlers.
	 *
	 * @param registry
	 * 		Tree item registry.
	 * @param context
	 * 		Per-build rendering context.
	 */
	static void register(@Nonnull ClassItemRegistry registry, @Nonnull JvmLowLevelContext context) {
		registerAttribute(registry, Attribute.class, JvmAttributeItems::attributeText, value -> null, (item, value) -> {
		});
		registerAttribute(registry, AnnotationDefaultAttribute.class,
				attribute -> context.elementValueToString(attribute.getElementValue()),
				value -> Icons.getIconView(Icons.ANNOTATION),
				(item, attribute) -> registry.add(item, "Default value", attribute.getElementValue()));
		registerAttribute(registry, AnnotationsAttribute.class,
				attribute -> "[" + attribute.getAnnotations().size() + "]",
				value -> Icons.getIconView(Icons.ANNOTATION),
				(item, attribute) -> {
					registry.add(item, "Visible", attribute.isVisible());
					ClassItem annotations = addList(registry, item, "Annotations", attribute.getAnnotations());
					for (int i = 0; i < attribute.getAnnotations().size(); i++)
						registry.add(annotations, "[" + i + "]", attribute.getAnnotations().get(i));
				});
		registerAttribute(registry, BootstrapMethodsAttribute.class,
				attribute -> "[" + attribute.getBootstrapMethods().size() + "]",
				value -> new FontIconView(CarbonIcons.CODE),
				(item, attribute) -> {
					ClassItem bootstrapMethods = addList(registry, item, "Bootstrap methods", attribute.getBootstrapMethods());
					for (int i = 0; i < attribute.getBootstrapMethods().size(); i++)
						registry.add(bootstrapMethods, "[" + i + "]", attribute.getBootstrapMethods().get(i));
				});
		registerAttribute(registry, CharacterRangeTableAttribute.class,
				attribute -> "[" + attribute.getCharacterRangeTable().size() + "]",
				value -> new FontIconView(CarbonIcons.QUERY_QUEUE),
				(item, attribute) -> {
					ClassItem entries = addList(registry, item, "Entries", attribute.getCharacterRangeTable());
					for (int i = 0; i < attribute.getCharacterRangeTable().size(); i++)
						registry.add(entries, "[" + i + "]", attribute.getCharacterRangeTable().get(i));
				});
		registerAttribute(registry, CodeAttribute.class,
				attribute -> "maxLocals=" + attribute.getMaxLocals() + ", maxStack=" + attribute.getMaxStack()
						+ ", instructions[" + attribute.getInstructions().size() + "]",
				value -> new FontIconView(CarbonIcons.CODE),
				(item, attribute) -> {
					registry.add(item, "Max stack", attribute.getMaxStack());
					registry.add(item, "Max locals", attribute.getMaxLocals());
					ClassItem instructions = addList(registry, item, "Instructions", attribute.getInstructions());
					for (int i = 0; i < attribute.getInstructions().size(); i++)
						registry.add(instructions, "[" + i + "]", attribute.getInstructions().get(i));
					ClassItem exceptions = addList(registry, item, "Exceptions", attribute.getExceptionTable());
					for (int i = 0; i < attribute.getExceptionTable().size(); i++)
						registry.add(exceptions, "[" + i + "]", attribute.getExceptionTable().get(i));
					addAttributes(registry, context, item, attribute);
				});
		registerAttribute(registry, CompilationIdAttribute.class,
				attribute -> attribute.getCompilationId().getText(),
				value -> new FontIconView(CarbonIcons.LICENSE_MAINTENANCE),
				(item, attribute) -> registry.add(item, "Compilation ID", attribute.getCompilationId()));
		registerAttribute(registry, ConstantValueAttribute.class,
				attribute -> context.cpToString(attribute.getConstantValue()),
				value -> new FontIconView(CarbonIcons.OPERATION),
				(item, attribute) -> registry.add(item, "Constant value", attribute.getConstantValue()));
		registerAttribute(registry, DefaultAttribute.class,
				attribute -> previewHex(attribute.getData()),
				value -> new FontIconView(CarbonIcons.UNKNOWN_FILLED),
				(item, attribute) -> registry.add(item, "Data", attribute.getData(), definition -> definition
						.text(context::fullHex)));
		registerAttribute(registry, DeprecatedAttribute.class, attribute -> "",
				value -> new FontIconView(CarbonIcons.WARNING_ALT_FILLED, Color.YELLOW),
				(item, attribute) -> {
				});
		registerAttribute(registry, EnclosingMethodAttribute.class,
				attribute -> enclosingMethodText(context, attribute),
				attribute -> enclosingMethodGraphic(context, attribute),
				(item, attribute) -> {
					registry.add(item, "Class", attribute.getClassEntry());
					registry.addNullable(item, "Method", attribute.getMethodEntry());
				});
		registerAttribute(registry, ExceptionsAttribute.class,
				attribute -> "[" + attribute.getExceptionTable().size() + "]",
				value -> new FontIconView(CarbonIcons.ERROR_FILLED, Color.RED),
				(item, attribute) -> {
					ClassItem entries = addList(registry, item, "Exceptions", attribute.getExceptionTable());
					for (int i = 0; i < attribute.getExceptionTable().size(); i++)
						registry.add(entries, "[" + i + "]", attribute.getExceptionTable().get(i));
				});
		registerAttribute(registry, InnerClassesAttribute.class,
				attribute -> "[" + attribute.getInnerClasses().size() + "]",
				value -> new FontIconView(CarbonIcons.COPY),
				(item, attribute) -> {
					ClassItem entries = addList(registry, item, "Inner classes", attribute.getInnerClasses());
					for (int i = 0; i < attribute.getInnerClasses().size(); i++)
						registry.add(entries, "[" + i + "]", attribute.getInnerClasses().get(i));
				});
		registerAttribute(registry, LineNumberTableAttribute.class,
				attribute -> "[" + attribute.getEntries().size() + "]",
				value -> new FontIconView(CarbonIcons.SPINE_LABEL),
				(item, attribute) -> {
					ClassItem entries = addList(registry, item, "Entries", attribute.getEntries());
					for (int i = 0; i < attribute.getEntries().size(); i++)
						registry.add(entries, "[" + i + "]", attribute.getEntries().get(i));
				});
		registerAttribute(registry, LocalVariableTableAttribute.class,
				attribute -> "[" + attribute.getEntries().size() + "]",
				value -> new FontIconView(CarbonIcons.SIGMA),
				(item, attribute) -> {
					ClassItem entries = addList(registry, item, "Entries", attribute.getEntries());
					for (int i = 0; i < attribute.getEntries().size(); i++)
						registry.add(entries, "[" + i + "]", attribute.getEntries().get(i));
				});
		registerAttribute(registry, LocalVariableTypeTableAttribute.class,
				attribute -> "[" + attribute.getEntries().size() + "]",
				value -> new FontIconView(CarbonIcons.SIGMA),
				(item, attribute) -> {
					ClassItem entries = addList(registry, item, "Entries", attribute.getEntries());
					for (int i = 0; i < attribute.getEntries().size(); i++)
						registry.add(entries, "[" + i + "]", attribute.getEntries().get(i));
				});
		registerAttribute(registry, MethodParametersAttribute.class,
				attribute -> "[" + attribute.getParameters().size() + "]",
				value -> new FontIconView(CarbonIcons.LETTER_PP),
				(item, attribute) -> {
					ClassItem parameters = addList(registry, item, "Parameters", attribute.getParameters());
					for (int i = 0; i < attribute.getParameters().size(); i++)
						registry.add(parameters, "[" + i + "]", attribute.getParameters().get(i));
				});
		registerAttribute(registry, ModuleAttribute.class,
				attribute -> attribute.getModule().getName().getText(),
				value -> new FontIconView(CarbonIcons.CATEGORIES),
				(item, attribute) -> addModuleChildren(registry, context, item, attribute));
		registerAttribute(registry, ModuleHashesAttribute.class,
				attribute -> attribute.getAlgorithmName().getText(),
				value -> new FontIconView(CarbonIcons.LOCKED),
				(item, attribute) -> addModuleHashChildren(registry, context, item, attribute));
		registerAttribute(registry, ModuleMainClassAttribute.class,
				attribute -> attribute.getMainClass().getName().getText(),
				attribute -> context.classGraphic(attribute.getMainClass()),
				(item, attribute) -> registry.add(item, "Main class", attribute.getMainClass()));
		registerAttribute(registry, ModulePackagesAttribute.class,
				attribute -> "[" + attribute.getPackages().size() + "]",
				value -> Icons.getIconView(Icons.FOLDER_PACKAGE),
				(item, attribute) -> {
					ClassItem packages = addList(registry, item, "Packages", attribute.getPackages());
					for (int i = 0; i < attribute.getPackages().size(); i++)
						registry.add(packages, "[" + i + "]", attribute.getPackages().get(i));
				});
		registerAttribute(registry, ModuleResolutionAttribute.class,
				attribute -> "mask=" + Integer.toBinaryString(attribute.getFlags()),
				value -> new FontIconView(CarbonIcons.SEARCH),
				(item, attribute) -> registry.add(item, "Flags", resolutionFlags(attribute.getFlags())));
		registerAttribute(registry, ModuleTargetAttribute.class,
				attribute -> attribute.getPlatformName().getText(),
				value -> new FontIconView(CarbonIcons.LAPTOP),
				(item, attribute) -> registry.add(item, "Platform name", attribute.getPlatformName()));
		registerAttribute(registry, NestHostAttribute.class,
				attribute -> attribute.getHostClass().getName().getText(),
				attribute -> context.classGraphic(attribute.getHostClass()),
				(item, attribute) -> registry.add(item, "Host class", attribute.getHostClass()));
		registerAttribute(registry, NestMembersAttribute.class,
				attribute -> "[" + attribute.getMemberClasses().size() + "]",
				value -> new FontIconView(CarbonIcons.CATEGORIES),
				(item, attribute) -> {
					ClassItem members = addList(registry, item, "Members", attribute.getMemberClasses());
					for (int i = 0; i < attribute.getMemberClasses().size(); i++)
						registry.add(members, "[" + i + "]", attribute.getMemberClasses().get(i));
				});
		registerAttribute(registry, ParameterAnnotationsAttribute.class,
				attribute -> "[" + attribute.getParameterAnnotations().size() + "]",
				value -> Icons.getIconView(Icons.ANNOTATION),
				(item, attribute) -> addParameterAnnotationChildren(registry, item, attribute));
		registerAttribute(registry, PermittedClassesAttribute.class,
				attribute -> "[" + attribute.getClasses().size() + "]",
				value -> new FontIconView(CarbonIcons.CATEGORIES),
				(item, attribute) -> {
					ClassItem classes = addList(registry, item, "Classes", attribute.getClasses());
					for (int i = 0; i < attribute.getClasses().size(); i++)
						registry.add(classes, "[" + i + "]", attribute.getClasses().get(i));
				});
		registerAttribute(registry, RecordAttribute.class,
				attribute -> "[" + attribute.getComponents().size() + "]",
				value -> new FontIconView(CarbonIcons.LIST_BOXES),
				(item, attribute) -> {
					ClassItem components = addList(registry, item, "Components", attribute.getComponents());
					for (int i = 0; i < attribute.getComponents().size(); i++)
						registry.add(components, "[" + i + "]", attribute.getComponents().get(i));
				});
		registerAttribute(registry, SignatureAttribute.class,
				attribute -> attribute.getSignature().getText(),
				attribute -> context.memberGraphic(null, !attribute.getSignature().getText().isEmpty()
						&& attribute.getSignature().getText().charAt(0) == '('),
				(item, attribute) -> registry.add(item, "Signature", attribute.getSignature()));
		registerAttribute(registry, SourceDebugExtensionAttribute.class,
				attribute -> previewHex(attribute.getDebugExtension()),
				value -> new FontIconView(CarbonIcons.DEBUG),
				(item, attribute) -> registry.add(item, "Debug extension", attribute.getDebugExtension(), definition -> definition
						.text(context::fullHex)));
		registerAttribute(registry, SourceFileAttribute.class,
				attribute -> attribute.getSourceFilename().getText(),
				value -> new FontIconView(CarbonIcons.CHAT),
				(item, attribute) -> registry.add(item, "Source filename", attribute.getSourceFilename()));
		registerAttribute(registry, SourceIdAttribute.class,
				attribute -> attribute.getSourceId().getText(),
				value -> new FontIconView(CarbonIcons.LICENSE_MAINTENANCE),
				(item, attribute) -> registry.add(item, "Source ID", attribute.getSourceId()));
		registerAttribute(registry, StackMapTableAttribute.class,
				attribute -> "[" + attribute.getFrames().size() + "]",
				value -> new FontIconView(CarbonIcons.CHART_STACKED),
				(item, attribute) -> {
					ClassItem frames = addList(registry, item, "Frames", attribute.getFrames());
					for (int i = 0; i < attribute.getFrames().size(); i++)
						registry.add(frames, "[" + i + "]", attribute.getFrames().get(i));
				});
		registerAttribute(registry, SyntheticAttribute.class, attribute -> "",
				value -> new FontIconView(CarbonIcons.SETTINGS), (item, attribute) -> {
				});

		registerNestedTypes(registry, context);
	}

	/**
	 * Adds an attribute collection and its registered children.
	 *
	 * @param registry
	 * 		Tree item registry.
	 * @param context
	 * 		Per-build rendering context.
	 * @param parent
	 * 		Parent item.
	 * @param holder
	 * 		Attribute holder.
	 *
	 * @return Attribute collection item.
	 */
	@Nonnull
	static ClassItem addAttributes(@Nonnull ClassItemRegistry registry, @Nonnull JvmLowLevelContext context,
	                               @Nonnull ClassItem parent, @Nonnull AttributeHolder holder) {
		ClassItem attributes = registry.add(parent, "Attributes", holder.getAttributes(), definition -> definition
				.text(values -> "[" + values.size() + "]")
				.graphic(values -> Icons.getIconView(Icons.ARRAY)));
		for (Attribute attribute : holder.getAttributes())
			registry.add(attributes, attribute.getName().getText(), attribute);
		return attributes;
	}

	private static void registerNestedTypes(@Nonnull ClassItemRegistry registry, @Nonnull JvmLowLevelContext context) {
		registry.register(BootstrapMethodsAttribute.BootstrapMethod.class)
				.text(value -> context.cpToString(value.getBsmMethodRef()) + " args[" + value.getArgs().size() + "]")
				.graphic(value -> context.cpToGraphic(value.getBsmMethodRef()))
				.children((item, value) -> {
					registry.add(item, "Method reference", value.getBsmMethodRef());
					ClassItem arguments = addList(registry, item, "Arguments", value.getArgs());
					for (int i = 0; i < value.getArgs().size(); i++)
						registry.add(arguments, "[" + i + "]", value.getArgs().get(i));
				});
		registry.register(CharacterRangeTableAttribute.CharacterRangeInfo.class)
				.text(value -> "charRange=[" + value.getCharacterRangeStart() + "-" + value.getCharacterRangeEnd() + "] codeRange=["
						+ value.getStartPc() + "-" + value.getEndPc() + "] flags=" + value.getFlags())
				.graphic(value -> new FontIconView(CarbonIcons.NUMBER_0))
				.children((item, value) -> {
					registry.add(item, "Start PC", value.getStartPc());
					registry.add(item, "End PC", value.getEndPc());
					registry.add(item, "Character range start", value.getCharacterRangeStart());
					registry.add(item, "Character range end", value.getCharacterRangeEnd());
					registry.add(item, "Flags", rawMask(value.getFlags()));
				});
		registry.register(CodeAttribute.ExceptionTableEntry.class)
				.text(value -> {
					String owner = value.getCatchType() == null ? "java/lang/Throwable" : value.getCatchType().getName().getText();
					return owner + "[" + value.getStartPc() + ":" + value.getEndPc() + "] handler[" + value.getHandlerPc() + "]";
				})
				.graphic(value -> Icons.getIconView(Icons.CLASS_EXCEPTION))
				.children((item, value) -> {
					registry.add(item, "Start PC", value.getStartPc());
					registry.add(item, "End PC", value.getEndPc());
					registry.add(item, "Handler PC", value.getHandlerPc());
					registry.addNullable(item, "Catch type", value.getCatchType());
				});
		registry.register(InnerClassesAttribute.InnerClass.class)
				.text(value -> context.cpToString(value.getInnerClassInfo()))
				.graphic(value -> context.cpToGraphic(value.getInnerClassInfo()))
				.children((item, value) -> {
					registry.add(item, "Inner class info", value.getInnerClassInfo());
					registry.addNullable(item, "Outer class info", value.getOuterClassInfo());
					registry.addNullable(item, "Inner name", value.getInnerName());
					registry.add(item, "Access", context.flags(AccessFlag.Type.INNER_CLASS, value.getInnerClassAccessFlags()));
				});
		registry.register(LineNumberTableAttribute.LineEntry.class)
				.text(value -> "Line " + value.getLine() + " : offset=" + value.getStartPc())
				.graphic(value -> new FontIconView(CarbonIcons.NUMBER_0))
				.children((item, value) -> {
					registry.add(item, "Start PC", value.getStartPc());
					registry.add(item, "Line", value.getLine());
				});
		registry.register(LocalVariableTableAttribute.VarEntry.class)
				.text(value -> localVariableText(context, value.getName().getText(), value.getDesc().getText(), value.getIndex()))
				.graphic(value -> descriptorGraphic(value.getDesc().getText()))
				.children((item, value) -> {
					registry.add(item, "Start PC", value.getStartPc());
					registry.add(item, "Length", value.getLength());
					registry.add(item, "Name", value.getName());
					registry.add(item, "Descriptor", value.getDesc());
					registry.add(item, "Index", value.getIndex());
				});
		registry.register(LocalVariableTypeTableAttribute.VarTypeEntry.class)
				.text(value -> localVariableText(context, value.getName().getText(), value.getSignature().getText(), value.getIndex()))
				.graphic(value -> descriptorGraphic(value.getSignature().getText()))
				.children((item, value) -> {
					registry.add(item, "Start PC", value.getStartPc());
					registry.add(item, "Length", value.getLength());
					registry.add(item, "Name", value.getName());
					registry.add(item, "Signature", value.getSignature());
					registry.add(item, "Index", value.getIndex());
				});
		registry.register(MethodParametersAttribute.Parameter.class)
				.text(value -> value.getName() == null ? "<null>" : value.getName().getText())
				.children((item, value) -> {
					registry.add(item, "Access", context.flags(AccessFlag.Type.PARAM, value.getAccessFlags()));
					registry.addNullable(item, "Name", value.getName());
				});
		registry.register(ModuleAttribute.Requires.class)
				.text(value -> value.getModule().getName().getText())
				.graphic(value -> new FontIconView(CarbonIcons.CATEGORIES))
				.children((item, value) -> {
					registry.add(item, "Module", value.getModule());
					registry.add(item, "Flags", context.flags(AccessFlag.Type.MODULE, value.getFlags()));
					registry.addNullable(item, "Version", value.getVersion());
				});
		registerModuleExports(registry, context);
		registerModuleOpens(registry, context);
		registry.register(ModuleAttribute.Provides.class)
				.text(value -> value.getModule().getName().getText())
				.graphic(value -> context.classGraphic(value.getModule()))
				.children((item, value) -> {
					registry.add(item, "Service", value.getModule());
					ClassItem implementations = addList(registry, item, "With", value.getWith());
					for (int i = 0; i < value.getWith().size(); i++)
						registry.add(implementations, "[" + i + "]", value.getWith().get(i));
				});
		registry.register(RecordAttribute.RecordComponent.class)
				.text(value -> context.cpToString(value.getName()))
				.graphic(value -> context.memberGraphic(context.classPath().child(value.getName().getText(), value.getDesc().getText()), false))
				.children((item, value) -> {
					registry.add(item, "Name", value.getName());
					registry.add(item, "Descriptor", value.getDesc());
					addAttributes(registry, context, item, value);
				});
	}

	private static void registerModuleExports(@Nonnull ClassItemRegistry registry,
	                                          @Nonnull JvmLowLevelContext context) {
		registry.register(ModuleAttribute.Exports.class)
				.text(value -> value.getPackageEntry().getPackageName().getText())
				.graphic(value -> Icons.getIconView(Icons.FOLDER_PACKAGE))
				.children((item, value) -> {
					registry.add(item, "Package", value.getPackageEntry());
					registry.add(item, "Flags", context.flags(AccessFlag.Type.MODULE, value.getFlags()));
					ClassItem to = addList(registry, item, "To", value.getTo());
					for (int i = 0; i < value.getTo().size(); i++)
						registry.add(to, "[" + i + "]", value.getTo().get(i));
				});
	}

	private static void registerModuleOpens(@Nonnull ClassItemRegistry registry,
	                                        @Nonnull JvmLowLevelContext context) {
		registry.register(ModuleAttribute.Opens.class)
				.text(value -> value.getPackageEntry().getPackageName().getText())
				.graphic(value -> Icons.getIconView(Icons.FOLDER_PACKAGE))
				.children((item, value) -> {
					registry.add(item, "Package", value.getPackageEntry());
					registry.add(item, "Flags", context.flags(AccessFlag.Type.MODULE, value.getFlags()));
					ClassItem to = addList(registry, item, "To", value.getTo());
					for (int i = 0; i < value.getTo().size(); i++)
						registry.add(to, "[" + i + "]", value.getTo().get(i));
				});
	}

	private static void addModuleChildren(@Nonnull ClassItemRegistry registry, @Nonnull JvmLowLevelContext context,
	                                      @Nonnull ClassItem item, @Nonnull ModuleAttribute attribute) {
		registry.add(item, "Module", attribute.getModule());
		registry.add(item, "Flags", context.flags(AccessFlag.Type.MODULE, attribute.getFlags()));
		registry.addNullable(item, "Version", attribute.getVersion());
		ClassItem requires = addList(registry, item, "Requires", attribute.getRequires());
		for (int i = 0; i < attribute.getRequires().size(); i++)
			registry.add(requires, "[" + i + "]", attribute.getRequires().get(i));
		ClassItem exports = addList(registry, item, "Exports", attribute.getExports());
		for (int i = 0; i < attribute.getExports().size(); i++)
			registry.add(exports, "[" + i + "]", attribute.getExports().get(i));
		ClassItem opens = addList(registry, item, "Opens", attribute.getOpens());
		for (int i = 0; i < attribute.getOpens().size(); i++)
			registry.add(opens, "[" + i + "]", attribute.getOpens().get(i));
		ClassItem uses = addList(registry, item, "Uses", attribute.getUses());
		for (int i = 0; i < attribute.getUses().size(); i++)
			registry.add(uses, "[" + i + "]", attribute.getUses().get(i));
		ClassItem provides = addList(registry, item, "Provides", attribute.getProvides());
		for (int i = 0; i < attribute.getProvides().size(); i++)
			registry.add(provides, "[" + i + "]", attribute.getProvides().get(i));
	}

	private static void addModuleHashChildren(@Nonnull ClassItemRegistry registry, @Nonnull JvmLowLevelContext context,
	                                          @Nonnull ClassItem item, @Nonnull ModuleHashesAttribute attribute) {
		registry.add(item, "Algorithm", attribute.getAlgorithmName());
		List<ModuleHash> hashes = new ArrayList<>(attribute.getModuleHashes().size());
		for (Map.Entry<CpUtf8, byte[]> entry : attribute.getModuleHashes().entrySet())
			hashes.add(new ModuleHash(entry.getKey(), entry.getValue()));
		ClassItem entries = addList(registry, item, "Module hashes", hashes);
		for (int i = 0; i < hashes.size(); i++) {
			ModuleHash hash = hashes.get(i);
			registry.add(entries, "[" + i + "]", hash, definition -> definition
					.text(value -> value.module().getText())
					.children((parent, value) -> {
						registry.add(parent, "Module", value.module());
						registry.add(parent, "Hash", value.hash(), hashDefinition -> hashDefinition.text(context::fullHex));
					}));
		}
	}

	private static void addParameterAnnotationChildren(@Nonnull ClassItemRegistry registry, @Nonnull ClassItem item,
	                                                   @Nonnull ParameterAnnotationsAttribute attribute) {
		registry.add(item, "Visible", attribute.isVisible());
		ClassItem parameters = registry.add(item, "Parameters", attribute.getParameterAnnotations(), definition -> definition
				.text(values -> "[" + values.size() + "]")
				.graphic(values -> Icons.getIconView(Icons.ARRAY)));
		for (Map.Entry<Integer, List<Annotation>> entry : attribute.getParameterAnnotations().entrySet()) {
			int index = entry.getKey();
			List<Annotation> annotations = entry.getValue();
			ClassItem parameter = registry.add(parameters, "[" + index + "]", index, definition -> definition
					.text(value -> "Parameter " + value + " [" + annotations.size() + "]")
					.children((parent, value) -> {
						for (int i = 0; i < annotations.size(); i++)
							registry.add(parent, "[" + i + "]", annotations.get(i));
					}));
		}
	}

	@Nonnull
	private static <E> ClassItem addList(@Nonnull ClassItemRegistry registry, @Nonnull ClassItem parent,
	                                    @Nonnull String prefix, @Nonnull List<E> values) {
		return registry.add(parent, prefix, values, definition -> definition
				.text(list -> "[" + list.size() + "]")
				.graphic(list -> Icons.getIconView(Icons.ARRAY)));
	}

	private static <E extends Attribute> void registerAttribute(@Nonnull ClassItemRegistry registry,
	                                                             @Nonnull Class<E> type,
	                                                             @Nonnull Function<E, String> text,
	                                                             @Nonnull Function<E, Node> graphic,
	                                                             @Nonnull BiConsumer<ClassItem, E> children) {
		registry.register(type)
				.text(text)
				.graphic(graphic)
				.children((item, attribute) -> {
					registry.add(item, "Name", attribute.getName());
					registry.add(item, "Internal length", attribute.computeInternalLength());
				})
				.children(children);
	}

	@Nonnull
	private static String localVariableText(@Nonnull JvmLowLevelContext context, @Nonnull String name,
	                                        @Nonnull String descriptor, int index) {
		String display = descriptor;
		if (!Types.isValidDesc(descriptor) && !Types.isValidFieldSignature(descriptor))
			display = Types.OBJECT_TYPE.getDescriptor();
		return index + " : " + context.memberDisplay().getFieldDisplay(name, display);
	}

	@Nonnull
	private static Node descriptorGraphic(@Nonnull String descriptor) {
		if (descriptor.isEmpty())
			return Icons.getIconView(Icons.PRIMITIVE);
		return switch (descriptor.charAt(0)) {
			case '[' -> Icons.getIconView(Icons.ARRAY);
			case 'L' -> Icons.getIconView(Icons.CLASS);
			default -> Icons.getIconView(Icons.PRIMITIVE);
		};
	}

	@Nonnull
	private static String enclosingMethodText(@Nonnull JvmLowLevelContext context, @Nonnull EnclosingMethodAttribute attribute) {
		String owner = attribute.getClassEntry().getName().getText();
		if (attribute.getMethodEntry() == null)
			return owner;
		String name = attribute.getMethodEntry().getName().getText();
		String descriptor = attribute.getMethodEntry().getType().getText();
		return owner + "." + context.memberDisplay().getMethodDisplay(name, descriptor);
	}

	@Nonnull
	private static Node enclosingMethodGraphic(@Nonnull JvmLowLevelContext context,
	                                           @Nonnull EnclosingMethodAttribute attribute) {
		if (attribute.getMethodEntry() == null)
			return context.classGraphic(attribute.getClassEntry());
		return context.memberGraphic(attribute.getClassEntry(), attribute.getMethodEntry());
	}

	@Nonnull
	private static String attributeText(@Nonnull Attribute attribute) {
		return switch (attribute) {
			case AnnotationDefaultAttribute value -> "[default]";
			case AnnotationsAttribute value -> "[" + value.getAnnotations().size() + "]";
			case BootstrapMethodsAttribute value -> "[" + value.getBootstrapMethods().size() + "]";
			case CharacterRangeTableAttribute value -> "[" + value.getCharacterRangeTable().size() + "]";
			case CodeAttribute value -> "maxLocals=" + value.getMaxLocals() + ", maxStack=" + value.getMaxStack()
					+ ", instructions[" + value.getInstructions().size() + "]";
			case CompilationIdAttribute value -> value.getCompilationId().getText();
			case ConstantValueAttribute value -> String.valueOf(value.getConstantValue());
			case DefaultAttribute value -> previewHex(value.getData());
			case DeprecatedAttribute value -> "";
			case EnclosingMethodAttribute value -> value.getClassEntry().getName().getText();
			case ExceptionsAttribute value -> "[" + value.getExceptionTable().size() + "]";
			case InnerClassesAttribute value -> "[" + value.getInnerClasses().size() + "]";
			case LineNumberTableAttribute value -> "[" + value.getEntries().size() + "]";
			case LocalVariableTableAttribute value -> "[" + value.getEntries().size() + "]";
			case LocalVariableTypeTableAttribute value -> "[" + value.getEntries().size() + "]";
			case MethodParametersAttribute value -> "[" + value.getParameters().size() + "]";
			case ModuleAttribute value -> value.getModule().getName().getText();
			case ModuleHashesAttribute value -> value.getAlgorithmName().getText();
			case ModuleMainClassAttribute value -> value.getMainClass().getName().getText();
			case ModulePackagesAttribute value -> "[" + value.getPackages().size() + "]";
			case ModuleResolutionAttribute value -> "mask=" + Integer.toBinaryString(value.getFlags());
			case ModuleTargetAttribute value -> value.getPlatformName().getText();
			case NestHostAttribute value -> value.getHostClass().getName().getText();
			case NestMembersAttribute value -> "[" + value.getMemberClasses().size() + "]";
			case ParameterAnnotationsAttribute value -> "[" + value.getParameterAnnotations().size() + "]";
			case PermittedClassesAttribute value -> "[" + value.getClasses().size() + "]";
			case RecordAttribute value -> "[" + value.getComponents().size() + "]";
			case SignatureAttribute value -> value.getSignature().getText();
			case SourceDebugExtensionAttribute value -> previewHex(value.getDebugExtension());
			case SourceFileAttribute value -> value.getSourceFilename().getText();
			case SourceIdAttribute value -> value.getSourceId().getText();
			case StackMapTableAttribute value -> "[" + value.getFrames().size() + "]";
			case SyntheticAttribute value -> "";
		};
	}

	@Nonnull
	private static Node attributeGraphic(@Nonnull Attribute attribute, @Nonnull JvmLowLevelContext context) {
		return switch (attribute) {
			case AnnotationDefaultAttribute value -> Icons.getIconView(Icons.ANNOTATION);
			case AnnotationsAttribute value -> Icons.getIconView(Icons.ANNOTATION);
			case BootstrapMethodsAttribute value -> new FontIconView(CarbonIcons.CODE);
			case CodeAttribute value -> new FontIconView(CarbonIcons.CODE);
			case CharacterRangeTableAttribute value -> new FontIconView(CarbonIcons.QUERY_QUEUE);
			case CompilationIdAttribute value -> new FontIconView(CarbonIcons.LICENSE_MAINTENANCE);
			case ConstantValueAttribute value -> new FontIconView(CarbonIcons.OPERATION);
			case DefaultAttribute value -> new FontIconView(CarbonIcons.UNKNOWN_FILLED);
			case DeprecatedAttribute value -> new FontIconView(CarbonIcons.WARNING_ALT_FILLED, Color.YELLOW);
			case EnclosingMethodAttribute value -> enclosingMethodGraphic(context, value);
			case ExceptionsAttribute value -> new FontIconView(CarbonIcons.ERROR_FILLED, Color.RED);
			case InnerClassesAttribute value -> new FontIconView(CarbonIcons.COPY);
			case LineNumberTableAttribute value -> new FontIconView(CarbonIcons.SPINE_LABEL);
			case LocalVariableTableAttribute value -> new FontIconView(CarbonIcons.SIGMA);
			case LocalVariableTypeTableAttribute value -> new FontIconView(CarbonIcons.SIGMA);
			case MethodParametersAttribute value -> new FontIconView(CarbonIcons.LETTER_PP);
			case ModuleAttribute value -> new FontIconView(CarbonIcons.CATEGORIES);
			case ModuleHashesAttribute value -> new FontIconView(CarbonIcons.LOCKED);
			case ModuleMainClassAttribute value -> context.classGraphic(value.getMainClass());
			case ModulePackagesAttribute value -> Icons.getIconView(Icons.FOLDER_PACKAGE);
			case ModuleResolutionAttribute value -> new FontIconView(CarbonIcons.SEARCH);
			case ModuleTargetAttribute value -> new FontIconView(CarbonIcons.LAPTOP);
			case NestHostAttribute value -> context.classGraphic(value.getHostClass());
			case NestMembersAttribute value -> new FontIconView(CarbonIcons.CATEGORIES);
			case PermittedClassesAttribute value -> new FontIconView(CarbonIcons.CATEGORIES);
			case ParameterAnnotationsAttribute value -> Icons.getIconView(Icons.ANNOTATION);
			case RecordAttribute value -> new FontIconView(CarbonIcons.LIST_BOXES);
			case SignatureAttribute value -> context.memberGraphic(null, value.getSignature().getText().startsWith("("));
			case SourceDebugExtensionAttribute value -> new FontIconView(CarbonIcons.DEBUG);
			case SourceFileAttribute value -> new FontIconView(CarbonIcons.CHAT);
			case SourceIdAttribute value -> new FontIconView(CarbonIcons.LICENSE_MAINTENANCE);
			case StackMapTableAttribute value -> new FontIconView(CarbonIcons.CHART_STACKED);
			case SyntheticAttribute value -> new FontIconView(CarbonIcons.SETTINGS);
		};
	}

	@Nonnull
	private static String previewHex(@Nonnull byte[] bytes) {
		int length = Math.min(16, bytes.length);
		StringBuilder builder = new StringBuilder(length * 3);
		for (int i = 0; i < length; i++) {
			if (i > 0)
				builder.append(' ');
			builder.append(HexUtil.strFormat00(bytes[i]));
		}
		return builder.toString();
	}

	@Nonnull
	private static String rawMask(int mask) {
		return "0x" + Integer.toHexString(mask);
	}

	@Nonnull
	private static String resolutionFlags(int flags) {
		List<String> names = new ArrayList<>();
		if ((flags & ModuleResolutionAttribute.DO_NOT_RESOLVE_BY_DEFAULT) != 0)
			names.add("do-not-resolve-by-default");
		if ((flags & ModuleResolutionAttribute.WARN_DEPRECATED) != 0)
			names.add("warn-deprecated");
		if ((flags & ModuleResolutionAttribute.WARN_DEPRECATED_FOR_REMOVAL) != 0)
			names.add("warn-deprecated-for-removal");
		if ((flags & ModuleResolutionAttribute.WARN_INCUBATING) != 0)
			names.add("warn-incubating");
		if (names.isEmpty())
			names.add("<none>");
		return String.join(" ", names) + " (" + rawMask(flags) + ")";
	}

	private record ModuleHash(@Nonnull CpUtf8 module, @Nonnull byte[] hash) {
	}
}
