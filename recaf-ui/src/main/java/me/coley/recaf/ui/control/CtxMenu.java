package me.coley.recaf.ui.control;

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
    private final ObservableList<T> items;
    private final Function<T, Node> layoutMapper;

    public CtxMenu(Function<T, Node> layoutMapper, Collection<T> items) {
        this.items = FXCollections.observableArrayList(items);
        this.layoutMapper = layoutMapper;
    }

    public ObservableList<T> getItems() {
        return items;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new CtxSkin<>(this, layoutMapper);
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
            node.setLeft(skin.layoutMapper.apply(item));
        }

        @Override
        public Node getNode() {
            return node;
        }
    }

    private static class CtxSkin<T> implements Skin<PopupControl> {
        private final VirtualFlow<T, CtxCell<T>> flow;
        private final Function<T, Node> layoutMapper;
        private final CtxMenu<T> menu;

        public CtxSkin(CtxMenu<T> menu, Function<T, Node> layoutMapper) {
            this.menu = menu;
            this.layoutMapper = layoutMapper;
            flow = VirtualFlow.createVertical(menu.items, i -> {
                CtxCell<T> row = new CtxCell<>(this);
                row.updateItem(i);
                return row;
            });
            this.flow.getStyleClass().add("context-menu");
            this.flow.setPrefWidth(300);
            this.flow.setPrefHeight(600);
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