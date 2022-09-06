package me.coley.recaf.ui.control;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.util.Collection;
import java.util.function.Function;

/**
 * A {@link org.fxmisc.flowless.Virtualized} version of {@link javafx.scene.control.ContextMenu}.
 *
 * @param <T>
 * 		Menu content type.
 *
 * @author Matt Coley
 */
public class VirtualizedContextMenu<T> extends PopupControl {
	// TODO: Allow user to also manipulate (add) class of cells
	//  - override default 'menu-item' properties like padding/size
	private static final String DEFAULT_STYLE_CLASS = "context-menu";
	private final ObjectProperty<Function<T, ? extends Node>> mapperProperty = new SimpleObjectProperty<>(t -> {
		throw new UnsupportedOperationException();
	});
	private final ObjectProperty<EventHandler<ActionEvent>> onAction = new ObjectPropertyBase<>() {
		@Override
		protected void invalidated() {
			setEventHandler(ActionEvent.ACTION, get());
		}

		@Override
		public Object getBean() {
			return VirtualizedContextMenu.this;
		}

		@Override
		public String getName() {
			return "onAction";
		}
	};
	private final ObservableList<T> items;
	private final ObjectProperty<T> selectedItem = new SimpleObjectProperty<>();

	/**
	 * @param items
	 * 		Initial collection of items.
	 */
	public VirtualizedContextMenu(Collection<T> items) {
		this.items = FXCollections.observableArrayList(items);
		getStyleClass().setAll(DEFAULT_STYLE_CLASS);
		setAutoHide(true);
		setConsumeAutoHidingEvents(false);
	}

	/**
	 * @param layoutMapper
	 * 		Mapper of {@code T} items to {@link Node} representations.
	 * @param items
	 * 		Initial collection of items.
	 */
	public VirtualizedContextMenu(Function<T, ? extends Node> layoutMapper, Collection<T> items) {
		this(items);
		mapperProperty.set(layoutMapper);
	}

	/**
	 * Called when the user interacts with a menu item.
	 */
	private void runAction() {
		getOnAction().handle(null);
		hide();
	}

	/**
	 * @return Action to run when user interacts with a menu item.
	 */
	public final EventHandler<ActionEvent> getOnAction() {
		return onActionProperty().get();
	}

	/**
	 * @param value
	 * 		Action to run when user interacts with a menu item.
	 */
	public final void setOnAction(EventHandler<ActionEvent> value) {
		onActionProperty().set(value);
	}

	/**
	 * @return Property wrapper of action to run when user interacts with a menu item.
	 */
	public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
		return onAction;
	}

	/**
	 * @return Property wrapper of mapper of {@code T} items to {@link Node} representations.
	 */
	public ObjectProperty<Function<T, ? extends Node>> mapperProperty() {
		return mapperProperty;
	}

	/**
	 * @return Property wrapper of current selected item.
	 */
	public ObjectProperty<T> selectedItemProperty() {
		return selectedItem;
	}

	/**
	 * @return Menu items.
	 */
	public ObservableList<T> getItems() {
		return items;
	}

	@Override
	protected Skin<?> createDefaultSkin() {
		return new CtxSkin<>(this);
	}

	/**
	 * Base UI implementation for {@link VirtualizedContextMenu}.
	 *
	 * @param <T>
	 * 		Item type.
	 *
	 * @see CtxCell Per-item UI implementation.
	 */
	private static class CtxSkin<T> implements Skin<PopupControl> {
		private final VirtualFlow<T, CtxCell<T>> flow;
		private final VirtualizedContextMenu<T> menu;
		private final Node node;

		/**
		 * @param menu
		 * 		Component to be a skin for.
		 */
		public CtxSkin(VirtualizedContextMenu<T> menu) {
			this.menu = menu;
			flow = VirtualFlow.createVertical(menu.items, i -> {
				CtxCell<T> cell = new CtxCell<>(this);
				cell.updateItem(i);
				cell.setFocused(menu.selectedItem.get() == i);
				return cell;
			});
			flow.setFocusTraversable(true);
			flow.getStyleClass().add("context-menu");
			flow.setPrefWidth(menu.getPrefWidth());
			flow.setPrefHeight(menu.getPrefHeight());
			flow.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
				T selected = menu.selectedItem.get();
				int itemsSize = menu.items.size();
				int index = menu.items.indexOf(selected);
				switch (e.getCode()) {
					case UP:
						if (selected == null && itemsSize > 0) {
							select(0);
							break;
						}
						if (index > 0)
							select(index - 1);
						break;
					case DOWN:
						if (selected == null && itemsSize > 0) {
							select(0);
							break;
						}
						if (index < itemsSize - 1)
							select(index + 1);
						break;
					case ENTER:
						menu.runAction();
						break;
					default:
						// Unhandled input, close menu
						menu.hide();
						return;
				}
				// Do not propagate the event.
				e.consume();
			});
			node = new VirtualizedScrollPane<>(flow);
		}

		/**
		 * Marks the given index as the selected item in the {@link VirtualizedContextMenu}.
		 * All other items are de-selected.
		 *
		 * @param index
		 * 		Index to select.
		 */
		private void select(int index) {
			T item = menu.items.get(index);
			menu.selectedItem.set(item);
			// Update focused state of visible cells
			int start = flow.getFirstVisibleIndex();
			int end = flow.getLastVisibleIndex() + 1; // +1 is odd, but fixes 'show' not keeping focus of target index
			for (int i = start; i < end; i++)
				flow.getCell(i).setFocused(i == index);
			// Ensure selected item is visible
			if (index < start || index >= end)
				flow.show(index);
		}

		@Override
		public PopupControl getSkinnable() {
			return menu;
		}

		@Override
		public Node getNode() {
			return node;
		}

		@Override
		public void dispose() {
			flow.dispose();
		}
	}

	/**
	 * UI implementation for items of each {@code T} value.
	 *
	 * @param <T>
	 * 		Item type.
	 */
	private static class CtxCell<T> implements Cell<T, Node> {
		private static final PseudoClass FOCUSED_PSEUDO_CLASS = PseudoClass.getPseudoClass("focused");
		private final BorderPane node = new BorderPane();
		private final CtxSkin<T> skin;
		private T item;

		/**
		 * @param skin
		 * 		Parent UI.
		 */
		public CtxCell(CtxSkin<T> skin) {
			this.skin = skin;
			node.getStyleClass().add("menu-item");
			node.setOnMousePressed(e -> {
				if (item != null) {
					skin.menu.selectedItem.set(item);
					skin.menu.runAction();
				}
			});
		}

		@Override
		public boolean isReusable() {
			return true;
		}

		@Override
		public void reset() {
			node.setLeft(null);
		}

		@Override
		public void updateItem(T item) {
			// Update held item
			this.item = item;
			// Update display
			if (item == null) {
				reset();
			} else {
				Node cellContent = skin.menu.mapperProperty.getValue().apply(item);
				node.setLeft(cellContent);
				// New content needs to know if it is focused, and we don't have an index check due to the
				// nature of VirtualFlows, so we do an equals check instead which isn't ideal but should suffice.	
				setFocused(skin.menu.selectedItem.isEqualTo(item).get());
			}
		}

		@Override
		public Node getNode() {
			return node;
		}

		/**
		 * @param status
		 *        {@code true} to set this item as the selected item.
		 */
		public void setFocused(boolean status) {
			node.pseudoClassStateChanged(FOCUSED_PSEUDO_CLASS, status);
		}
	}
}