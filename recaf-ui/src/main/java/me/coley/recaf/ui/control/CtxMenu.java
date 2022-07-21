package me.coley.recaf.ui.control;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.layout.BorderPane;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;

import java.util.Collection;
import java.util.function.Function;

public class CtxMenu<T> extends PopupControl {
    private final ObjectProperty<Function<T, ? extends Node>> mapperProperty = new SimpleObjectProperty<>(t -> {
        throw new UnsupportedOperationException();
    });
    private final ObservableList<T> items;

    public CtxMenu(Collection<T> items) {
        this.items = FXCollections.observableArrayList(items);
    }

    public CtxMenu(Function<T, ? extends Node> layoutMapper, Collection<T> items) {
        this(items);
        mapperProperty.set(layoutMapper);
    }

    public ObjectProperty<Function<T, ? extends Node>> mapperProperty() {
        return mapperProperty;
    }

    public ObservableList<T> getItems() {
        return items;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new CtxSkin<>(this, mapperProperty);
    }

    private static class CtxCell<T> implements Cell<T, Node> {
        private final BorderPane node = new BorderPane();
        private final CtxSkin<T> skin;

        public CtxCell(CtxSkin<T> skin) {
            this.skin = skin;
            this.node.getStyleClass().add("menu-item");
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
            node.setLeft(skin.layoutMapper.getValue().apply(item));
        }

        @Override
        public Node getNode() {
            return node;
        }
    }

    private static class CtxSkin<T> implements Skin<PopupControl> {
        private final VirtualFlow<T, CtxCell<T>> flow;
        private final ObjectProperty<Function<T, ? extends Node>> layoutMapper;
        private final CtxMenu<T> menu;

        public CtxSkin(CtxMenu<T> menu, ObjectProperty<Function<T, ? extends Node>> layoutMapper) {
            this.menu = menu;
            this.layoutMapper = layoutMapper;
            flow = VirtualFlow.createVertical(menu.items, i -> {
                CtxCell<T> row = new CtxCell<>(this);
                row.updateItem(i);
                return row;
            });
            this.flow.getStyleClass().add("context-menu");
            this.flow.setPrefWidth(300);
            this.flow.setPrefHeight(Math.min(menu.items.size() * 35, 630));
        }

        @Override
        public PopupControl getSkinnable() {
            return menu;
        }

        @Override
        public Node getNode() {
            return flow;
        }

        @Override
        public void dispose() {
            flow.dispose();
        }
    }
}