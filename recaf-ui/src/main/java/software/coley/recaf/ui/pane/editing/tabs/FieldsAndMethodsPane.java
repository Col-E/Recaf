package software.coley.recaf.ui.pane.editing.tabs;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.info.Accessed;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.Named;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.InnerClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.context.ContextSource;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.navigation.ClassNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.control.BoundMultiToggleIcon;
import software.coley.recaf.ui.control.BoundToggleIcon;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.tree.TreeFiltering;
import software.coley.recaf.ui.control.tree.WorkspaceTreeCell;
import software.coley.recaf.ui.control.tree.WorkspaceTreeNode;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.Translatable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Displays fields, methods and other subcomponents of a {@link ClassInfo}.
 *
 * @author Amejonah
 * @author Matt Coley
 */
@Dependent
public class FieldsAndMethodsPane extends BorderPane implements ClassNavigable, UpdatableNavigable {
	private final SimpleStringProperty nameFilter = new SimpleStringProperty();
	private final SimpleBooleanProperty nameFilterCaseSensitivity = new SimpleBooleanProperty();
	private final SimpleBooleanProperty showSynthetics = new SimpleBooleanProperty(true);
	private final SimpleObjectProperty<MemberType> memberType = new SimpleObjectProperty<>(MemberType.ALL);
	private final SimpleObjectProperty<Visibility> visibility = new SimpleObjectProperty<>(Visibility.ALL);
	private final SimpleBooleanProperty sortAlphabetically = new SimpleBooleanProperty();
	private final SimpleBooleanProperty sortByVisibility = new SimpleBooleanProperty();
	private final TreeView<PathNode<?>> tree = new TreeView<>();
	private boolean navigationLock;
	private ClassPathNode path;

	@Inject
	public FieldsAndMethodsPane(@Nonnull CellConfigurationService configurationService,
	                            @Nonnull KeybindingConfig keys,
	                            @Nonnull Actions actions) {
		// Configure tree.
		tree.setShowRoot(false);
		tree.setCellFactory(param -> new WorkspaceTreeCell(ContextSource.DECLARATION, configurationService));
		tree.getStyleClass().addAll(Tweaks.EDGE_TO_EDGE, Styles.DENSE);
		tree.setOnKeyPressed(e -> {
			if (keys.getRename().match(e)) {
				TreeItem<PathNode<?>> selectedItem = tree.getSelectionModel().getSelectedItem();
				if (selectedItem != null)
					actions.rename(selectedItem.getValue());
			}
		});

		// Layout
		VBox box = new VBox(createButtonBar(), createFilterBar());
		box.setFillWidth(true);
		setCenter(tree);
		setBottom(box);
	}

	/**
	 * Sets up a double click listener on the tree that will call {@link ClassNavigable#requestFocus(ClassMember)}
	 * with the current selected item.
	 *
	 * @param navigableClass
	 * 		Parent class navigable component.
	 */
	public void setupSelectionNavigationListener(@Nonnull ClassNavigable navigableClass) {
		tree.setOnMouseClicked(mouseEvent -> {
			if (mouseEvent.getClickCount() == 2) {
				TreeItem<PathNode<?>> selectedItem = tree.getSelectionModel().getSelectedItem();
				if (!navigationLock && selectedItem != null && selectedItem.getValue() instanceof ClassMemberPathNode memberPathNode) {
					navigationLock = true;
					navigableClass.requestFocus(memberPathNode.getValue());
					navigationLock = false;
				}
			}
		});
	}

	/**
	 * Called when a filter is updated, which should trigger re-computation of which cells in the tree are visible.
	 */
	private void refreshTreeFilter() {
		if (tree.getRoot() instanceof WorkspaceTreeNode root) {
			root.predicateProperty().set(item -> {
				PathNode<?> path = item.getValue();

				Accessed accessed = (Accessed) path.getValue();
				if (!showSynthetics.get() && (accessed.hasSyntheticModifier() || accessed.hasBridgeModifier()))
					return false;
				if (!memberType.get().shouldDisplay(accessed))
					return false;
				if (!visibility.get().match(accessed))
					return false;

				String name;
				if (path instanceof ClassMemberPathNode memberPath) {
					ClassMember member = memberPath.getValue();
					name = member.getName();
				} else if (path instanceof InnerClassPathNode innerPath) {
					InnerClassInfo inner = innerPath.getValue();
					name = inner.getSimpleName();
				} else {
					throw new IllegalStateException("Unsupported type in fields & methods tree");
				}

				String filterText = nameFilter.getValue();
				if (!filterText.isBlank()) {
					if (nameFilterCaseSensitivity.get()) {
						return name.contains(filterText);
					} else {
						return name.toLowerCase().contains(filterText.toLowerCase());
					}
				}
				return true;
			});
		}
	}

