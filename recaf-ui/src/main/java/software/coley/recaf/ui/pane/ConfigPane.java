package software.coley.recaf.ui.pane;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.collections.Lists;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.services.config.ConfigComponentFactory;
import software.coley.recaf.services.config.ConfigComponentManager;
import software.coley.recaf.services.config.ConfigIconManager;
import software.coley.recaf.services.config.ConfigManager;
import software.coley.recaf.services.config.ManagedConfigListener;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.popup.NamePopup;
import software.coley.recaf.ui.control.tree.FilterableTreeItem;
import software.coley.recaf.ui.control.tree.TreeFiltering;
import software.coley.recaf.util.ErrorDialogs;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.SceneUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static software.coley.recaf.config.ConfigGroups.PACKAGE_SPLIT;
import static software.coley.recaf.config.ConfigGroups.getGroupPackages;
import static software.coley.recaf.util.Lang.getBinding;

/**
 * Pane to display all config values.
 *
 * @author Matt Coley
 * @see ConfigManager Source of values to pull from.
 * @see ConfigComponentManager Controls how to represent {@link ConfigValue} instances.
 * @see ConfigIconManager Controls which icons to show in the tree for {@link ConfigContainer} paths.
 */
@Dependent
public class ConfigPane extends BorderPane implements ManagedConfigListener {
	private static final Logger logger = Logging.get(ConfigPane.class);
	private final Map<String, ContainerPane> idToPage = new TreeMap<>();
	private final Map<String, ConfigTreeItem> idToTree = new TreeMap<>();
	private final Map<String, ConfigContainer> idToContainer = new TreeMap<>();
	private final ConfigTreeItem root = new ConfigTreeItem("root", null, "root", null);
	private final TreeView<String> tree = new TreeView<>();
	private final ScrollPane content = new ScrollPane();
	private final ObservableList<String> profiles = FXCollections.observableArrayList();
	private final ComboBox<String> profileCombo = new ComboBox<>(profiles);
	private final CustomTextField searchField = new CustomTextField();
	private final Label noResultsLabel = new BoundLabel(Lang.getBinding("menu.search.noresults"));
	private final SplitPane split = new SplitPane();
	private final ConfigComponentManager componentManager;
	private final ConfigIconManager iconManager;
	private final ConfigManager configManager;
	private boolean updatingProfileSelection;
	private boolean profileLifecycleInstalled;
	private boolean activeProfileInitialized;

	@Inject
	public ConfigPane(@Nonnull ConfigManager configManager,
	                  @Nonnull ConfigComponentManager componentManager,
	                  @Nonnull ConfigIconManager iconManager) {
		this.configManager = configManager;
		this.componentManager = componentManager;
		this.iconManager = iconManager;
		configManager.addManagedConfigListener(this);

		// Setup UI
		initialize();

		// Initial state from existing containers
		for (ConfigContainer container : configManager.getContainers())
			onRegister(container);

		// Select first page
		selectFirstVisibleItem();

		// Register hooks to detect when the pane is shown/hidden to trigger profile loading/saving.
		SceneUtils.whenAddedToSceneConsume(this, pane -> installWindowHooks(pane.getScene()));
	}

