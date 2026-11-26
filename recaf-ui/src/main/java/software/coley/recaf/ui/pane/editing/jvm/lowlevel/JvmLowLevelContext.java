package software.coley.recaf.ui.pane.editing.jvm.lowlevel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.cafedude.classfile.annotation.Annotation;
import software.coley.cafedude.classfile.annotation.AnnotationElementValue;
import software.coley.cafedude.classfile.annotation.ArrayElementValue;
import software.coley.cafedude.classfile.annotation.ClassElementValue;
import software.coley.cafedude.classfile.annotation.ElementValue;
import software.coley.cafedude.classfile.annotation.EnumElementValue;
import software.coley.cafedude.classfile.annotation.PrimitiveElementValue;
import software.coley.cafedude.classfile.annotation.Utf8ElementValue;
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
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.ui.config.MemberDisplayFormatConfig;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.pane.editing.binary.hex.HexUtil;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.Icons;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Locale;

import static software.coley.cafedude.classfile.constant.CpMethodHandle.*;
import static software.coley.recaf.util.AccessFlag.sortAndToString;

/**
 * Immutable rendering dependencies and formatters for one low-level class-file tree build.
 *
 * @author Matt Coley
 */
public final class JvmLowLevelContext {
	private final Workspace workspace;
	private final MemberDisplayFormatConfig memberDisplay;
	private final CellConfigurationService configurationService;
	private final ClassPathNode classPath;

	JvmLowLevelContext(@Nonnull Workspace workspace,
	                   @Nonnull MemberDisplayFormatConfig memberDisplay,
	                   @Nonnull CellConfigurationService configurationService,
	                   @Nonnull ClassPathNode classPath) {
		this.workspace = workspace;
		this.memberDisplay = memberDisplay;
		this.configurationService = configurationService;
		this.classPath = classPath;
	}

	/**
	 * Creates a graphic for a class constant.
	 *
	 * @param className
	 * 		Class constant.
	 *
	 * @return Class graphic.
	 */
	@Nonnull
	public Node classGraphic(@Nonnull CpClass className) {
		return classGraphic(className.getName().getText());
	}

	/**
	 * Creates a graphic for a class name.
	 *
	 * @param className
	 * 		Internal class name.
	 *
	 * @return Class graphic.
	 */
	@Nonnull
	public Node classGraphic(@Nonnull String className) {
		ClassPathNode path = workspace.findClass(className);
		if (path == null)
			return Icons.getIconView(Icons.CLASS);
		return configurationService.graphicOf(path);
	}

	/**
	 * Creates a graphic for a member reference.
	 *
	 * @param owner
	 * 		Owner class constant.
	 * @param nameType
	 * 		Member name-and-type constant.
	 *
	 * @return Member graphic.
	 */
	@Nonnull
	public Node memberGraphic(@Nonnull CpClass owner, @Nonnull CpNameType nameType) {
		ClassPathNode ownerPath = workspace.findClass(owner.getName().getText());
		String name = nameType.getName().getText();
		String type = nameType.getType().getText();
		boolean isMethod = isMethodDescriptor(type);
		return memberGraphic(ownerPath == null ? null : ownerPath.child(name, type), isMethod);
	}

	/**
	 * Creates a graphic for a textual owner/member reference.
	 *
	 * @param owner
	 * 		Owner class name.
	 * @param name
	 * 		Member name.
	 * @param type
	 * 		Member descriptor.
	 *
	 * @return Member graphic.
	 */
	@Nonnull
	public Node memberGraphic(@Nonnull String owner, @Nonnull String name, @Nonnull String type) {
		ClassPathNode ownerPath = workspace.findClass(owner);
		return memberGraphic(ownerPath == null ? null : ownerPath.child(name, type), isMethodDescriptor(type));
	}

	/**
	 * Creates a graphic for a resolved member path.
	 *
	 * @param member
	 * 		Resolved member path, or {@code null} if it cannot be resolved.
	 * @param isMethod
	 * 		Whether the member is a method.
	 *
	 * @return Member graphic.
	 */
	@Nonnull
	public Node memberGraphic(@Nullable ClassMemberPathNode member, boolean isMethod) {
		if (member == null)
			return isMethod ? Icons.getIconView(Icons.METHOD) : Icons.getIconView(Icons.FIELD);
		return configurationService.graphicOf(member);
	}

