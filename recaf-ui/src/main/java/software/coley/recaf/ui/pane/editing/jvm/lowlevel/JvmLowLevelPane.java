package software.coley.recaf.ui.pane.editing.jvm.lowlevel;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import software.coley.cafedude.InvalidClassException;
import software.coley.cafedude.classfile.ClassFile;
import software.coley.cafedude.classfile.attribute.InnerClassesAttribute;
import software.coley.cafedude.io.ClassFileReader;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.ClassNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.config.MemberDisplayFormatConfig;
import software.coley.recaf.ui.control.tree.TreeItems;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.NodeEvents;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.function.Predicate;

/**
 * Displays a {@link JvmClassInfo} as a {@link TreeView} that closely aligns to the class file specification.
 *
 * @author Matt Coley
 */
@Dependent
public class JvmLowLevelPane extends BorderPane implements ClassNavigable, UpdatableNavigable {
	private static final Logger logger = Logging.get(JvmLowLevelPane.class);
	private final JvmLowLevelTreeBuilder treeBuilder;
	private ClassPathNode path;
	@Nullable
	private TreeView<ClassElement> tree;
	@Nullable
	private JvmLowLevelSearchBar searchBar;

	@Inject
	public JvmLowLevelPane(@Nonnull WorkspaceManager workspaceManager,
	                       @Nonnull MemberDisplayFormatConfig memberDisplay,
	                       @Nonnull CellConfigurationService configurationService,
	                       @Nonnull KeybindingConfig keys) {
		Workspace workspace = workspaceManager.getCurrent();
		treeBuilder = new JvmLowLevelTreeBuilder(workspace, memberDisplay, configurationService);

		// The search bar stays hidden until the find keybind opens it.
		NodeEvents.addKeyPressHandler(this, e -> {
			if (keys.getFind().match(e))
				showSearchBar();
		});
	}

	private void updateDisplay(@Nonnull JvmClassInfo info) throws InvalidClassException {
		ClassFileReader reader = new ClassFileReader();
		ClassFile classFile = reader.read(info.getBytecode());

		// Build a fresh registry-backed tree so path-dependent renderers never leak across updates.
		ClassItem root = treeBuilder.build(classFile, path);
		TreeView<ClassElement> nextTree = new TreeView<>();
		nextTree.setCellFactory(_ -> new TreeCell<>() {
			@Override
			protected void updateItem(ClassElement item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
					setOnContextMenuRequested(null);
				} else {
					item.configureDisplay(this);
				}
			}
		});
		nextTree.getStyleClass().addAll(Tweaks.EDGE_TO_EDGE, Styles.DENSE);
		nextTree.setShowRoot(false);
		nextTree.setRoot(root);

		// Publish the tree only after parsing and construction have both succeeded.
		tree = nextTree;
		setCenter(nextTree);

		// Each displayed tree starts with the search bar hidden.
		searchBar = new JvmLowLevelSearchBar(nextTree, this::hideSearchBar);
		setTop(null);
	}

	/**
	 * Shows the search bar at the top of the pane and focuses its input.
	 */
	private void showSearchBar() {
		JvmLowLevelSearchBar bar = searchBar;
		if (bar == null)
			return;
		setTop(bar);
		bar.requestSearchFocus();
	}

	/**
	 * Hides the search bar and returns focus to the tree, so the find keybind keeps working.
	 */
	private void hideSearchBar() {
		setTop(null);

		// Delayed, as the editor's search bar requires when hidden from the Escape key-event.
		TreeView<ClassElement> currentTree = tree;
		if (currentTree != null)
			FxThreadUtil.delayedRun(1, currentTree::requestFocus);
	}

	@Override
	public void requestFocus(@Nonnull ClassMember member) {
		selectTreeValue(element -> {
			if (!(element instanceof LazyClassElement<?> lazyElement))
				return false;
			if (!(lazyElement.getElement() instanceof software.coley.cafedude.classfile.ClassMember itemMember))
				return false;
			return member.getName().equals(itemMember.getName().getText())
					&& member.getDescriptor().equals(itemMember.getType().getText());
		});
	}

	@Override
	public void requestFocus(@Nonnull InnerClassInfo innerClass) {
		selectTreeValue(element -> {
			if (!(element instanceof LazyClassElement<?> lazyElement))
				return false;
			if (!(lazyElement.getElement() instanceof InnerClassesAttribute.InnerClass entry))
				return false;
			return innerClass.getInnerClassName().equals(entry.getInnerClassInfo().getName().getText());
		});
	}

	/**
	 * Selects the first low-level tree item matching a model predicate.
	 *
	 * @param predicate
	 * 		Predicate applied to source tree values.
	 */
	public void selectTreeValue(@Nonnull Predicate<ClassElement> predicate) {
		TreeView<ClassElement> currentTree = tree;
		if (currentTree == null || currentTree.getRoot() == null)
			return;
		TreeItem<ClassElement> match = findTreeItem(currentTree.getRoot(), predicate);
		if (match == null)
			return;
		selectTreeItem(currentTree, match);
	}

	/**
	 * Selects, focuses, and scrolls to a tree item.
	 *
	 * @param tree
	 * 		Tree containing the item.
	 * @param item
	 * 		Item to select.
	 */
	public void selectTreeItem(@Nonnull TreeView<ClassElement> tree, @Nonnull TreeItem<ClassElement> item) {
		TreeItems.expandParents(item);
		tree.getSelectionModel().select(item);
		int row = tree.getRow(item);
		if (row >= 0) {
			tree.getFocusModel().focus(row);
			tree.scrollTo(row);
		}
	}

	@Nullable
	private TreeItem<ClassElement> findTreeItem(@Nonnull TreeItem<ClassElement> root,
	                                            @Nonnull Predicate<ClassElement> predicate) {
		Queue<TreeItem<ClassElement>> queue = new ArrayDeque<>();
		queue.add(root);
		while (!queue.isEmpty()) {
			TreeItem<ClassElement> item = queue.remove();
			ClassElement value = item.getValue();
			if (value != null && predicate.test(value))
				return item;
			queue.addAll(item.getChildren());
		}
		return null;
	}

	@Nonnull
	@Override
	public ClassPathNode getClassPath() {
		return path;
	}

	@Nullable
	@Override
	public ClassPathNode getPath() {
		return path;
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		if (path instanceof ClassPathNode classPath) {
			this.path = classPath;
			try {
				updateDisplay(classPath.getValue().asJvmClass());
			} catch (InvalidClassException e) {
				logger.error("Failed to create low-level display for invalid class", e);
				// Clear stale navigation/search state before showing the invalid-class message.
				tree = null;
				searchBar = null;
				setTop(null);
				setCenter(new Label("Invalid class"));
			}
		}
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		setDisable(true);
	}
}
