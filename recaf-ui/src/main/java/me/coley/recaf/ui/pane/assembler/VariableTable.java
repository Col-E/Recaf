package me.coley.recaf.ui.pane.assembler;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.pipeline.AssemblerPipeline;
import me.coley.recaf.assemble.pipeline.AstValidationListener;
import me.coley.recaf.assemble.pipeline.PipelineCompletionListener;
import me.coley.recaf.assemble.transformer.VariableInfo;
import me.coley.recaf.assemble.transformer.Variables;
import me.coley.recaf.assemble.util.InheritanceChecker;
import me.coley.recaf.assemble.validation.Validator;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.ui.behavior.MemberEditor;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.code.bytecode.AssemblerArea;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.Threads;
import me.coley.recaf.util.WorkspaceInheritanceChecker;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A subcomponent of {@link AssemblerPane} that shows all variables defined in the current method.
 *
 * @author Matt Coley
 */
public class VariableTable extends BorderPane implements MemberEditor {
	private final TableView<VariableInfo> tableView = new TableView<>();
	private final AssemblerPipeline pipeline;

	/**
	 * @param assemblerArea
	 * 		Associated assembler area to interact with.
	 * @param pipeline
	 * 		Assembler pipeline.
	 */
	public VariableTable(AssemblerArea assemblerArea, AssemblerPipeline pipeline) {
		this.pipeline = pipeline;

		InheritanceChecker checker = WorkspaceInheritanceChecker.getInstance();

		TableColumn<VariableInfo, Integer> colIndex = new TableColumn<>("Index");
		TableColumn<VariableInfo, String> colName = new TableColumn<>("Name");
		TableColumn<VariableInfo, String> colType = new TableColumn<>("Type");
		colIndex.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getIndex()));
		colName.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getName()));
		colType.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCommonType(checker).getInternalName()));

		colIndex.prefWidthProperty().bind(tableView.widthProperty().multiply(0.1));
		colName.prefWidthProperty().bind(tableView.widthProperty().multiply(0.4));
		colType.prefWidthProperty().bind(tableView.widthProperty().multiply(0.5));

		tableView.setOnMouseClicked(e -> {
			// Can't do anything if no item is selected
			VariableInfo item = tableView.getSelectionModel().getSelectedItem();
			if (item == null)
				return;
			// Get references to the selected variable
			List<Element> sources = item.getSources().stream()
					.filter(element -> element instanceof AbstractInstruction)
					.collect(Collectors.toList());
			if (sources.isEmpty())
				return;
			// Current line / selected element
			int line = assemblerArea.getCurrentParagraph() + 1;
			Element selected = pipeline.getElementOnLine(line);
			int currentIndex = sources.indexOf(selected);
			// Get target (next element)
			int targetIndex = (currentIndex + 1) % sources.size();
			Element target = sources.get(targetIndex);
			int targetPos = target.getStart();
			int targetLine = target.getLine();
			// Select it
			if (line != targetLine) {
				assemblerArea.selectPosition(targetPos);
				Threads.runFxDelayed(10, () -> {
					assemblerArea.selectLine();
					assemblerArea.centerParagraph(targetLine);
				});
			} else {
				assemblerArea.selectLine();
			}
		});

		tableView.getColumns().add(colIndex);
		tableView.getColumns().add(colName);
		tableView.getColumns().add(colType);

		// TODO: Adjust pipeline order OR force a single compilation on opening the window
		//       so that the variable information is instantly available.
		//        - Then also change the placeholder text when this change is made in the lang files
		Label empty = new Label();
		empty.textProperty().bind(Lang.getBinding("assembler.vartable.empty"));
		tableView.setPlaceholder(empty);

		setCenter(tableView);
		setDisable(true);

		pipeline.addPipelineCompletionListener(method -> populateVariables());
	}

	private void populateVariables() {
		Variables variables = pipeline.getLastVariables();
		if (variables != null) {
			tableView.setItems(FXCollections.observableArrayList(variables.inSortedOrder()));
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
}