	/**
	 * Called when a sort input is updated, which should trigger re-computation of cell order in the tree.
	 */
	private void refreshTreeSort() {
		if (tree.getRoot() instanceof WorkspaceTreeNode root) {
			Comparator<WorkspaceTreeNode> comparator = (a, b) -> {
				Object valueA = a.getValue().getValue();
				Object valueB = b.getValue().getValue();

				// Sort by visibility first.
				int result = 0;
				if (sortByVisibility.get()) {
					if (valueA instanceof Accessed accessedA && valueB instanceof Accessed accessedB) {
						result = Visibility.of(accessedA).compareTo(Visibility.of(accessedB));
					}
				}

				// Then by alphabetic order.
				if (result == 0 && sortAlphabetically.get()) {
					if (valueA instanceof Named namedA && valueB instanceof Named namedB) {
						result = CaseInsensitiveSimpleNaturalComparator.getInstance().compare(namedA.getName(), namedB.getName());
					}
				}

				// Then by default order.
				if (result == 0)
					return a.compareTo(b);

				return result;
			};
			root.sortChildren(comparator);
		}
	}

	/**
	 * @return Search filter bar.
	 */
	@Nonnull
	private Node createFilterBar() {
		TextField filter = new TextField();
		filter.promptTextProperty().bind(Lang.getBinding("fieldsandmethods.filter.prompt"));
		filter.getStyleClass().add("filter-field");
		filter.setMaxWidth(Integer.MAX_VALUE);
		TreeFiltering.install(filter, tree);
		nameFilter.bind(filter.textProperty());
		filter.textProperty().addListener((observable, oldValue, newValue) -> {
			refreshTreeFilter();
		});
		HBox box = new HBox(
				filter,
				new BoundToggleIcon(new FontIconView(CarbonIcons.LETTER_CC), nameFilterCaseSensitivity).withTooltip("misc.casesensitive")
		);
		HBox.setHgrow(filter, Priority.ALWAYS);
		box.setMaxWidth(Integer.MAX_VALUE);
		return box;
	}

	/**
	 * @return Box containing tree display options.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Nonnull
	private Node createButtonBar() {
		ChangeListener listener = (ob, old, cur) -> refreshTreeFilter();
		ChangeListener listener2 = (ob, old, cur) -> refreshTreeSort();

		memberType.addListener(listener);
		visibility.addListener(listener);
		showSynthetics.addListener(listener);
		sortAlphabetically.addListener(listener2);
		sortByVisibility.addListener(listener2);

		Button[] buttons = {
				// Show synthetics
				new BoundToggleIcon(Icons.SYNTHETIC, showSynthetics).withTooltip("fieldsandmethods.showoutlinedsynths"),
				// Member type
				new BoundMultiToggleIcon<>(MemberType.class, memberType, m -> Icons.getIconView(m.icon))
						.withTooltip("fieldsandmethods.showoutlinedmembertype"),
				// Visibility
				new BoundMultiToggleIcon<>(Visibility.class, visibility, v -> Icons.getIconView(v.icon))
						.withTooltip("fieldsandmethods.showoutlinedvisibility"),
				// Sort alphabetically/visibility
				new BoundToggleIcon(Icons.SORT_ALPHABETICAL, sortAlphabetically).withTooltip("fieldsandmethods.sortalphabetically"),
				new BoundToggleIcon(Icons.SORT_VISIBILITY, sortByVisibility).withTooltip("fieldsandmethods.sortbyvisibility")
		};
		for (Button button : buttons)
			button.setFocusTraversable(false);

		return new HBox(buttons);
	}

	@Nonnull
	@Override
	public ClassPathNode getPath() {
		return path;
	}

	@Nonnull
	@Override
	public ClassPathNode getClassPath() {
		return path;
	}

	@Override
	public void requestFocus(@Nonnull ClassMember member) {
		// Skip if lock is active. Implies we are the source of the request.
		if (navigationLock) return;

		// Select the given member.
		TreeItem<PathNode<?>> root = tree.getRoot();
		if (root == null)
			// If the value is null, it's probably waiting on the initialization from the path update handling.
			// Request focus with a small delay.
			FxThreadUtil.delayedRun(100, () -> requestFocusInternal(member));
		else
			requestFocusInternal(member);
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		if (path instanceof ClassPathNode classPath) {
			this.path = classPath;
			WorkspaceTreeNode root = new WorkspaceTreeNode(classPath);
			ClassInfo classInfo = classPath.getValue();
			for (InnerClassInfo innerClass : classInfo.getInnerClasses()) {
				if (innerClass.isExternalReference()) continue;
				InnerClassPathNode innerNode = classPath.child(innerClass);
				root.addAndSortChild(new WorkspaceTreeNode(innerNode));
			}
			for (FieldMember field : classInfo.getFields()) {
				ClassMemberPathNode memberNode = classPath.child(field);
				root.addAndSortChild(new WorkspaceTreeNode(memberNode));
			}
			for (MethodMember method : classInfo.getMethods()) {
				ClassMemberPathNode memberNode = classPath.child(method);
				root.addAndSortChild(new WorkspaceTreeNode(memberNode));
			}
			FxThreadUtil.run(() -> tree.setRoot(root));
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
		tree.setRoot(null);
	}

	private void requestFocusInternal(@Nonnull ClassMember member) {
		for (TreeItem<PathNode<?>> child : tree.getRoot().getChildren()) {
			if (member.equals(child.getValue().getValue())) {
				var selectionModel = tree.getSelectionModel();
				selectionModel.select(child);
				tree.getFocusModel().focus(selectionModel.getSelectedIndex());
				return;
			}
		}
	}

	/**
	 * Enum for differentiating different member type filters for {@link FieldsAndMethodsPane}.
	 *
	 * @author Amejonah
	 */
	public enum MemberType implements Translatable {
		ALL(Icons.CLASS_N_FIELD_N_METHOD, "misc.all"),
		FIELD(Icons.FIELD, "misc.member.field"),
		METHOD(Icons.METHOD, "misc.member.method"),
		FIELD_AND_METHOD(Icons.FIELD_N_METHOD, "misc.member.field-n-method"),
		INNER_CLASS(Icons.CLASS, "misc.member.inner-class");

