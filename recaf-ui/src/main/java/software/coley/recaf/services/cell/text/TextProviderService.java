package software.coley.recaf.services.cell.text;

import dev.xdark.blw.asm.internal.Util;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.darknet.assembler.helper.Names;
import me.darknet.assembler.printer.InstructionPrinter;
import me.darknet.assembler.printer.PrintContext;
import org.benf.cfr.reader.entities.annotations.ElementValue;
import org.objectweb.asm.tree.*;
import software.coley.recaf.info.*;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationElement;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.phantom.GeneratedPhantomWorkspaceResource;
import software.coley.recaf.ui.config.TextFormatConfig;
import software.coley.recaf.ui.control.tree.WorkspaceTreeCell;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.*;
import software.coley.recaf.workspace.model.resource.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Provides support for providing text for a variety of item types.
 * For instance, the text of {@link WorkspaceTreeCell} instances.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TextProviderService implements Service {
	public static final String SERVICE_ID = "cell-text";
	private final TextProviderServiceConfig config;
	private final TextFormatConfig formatConfig;

	@Inject
	public TextProviderService(@Nonnull TextProviderServiceConfig config,
							   @Nonnull TextFormatConfig formatConfig) {
		this.config = config;
		this.formatConfig = formatConfig;
		// Unlike the other services for graphics/menus, I don't see a use-case for text customization...
		// Will keep the model similar to them though just in case so that it is easy to add in the future.
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The class to create a text for.
	 *
	 * @return Text provider for the class.
	 */
	@Nonnull
	public TextProvider getClassInfoTextProvider(@Nonnull Workspace workspace,
												 @Nonnull WorkspaceResource resource,
												 @Nonnull ClassBundle<? extends ClassInfo> bundle,
												 @Nonnull ClassInfo info) {
		return () -> formatConfig.filter(info.getName());
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The class to create a text for.
	 *
	 * @return Text provider for the class.
	 */
	@Nonnull
	public TextProvider getJvmClassInfoTextProvider(@Nonnull Workspace workspace,
													@Nonnull WorkspaceResource resource,
													@Nonnull JvmClassBundle bundle,
													@Nonnull JvmClassInfo info) {
		return () -> formatConfig.filter(info.getName());
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The class to create text for.
	 *
	 * @return Text provider for the class.
	 */
	@Nonnull
	public TextProvider getAndroidClassInfoTextProvider(@Nonnull Workspace workspace,
														@Nonnull WorkspaceResource resource,
														@Nonnull AndroidClassBundle bundle,
														@Nonnull AndroidClassInfo info) {
		return () -> formatConfig.filter(info.getName());
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param outerClass
	 * 		Outer class.
	 * @param inner
	 * 		The inner class to create text for.
	 *
	 * @return Text provider for the inner class.
	 */
	@Nonnull
	public TextProvider getInnerClassInfoTextProvider(@Nonnull Workspace workspace,
													  @Nonnull WorkspaceResource resource,
													  @Nonnull ClassBundle<? extends ClassInfo> bundle,
													  @Nonnull ClassInfo outerClass,
													  @Nonnull InnerClassInfo inner) {
		return () -> formatConfig.filter(inner.getSimpleName());
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Containing class.
	 * @param fieldOrMethod
	 * 		The field or method to create text for.
	 *
	 * @return Text provider for the field or method.
	 */
	@Nonnull
	public TextProvider getMemberTextProvider(@Nonnull Workspace workspace,
											  @Nonnull WorkspaceResource resource,
											  @Nonnull ClassBundle<? extends ClassInfo> bundle,
											  @Nonnull ClassInfo declaringClass,
											  @Nonnull ClassMember fieldOrMethod) {
		return fieldOrMethod.isField() ?
				getFieldMemberTextProvider(workspace, resource, bundle, declaringClass, (FieldMember) fieldOrMethod) :
				getMethodMemberTextProvider(workspace, resource, bundle, declaringClass, (MethodMember) fieldOrMethod);
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Containing class.
	 * @param field
	 * 		The field to create text for.
	 *
	 * @return Text provider for the field.
	 */
	@Nonnull
	public TextProvider getFieldMemberTextProvider(@Nonnull Workspace workspace,
												   @Nonnull WorkspaceResource resource,
												   @Nonnull ClassBundle<? extends ClassInfo> bundle,
												   @Nonnull ClassInfo declaringClass,
												   @Nonnull FieldMember field) {
		// TODO: Will want to provide config option for showing the type
		//  - name (default)
		//  - type + name
		return () -> formatConfig.filter(field.getName());
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Containing class.
	 * @param method
	 * 		The method to create text for.
	 *
	 * @return Text provider for the method.
	 */
	@Nonnull
	public TextProvider getMethodMemberTextProvider(@Nonnull Workspace workspace,
													@Nonnull WorkspaceResource resource,
													@Nonnull ClassBundle<? extends ClassInfo> bundle,
													@Nonnull ClassInfo declaringClass,
													@Nonnull MethodMember method) {
		// TODO: Will want to provide config option for showing the descriptor
		//  - hidden (default)
		//  - raw
		//  - simple names
		return () -> formatConfig.filter(method.getName());
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Containing class.
	 * @param declaringMethod
	 * 		Containing method.
	 *
	 * @return Text provider for the instruction.
	 */
	@Nonnull
	public TextProvider getInstructionTextProvider(@Nonnull Workspace workspace,
												   @Nonnull WorkspaceResource resource,
												   @Nonnull ClassBundle<? extends ClassInfo> bundle,
												   @Nonnull ClassInfo declaringClass,
												   @Nonnull MethodMember declaringMethod,
												   @Nonnull AbstractInsnNode insn,
												   int index) {
		return () -> {
			PrintContext<?> ctx = new PrintContext<>("");
			InstructionPrinter printer = new InstructionPrinter(ctx.code(),
					null, new Names(Collections.emptyMap(), Collections.emptyList()),
					Collections.emptyMap()
			);

			// Map ASM insn model to BLW which is used by JASM
			if (insn instanceof LdcInsnNode ldc) {
				printer.execute(Util.wrapLdcInsn(ldc.cst));
			} else if (insn instanceof MethodInsnNode min) {
				printer.execute(Util.wrapMethodInsn(min.getOpcode(), min.owner, min.name, min.desc, false));
			} else if (insn instanceof FieldInsnNode fin) {
				printer.execute(Util.wrapFieldInsn(fin.getOpcode(), fin.owner, fin.name, fin.desc));
			} else if (insn instanceof TypeInsnNode tin) {
				printer.execute(Util.wrapTypeInsn(tin.getOpcode(), tin.desc));
			} else if (insn instanceof IntInsnNode iin) {
				printer.execute(Util.wrapIntInsn(iin.getOpcode(), iin.operand));
			} else if (insn instanceof InsnNode in) {
				printer.execute(Util.wrapInsn(in.getOpcode()));
			} else if (insn instanceof InvokeDynamicInsnNode indy) {
				printer.execute(Util.wrapInvokeDynamicInsn(indy.name, indy.desc, indy.bsm, indy.bsmArgs));
			} else {
				// The current search models shouldn't yield anything aside from the above types.
				return "<missing text mapper: " + insn.getClass().getSimpleName() + ">";
			}

			// Cut off first 2 chars of unused indentation then cap off the max length.
			String text = ctx.toString().substring(2);
			return formatConfig.filterMaxLength(text);
		};
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringFile
	 * 		Containing file.
	 * @param line
	 * 		The line in the file to get the text of.
	 *
	 * @return Text provider for the line number.
	 */
	@Nonnull
	public TextProvider getLineNumberTextProvider(@Nonnull Workspace workspace,
												  @Nonnull WorkspaceResource resource,
												  @Nonnull FileBundle bundle,
												  @Nonnull FileInfo declaringFile,
												  int line) {
		return () -> {
			if (declaringFile.isTextFile()) {
				int index = line - 1;
				String[] lines = declaringFile.asTextFile().getTextLines();
				if (index >= 0 && index < lines.length)
					return lines[index];
			}
			return "???";
		};
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param annotated
	 * 		The annotated item.
	 * @param annotation
	 * 		The annotation to create an icon for.
	 *
	 * @return Text provider for the annotation.
	 */
	@Nonnull
	public TextProvider getAnnotationTextProvider(@Nonnull Workspace workspace,
												  @Nonnull WorkspaceResource resource,
												  @Nonnull ClassBundle<? extends ClassInfo> bundle,
												  @Nonnull Annotated annotated,
												  @Nonnull AnnotationInfo annotation) {
		return () -> {
			String desc = annotation.getDescriptor();
			String type = formatConfig.filter(desc.substring(1, desc.length() - 1));

			StringBuilder sb = new StringBuilder(type);
			Map<String, AnnotationElement> elements = annotation.getElements();
			if (!elements.isEmpty()) {
				sb.append('(');
				elements.forEach((key, element) -> {
					Object value = element.getElementValue();
					sb.append(key).append(" = ");
					if (value instanceof String s)
						sb.append('"').append(formatConfig.filter(s)).append('"');
					else if (value instanceof ElementValue)
						sb.append("{...}");
					else if (value instanceof List)
						sb.append("[...]");
					else
						sb.append('"').append(formatConfig.filter(String.valueOf(value))).append('"');
					sb.append(", ");
				});
				sb.setLength(sb.length() - 2);
				sb.append(')');
			}

			return formatConfig.filterMaxLength(sb.toString());
		};
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The file to create text for.
	 *
	 * @return Text provider for the file.
	 */
	@Nonnull
	public TextProvider getFileInfoTextProvider(@Nonnull Workspace workspace,
												@Nonnull WorkspaceResource resource,
												@Nonnull FileBundle bundle,
												@Nonnull FileInfo info) {
		return () -> formatConfig.filter(info.getName());
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param packageName
	 * 		The full package name, separated by {@code /}.
	 *
	 * @return Text provider for the package.
	 */
	@Nonnull
	public TextProvider getPackageTextProvider(@Nonnull Workspace workspace,
											   @Nonnull WorkspaceResource resource,
											   @Nonnull ClassBundle<? extends ClassInfo> bundle,
											   @Nonnull String packageName) {
		return () -> {
			if (packageName.isEmpty())
				return Lang.get("tree.defaultpackage");
			return formatConfig.filter(packageName);
		};
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param directoryName
	 * 		The full path of the directory.
	 *
	 * @return Text provider for the directory.
	 */
	@Nonnull
	public TextProvider getDirectoryTextProvider(@Nonnull Workspace workspace,
												 @Nonnull WorkspaceResource resource,
												 @Nonnull FileBundle bundle,
												 @Nonnull String directoryName) {
		return () -> {
			if (directoryName.isEmpty())
				return Lang.get("tree.defaultdirectory");
			return formatConfig.filter(directoryName);
		};
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		The bundle to create text for.
	 *
	 * @return Text provider for the bundle.
	 */
	@Nonnull
	public TextProvider getBundleTextProvider(@Nonnull Workspace workspace,
											  @Nonnull WorkspaceResource resource,
											  @Nonnull Bundle<? extends Info> bundle) {
		return () -> {
			if (bundle instanceof AndroidClassBundle) {
				String dexName = resource.getAndroidClassBundles().entrySet().stream()
						.filter(e -> e.getValue() == bundle)
						.map(Map.Entry::getKey)
						.findFirst()
						.orElse(null);
				if (dexName != null)
					return dexName;
			} else if (bundle instanceof AgentServerRemoteVmResource.RemoteJvmClassBundle remoteBundle) {
				return formatConfig.filter(remoteBundle.getLoaderInfo().getName());
			} else if (bundle instanceof VersionedClassBundle versionedClassBundle) {
				return Lang.get("tree.classes") + " (Java " + versionedClassBundle.version() + ")";
			}

			if (bundle instanceof ClassBundle)
				return Lang.get("tree.classes");
			else
				return Lang.get("tree.files");
		};
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		The resource to create text for.
	 *
	 * @return Text provider for the resource.
	 */
	@Nonnull
	public TextProvider getResourceTextProvider(@Nonnull Workspace workspace,
												@Nonnull WorkspaceResource resource) {
		return () -> {
			if (resource instanceof WorkspaceFileResource fileResource) {
				String name = fileResource.getFileInfo().getName();
				return name.substring(name.lastIndexOf('/') + 1);
			} else if (resource instanceof WorkspaceDirectoryResource directoryResource) {
				return StringUtil.pathToNameString(directoryResource.getDirectoryPath());
			} else if (resource instanceof WorkspaceRemoteVmResource remoteVmResource) {
				return remoteVmResource.getVirtualMachine().id();
			} else if (resource instanceof GeneratedPhantomWorkspaceResource) {
				return Lang.get("tree.phantoms");
			}
			return resource.getClass().getSimpleName();
		};
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public TextProviderServiceConfig getServiceConfig() {
		return config;
	}
}
