package software.coley.recaf.ui.pane.editing.assembler;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.query.AssemblyQueries;
import me.darknet.assembler.query.VariableAccessKind;
import me.darknet.assembler.query.VariableQueryResult;
import me.darknet.assembler.query.VariableUsage;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.objectweb.asm.Type;
import org.reactfx.Change;
import org.reactfx.EventStreams;
import org.slf4j.Logger;
import software.coley.collections.Lists;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.IconView;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.linegraphics.AbstractLineGraphicFactory;
import software.coley.recaf.ui.control.richtext.linegraphics.LineContainer;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.SVG;
import software.coley.recaf.workspace.model.Workspace;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Component panel for the assembler which shows the variables of the currently selected method.
 *
 * @author Matt Coley
 */
@Dependent
public class JvmVariablesPane extends AstBuildConsumerComponent {
	private static final Type INVALID_VAR_TYPE_MARKER = Type.VOID_TYPE;
	private static final Logger logger = Logging.get(JvmVariablesPane.class);
	private final SimpleObjectProperty<Object> notifyQueue = new SimpleObjectProperty<>(new Object());
	private final TableView<VariableData> table = new TableView<>();
	private final Consumer<Change<Integer>> onCaretMove = this::onCaretMove;
	private final VarHighlightLineFactory varHighlighter = new VarHighlightLineFactory();

