package software.coley.recaf.ui.pane.editing.assembler;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import dev.xdark.blw.type.ClassType;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.util.Range;
import org.fxmisc.richtext.CodeArea;
import org.reactfx.EventStreams;
import software.coley.collections.Lists;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.ui.config.TextFormatConfig;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;

import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Component panel for the assembler which shows the variables of the currently selected method.
 *
 * @author Matt Coley
 */
@Dependent
public class JvmVariablesPane extends AstBuildConsumerComponent {
	private final SimpleObjectProperty<Object> notifyQueue = new SimpleObjectProperty<>(new Object());
	private final TableView<VariableData> table = new TableView<>();

	@Inject
	public JvmVariablesPane(@Nonnull CellConfigurationService cellConfigurationService,
							@Nonnull TextFormatConfig formatConfig,
							@Nonnull Workspace workspace) {
		TableColumn<VariableData, String> columnName = new TableColumn<>(Lang.get("assembler.variables.name"));
		TableColumn<VariableData, ClassType> columnType = new TableColumn<>(Lang.get("assembler.variables.type"));
		TableColumn<VariableData, VariableUsages> columnUsage = new TableColumn<>(Lang.get("assembler.variables.usage"));
		columnName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().name));
		columnType.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().type));
		columnUsage.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().usage));
		columnType.setCellFactory(param -> new TypeTableCell<>(cellConfigurationService, formatConfig, workspace));
		columnUsage.setCellFactory(param -> new TableCell<>() {
			@Override
			protected void updateItem(VariableUsages usages, boolean empty) {
				super.updateItem(usages, empty);
				if (empty || usages == null) {
					setText(null);
					setGraphic(null);
					setOnMousePressed(null);
				} else {
					String usageFmt = "%d reads, %d writes".formatted(usages.readers.size(), usages.writers.size());
					setText(usageFmt);
				}
			}
		});
		table.getColumns().addAll(columnName, columnType, columnUsage);
		table.getStyleClass().addAll(Styles.STRIPED, Tweaks.EDGE_TO_EDGE, "variable-table");
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		table.setOnMousePressed(e -> {
			if (e.isPrimaryButtonDown()) {
				VariableData selectedItem = table.getSelectionModel().getSelectedItem();
				if (selectedItem == null) return;

				// Collect ranges AST items where the variable is used.
				NavigableMap<Integer, Range> elementRanges = new TreeMap<>();
				selectedItem.usage().readersAndWriters().forEach(rw -> {
					Range range = rw.range();
					if (range != null)
						elementRanges.put(range.start(), range);
				});

				// Select next. Wrap around if nothing is next.
				CodeArea area = editor.getCodeArea();
				int caret = area.getCaretPosition();
				var nextEntry = elementRanges.higherEntry(caret + 1);
				if (nextEntry == null) nextEntry = elementRanges.firstEntry();
				Range value = nextEntry.getValue();
				area.selectRange(value.start(), value.end());
				area.showParagraphAtCenter(area.getCurrentParagraph());
			}
		});

		setCenter(table);

		EventStreams.changesOf(notifyQueue)
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(Editor.SHORT_DELAY_MS))
				.addObserver(unused -> updateTable());
	}

	@Override
	protected void onClassSelected() {
		clearData();
	}

	@Override
	protected void onMethodSelected() {
		scheduleTableUpdate();
	}

	@Override
	protected void onFieldSelected() {
		clearData();
	}

	@Override
	protected void onPipelineOutputUpdate() {
		scheduleTableUpdate();
	}

	private void scheduleTableUpdate() {
		if (currentMethod == null || analysisLookup == null) return;
		notifyQueue.set(new Object());
	}

	private void updateTable() {
		ObservableList<VariableData> items = table.getItems();
		items.clear();

		// Collect all variable usage information from the AST.
		VariableUsages emptyUsage = VariableUsages.EMPTY_USAGE;
		Map<String, VariableUsages> variableUsages = new HashMap<>();
		BiConsumer<String, ASTElement> readUpdater = (name, element) -> {
			VariableUsages existing = variableUsages.getOrDefault(name, emptyUsage);
			variableUsages.put(name, existing.withNewRead(element));
		};
		BiConsumer<String, ASTElement> writeUpdater = (name, element) -> {
			VariableUsages existing = variableUsages.getOrDefault(name, emptyUsage);
			variableUsages.put(name, existing.withNewWrite(element));
		};
		if (astElements != null) {
			for (ASTElement astElement : astElements) {
				if (astElement instanceof ASTMethod astMethod) {
					for (ASTIdentifier parameter : astMethod.parameters()) {
						String literalName = parameter.literal();
						variableUsages.putIfAbsent(literalName, emptyUsage);
					}
					for (ASTInstruction instruction : astMethod.code().instructions()) {
						String insnName = instruction.identifier().content();
						boolean isLoad = insnName.endsWith("load");
						if (((isLoad || insnName.endsWith("store")) && insnName.charAt(1) != 'a') || insnName.equals("iinc")) {
							List<ASTElement> arguments = instruction.arguments();
							if (arguments.size() > 0) {
								ASTElement arg = arguments.get(0);
								String varName = arg instanceof ASTIdentifier identifierArg ?
										identifierArg.literal() : arg.content();
								if (isLoad) {
									readUpdater.accept(varName, instruction);
								} else {
									writeUpdater.accept(varName, instruction);
								}
							}
						}
					}
				}
			}
		}

		// Populate the variables map from the stack analysis results.
		AnalysisResults analysisResults = analysisLookup.results(currentMethod.getName(), currentMethod.getDescriptor());
		if (analysisResults != null && !analysisResults.frames().isEmpty()) {
			// Linked map for ordering
			Map<String, VariableData> variables = new LinkedHashMap<>();

			// In JASM variables are un-scoped, so the last frame will have all entries.
			analysisResults.frames().values().stream()
					.flatMap(Frame::locals)
					.distinct()
					.forEach(local -> {
						String localName = local.name();
						variables.put(localName, VariableData.adaptFrom(local,
								variableUsages.getOrDefault(localName, emptyUsage)));
					});

			// Add all found items to the table.
			items.addAll(variables.values());
		}
	}

	private void clearData() {
		table.getItems().clear();
		currentMethod = null;
	}

	/**
	 * Models a variable.
	 *
	 * @param name
	 * 		Name of variable.
	 * @param type
	 * 		Type of variable.
	 * @param usage
	 * 		Usages of the variable in the AST.
	 */
	public record VariableData(@Nonnull String name, @Nonnull ClassType type, @Nonnull VariableUsages usage) {
		/**
		 * @param local
		 * 		blw variable declaration.
		 * @param usage
		 * 		AST usage.
		 *
		 * @return Data from a blw variable, plus AST usage.
		 */
		@Nonnull
		public static VariableData adaptFrom(@Nonnull Local local, @Nonnull VariableUsages usage) {
			return new VariableData(local.name(), local.type(), usage);
		}
	}

	/**
	 * Models variable usage.
	 *
	 * @param readers
	 * 		Elements that read from the variable.
	 * @param writers
	 * 		Elements that write to the variable.
	 */
	public record VariableUsages(@Nonnull List<ASTElement> readers, @Nonnull List<ASTElement> writers) {
		/**
		 * Empty variable usage.
		 */
		private static final VariableUsages EMPTY_USAGE = new VariableUsages(Collections.emptyList(), Collections.emptyList());

		/**
		 * @return Stream of both readers and writers.
		 */
		@Nonnull
		public Stream<ASTElement> readersAndWriters() {
			return Stream.concat(readers.stream(), writers.stream());
		}

		/**
		 * @param element
		 * 		Element to add as a reader.
		 *
		 * @return Copy with added element.
		 */
		@Nonnull
		public VariableUsages withNewRead(@Nonnull ASTElement element) {
			List<ASTElement> newReaders = new ArrayList<>(readers);
			newReaders.add(element);
			return new VariableUsages(newReaders, writers);
		}

		/**
		 * @param element
		 * 		Element to add as a writer.
		 *
		 * @return Copy with added element.
		 */
		@Nonnull
		public VariableUsages withNewWrite(@Nonnull ASTElement element) {
			List<ASTElement> newWriters = new ArrayList<>(writers);
			newWriters.add(element);
			return new VariableUsages(readers, newWriters);
		}
	}
}