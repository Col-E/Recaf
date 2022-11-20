package me.coley.recaf.ui.pane.assembler;

import javafx.beans.value.ObservableValueBase;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.assemble.ContextualPipeline;
import me.coley.recaf.assemble.analysis.Analysis;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.code.*;
import me.coley.recaf.ui.behavior.MemberEditor;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.BoundLabel;
import me.coley.recaf.ui.control.code.bytecode.AssemblerArea;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.Type;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A subcomponent of {@link AssemblerPane} that shows state of variables and the stack at the
 * current selected line.
 *
 * @author Matt Coley
 */
public class StackAnalysisPane extends BorderPane implements MemberEditor {
	private final FrameVariableTable variableView = new FrameVariableTable();
	private final FrameStackView stackView = new FrameStackView();
	private final ContextualPipeline pipeline;

	/**
	 * @param assemblerArea
	 * 		Associated assembler area to interact with.
	 * @param pipeline
	 * 		Assembler pipeline.
	 */
	public StackAnalysisPane(AssemblerArea assemblerArea, ContextualPipeline pipeline) {
		this.pipeline = pipeline;
		BorderPane stackWrapper = new BorderPane(stackView);
		Label stackTitle = new BoundLabel(Lang.getBinding("assembler.analysis.stack"));
		stackTitle.getStyleClass().add("analysis-list-header");
		stackTitle.prefWidthProperty().bind(stackWrapper.widthProperty());
		stackTitle.setAlignment(Pos.CENTER);
		stackWrapper.setTop(stackTitle);
		setCenter(new SplitPane(variableView, stackWrapper));
		assemblerArea.caretPositionProperty().addListener((observable, oldIndex, currentIndex) -> {
			onCaretPosUpdate(assemblerArea.getCurrentParagraph(), assemblerArea.getCaretColumn());
		});
	}

	private void onCaretPosUpdate(int paragraphIndex, int columnIndex) {
		if (pipeline == null || pipeline.getUnit() == null)
			return;
		Analysis analysis = pipeline.getLastAnalysis();
		if (analysis == null) {
			// Clear if we are working on a class-level and 'lose' selection of the method.
			if (pipeline.isClass()) {
				variableView.getItems().clear();
				stackView.getItems().clear();
			}
			return;
		}
		if (!pipeline.isCurrentMethod())
			return;
		Code code = pipeline.getCurrentMethod().getCode();
		Element element = code.getChildAt(paragraphIndex + 1, columnIndex);
		if (element instanceof AbstractInstruction) {
			int insnIndex = code.getInstructions().indexOf(element);
			if (insnIndex < analysis.getFrames().size()) {
				Frame frame = analysis.frame(insnIndex);
				variableView.update(frame);
				stackView.update(frame);
			}
		}
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		// no-op
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		// Not relevant here
		return null;
	}

	@Override
	public MemberInfo getTargetMember() {
		throw new UnsupportedOperationException("Unused");
	}

	@Override
	public void setTargetMember(MemberInfo targetMember) {
		// Not relevant here
	}

	@Override
	public boolean supportsMemberSelection() {
		// Not relevant here
		return false;
	}

	@Override
	public boolean isMemberSelectionReady() {
		// Not relevant here
		return false;
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		// Not relevant here
	}

	@Override
	public SaveResult save() {
		return SaveResult.IGNORED;
	}

