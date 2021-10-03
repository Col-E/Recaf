package me.coley.recaf.ui.context;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.mapping.MappingsAdapter;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.dialog.ConfirmDialog;
import me.coley.recaf.ui.dialog.TextInputDialog;
import me.coley.recaf.ui.pane.SearchPane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;
import me.coley.recaf.util.MemberCopyingVisitor;
import me.coley.recaf.util.MemberRemovingVisitor;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.RuntimeResource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.util.Optional;

import static me.coley.recaf.ui.util.Menus.*;

/**
 * Context menu builder for fields.
 *
 * @author Matt Coley
 */
public class FieldContextBuilder extends ContextBuilder {
	private CommonClassInfo ownerInfo;
	private FieldInfo fieldInfo;

	/**
	 * @param info
	 * 		Class information about selected item's defining class.
	 *
	 * @return Builder.
	 */
	public FieldContextBuilder setOwnerInfo(CommonClassInfo info) {
		this.ownerInfo = info;
		return this;
	}

	/**
	 * @param info
	 * 		field information about selected item.
	 *
	 * @return Builder.
	 */
	public FieldContextBuilder setFieldInfo(FieldInfo info) {
		this.fieldInfo = info;
		return this;
	}

	@Override
	public ContextMenu build() {
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(createHeader(fieldInfo.getName(), Icons.getFieldIcon(fieldInfo)));
		menu.getItems().add(action("menu.goto.field", Icons.OPEN, this::openField));
		if (isPrimary()) {
			Menu refactor = menu("menu.refactor");
			menu.getItems().add(action("menu.edit.copy", Icons.ACTION_COPY, this::copy));
			menu.getItems().add(action("menu.edit.delete", Icons.ACTION_DELETE, this::delete));
			refactor.getItems().add(action("menu.refactor.rename", Icons.ACTION_EDIT, this::rename));
			menu.getItems().add(refactor);
		}
		Menu search = menu("menu.search", Icons.ACTION_SEARCH);
		search.getItems().add(action("menu.search.references", Icons.REFERENCE, this::search));
		menu.getItems().add(search);
		return menu;
	}

	@Override
	public Resource findContainerResource() {
		String name = ownerInfo.getName();
		Workspace workspace = RecafUI.getController().getWorkspace();
		Resource resource = workspace.getResources().getContainingForClass(name);
		if (resource == null)
			resource = workspace.getResources().getContainingForDexClass(name);
		if (resource == null)
			logger.warn("Could not find container resource for class {}", name);
		return null;
	}

	private void openField() {
		CommonUX.openMember(ownerInfo, fieldInfo);
	}

	private void copy() {
		String name = ownerInfo.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			String title = Lang.get("dialog.title.copy-field");
			String header = Lang.get("dialog.header.copy-field");
			TextInputDialog copyDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_COPY));
			copyDialog.setName(fieldInfo.getName());
			Optional<Boolean> copyResult = copyDialog.showAndWait();
			if (copyResult.isPresent() && copyResult.get()) {
				// Create mappings and pass the class through it. This will be our copied class.
				String newName = copyDialog.getName();
				if (ownerInfo instanceof ClassInfo) {
					// Create the new class bytecode filtered through the renamer
					ClassInfo javaOwner = (ClassInfo) ownerInfo;
					ClassWriter cw = new ClassWriter(WRITE_FLAGS);
					ClassReader cr = new ClassReader(javaOwner.getValue());
					cr.accept(new MemberCopyingVisitor(cw, fieldInfo, newName), READ_FLAGS);
					resource.getClasses().put(ClassInfo.read(cw.toByteArray()));
				} else if (ownerInfo instanceof DexClassInfo) {
					// TODO: Copy dex member
					logger.warn("Android currently unsupported");
				}
			}
		} else {
			logger.error("Failed to resolve containing resource for class '{}'", name);
		}
	}

	private void delete() {
		String name = ownerInfo.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			if (Configs.display().promptDeleteItem) {
				String title = Lang.get("dialog.title.delete-field");
				String header = String.format(Lang.get("dialog.header.delete-field"), "\n" + name);
				ConfirmDialog deleteDialog = new ConfirmDialog(title, header, Icons.getImageView(Icons.ACTION_DELETE));
				boolean canRemove = deleteDialog.showAndWait().orElse(false);
				if (!canRemove) {
					return;
				}
			}
			boolean removed = false;
			if (ownerInfo instanceof ClassInfo) {
				ClassInfo javaOwner = (ClassInfo) ownerInfo;
				ClassWriter cw = new ClassWriter(WRITE_FLAGS);
				ClassReader cr = new ClassReader(javaOwner.getValue());
				MemberRemovingVisitor remover = new MemberRemovingVisitor(cw, fieldInfo);
				cr.accept(remover, READ_FLAGS);
				resource.getClasses().put(ClassInfo.read(cw.toByteArray()));
				removed = remover.isRemoved();
			} else if (ownerInfo instanceof DexClassInfo) {
				// TODO: Dex member removal
				logger.warn("Android currently unsupported");
			}
			if (!removed) {
				logger.warn("Tried to delete field '{}' but failed", name);
			}
		} else {
			logger.error("Failed to resolve containing resource for class '{}'", name);
		}
	}

	private void rename() {
		String name = ownerInfo.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			String title = Lang.get("dialog.title.rename-field");
			String header = Lang.get("dialog.header.rename-field");
			TextInputDialog renameDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_EDIT));
			renameDialog.setName(fieldInfo.getName());
			Optional<Boolean> renameResult = renameDialog.showAndWait();
			if (renameResult.isPresent() && renameResult.get()) {
				if (ownerInfo instanceof ClassInfo) {
					// Create mappings to use for renaming.
					String newName = renameDialog.getName();
					MappingsAdapter mappings = new MappingsAdapter("RECAF-RENAME", true, false);
					mappings.addField(ownerInfo.getName(), fieldInfo.getName(), fieldInfo.getDescriptor(), newName);
					// Update all classes in the resource
					applyMappings(resource, mappings);
				} else if (ownerInfo instanceof DexClassInfo) {
					// TODO: Dex mappings
					logger.warn("Android currently unsupported");
				}
			}
		} else {
			logger.error("Failed to resolve containing resource for class '{}'", name);
		}
	}

	private void search() {
		new GenericWindow(SearchPane.createReferenceSearch(
				ownerInfo.getName(), fieldInfo.getName(), fieldInfo.getDescriptor())).show();
	}
}
