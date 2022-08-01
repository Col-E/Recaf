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

public class CtxMenu<T> extends PopupControl {
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
			return CtxMenu.this;
		}

		@Override
		public String getName() {
			return "onAction";
		}
	};
	private final ObservableList<T> items;
	private final ObjectProperty<T> selectedItem = new SimpleObjectProperty<>();

	public CtxMenu(Collection<T> items) {
		this.items = FXCollections.observableArrayList(items);
		getStyleClass().setAll(DEFAULT_STYLE_CLASS);
		setAutoHide(true);
		setConsumeAutoHidingEvents(false);
	}

	public CtxMenu(Function<T, ? extends Node> layoutMapper, Collection<T> items) {
		this(items);
		mapperProperty.set(layoutMapper);
	}

	private void runAction() {
		getOnAction().handle(null);
		hide();
	}

	public final EventHandler<ActionEvent> getOnAction() {
		return onActionProperty().get();
	}

	public final void setOnAction(EventHandler<ActionEvent> value) {
		onActionProperty().set(value);
	}

	public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
		return onAction;
	}

	public ObjectProperty<Function<T, ? extends Node>> mapperProperty() {
		return mapperProperty;
	}

	public ObjectProperty<T> selectedItemProperty() {
		return selectedItem;
	}

	public ObservableList<T> getItems() {
		return items;
	}

	@Override
	protected Skin<?> createDefaultSkin() {
		return new CtxSkin<>(this, mapperProperty);
	}

	private static class CtxSkin<T> implements Skin<PopupControl> {
		private final VirtualFlow<T, CtxCell<T>> flow;
		private final ObjectProperty<Function<T, ? extends Node>> layoutMapper;
		private final CtxMenu<T> menu;
		private final Node node;

		public CtxSkin(CtxMenu<T> menu, ObjectProperty<Function<T, ? extends Node>> layoutMapper) {
			this.menu = menu;
			this.layoutMapper = layoutMapper;
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
			// TODO: When moving requires scrolling to fit the next selected item in view, it doesn't 'appear' as focused
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

		private void select(int index) {
			T item = menu.items.get(index);
			menu.selectedItem.set(item);
			// Update focused state of visible cells
			int start = flow.getFirstVisibleIndex();
			int end = flow.getLastVisibleIndex();
			for (int i = start; i < end; i++)
				flow.getCell(i).setFocused(i == index);
			// Ensure selected item is visible
			if (index <= start || index >= end)
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

	private static class CtxCell<T> implements Cell<T, Node> {
		private static final PseudoClass FOCUSED_PSEUDO_CLASS = PseudoClass.getPseudoClass("focused");
		private final BorderPane node = new BorderPane();
		private final CtxSkin<T> skin;
		private T item;

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
				Node cellContent = skin.layoutMapper.getValue().apply(item);
				node.setLeft(cellContent);
			}
		}

		@Override
		public Node getNode() {
			return node;
		}

		public void setFocused(boolean status) {
			node.pseudoClassStateChanged(FOCUSED_PSEUDO_CLASS, status);
		}
	}
}