package me.coley.recaf.ui.context;

import javafx.beans.binding.StringBinding;
import javafx.scene.control.ContextMenu;
import me.coley.recaf.RecafUI;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.dialog.ConfirmDialog;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.util.Menus;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

import static me.coley.recaf.ui.util.Menus.action;

/**
 * Context menu builder for {@link Resource}.
 *
 * @author Matt Coley
 */
public class ResourceContextBuilder extends ContextBuilder {
	private Resource resource;

	/**
	 * @param resource
	 * 		Target resource.
	 *
	 * @return Builder.
	 */
	public ResourceContextBuilder setResource(Resource resource) {
		this.resource = resource;
		return this;
	}

	@Override
	public ContextMenu build() {
		Workspace workspace = RecafUI.getController().getWorkspace();
		ContextMenu menu = new ContextMenu();

		if (!findContainerResource().equals(workspace.getResources().getPrimary())) {
			menu.getItems().add(action("menu.edit.delete", Icons.ACTION_DELETE, this::delete));
		} else {
			menu.getItems().add(Menus.action("menu.file.close", Icons.ACTION_DELETE,
					() -> RecafUI.getController().setWorkspace(null)));
		}

		return menu;
	}

	@Override
	public Resource findContainerResource() {
		return resource;
	}

	private void delete() {
		Workspace workspace = RecafUI.getController().getWorkspace();
		if (workspace != null) {
			if (Configs.display().promptDeleteItem) {
				StringBinding title = Lang.getBinding("dialog.title.delete-resource");
				StringBinding header = Lang.format("dialog.header.delete-resource",
						"\n" + resource.getContentSource().toString());
				ConfirmDialog deleteDialog = new ConfirmDialog(title, header, Icons.getImageView(Icons.ACTION_DELETE));
				boolean canRemove = deleteDialog.showAndWait().orElse(false);
				if (!canRemove) {
					return;
				}
			}
			workspace.removeLibrary(resource);
		}
	}
}
