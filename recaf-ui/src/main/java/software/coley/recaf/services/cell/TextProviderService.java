package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.info.*;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.Service;
import software.coley.recaf.ui.config.TextFormatConfig;
import software.coley.recaf.ui.control.tree.WorkspaceTreeCell;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.*;
import software.coley.recaf.workspace.model.resource.WorkspaceDirectoryResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceRemoteVmResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

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
			// TODO: Will want to provide config option for showing elements
			//  - type name
			//  - type name + elements
			String desc = annotation.getDescriptor();
			return formatConfig.filter(desc.substring(1, desc.length() - 1));
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
