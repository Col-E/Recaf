package me.coley.recaf.ui.context;

import javafx.beans.binding.StringBinding;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.mapping.MappingsAdapter;
import me.coley.recaf.search.TextMatchMode;
import me.coley.recaf.ssvm.SsvmIntegration;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.dialog.ConfirmDialog;
import me.coley.recaf.ui.dialog.SsvmInvokeCallDialog;
import me.coley.recaf.ui.dialog.SsvmOptimizeDialog;
import me.coley.recaf.ui.dialog.TextInputDialog;
import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.ui.docking.RecafDockingManager;
import me.coley.recaf.ui.docking.impl.ClassTab;
import me.coley.recaf.ui.pane.SearchPane;
import me.coley.recaf.ui.pane.assembler.AssemblerPane;
import me.coley.recaf.ui.pane.graph.MethodGraphPane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.TextDisplayUtil;
import me.coley.recaf.util.visitor.MemberCopyingVisitor;
import me.coley.recaf.util.visitor.MemberRemovingVisitor;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.util.Optional;

import static me.coley.recaf.ui.util.Menus.*;

/**
 * Context menu builder for methods.
 *
 * @author Matt Coley
 */
public class MethodContextBuilder extends MemberContextBuilder {
	private CommonClassInfo ownerInfo;
	private MethodInfo methodInfo;
	private boolean declaration;

	/**
	 * @param info
	 * 		Method information about selected item.
	 *
	 * @return Builder.
	 */
	public MethodContextBuilder setMethodInfo(MethodInfo info) {
		this.methodInfo = info;
		return this;
	}

	@Override
	public ContextMenu build() {
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(createHeader(methodInfo.getName(), Icons.getMethodIcon(methodInfo)));
		if (!declaration)
			menu.getItems().add(action("menu.goto.method", Icons.OPEN, this::openDefinition));
		if (isPrimary() && isOwnerJvmClass()) {
			// TODO: When android cases are supported, remove 'isOwnerJvmClass()' check
			Menu refactor = menu("menu.refactor");
			if (declaration) {
				menu.getItems().add(action("menu.edit.assemble.method", Icons.ACTION_EDIT, this::assemble));
				menu.getItems().add(action("menu.edit.copy", Icons.ACTION_COPY, this::copy));
				menu.getItems().add(action("menu.edit.delete", Icons.ACTION_DELETE, this::delete));
			}
			refactor.getItems().add(action("menu.refactor.rename", Icons.ACTION_EDIT, this::rename));
			menu.getItems().add(refactor);
			Menu view = menu("menu.view", Icons.EYE);
			view.getItems().add(action("menu.view.methodcfg", Icons.CHILDREN, this::graph));
			menu.getItems().add(view);
		}
		if (canUseVm()) {
			Menu vm = menu("menu.vm", Icons.VM);
			String name = methodInfo.getName();
			if (!("<clinit>".equals(name) || "<init>".equals(name))) {
				// Run should not be used on static-initializers/constructors
				vm.getItems().add(action("menu.vm.run", Icons.PLAY, this::vmRun));
			}
			vm.getItems().add(action("menu.vm.optimize", Icons.CONFIG, this::vmOptimize));
			menu.getItems().add(vm);
		}
		Menu search = menu("menu.search", Icons.ACTION_SEARCH);
		search.getItems().add(action("menu.search.references", Icons.REFERENCE, this::search));
		menu.getItems().add(search);
		return menu;
	}

	@Override
	public MethodContextBuilder setOwnerInfo(CommonClassInfo info) {
		this.ownerInfo = info;
		return this;
	}

	@Override
	public MethodContextBuilder setDeclaration(boolean declaration) {
		this.declaration = declaration;
		return this;
	}

	@Override
	public CommonClassInfo getOwnerInfo() {
		return ownerInfo;
	}

	@Override
	public void openDefinition() {
		CommonUX.openMember(ownerInfo, methodInfo);
	}