		final String icon;
		final String key;

		MemberType(String icon, String key) {
			this.icon = icon;
			this.key = key;
		}

		@Nonnull
		@Override
		public String getTranslationKey() {
			return key;
		}

		private boolean shouldDisplay(@Nonnull Object object) {
			if (object instanceof ClassMember member)
				return shouldDisplay(member);
			else if (object instanceof InnerClassInfo inner)
				return shouldDisplay(inner);
			return true;
		}

		public boolean shouldDisplay(@Nonnull ClassMember member) {
			return this == ALL ||
					this == FIELD_AND_METHOD ||
					(this == FIELD && member.isField()) ||
					(this == METHOD && member.isMethod());
		}

		public boolean shouldDisplay(@Nonnull InnerClassInfo inner) {
			return this == ALL ||
					this == INNER_CLASS;
		}
	}

	/**
	 * Enum for differentiating different visibility filter for {@link FieldsAndMethodsPane}.
	 *
	 * @author Amejonah
	 */
	public enum Visibility implements Translatable {
		ALL(Icons.ACCESS_ALL_VISIBILITY, acc -> true),
		PUBLIC(Icons.ACCESS_PUBLIC, Accessed::hasPublicModifier),
		PROTECTED(Icons.ACCESS_PROTECTED, Accessed::hasProtectedModifier),
		PACKAGE(Icons.ACCESS_PACKAGE, Accessed::hasPackagePrivateModifier),
		PRIVATE(Icons.ACCESS_PRIVATE, Accessed::hasPrivateModifier);

		private final Function<Accessed, Boolean> accCheck;
		public final String icon;

		Visibility(@Nonnull String icon, @Nonnull Function<Accessed, Boolean> accCheck) {
			this.icon = icon;
			this.accCheck = accCheck;
		}

		public static Visibility of(Accessed accessed) {
			if (accessed.hasPublicModifier())
				return PUBLIC;
			else if (accessed.hasPackagePrivateModifier())
				return PACKAGE;
			else if (accessed.hasProtectedModifier())
				return PROTECTED;
			else if (accessed.hasPrivateModifier())
				return PRIVATE;
			return ALL;
		}

		public boolean match(Accessed accessed) {
			return accCheck.apply(accessed);
		}

		@Nonnull
		@Override
		public String getTranslationKey() {
			return this == ALL ? "misc.all" : "misc.accessflag.visibility." + name().toLowerCase();
		}
	}
}
