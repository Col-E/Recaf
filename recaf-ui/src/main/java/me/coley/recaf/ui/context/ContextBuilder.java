package me.coley.recaf.ui.context;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Tab;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.*;
import me.coley.recaf.mapping.MappingUtils;
import me.coley.recaf.mapping.Mappings;
import me.coley.recaf.mapping.data.ClassMapping;
import me.coley.recaf.mapping.impl.IntermediateMappings;
import me.coley.recaf.ui.ClassView;
import me.coley.recaf.ui.pane.DockingRootPane;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Base for context menu building.
 *
 * @author Matt Coley
 */
public abstract class ContextBuilder {
	protected static final int WRITE_FLAGS = 0;
	protected static final int READ_FLAGS = 0;
	protected static final Logger logger = Logging.get(ContextBuilder.class);
	protected ContextSource where;
	private Resource containingResource;

	protected ContextBuilder() {
		// Must construct in implementation
	}

	/**
	 * @param info
	 * 		Class info to operate on.
	 *
	 * @return Builder.
	 */
	public static ClassContextBuilder forClass(ClassInfo info) {
		return new ClassContextBuilder().setClassInfo(info);
	}

	/**
	 * @param info
	 * 		Class info to operate on.
	 *
	 * @return Builder.
	 */
	public static DexClassContextBuilder forDexClass(DexClassInfo info) {
		return new DexClassContextBuilder().setClassInfo(info);
	}

	/**
	 * @param ownerInfo
	 * 		Owner info of the class defining the field.
	 * @param fieldInfo
	 * 		Field info to operate on.
	 *
	 * @return Builder.
	 */
	public static FieldContextBuilder forField(CommonClassInfo ownerInfo, FieldInfo fieldInfo) {
		return new FieldContextBuilder().setOwnerInfo(ownerInfo).setFieldInfo(fieldInfo);
	}

	/**
	 * @param ownerInfo
	 * 		Owner info of the class defining the method.
	 * @param methodInfo
	 * 		Method info to operate on.
	 *
	 * @return Builder.
	 */
	public static MethodContextBuilder forMethod(CommonClassInfo ownerInfo, MethodInfo methodInfo) {
		return new MethodContextBuilder().setOwnerInfo(ownerInfo).setMethodInfo(methodInfo);
	}

	/**
	 * @param packageName
	 * 		Name of package to operate on.
	 *
	 * @return Builder.
	 */
	public static PackageContextBuilder forPackage(String packageName) {
		return new PackageContextBuilder().setPackageName(packageName);
	}

	/**
	 * @param info
	 * 		File info to operate on.
	 *
	 * @return Builder.
	 */
	public static FileContextBuilder forFile(FileInfo info) {
		return new FileContextBuilder().setFileInfo(info);
	}

	/**
	 * @param directoryName
	 * 		Name of directory to operate on.
	 *
	 * @return Builder.
	 */
	public static DirectoryContextBuilder forDirectory(String directoryName) {
		return new DirectoryContextBuilder().setDirectoryName(directoryName);
	}

	/**
	 * @param resource
	 * 		Resource to operate on.
	 *
	 * @return Builder.
	 */
	public static ResourceContextBuilder forResource(Resource resource) {
		return new ResourceContextBuilder().setResource(resource);
	}

	/**
	 * @param resource
	 * 		Resource of the item the context menu is being built for.
	 * @param <C>
	 * 		Context builder implementation tye.
	 *
	 * @return Builder.
	 */
	@SuppressWarnings("unchecked")
	public <C extends ContextBuilder> C withResource(Resource resource) {
		containingResource = resource;
		return (C) this;
	}

	/**
	 * @return Build the context menu.
	 */
	public abstract ContextMenu build();

	/**
	 * @return The containing resource of the item being operated on.
	 */
	protected abstract Resource findContainerResource();

	/**
	 * @return The containing resource of the item being operated on.
	 */
	public Resource getContainingResource() {
		if (containingResource == null) {
			containingResource = findContainerResource();
		}
		return containingResource;
	}

	/**
	 * @param where
	 * 		UI location where the context menu is being created from.
	 * @param <T>
	 * 		Implied type of the current builder. Allows builder chains to not lose their type.
	 *
	 * @return Builder.
	 */
	@SuppressWarnings("unchecked")
	public <T extends ContextBuilder> T setWhere(ContextSource where) {
		this.where = where;
		return (T) this;
	}

	/**
	 * @return {@code true} when the {@link #getContainingResource() containing resource} is the
	 * current workspace's primary resource.
	 */
	protected boolean isPrimary() {
		Workspace workspace = RecafUI.getController().getWorkspace();
		if (workspace != null) {
			return workspace.getResources().getPrimary().equals(getContainingResource());
		}
		return false;
	}

	/**
	 * Quick utility for applying mappings for operations like copy, rename, move.
	 *
	 * @param resource
	 * 		Resource to update.
	 * @param mappings
	 * 		Mappings to apply.
	 */
	protected static void applyMappings(Resource resource, Mappings mappings) {
		List<Tab> openedClassTabs = RecafUI.getWindows().getMainWindow().getDockingRootPane().getAllTabs().stream()
				.filter(tab -> tab.getContent() instanceof ClassView)
				.collect(Collectors.toList());
		BiConsumer<List<Tab>, Mappings> postMappingAction = ContextBuilder::handleRename;
		// Apply the mappings
		MappingUtils.applyMappings(READ_FLAGS, WRITE_FLAGS, RecafUI.getController(), resource, mappings);
		// Run the post-mapping action
		postMappingAction.accept(openedClassTabs, mappings);
	}

	/**
	 * @param openedClassTabs
	 * 		Tabs that were open prior to the mapping application.
	 * 		These all have {@link ClassView} as their content.
	 * @param mappings
	 * 		Mappings applied.
	 */
	private static void handleRename(List<Tab> openedClassTabs, Mappings mappings) {
		// Skip if mapping doesn't support intermediate representation
		if (!mappings.supportsExportIntermediate())
			return;
		IntermediateMappings intermediate = mappings.exportIntermediate();
		Map<String, ClassMapping> mappedClasses = intermediate.getClasses();
		// Skip if no classes were mapped.
		if (mappedClasses.isEmpty())
			return;
		// Re-open the mapped classes as their new name, attempt to reset scroll position and such.
		Map<String, Tab> classToOpenedTab = openedClassTabs.stream()
				.collect(Collectors.toMap(
						tab -> ((ClassView) tab.getContent()).getCurrentClassInfo().getName(),
						Function.identity())
				);
		mappedClasses.forEach((oldName, classMapping) -> {
			Tab tab = classToOpenedTab.get(oldName);
			if (tab == null)
				return;
			// Get old content of tab
			ClassView oldView = (ClassView) tab.getContent();
			tab.setContent(null);
			// Open it in a new tab
			Workspace workspace = RecafUI.getController().getWorkspace();
			CommonClassInfo newClassInfo = workspace.getResources().getClass(classMapping.getNewName());
			DockingRootPane docking = RecafUI.getWindows().getMainWindow().getDockingRootPane();
			docking.openInfoTab(newClassInfo, () -> oldView);
			oldView.onUpdate(newClassInfo);
		});
	}
}