	@Override
	public void assemble() {
		String name = ownerInfo.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			if (ownerInfo instanceof ClassInfo) {
				// Open assembler
				AssemblerPane assembler = new AssemblerPane();
				assembler.setTargetMember(methodInfo);
				assembler.onUpdate(ownerInfo);
				new GenericWindow(assembler, 800, 600).show();
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
			StringBinding title = Lang.getBinding("dialog.title.copy-method");
			StringBinding header = Lang.getBinding("dialog.header.copy-method");
			TextInputDialog copyDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_COPY));
			copyDialog.setText(methodInfo.getName());
			Optional<Boolean> copyResult = copyDialog.showAndWait();
			if (copyResult.isPresent() && copyResult.get()) {
				// Create mappings and pass the class through it. This will be our copied class.
				String newName = copyDialog.getText();
				if (ownerInfo instanceof ClassInfo) {
					// Create the new class bytecode filtered through the renamer
					ClassInfo javaOwner = (ClassInfo) ownerInfo;
					ClassWriter cw = new ClassWriter(WRITE_FLAGS);
					ClassReader cr = javaOwner.getClassReader();
					cr.accept(new MemberCopyingVisitor(cw, methodInfo, newName), READ_FLAGS);
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
		String methodName = TextDisplayUtil.shortenEscapeLimit(methodInfo.getName());
		Resource resource = getContainingResource();
		if (resource != null) {
			if (Configs.display().promptDeleteItem) {
				StringBinding title = Lang.getBinding("dialog.title.delete-method");
				StringBinding header = Lang.format("dialog.header.delete-method", "\n" + methodName);
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
				MemberRemovingVisitor remover = new MemberRemovingVisitor(cw, methodInfo);
				cr.accept(remover, READ_FLAGS);
				resource.getClasses().put(ClassInfo.read(cw.toByteArray()));
				removed = remover.isRemoved();
			} else if (ownerInfo instanceof DexClassInfo) {
				// TODO: Dex member removal
				logger.warn("Android currently unsupported");
			}
			if (!removed) {
				logger.warn("Tried to delete method '{}' but failed", methodName);
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
			StringBinding title = Lang.getBinding("dialog.title.rename-method");
			StringBinding header = Lang.getBinding("dialog.header.rename-method");
			TextInputDialog renameDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_EDIT));
			renameDialog.setText(methodInfo.getName());
			Optional<Boolean> renameResult = renameDialog.showAndWait();
			if (renameResult.isPresent() && renameResult.get()) {
				if (ownerInfo instanceof ClassInfo) {
					// Create mappings to use for renaming.
					String newName = renameDialog.getText();
					MappingsAdapter mappings = new MappingsAdapter("RECAF-RENAME", false, false);
					mappings.addMethod(ownerInfo.getName(), methodInfo.getName(), methodInfo.getDescriptor(), newName);
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
				ownerInfo.getName(), methodInfo.getName(), methodInfo.getDescriptor(), TextMatchMode.EQUALS)).show();
	}

	private void graph() {
		String name = ownerInfo.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			if (ownerInfo instanceof ClassInfo) {
				// Open assembler
				String title = "CFG: " + methodInfo.getName();
				DockTab tab = RecafDockingManager.getInstance()
						.createTab(() -> new ClassTab(title, new MethodGraphPane((ClassInfo) ownerInfo, methodInfo)));
				tab.setGraphic(Icons.getMethodIcon(methodInfo));
				tab.select();
			} else if (ownerInfo instanceof DexClassInfo) {
				// TODO: Copy dex member
				logger.warn("Android currently unsupported");
			}
		} else {
			logger.error("Failed to resolve containing resource for class '{}'", name);
		}
	}

	private void vmRun() {
		SsvmIntegration ssvm = RecafUI.getController().getServices().getSsvmIntegration();
		SsvmInvokeCallDialog dialog = new SsvmInvokeCallDialog(ownerInfo, methodInfo, ssvm);
		dialog.show();
	}

	private void vmOptimize() {
		SsvmIntegration ssvm = RecafUI.getController().getServices().getSsvmIntegration();
		SsvmOptimizeDialog dialog = new SsvmOptimizeDialog(ownerInfo, methodInfo, ssvm);
		dialog.show();
	}

	private boolean canUseVm() {
		// Can only be run on JVM classes
		if (!isOwnerJvmClass())
			return false;
		// SSVM must have been initialized
		SsvmIntegration ssvm = RecafUI.getController().getServices().getSsvmIntegration();
		if (!ssvm.isInitialized())
			return false;
		// Cannot run on abstract/native methods
		int access = methodInfo.getAccess();
		return !AccessFlag.isAbstract(access) && !AccessFlag.isNative(access);
	}
}
