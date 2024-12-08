package software.coley.recaf.ui.pane;

import atlantafx.base.controls.Card;
import atlantafx.base.controls.ModalPane;
import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.collections.Lists;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.config.ConfigComponentFactory;
import software.coley.recaf.services.config.ConfigComponentManager;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.MappingApplier;
import software.coley.recaf.services.mapping.MappingApplierService;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.aggregate.AggregateMappingManager;
import software.coley.recaf.services.mapping.aggregate.AggregatedMappings;
import software.coley.recaf.services.mapping.format.EnigmaMappings;
import software.coley.recaf.services.mapping.gen.MappingGenerator;
import software.coley.recaf.services.mapping.gen.filter.*;
import software.coley.recaf.services.mapping.gen.naming.DeconflictingNameGenerator;
import software.coley.recaf.services.mapping.gen.naming.IncrementingNameGeneratorProvider;
import software.coley.recaf.services.mapping.gen.naming.NameGenerator;
import software.coley.recaf.services.mapping.gen.naming.NameGeneratorProvider;
import software.coley.recaf.services.mapping.gen.naming.NameGeneratorProviders;
import software.coley.recaf.services.search.match.StringPredicate;
import software.coley.recaf.services.search.match.StringPredicateProvider;
import software.coley.recaf.ui.LanguageStylesheets;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.ui.control.BoundCheckBox;
import software.coley.recaf.ui.control.BoundComboBox;
import software.coley.recaf.ui.control.BoundIntSpinner;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.BoundTextField;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.control.richtext.syntax.RegexLanguages;
import software.coley.recaf.ui.control.richtext.syntax.RegexSyntaxHighlighter;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.ToStringConverter;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static software.coley.recaf.util.Lang.getBinding;

/**
 * Pane for configurable mapping generation.
 *
 * @author Matt Coley
 */
@Dependent
public class MappingGeneratorPane extends StackPane {
	private static final Logger logger = Logging.get(MappingGenerator.class);
	private static final ToStringConverter<String> textPredicateConverter = ToStringConverter.from(MappingGeneratorPane::predicateIdToTranslation);
	private static final NameGeneratorProvider<?> fallbackProvider = new IncrementingNameGeneratorProvider();
	private final StringProperty currentProvider = new SimpleStringProperty(IncrementingNameGeneratorProvider.ID);
	private final ObjectProperty<Mappings> mappingsToApply = new SimpleObjectProperty<>();
	private final ListView<FilterWithConfigNode<?>> filters = new ListView<>();
	private final List<String> stringPredicates;
	private final List<String> stringPredicatesWithNull;
	private final Workspace workspace;
	private final NameGeneratorProviders nameGeneratorProviders;
	private final StringPredicateProvider stringPredicateProvider;
	private final MappingGenerator mappingGenerator;
	private final ConfigComponentManager componentManager;
	private final InheritanceGraph inheritanceGraph;
	private final ModalPane modal = new ModalPane();
	private final MappingApplierService mappingApplierService;
	private final Pane previewGroup;

	@Inject
	public MappingGeneratorPane(@Nonnull Workspace workspace,
	                            @Nonnull NameGeneratorProviders nameGeneratorProviders,
	                            @Nonnull StringPredicateProvider stringPredicateProvider,
	                            @Nonnull MappingGenerator mappingGenerator,
	                            @Nonnull ConfigComponentManager componentManager,
	                            @Nonnull InheritanceGraphService graphService,
	                            @Nonnull AggregateMappingManager aggregateMappingManager,
	                            @Nonnull MappingApplierService mappingApplierService,
	                            @Nonnull Instance<SearchBar> searchBarProvider) {

		this.workspace = workspace;
		this.nameGeneratorProviders = nameGeneratorProviders;
		this.stringPredicateProvider = stringPredicateProvider;
		this.mappingGenerator = mappingGenerator;
		this.componentManager = componentManager;
		this.inheritanceGraph = Objects.requireNonNull(graphService.getCurrentWorkspaceInheritanceGraph(), "Graph not created");
		this.mappingApplierService = mappingApplierService;

		// Cache text matchers.
		stringPredicates = stringPredicateProvider.getBiStringMatchers().keySet().stream().sorted().toList();
		stringPredicatesWithNull = Lists.add(stringPredicates, null);

		// Create filter list and editor controls.
		Node filterGroup = createFilterDisplay(aggregateMappingManager);

		// Create preview.
		BorderPane wrapper = new BorderPane();
		previewGroup = wrapper;
		FxThreadUtil.run(() -> wrapper.setCenter(createPreviewDisplay(searchBarProvider)));

		// Layout and wrap up.
		SplitPane horizontalWrapper = new SplitPane(filterGroup, previewGroup);
		horizontalWrapper.setDividerPositions(1); // Counter-intuitive, this correctly places the divider to not suffocate the filter-group
		SplitPane.setResizableWithParent(filterGroup, false);

		getChildren().addAll(modal, horizontalWrapper);
	}

	@PreDestroy
	private void destroy() {
		// We want to clear out a number of things here to assist in proper GC cleanup.
		//  - Mappings (which can hold a workspace/graph reference)
		//  - UI components (which can hold large virtualized text models for the mapping text representation)
		FxThreadUtil.run(() -> {
			mappingsToApply.setValue(null);
			previewGroup.getChildren().clear();
			getChildren().clear();
		});
	}

