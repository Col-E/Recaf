package software.coley.recaf.ui.pane.editing.assembler;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikAnalysisFailure;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikAnalysisResults;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikRegisterState;
import me.darknet.assembler.descriptor.DescriptorType;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import org.objectweb.asm.Type;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.List;

/**
 * Component panel showing the Dalvik register state before the caret-selected instruction.
 *
 * @author Matt Coley
 */
@Dependent
public class DalvikAnalysisPane extends DalvikAstBuildConsumerComponent {
	private final TableView<AnalysisRow> table = new TableView<>();
	private final Label noStateMessage = new Label(Lang.get("assembler.analysis.no-state"));

	@Inject
	public DalvikAnalysisPane(@Nonnull CellConfigurationService cellConfigurationService,
	                          @Nonnull TextFormatConfig formatConfig,
	                          @Nonnull WorkspaceManager workspaceManager) {
		Workspace workspace = workspaceManager.getCurrent();

		TableColumn<AnalysisRow, String> registerColumn = new TableColumn<>(Lang.get("assembler.analysis.register"));
		TableColumn<AnalysisRow, Type> typeColumn = new TableColumn<>(Lang.get("assembler.analysis.type"));
		TableColumn<AnalysisRow, ValueTableCell.ValueWrapper> valueColumn = new TableColumn<>(Lang.get("assembler.analysis.value"));
		TableColumn<AnalysisRow, String> stateColumn = new TableColumn<>(Lang.get("assembler.analysis.state"));
		registerColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().registerName()));
		typeColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(toType(cell.getValue().value())));
		valueColumn.setCellValueFactory(cell -> {
			Value value = cell.getValue().value();
			return new SimpleObjectProperty<>(value == null ? null : new ValueTableCell.ValueWrapper(value, null));
		});
		stateColumn.setCellValueFactory(cell -> new SimpleStringProperty(stateOf(cell.getValue().slot())));
		typeColumn.setCellFactory(column -> new UnknownTypeTableCell(cellConfigurationService, formatConfig, workspace));
		valueColumn.setCellFactory(column -> new ValueTableCell<>());
		table.getColumns().addAll(List.of(registerColumn, typeColumn, valueColumn, stateColumn));
		table.getStyleClass().addAll(Styles.STRIPED, Tweaks.EDGE_TO_EDGE, "variable-table");
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

		noStateMessage.setPadding(new Insets(8));
		noStateMessage.setVisible(false);
		noStateMessage.setManaged(false);
		VBox content = new VBox(noStateMessage, table);
		VBox.setVgrow(table, Priority.ALWAYS);
		setCenter(content);
	}

	@Override
	protected void updatePane() {
		table.getItems().clear();
		setNoStateVisible(false);

		// Skip if the component isn't installed, or there is no method selected.
		ASTMethod astMethod = currentMethod == null ? null : findCurrentAstMethod();
		DalvikAnalysisResults results = currentMethod == null ? null : getCurrentAnalysisResults();
		if (editor == null || currentMethod == null)
			return;

		// Make a snapshot to work off of.
		ASTInstruction selectedSource = astMethod == null ? null : findSelectedAstInstruction(astMethod);
		Snapshot snapshot = create(results, selectedSource);

		// Populate table with register state.
		List<AnalysisRow> rows = new ArrayList<>(snapshot.registers().size() + 1);
		for (RegisterRow row : snapshot.registers())
			rows.add(new AnalysisRow("v" + row.registerIndex(), row.slot(), valueOf(row.slot())));
		if (snapshot.pendingResult() != null)
			rows.add(new AnalysisRow(Lang.get("assembler.analysis.pending-result"), null, snapshot.pendingResult()));
		table.getItems().setAll(rows);
		setNoStateVisible(!snapshot.hasState());
	}

	/**
	 * @param visible
	 * 		Whether the "no state" message should be visible.
	 */
	private void setNoStateVisible(boolean visible) {
		noStateMessage.setVisible(visible);
		noStateMessage.setManaged(visible);
	}

	/**
	 * @param slot
	 * 		Register slot state.
	 *
	 * @return Value held in the slot, if known.
	 */
	@Nullable
	private static Value valueOf(@Nullable DalvikRegisterState.Slot slot) {
		return slot instanceof DalvikRegisterState.ValueHead(Value value) ? value : null;
	}

	/**
	 * @param value
	 * 		Analysis value.
	 *
	 * @return Type of the value, or {@code null} when the type is unknown.
	 */
	@Nullable
	private static Type toType(@Nullable Value value) {
		if (value == null)
			return null;
		if (value instanceof Value.NullValue)
			return TypeTableCell.NULL_TYPE;
		DescriptorType descriptor = value.type();
		return descriptor == null ? null : Type.getType(descriptor.descriptor());
	}

	/**
	 * @param slot
	 * 		Register slot state.
	 *
	 * @return Display text for the register state, or {@code null} when no special state is present.
	 */
	@Nullable
	private static String stateOf(@Nullable DalvikRegisterState.Slot slot) {
		if (slot instanceof DalvikRegisterState.WideTail(int headRegister))
			return Lang.get("assembler.analysis.wide-tail") + " v" + headRegister;
		if (slot == DalvikRegisterState.Undefined.INSTANCE)
			return Lang.get("assembler.analysis.undefined");
		return null;
	}

	/**
	 * @param results
	 * 		Analysis results for the selected method.
	 * @param selectedSource
	 * 		Source instruction at the caret, or {@code null} when none is selected.
	 *
	 * @return Register state immediately before the selected instruction, when available.
	 */
	@Nonnull
	private static Snapshot create(@Nullable DalvikAnalysisResults results, @Nullable ASTInstruction selectedSource) {
		List<DalvikAnalysisFailure> failures = results == null ? List.of() : results.getFailures();
		if (results == null || selectedSource == null)
			return new Snapshot(false, List.of(), null, failures);

		List<Instruction> instructions = results.getCodeInstructions();
		for (int index = 0; index < instructions.size(); index++) {
			Instruction instruction = instructions.get(index);
			if (results.getSource(instruction) != selectedSource)
				continue;

			DalvikRegisterState state = results.getStateBefore(index);
			if (state == null)
				return new Snapshot(false, List.of(), null, failures);

			List<RegisterRow> registers = new ArrayList<>(state.registerCount());
			for (int registerIndex = 0; registerIndex < state.registerCount(); registerIndex++)
				registers.add(new RegisterRow(registerIndex, state.slot(registerIndex)));

			return new Snapshot(true, registers, state.pendingResult(), failures);
		}

		return new Snapshot(false, List.of(), null, failures);
	}

	/**
	 * Row in the register-state table.
	 *
	 * @param registerName
	 * 		Display name of the register.
	 * @param slot
	 * 		Register slot state, or {@code null} for the pending result.
	 * @param value
	 * 		Value held in the slot, if known.
	 */
	private record AnalysisRow(@Nonnull String registerName, @Nullable DalvikRegisterState.Slot slot,
	                           @Nullable Value value) {}

	/**
	 * Analysis data presented for the selected instruction.
	 *
	 * @param hasState
	 * 		Whether register state is available.
	 * @param registers
	 * 		Register slots at the selected instruction.
	 * @param pendingResult
	 * 		Pending method result, if present.
	 * @param failures
	 * 		Analysis failures for the method.
	 */
	record Snapshot(boolean hasState, @Nonnull List<RegisterRow> registers,
	                @Nullable Value pendingResult, @Nonnull List<DalvikAnalysisFailure> failures) {}

	/**
	 * One register and its analyzed slot.
	 *
	 * @param registerIndex
	 * 		Index of the register.
	 * @param slot
	 * 		Register slot state.
	 */
	record RegisterRow(int registerIndex, @Nonnull DalvikRegisterState.Slot slot) {}

	/**
	 * Type cell that distinguishes unknown analysis types from null values.
	 */
	private static final class UnknownTypeTableCell extends TypeTableCell<AnalysisRow> {
		private final String unknownText = Lang.get("assembler.analysis.unknown");

		private UnknownTypeTableCell(CellConfigurationService cellConfigurationService, TextFormatConfig formatConfig,
		                             Workspace workspace) {
			super(cellConfigurationService, formatConfig, workspace);
		}

		@Override
		protected void updateItem(Type type, boolean empty) {
			super.updateItem(type, empty);
			if (empty) {
				setText(null);
				setGraphic(null);
				setOnMouseClicked(null);
				setContextMenu(null);
			} else {
				AnalysisRow row = getTableRow() == null ? null : getTableRow().getItem();
				if (row != null && row.value() != null && !(row.value() instanceof Value.NullValue)) {
					setText(unknownText);
					setGraphic(null);
					setOnMouseClicked(null);
					setContextMenu(null);
					setOpacity(1);
				}
			}
		}
	}
}
