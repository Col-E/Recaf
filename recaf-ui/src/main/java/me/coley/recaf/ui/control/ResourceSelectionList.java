package me.coley.recaf.ui.control;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.Resources;

import java.util.ArrayList;
import java.util.List;

/**
 * Workspace creation utility. Select primary resource from a list.
 *
 * @author Matt Coley
 */
public class ResourceSelectionList extends BorderPane {
	private static final float LIST_CELL_HEIGHT = 32;
	private final ListView<Resource> resourceListView;
	private Resource selectedPrimary;

	/**
	 * Empty selection list, assuming usage of {@link #addResources(List)} later.
	 */
	public ResourceSelectionList() {
		resourceListView = new ListView<>(FXCollections.observableArrayList());
		resourceListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		resourceListView.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> selectedPrimary = newValue);
		resourceListView.setCellFactory(param -> new ResourceCell());
		resourceListView.prefHeightProperty()
				.bind(Bindings.size(resourceListView.getItems()).multiply(LIST_CELL_HEIGHT));
		setCenter(resourceListView);
	}

	/**
	 * Selection list from predefined items.
	 *
	 * @param resources
	 * 		List of resources to pick from. Must contain at least one item.
	 */
	public ResourceSelectionList(List<Resource> resources) {
		this();
		if (resources.isEmpty())
			throw new IllegalStateException("Workspace input list must take non-empty resource list!");
		addResources(resources);
		selectFirst();
	}

	/**
	 * Select the first item.
	 */
	public void selectFirst() {
		FxThreadUtil.run(() -> resourceListView.getSelectionModel().select(0));
	}

	/**
	 * @param resources
	 * 		Resources to add to the list.
	 */
	public void addResources(List<Resource> resources) {
		resourceListView.getItems().addAll(resources);
	}

	/**
	 * @return Selected primary resource.
	 */
	public Resource getSelectedPrimary() {
		return selectedPrimary;
	}

	/**
	 * @return All unselected resources.
	 */
	public List<Resource> getUnselected() {
		List<Resource> libraries = new ArrayList<>();
		for (Resource resource : resourceListView.getItems()) {
			if (!resource.equals(selectedPrimary))
				libraries.add(resource);
		}
		return libraries;
	}

	/**
	 * @return {@code true} when there is only a single resource to select from.
	 */
	public boolean isSingleResource() {
		return resourceListView.getItems().size() == 1;
	}

	/**
	 * @return Created workspace.
	 */
	public Workspace createFromSelection() {
		return new Workspace(new Resources(getSelectedPrimary(), getUnselected()));
	}

	private static class ResourceCell extends ListCell<Resource> {
		@Override
		protected void updateItem(Resource item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setGraphic(null);
				setText(null);
			} else {
				setGraphic(Icons.getResourceIcon(item));
				setText(item.getContentSource().toString());
				// Bind the cell width to the parent width.
				// This prevents the horizontal scrollbar from appearing and instead uses the "..." ellipsis.
				prefWidthProperty().bind(getListView().widthProperty().subtract(5));
				setMaxWidth(Control.USE_PREF_SIZE);
			}
		}
	}
}
