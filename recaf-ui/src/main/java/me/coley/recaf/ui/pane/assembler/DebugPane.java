package me.coley.recaf.ui.pane.assembler;

import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.pipeline.AssemblerPipeline;
import me.coley.recaf.assemble.pipeline.ParserCompletionListener;
import me.coley.recaf.ui.control.code.bytecode.AssemblerArea;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Location;
import me.darknet.assembler.parser.Token;

import java.util.Collection;
import java.util.List;

/**
 * A utility pane to add to {@link AssemblerPane} to debug the parsed tokens/groups.
 *
 * @author Justus Garbe
 */
public class DebugPane extends BorderPane implements ParserCompletionListener {
	private final GroupTreeView treeView;
	private final TokenList listView;
	private final AssemblerArea assemblerArea;

	/**
	 * @param area
	 * 		Parent assembler area.
	 * @param pipeline
	 * 		Pipeline associated with the assembler pane.
	 */
	public DebugPane(AssemblerArea area, AssemblerPipeline pipeline) {
		super();
		treeView = new GroupTreeView();
		listView = new TokenList();
		setCenter(new SplitPane(treeView, listView));
		assemblerArea = area;
		pipeline.addParserCompletionListener(this);
	}

	@Override
	public void onCompleteTokenize(List<Token> tokens) {
		listView.update(tokens);
	}

	@Override
	public void onCompleteParse(List<Group> groups) {
		treeView.update(groups);
	}

	@Override
	public void onCompleteTransform(Unit unit) {
		// No-op
	}

	private class TokenList extends ListView<Token> {
		private void update(Collection<Token> tokens) {
			FxThreadUtil.run(() -> {
				getItems().clear();
				for (Token token : tokens) {
					getItems().add(token);
				}
				setCellFactory(param -> new ListCell<>() {
					@Override
					protected void updateItem(Token item, boolean empty) {
						super.updateItem(item, empty);
						if (empty) {
							setText(null);
						} else {
							Location loc = item.getLocation();
							setText(item.getType().name() + ": " +
									item.getContent() + " " +
									loc.getLine() + ":" +
									loc.getColumn());
							setOnMouseClicked(event -> {
								if (event.getClickCount() == 2) {
									assemblerArea.selectPosition(loc.getLine(), loc.getColumn());
								}
							});
						}
					}
				});
			});
		}
	}

	private class GroupTreeView extends TreeView<Group> {
		public GroupTreeView() {
			getStyleClass().add("transparent-tree");
		}

		public GroupTreeItem add(Group group) {
			GroupTreeItem item = new GroupTreeItem(group);
			for (Group child : group.getChildren()) {
				if(child != null) {
					item.getChildren().add(add(child));
				}
			}
			return item;
		}

		public void update(Collection<Group> groups) {
			GroupTreeItem root = new GroupTreeItem(null);
			setCellFactory(null);
			for (Group group : groups) {
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

	private static class GroupTreeItem extends TreeItem<Group> {
		public GroupTreeItem(Group group) {
			super(group);
		}
	}

	private class GroupTreeCell extends TreeCell<Group> {
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
			} else if (item == null) {
				setText("Root");
			} else {
				String content = item.getValue() == null ? "" : item.content();
				Location loc = item.getStartLocation();
				setText((loc.getLine() == -1 ? "" : "" + loc.getLine() + " ") + item.getType() + ": " +
						content);
				setOnMouseClicked(event -> {
					if (event.getClickCount() == 2) {
						assemblerArea.selectPosition(loc.getLine(), loc.getColumn());
						listView.getSelectionModel().select(item.getValue());
						listView.scrollTo(item.getValue());
					}
				});
			}
		}

		private boolean isRootBeingUpdated() {
			return getTreeView().getCellFactory() == null;
		}
	}
}
