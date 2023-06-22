package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.info.*;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.Service;
import software.coley.recaf.ui.control.tree.WorkspaceTreeCell;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.*;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Provides support for providing context menus for a variety of item types.
 * For instance, the menus of {@link WorkspaceTreeCell} instances.
 * <br>
 * The menus displayed in the UI can be swapped out by supplying your own
 * {@link ContextMenuProviderFactory} instances to the overrides:
 * <ul>
 *     <li>{@link #setClassContextMenuProviderOverride(ClassContextMenuProviderFactory)}</li>
 *     <li>{@link #setFileContextMenuProviderOverride(FileContextMenuProviderFactory)}</li>
 *     <li>{@link #setInnerClassContextMenuProviderOverride(InnerClassContextMenuProviderFactory)}</li>
 *     <li>{@link #setFieldContextMenuProviderOverride(FieldContextMenuProviderFactory)}</li>
 *     <li>{@link #setMethodContextMenuProviderOverride(MethodContextMenuProviderFactory)}</li>
 *     <li>{@link #setPackageContextMenuProviderOverride(PackageContextMenuProviderFactory)}</li>
 *     <li>{@link #setDirectoryContextMenuProviderOverride(DirectoryContextMenuProviderFactory)}</li>
 *     <li>{@link #setBundleContextMenuProviderOverride(BundleContextMenuProviderFactory)}</li>
 *     <li>{@link #setResourceContextMenuProviderOverride(ResourceContextMenuProviderFactory)}</li>
 * </ul>
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ContextMenuProviderService implements Service {
	public static final String SERVICE_ID = "cell-menus";
	private final ContextMenuProviderServiceConfig config;
	// Defaults
	private final ClassContextMenuProviderFactory classContextMenuDefault;
	private final FileContextMenuProviderFactory fileContextMenuDefault;
	private final InnerClassContextMenuProviderFactory innerClassContextMenuDefault;
	private final FieldContextMenuProviderFactory fieldContextMenuDefault;
	private final MethodContextMenuProviderFactory methodContextMenuDefault;
	private final AnnotationContextMenuProviderFactory annotationContextMenuDefault;
	private final PackageContextMenuProviderFactory packageContextMenuDefault;
	private final DirectoryContextMenuProviderFactory directoryContextMenuDefault;
	private final BundleContextMenuProviderFactory bundleContextMenuDefault;
	private final ResourceContextMenuProviderFactory resourceContextMenuDefault;
	// Overrides
	private ClassContextMenuProviderFactory classContextMenuOverride;
	private FileContextMenuProviderFactory fileContextMenuOverride;
	private InnerClassContextMenuProviderFactory innerClassContextMenuOverride;
	private FieldContextMenuProviderFactory fieldContextMenuOverride;
	private MethodContextMenuProviderFactory methodContextMenuOverride;
	private AnnotationContextMenuProviderFactory annotationContextMenuOverride;
	private PackageContextMenuProviderFactory packageContextMenuOverride;
	private DirectoryContextMenuProviderFactory directoryContextMenuOverride;
	private BundleContextMenuProviderFactory bundleContextMenuOverride;
	private ResourceContextMenuProviderFactory resourceContextMenuOverride;

	@Inject
	public ContextMenuProviderService(@Nonnull ContextMenuProviderServiceConfig config,
									  @Nonnull ClassContextMenuProviderFactory classContextMenuDefault,
									  @Nonnull FileContextMenuProviderFactory fileContextMenuDefault,
									  @Nonnull InnerClassContextMenuProviderFactory innerClassContextMenuDefault,
									  @Nonnull FieldContextMenuProviderFactory fieldContextMenuDefault,
									  @Nonnull MethodContextMenuProviderFactory methodContextMenuDefault,
									  @Nonnull AnnotationContextMenuProviderFactory annotationContextMenuDefault,
									  @Nonnull PackageContextMenuProviderFactory packageContextMenuDefault,
									  @Nonnull DirectoryContextMenuProviderFactory directoryContextMenuDefault,
									  @Nonnull BundleContextMenuProviderFactory bundleContextMenuDefault,
									  @Nonnull ResourceContextMenuProviderFactory resourceContextMenuDefault) {
		this.config = config;

		// Default factories
		this.classContextMenuDefault = classContextMenuDefault;
		this.fileContextMenuDefault = fileContextMenuDefault;
		this.innerClassContextMenuDefault = innerClassContextMenuDefault;
		this.fieldContextMenuDefault = fieldContextMenuDefault;
		this.methodContextMenuDefault = methodContextMenuDefault;
		this.annotationContextMenuDefault = annotationContextMenuDefault;
		this.packageContextMenuDefault = packageContextMenuDefault;
		this.directoryContextMenuDefault = directoryContextMenuDefault;
		this.bundleContextMenuDefault = bundleContextMenuDefault;
		this.resourceContextMenuDefault = resourceContextMenuDefault;
	}

	/**
	 * Delegates to {@link ClassContextMenuProviderFactory}.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The class to create a menu for.
	 *
	 * @return Menu provider for the class.
	 */
	@Nonnull
	public ContextMenuProvider getJvmClassInfoContextMenuProvider(@Nonnull ContextSource source,
																  @Nonnull Workspace workspace,
																  @Nonnull WorkspaceResource resource,
																  @Nonnull JvmClassBundle bundle,
																  @Nonnull JvmClassInfo info) {
		ClassContextMenuProviderFactory factory = classContextMenuOverride != null ? classContextMenuOverride : classContextMenuDefault;
		return factory.getJvmClassInfoContextMenuProvider(source, workspace, resource, bundle, info);
	}

	/**
	 * Delegates to {@link ClassContextMenuProviderFactory}.
	 *
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The class to create a menu for.
	 *
	 * @return Menu provider for the class.
	 */
	@Nonnull
	public ContextMenuProvider getAndroidClassInfoContextMenuProvider(@Nonnull ContextSource source,
																	  @Nonnull Workspace workspace,
																	  @Nonnull WorkspaceResource resource,
																	  @Nonnull AndroidClassBundle bundle,
																	  @Nonnull AndroidClassInfo info) {
		ClassContextMenuProviderFactory factory = classContextMenuOverride != null ? classContextMenuOverride : classContextMenuDefault;
		return factory.getAndroidClassInfoContextMenuProvider(source, workspace, resource, bundle, info);
	}

	/**
	 * Delegates to {@link InnerClassContextMenuProviderFactory}.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param outerClass
	 * 		Outer class.
	 * @param inner
	 * 		The inner class to create a menu for.
	 *
	 * @return Menu provider for the class.
	 */
	@Nonnull
	public ContextMenuProvider getInnerClassInfoContextMenuProvider(@Nonnull ContextSource source,
																	@Nonnull Workspace workspace,
																	@Nonnull WorkspaceResource resource,
																	@Nonnull ClassBundle<? extends ClassInfo> bundle,
																	@Nonnull ClassInfo outerClass,
																	@Nonnull InnerClassInfo inner) {
		InnerClassContextMenuProviderFactory factory = innerClassContextMenuOverride != null ? innerClassContextMenuOverride : innerClassContextMenuDefault;
		return factory.getInnerClassInfoContextMenuProvider(source, workspace, resource, bundle, outerClass, inner);
	}

	/**
	 * Delegates to {@link FieldContextMenuProviderFactory} and {@link MethodContextMenuProviderFactory}.
	 *
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Containing class.
	 * @param member
	 * 		The member to create a menu for.
	 *
	 * @return Menu provider for the class member.
	 */
	@Nonnull
	public ContextMenuProvider getClassMemberContextMenuProvider(@Nonnull ContextSource source,
																 @Nonnull Workspace workspace,
																 @Nonnull WorkspaceResource resource,
																 @Nonnull ClassBundle<? extends ClassInfo> bundle,
																 @Nonnull ClassInfo declaringClass,
																 @Nonnull ClassMember member) {
		if (member.isField()) {
			FieldContextMenuProviderFactory factory = fieldContextMenuOverride != null ? fieldContextMenuOverride : fieldContextMenuDefault;
			return factory.getFieldContextMenuProvider(source, workspace, resource, bundle, declaringClass, (FieldMember) member);
		} else if (member.isMethod()) {
			MethodContextMenuProviderFactory factory = methodContextMenuOverride != null ? methodContextMenuOverride : methodContextMenuDefault;
			return factory.getMethodContextMenuProvider(source, workspace, resource, bundle, declaringClass, (MethodMember) member);
		} else {
			throw new IllegalStateException("Unsupported member: " + member.getClass().getName());
		}
	}

	/**
	 * Delegates to {@link AnnotationContextMenuProviderFactory}.
	 *
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
	public ContextMenuProvider getAnnotationContextMenuProvider(@Nonnull ContextSource source,
																@Nonnull Workspace workspace,
																@Nonnull WorkspaceResource resource,
																@Nonnull ClassBundle<? extends ClassInfo> bundle,
																@Nonnull Annotated annotated,
																@Nonnull AnnotationInfo annotation) {
		AnnotationContextMenuProviderFactory factory = annotationContextMenuOverride != null ? annotationContextMenuOverride : annotationContextMenuDefault;
		return factory.getAnnotationContextMenuProvider(source, workspace, resource, bundle, annotated, annotation);
	}

	/**
	 * Delegates to {@link FileContextMenuProviderFactory}.
	 *
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The file to create a menu for.
	 *
	 * @return Menu provider for the file.
	 */
	@Nonnull
	public ContextMenuProvider getFileInfoContextMenuProvider(@Nonnull ContextSource source,
															  @Nonnull Workspace workspace,
															  @Nonnull WorkspaceResource resource,
															  @Nonnull FileBundle bundle,
															  @Nonnull FileInfo info) {
		FileContextMenuProviderFactory factory = fileContextMenuOverride != null ? fileContextMenuOverride : fileContextMenuDefault;
		return factory.getFileInfoContextMenuProvider(source, workspace, resource, bundle, info);
	}

	/**
	 * Delegates to {@link PackageContextMenuProviderFactory}.
	 *
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param packageName
	 * 		The full package name, separated by {@code /}.
	 *
	 * @return Menu provider for the package.
	 */
	@Nonnull
	public ContextMenuProvider getPackageContextMenuProvider(@Nonnull ContextSource source,
															 @Nonnull Workspace workspace,
															 @Nonnull WorkspaceResource resource,
															 @Nonnull ClassBundle<? extends ClassInfo> bundle,
															 @Nonnull String packageName) {
		PackageContextMenuProviderFactory factory = packageContextMenuOverride != null ? packageContextMenuOverride : packageContextMenuDefault;
		return factory.getPackageContextMenuProvider(source, workspace, resource, bundle, packageName);
	}

	/**
	 * Delegates to {@link DirectoryContextMenuProviderFactory}.
	 *
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param directoryName
	 * 		The full path of the directory.
	 *
	 * @return Menu provider for the directory.
	 */
	@Nonnull
	public ContextMenuProvider getDirectoryContextMenuProvider(@Nonnull ContextSource source,
															   @Nonnull Workspace workspace,
															   @Nonnull WorkspaceResource resource,
															   @Nonnull FileBundle bundle,
															   @Nonnull String directoryName) {
		DirectoryContextMenuProviderFactory factory = directoryContextMenuOverride != null ? directoryContextMenuOverride : directoryContextMenuDefault;
		return factory.getDirectoryContextMenuProvider(source, workspace, resource, bundle, directoryName);
	}

	/**
	 * Delegates to {@link BundleContextMenuProviderFactory}.
	 *
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		The bundle to create a menu for.
	 *
	 * @return Menu provider for the bundle.
	 */
	@Nonnull
	public ContextMenuProvider getBundleContextMenuProvider(@Nonnull ContextSource source,
															@Nonnull Workspace workspace,
															@Nonnull WorkspaceResource resource,
															@Nonnull Bundle<? extends Info> bundle) {
		BundleContextMenuProviderFactory factory = bundleContextMenuOverride != null ? bundleContextMenuOverride : bundleContextMenuDefault;
		return factory.getBundleContextMenuProvider(source, workspace, resource, bundle);
	}

	/**
	 * Delegates to {@link ResourceContextMenuProviderFactory}.
	 *
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		The resource to create a menu for.
	 *
	 * @return Menu provider for the resource.
	 */
	@Nonnull
	public ContextMenuProvider getResourceContextMenuProvider(@Nonnull ContextSource source,
															  @Nonnull Workspace workspace,
															  @Nonnull WorkspaceResource resource) {
		ResourceContextMenuProviderFactory factory = resourceContextMenuOverride != null ? resourceContextMenuOverride : resourceContextMenuDefault;
		return factory.getResourceContextMenuProvider(source, workspace, resource);
	}

	/**
	 * @return Default menu provider for classes.
	 */
	@Nonnull
	public ClassContextMenuProviderFactory getClassContextMenuDefault() {
		return classContextMenuDefault;
	}

	/**
	 * @return Default menu provider for files.
	 */
	@Nonnull
	public FileContextMenuProviderFactory getFileContextMenuDefault() {
		return fileContextMenuDefault;
	}

	/**
	 * @return Default menu provider for packages.
	 */
	@Nonnull
	public PackageContextMenuProviderFactory getPackageContextMenuDefault() {
		return packageContextMenuDefault;
	}

	/**
	 * @return Default menu provider for directories.
	 */
	@Nonnull
	public DirectoryContextMenuProviderFactory getDirectoryContextMenuDefault() {
		return directoryContextMenuDefault;
	}

	/**
	 * @return Default menu provider for bundles.
	 */
	@Nonnull
	public BundleContextMenuProviderFactory getBundleContextMenuDefault() {
		return bundleContextMenuDefault;
	}

	/**
	 * @return Default menu provider for resources.
	 */
	@Nonnull
	public ResourceContextMenuProviderFactory getResourceContextMenuDefault() {
		return resourceContextMenuDefault;
	}

	/**
	 * @return Override factory for supplying class menu providers.
	 */
	@Nullable
	public ClassContextMenuProviderFactory getClassContextMenuProviderOverride() {
		return classContextMenuOverride;
	}

	/**
	 * @param classContextMenuOverride
	 * 		Override factory for supplying class menu providers.
	 */
	public void setClassContextMenuProviderOverride(@Nullable ClassContextMenuProviderFactory classContextMenuOverride) {
		this.classContextMenuOverride = classContextMenuOverride;
	}

	/**
	 * @return Override factory for supplying file menu providers.
	 */
	@Nullable
	public FileContextMenuProviderFactory getFileContextMenuProviderOverride() {
		return fileContextMenuOverride;
	}

	/**
	 * @param fileContextMenuOverride
	 * 		Override factory for supplying file menu providers.
	 */
	public void setFileContextMenuProviderOverride(@Nullable FileContextMenuProviderFactory fileContextMenuOverride) {
		this.fileContextMenuOverride = fileContextMenuOverride;
	}

	/**
	 * @return Override factory for supplying inner class menu providers.
	 */
	@Nullable
	public InnerClassContextMenuProviderFactory getInnerClassContextMenuProviderOverride() {
		return innerClassContextMenuOverride;
	}

	/**
	 * @param innerClassContextMenuOverride
	 * 		Override factory for supplying inner class menu providers.
	 */
	public void setInnerClassContextMenuProviderOverride(@Nullable InnerClassContextMenuProviderFactory innerClassContextMenuOverride) {
		this.innerClassContextMenuOverride = innerClassContextMenuOverride;
	}

	/**
	 * @return Override factory for supplying field menu providers.
	 */
	@Nullable
	public FieldContextMenuProviderFactory getFieldContextMenuProviderOverride() {
		return fieldContextMenuOverride;
	}

	/**
	 * @param fieldContextMenuOverride
	 * 		Override factory for supplying field menu providers.
	 */
	public void setFieldContextMenuProviderOverride(@Nullable FieldContextMenuProviderFactory fieldContextMenuOverride) {
		this.fieldContextMenuOverride = fieldContextMenuOverride;
	}

	/**
	 * @return Override factory for supplying method menu providers.
	 */
	@Nullable
	public MethodContextMenuProviderFactory getMethodContextMenuProviderOverride() {
		return methodContextMenuOverride;
	}

	/**
	 * @param methodContextMenuOverride
	 * 		Override factory for supplying method menu providers.
	 */
	public void setMethodContextMenuProviderOverride(@Nullable MethodContextMenuProviderFactory methodContextMenuOverride) {
		this.methodContextMenuOverride = methodContextMenuOverride;
	}

	/**
	 * @return Override factory for supplying annotation menu providers.
	 */
	@Nullable
	public AnnotationContextMenuProviderFactory getAnnotationContextMenuProviderOverride() {
		return annotationContextMenuOverride;
	}

	/**
	 * @param annotationContextMenuOverride
	 * 		Override factory for supplying annotation menu providers.
	 */
	public void setAnnotationContextMenuProviderOverride(@Nullable AnnotationContextMenuProviderFactory annotationContextMenuOverride) {
		this.annotationContextMenuOverride = annotationContextMenuOverride;
	}

	/**
	 * @return Override factory for supplying package menu providers.
	 */
	@Nullable
	public PackageContextMenuProviderFactory getPackageContextMenuProviderOverride() {
		return packageContextMenuOverride;
	}

	/**
	 * @param packageContextMenuOverride
	 * 		Override factory for supplying package menu providers.
	 */
	public void setPackageContextMenuProviderOverride(@Nullable PackageContextMenuProviderFactory packageContextMenuOverride) {
		this.packageContextMenuOverride = packageContextMenuOverride;
	}

	/**
	 * @return Override factory for supplying directory menu providers.
	 */
	@Nullable
	public DirectoryContextMenuProviderFactory getDirectoryContextMenuProviderOverride() {
		return directoryContextMenuOverride;
	}

	/**
	 * @param directoryContextMenuOverride
	 * 		Override factory for supplying directory menu providers.
	 */
	public void setDirectoryContextMenuProviderOverride(@Nullable DirectoryContextMenuProviderFactory directoryContextMenuOverride) {
		this.directoryContextMenuOverride = directoryContextMenuOverride;
	}

	/**
	 * @return Override factory for supplying bundle menu providers.
	 */
	@Nullable
	public BundleContextMenuProviderFactory getBundleContextMenuProviderOverride() {
		return bundleContextMenuOverride;
	}

	/**
	 * @param bundleContextMenuOverride
	 * 		Override factory for supplying bundle menu providers.
	 */
	public void setBundleContextMenuProviderOverride(@Nullable BundleContextMenuProviderFactory bundleContextMenuOverride) {
		this.bundleContextMenuOverride = bundleContextMenuOverride;
	}

	/**
	 * @return Override factory for supplying resource menu providers.
	 */
	@Nullable
	public ResourceContextMenuProviderFactory getResourceContextMenuProviderOverride() {
		return resourceContextMenuOverride;
	}

	/**
	 * @param resourceContextMenuOverride
	 * 		Override factory for supplying resource menu providers.
	 */
	public void setResourceContextMenuProviderOverride(@Nullable ResourceContextMenuProviderFactory resourceContextMenuOverride) {
		this.resourceContextMenuOverride = resourceContextMenuOverride;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public ContextMenuProviderServiceConfig getServiceConfig() {
		return config;
	}
}
