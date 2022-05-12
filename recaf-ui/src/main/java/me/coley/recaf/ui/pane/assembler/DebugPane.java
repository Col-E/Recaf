package me.coley.recaf.ui.pane.assembler;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.pipeline.AssemblerPipeline;
import me.coley.recaf.assemble.pipeline.ParserCompletionListener;
import me.coley.recaf.ui.control.code.bytecode.AssemblerArea;
import me.coley.recaf.ui.pane.OutlinePane;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DebugPane extends BorderPane implements ParserCompletionListener {

    public GroupTreeView treeView;
    static AssemblerArea assemblerPane;

    public DebugPane(AssemblerArea pane, AssemblerPipeline pipeline) {
        super();
        treeView = new GroupTreeView();
        setCenter(treeView);
        assemblerPane = pane;
        pipeline.addParserCompletionListener(this);
    }

    @Override
    public void onCompleteParse(Collection<Group> groups) {
        treeView.update(groups);
    }

    @Override
    public void onCompleteTransform(Unit unit) {
        // No-op
    }

    static class GroupTreeView extends TreeView<Group> {

        public GroupTreeView() {
            getStyleClass().add("transparent-tree");
        }

        public GroupTreeItem add(Group group) {
            GroupTreeItem item = new GroupTreeItem(group);
            for(Group child : group.getChildren()) {
                item.getChildren().add(add(child));
            }
            return item;
        }

        public void update(Collection<Group> groups) {
            GroupTreeItem root = new GroupTreeItem(null);
            setCellFactory(null);
            for(Group group : groups) {
                root.getChildren().add(add(group));
            }
            FxThreadUtil.run(() -> {
                setRoot(root);
                // Now that the root is set we can reinstate the intended cell factory. Cells for the root and its children
                // will use this factory when the FX thread requests them.
                setCellFactory(param -> new GroupTreeCell());
            });
        }

    }

    static class GroupTreeItem extends TreeItem<Group> {
        public GroupTreeItem(Group group) {
            super(group);
        }
    }

    static class GroupTreeCell extends TreeCell<Group> {


        public GroupTreeCell() {
            getStyleClass().add("transparent-cell");
            getStyleClass().add("monospace");
        }

        @Override
        protected void updateItem(Group item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || isRootBeingUpdated()) {
                setText(null);
                setGraphic(null);
                setOnMouseClicked(null);
                setContextMenu(null);
            }else if(item == null){
                setText("Root");
            }else {
                String content = item.value == null ? "" : item.content();
                setText(item.type + ": " +
                        content);
                Location loc = item.location();
                setOnMouseClicked(event -> {
                    if(event.getClickCount() == 2) {
                        System.out.println(loc);
                        assemblerPane.selectPosition(loc.line, loc.column);
                    }
                });
            }
        }

        private boolean isRootBeingUpdated() {
            return getTreeView().getCellFactory() == null;
        }

    }

}
