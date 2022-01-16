package me.coley.recaf.ui.pane.assembler;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.assemble.MethodCompileException;
import me.coley.recaf.assemble.analysis.InheritanceChecker;
import me.coley.recaf.assemble.analysis.ReflectiveInheritanceChecker;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.compiler.ClassSupplier;
import me.coley.recaf.assemble.transformer.ExpressionToAsmTransformer;
import me.coley.recaf.assemble.transformer.ExpressionToAstTransformer;
import me.coley.recaf.assemble.transformer.VariableInfo;
import me.coley.recaf.assemble.transformer.Variables;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.ui.behavior.MemberEditor;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.bytecode.AssemblerArea;
import me.coley.recaf.ui.control.code.bytecode.AssemblerAstListener;
import me.coley.recaf.util.WorkspaceClassSupplier;
import me.coley.recaf.util.WorkspaceInheritanceChecker;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

/**
 * A subcomponent of {@link AssemblerPane} that shows all variables defined in the current method.
 *
 * @author Matt Coley
 */
public class VariableTable extends BorderPane implements MemberEditor, AssemblerAstListener {
	private static final Logger logger = Logging.get(VariableTable.class);
	private final TableView<VariableInfo> tableView = new TableView<>();
	private final AssemblerArea assemblerArea;
	private CommonClassInfo declaringClass;

	/**
	 * @param assemblerArea
	 * 		Associated assembler area to interact with.
	 */
	public VariableTable(AssemblerArea assemblerArea) {
		this.assemblerArea = assemblerArea;

		// TODO: Setup table
		//  - click to go to next usage of variable

		InheritanceChecker checker = WorkspaceInheritanceChecker.getInstance();

		TableColumn<VariableInfo, Integer> colIndex = new TableColumn<>("Index");
		TableColumn<VariableInfo, String> colName = new TableColumn<>("Name");
		TableColumn<VariableInfo, String> colType = new TableColumn<>("Type");
		colIndex.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getIndex()));
		colName.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getName()));
		colType.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCommonType(checker).getInternalName()));

		tableView.getColumns().add(colIndex);
		tableView.getColumns().add(colName);
		tableView.getColumns().add(colType);

		setCenter(tableView);
		setDisable(true);
	}

	@Override
	public void onAstBuildPass(Unit unit) {
		// Method only
		setDisable(unit.isField());
		if (isDisabled())
			return;
		ClassSupplier supplier = WorkspaceClassSupplier.getInstance();
		InheritanceChecker checker = WorkspaceInheritanceChecker.getInstance();
		MethodDefinition definition = (MethodDefinition) unit.getDefinition();
		String selfType = declaringClass.getName();
		try {
			// Setup variables
			Variables variables = new Variables();
			variables.visitDefinition(declaringClass.getName(), definition);
			variables.visitParams(definition);
			variables.visitCodeFirstPass(unit.getCode());
			// Compute enhanced variable information
			ExpressionToAsmTransformer exprToAsm = new ExpressionToAsmTransformer(supplier, definition, variables, selfType);
			ExpressionToAstTransformer exprToAst = new ExpressionToAstTransformer(definition, variables, exprToAsm);
			variables.visitCodeSecondPass(declaringClass.getName(), unit, checker, exprToAst);
			// Repopulate table model
			tableView.setItems(FXCollections.observableArrayList(variables.inSortedOrder()));
		} catch (MethodCompileException ex) {
			logger.error("Failed to populate variable info from unit", ex);
		}
	}

	@Override
	public void onAstBuildFail(Unit unit, ProblemTracking problemTracking) {
		// no-op
	}

	@Override
	public void onAstBuildCrash(Unit unit, Throwable reason) {
		// no-op
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		this.declaringClass = newValue;
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return declaringClass;
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
