package software.coley.recaf.ui.pane.editing.assembler;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
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
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikRegisterUsage;
import me.darknet.assembler.util.Range;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.Lang;

import java.util.List;

/**
 * Component panel showing register reads and writes in the selected Dalvik method.
 *
 * @author Matt Coley
 */
@Dependent
public class DalvikRegistersPane extends DalvikAstBuildConsumerComponent {
	private final TableView<DalvikRegisterUsage> table = new TableView<>();
	private final Label emptyMessage = new Label(Lang.get("assembler.registers.empty"));

	@Inject
	public DalvikRegistersPane() {
		TableColumn<DalvikRegisterUsage, String> registerColumn =
				new TableColumn<>(Lang.get("assembler.registers.register"));
		TableColumn<DalvikRegisterUsage, Integer> readsColumn =
				new TableColumn<>(Lang.get("assembler.registers.reads"));
		TableColumn<DalvikRegisterUsage, Integer> writesColumn =
				new TableColumn<>(Lang.get("assembler.registers.writes"));
		registerColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().registerName()));
		readsColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().readCount()));
		writesColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().writeCount()));
		table.getColumns().addAll(List.of(registerColumn, readsColumn, writesColumn));
		table.getStyleClass().addAll(Styles.STRIPED, Tweaks.EDGE_TO_EDGE, "variable-table");
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		table.setOnMousePressed(event -> {
			if (event.isPrimaryButtonDown())
				navigateToNextReference(table.getSelectionModel().getSelectedItem());
		});

		emptyMessage.setPadding(new Insets(8));
		emptyMessage.setVisible(false);
		emptyMessage.setManaged(false);
		VBox content = new VBox(emptyMessage, table);
		VBox.setVgrow(table, Priority.ALWAYS);
		setCenter(content);
	}

	/**
	 * Displays register usage summaries from the current Dalvik compile result.
	 */
	@Override
	protected void updatePane() {
		table.getItems().clear();
		setEmptyMessageVisible(false);

		// Skip if component not installed, or if no method is selected.
		if (editor == null || currentMethod == null)
			return;

		// Skip if there's no register usage data to show.
		List<DalvikRegisterUsage> usages = getCurrentRegisterUsages();
		if (usages == null)
			return;

		table.getItems().setAll(usages);
		setEmptyMessageVisible(usages.isEmpty());
	}

	/**
	 * Moves the editor selection to the next source reference for a register.
	 *
	 * @param usage
	 * 		Selected register usage, or {@code null} when no row is selected.
	 */
	private void navigateToNextReference(@Nullable DalvikRegisterUsage usage) {
		// Skip if component not installed.
		Editor currentEditor = editor;
		if (usage == null || currentEditor == null)
			return;

		// And skip if no references exist for the register.
		List<ASTInstruction> references = usage.references();
		if (references.isEmpty())
			return;

		// Find the next reference after the current caret position, or wrap around to the first reference.
		int caret = currentEditor.getCodeArea().getCaretPosition();
		ASTInstruction next = references.getFirst();
		for (ASTInstruction reference : references) {
			if (reference.range().start() > caret) {
				next = reference;
				break;
			}
		}

		// Select the next reference in the editor and scroll to it.
		Range range = next.range();
		currentEditor.getCodeArea().selectRange(range.start(), range.end());
		currentEditor.getCodeArea().showParagraphAtCenter(currentEditor.getCodeArea().getCurrentParagraph());
	}

	private void setEmptyMessageVisible(boolean visible) {
		emptyMessage.setVisible(visible);
		emptyMessage.setManaged(visible);
	}
}