	@Override
	public boolean supportsEditing() {
		return false;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	/**
	 * Populates the table/list cell with text/graphics for the given value.
	 *
	 * @param cell
	 * 		Cell to update.
	 * @param item
	 * 		Cell value.
	 */
	private static void populate(IndexedCell<Value> cell, Value item) {
		String text = item == null ? null : item.toString();
		if (item instanceof Value.EmptyPoppedValue || item instanceof Value.WideReservedValue) {
			// Internal
			cell.setGraphic(Icons.getIconView(Icons.INTERNAL));
		} else if (item instanceof Value.NumericValue) {
			// Primitive
			cell.setGraphic(Icons.getIconView(Icons.PRIMITIVE));
		} else if (item instanceof Value.NullValue) {
			// Null constant
			cell.setGraphic(Icons.getIconView(Icons.NULL));
		} else if (item instanceof Value.ObjectValue) {
			// Object
			cell.setGraphic(createObjectGraphic((Value.ObjectValue) item));
		} else if (item instanceof Value.ArrayValue) {
			// Array
			cell.setGraphic(Icons.getIconView(Icons.ARRAY));
		} else if (item instanceof Value.HandleValue) {
			// Handle reference
			cell.setGraphic(createHandleGraphic((Value.HandleValue) item));
		} else if (item != null) {
			cell.setGraphic(null);
		} else {
			// TODO: This should not happen
			cell.setGraphic(null);
		}
		cell.setText(text);
	}

	private static Node createObjectGraphic(Value.ObjectValue item) {
		Node graphic = null;
		Type type = item.getType();
		if (item instanceof Value.TypeValue) {
			// Class<T> object
			type = ((Value.TypeValue) item).getArgumentType();
		}
		String internalName = type.getInternalName();
		Workspace workspace = RecafUI.getController().getWorkspace();
		if (workspace != null) {
			ClassInfo info = workspace.getResources().getClass(internalName);
			if (info != null) {
				graphic = Icons.getClassIcon(info);
			}
		}
		if (graphic == null) {
			graphic = Icons.getIconView(Icons.CLASS);
		}
		return graphic;
	}

	private static Node createHandleGraphic(Value.HandleValue item) {
		HandleInfo handleInfo = item.getInfo();
		Workspace workspace = RecafUI.getController().getWorkspace();
		Node graphic = null;
		if (workspace != null) {
			ClassInfo info = workspace.getResources().getClass(handleInfo.getOwner());
			if (info != null) {
				if (handleInfo.getDesc().charAt(0) == '(') {
					MethodInfo methodInfo = info.findMethod(handleInfo.getName(), handleInfo.getDesc());
					if (methodInfo != null) {
						graphic = Icons.getMethodIcon(methodInfo);
					}
				} else {
					FieldInfo fieldInfo = info.findField(handleInfo.getName(), handleInfo.getDesc());
					if (fieldInfo != null) {
						graphic = Icons.getFieldIcon(fieldInfo);
					}
				}
			}
		}
		if (graphic == null) {
			graphic = Icons.getIconView(Icons.INTERNAL);
		}
		return graphic;
	}

	/**
	 * Wrapper of list-view to support drawing {@link Value} cells.
	 */
	static class FrameStackView extends ListView<Value> {
		public FrameStackView() {
			getStyleClass().add("analysis-list");
			setCellFactory(param -> new ValueListCell());
		}

		/**
		 * @param frame
		 * 		Frame to pull stack data from.
		 */
		public void update(Frame frame) {
			setItems(FXCollections.observableArrayList(frame.getStack()));
		}

		private static class ValueListCell extends ListCell<Value> {
			public ValueListCell() {
				getStyleClass().add("analysis-list-cell");
			}

			@Override
			protected void updateItem(Value item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
					setContextMenu(null);
				} else {
					populate(this, item);
				}
			}
		}
	}

	/**
	 * Wrapper of table-view to support drawing {@link Value} cells.
	 */
	static class FrameVariableTable extends TableView<String> {
		private Frame frame;

		@SuppressWarnings("unchecked")
		public FrameVariableTable() {
			getStyleClass().add("analysis-table");
			TableColumn<String, String> colName = new TableColumn<>();
			colName.textProperty().bind(Lang.getBinding("assembler.analysis.name-column"));
			colName.setCellValueFactory(param -> new ObservableValueBase<>() {
				@Override
				public String getValue() {
					return param.getValue();
				}
			});
			TableColumn<String, Value> colValue = new TableColumn<>();
			colValue.textProperty().bind(Lang.getBinding("assembler.analysis.value-column"));
			colValue.setCellFactory(param -> new ValueTableCell());
			colValue.setCellValueFactory(param -> new ObservableValueBase<>() {
				@Override
				public Value getValue() {
					if (frame == null)
						return null;
					return frame.getLocal(param.getValue());
				}
			});
			colName.prefWidthProperty().bind(widthProperty().multiply(0.295));
			colValue.prefWidthProperty().bind(widthProperty().multiply(0.70));
			getColumns().addAll(colName, colValue);
		}

		/**
		 * @param frame
		 * 		Frame to pull local variable data from.
		 */
		public void update(Frame frame) {
			this.frame = frame;
			if (frame == null) {
				setItems(FXCollections.emptyObservableList());
			} else {
				SortedSet<String> sorted = new TreeSet<>(frame.getLocals().keySet());
				setItems(FXCollections.observableArrayList(sorted));
			}
		}

		private static class ValueTableCell extends TableCell<String, Value> {
			@Override
			protected void updateItem(Value item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
					setContextMenu(null);
				} else {
					populate(this, item);
				}
			}
		}
	}
}