	/**
	 * Formats a constant-pool entry using the configured member display style.
	 *
	 * @param entry
	 * 		Constant-pool entry.
	 *
	 * @return Label for the entry.
	 */
	@Nonnull
	public String cpToString(@Nonnull CpEntry entry) {
		return switch (entry) {
			case ConstDynamic cp -> {
				CpNameType nameType = cp.getNameType();
				int bsmIndex = cp.getBsmIndex();
				String name = nameType.getName().getText();
				String desc = nameType.getType().getText();
				String ntSuffix = "nameType=" + memberDisplayFor(name, desc);
				yield "bootstrapMethodIndex=" + bsmIndex + " " + ntSuffix;
			}
			case ConstRef cp -> {
				CpNameType nameType = cp.getNameType();
				String owner = cp.getClassRef().getName().getText();
				String name = nameType.getName().getText();
				String desc = nameType.getType().getText();
				yield owner + "." + memberDisplayFor(name, desc);
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
				yield kind + "(" + owner + "." + memberDisplayFor(name, desc) + ")";
			}
			case CpMethodType cp -> memberDisplay.getDescriptorDisplay(cp.getDescriptor().getText());
			case CpModule cp -> cp.getName().getText();
			case CpNameType cp -> {
				String name = cp.getName().getText();
				String desc = cp.getType().getText();
				yield name + (isMethodDescriptor(desc) ? "" : ".") + memberDisplay.getDescriptorDisplay(desc);
			}
			case CpPackage cp -> cp.getPackageName().getText();
			case CpString cp -> cp.getString().getText();
			case CpUtf8 cp -> cp.getText();
			case CpInternal cp -> "<internal>";
		};
	}

	/**
	 * Creates a graphic for a constant-pool entry.
	 *
	 * @param entry
	 * 		Constant-pool entry.
	 *
	 * @return Constant graphic.
	 */
	@Nonnull
	public Node cpToGraphic(@Nonnull CpEntry entry) {
		return switch (entry) {
			case ConstDynamic cp -> memberGraphic(null, isMethodDescriptor(cp.getNameType().getType().getText()));
			case ConstRef cp -> memberGraphic(cp.getClassRef(), cp.getNameType());
			case CpClass cp -> classGraphic(cp);
			case CpDouble cp -> new FontIconView(CarbonIcons.STRING_INTEGER);
			case CpFloat cp -> new FontIconView(CarbonIcons.STRING_INTEGER);
			case CpInt cp -> new FontIconView(CarbonIcons.STRING_INTEGER);
			case CpLong cp -> new FontIconView(CarbonIcons.STRING_INTEGER);
			case CpMethodHandle cp -> memberGraphic(cp.getReference().getClassRef(), cp.getReference().getNameType());
			case CpMethodType cp -> memberGraphic(null, true);
			case CpModule cp -> new FontIconView(CarbonIcons.CATEGORIES);
			case CpNameType cp -> memberGraphic(null, isMethodDescriptor(cp.getType().getText()));
			case CpPackage cp -> Icons.getIconView(Icons.FOLDER_PACKAGE);
			case CpString cp -> new FontIconView(CarbonIcons.QUOTES);
			case CpUtf8 cp -> new FontIconView(CarbonIcons.STRING_TEXT);
			case CpInternal cp -> new Label("<internal>");
		};
	}

	/**
	 * Formats an annotation element value.
	 *
	 * @param value
	 * 		Element value.
	 *
	 * @return Label text for the value.
	 */
	@Nonnull
	public String elementValueToString(@Nonnull ElementValue value) {
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

	/**
	 * Formats a complete byte array as space-separated hexadecimal bytes.
	 *
	 * @param bytes
	 * 		Bytes to format.
	 *
	 * @return Complete hexadecimal representation, or an empty string for no bytes.
	 */
	@Nonnull
	public String fullHex(@Nonnull byte[] bytes) {
		StringBuilder builder = new StringBuilder(bytes.length * 3);
		for (int i = 0; i < bytes.length; i++) {
			if (i > 0)
				builder.append(' ');
			builder.append(HexUtil.strFormat00(bytes[i]));
		}
		return builder.toString();
	}

	/**
	 * Formats decoded access flags together with their raw numeric mask.
	 *
	 * @param type
	 * 		Flag group.
	 * @param mask
	 * 		Raw flag mask.
	 *
	 * @return Decoded flags followed by the raw hexadecimal mask.
	 */
	@Nonnull
	public String flags(@Nonnull AccessFlag.Type type, int mask) {
		String decoded = sortAndToString(type, mask);
		if (decoded.isEmpty())
			decoded = "<none>";
		return decoded + " (0x" + Integer.toHexString(mask).toUpperCase(Locale.ROOT) + ")";
	}

	/**
	 * @return Class path used to resolve workspace-dependent graphics.
	 */
	@Nonnull
	public ClassPathNode classPath() {
		return classPath;
	}

	/**
	 * @return Member display configuration for this tree build.
	 */
	@Nonnull
	public MemberDisplayFormatConfig memberDisplay() {
		return memberDisplay;
	}

	@Nonnull
	private String memberDisplayFor(@Nonnull String name, @Nonnull String descriptor) {
		return isMethodDescriptor(descriptor)
				? memberDisplay.getMethodDisplay(name, descriptor)
				: memberDisplay.getFieldDisplay(name, descriptor);
	}

	private static boolean isMethodDescriptor(@Nonnull String descriptor) {
		return !descriptor.isEmpty() && descriptor.charAt(0) == '(';
	}
}
