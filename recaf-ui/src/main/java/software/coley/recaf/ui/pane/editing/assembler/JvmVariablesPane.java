package software.coley.recaf.ui.pane.editing.assembler;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import dev.xdark.blw.type.ArrayType;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.PrimitiveType;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.Frame;
import me.darknet.assembler.compile.analysis.LocalInfo;
import me.darknet.assembler.compile.analysis.MethodAnalysisLookup;
import me.darknet.assembler.compiler.ClassRepresentation;
import org.reactfx.EventStreams;
import software.coley.collections.Lists;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.ContextSource;
import software.coley.recaf.ui.config.TextFormatConfig;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.Icons;
import software.coley.recaf.workspace.model.Workspace;

import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Component panel for the assembler which shows the variables of the currently selected method.
 *
 * @author Matt Coley
 */
@Dependent
public class JvmVariablesPane extends ContextualAssemblerComponent implements AssemblerAstConsumer, AssemblerBuildConsumer {
	private final SimpleObjectProperty<Object> observable = new SimpleObjectProperty<>(new Object());
	private final TableView<VariableData> table = new TableView<>();
	private List<ASTElement> astElements = Collections.emptyList();
	private MethodAnalysisLookup analysisLookup;

	private MethodMember selectedMethod;

	@Inject
	public JvmVariablesPane(@Nonnull CellConfigurationService cellConfigurationService,
							@Nonnull TextFormatConfig formatConfig,
							@Nonnull Workspace workspace) {
		TableColumn<VariableData, String> columnName = new TableColumn<>("Name");
		TableColumn<VariableData, ClassType> columnType = new TableColumn<>("Type");
		TableColumn<VariableData, VariableUsages> columnUsage = new TableColumn<>("Usage");
		columnName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().name));
		columnType.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().type));
		columnUsage.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().usage));
		columnType.setCellFactory(param -> new TableCell<>() {
			@Override
			protected void updateItem(ClassType type, boolean empty) {
				super.updateItem(type, empty);
				if (empty || type == null) {
					setText(null);
					setGraphic(null);
				} else {
					configureType(type);
				}
			}

			private void configureType(ClassType type) {
				if (type instanceof PrimitiveType primitiveType) {
					setGraphic(Icons.getIconView(Icons.PRIMITIVE));
					setText(primitiveType.name());
				} else if (type instanceof InstanceType instanceType) {
					String typeName = instanceType.internalName();
					ClassPathNode classPath = workspace.findClass(typeName);
					if (classPath != null) {
						// TODO: The 'reference' ctx src still shows the refactor menu, which we'll need to ensure
						//  updates the disassembled code
						cellConfigurationService.configure(this, classPath, ContextSource.REFERENCE);
					} else {
						setGraphic(Icons.getIconView(Icons.CLASS));
						setText(formatConfig.filter(typeName));
					}
				} else if (type instanceof ArrayType arrayType) {
					ClassType componentType = arrayType.componentType();
					configureType(componentType);
				}
			}
		});
		columnUsage.setCellFactory(param -> new TableCell<>() {
			@Override
			protected void updateItem(VariableUsages usages, boolean empty) {
				super.updateItem(usages, empty);
				if (empty || usages == null) {
					setText(null);
					setGraphic(null);
				} else {
					String usageS = "%d reads, %d writes".formatted(usages.readers.size(), usages.writers.size());
					setText(usageS);
				}
			}
		});
		table.getColumns().addAll(columnName, columnType, columnUsage);
		table.getStyleClass().addAll(Styles.STRIPED, Tweaks.EDGE_TO_EDGE, "variable-table");
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

		setCenter(table);

		EventStreams.changesOf(observable)
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(Editor.SHORT_DELAY_MS))
				.addObserver(unused -> {
					updateTable();
				});
	}

	private void clearData() {
		table.getItems().clear();
		selectedMethod = null;
	}

	@Override
	protected void onSelectClass(@Nonnull ClassInfo declared) {
		clearData();
	}

	@Override
	protected void onSelectMethod(@Nonnull ClassInfo declaring, @Nonnull MethodMember method) {
		selectedMethod = method;
		scheduleTableUpdate();
	}

	@Override
	protected void onSelectField(@Nonnull ClassInfo declaring, @Nonnull FieldMember field) {
		clearData();
	}

	@Override
	public void consumeAst(@Nonnull List<ASTElement> astElements, @Nonnull AstPhase phase) {
		this.astElements = Collections.unmodifiableList(astElements);
		scheduleTableUpdate();
	}

	@Override
	public void consumeClass(@Nonnull ClassRepresentation classRepresentation, @Nonnull ClassInfo classInfo) {
		if (classRepresentation instanceof JavaClassRepresentation javaClassRep) {
			analysisLookup = javaClassRep.analysisLookup();
			scheduleTableUpdate();
		}
	}

	private void scheduleTableUpdate() {
		if (selectedMethod == null || analysisLookup == null) return;
		observable.set(new Object());
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
		AnalysisResults analysisResults = analysisLookup.results(selectedMethod.getName(), selectedMethod.getDescriptor());
		if (analysisResults != null && !analysisResults.frames().isEmpty()) {
			// Linked map for ordering
			Map<String, VariableData> variables = new LinkedHashMap<>();

			// In JASM variables are un-scoped, so the last frame will have all entries.
			Frame endFrame = analysisResults.getLastFrame();
			for (LocalInfo local : endFrame.getLocals().values()) {
				String localName = local.name();
				variables.put(localName, VariableData.adaptFrom(local, variableUsages.getOrDefault(localName, emptyUsage)));
			}

			// Add all found items to the table.
			items.addAll(variables.values());
		}
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
		public static VariableData adaptFrom(@Nonnull LocalInfo local, @Nonnull VariableUsages usage) {
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