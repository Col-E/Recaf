package me.coley.recaf.ui.component;

import java.util.List;

import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import me.coley.recaf.Logging;
import me.coley.recaf.config.impl.ConfBlocks;
import me.coley.recaf.config.impl.ConfBlocks.Block;
import me.coley.recaf.ui.FormatFactory;
import me.coley.recaf.ui.component.editor.InsnListEditor;
import me.coley.recaf.util.JavaFX;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.Threads;

/**
 * Panes for use in the block save/load windows.
 * 
 * @author Matt
 */
public class BlockPane extends BorderPane {
	public static class Saver extends BlockPane {
		public Saver(List<AbstractInsnNode> opcodes, MethodNode method) {
			// show selected opcodes as list
			ListView<AbstractInsnNode> list = new ListView<>();
			list.setCellFactory(cell -> new ListCell<AbstractInsnNode>() {
				@Override
				protected void updateItem(AbstractInsnNode node, boolean empty) {
					super.updateItem(node, empty);
					if (empty || node == null) {
						setGraphic(null);
					} else {
						setGraphic(FormatFactory.opcode(node, method));
					}
				}
			});
			list.getItems().addAll(opcodes);
			setCenter(list);
			// add menu buttons to save / name collection
			HBox menu = new HBox();
			TextField name = new TextField();
			ActionButton btn = new ActionButton(Lang.get("misc.save"), () -> save(name.getText(), opcodes));
			menu.getChildren().add(name);
			menu.getChildren().add(btn);
			setBottom(menu);
			Threads.runFx(() -> name.requestFocus());
		}

		/**
		 * Save opcodes as a block by the given name.
		 * 
		 * @param text
		 *            Block name.
		 * @param opcodes
		 *            Block contents.
		 */
		private void save(String text, List<AbstractInsnNode> opcodes) {
			ConfBlocks.instance().add(text, opcodes);
			// close inserter window
			Stage stage = (Stage) getScene().getWindow();
			stage.close();
		}
	}

	public static class Inserter extends BlockPane {
		public Inserter(AbstractInsnNode ain, InsnListEditor editor) {
			// Textfield that will hold selected block name.
			TextField txtName = new TextField();
			// show all blocks
			GridView<Block> gv = new GridView<>();
			gv.setCellFactory(new Callback<GridView<Block>, GridCell<Block>>() {
				public GridCell<Block> call(GridView<Block> gridView) {
					return new GridCell<Block>() {
						@Override
						protected void updateItem(Block block, boolean empty) {
							super.updateItem(block, empty);
							if (empty || block == null) {
								setGraphic(null);
							} else {
								ListView<AbstractInsnNode> list = new ListView<>();
								list.setCellFactory(cell -> new ListCell<AbstractInsnNode>() {
									@Override
									protected void updateItem(AbstractInsnNode node, boolean empty) {
										super.updateItem(node, empty);
										if (empty || node == null) {
											setGraphic(null);
										} else {
											setGraphic(FormatFactory.opcode(node, null));
										}
									}
								});
								list.getItems().addAll(block.list);
								TitledPane tp = new TitledPane(block.name, list);
								tp.setCollapsible(false);
								tp.setOnMouseClicked(e -> txtName.setText(block.name));
								setGraphic(tp);
							}
						}
					};
				}
			});
			gv.cellHeightProperty().set(180);
			int padding = 11;
			gv.prefWidthProperty().bind(widthProperty().subtract(15));
			gv.cellWidthProperty().bind(widthProperty().divide(2).subtract((padding * 4)));
			for (String name : ConfBlocks.instance().getMaps().keySet()) {
				gv.getItems().add(ConfBlocks.instance().getClone(name));
			}
			ScrollPane scroll = new ScrollPane(gv);
			setCenter(scroll);
			// add menu buttons to save / name collection
			HBox menu = new HBox();
			ComboBox<InsertMode> comboLocation = new ComboBox<>(JavaFX.observableList(InsertMode.values()));
			comboLocation.setValue(InsertMode.AFTER);
			ActionButton btn = new ActionButton(Lang.get("misc.load"), () -> load(txtName.getText(), comboLocation.getValue(),
					editor, ain));
			menu.getChildren().add(txtName);
			menu.getChildren().add(btn);
			menu.getChildren().add(comboLocation);
			setBottom(menu);
			Threads.runFx(() -> txtName.requestFocus());
		}

		private void load(String text, InsertMode mode, InsnListEditor editor, AbstractInsnNode location) {
			Block block = ConfBlocks.instance().getClone(text);
			if (block == null) {
				Logging.error("Failed to load block by name '" + text + "'");
				return;
			}
			Threads.runFx(() -> {
				// update underlying list
				ObservableList<AbstractInsnNode> obsList = editor.getOpcodeList().getItems();
				int index = obsList.indexOf(location);
				if (mode == InsertMode.BEFORE) {
					obsList.addAll(index, block.list);
				} else {
					obsList.addAll(index + 1, block.list);
				}
				// close inserter window
				Stage stage = (Stage) getScene().getWindow();
				stage.close();
			});
		}
	}
}