	@Inject
	@SuppressWarnings("unchecked")
	public JvmVariablesPane(@Nonnull CellConfigurationService cellConfigurationService,
	                        @Nonnull TextFormatConfig formatConfig,
	                        @Nonnull WorkspaceManager workspaceManager) {
		Workspace workspace = workspaceManager.getCurrent();

		TableColumn<VariableData, String> columnName = new TableColumn<>(Lang.get("assembler.variables.name"));
		TableColumn<VariableData, Type> columnType = new TableColumn<>(Lang.get("assembler.variables.type"));
		TableColumn<VariableData, VariableData> columnUsage = new TableColumn<>(Lang.get("assembler.variables.usage"));
		columnName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().name()));
		columnType.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().type()));
		columnUsage.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
		columnType.setCellFactory(param -> new TypeTableCell<>(cellConfigurationService, formatConfig, workspace));
		columnUsage.setCellFactory(param -> new TableCell<>() {
			{
				setContentDisplay(ContentDisplay.RIGHT);
			}

			@Override
			protected void updateItem(VariableData variable, boolean empty) {
				super.updateItem(variable, empty);
				if (empty || variable == null) {
					setText(null);
					setGraphic(null);
					setOnMousePressed(null);
				} else {
					String usageFmt = "%d reads, %d writes".formatted(variable.readers().size(), variable.writers().size());
					setText(usageFmt);

					// Put a warning symbol on variables that are read from before ever being written to.
					//  - Parameters are implicitly treated as being written to.
					if (variable.readBeforeWrite()) {
						BoundLabel warning = new BoundLabel(Lang.getBinding("assembler.variables.read-before-write"),
								new FontIconView(CarbonIcons.WARNING, Color.YELLOW));
						warning.getStyleClass().add(Styles.WARNING);
						setGraphic(warning);
					} else {
						setGraphic(null);
					}
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
				selectedItem.references().forEach(reference -> {
					Range range = reference.range();
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
	 * <p>
	 * This logic is shoe-horned into here <i>(for now)</i> because
	 * the variable tracking logic is internal to this class only.
	 *
	 * @param caretChange
	 * 		Caret pos change.
	 */
	private void onCaretMove(Change<Integer> caretChange) {
		// Skip if the user is selecting text.
		if (editor.getCodeArea().getSelection().getLength() > 0) {
			varHighlighter.setSelectedVariable(null);
			return;
		}

		// Determine which variable is at the caret position
		int pos = caretChange.getNewValue();
		VariableData currentVarSelection = null;
		for (VariableData item : table.getItems()) {
			ASTElement matchedAst = item.references()
					.filter(element -> element.range().within(pos))
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

		// Skip if we cannot resolve the current method in the AST.
		ASTMethod astMethod = findCurrentAstMethod();
		if (astMethod == null)
			return;

		VariableQueryResult queryResult = AssemblyQueries.variables(astMethod);
		AnalysisResults analysisResults = analysisLookup.results(currentMethod.getName(), currentMethod.getDescriptor());
		if (analysisResults != null && !analysisResults.frames().isEmpty()) {
			// Linked map for ordering
			Map<String, VariableData> variables = new LinkedHashMap<>();

			// Gather variables from frames.
			analysisResults.frames().values().stream()
					.flatMap(Frame::locals)
					.distinct()
					.forEach(local -> variables.put(local.name(), new VariableData(local.name(), local.safeType(), queryResult)));

			// In some cases, the last frame may not have some entries.
			// This generally means there is either a problem with the code or with JASM.
			// Either way, reporting them here with a bogus type is good for diagnosing the issue.
			queryResult.declarations().forEach(declaration ->
					variables.putIfAbsent(declaration.identity().name(),
							new VariableData(declaration.identity().name(), INVALID_VAR_TYPE_MARKER, queryResult)));
			queryResult.usages().forEach(usage ->
					variables.putIfAbsent(usage.name(),
							new VariableData(usage.name(), INVALID_VAR_TYPE_MARKER, queryResult)));

			// Add all found items to the table.
			items.addAll(variables.values());
		}
	}

	@Nullable
	private ASTMethod findCurrentAstMethod() {
		if (currentMethod == null)
			return null;

		String methodName = currentMethod.getName();
		String methodDescriptor = currentMethod.getDescriptor();
		for (ASTElement astElement : astElements) {
			if (astElement instanceof ASTMethod astMethod) {
				if (matchesMethod(astMethod, methodName, methodDescriptor))
					return astMethod;
			} else if (astElement instanceof ASTClass astClass) {
				for (ASTElement child : astClass.children()) {
					if (child instanceof ASTMethod astMethod && matchesMethod(astMethod, methodName, methodDescriptor))
						return astMethod;
				}
			}
		}
		return null;
	}

	private static boolean matchesMethod(@Nonnull ASTMethod astMethod, @Nonnull String methodName, @Nonnull String methodDescriptor) {
		return Objects.equals(methodName, astMethod.getName().literal()) &&
				Objects.equals(methodDescriptor, astMethod.getDescriptor().literal());
	}

	private void clearData() {
		FxThreadUtil.run(() -> table.getItems().clear());
		currentMethod = null;
	}

	/**
	 * Highlighter which shows read and write access of a {@link VariableData}.
	 */
	private class VarHighlightLineFactory extends AbstractLineGraphicFactory {
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
			} else if (existing.matchesNameType(variable)) {
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
					for (ASTElement reader : variable.readers()) {
						Location location = reader.location();
						if (location != null && location.line() - 1 == paragraph) {
							graphic = SVG.ofIconFile(SVG.REF_READ);
							break createGraphic;
						}
					}
					for (ASTElement writer : variable.writers()) {
						Location location = writer.location();
						if (location != null && location.line() - 1 == paragraph) {
							graphic = SVG.ofIconFile(SVG.REF_WRITE);
							break createGraphic;
						}
					}
				}
				graphic = new Spacer(IconView.DEFAULT_ICON_SIZE);
			}
			graphic.setCursor(Cursor.HAND);
			container.addHorizontal(graphic);
		}
	}

	/**
	 * Wrapper around {@link VariableQueryResult} data for a single variable.
	 * Provides utility methods for accessing the variable's usages and declarations.
	 *
	 * @param name
	 * 		Variable name.
	 * @param type
	 * 		Variable type.
	 * @param queryResult
	 * 		Query result containing all variable usages and declarations for the current method.
	 */
	private record VariableData(@Nonnull String name, @Nonnull Type type, @Nonnull VariableQueryResult queryResult) {
		public boolean matchesNameType(@Nullable VariableData other) {
			return other != null && name.equals(other.name) && type.equals(other.type);
		}

		@Nonnull
		public List<ASTElement> readers() {
			return queryResult.usagesOf(name).stream()
					.filter(usage -> usage.kind() == VariableAccessKind.READ)
					.map(VariableUsage::instruction)
					.map(ASTElement.class::cast)
					.toList();
		}

		@Nonnull
		public List<ASTElement> writers() {
			return queryResult.usagesOf(name).stream()
					.filter(usage -> usage.kind() != VariableAccessKind.READ)
					.map(VariableUsage::instruction)
					.map(ASTElement.class::cast)
					.toList();
		}

		@Nonnull
		public Stream<ASTElement> references() {
			return queryResult.usagesOf(name).stream()
					.map(VariableUsage::instruction)
					.map(ASTElement.class::cast);
		}

		public boolean isParameter() {
			return queryResult.declarationsOf(name).stream().anyMatch(declaration -> declaration.parameter());
		}

		public boolean readBeforeWrite() {
			List<ASTElement> elements = references()
					.sorted(Comparator.comparing(ASTElement::location))
					.toList();
			return !elements.isEmpty() && !isParameter() && readers().contains(elements.getFirst());
		}
	}
}
