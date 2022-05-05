package me.coley.recaf.ui.pane.assembler;

import javafx.beans.value.ObservableValueBase;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.assemble.analysis.Analysis;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.pipeline.AssemblerPipeline;
import me.coley.recaf.code.*;
import me.coley.recaf.ui.behavior.MemberEditor;
import me.coley.recaf.ui.behavior.SaveResult;
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
	private final AssemblerPipeline pipeline;

	/**
	 * @param assemblerArea
	 * 		Associated assembler area to interact with.
	 * @param pipeline
	 * 		Assembler pipeline.
	 */
	public StackAnalysisPane(AssemblerArea assemblerArea, AssemblerPipeline pipeline) {
		this.pipeline = pipeline;
		setCenter(new SplitPane(variableView, stackView));
		assemblerArea.currentParagraphProperty().addListener((observable, oldIndex, currentIndex) -> {
			onIndexChange(currentIndex);
		});
	}

	private void onIndexChange(int paragraphIndex) {
		if (pipeline == null || pipeline.getUnit() == null)
			return;
		Unit unit = pipeline.getUnit();
		Element element = unit.getCode().getChildOnLine(paragraphIndex + 1);
		if (element instanceof AbstractInstruction) {
			int insnIndex = unit.getCode().getInstructions().indexOf(element);
			Analysis analysis = pipeline.getLastAnalysis();
			Frame frame = analysis.frame(insnIndex);
			variableView.update(frame);
			stackView.update(frame);
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

	private static void populate(IndexedCell<Value> cell, Value item) {
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
			Node graphic = null;
			Type type = ((Value.ObjectValue) item).getType();
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
			cell.setGraphic(graphic);
		} else if (item instanceof Value.HandleValue) {
			// Handle reference
			HandleInfo handleInfo = ((Value.HandleValue) item).getInfo();
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
			cell.setGraphic(graphic);
		} else {
			cell.setGraphic(null);
		}
		cell.setText(item.toString());
	}

	static class FrameStackView extends ListView<Value> {
		public FrameStackView() {
			setCellFactory(param -> new ValueListCell());
		}

		public void update(Frame frame) {
			setItems(FXCollections.observableArrayList(frame.getStack()));
		}

		static class ValueListCell extends ListCell<Value> {
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

	static class FrameVariableTable extends TableView<String> {
		private Frame frame;

		@SuppressWarnings("unchecked")
		public FrameVariableTable() {
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

			getColumns().addAll(colName, colValue);
		}

		public void update(Frame frame) {
			this.frame = frame;
			if (frame == null) {
				setItems(FXCollections.emptyObservableList());
			} else {
				SortedSet<String> sorted = new TreeSet<>(frame.getLocals().keySet());
				setItems(FXCollections.observableArrayList(sorted));
			}
		}

		static class ValueTableCell extends TableCell<String, Value> {
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
