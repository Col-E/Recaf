package software.coley.recaf.ui.pane.editing;

import jakarta.annotation.Nonnull;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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
	private static final PseudoClass PSEUDO_HORIZONTAL = PseudoClass.getPseudoClass("horizontal");
	private static final PseudoClass PSEUDO_VERTICAL = PseudoClass.getPseudoClass("vertical");
	private final ObservableList<Tab> tabs = FXCollections.observableArrayList();
	private final ObservableObject<Tab> selectedTab = new ObservableObject<>(null);
	private final DoubleProperty initialSize = new SimpleDoubleProperty();
	private final Pane tabContainer;
	private final ResizeGrip grip;
	private final DoubleProperty prefSize;
	private double lastSize;
	private boolean sizeIsBound;
	private PathNode<?> path;

	/**
	 * New side tabs.
	 *
	 * @param orientation
	 * 		Orientation of tab layout.
	 */
	public SideTabs(@Nonnull Orientation orientation) {
		if (orientation == Orientation.VERTICAL) {
			VBox vBox = new VBox();
			vBox.setFillWidth(true);
			tabContainer = vBox;
		} else {
			HBox hBox = new HBox();
			hBox.setFillHeight(true);
			tabContainer = hBox;
		}
		tabContainer.getStyleClass().add("side-tab-pane");

		if (orientation == Orientation.VERTICAL) {
			pseudoClassStateChanged(PSEUDO_VERTICAL, true);
			setRight(tabContainer);
		} else {
			pseudoClassStateChanged(PSEUDO_HORIZONTAL, true);
			setBottom(tabContainer);
		}

		// When the tabs list is updated, add or remove tabs as necessary.
		tabs.addListener((ListChangeListener<Tab>) change -> {
			while ((change.next())) {
				// Add new tabs.
				for (Tab tab : change.getAddedSubList()) {
					// Add new tab display node.
					// When it is clicked on, update the selected tab.
					TabAdapter adapter = new TabAdapter(orientation, tab);
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
		DoubleProperty prefSize;
		ResizeGrip grip;
		if (orientation == Orientation.VERTICAL) {
			prefSize = prefWidthProperty();
			grip = new ResizeGrip(initialSize, orientation, this::getWidth,
					size -> prefSize.bind(size.map((Function<Number, Number>)
							number -> lastSize = Math.min(number.doubleValue(), getMaxWidth()))));
		} else {
			prefSize = prefHeightProperty();
			grip = new ResizeGrip(initialSize, orientation, this::getHeight,
					size -> prefSize.bind(size.map((Function<Number, Number>)
							number -> lastSize = Math.min(number.doubleValue(), getMaxHeight()))));
		}
		this.grip = grip;
		this.prefSize = prefSize;


		// When the selected tab changes display the selected one's content, clear content if no selection.
		selectedTab.addChangeListener((ob, old, cur) -> {
			for (Node child : tabContainer.getChildren()) {
				if (child instanceof TabAdapter adapter) {
					adapter.selected.setValue(cur == adapter.tab);
					if (cur == null) {
						// We unbind here so that the empty region can collapse.
						prefSize.unbind();
						prefSize.setValue(0);
						setCenter(null);

						// Mark that we are not bound.
						// When we go to bind again, we will know we can call the size setter without a binding conflict.
						sizeIsBound = false;
					} else {
						// Set initial pref size to what it was from the last open tab.
						if (!sizeIsBound) {
							prefSize.set(lastSize);
							sizeIsBound = true;
						}

						// We have this wrapper which has a resize bar on the left, and the main content in the center.
						// The resize bar will allow the main content size to be modified.
						// With the given binding logic, the main content is not allowed to expand beyond the container bounds.
						// We also record the bound size to a variable so that when we close a tab and re-open it, we can
						// restore the prior size.
						BorderPane wrapper = new BorderPane(cur.getContent());
						wrapper.getStyleClass().add("side-tab-content");
						if (orientation == Orientation.VERTICAL) {
							wrapper.setLeft(grip);
							wrapper.setPrefWidth(lastSize);
						} else {
							wrapper.setTop(grip);
							wrapper.setPrefHeight(lastSize);
						}
						setCenter(wrapper);
					}
				}
			}
		});

		// When the side-tabs are added to the UI we want to initialize the max-width/height property so that we can reference
		// it later in our logic to prevent the resize grip from making content larger than the parent size.
		NodeEvents.runOnceOnChange(parentProperty(), parent -> {
			if (parent instanceof Region parentRegion) {
				if (orientation == Orientation.VERTICAL) {
					ReadOnlyDoubleProperty parentWidthProperty = parentRegion.widthProperty();
					maxWidthProperty().bind(parentWidthProperty.subtract(35));
				} else {
					ReadOnlyDoubleProperty parentHeightProperty = parentRegion.heightProperty();
					maxHeightProperty().bind(parentHeightProperty.subtract(35));
				}
			}
		});
	}

	/**
	 * @return List of tabs.
	 */
	public ObservableList<Tab> getTabs() {
		return tabs;
	}


	/**
	 * Sets the size of content that will be shown when a tab is selected.
	 * By default, the tab content auto-sizes to the content shown.
	 * In some cases when the auto-size is 0 you may want to use this.
	 *
	 * @param initialSize
	 * 		Initial size of the tab content.
	 */
	public void setInitialSize(double initialSize) {
		this.initialSize.setValue(initialSize);
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
		for (Navigable child : getNavigableChildren())
			child.disable();
		setDisable(true);
	}

	/**
	 * Node adapter for {@link Tab} to display.
	 */
	private static class TabAdapter extends BorderPane {
		private static final PseudoClass STATE_SELECTED = new PseudoClass() {
			@Override
			public String getPseudoClassName() {
				return "selected";
			}
		};
		private final ObservableBoolean selected = new ObservableBoolean(false);
		private final Tab tab;

		/**
		 * @param orientation
		 * 		Parent side-tabs orientation.
		 * @param tab
		 * 		Tab content to represent.
		 */
		public TabAdapter(@Nonnull Orientation orientation, @Nonnull Tab tab) {
			this.tab = tab;
			Pane box;
			int padSmall = 5;
			int padBig = 10;
			if (orientation == Orientation.VERTICAL) {
				VBox vBox = new VBox();
				vBox.setSpacing(padSmall);
				box = vBox;
			} else {
				HBox hBox = new HBox();
				hBox.setSpacing(padSmall);
				box = hBox;
			}
			ObservableList<String> styleClasses = getStyleClass();
			styleClasses.add("side-tab");
			setPadding(orientation == Orientation.HORIZONTAL ?
					new Insets(padSmall, padBig, padSmall, padBig) :
					new Insets(padBig, padSmall, padBig, padSmall));

			// Layout & map content from tab
			BorderPane graphicWrapper = new BorderPane();
			graphicWrapper.centerProperty().bind(tab.graphicProperty());
			Text text = new Text();
			text.textProperty().bind(tab.textProperty());
			if (orientation == Orientation.VERTICAL) {
				// must be wrapped in group for this to use proper rotated dimensions
				graphicWrapper.setRotate(90);
				text.setRotate(90);
			}
			box.getChildren().addAll(graphicWrapper, new Group(text));
			setCenter(box);

			// Handle visual state change with selection & hover events.
			selected.addChangeListener((ob, old, cur) -> {
				pseudoClassStateChanged(STATE_SELECTED, cur);
			});
		}
	}

	/**
	 * Control for handling resizing the display.
	 */
	private static class ResizeGrip extends BorderPane {
		private final SimpleDoubleProperty targetSize = new SimpleDoubleProperty();

		/**
		 * @param initialSize
		 * 		Initial desired size of the side-tab content.
		 * @param orientation
		 * 		Parent side-tabs orientation.
		 * @param sizeLookup
		 * 		Parent container size lookup.
		 * @param consumer
		 * 		Consumer to take in the desired new size, wrapped in a {@link DoubleProperty} in order to
		 * 		support mapping operations.
		 */
		private ResizeGrip(@Nonnull DoubleProperty initialSize,
		                   @Nonnull Orientation orientation,
		                   @Nonnull DoubleSupplier sizeLookup,
		                   @Nonnull Consumer<DoubleProperty> consumer) {
			AtomicInteger startPos = new AtomicInteger();
			Pane box;
			if (orientation == Orientation.VERTICAL) {
				VBox vBox = new VBox();
				vBox.setSpacing(5);
				vBox.setFillWidth(true);
				box = vBox;
			} else {
				HBox hBox = new HBox();
				hBox.setSpacing(5);
				hBox.setFillHeight(true);
				box = hBox;
			}
			setCenter(box);
			getStyleClass().add("side-tab-grip");
			if (orientation == Orientation.VERTICAL) {
				setCursor(Cursor.H_RESIZE);
				setMinWidth(5);
				setOnMousePressed(e -> startPos.set((int) e.getX()));
				setOnMouseDragged(e -> {
					double x = e.getX();
					double diffX = (x - startPos.get());
					double width = sizeLookup.getAsDouble();
					double newWidth = width - diffX;
					targetSize.set(newWidth);
					consumer.accept(targetSize);
				});
			} else {
				setCursor(Cursor.V_RESIZE);
				setMinHeight(5);
				setOnMousePressed(e -> startPos.set((int) e.getY()));
				setOnMouseDragged(e -> {
					double y = e.getY();
					double diffY = (y - startPos.get());
					double height = sizeLookup.getAsDouble();
					double newHeight = height - diffY;
					targetSize.set(newHeight);
					consumer.accept(targetSize);
				});
			}

			// Trigger call to consumer once with existing parent width once initially added to the UI.
			NodeEvents.runOnceOnChange(parentProperty(), parent -> {
				targetSize.set(Math.max(sizeLookup.getAsDouble(), initialSize.doubleValue()));
				consumer.accept(targetSize);
			});
		}
	}
}