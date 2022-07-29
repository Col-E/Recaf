package me.coley.recaf.ui.pane;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import me.coley.recaf.Controller;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.mapping.*;
import me.coley.recaf.mapping.gen.MappingGenerator;
import me.coley.recaf.mapping.gen.NameGenerator;
import me.coley.recaf.mapping.gen.NameGeneratorFilter;
import me.coley.recaf.mapping.impl.IntermediateMappings;
import me.coley.recaf.ui.control.ActionButton;
import me.coley.recaf.ui.control.BoundLabel;
import me.coley.recaf.ui.control.EnumComboBox;
import me.coley.recaf.ui.dialog.TextInputDialog;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.Translatable;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pane that outlines inputs and outputs for using {@link MappingGenerator}.
 *
 * @author Matt Coley
 */
public class MappingGenPane extends VBox {
	private static final Logger logger = Logging.get(MappingGenPane.class);
	private final ListView<FilterIntermediate> filterList = new ListView<>();
	private final ComboBox<UxNameGenerator> generatorComboBox = new ComboBox<>();
	private final ComboBox<MappingsTool> outputTypeComboBox = new ComboBox<>();
	private final ComboBox<NewFilterMode> newFilterModeComboBox = new EnumComboBox<>(NewFilterMode.class,
			NewFilterMode.EXCLUDE_ALREADY_MAPPED);
	private final BooleanProperty hasFilterSelection = new SimpleBooleanProperty(false);

