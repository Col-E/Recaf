package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.AssemblerPathData;
import software.coley.recaf.services.Service;
import software.coley.recaf.ui.control.tree.WorkspaceTreeCell;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Provides support for providing contextual right click menus for a variety of item types.
 * For instance, the menus of {@link WorkspaceTreeCell} instances.
 * <br>
 * The menus displayed in the UI can be adapted out by supplying your own
 * {@link ContextMenuAdapter} instances via:
 * <ul>
 *     <li>{@link #addClassContextMenuAdapter(ClassContextMenuAdapter)}</li>
 *     <li>{@link #addFileContextMenuAdapter(FileContextMenuAdapter)}</li>
 *     <li>{@link #addInnerClassContextMenuAdapter(InnerClassContextMenuAdapter)}</li>
 *     <li>{@link #addFieldContextMenuAdapter(FieldContextMenuAdapter)}</li>
 *     <li>{@link #addMethodContextMenuAdapter(MethodContextMenuAdapter)}</li>
 *     <li>{@link #addAnnotationContextMenuAdapter(AnnotationContextMenuAdapter)}</li>
 *     <li>{@link #addPackageContextMenuAdapter(PackageContextMenuAdapter)}</li>
 *     <li>{@link #addDirectoryContextMenuAdapter(DirectoryContextMenuAdapter)}</li>
 *     <li>{@link #addBundleContextMenuAdapter(BundleContextMenuAdapter)}</li>
 *     <li>{@link #addResourceContextMenuAdapter(ResourceContextMenuAdapter)}</li>
 *     <li>{@link #addAssemblerContextMenuAdapter(AssemblerContextMenuAdapter)}</li>
 * </ul>
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ContextMenuProviderService implements Service {
	public static final String SERVICE_ID = "cell-menus";
	private final ContextMenuProviderServiceConfig config;
	// Adapters
	private final List<ClassContextMenuAdapter> classContextMenuAdapters = new ArrayList<>();
	private final List<FileContextMenuAdapter> fileContextMenuAdapters = new ArrayList<>();
	private final List<InnerClassContextMenuAdapter> innerClassContextMenuAdapters = new ArrayList<>();
	private final List<FieldContextMenuAdapter> fieldContextMenuAdapters = new ArrayList<>();
	private final List<MethodContextMenuAdapter> methodContextMenuAdapters = new ArrayList<>();
	private final List<AnnotationContextMenuAdapter> annotationContextMenuAdapters = new ArrayList<>();
	private final List<PackageContextMenuAdapter> packageContextMenuAdapters = new ArrayList<>();
	private final List<DirectoryContextMenuAdapter> directoryContextMenuAdapters = new ArrayList<>();
	private final List<BundleContextMenuAdapter> bundleContextMenuAdapters = new ArrayList<>();
	private final List<ResourceContextMenuAdapter> resourceContextMenuAdapters = new ArrayList<>();
	private final List<AssemblerContextMenuAdapter> assemblerContextMenuAdapters = new ArrayList<>();
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
	private final AssemblerContextMenuProviderFactory assemblerContextMenuDefault;

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
	                                  @Nonnull ResourceContextMenuProviderFactory resourceContextMenuDefault,
	                                  @Nonnull AssemblerContextMenuProviderFactory assemblerContextMenuDefault) {
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
		this.assemblerContextMenuDefault = assemblerContextMenuDefault;
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
		ContextMenuProvider provider = classContextMenuDefault.getJvmClassInfoContextMenuProvider(source, workspace, resource, bundle, info);
		provider = adapt(provider, classContextMenuAdapters, (adapter, menu) -> adapter.adaptJvmClassMenu(menu, source, workspace, resource, bundle, info));
		return provider;
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
		ContextMenuProvider provider = classContextMenuDefault.getAndroidClassInfoContextMenuProvider(source, workspace, resource, bundle, info);
		provider = adapt(provider, classContextMenuAdapters, (adapter, menu) -> adapter.adaptAndroidClassMenu(menu, source, workspace, resource, bundle, info));
		return provider;
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
		ContextMenuProvider provider = innerClassContextMenuDefault.getInnerClassInfoContextMenuProvider(source, workspace, resource, bundle, outerClass, inner);
		provider = adapt(provider, innerClassContextMenuAdapters, (adapter, menu) -> adapter.adaptInnerClassInfoContextMenu(menu, source, workspace, resource, bundle, outerClass, inner));
		return provider;
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
			FieldMember field = (FieldMember) member;
			ContextMenuProvider provider = fieldContextMenuDefault.getFieldContextMenuProvider(source, workspace, resource, bundle, declaringClass, field);
			provider = adapt(provider, fieldContextMenuAdapters, (adapter, menu) -> adapter.adaptFieldContextMenu(menu, source, workspace, resource, bundle, declaringClass, field));
			return provider;
		} else if (member.isMethod()) {
			MethodMember method = (MethodMember) member;
			ContextMenuProvider provider = methodContextMenuDefault.getMethodContextMenuProvider(source, workspace, resource, bundle, declaringClass, method);
			provider = adapt(provider, methodContextMenuAdapters, (adapter, menu) -> adapter.adaptMethodContextMenu(menu, source, workspace, resource, bundle, declaringClass, method));
			return provider;
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
		ContextMenuProvider provider = annotationContextMenuDefault.getAnnotationContextMenuProvider(source, workspace, resource, bundle, annotated, annotation);
		provider = adapt(provider, annotationContextMenuAdapters, (adapter, menu) -> adapter.adaptAnnotationContextMenu(menu, source, workspace, resource, bundle, annotated, annotation));
		return provider;
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
		ContextMenuProvider provider = fileContextMenuDefault.getFileInfoContextMenuProvider(source, workspace, resource, bundle, info);
		provider = adapt(provider, fileContextMenuAdapters, (adapter, menu) -> adapter.adaptFileInfoContextMenu(menu, source, workspace, resource, bundle, info));
		return provider;
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
		ContextMenuProvider provider = packageContextMenuDefault.getPackageContextMenuProvider(source, workspace, resource, bundle, packageName);
		provider = adapt(provider, packageContextMenuAdapters, (adapter, menu) -> adapter.adaptPackageContextMenu(menu, source, workspace, resource, bundle, packageName));
		return provider;
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
		ContextMenuProvider provider = directoryContextMenuDefault.getDirectoryContextMenuProvider(source, workspace, resource, bundle, directoryName);
		provider = adapt(provider, directoryContextMenuAdapters, (adapter, menu) -> adapter.adaptDirectoryContextMenu(menu, source, workspace, resource, bundle, directoryName));
		return provider;
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
		ContextMenuProvider provider = bundleContextMenuDefault.getBundleContextMenuProvider(source, workspace, resource, bundle);
		provider = adapt(provider, bundleContextMenuAdapters, (adapter, menu) -> adapter.adaptBundleContextMenu(menu, source, workspace, resource, bundle));
		return provider;
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
		ContextMenuProvider provider = resourceContextMenuDefault.getResourceContextMenuProvider(source, workspace, resource);
		provider = adapt(provider, resourceContextMenuAdapters, (adapter, menu) -> adapter.adaptResourceContextMenu(menu, source, workspace, resource));
		return provider;
	}

	/**
	 * Delegates to {@link AssemblerContextMenuProviderFactory}.
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
	 * @param assemblerData
	 * 		The assembler data to create a menu for.
	 *
	 * @return Menu provider for the resource.
	 */
	@Nonnull
	public ContextMenuProvider getAssemblerContextMenuProvider(@Nonnull ContextSource source,
	                                                           @Nonnull Workspace workspace,
	                                                           @Nonnull WorkspaceResource resource,
	                                                           @Nonnull ClassBundle<? extends ClassInfo> bundle,
	                                                           @Nonnull ClassInfo declaringClass,
	                                                           @Nonnull AssemblerPathData assemblerData) {
		ContextMenuProvider provider = assemblerContextMenuDefault.getAssemblerMenuProvider(source, workspace, resource, bundle, declaringClass, assemblerData);
		provider = adapt(provider, assemblerContextMenuAdapters, (adapter, menu) -> adapter.adaptAssemblerMenu(menu, source, workspace, resource, bundle, declaringClass, assemblerData));
		return provider;
	}

	/**
	 * @param adapter
	 * 		Adapter to register for modifying class context menus.
	 *
	 * @return {@code true} when the adapter was added. {@link false} when the adapter has already been added.
	 */
	public boolean addClassContextMenuAdapter(@Nonnull ClassContextMenuAdapter adapter) {
		return PrioritySortable.add(classContextMenuAdapters, adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to remove.
	 *
	 * @return {@code true} when the adapter was removed. {@link false} when the adapter was not previously registered.
	 */
	public boolean removeClassContextMenuAdapter(@Nonnull ClassContextMenuAdapter adapter) {
		return classContextMenuAdapters.remove(adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to register for modifying file context menus.
	 *
	 * @return {@code true} when the adapter was added. {@link false} when the adapter has already been added.
	 */
	public boolean addFileContextMenuAdapter(@Nonnull FileContextMenuAdapter adapter) {
		return PrioritySortable.add(fileContextMenuAdapters, adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to remove.
	 *
	 * @return {@code true} when the adapter was removed. {@link false} when the adapter was not previously registered.
	 */
	public boolean removeFileContextMenuAdapter(@Nonnull FileContextMenuAdapter adapter) {
		return fileContextMenuAdapters.remove(adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to register for modifying inner-class context menus.
	 *
	 * @return {@code true} when the adapter was added. {@link false} when the adapter has already been added.
	 */
	public boolean addInnerClassContextMenuAdapter(@Nonnull InnerClassContextMenuAdapter adapter) {
		return PrioritySortable.add(innerClassContextMenuAdapters, adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to remove.
	 *
	 * @return {@code true} when the adapter was removed. {@link false} when the adapter was not previously registered.
	 */
	public boolean removeInnerClassContextMenuAdapter(@Nonnull InnerClassContextMenuAdapter adapter) {
		return innerClassContextMenuAdapters.remove(adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to register for modifying field context menus.
	 *
	 * @return {@code true} when the adapter was added. {@link false} when the adapter has already been added.
	 */
	public boolean addFieldContextMenuAdapter(@Nonnull FieldContextMenuAdapter adapter) {
		return PrioritySortable.add(fieldContextMenuAdapters, adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to remove.
	 *
	 * @return {@code true} when the adapter was removed. {@link false} when the adapter was not previously registered.
	 */
	public boolean removeFieldContextMenuAdapter(@Nonnull FieldContextMenuAdapter adapter) {
		return fieldContextMenuAdapters.remove(adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to register for modifying method context menus.
	 *
	 * @return {@code true} when the adapter was added. {@link false} when the adapter has already been added.
	 */
	public boolean addMethodContextMenuAdapter(@Nonnull MethodContextMenuAdapter adapter) {
		return PrioritySortable.add(methodContextMenuAdapters, adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to remove.
	 *
	 * @return {@code true} when the adapter was removed. {@link false} when the adapter was not previously registered.
	 */
	public boolean removeMethodContextMenuAdapter(@Nonnull MethodContextMenuAdapter adapter) {
		return methodContextMenuAdapters.remove(adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to register for modifying annotation context menus.
	 *
	 * @return {@code true} when the adapter was added. {@link false} when the adapter has already been added.
	 */
	public boolean addAnnotationContextMenuAdapter(@Nonnull AnnotationContextMenuAdapter adapter) {
		return PrioritySortable.add(annotationContextMenuAdapters, adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to remove.
	 *
	 * @return {@code true} when the adapter was removed. {@link false} when the adapter was not previously registered.
	 */
	public boolean removeAnnotationContextMenuAdapter(@Nonnull AnnotationContextMenuAdapter adapter) {
		return annotationContextMenuAdapters.remove(adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to register for modifying package context menus.
	 *
	 * @return {@code true} when the adapter was added. {@link false} when the adapter has already been added.
	 */
	public boolean addPackageContextMenuAdapter(@Nonnull PackageContextMenuAdapter adapter) {
		return PrioritySortable.add(packageContextMenuAdapters, adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to remove.
	 *
	 * @return {@code true} when the adapter was removed. {@link false} when the adapter was not previously registered.
	 */
	public boolean removePackageContextMenuAdapter(@Nonnull PackageContextMenuAdapter adapter) {
		return packageContextMenuAdapters.remove(adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to register for modifying directory context menus.
	 *
	 * @return {@code true} when the adapter was added. {@link false} when the adapter has already been added.
	 */
	public boolean addDirectoryContextMenuAdapter(@Nonnull DirectoryContextMenuAdapter adapter) {
		return PrioritySortable.add(directoryContextMenuAdapters, adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to remove.
	 *
	 * @return {@code true} when the adapter was removed. {@link false} when the adapter was not previously registered.
	 */
	public boolean removeDirectoryContextMenuAdapter(@Nonnull DirectoryContextMenuAdapter adapter) {
		return directoryContextMenuAdapters.remove(adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to register for modifying bundle context menus.
	 *
	 * @return {@code true} when the adapter was added. {@link false} when the adapter has already been added.
	 */
	public boolean addBundleContextMenuAdapter(@Nonnull BundleContextMenuAdapter adapter) {
		return PrioritySortable.add(bundleContextMenuAdapters, adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to remove.
	 *
	 * @return {@code true} when the adapter was removed. {@link false} when the adapter was not previously registered.
	 */
	public boolean removeBundleContextMenuAdapter(@Nonnull BundleContextMenuAdapter adapter) {
		return bundleContextMenuAdapters.remove(adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to register for modifying resource context menus.
	 *
	 * @return {@code true} when the adapter was added. {@link false} when the adapter has already been added.
	 */
	public boolean addResourceContextMenuAdapter(@Nonnull ResourceContextMenuAdapter adapter) {
		return PrioritySortable.add(resourceContextMenuAdapters, adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to remove.
	 *
	 * @return {@code true} when the adapter was removed. {@link false} when the adapter was not previously registered.
	 */
	public boolean removeResourceContextMenuAdapter(@Nonnull ResourceContextMenuAdapter adapter) {
		return resourceContextMenuAdapters.remove(adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to register for modifying assembler context menus.
	 *
	 * @return {@code true} when the adapter was added. {@link false} when the adapter has already been added.
	 */
	public boolean addAssemblerContextMenuAdapter(@Nonnull AssemblerContextMenuAdapter adapter) {
		return PrioritySortable.add(assemblerContextMenuAdapters, adapter);
	}

	/**
	 * @param adapter
	 * 		Adapter to remove.
	 *
	 * @return {@code true} when the adapter was removed. {@link false} when the adapter was not previously registered.
	 */
	public boolean removeAssemblerContextMenuAdapter(@Nonnull AssemblerContextMenuAdapter adapter) {
		return assemblerContextMenuAdapters.remove(adapter);
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
	 * @return Default menu provider for inner classes.
	 */
	@Nonnull
	public InnerClassContextMenuProviderFactory getInnerClassContextMenuDefault() {
		return innerClassContextMenuDefault;
	}

	/**
	 * @return Default menu provider for fields.
	 */
	@Nonnull
	public FieldContextMenuProviderFactory getFieldContextMenuDefault() {
		return fieldContextMenuDefault;
	}

	/**
	 * @return Default menu provider for methods.
	 */
	@Nonnull
	public MethodContextMenuProviderFactory getMethodContextMenuDefault() {
		return methodContextMenuDefault;
	}

	/**
	 * @return Default menu provider for annotations.
	 */
	@Nonnull
	public AnnotationContextMenuProviderFactory getAnnotationContextMenuDefault() {
		return annotationContextMenuDefault;
	}

	/**
	 * @return Default menu provider for assembler data.
	 */
	@Nonnull
	public AssemblerContextMenuProviderFactory getAssemblerContextMenuDefault() {
		return assemblerContextMenuDefault;
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

	@Nonnull
	private static <T extends ContextMenuAdapter> ContextMenuProvider adapt(@Nonnull ContextMenuProvider provider,
	                                                                        @Nonnull Collection<T> adapters,
	                                                                        @Nonnull BiConsumer<T, ContextMenu> adapterConsumer) {
		for (T adapter : adapters) {
			ContextMenuProvider currentProvider = provider;
			provider = () -> {
				ContextMenu menu = currentProvider.makeMenu();
				if (menu != null)
					adapterConsumer.accept(adapter, menu);
				return menu;
			};
		}
		return provider;
	}
}
