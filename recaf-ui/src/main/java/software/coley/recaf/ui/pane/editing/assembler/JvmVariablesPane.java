package software.coley.recaf.ui.pane.editing.assembler;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import dev.xdark.blw.type.ClassType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import org.fxmisc.richtext.CodeArea;
import org.reactfx.Change;
import org.reactfx.EventStreams;
import org.slf4j.Logger;
import software.coley.collections.Lists;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.linegraphics.AbstractLineGraphicFactory;
import software.coley.recaf.ui.control.richtext.linegraphics.LineContainer;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.SVG;
import software.coley.recaf.workspace.model.Workspace;

import java.awt.RenderingHints;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Component panel for the assembler which shows the variables of the currently selected method.
 *
 * @author Matt Coley
 */
@Dependent
public class JvmVariablesPane extends AstBuildConsumerComponent {
	private static final Logger logger = Logging.get(JvmVariablesPane.class);
	private final SimpleObjectProperty<Object> notifyQueue = new SimpleObjectProperty<>(new Object());
	private final TableView<VariableData> table = new TableView<>();
	private final Consumer<Change<Integer>> onCaretMove = this::onCaretMove;
	private final VarHighlightLineFactory varHighlighter = new VarHighlightLineFactory();

	@Inject
	@SuppressWarnings("unchecked")
	public JvmVariablesPane(@Nonnull CellConfigurationService cellConfigurationService,
	                        @Nonnull TextFormatConfig formatConfig,
	                        @Nonnull Workspace workspace) {
		TableColumn<VariableData, String> columnName = new TableColumn<>(Lang.get("assembler.variables.name"));
		TableColumn<VariableData, ClassType> columnType = new TableColumn<>(Lang.get("assembler.variables.type"));
		TableColumn<VariableData, AstUsages> columnUsage = new TableColumn<>(Lang.get("assembler.variables.usage"));
		columnName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().name()));
		columnType.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().type()));
		columnUsage.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().usage()));
		columnType.setCellFactory(param -> new TypeTableCell<>(cellConfigurationService, formatConfig, workspace));
		columnUsage.setCellFactory(param -> new TableCell<>() {
			@Override
			protected void updateItem(AstUsages usages, boolean empty) {
				super.updateItem(usages, empty);
				if (empty || usages == null) {
					setText(null);
					setGraphic(null);
					setOnMousePressed(null);
				} else {
					String usageFmt = "%d reads, %d writes".formatted(usages.readers().size(), usages.writers().size());
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
				if (nextEntry == null) return;
				Range value = nextEntry.getValue();
				area.selectRange(value.start(), value.end());
				area.showParagraphAtCenter(area.getCurrentParagraph());
			}
		});

		setCenter(table);

		EventStreams.changesOf(notifyQueue)
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(Editor.SHORT_DELAY_MS))
				.addObserver(unused -> {
					try {
						updateTable();
					} catch (Throwable t) {
						logger.error("Error updating variables table", t);
					}
				});
	}

	@Override
	public void install(@Nonnull Editor editor) {
		super.install(editor);

		editor.getRootLineGraphicFactory().addLineGraphicFactory(varHighlighter);
		editor.getCaretPosEventStream().addObserver(onCaretMove);
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		super.uninstall(editor);

		editor.getRootLineGraphicFactory().removeLineGraphicFactory(varHighlighter);
		editor.getCaretPosEventStream().removeObserver(onCaretMove);
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

	/**
	 * Handles updating the {@link VarHighlightLineFactory}.
	 * <p/>
	 * This logic is shoe-horned into here <i>(for now)</i> because
	 * the variable tracking logic is internal to this class only.
	 *
	 * @param caretChange
	 * 		Caret pos change.
	 */
	private void onCaretMove(Change<Integer> caretChange) {
		int pos = caretChange.getNewValue();

		// Determine which variable is at the caret position
		VariableData currentVarSelection = null;
		for (VariableData item : table.getItems()) {
			AstUsages usage = item.usage();
			ASTElement matchedAst = usage.readersAndWriters()
					.filter(e -> e.range().within(pos))
					.findFirst().orElse(null);
			if (matchedAst != null) {
				currentVarSelection = item;
				break;
			}
		}

		// Notify the highlighter of the difference
		varHighlighter.setSelectedVariable(currentVarSelection);
	}

	private void scheduleTableUpdate() {
		if (currentMethod == null || analysisLookup == null) return;
		FxThreadUtil.run(() -> notifyQueue.set(new Object()));
	}

	private void updateTable() {
		ObservableList<VariableData> items = table.getItems();
		items.clear();

		// Collect all variable usage information from the AST.
		AstUsages emptyUsage = AstUsages.EMPTY_USAGE;
		Map<String, AstUsages> variableUsages = new HashMap<>();
		BiConsumer<String, ASTElement> readUpdater = (name, element) -> {
			AstUsages existing = variableUsages.getOrDefault(name, emptyUsage);
			variableUsages.put(name, existing.withNewRead(element));
		};
		BiConsumer<String, ASTElement> writeUpdater = (name, element) -> {
			AstUsages existing = variableUsages.getOrDefault(name, emptyUsage);
			variableUsages.put(name, existing.withNewWrite(element));
		};
		if (astElements != null) {
			Consumer<ASTMethod> methodConsumer = astMethod -> {
				if (currentMethod != null && !Objects.equals(currentMethod.getName(), astMethod.getName().literal()))
					return;
				for (ASTIdentifier parameter : astMethod.parameters()) {
					String literalName = parameter.literal();
					variableUsages.putIfAbsent(literalName, emptyUsage);
				}
				ASTCode code = astMethod.code();
				if (code == null)
					return;
				for (ASTInstruction instruction : code.instructions()) {
					String insnName = instruction.identifier().content();
					boolean isLoad = insnName.endsWith("load");
					if (((isLoad || insnName.endsWith("store")) && insnName.charAt(1) != 'a') || insnName.equals("iinc")) {
						List<ASTElement> arguments = instruction.arguments();
						if (!arguments.isEmpty()) {
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
			};
			for (ASTElement astElement : astElements) {
				if (astElement instanceof ASTMethod astMethod) {
					methodConsumer.accept(astMethod);
				} else if (astElement instanceof ASTClass astClass) {
					for (ASTElement child : astClass.children()) {
						if (child instanceof ASTMethod astMethod) {
							methodConsumer.accept(astMethod);
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
		FxThreadUtil.run(() -> table.getItems().clear());
		currentMethod = null;
	}

	/**
	 * Highlighter which shows read and write access of a {@link VariableData}.
	 */
	private class VarHighlightLineFactory extends AbstractLineGraphicFactory {
		private static final Map<RenderingHints.Key, Object> REF_RENDER_HINTS =
				Map.of(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
		private static final int ICON_SIZE = 16;
		private VariableData variable;

		private VarHighlightLineFactory() {
			super(AbstractLineGraphicFactory.P_LINE_NUMBERS + 1);
		}

		/**
		 * @param variable
		 * 		New selected variable.
		 */
		public void setSelectedVariable(@Nullable VariableData variable) {
			VariableData existing = this.variable;
			if (existing == null) {
				if (variable == null)
					return;
			} else {
				if (existing.matchesNameType(variable))
					return;
			}

			this.variable = variable;
			editor.redrawParagraphGraphics();
		}

		@Override
		public void install(@Nonnull Editor editor) {
			// no-op, outer class has all the data we need
		}

		@Override
		public void uninstall(@Nonnull Editor editor) {
			// no-op
		}

		@Override
		public void apply(@Nonnull LineContainer container, int paragraph) {
			Node graphic;
			createGraphic:
			{
				if (variable != null) {
					for (ASTElement reader : variable.usage().readers()) {
						Location location = reader.location();
						if (location != null && location.line() - 1 == paragraph) {
							graphic = SVG.ofFile(SVG.REF_READ, ICON_SIZE, REF_RENDER_HINTS);
							graphic.prefWidth(ICON_SIZE);
							break createGraphic;
						}
					}
					for (ASTElement writer : variable.usage().writers()) {
						Location location = writer.location();
						if (location != null && location.line() - 1 == paragraph) {
							graphic = SVG.ofFile(SVG.REF_WRITE, ICON_SIZE,
									REF_RENDER_HINTS);
							graphic.prefWidth(ICON_SIZE);
							break createGraphic;
						}
					}
				}
				graphic = new Spacer(ICON_SIZE);
			}
			graphic.setCursor(Cursor.HAND);
			container.addHorizontal(graphic);
		}
	}
}