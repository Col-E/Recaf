package software.coley.recaf.ui.control.popup;

import jakarta.annotation.Nonnull;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Generic tree item selection handling popup.
 *
 * @author Matt Coley
 */
public class ItemTreeSelectionPopup<T> extends SelectionPopup<T> {
	private final TreeView<T> tree = new TreeView<>();

	/**
	 * @param consumer
	 * 		Consumer to run when user accepts selected items.
	 */
	public ItemTreeSelectionPopup(@Nonnull Consumer<List<T>> consumer) {
		setup(consumer);

		// Handle user accepting input
		tree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		tree.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				accept(consumer);
			} else if (e.getCode() == KeyCode.ESCAPE) {
				hide();
			}
		});
		tree.setCellFactory(param -> new TreeCell<>() {
			@Override
			protected void updateItem(T item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
				} else {
					if (textMapper != null) setText(textMapper.apply(item));
					if (graphicMapper != null) setGraphic(graphicMapper.apply(item));
				}
			}
		});
	}

	@Nonnull
	@Override
	protected Node getSelectionComponent() {
		return tree;
	}

	@Nonnull
	@Override
	protected List<T> adaptCurrentSelection() {
		return tree.getSelectionModel().getSelectedItems().stream()
				.map(TreeItem::getValue)
				.toList();
	}

	@Nonnull
	@Override
	protected ObservableValue<Boolean> isNullSelection() {
		return tree.getSelectionModel().selectedItemProperty().isNull();
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public ItemTreeSelectionPopup<T> withMultipleSelection() {
		tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		return this;
	}

	/**
	 * @param bundle
	 * 		Target bundle to pull packages from.
	 * @param packageConsumer
	 * 		Action to run on accepted packages.
	 *
	 * @return Popup for package names.
	 */
	@Nonnull
	public static ItemTreeSelectionPopup<String> forPackageNames(@Nonnull ClassBundle<?> bundle,
																 @Nonnull Consumer<List<String>> packageConsumer) {
		Set<String> packages = new TreeSet<>();
		packages.add(""); // Empty package
		for (String className : bundle.keySet()) {
			int slash = className.lastIndexOf('/');
			if (slash > 0) packages.add(className.substring(0, slash));
		}
		ItemTreeSelectionPopup<String> popup = new ItemTreeSelectionPopup<>(packageConsumer);
		buildTreeOfStringPaths(popup, packages);
		return popup;
	}

	/**
	 * @param bundle
	 * 		Target bundle to pull directories from.
	 * @param directoryConsumer
	 * 		Action to run on accepted directories.
	 *
	 * @return Popup for directory names.
	 */
	@Nonnull
	public static ItemTreeSelectionPopup<String> forDirectoryNames(@Nonnull FileBundle bundle,
																   @Nonnull Consumer<List<String>> directoryConsumer) {
		Set<String> directories = new TreeSet<>();
		directories.add(""); // Empty directory
		for (String fileName : bundle.keySet()) {
			int slash = fileName.lastIndexOf('/');
			if (slash > 0) directories.add(fileName.substring(0, slash));
		}
		ItemTreeSelectionPopup<String> popup = new ItemTreeSelectionPopup<>(directoryConsumer);
		buildTreeOfStringPaths(popup, directories);
		return popup;
	}

	/**
	 * Creates a tree model following a directory structure.
	 *
	 * @param popup
	 * 		Tree selection popup to build tree within.
	 * @param paths
	 * 		Paths making the tree, split by {@code '/'}.
	 */
	private static void buildTreeOfStringPaths(@Nonnull ItemTreeSelectionPopup<String> popup, @Nonnull Set<String> paths) {
		TreeItem<String> rootItem = new TreeItem<>();
		for (String pathName : paths) {
			TreeItem<String> path = rootItem;
			String[] parts = pathName.split("/", -1);
			StringBuilder pathBuilder = new StringBuilder();
			for (String part : parts) {
				pathBuilder.append(part).append('/');
				String currentPath = pathBuilder.substring(0, pathBuilder.length() - 1);
				path = getOrCreateChild(path, currentPath);
			}
		}
		popup.tree.setShowRoot(false);
		popup.tree.setRoot(rootItem);
	}

	/**
	 * @param parent
	 * 		Parent to look in, or place child into.
	 * @param value
	 * 		Value to look for in an associated child.
	 * @param <T>
	 * 		Value type.
	 *
	 * @return Tree item, a child of the parent, with the given value.
	 */
	@Nonnull
	private static <T> TreeItem<T> getOrCreateChild(@Nonnull TreeItem<T> parent, @Nonnull T value) {
		for (TreeItem<T> child : parent.getChildren())
			if (value.equals(child.getValue()))
				return child;
		TreeItem<T> child = new TreeItem<>(value);
		parent.getChildren().add(child);
		return child;
	}
}