	/**
	 * Create the mapping pane.
	 */
	public MappingGenPane() {
		Controller controller = RecafUI.getController();
		MappingsManager mappingsManager = controller.getServices().getMappingsManager();
		// Populate models
		List<MappingsTool> outputMappingTypes = mappingsManager.getRegisteredImpls().stream()
				.filter(MappingsTool::supportsTextExport)
				.collect(Collectors.toList());
		outputTypeComboBox.getItems().addAll(outputMappingTypes);
		outputTypeComboBox.getSelectionModel().select(0);
		outputTypeComboBox.setConverter(new StringConverter<>() {
			@Override
			public String toString(MappingsTool mappingsTool) {
				return mappingsTool.getName();
			}

			@Override
			public MappingsTool fromString(String name) {
				throw new UnsupportedOperationException();
			}
		});
		generatorComboBox.getItems().addAll(createGenerators());
		generatorComboBox.getSelectionModel().select(0);
		generatorComboBox.setConverter(new StringConverter<>() {
			@Override
			public String toString(UxNameGenerator generator) {
				return generator.asString();
			}

			@Override
			public UxNameGenerator fromString(String name) {
				throw new UnsupportedOperationException();
			}
		});
		filterList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		filterList.setPrefHeight(300);
		// Add combo to change name generator model
		Label labelGeneratorTitle = new BoundLabel(Lang.getBinding("mapgen.genimpl"));
		HBox boxGeneratorWrapper = new HBox(labelGeneratorTitle, generatorComboBox);
		getChildren().add(boxGeneratorWrapper);
		// Add filter list
		Label labelFiltersTitle = new BoundLabel(Lang.getBinding("mapgen.filters"));
		getChildren().addAll(labelFiltersTitle, filterList);
		// Add controls to add/remove filters
		Button btnAddFilter = new ActionButton(Lang.getBinding("mapgen.filters.add"), () -> {
			ObservableList<FilterIntermediate> items = filterList.getItems();
			NewFilterMode filterMode = newFilterModeComboBox.getSelectionModel().getSelectedItem();
			FilterIntermediate filterIntermediate = filterMode.build(items);
			if (filterIntermediate != null)
				items.add(filterIntermediate);
		});
		Button btnRemoveRemove = new ActionButton(Lang.getBinding("mapgen.filters.delete"), () -> {
			filterList.getItems().remove(filterList.getSelectionModel().getSelectedItem());
		});
		filterList.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> hasFilterSelection.set(newValue != null));
		btnRemoveRemove.disableProperty().bind(hasFilterSelection.not());
		HBox boxFilterControls = new HBox(btnRemoveRemove, btnAddFilter, newFilterModeComboBox);
		getChildren().add(boxFilterControls);
		// Add button to apply the mappings
		Button copy = new ActionButton(Lang.getBinding("mapgen.copy"), () -> {
			IntermediateMappings intermediate = createMappings(controller).exportIntermediate();
			MappingsTool outputImpl = outputTypeComboBox.getSelectionModel().getSelectedItem();
			Mappings mappings = outputImpl.create();
			mappings.importIntermediate(intermediate);
			// Export
			ClipboardContent clipboard = new ClipboardContent();
			clipboard.putString(mappings.exportText());
			Clipboard.getSystemClipboard().setContent(clipboard);
			logger.info("Copied generated mappings to clipboard");
		});
		Button apply = new ActionButton(Lang.getBinding("mapgen.apply"), () -> {
			Mappings mappings = createMappings(controller);
			// Apply
			Resource primary = controller.getWorkspace().getResources().getPrimary();
			MappingUtils.applyMappings(ClassReader.EXPAND_FRAMES, 0, controller, primary, mappings);
			logger.info("Applied generated mappings");
		});
		HBox boxGenerateWrapper = new HBox(apply, copy, outputTypeComboBox);
		getChildren().add(boxGenerateWrapper);
		setPadding(new Insets(10));
		setFillWidth(true);
	}

	/**
	 * Content for {@link #newFilterModeComboBox}.
	 * Delegates creation of new {@link FilterIntermediate} to implementations based on selected value.
	 */
	private enum NewFilterMode implements Translatable {
		EXCLUDE_ALREADY_MAPPED,
		MODIFIER_EXCLUDE_CLASS,
		MODIFIER_EXCLUDE_FIELD,
		MODIFIER_EXCLUDE_METHOD,
		MODIFIER_INCLUDE_CLASS,
		MODIFIER_INCLUDE_FIELD,
		MODIFIER_INCLUDE_METHOD,
		PATH_INCLUDE,
		PATH_EXCLUDE;

		public FilterIntermediate build(List<FilterIntermediate> items) {
			switch (this) {
				case EXCLUDE_ALREADY_MAPPED:
					return new ExcludeExistingIntermediate(items);
				case MODIFIER_EXCLUDE_CLASS: {
					String text = queryText();
					if (text != null)
						return new ModifierIntermediate(items, AccessFlag.getFlags(text), false,
								true, false, false);
					break;
				}
				case MODIFIER_EXCLUDE_FIELD: {
					String text = queryText();
					if (text != null)
						return new ModifierIntermediate(items, AccessFlag.getFlags(text), false,
								false, true, false);
					break;
				}
				case MODIFIER_EXCLUDE_METHOD: {
					String text = queryText();
					if (text != null)
						return new ModifierIntermediate(items, AccessFlag.getFlags(text), false,
								false, false, true);
					break;
				}
				case MODIFIER_INCLUDE_CLASS: {
					String text = queryText();
					if (text != null)
						return new ModifierIntermediate(items, AccessFlag.getFlags(text), true,
								true, false, false);
					break;
				}
				case MODIFIER_INCLUDE_FIELD: {
					String text = queryText();
					if (text != null)
						return new ModifierIntermediate(items, AccessFlag.getFlags(text), true,
								false, true, false);
					break;
				}
				case MODIFIER_INCLUDE_METHOD: {
					String text = queryText();
					if (text != null)
						return new ModifierIntermediate(items, AccessFlag.getFlags(text), true,
								false, false, true);
					break;
				}
				case PATH_INCLUDE: {
					String text = queryText();
					if (text != null)
						return new PathFilterIntermediate(items, text, true);
					break;
				}
				case PATH_EXCLUDE: {
					String text = queryText();
					if (text != null)
						return new PathFilterIntermediate(items, text, false);
					break;
				}
			}
			return null;
		}

		@Override
		public String getTranslationKey() {
			return "mapgen.newfilter." + name().toLowerCase().replace("_", "");
		}

		private static String queryText() {
			StringBinding title = Lang.getBinding("mapgen.title.newfilter");
			StringBinding header = Lang.getBinding("mapgen.header.newfilter");
			TextInputDialog inputDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_EDIT));
			Optional<Boolean> inputResult = inputDialog.showAndWait();
			if (inputResult.isPresent() && inputResult.get()) {
				return inputDialog.getText();
			}
			return null;
		}
	}

	/**
	 * @param controller
	 * 		Controller to pull class information from.
	 *
	 * @return Generated mappings.
	 */
	private Mappings createMappings(Controller controller) {
		UxNameGenerator nameGen = generatorComboBox.getSelectionModel().getSelectedItem();
		NameGeneratorFilter filter = null;
		if (!filterList.getItems().isEmpty()) {
			filter = filterList.getItems().get(filterList.getItems().size() - 1).export();
		}
		// Run generator
		nameGen.reset();
		MappingGenerator generator = new MappingGenerator(controller);
		generator.setNameGenerator(nameGen);
		generator.setFilter(filter);
		return generator.generate();
	}

	/**
	 * @return List of name generators.
	 */
	private Collection<UxNameGenerator> createGenerators() {
		List<UxNameGenerator> list = new ArrayList<>();
		list.add(new IncrementingGenerator());
		return list;
	}

	/**
	 * Simple pattern to generate names based on an incrementing counter.
	 */
	private static class IncrementingGenerator implements UxNameGenerator {
		private int classId;
		private int fieldId;
		private int methodId;

		@Override
		public String mapClass(CommonClassInfo info) {
			return "generated/Class" + classId++;
		}

		@Override
		public String mapField(CommonClassInfo owner, FieldInfo info) {
			return "field" + fieldId++;
		}

		@Override
		public String mapMethod(CommonClassInfo owner, MethodInfo info) {
			return "method" + methodId++;
		}

		@Override
		public String asString() {
			return "Incrementing";
		}

		@Override
		public void reset() {
			classId = 0;
			fieldId = 0;
			methodId = 0;
		}
	}

	/**
	 * Name generator that can be {@link UxNameGenerator#asString() represented as a String}.
	 */
	private interface UxNameGenerator extends NameGenerator {
		/**
		 * @return String representation of the name generator.
		 */
		String asString();

		/**
		 * Reset internal state.
		 */
		void reset();
	}

	private static class ModifierIntermediate extends FilterIntermediate {
		private final List<AccessFlag> flags;
		private final boolean include;
		private final boolean targetClasses;
		private final boolean targetFields;
		private final boolean targetMethods;

		public ModifierIntermediate(List<FilterIntermediate> items, List<AccessFlag> flags, boolean include,
									boolean targetClasses, boolean targetFields, boolean targetMethods) {
			super(items);
			this.flags = flags;
			this.include = include;
			this.targetClasses = targetClasses;
			this.targetFields = targetFields;
			this.targetMethods = targetMethods;
		}

		@Override
		protected String asString() {
			String target = "?";
			if (targetClasses)
				target = "class";
			else if (targetFields)
				target = "field";
			else if (targetMethods)
				target = "method";
			String suffix = include ? "include" : "exclude";
			return Lang.get("mapgen.newfilter.modifier" + suffix) + "(" + target + "): " + AccessFlag.toString(flags);
		}

		@Override
		protected NameGeneratorFilter export() {
			return new NameGeneratorFilter(getPriorFilter()) {
				@Override
				public boolean shouldMapClass(CommonClassInfo info) {
					if (targetClasses && AccessFlag.hasAny(info.getAccess(), flags))
						return false;
					return super.shouldMapClass(info);
				}

				@Override
				public boolean shouldMapField(CommonClassInfo owner, FieldInfo info) {
					if (targetFields && AccessFlag.hasAny(info.getAccess(), flags))
						return false;
					if (targetClasses && AccessFlag.hasAny(owner.getAccess(), flags))
						return false;
					return super.shouldMapField(owner, info);
				}

				@Override
				public boolean shouldMapMethod(CommonClassInfo owner, MethodInfo info) {
					if (targetMethods && AccessFlag.hasAny(info.getAccess(), flags))
						return false;
					if (targetClasses && AccessFlag.hasAny(owner.getAccess(), flags))
						return false;
					return super.shouldMapMethod(owner, info);
				}
			};
		}
	}

	/**
	 * Excludes classes already mapped in the {@link AggregatedMappings} for the current workspace.
	 */
	private static class ExcludeExistingIntermediate extends FilterIntermediate {
		protected ExcludeExistingIntermediate(List<FilterIntermediate> items) {
			super(items);
		}

		@Override
		protected String asString() {
			return Lang.get("mapgen.newfilter.excludealreadymapped");
		}

		@Override
		protected NameGeneratorFilter export() {
			// TODO: Validate if the 'getMappedX(...)' handling is correct here
			MappingsManager mappingsManager = RecafUI.getController().getServices().getMappingsManager();
			AggregatedMappings aggregate = mappingsManager.getAggregatedMappings();
			return new NameGeneratorFilter(getPriorFilter()) {
				@Override
				public boolean shouldMapClass(CommonClassInfo info) {
					if (aggregate.getMappedClassName(info.getName()) != null)
						return false;
					return super.shouldMapClass(info);
				}

				@Override
				public boolean shouldMapField(CommonClassInfo owner, FieldInfo info) {
					if (aggregate.getMappedFieldName(owner.getName(), info.getName(), info.getDescriptor()) != null)
						return false;
					return super.shouldMapField(owner, info);
				}

				@Override
				public boolean shouldMapMethod(CommonClassInfo owner, MethodInfo info) {
					if (aggregate.getMappedMethodName(owner.getName(), info.getName(), info.getDescriptor()) != null)
						return false;
					return super.shouldMapMethod(owner, info);
				}
			};
		}
	}

	/**
	 * Excludes or includes naming anything declared in the given path ({@link #include}).
	 * Includes classes and any members defined in those classes.
	 * <br>
	 * Though, if something extends those classes and is in another package, then the name will propagate.
	 */
	private static class PathFilterIntermediate extends FilterIntermediate {
		private final String path;
		private final boolean include;

		private PathFilterIntermediate(List<FilterIntermediate> items, String path, boolean include) {
			super(items);
			this.path = path;
			this.include = include;
		}

		@Override
		public String asString() {
			return path;
		}

		@Override
		public NameGeneratorFilter export() {
			return new NameGeneratorFilter(getPriorFilter()) {
				@Override
				public boolean shouldMapClass(CommonClassInfo info) {
					String name = info.getName();
					boolean startsWith = name.startsWith(path);
					if (include && !startsWith)
						return false; // (inclusion) class name does not match whitelisted path
					else if (!include && startsWith)
						return false; // (exclusion) class name contains blacklisted path
					return super.shouldMapClass(info);
				}

				@Override
				public boolean shouldMapField(CommonClassInfo owner, FieldInfo info) {
					return super.shouldMapField(owner, info) && shouldMapClass(owner);
				}

				@Override
				public boolean shouldMapMethod(CommonClassInfo owner, MethodInfo info) {
					return super.shouldMapMethod(owner, info) && shouldMapClass(owner);
				}
			};
		}
	}

	/**
	 * Intermediate to be held by {@link #filterList} before being transformed into a {@link NameGeneratorFilter}.
	 */
	private static abstract class FilterIntermediate {
		private final List<FilterIntermediate> items;

		protected FilterIntermediate(List<FilterIntermediate> items) {
			this.items = items;
		}

		/**
		 * @return The prior name filter from the item before this one in the {@link #filterList}.
		 */
		protected NameGeneratorFilter getPriorFilter() {
			NameGeneratorFilter prior = null;
			int indexOfSelf = items.indexOf(this);
			if (indexOfSelf > 0) {
				FilterIntermediate priorIntermediate = items.get(indexOfSelf - 1);
				prior = priorIntermediate.export();
			}
			return prior;
		}

		/**
		 * @return String representation of the filter.
		 */
		protected abstract String asString();

		/**
		 * @return New filter from this intermediate.
		 */
		protected abstract NameGeneratorFilter export();

		@Override
		public String toString() {
			return asString();
		}
	}
}