	private void initialize() {
		content.setFitToWidth(true);
		root.setExpanded(true);
		tree.setShowRoot(false);
		tree.setRoot(root);
		tree.getStyleClass().addAll(Tweaks.EDGE_TO_EDGE, Styles.DENSE);
		tree.setCellFactory(param -> new TreeCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				textProperty().unbind();

				if (empty || item == null) {
					setText(null);
					setGraphic(null);
				} else {
					ConfigTreeItem treeItem = getTreeItem() instanceof ConfigTreeItem configTreeItem ? configTreeItem : null;
					if (treeItem == null) {
						setText(item);
						setGraphic(new FontIconView(CarbonIcons.DOT_MARK));
						return;
					}

					StringBinding binding = treeItem.getTextBinding();
					if (binding != null)
						textProperty().bind(binding);
					else
						setText(treeItem.getTextFallback());
					setGraphic(new FontIconView(treeItem.getIcon()));
				}
			}
		});
		tree.getSelectionModel().selectedItemProperty().addListener((ob, old, cur) -> {
			if (cur != null) {
				ContainerPane page = idToPage.get(cur.getValue());
				if (page != null) content.setContent(page);
				else content.setContent(new MissingPage((ConfigTreeItem) cur));
			}
		});
		TreeFiltering.install(searchField, tree);

		profileCombo.setMaxWidth(250);
		profileCombo.valueProperty().addListener((ob, old, cur) -> {
			if (!updatingProfileSelection && cur != null)
				switchProfile(cur);
		});

		searchField.setLeft(new FontIconView(CarbonIcons.SEARCH));
		searchField.promptTextProperty().bind(getBinding("menu.config.filter-prompt"));
		searchField.textProperty().addListener((ob, old, cur) -> updateTreeFilter());

		noResultsLabel.setGraphic(new FontIconView(CarbonIcons.SEARCH));
		noResultsLabel.setMouseTransparent(true);
		noResultsLabel.setVisible(false);
		noResultsLabel.setOpacity(0.5);

		// Layout
		ActionButton createProfileButton = new ActionButton(CarbonIcons.ADD_ALT, getBinding("menu.config.profile.new"), this::promptCreateProfile);
		ActionButton deleteProfileButton = new ActionButton(CarbonIcons.TRASH_CAN, getBinding("menu.config.profile.delete"), this::deleteSelectedProfile);
		deleteProfileButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
						profiles.size() <= 1 || profileCombo.getValue() == null,
				profiles, profileCombo.valueProperty()));

		Label profileLabel = new BoundLabel(getBinding("menu.config.profile"));
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox toolbar = new HBox(10, profileLabel, profileCombo, createProfileButton, deleteProfileButton, spacer, searchField);
		toolbar.setAlignment(Pos.CENTER_LEFT);
		toolbar.setPadding(new Insets(10));
		toolbar.getStyleClass().add("config-toolbar");

		SplitPane.setResizableWithParent(tree, false);
		StackPane treeStack = new StackPane(tree, noResultsLabel);
		StackPane.setAlignment(noResultsLabel, Pos.CENTER);
		split.getItems().addAll(treeStack, content);
		split.setDividerPositions(0.3);
		setTop(toolbar);
		setCenter(split);
	}

	@Override
	public void onRegister(@Nonnull ConfigContainer container) {
		// Skip empty containers, or any that only have hidden values and otherwise appear empty.
		if (!hasVisibleValues(container))
			return;

		// Setup tree structure.
		ConfigTreeItem item = getItem(container, true);
		if (item != null) {
			String pageKey = item.getValue() + PACKAGE_SPLIT + container.getId();
			Ikon icon = iconManager.getContainerIcon(container);
			ConfigTreeItem treeItem = new ConfigTreeItem(pageKey, pageKey, container.getId(), icon);
			addSortedChild(item, treeItem);

			// Register page.
			idToPage.put(pageKey, new ContainerPane(container));
			idToTree.put(pageKey, treeItem);
			idToContainer.put(pageKey, container);
		}
		updateTreeFilter();
	}

	@Override
	public void onUnregister(@Nonnull ConfigContainer container) {
		ConfigTreeItem item = getItem(container, false);
		if (item == null)
			return;

		// Remove page.
		String pageKey = item.getValue() + PACKAGE_SPLIT + container.getId();
		ConfigTreeItem pageItem = idToTree.remove(pageKey);
		idToPage.remove(pageKey);
		idToContainer.remove(pageKey);
		if (pageItem == null)
			return;

		// Remove from tree and prune any empty parent items.
		item.removeSourceChild(pageItem);
		pruneIfEmpty(item);
		updateTreeFilter();
	}

	/**
	 * @param item
	 * 		Tree item to look in.
	 * @param name
	 * 		Name to match.
	 *
	 * @return Child with {@link TreeItem#getValue()} matching the given name value.
	 */
	@Nullable
	private ConfigTreeItem getChildTreeItemByName(@Nonnull ConfigTreeItem item, @Nonnull String name) {
		for (TreeItem<String> child : item.getSourceChildren())
			if (name.equals(child.getValue()))
				return (ConfigTreeItem) child;
		return null;
	}

	/**
	 * @param container
	 * 		Container to get item of.
	 * @param createIfMissing
	 * 		Flag to create item if it does not exist.
	 *
	 * @return Tree item if it exists. Otherwise {@code null}.
	 */
	@Nullable
	private ConfigTreeItem getItem(@Nonnull ConfigContainer container, boolean createIfMissing) {
		ConfigTreeItem currentItem = root;
		String currentPackage = null;
		String[] packages = getGroupPackages(container);
		for (String packageName : packages) {
			if (currentPackage == null)
				currentPackage = packageName;
			else
				currentPackage += PACKAGE_SPLIT + packageName;

			// Get or setup tree item.
			ConfigTreeItem child = getChildTreeItemByName(currentItem, currentPackage);
			if (child == null) {
				if (createIfMissing) {
					// No existing tree item, create one.
					Ikon packageIcon = iconManager.getGroupIcon(currentPackage);
					Ikon icon = packageIcon == null ? CarbonIcons.DOT_MARK : packageIcon;
					child = new ConfigTreeItem(currentPackage, currentPackage, packageName, icon);
					child.setExpanded(true);
					addSortedChild(currentItem, child);
				} else {
					// Exit, cannot find item
					return null;
				}
			}
			currentItem = child;
		}
		return currentItem;
	}

	private void addSortedChild(@Nonnull ConfigTreeItem parent, @Nonnull ConfigTreeItem child) {
		List<String> childrenNames = parent.getSourceChildren().stream()
				.map(TreeItem::getValue)
				.toList();
		int insertIndex = Lists.sortedInsertIndex(childrenNames, child.getValue());
		parent.addPreSortedChild(child, insertIndex);
	}

	private void pruneIfEmpty(@Nonnull ConfigTreeItem item) {
		ConfigTreeItem current = item;
		while (current != root && current.getSourceChildren().isEmpty()) {
			ConfigTreeItem parent = current.getSourceParent() instanceof ConfigTreeItem configTreeItem ? configTreeItem : null;
			if (parent == null)
				break;
			parent.removeSourceChild(current);
			current = parent;
		}
	}

	/**
	 * Update tree filter based on search field value.
	 */
	private void updateTreeFilter() {
		String query = searchField.getText();
		if (query == null || query.isBlank()) {
			root.predicateProperty().set(null);
		} else {
			List<String> tokens = ConfigPaneSearch.tokenizeQuery(query);
			root.predicateProperty().set(item -> {
				ConfigContainer container = idToContainer.get(item.getValue());
				return container != null && ConfigPaneSearch.matches(container, tokens);
			});
		}
		noResultsLabel.setVisible(root.getChildren().isEmpty() && !searchField.getText().isBlank());

		// Select first visible item if current selection is not visible.
		TreeItem<String> currentSelection = tree.getSelectionModel().getSelectedItem();
		if (currentSelection != null && isVisible(root, currentSelection))
			return;
		selectFirstVisibleItem();
	}

	private void selectFirstVisibleItem() {
		TreeItem<String> firstItem = findFirstVisibleItem(root);
		if (firstItem != null)
			tree.getSelectionModel().select(firstItem);
	}

	@Nullable
	private TreeItem<String> findFirstVisibleItem(@Nonnull TreeItem<String> item) {
		for (TreeItem<String> child : item.getChildren()) {
			TreeItem<String> match = child.isLeaf() ? child : findFirstVisibleItem(child);
			if (match != null)
				return match;
		}
		return null;
	}

	private boolean isVisible(@Nonnull TreeItem<String> parent, @Nonnull TreeItem<String> target) {
		for (TreeItem<String> child : parent.getChildren())
			if (child == target || isVisible(child, target))
				return true;
		return false;
	}

	private void installWindowHooks(@Nullable Scene scene) {
		if (scene == null)
			return;
		Window window = scene.getWindow();
		if (window != null) {
			installWindowHooks(window);
		} else {
			scene.windowProperty().addListener(new ChangeListener<>() {
				@Override
				public void changed(javafx.beans.value.ObservableValue<? extends Window> observable, Window oldValue, Window newValue) {
					if (newValue != null) {
						scene.windowProperty().removeListener(this);
						installWindowHooks(newValue);
					}
				}
			});
		}
	}

	private void installWindowHooks(@Nonnull Window window) {
		if (profileLifecycleInstalled)
			return;
		profileLifecycleInstalled = true;
		window.addEventHandler(WindowEvent.WINDOW_SHOWING, e -> onWindowShowing());
		window.addEventHandler(WindowEvent.WINDOW_HIDDEN, e -> onWindowHidden());
	}

	/**
	 * When shown, ensure the active profile is loaded and refresh the profile list to reflect any external changes.
	 */
	private void onWindowShowing() {
		try {
			if (!activeProfileInitialized) {
				configManager.ensureActiveProfile();
				activeProfileInitialized = true;
			}
			refreshProfiles();
		} catch (IOException ex) {
			logger.error("Failed to initialize config profiles", ex);
			showProfileError("Failed to initialize config profiles", ex);
		}
	}

	/**
	 * When hidden, attempt to persist the active profile to ensure changes are not lost.
	 */
	private void onWindowHidden() {
		if (!activeProfileInitialized)
			return;

		try {
			configManager.exportProfile(configManager.getServiceConfig().getCurrentProfile().getValue());
		} catch (IOException ex) {
			logger.error("Failed to persist active config profile on window close", ex);
		}
	}

	/**
	 * Refresh the list of profiles in {@link #profileCombo}, and select the current active profile.
	 *
	 * @throws IOException
	 * 		When profile names cannot be loaded.
	 */
	private void refreshProfiles() throws IOException {
		List<String> profileNames = new ArrayList<>(configManager.getProfileNames());
		String currentProfile = configManager.getServiceConfig().getCurrentProfile().getValue();
		updatingProfileSelection = true;
		profiles.setAll(profileNames);
		if (profileNames.contains(currentProfile))
			profileCombo.getSelectionModel().select(currentProfile);
		else if (!profileNames.isEmpty())
			profileCombo.getSelectionModel().select(profileNames.getFirst());
		else
			profileCombo.getSelectionModel().clearSelection();
		updatingProfileSelection = false;
	}

	/**
	 * Switch to the given profile, persisting the current active profile before switching.
	 *
	 * @param profileName
	 * 		Name of profile to switch to.
	 */
	private void switchProfile(@Nonnull String profileName) {
		// Normalize name, check if we're already on the profile.
		String normalizedProfile = ConfigManager.normalizeProfileName(profileName);
		String currentProfile = ConfigManager.normalizeProfileName(configManager.getServiceConfig().getCurrentProfile().getValue());
		if (currentProfile.equals(normalizedProfile))
			return;

		try {
			// Save what we got on the current profile before switching, then load the new one.
			configManager.exportProfile(currentProfile);
			configManager.importProfile(normalizedProfile);

			// And update active profile value to reflect the switch.
			configManager.getServiceConfig().getCurrentProfile().setValue(normalizedProfile);
			refreshProfiles();
		} catch (IOException | IllegalArgumentException ex) {
			logger.error("Failed to switch config profile to '{}'", profileName, ex);
			showProfileError("Failed to switch config profile", ex);
			try {
				refreshProfiles();
			} catch (IOException refreshEx) {
				logger.error("Failed to refresh config profile list after switch error", refreshEx);
			}
		}
	}

	private void promptCreateProfile() {
		NamePopup popup = new NamePopup(profileName -> {
			try {
				String normalizedProfile = ConfigManager.normalizeProfileName(profileName);
				if (configManager.getProfileNames().contains(normalizedProfile))
					throw new IllegalStateException("Config profile already exists: " + normalizedProfile);

				configManager.exportProfile(normalizedProfile);
				configManager.getServiceConfig().getCurrentProfile().setValue(normalizedProfile);
				javafx.application.Platform.runLater(() -> {
					try {
						refreshProfiles();
					} catch (IOException ex) {
						logger.error("Failed to refresh config profiles after creating '{}'", normalizedProfile, ex);
						showProfileError("Failed to refresh config profiles", ex);
					}
				});
			} catch (IOException | IllegalArgumentException | IllegalStateException ex) {
				logger.error("Failed to create config profile '{}'", profileName, ex);
				showProfileError("Failed to create config profile", ex);
			}
		});
		popup.withTitle(getBinding("dialog.title.create-config-profile"));
		popup.show();
	}

	private void deleteSelectedProfile() {
		String selectedProfile = profileCombo.getValue();
		if (selectedProfile == null)
			return;

		try {
			List<String> profileNames = new ArrayList<>(configManager.getProfileNames());
			if (profileNames.size() <= 1)
				return;

			// If the profile being deleted is the active profile, we need to switch to a different profile first.
			String normalizedSelected = ConfigManager.normalizeProfileName(selectedProfile);
			String currentProfile = ConfigManager.normalizeProfileName(configManager.getServiceConfig().getCurrentProfile().getValue());
			if (normalizedSelected.equals(currentProfile)) {
				if (ConfigManager.DEFAULT_PROFILE_NAME.equals(normalizedSelected)) {
					// Switch to the first available alternative profile.
					String replacement = profileNames.stream()
							.filter(name -> !name.equals(normalizedSelected))
							.findFirst()
							.orElse(null);
					if (replacement == null)
						return;

					configManager.importProfile(replacement);
					configManager.getServiceConfig().getCurrentProfile().setValue(replacement);
				} else {
					// Switch to the default profile, which should always be available (and made if not for any reason).
					if (!configManager.hasProfile(ConfigManager.DEFAULT_PROFILE_NAME))
						configManager.exportProfile(ConfigManager.DEFAULT_PROFILE_NAME);
					configManager.importProfile(ConfigManager.DEFAULT_PROFILE_NAME);
					configManager.getServiceConfig().getCurrentProfile().setValue(ConfigManager.DEFAULT_PROFILE_NAME);
				}
			}

			configManager.deleteProfile(normalizedSelected);
			refreshProfiles();
		} catch (IOException | IllegalArgumentException ex) {
			logger.error("Failed to delete config profile '{}'", selectedProfile, ex);
			showProfileError("Failed to delete config profile", ex);
		}
	}

	private static void showProfileError(@Nonnull String header, @Nonnull Throwable throwable) {
		ErrorDialogs.show("Config profile error", header, "The error was:", throwable);
	}

	private static boolean hasVisibleValues(@Nonnull ConfigContainer container) {
		return container.getValues().values().stream().anyMatch(value -> !value.isHidden());
	}

	/**
	 * Page for a single {@link ConfigContainer}.
	 */
	private class ContainerPane extends GridPane {
		@SuppressWarnings({"rawtypes", "unchecked"})
		private ContainerPane(@Nonnull ConfigContainer container) {
			// Title
			Label title = createTranslatedOrLiteralLabel(container.getGroupAndId(), container.getId());
			title.getStyleClass().add(Styles.TITLE_4);
			add(title, 0, 0, 2, 1);
			add(new Separator(), 0, 1, 2, 1);

			// Values
			Map<String, ConfigValue<?>> values = container.getValues();
			int row = 2;
			for (Map.Entry<String, ConfigValue<?>> entry : values.entrySet()) {
				ConfigValue<?> value = entry.getValue();
				if (value.isHidden()) continue;
				ConfigComponentFactory componentFactory = componentManager.getFactory(container, value);
				if (componentFactory.isStandAlone()) {
					add(componentFactory.create(container, value), 0, row, 2, 1);
				} else {
					String key = container.getScopedId(value);
					add(createTranslatedOrLiteralLabel(key, value.getId()), 0, row);
					add(componentFactory.create(container, value), 1, row);
				}
				row++;
			}

			// Layout
			setPadding(new Insets(10));
			setVgap(5);
			setHgap(5);
			ColumnConstraints columnLabel = new ColumnConstraints();
			ColumnConstraints columnEditor = new ColumnConstraints();
			columnEditor.setHgrow(Priority.ALWAYS);
			getColumnConstraints().addAll(columnLabel, columnEditor);
		}

		@Nonnull
		private static Label createTranslatedOrLiteralLabel(@Nonnull String translationKey, @Nonnull String fallback) {
			return Lang.has(translationKey) ?
					new BoundLabel(getBinding(translationKey)) :
					new Label(fallback);
		}
	}

	/**
	 * Page to show child-pages when the group itself does not have content.
	 */
	private class MissingPage extends VBox {
		public MissingPage(@Nonnull ConfigTreeItem item) {
			// Title
			ObservableList<Node> children = getChildren();
			Label title;
			StringBinding binding = item.getTextBinding();
			if (binding != null) {
				title = new BoundLabel(binding);
			} else {
				title = new Label(item.getTextFallback());
			}
			title.getStyleClass().add(Styles.TITLE_4);
			children.add(title);
			children.add(new Separator());

			// Sub-menus
			String id = item.getValue();
			for (String key : idToPage.keySet()) {
				if (key.startsWith(id)) {
					Hyperlink child = new Hyperlink();
					ConfigTreeItem childItem = idToTree.get(key);
					if (childItem != null && childItem.getTextBinding() != null)
						child.textProperty().bind(childItem.getTextBinding());
					else if (childItem != null)
						child.setText(childItem.getTextFallback());
					else
						child.setText(key);
					child.setOnAction(e -> {
						TreeItem<String> treeItem = idToTree.get(key);
						tree.getSelectionModel().select(treeItem);
					});
					children.add(child);
				}
			}

			// Layout
			setPadding(new Insets(10));
			setSpacing(5);
			setFillWidth(true);
		}
	}

	private static class ConfigTreeItem extends FilterableTreeItem<String> {
		private final String translationKey;
		private final String textFallback;
		private final Ikon icon;

		private ConfigTreeItem(@Nonnull String value, @Nullable String translationKey,
		                       @Nonnull String textFallback, @Nullable Ikon icon) {
			setValue(value);
			this.translationKey = translationKey;
			this.textFallback = textFallback;
			this.icon = icon == null ? CarbonIcons.DOT_MARK : icon;
		}

		@Nullable
		private StringBinding getTextBinding() {
			if (translationKey != null && Lang.has(translationKey))
				return Lang.getBinding(translationKey);
			return null;
		}

		@Nonnull
		private String getTextFallback() {
			return textFallback;
		}

		@Nonnull
		private Ikon getIcon() {
			return icon;
		}
	}
}
