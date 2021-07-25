package me.coley.recaf.ui.context;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.mapping.MappingsAdapter;
import me.coley.recaf.mapping.RemappingVisitor;
import me.coley.recaf.ui.control.menu.ActionMenuItem;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;

import java.util.ArrayList;

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
	 * @param name
	 * 		Header text.
	 * @param graphic
	 * 		Header graphic.
	 *
	 * @return Header menu item.
	 */
	protected static MenuItem createHeader(String name, Node graphic) {
		MenuItem header = new MenuItem(name);
		header.getStyleClass().add("context-menu-header");
		header.setGraphic(graphic);
		header.setDisable(true);
		return header;
	}

	/**
	 * @param name
	 * 		Path name to shorten.
	 *
	 * @return Shortened name.
	 */
	protected static String shortenPath(String name) {
		int separatorIndex = name.lastIndexOf('/');
		if (separatorIndex > 0)
			name = name.substring(separatorIndex + 1);
		return name;
	}

	/**
	 * Quick utility for cutting down boilerplate for creating {@link Menu}s.
	 *
	 * @param textKey
	 * 		Translation key.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	protected static Menu menu(String textKey) {
		return menu(textKey, null);
	}

	/**
	 * Quick utility for cutting down boilerplate for creating {@link Menu}s.
	 *
	 * @param textKey
	 * 		Translation key.
	 * @param imagePath
	 * 		Path to image for menu graphic.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	protected static Menu menu(String textKey, String imagePath) {
		Node graphic = imagePath == null ? null : Icons.getIconView(imagePath);
		return new Menu(Lang.get(textKey), graphic);
	}

	/**
	 * Quick utility for cutting down boilerplate for creating {@link ActionMenuItem}s.
	 *
	 * @param textKey
	 * 		Translation key.
	 * @param runnable
	 * 		Action to run on click.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	protected static ActionMenuItem action(String textKey, Runnable runnable) {
		return action(textKey, null, runnable);
	}

	/**
	 * Quick utility for cutting down boilerplate for creating {@link ActionMenuItem}s.
	 *
	 * @param textKey
	 * 		Translation key.
	 * @param imagePath
	 * 		Path to image for menu graphic.
	 * @param runnable
	 * 		Action to run on click.
	 *
	 * @return Action menu item with behavior on-click.
	 */
	protected static ActionMenuItem action(String textKey, String imagePath, Runnable runnable) {
		Node graphic = imagePath == null ? null : Icons.getIconView(imagePath);
		return new ActionMenuItem(Lang.get(textKey), graphic, runnable);
	}

	/**
	 * Quick utility for applying mappings for operations like copy, rename, move.
	 *
	 * @param resource
	 * 		Resource to update.
	 * @param mappings
	 * 		Mappings to apply.
	 */
	protected static void applyMappings(Resource resource, MappingsAdapter mappings) {
		for (ClassInfo classInfo : new ArrayList<>(resource.getClasses().values())) {
			String originalName = classInfo.getName();
			// Apply renamer
			ClassWriter cw = new ClassWriter(WRITE_FLAGS);
			ClassReader cr = new ClassReader(classInfo.getValue());
			RemappingVisitor remapVisitor = new RemappingVisitor(cw, mappings);
			cr.accept(remapVisitor, READ_FLAGS);
			// Update class if it has any modified references
			if (remapVisitor.hasMappingBeenApplied()) {
				ClassInfo updatedInfo = ClassInfo.read(cw.toByteArray());
				resource.getClasses().put(updatedInfo);
				// Remove old classes if they have been renamed
				if (!originalName.equals(updatedInfo.getName())) {
					resource.getClasses().remove(originalName);
				}
			}
		}
	}
}
