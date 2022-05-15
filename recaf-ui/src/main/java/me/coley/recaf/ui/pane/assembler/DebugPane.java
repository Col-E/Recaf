package me.coley.recaf.ui.pane.assembler;

import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.pipeline.AssemblerPipeline;
import me.coley.recaf.assemble.pipeline.ParserCompletionListener;
import me.coley.recaf.ui.control.code.bytecode.AssemblerArea;
import me.coley.recaf.ui.pane.OutlinePane;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Location;
import me.darknet.assembler.parser.Token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DebugPane extends BorderPane implements ParserCompletionListener {

    public GroupTreeView treeView;
    public TokenList listView;
    public AssemblerArea assemblerPane;

    public DebugPane(AssemblerArea pane, AssemblerPipeline pipeline) {
        super();
        treeView = new GroupTreeView();
        listView = new TokenList();
        setCenter(new SplitPane(treeView, listView));
        assemblerPane = pane;
        pipeline.addParserCompletionListener(this);
    }

    @Override
    public void onCompleteTokenize(Collection<Token> tokens) {
        listView.update(tokens);
    }

    @Override
    public void onCompleteParse(Collection<Group> groups) {
        treeView.update(groups);
    }

    @Override
    public void onCompleteTransform(Unit unit) {
        // No-op
    }

    class TokenList extends ListView<Token> {
        public TokenList() {
            super();
        }

        public void update(Collection<Token> tokens) {
            FxThreadUtil.run(() -> {
                getItems().clear();
                for(Token token : tokens) {
                    getItems().add(token);
                }
                setCellFactory(param -> new ListCell<>() {
                    @Override
                    protected void updateItem(Token item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText(null);
                        } else {
                            setText(item.type.name() + ": " + item.content + " " + item.location.line + ":" + item.location.column);
                            Location loc = item.location;
                            setOnMouseClicked(event -> {
                                if (event.getClickCount() == 2) {
                                    assemblerPane.selectPosition(loc.line, loc.column);
                                }
                            });
                        }
                    }
                });
            });
        }
    }

    class GroupTreeView extends TreeView<Group> {

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

    class GroupTreeItem extends TreeItem<Group> {
        public GroupTreeItem(Group group) {
            super(group);
        }
    }

    class GroupTreeCell extends TreeCell<Group> {


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
                Location loc  = item.location();
                setText((loc.line == -1 ? "" : ""  +loc.line + " ") + item.type + ": " +
                        content);
                setOnMouseClicked(event -> {
                    if(event.getClickCount() == 2) {
                        assemblerPane.selectPosition(loc.line, loc.column);
                        listView.getSelectionModel().select(item.value);
                        listView.scrollTo(item.value);
                    }
                });
            }
        }

        private boolean isRootBeingUpdated() {
            return getTreeView().getCellFactory() == null;
        }

    }

}
