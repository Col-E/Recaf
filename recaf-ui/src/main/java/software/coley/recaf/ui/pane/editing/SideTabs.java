package software.coley.recaf.ui.pane.editing;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableObject;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.util.NodeEvents;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Alternative to vertical oriented {@link TabPane} that operates similarly to the side-tabs seen in IntelliJ.
 * When these tabs are selected their content is <i>"toggled"</i> and expands into the main editor viewport.
 * By default {@link TabPane} isn't really suitable for this. Thus, we make our own minimal implementation.
 *
 * @author Matt Coley
 */
public class SideTabs extends BorderPane implements UpdatableNavigable {
	private final ObservableList<Tab> tabs = FXCollections.observableArrayList();
	private final ObservableObject<Tab> selectedTab = new ObservableObject<>(null);
	private final VBox tabContainer = new VBox();
	private double lastWidth;
	private boolean widthIsBound;
	private PathNode<?> path;

	/**
	 * New side tabs.
	 */
	public SideTabs() {
		tabContainer.getStyleClass().add("side-tab-pane");
		tabContainer.setFillWidth(true);

		// Display tabs.
		setRight(tabContainer);

		// When the tabs list is updated, add or remove tabs as necessary.
		tabs.addListener((ListChangeListener<Tab>) change -> {
			while ((change.next())) {
				// Add new tabs.
				for (Tab tab : change.getAddedSubList()) {
					// Add new tab display node.
					// When it is clicked on, update the selected tab.
					TabAdapter adapter = new TabAdapter(tab);
					adapter.setOnMousePressed(e -> {
						if (selectedTab.getValue() == tab)
							selectedTab.setValue(null);
						else
							selectedTab.setValue(tab);
					});
					tabContainer.getChildren().add(adapter);
				}

				// Remove old tabs.
				for (Tab tab : change.getRemoved())
					tabContainer.getChildren().removeIf(n -> n instanceof TabAdapter adapter && adapter.tab == tab);
			}
		});

		// We only want to allocate ONE grip element, which will be reused.
		// Less allocation is good, but we want a single instance so the initial callback to the consumer parameter
		// is only run ONCE. Not each time we select a new tab.
		DoubleProperty prefWidth = prefWidthProperty();
		ResizeGrip grip = new ResizeGrip(this::getWidth,
				size -> prefWidth.bind(size.map((Function<Number, Number>)
						number -> lastWidth = Math.min(number.doubleValue(), getMaxWidth()))));

		// When the selected tab changes display the selected one's content, clear content if no selection.
		selectedTab.addChangeListener((ob, old, cur) -> {
			for (Node child : tabContainer.getChildren()) {
				if (child instanceof TabAdapter adapter) {
					adapter.selected.setValue(cur == adapter.tab);
					if (cur == null) {
						// We unbind here so that the empty region can collapse.
						prefWidth.unbind();
						prefWidth.setValue(0);
						setCenter(null);

						// Mark that we are not bound.
						// When we go to bind again, we will know we can call the width setter without a binding conflict.
						widthIsBound = false;
					} else {
						// Set initial pref width to what it was from the last open tab.
						if (!widthIsBound) {
							prefWidth.set(lastWidth);
							widthIsBound = true;
						}

						// We have this wrapper which has a resize bar on the left, and the main content in the center.
						// The resize bar will allow the main content width to be modified.
						// With the given binding logic, the main content is not allowed to expand beyond the container bounds.
						// We also record the bound width to a variable so that when we close a tab and re-open it, we can
						// restore the prior width.
						BorderPane wrapper = new BorderPane(cur.getContent());
						wrapper.getStyleClass().add("side-tab-content");
						wrapper.setLeft(grip);
						setCenter(wrapper);
					}
				}
			}
		});

		// When the side-tabs are added to the UI we want to initialize the max-width property so that we can reference
		// it later in our logic to prevent the resize grip from making content larger than the parent size.
		NodeEvents.runOnceOnChange(parentProperty(), parent -> {
			if (parent instanceof Region parentRegion) {
				ReadOnlyDoubleProperty parentWidthProperty = parentRegion.widthProperty();
				maxWidthProperty().bind(parentWidthProperty.subtract(35));
			}
		});
	}

	/**
	 * @return List of tabs.
	 */
	public ObservableList<Tab> getTabs() {
		return tabs;
	}

	@Nonnull
	@Override
	public PathNode<?> getPath() {
		return path;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return tabs.stream()
				.filter(t -> t.getContent() instanceof Navigable)
				.map(t -> (Navigable) t.getContent())
				.collect(Collectors.toList());
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		if (this.path == null || this.path.getClass() == path.getClass()) {
			this.path = path;
			for (Navigable navigable : getNavigableChildren()) {
				if (navigable instanceof UpdatableNavigable updatable) {
					updatable.onUpdatePath(path);
				}
			}
		}
	}

	@Override
	public void disable() {
		setDisable(true);
	}

	/**
	 * Node adapter for {@link Tab} to display.
	 */
	private static class TabAdapter extends VBox {
		private final ObservableBoolean selected = new ObservableBoolean(false);
		private final Tab tab;

		/**
		 * @param tab
		 * 		Tab content to represent.
		 */
		public TabAdapter(Tab tab) {
			this.tab = tab;
			ObservableList<String> styleClasses = getStyleClass();
			setPadding(new Insets(5));
			setSpacing(5);

			// Layout & map content from tab
			BorderPane graphicWrapper = new BorderPane();
			graphicWrapper.centerProperty().bind(tab.graphicProperty());
			graphicWrapper.setRotate(90);
			Text text = new Text();
			text.textProperty().bind(tab.textProperty());
			text.setRotate(90); // must be wrapped in group for this to use proper rotated dimensions
			getChildren().addAll(graphicWrapper, new Group(text));

			// Handle visual state change with selection & hover events.
			selected.addChangeListener((ob, old, cur) -> {
				if (cur) {
					if (!styleClasses.contains("side-tab"))
						styleClasses.add("side-tab");
				} else
					styleClasses.remove("side-tab");
			});
			setOnMouseEntered(e -> {
				if (!selected.getValue() && !styleClasses.contains("side-tab"))
					styleClasses.add("side-tab");
			});
			setOnMouseExited(e -> {
				if (!selected.getValue())
					styleClasses.remove("side-tab");
			});
		}
	}

	/**
	 * Control for handling resizing the display.
	 */
	private static class ResizeGrip extends HBox {
		private final SimpleDoubleProperty targetSize = new SimpleDoubleProperty();

		/**
		 * @param widthLookup
		 * 		Parent container width lookup.
		 * @param consumer
		 * 		Consumer to take in the desired new width, wrapped in a {@link DoubleProperty} in order to
		 * 		support mapping operations.
		 */
		private ResizeGrip(DoubleSupplier widthLookup, Consumer<DoubleProperty> consumer) {
			AtomicInteger startX = new AtomicInteger();
			setFillHeight(true);
			getStyleClass().add("side-tab-grip");
			setMinWidth(5);
			setCursor(Cursor.H_RESIZE);
			setOnMousePressed(e -> startX.set((int) e.getX()));
			setOnMouseDragged(e -> {
				double x = e.getX();
				double diffX = (x - startX.get());
				double width = widthLookup.getAsDouble();
				double newWidth = width - diffX;
				targetSize.set(newWidth);
				consumer.accept(targetSize);
			});

			// Trigger call to consumer once with existing parent width once initially added to the UI.
			NodeEvents.runOnceOnChange(parentProperty(), parent -> {
				targetSize.set(widthLookup.getAsDouble());
				consumer.accept(targetSize);
			});
		}
	}
}