	public void addConfiguredFilter(@Nonnull FilterWithConfigNode<?> filterConfig) {
		filters.getItems().add(filterConfig);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void configureGenerator() {
		// Create the configuration card.
		Card card = new Card();
		card.maxWidthProperty().bind(widthProperty().multiply(0.70));
		card.maxHeightProperty().bind(heightProperty().divide(2));

		// Populate it based on available items in the current name provider's config.
		ConfigContainer container = nameGeneratorProviders.getProviders().get(currentProvider.get());
		if (container != null && !container.getValues().isEmpty()) {
			boolean isThirdPartyConfig = ConfigGroups.EXTERNAL.equals(container.getGroup());
			GridPane grid = createGrid();
			container.getValues().forEach((id, value) -> {
				int row = grid.getRowCount();
				String key = container.getScopedId(value);
				if (isThirdPartyConfig) {
					grid.add(new Label(value.getId()), 0, row);
				} else {
					grid.add(new BoundLabel(getBinding(key)), 0, row);
				}
				ConfigComponentFactory factory = componentManager.getFactory(container, value);
				Node node = factory.create(container, value);
				grid.add(node, 1, row);
			});
			card.setBody(grid);
		} else {
			BoundLabel label = new BoundLabel(getBinding("mapgen.configure.nothing"));
			label.setMaxHeight(Integer.MAX_VALUE);
			label.setMaxWidth(Integer.MAX_VALUE);
			label.setTextAlignment(TextAlignment.CENTER);
			label.setAlignment(Pos.CENTER);
			card.setBody(label);
		}

		// Show the configuration card.
		modal.show(card);
	}

	/**
	 * Generates the mappings for the current settings. The result is displayed in the {@link #previewGroup}.
	 */
	public void generate() {
		// Linking items in reverse order from the list.
		NameGeneratorFilter filter = null;
		List<FilterWithConfigNode<?>> list = filters.getItems();
		for (int i = list.size() - 1; i >= 0; i--) {
			FilterWithConfigNode<?> configurableFilter = list.get(i);
			filter = configurableFilter.build(filter);
		}
		NameGeneratorFilter finalFilter = filter;

		// Generate the mappings
		previewGroup.setDisable(true);
		CompletableFuture.supplyAsync(() -> {
			// Get provider from current chosen option, or fallback if option is unavailable
			NameGeneratorProvider<?> provider = nameGeneratorProviders.getProviders()
					.getOrDefault(currentProvider.get(), fallbackProvider);
			NameGenerator generator = provider.createGenerator();

			// Pass along the workspace for name deconfliction
			if (generator instanceof DeconflictingNameGenerator deconflictingGenerator)
				deconflictingGenerator.setWorkspace(workspace);

			// Generate the mappings
			return mappingGenerator.generate(workspace, workspace.getPrimaryResource(), inheritanceGraph, generator, finalFilter);
		}).whenCompleteAsync((mappings, error) -> {
			previewGroup.setDisable(false);
			if (mappings != null) {
				mappingsToApply.set(mappings);
			} else if (error != null) {
				logger.error("Failed to generate mappings", error);
			}
		}, FxThreadUtil.executor());
	}

	private void apply() {
		Mappings mappings = mappingsToApply.get();
		if (mappings == null)
			return;

		MappingApplier applier = mappingApplierService.inCurrentWorkspace();
		if (applier == null)
			return;

		// Apply the mappings
		MappingResults results = applier.applyToPrimaryResource(mappings);
		results.apply();

		// Clear property now that the mappings have been applied
		mappingsToApply.set(null);
	}

	@Nonnull
	private Node createPreviewDisplay(@Nonnull Instance<SearchBar> searchBarProvider) {
		// Editor to preview the current mappings
		Editor editor = new Editor();
		editor.setText("# A preview of your mappings will appear here once generated");
		editor.getStylesheets().add(LanguageStylesheets.getEnigmaStylesheet());
		editor.setSyntaxHighlighter(new RegexSyntaxHighlighter(RegexLanguages.getLangEngimaMap()));
		editor.getCodeArea().setEditable(false);
		editor.disableProperty().bind(mappingsToApply.isNull());
		editor.getStyleClass().add(Styles.BORDER_DEFAULT);
		searchBarProvider.get().install(editor);

		// Label describing how much of the workspace will be updated by the current mappings.
		Label stats = new Label();
		stats.textProperty().bind(Lang.getBinding("mapgen.preview.empty"));
		mappingsToApply.addListener((ob, old, cur) -> {
			if (stats.textProperty().isBound()) stats.textProperty().unbind();
			if (cur == null) {
				// Shouldn't happen, but here just in case
				FxThreadUtil.run(() -> {
					stats.textProperty().bind(Lang.getBinding("mapgen.preview.empty"));
					editor.setText("# Nothing");
				});
			} else {
				// Update stats
				IntermediateMappings mappings = cur.exportIntermediate();
				int classes = mappings.getClasses().size();
				int fields = mappings.getFields().size();
				int methods = mappings.getMethods().size();
				int variables = mappings.getVariables().size();
				String formatted = """
						Mappings will rename:
						 - %d classes
						 - %d fields
						 - %d methods
						 - %d variables
						""".formatted(classes, fields, methods, variables);
				stats.setText(formatted);

				// Also update editor preview
				String mappingText = new EnigmaMappings().exportText(mappings).replace("\0", "");
				FxThreadUtil.run(() -> editor.setText(mappingText));
			}
		});

		// Buttons to generate/apply mappings
		Button configureGenerator = new ActionButton(new FontIconView(CarbonIcons.SETTINGS, 20), this::configureGenerator);
		Button generateButton = new ActionButton(Lang.getBinding("mapgen.generate"), this::generate);
		Button applyButton = new ActionButton(Lang.getBinding("mapgen.apply"), this::apply);
		List<String> providerIds = nameGeneratorProviders.getProviders().keySet().stream().sorted().collect(Collectors.toList());
		BoundComboBox<String> generatorCombo = new BoundComboBox<>(currentProvider, providerIds, new StringConverter<>() {
			@Override
			public String toString(String s) {
				return StringUtil.uppercaseFirstChar(s).replace('-', ' ');
			}

			@Override
			public String fromString(String s) {
				return s;
			}
		});
		generatorCombo.getStyleClass().add("dark-combo-box");
		generateButton.setMaxWidth(Double.MAX_VALUE);
		applyButton.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(applyButton, Priority.ALWAYS);
		applyButton.disableProperty().bind(mappingsToApply.isNull());

		// Layout
		InputGroup inputGroup = new InputGroup(configureGenerator, generateButton, generatorCombo);
		HBox.setHgrow(generateButton, Priority.ALWAYS);
		HBox.setHgrow(inputGroup, Priority.ALWAYS);
		HBox buttons = new HBox(inputGroup, applyButton);
		buttons.setSpacing(10);
		VBox layout = new VBox(editor, stats, buttons);
		VBox.setVgrow(editor, Priority.ALWAYS);
		layout.setSpacing(10);
		return layout;
	}

	@Nonnull
	private Node createFilterDisplay(@Nonnull AggregateMappingManager aggregateMappingManager) {
		// List to house current filters.
		filters.setCellFactory(param -> new ConfiguredFilterCell());
		filters.getItems().add(new ExcludeExistingMapped(aggregateMappingManager.getAggregatedMappings()));
		ReadOnlyObjectProperty<FilterWithConfigNode<?>> selectedItem = filters.getSelectionModel().selectedItemProperty();
		BooleanBinding hasItemSelection = selectedItem.isNull();

		// Button to add new filters to the list.
		// - Pops open a modal to configure the filter, and an OK button to commit it.
		ObjectProperty<Supplier<FilterWithConfigNode<?>>> nodeSupplier = new SimpleObjectProperty<>();
		Button addFilter = new ActionButton(CarbonIcons.ADD_ALT, Lang.getBinding("mapgen.filters.add"), () -> {
			FilterWithConfigNode<?> filterConfig = nodeSupplier.get().get();
			showConfigurator(filterConfig, () -> addConfiguredFilter(filterConfig));
		});
		addFilter.disableProperty().bind(nodeSupplier.isNull());

		// Button to edit the selected filter.
		Button editSelectedFilter = new ActionButton(CarbonIcons.EDIT, Lang.getBinding("mapgen.filters.edit"), () -> {
			FilterWithConfigNode<?> filterConfig = selectedItem.get();
			showConfigurator(filterConfig, null);
		});
		BooleanProperty isZero = new SimpleBooleanProperty();
		selectedItem.addListener((ob, old, cur) -> isZero.set(cur == null || cur.getInputCount() == 0));
		editSelectedFilter.disableProperty().bind(hasItemSelection.or(isZero));

		// Button to remove the selected filter.
		Button deleteSelectedFilter = new ActionButton(CarbonIcons.TRASH_CAN, Lang.getBinding("mapgen.filters.delete"), () -> {
			filters.getItems().remove(selectedItem.get());
		});
		deleteSelectedFilter.disableProperty().bind(hasItemSelection);

		// Dropdown to change the type of filter made with the add button.
		MenuButton dropdown = new MenuButton();
		StringProperty dropdownText = dropdown.textProperty();
		dropdownText.bind(Lang.getBinding("mapgen.filters.type"));
		dropdown.getItems().addAll(
				typeSetAction(nodeSupplier, dropdownText, "mapgen.filter.excludealreadymapped",
						() -> new ExcludeExistingMapped(aggregateMappingManager.getAggregatedMappings())),
				typeSetAction(nodeSupplier, dropdownText, "mapgen.filter.excludename", ExcludeName::new),
				typeSetAction(nodeSupplier, dropdownText, "mapgen.filter.excludeclasses", ExcludeClasses::new),
				typeSetAction(nodeSupplier, dropdownText, "mapgen.filter.excludemodifier", ExcludeModifiers::new),
				typeSetAction(nodeSupplier, dropdownText, "mapgen.filter.includename", IncludeName::new),
				typeSetAction(nodeSupplier, dropdownText, "mapgen.filter.includelong", IncludeLongName::new),
				typeSetAction(nodeSupplier, dropdownText, "mapgen.filter.includeclasses", IncludeClasses::new),
				typeSetAction(nodeSupplier, dropdownText, "mapgen.filter.includemodifier", IncludeModifiers::new),
				typeSetAction(nodeSupplier, dropdownText, "mapgen.filter.includewhitespacenames", IncludeWhitespaceNames::new),
				typeSetAction(nodeSupplier, dropdownText, "mapgen.filter.includenonasciinames", IncludeNonAsciiNames::new),
				typeSetAction(nodeSupplier, dropdownText, "mapgen.filter.includekeywords", IncludeKeywordNames::new)
		);

		// Layout
		addFilter.setMaxWidth(Double.MAX_VALUE);
		editSelectedFilter.setMaxWidth(Double.MAX_VALUE);
		deleteSelectedFilter.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(addFilter, Priority.ALWAYS);
		HBox.setHgrow(editSelectedFilter, Priority.ALWAYS);
		HBox.setHgrow(deleteSelectedFilter, Priority.ALWAYS);
		VBox filterButtons = new VBox(new InputGroup(dropdown, addFilter),
				new HBox(editSelectedFilter, deleteSelectedFilter)
		);
		filterButtons.setFillWidth(true);
		VBox layout = new VBox(filters, filterButtons);
		VBox.setVgrow(filters, Priority.ALWAYS);
		return layout;
	}

	@Nonnull
	private ActionMenuItem typeSetAction(@Nonnull ObjectProperty<Supplier<FilterWithConfigNode<?>>> nodeSupplier,
	                                     @Nonnull StringProperty parentText,
	                                     @Nonnull String translationKey,
	                                     @Nonnull Supplier<FilterWithConfigNode<?>> supplier) {
		StringBinding nameBinding = Lang.getBinding(translationKey);
		return new ActionMenuItem(nameBinding,
				() -> {
					nodeSupplier.set(supplier);
					parentText.unbind();
					parentText.bind(nameBinding);
				});
	}

	private void showConfigurator(@Nonnull FilterWithConfigNode<?> filterConfig, @Nullable Runnable onDone) {
		Node configurator = filterConfig.getConfigurator();
		int inputCount = filterConfig.getInputCount();
		if (inputCount > 0) {
			// At least one input, show them via the modal
			Label title = new BoundLabel(filterConfig.display());
			title.getStyleClass().add(Styles.TITLE_3);
			VBox layout = new VBox(title, configurator, new ActionButton(Lang.getBinding("misc.done"), () -> {
				modal.hide(true);
				if (onDone != null)
					onDone.run();
			}));
			layout.setSpacing(10);
			layout.setFillWidth(true);
			layout.setAlignment(Pos.CENTER);
			Card card = new Card();
			card.maxWidthProperty().bind(widthProperty().multiply(0.70));
			card.maxHeightProperty().bind(heightProperty().divide(2));
			card.setBody(layout);
			modal.show(card);
			configurator.requestFocus();
		} else {
			// No inputs, just do the action
			if (onDone != null)
				onDone.run();
		}
	}

	@Nonnull
	private static String predicateIdToTranslation(@Nullable String id) {
		if (id == null) return Lang.get("misc.ignored");
		return Lang.get(StringPredicate.TRANSLATION_PREFIX + id);
	}

	@Nonnull
	private static GridPane createGrid() {
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setVgap(5);
		grid.setHgap(5);
		grid.setPadding(new Insets(5));
		return grid;
	}

	/**
	 * Config node for {@link ExcludeNameFilter}.
	 */
	public class ExcludeName extends FilterWithConfigNode<ExcludeNameFilter> {
		private final StringProperty classPredicateId = new SimpleStringProperty();
		private final StringProperty fieldPredicateId = new SimpleStringProperty();
		private final StringProperty methodPredicateId = new SimpleStringProperty();
		private final StringProperty variablePredicateId = new SimpleStringProperty();
		private final StringProperty className = new SimpleStringProperty("com/example/Foo");
		private final StringProperty fieldName = new SimpleStringProperty("foo");
		private final StringProperty methodName = new SimpleStringProperty("getFoo");
		private final StringProperty variableName = new SimpleStringProperty("fizz");

		@Nonnull
		@Override
		public ObservableValue<String> display() {
			return classPredicateId.isNotNull().flatMap(hasClass -> {
				if (hasClass)
					return Bindings.concat(Lang.getBinding("mapgen.filter.excludename"), ": ", classPredicateId.flatMap(key -> {
						if (StringPredicateProvider.KEY_ANYTHING.equals(key))
							return Lang.getBinding("string.match.anything");
						else if (StringPredicateProvider.KEY_NOTHING.equals(key))
							return Lang.getBinding("string.match.zilch");
						else
							return className;
					}));
				return fieldPredicateId.isNotNull().flatMap(hasField -> {
					if (hasField)
						return Bindings.concat(Lang.getBinding("mapgen.filter.excludename"), ": ", fieldPredicateId.flatMap(key -> {
							if (StringPredicateProvider.KEY_ANYTHING.equals(key))
								return Lang.getBinding("string.match.anything");
							else if (StringPredicateProvider.KEY_NOTHING.equals(key))
								return Lang.getBinding("string.match.zilch");
							else
								return fieldName;
						}));
					return methodPredicateId.isNotNull().flatMap(hasMethod -> {
						if (hasMethod)
							return Bindings.concat(Lang.getBinding("mapgen.filter.excludename"), ": ", methodPredicateId.flatMap(key -> {
								if (StringPredicateProvider.KEY_ANYTHING.equals(key))
									return Lang.getBinding("string.match.anything");
								else if (StringPredicateProvider.KEY_NOTHING.equals(key))
									return Lang.getBinding("string.match.zilch");
								else
									return methodName;
							}));
						return variablePredicateId.isNotNull().flatMap(hasVariable -> {
							if (hasVariable)
								return Bindings.concat(Lang.getBinding("mapgen.filter.excludename"), ": ", variablePredicateId.flatMap(key -> {
									if (StringPredicateProvider.KEY_ANYTHING.equals(key))
										return Lang.getBinding("string.match.anything");
									else if (StringPredicateProvider.KEY_NOTHING.equals(key))
										return Lang.getBinding("string.match.zilch");
									else
										return variableName;
								}));
							return Lang.getBinding("misc.ignored");
						});
					});
				});
			});
		}

		@Nonnull
		@Override
		protected Function<NameGeneratorFilter, ExcludeNameFilter> makeProvider() {
			return next -> new ExcludeNameFilter(next,
					classPredicateId.isNotNull().get() ? stringPredicateProvider.newBiStringPredicate(classPredicateId.get(), className.get()) : null,
					fieldPredicateId.isNotNull().get() ? stringPredicateProvider.newBiStringPredicate(fieldPredicateId.get(), fieldName.get()) : null,
					methodPredicateId.isNotNull().get() ? stringPredicateProvider.newBiStringPredicate(methodPredicateId.get(), methodName.get()) : null,
					variablePredicateId.isNotNull().get() ? stringPredicateProvider.newBiStringPredicate(variablePredicateId.get(), variableName.get()) : null
			);
		}

		@Override
		protected void fillConfigurator(@Nonnull BiConsumer<StringBinding, Node> sink) {
			BoundTextField txtClass = new BoundTextField(className);
			BoundTextField txtField = new BoundTextField(fieldName);
			BoundTextField txtMethod = new BoundTextField(methodName);
			BoundTextField txtVariable = new BoundTextField(variableName);
			txtClass.disableProperty().bind(classPredicateId.isNull()
					.or(classPredicateId.isEqualTo(StringPredicateProvider.KEY_ANYTHING))
					.or(classPredicateId.isEqualTo(StringPredicateProvider.KEY_NOTHING)));
			txtField.disableProperty().bind(fieldPredicateId.isNull()
					.or(fieldPredicateId.isEqualTo(StringPredicateProvider.KEY_ANYTHING))
					.or(fieldPredicateId.isEqualTo(StringPredicateProvider.KEY_NOTHING)));
			txtMethod.disableProperty().bind(methodPredicateId.isNull()
					.or(methodPredicateId.isEqualTo(StringPredicateProvider.KEY_ANYTHING))
					.or(methodPredicateId.isEqualTo(StringPredicateProvider.KEY_NOTHING)));
			txtVariable.disableProperty().bind(variablePredicateId.isNull()
					.or(variablePredicateId.isEqualTo(StringPredicateProvider.KEY_ANYTHING))
					.or(variablePredicateId.isEqualTo(StringPredicateProvider.KEY_NOTHING)));

			GridPane grid = new GridPane();
			grid.setVgap(5);
			grid.setHgap(5);
			grid.addRow(0, new BoundLabel(Lang.getBinding("mapgen.filter.owner-name")),
					txtClass, new BoundComboBox<>(classPredicateId, stringPredicatesWithNull, textPredicateConverter));
			grid.addRow(1, new BoundLabel(Lang.getBinding("mapgen.filter.field-name")),
					txtField, new BoundComboBox<>(fieldPredicateId, stringPredicatesWithNull, textPredicateConverter));
			grid.addRow(2, new BoundLabel(Lang.getBinding("mapgen.filter.method-name")),
					txtMethod, new BoundComboBox<>(methodPredicateId, stringPredicatesWithNull, textPredicateConverter));
			grid.addRow(3, new BoundLabel(Lang.getBinding("mapgen.filter.variable-name")),
					txtVariable, new BoundComboBox<>(variablePredicateId, stringPredicatesWithNull, textPredicateConverter));
			sink.accept(null, grid);
		}
	}

	/**
	 * Config node for {@link ExcludeClassesFilter}.
	 */
	public class ExcludeClasses extends FilterWithConfigNode<ExcludeClassesFilter> {
		private final StringProperty classPredicateId = new SimpleStringProperty(StringPredicateProvider.KEY_CONTAINS);
		private final StringProperty className = new SimpleStringProperty("com/example/Foo");

		@Nonnull
		@Override
		public ObservableValue<String> display() {
			return Bindings.concat(Lang.getBinding("mapgen.filter.excludeclasses"), ": ", className);
		}

		@Nonnull
		@Override
		@SuppressWarnings("DataFlowIssue")
		protected Function<NameGeneratorFilter, ExcludeClassesFilter> makeProvider() {
			return next -> {
				String id = classPredicateId.get();
				StringPredicate predicate;
				if (id == null) predicate = stringPredicateProvider.newNothingPredicate();
				else predicate = stringPredicateProvider.newBiStringPredicate(id, className.get());
				return new ExcludeClassesFilter(next, predicate);
			};
		}

		@Override
		protected void fillConfigurator(@Nonnull BiConsumer<StringBinding, Node> sink) {
			BoundTextField txtClass = new BoundTextField(className);
			sink.accept(Lang.getBinding("search.textmode"), new BoundComboBox<>(classPredicateId, stringPredicates, textPredicateConverter));
			sink.accept(Lang.getBinding("mapgen.filter.class-name"), txtClass);
		}
	}

	/**
	 * Config node for {@link ExcludeExistingMappedFilter}.
	 */
	public static class ExcludeExistingMapped extends FilterWithConfigNode<ExcludeExistingMappedFilter> {
		private final AggregatedMappings aggregate;

		private ExcludeExistingMapped(@Nonnull AggregatedMappings aggregate) {
			this.aggregate = aggregate;
		}

		@Nonnull
		@Override
		public ObservableValue<String> display() {
			return Lang.getBinding("mapgen.filter.excludealreadymapped");
		}

		@Nonnull
		@Override
		protected Function<NameGeneratorFilter, ExcludeExistingMappedFilter> makeProvider() {
			return next -> new ExcludeExistingMappedFilter(next, aggregate);
		}

		@Override
		protected void fillConfigurator(@Nonnull BiConsumer<StringBinding, Node> sink) {
		}
	}

	/**
	 * Config node for {@link ExcludeModifiersNameFilter}.
	 */
	public static class ExcludeModifiers extends FilterWithConfigNode<ExcludeModifiersNameFilter> {
		private final StringProperty modifiers = new SimpleStringProperty("public protected");
		private final BooleanProperty classes = new SimpleBooleanProperty(true);
		private final BooleanProperty fields = new SimpleBooleanProperty(true);
		private final BooleanProperty methods = new SimpleBooleanProperty(true);

		@Nonnull
		@Override
		public ObservableValue<String> display() {
			return Bindings.concat(Lang.getBinding("mapgen.filter.excludemodifier"), ": ", modifiers.map(text -> {
				String[] words = text.split("\\s+");
				return Arrays.stream(words)
						.filter(word -> !AccessFlag.getFlags(word).isEmpty())
						.collect(Collectors.joining(" "));
			}));
		}

		@Nonnull
		@Override
		protected Function<NameGeneratorFilter, ExcludeModifiersNameFilter> makeProvider() {
			return next -> {
				Collection<Integer> flags = AccessFlag.getFlags(modifiers.get()).stream()
						.map(AccessFlag::getMask)
						.collect(Collectors.toList());
				return new ExcludeModifiersNameFilter(next, flags, classes.get(), fields.get(), methods.get());
			};
		}

		@Override
		protected void fillConfigurator(@Nonnull BiConsumer<StringBinding, Node> sink) {
			sink.accept(Lang.getBinding("mapgen.filter.excludemodifier"), new BoundTextField(modifiers)
					.withTooltip("mapgen.filter.modifiers.tooltip"));
			sink.accept(null, new BoundCheckBox(Lang.getBinding("mapgen.filter.excludeclass"), classes));
			sink.accept(null, new BoundCheckBox(Lang.getBinding("mapgen.filter.excludefield"), fields));
			sink.accept(null, new BoundCheckBox(Lang.getBinding("mapgen.filter.excludemethod"), methods));
		}
	}

	/**
	 * Config node for {@link IncludeNameFilter}.
	 */
	public class IncludeName extends FilterWithConfigNode<IncludeNameFilter> {
		private final StringProperty classPredicateId = new SimpleStringProperty();
		private final StringProperty fieldPredicateId = new SimpleStringProperty();
		private final StringProperty methodPredicateId = new SimpleStringProperty();
		private final StringProperty variablePredicateId = new SimpleStringProperty();
		private final StringProperty className = new SimpleStringProperty("com/example/Foo");
		private final StringProperty fieldName = new SimpleStringProperty("foo");
		private final StringProperty methodName = new SimpleStringProperty("getFoo");
		private final StringProperty variableName = new SimpleStringProperty("fizz");

		@Nonnull
		@Override
		public ObservableValue<String> display() {
			return classPredicateId.isNotNull().flatMap(hasClass -> {
				if (hasClass)
					return Bindings.concat(Lang.getBinding("mapgen.filter.includename"), ": ", classPredicateId.flatMap(key -> {
						if (StringPredicateProvider.KEY_ANYTHING.equals(key))
							return Lang.getBinding("string.match.anything");
						else if (StringPredicateProvider.KEY_NOTHING.equals(key))
							return Lang.getBinding("string.match.zilch");
						else
							return className;
					}));
				return fieldPredicateId.isNotNull().flatMap(hasField -> {
					if (hasField)
						return Bindings.concat(Lang.getBinding("mapgen.filter.includename"), ": ", fieldPredicateId.flatMap(key -> {
							if (StringPredicateProvider.KEY_ANYTHING.equals(key))
								return Lang.getBinding("string.match.anything");
							else if (StringPredicateProvider.KEY_NOTHING.equals(key))
								return Lang.getBinding("string.match.zilch");
							else
								return fieldName;
						}));
					return methodPredicateId.isNotNull().flatMap(hasMethod -> {
						if (hasMethod)
							return Bindings.concat(Lang.getBinding("mapgen.filter.includename"), ": ", methodPredicateId.flatMap(key -> {
								if (StringPredicateProvider.KEY_ANYTHING.equals(key))
									return Lang.getBinding("string.match.anything");
								else if (StringPredicateProvider.KEY_NOTHING.equals(key))
									return Lang.getBinding("string.match.zilch");
								else
									return methodName;
							}));
						return variablePredicateId.isNotNull().flatMap(hasVariable -> {
							if (hasVariable)
								return Bindings.concat(Lang.getBinding("mapgen.filter.includename"), ": ", variablePredicateId.flatMap(key -> {
									if (StringPredicateProvider.KEY_ANYTHING.equals(key))
										return Lang.getBinding("string.match.anything");
									else if (StringPredicateProvider.KEY_NOTHING.equals(key))
										return Lang.getBinding("string.match.zilch");
									else
										return variableName;
								}));
							return Lang.getBinding("misc.ignored");
						});
					});
				});
			});
		}

		@Nonnull
		@Override
		protected Function<NameGeneratorFilter, IncludeNameFilter> makeProvider() {
			return next -> new IncludeNameFilter(next,
					classPredicateId.isNotNull().get() ? stringPredicateProvider.newBiStringPredicate(classPredicateId.get(), className.get()) : null,
					fieldPredicateId.isNotNull().get() ? stringPredicateProvider.newBiStringPredicate(fieldPredicateId.get(), fieldName.get()) : null,
					methodPredicateId.isNotNull().get() ? stringPredicateProvider.newBiStringPredicate(methodPredicateId.get(), methodName.get()) : null,
					variablePredicateId.isNotNull().get() ? stringPredicateProvider.newBiStringPredicate(variablePredicateId.get(), variableName.get()) : null
			);
		}

		@Override
		protected void fillConfigurator(@Nonnull BiConsumer<StringBinding, Node> sink) {
			BoundTextField txtClass = new BoundTextField(className);
			BoundTextField txtField = new BoundTextField(fieldName);
			BoundTextField txtMethod = new BoundTextField(methodName);
			BoundTextField txtVariable = new BoundTextField(variableName);
			txtClass.disableProperty().bind(classPredicateId.isNull()
					.or(classPredicateId.isEqualTo(StringPredicateProvider.KEY_ANYTHING))
					.or(classPredicateId.isEqualTo(StringPredicateProvider.KEY_NOTHING)));
			txtField.disableProperty().bind(fieldPredicateId.isNull()
					.or(fieldPredicateId.isEqualTo(StringPredicateProvider.KEY_ANYTHING))
					.or(fieldPredicateId.isEqualTo(StringPredicateProvider.KEY_NOTHING)));
			txtMethod.disableProperty().bind(methodPredicateId.isNull()
					.or(methodPredicateId.isEqualTo(StringPredicateProvider.KEY_ANYTHING))
					.or(methodPredicateId.isEqualTo(StringPredicateProvider.KEY_NOTHING)));
			txtVariable.disableProperty().bind(variablePredicateId.isNull()
					.or(variablePredicateId.isEqualTo(StringPredicateProvider.KEY_ANYTHING))
					.or(variablePredicateId.isEqualTo(StringPredicateProvider.KEY_NOTHING)));

			GridPane grid = new GridPane();
			grid.setVgap(5);
			grid.setHgap(5);
			grid.addRow(0, new BoundLabel(Lang.getBinding("mapgen.filter.owner-name")),
					txtClass, new BoundComboBox<>(classPredicateId, stringPredicatesWithNull, textPredicateConverter));
			grid.addRow(1, new BoundLabel(Lang.getBinding("mapgen.filter.field-name")),
					txtField, new BoundComboBox<>(fieldPredicateId, stringPredicatesWithNull, textPredicateConverter));
			grid.addRow(2, new BoundLabel(Lang.getBinding("mapgen.filter.method-name")),
					txtMethod, new BoundComboBox<>(methodPredicateId, stringPredicatesWithNull, textPredicateConverter));
			grid.addRow(3, new BoundLabel(Lang.getBinding("mapgen.filter.variable-name")),
					txtMethod, new BoundComboBox<>(variablePredicateId, stringPredicatesWithNull, textPredicateConverter));
			sink.accept(null, grid);
		}
	}

	/**
	 * Config node for {@link IncludeClassesFilter}.
	 */
	public class IncludeClasses extends FilterWithConfigNode<IncludeClassesFilter> {
		private final StringProperty classPredicateId = new SimpleStringProperty(StringPredicateProvider.KEY_CONTAINS);
		private final StringProperty className = new SimpleStringProperty("com/example/Foo");

		@Nonnull
		@Override
		public ObservableValue<String> display() {
			return Bindings.concat(Lang.getBinding("mapgen.filter.includeclasses"), ": ", className);
		}

		@Nonnull
		@Override
		@SuppressWarnings("DataFlowIssue")
		protected Function<NameGeneratorFilter, IncludeClassesFilter> makeProvider() {
			return next -> {
				String id = classPredicateId.get();
				StringPredicate predicate;
				if (id == null) predicate = stringPredicateProvider.newNothingPredicate();
				else predicate = stringPredicateProvider.newBiStringPredicate(id, className.get());
				return new IncludeClassesFilter(next, predicate);
			};
		}

		@Override
		protected void fillConfigurator(@Nonnull BiConsumer<StringBinding, Node> sink) {
			BoundTextField txtClass = new BoundTextField(className);
			sink.accept(Lang.getBinding("search.textmode"), new BoundComboBox<>(classPredicateId, stringPredicates, textPredicateConverter));
			sink.accept(Lang.getBinding("mapgen.filter.class-name"), txtClass);
		}
	}

	/**
	 * Config node for {@link IncludeModifiersNameFilter}.
	 */
	public static class IncludeModifiers extends FilterWithConfigNode<IncludeModifiersNameFilter> {
		private final StringProperty modifiers = new SimpleStringProperty("public protected");
		private final BooleanProperty classes = new SimpleBooleanProperty(true);
		private final BooleanProperty fields = new SimpleBooleanProperty(true);
		private final BooleanProperty methods = new SimpleBooleanProperty(true);

		@Nonnull
		@Override
		public ObservableValue<String> display() {
			return Bindings.concat(Lang.getBinding("mapgen.filter.includemodifier"), ": ", modifiers.map(text -> {
				String[] words = text.split("\\s+");
				return Arrays.stream(words)
						.filter(word -> !AccessFlag.getFlags(word).isEmpty())
						.collect(Collectors.joining(" "));
			}));
		}

		@Nonnull
		@Override
		protected Function<NameGeneratorFilter, IncludeModifiersNameFilter> makeProvider() {
			return next -> {
				Collection<Integer> flags = AccessFlag.getFlags(modifiers.get()).stream()
						.map(AccessFlag::getMask)
						.collect(Collectors.toList());
				return new IncludeModifiersNameFilter(next, flags, classes.get(), fields.get(), methods.get());
			};
		}

		@Override
		protected void fillConfigurator(@Nonnull BiConsumer<StringBinding, Node> sink) {
			sink.accept(Lang.getBinding("mapgen.filter.includemodifier"), new BoundTextField(modifiers)
					.withTooltip("mapgen.filter.modifiers.tooltip"));
			sink.accept(null, new BoundCheckBox(Lang.getBinding("mapgen.filter.includeclass"), classes));
			sink.accept(null, new BoundCheckBox(Lang.getBinding("mapgen.filter.includefield"), fields));
			sink.accept(null, new BoundCheckBox(Lang.getBinding("mapgen.filter.includemethod"), methods));
		}
	}

	/**
	 * Config node for {@link IncludeLongNameFilter}.
	 */
	public static class IncludeLongName extends FilterWithConfigNode<IncludeLongNameFilter> {
		private final IntegerProperty length = new SimpleIntegerProperty();
		private final BooleanProperty classes = new SimpleBooleanProperty(true);
		private final BooleanProperty fields = new SimpleBooleanProperty(true);
		private final BooleanProperty methods = new SimpleBooleanProperty(true);
		private final BooleanProperty variables = new SimpleBooleanProperty(true);

		@Nonnull
		@Override
		public ObservableValue<String> display() {
			return Bindings.concat(Lang.getBinding("mapgen.filter.includelong"), ": ", length.map(i -> Integer.toString(i.intValue())));
		}

		@Nonnull
		@Override
		protected Function<NameGeneratorFilter, IncludeLongNameFilter> makeProvider() {
			return next -> new IncludeLongNameFilter(next, length.get(), classes.get(), fields.get(), methods.get(), variables.get());
		}

		@Override
		protected void fillConfigurator(@Nonnull BiConsumer<StringBinding, Node> sink) {
			sink.accept(Lang.getBinding("mapgen.filter.includelong"), new BoundIntSpinner(length));
			sink.accept(null, new BoundCheckBox(Lang.getBinding("mapgen.filter.includeclass"), classes));
			sink.accept(null, new BoundCheckBox(Lang.getBinding("mapgen.filter.includefield"), fields));
			sink.accept(null, new BoundCheckBox(Lang.getBinding("mapgen.filter.includemethod"), methods));
			sink.accept(null, new BoundCheckBox(Lang.getBinding("mapgen.filter.includevariable"), variables));
		}
	}

	/**
	 * Config node for {@link IncludeKeywordNameFilter}.
	 */
	public static class IncludeKeywordNames extends FilterWithConfigNode<IncludeKeywordNameFilter> {
		@Nonnull
		@Override
		public ObservableValue<String> display() {
			return Lang.getBinding("mapgen.filter.includekeywords");
		}

		@Nonnull
		@Override
		protected Function<NameGeneratorFilter, IncludeKeywordNameFilter> makeProvider() {
			return IncludeKeywordNameFilter::new;
		}

		@Override
		protected void fillConfigurator(@Nonnull BiConsumer<StringBinding, Node> sink) {
		}
	}

	/**
	 * Config node for {@link IncludeNonAsciiNameFilter}.
	 */
	public static class IncludeNonAsciiNames extends FilterWithConfigNode<IncludeNonAsciiNameFilter> {
		@Nonnull
		@Override
		public ObservableValue<String> display() {
			return Lang.getBinding("mapgen.filter.includenonasciinames");
		}

		@Nonnull
		@Override
		protected Function<NameGeneratorFilter, IncludeNonAsciiNameFilter> makeProvider() {
			return IncludeNonAsciiNameFilter::new;
		}

		@Override
		protected void fillConfigurator(@Nonnull BiConsumer<StringBinding, Node> sink) {
		}
	}

	/**
	 * Config node for {@link IncludeNonAsciiNameFilter}.
	 */
	public static class IncludeWhitespaceNames extends FilterWithConfigNode<IncludeWhitespaceNameFilter> {
		@Nonnull
		@Override
		public ObservableValue<String> display() {
			return Lang.getBinding("mapgen.filter.includewhitespacenames");
		}

		@Nonnull
		@Override
		protected Function<NameGeneratorFilter, IncludeWhitespaceNameFilter> makeProvider() {
			return IncludeWhitespaceNameFilter::new;
		}

		@Override
		protected void fillConfigurator(@Nonnull BiConsumer<StringBinding, Node> sink) {
		}
	}

	/**
	 * List cell to render {@link FilterWithConfigNode}.
	 */
	private static class ConfiguredFilterCell extends ListCell<FilterWithConfigNode<?>> {
		@Override
		protected void updateItem(FilterWithConfigNode<?> item, boolean empty) {
			super.updateItem(item, empty);
			StringProperty property = textProperty();
			if (empty || item == null) {
				property.unbind();
				setText(null);
			} else {
				property.bind(item.display().map(display -> (getIndex() + 1) + ". " + display));
			}
		}
	}

	/**
	 * Base to create a filter with configuration node.
	 *
	 * @param <F>
	 * 		Filter type.
	 */
	public static abstract class FilterWithConfigNode<F extends NameGeneratorFilter> {
		private final Function<NameGeneratorFilter, F> filterProvider;
		private Node node;
		private int inputs;

		private FilterWithConfigNode() {
			this.filterProvider = makeProvider();
		}

		@Nonnull
		public abstract ObservableValue<String> display();

		@Nonnull
		public Node getConfigurator() {
			if (node == null) {
				GridPane grid = createGrid();
				fillConfigurator((key, editor) -> {
					inputs++;
					int row = grid.getRowCount();
					if (key == null) {
						grid.add(editor, 0, row, 2, 1);
					} else {
						grid.add(new BoundLabel(key), 0, row);
						grid.add(editor, 1, row);
					}
				});
				node = grid;
			}
			return node;
		}

		@Nonnull
		public NameGeneratorFilter build(@Nullable NameGeneratorFilter previous) {
			return filterProvider.apply(previous);
		}

		@Nonnull
		protected abstract Function<NameGeneratorFilter, F> makeProvider();

		/**
		 * @return Number of configurable inputs.
		 */
		public int getInputCount() {
			return inputs;
		}

		protected abstract void fillConfigurator(@Nonnull BiConsumer<StringBinding, Node> sink);
	}
}
