package me.coley.recaf.ui.context;

import javafx.beans.binding.StringBinding;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.mapping.MappingsAdapter;
import me.coley.recaf.search.TextMatchMode;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.dialog.ConfirmDialog;
import me.coley.recaf.ui.dialog.TextInputDialog;
import me.coley.recaf.ui.pane.SearchPane;
import me.coley.recaf.ui.pane.assembler.AssemblerPane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;
import me.coley.recaf.util.TextDisplayUtil;
import me.coley.recaf.util.visitor.MemberCopyingVisitor;
import me.coley.recaf.util.visitor.MemberRemovingVisitor;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.util.Optional;

import static me.coley.recaf.ui.util.Menus.*;

/**
 * Context menu builder for fields.
 *
 * @author Matt Coley
 */
public class FieldContextBuilder extends MemberContextBuilder {
	private CommonClassInfo ownerInfo;
	private FieldInfo fieldInfo;
	private boolean declaration;

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
		if (!declaration)
			menu.getItems().add(action("menu.goto.field", Icons.OPEN, this::openDefinition));
		if (isPrimary() && isOwnerJvmClass()) {
			// TODO: When android cases are supported, remove 'isOwnerJvmClass()' check
			Menu refactor = menu("menu.refactor");
			if (declaration) {
				menu.getItems().add(action("menu.edit.assemble.field", Icons.ACTION_EDIT, this::assemble));
				menu.getItems().add(action("menu.edit.copy", Icons.ACTION_COPY, this::copy));
				menu.getItems().add(action("menu.edit.delete", Icons.ACTION_DELETE, this::delete));
			}
			refactor.getItems().add(action("menu.refactor.rename", Icons.ACTION_EDIT, this::rename));
			menu.getItems().add(refactor);
		}
		Menu search = menu("menu.search", Icons.ACTION_SEARCH);
		search.getItems().add(action("menu.search.references", Icons.REFERENCE, this::search));
		menu.getItems().add(search);
		return menu;
	}

	@Override
	public FieldContextBuilder setOwnerInfo(CommonClassInfo info) {
		this.ownerInfo = info;
		return this;
	}

	@Override
	public FieldContextBuilder setDeclaration(boolean declaration) {
		this.declaration = declaration;
		return this;
	}

	@Override
	public CommonClassInfo getOwnerInfo() {
		return ownerInfo;
	}

	@Override
	public void openDefinition() {
		CommonUX.openMember(ownerInfo, fieldInfo);
	}

	@Override
	public void assemble() {
		String name = ownerInfo.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			if (ownerInfo instanceof ClassInfo) {
				// Open assembler
				AssemblerPane assembler = new AssemblerPane();
				assembler.setTargetMember(fieldInfo);
				assembler.onUpdate(ownerInfo);
				new GenericWindow(assembler, 800, 300).show();
			} else if (ownerInfo instanceof DexClassInfo) {
				// TODO: Copy dex member
				logger.warn("Android currently unsupported");
			}
		} else {
			logger.error("Failed to resolve containing resource for class '{}'", name);
		}
	}

	@Override
	public void copy() {
		String name = ownerInfo.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			StringBinding title = Lang.getBinding("dialog.title.copy-field");
			StringBinding header = Lang.getBinding("dialog.header.copy-field");
			TextInputDialog copyDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_COPY));
			copyDialog.setText(fieldInfo.getName());
			Optional<Boolean> copyResult = copyDialog.showAndWait();
			if (copyResult.isPresent() && copyResult.get()) {
				// Create mappings and pass the class through it. This will be our copied class.
				String newName = copyDialog.getText();
				if (ownerInfo instanceof ClassInfo) {
					// Create the new class bytecode filtered through the renamer
					ClassInfo javaOwner = (ClassInfo) ownerInfo;
					ClassWriter cw = new ClassWriter(WRITE_FLAGS);
					ClassReader cr = javaOwner.getClassReader();
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

	@Override
	public void delete() {
		String ownerName = TextDisplayUtil.shortenEscapeLimit(ownerInfo.getName());
		String fieldName = TextDisplayUtil.shortenEscapeLimit(fieldInfo.getName());
		Resource resource = getContainingResource();
		if (resource != null) {
			if (Configs.display().promptDeleteItem) {
				StringBinding title = Lang.getBinding("dialog.title.delete-field");
				StringBinding header = Lang.format("dialog.header.delete-field", "\n" + fieldName);
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
				ClassReader cr = javaOwner.getClassReader();
				MemberRemovingVisitor remover = new MemberRemovingVisitor(cw, fieldInfo);
				cr.accept(remover, READ_FLAGS);
				resource.getClasses().put(ClassInfo.read(cw.toByteArray()));
				removed = remover.isRemoved();
			} else if (ownerInfo instanceof DexClassInfo) {
				// TODO: Dex member removal
				logger.warn("Android currently unsupported");
			}
			if (!removed) {
				logger.warn("Tried to delete field '{}' but failed", fieldName);
			}
		} else {
			logger.error("Failed to resolve containing resource for class '{}'", ownerName);
		}
	}

	@Override
	public void rename() {
		String name = ownerInfo.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			StringBinding title = Lang.getBinding("dialog.title.rename-field");
			StringBinding header = Lang.getBinding("dialog.header.rename-field");
			TextInputDialog renameDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_EDIT));
			renameDialog.setText(fieldInfo.getName());
			Optional<Boolean> renameResult = renameDialog.showAndWait();
			if (renameResult.isPresent() && renameResult.get()) {
				if (ownerInfo instanceof ClassInfo) {
					// Create mappings to use for renaming.
					String newName = renameDialog.getText();
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

	@Override
	public void search() {
		new GenericWindow(SearchPane.createReferenceSearch(
				ownerInfo.getName(), fieldInfo.getName(), fieldInfo.getDescriptor(), TextMatchMode.EQUALS)).show();
	}
}
