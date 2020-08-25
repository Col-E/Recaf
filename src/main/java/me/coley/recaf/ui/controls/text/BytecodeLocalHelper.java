package me.coley.recaf.ui.controls.text;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.*;
import me.coley.recaf.parse.bytecode.MethodAssembler;
import me.coley.recaf.parse.bytecode.ast.AST;
import me.coley.recaf.parse.bytecode.ast.LabelAST;
import me.coley.recaf.parse.bytecode.ast.VarInsnAST;
import me.coley.recaf.ui.controls.ActionMenuItem;
import me.coley.recaf.util.LangUtil;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;

import static me.coley.recaf.util.LangUtil.translate;

/**
 * UI component that displays variable information.
 *
 * @author Matt
 */
public class BytecodeLocalHelper extends TableView<LocalVariableNode> {
	private final BytecodeEditorPane parent;
	private MethodAssembler assembler;
	private ContextMenu ctxMenu;

	/**
	 * @param parent
	 * 		Parent editor pane this belongs to.
	 */
	public BytecodeLocalHelper(BytecodeEditorPane parent) {
		this.parent = parent;
		getStyleClass().add("monospaced");
		// Column setup
		TableColumn<LocalVariableNode, Integer> colIndex =
				new TableColumn<>(translate("ui.bean.method.localvariable.index"));
		TableColumn<LocalVariableNode, LabelNode> colStart =
				new TableColumn<>(translate("ui.bean.method.localvariable.start"));
		TableColumn<LocalVariableNode, LabelNode> colEnd =
				new TableColumn<>(translate("ui.bean.method.localvariable.end"));
		TableColumn<LocalVariableNode, String> colName =
				new TableColumn<>(translate("ui.bean.method.localvariable.name"));
		TableColumn<LocalVariableNode, String> colDesc =
				new TableColumn<>(translate("ui.bean.method.localvariable.desc"));
		colIndex.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().index));
		colName.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().name));
		colDesc.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().desc));
		colStart.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().start));
		colEnd.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().end));
		colStart.setCellFactory(col -> new TableCell<LocalVariableNode, LabelNode>() {
			@Override
			public void updateItem(LabelNode item, boolean empty) {
				super.updateItem(item, empty);
				if (item == null) {
					setText(null);
				} else if (assembler != null) {
					LabelAST ast = assembler.getCompilation().getLabelAst(item);
					setText(ast == null ? "?" : ast.getName().print());
				} else {
					setText("?");
				}
			}
		});
		colEnd.setCellFactory(colStart.getCellFactory());
		getColumns().add(colIndex);
		getColumns().add(colStart);
		getColumns().add(colEnd);
		getColumns().add(colName);
		getColumns().add(colDesc);
		// Row setup
		setRowFactory(t -> new TableRow<LocalVariableNode>() {
			@Override
			protected void updateItem(LocalVariableNode item, boolean empty) {
				super.updateItem(item, empty);
				// Register double click action
				setOnMousePressed(e -> {
					if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
						// Goto first usage
						for (AST ast : parent.getLastParse().getRoot().getChildren()) {
							if (ast instanceof VarInsnAST &&
									((VarInsnAST) ast).getVariableName().getName().equals(item.name)) {
								gotoAst(ast);
								return;
							}
						}
					}
				});
				// Show context menu
				setOnContextMenuRequested(e -> {
					// close old context menu
					if (ctxMenu != null)
						ctxMenu.hide();
					// No context menu if no parse
					if (parent.getLastParse() == null)
						return;
					// Show where local is used
					ctxMenu = new ContextMenu();
					Menu refs = new Menu(LangUtil.translate("ui.edit.method.referrers"));
					for (AST ast : parent.getLastParse().getRoot().getChildren()) {
						if (ast instanceof VarInsnAST &&
								((VarInsnAST) ast).getVariableName().getName().equals(item.name)) {
							MenuItem ref = new ActionMenuItem(ast.getLine() + ": " + ast.print(), () -> {
								gotoAst(ast);
							});
							refs.getItems().add(ref);
						}
					}
					if (refs.getItems().isEmpty())
						refs.setDisable(true);
					ctxMenu.getItems().add(refs);
					ctxMenu.show(this, e.getScreenX(), e.getScreenY());
				});
			}
		});
		// Sizing
		setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		colIndex.setMaxWidth(Integer.MAX_VALUE / 5.0);
		colStart.setMaxWidth(Integer.MAX_VALUE / 5.0);
		colEnd.setMaxWidth(Integer.MAX_VALUE / 5.0);
		colName.setMaxWidth(Integer.MAX_VALUE / 2.0);
		colDesc.setMaxWidth(Integer.MAX_VALUE);
	}

	private void gotoAst(AST ast) {
		int line = ast.getLine() - 1;
		parent.codeArea.moveTo(line, 0);
		parent.codeArea.requestFollowCaret();
	}

	/**
	 * Updates information in the table.
	 *
	 * @param assembler
	 * 		Most recent assembler instance.
	 */
	public void setMethodAssembler(MethodAssembler assembler) {
		this.assembler = assembler;
		getItems().clear();
		if (assembler.getLastCompile() == null)
			return;
		if (assembler.getLastCompile().localVariables != null)
			getItems().addAll(assembler.getLastCompile().localVariables);
	}
}
