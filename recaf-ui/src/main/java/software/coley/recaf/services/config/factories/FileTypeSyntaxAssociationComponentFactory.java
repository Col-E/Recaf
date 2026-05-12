package software.coley.recaf.services.config.factories;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.services.config.KeyedConfigComponentFactory;
import software.coley.recaf.services.info.association.FileTypeSyntaxAssociationServiceConfig;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.richtext.syntax.RegexLanguages;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Factory for {@link FileTypeSyntaxAssociationServiceConfig#getExtensionsToLangKeys()}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class FileTypeSyntaxAssociationComponentFactory extends KeyedConfigComponentFactory<Map<String, String>> {
	private static final String VALUE_ID = "extensions-to-langs";
	private final List<String> languages;

	@Inject
	public FileTypeSyntaxAssociationComponentFactory(@Nonnull FileTypeSyntaxAssociationServiceConfig config) {
		super(true, id(config));

		// Get supported languages from the syntax highlighter and sort them for display.
		languages = RegexLanguages.getLanguages().keySet().stream()
				.sorted()
				.toList();
	}

	@Nonnull
	@Override
	public Node create(@Nonnull ConfigContainer container, @Nonnull ConfigValue<Map<String, String>> value) {
		if (!(container instanceof FileTypeSyntaxAssociationServiceConfig syntaxConfig))
			return new Label("Unsupported container: " + container.getClass().getName());

		FileTypeSyntaxAssociationServiceConfig.ExtensionMapping mapping = syntaxConfig.getExtensionsToLangKeys();
		ObservableList<ExtensionRow> rows = FXCollections.observableArrayList(toRows(mapping));

		TableView<ExtensionRow> table = new TableView<>(rows);
		table.setEditable(true);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

		TableColumn<ExtensionRow, String> extensionColumn = new TableColumn<>("Extension");
		extensionColumn.setEditable(true);
		extensionColumn.setSortable(false);
		extensionColumn.setCellValueFactory(param -> param.getValue().extensionProperty());
		extensionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		extensionColumn.setOnEditCommit(event -> {
			ExtensionRow row = event.getRowValue();
			String oldExtension = row.getExtension();
			String newExtension = normalizeExtension(event.getNewValue());
			if (!isValidExtension(rows, row, newExtension)) {
				table.refresh();
				return;
			}

			if (Objects.equals(oldExtension, newExtension))
				return;

			if (!oldExtension.isBlank())
				mapping.remove(oldExtension);
			row.setExtension(newExtension);
			if (!newExtension.isBlank())
				mapping.put(newExtension, row.getLanguage());
			FXCollections.sort(rows);
			table.refresh();
		});

		TableColumn<ExtensionRow, String> languageColumn = new TableColumn<>("Language");
		languageColumn.setEditable(true);
		languageColumn.setSortable(false);
		languageColumn.setCellValueFactory(param -> param.getValue().languageProperty());
		languageColumn.setCellFactory(_ -> new LanguageTableCell(mapping));

		TableColumn<ExtensionRow, ExtensionRow> actionColumn = new TableColumn<>("");
		actionColumn.setEditable(false);
		actionColumn.setSortable(false);
		actionColumn.setMaxWidth(50);
		actionColumn.setPrefWidth(50);
		actionColumn.setCellValueFactory(param -> javafx.beans.binding.Bindings.createObjectBinding(param::getValue));
		actionColumn.setCellFactory(_ -> new TableCell<>() {
			private final ActionButton removeButton = new ActionButton(CarbonIcons.TRASH_CAN, this::removeCurrentRow);

			{
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				setAlignment(Pos.CENTER);
			}

			private void removeCurrentRow() {
				ExtensionRow row = getItem();
				if (row == null)
					return;
				rows.remove(row);
				if (!row.getExtension().isBlank())
					mapping.remove(row.getExtension());
			}

			@Override
			protected void updateItem(ExtensionRow item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(empty || item == null ? null : removeButton);
			}
		});

		table.getColumns().add(extensionColumn);
		table.getColumns().add(languageColumn);
		table.getColumns().add(actionColumn);

		ActionButton addButton = new ActionButton(CarbonIcons.ADD_ALT, () -> {
			ExtensionRow row = new ExtensionRow("", defaultLanguage());
			rows.add(row);
			FXCollections.sort(rows);
			table.getSelectionModel().select(row);
			table.scrollTo(row);
		});

		VBox layout = new VBox(10, addButton, table);
		VBox.setVgrow(table, Priority.ALWAYS);

		mapping.addChangeListener((ob, old, cur) -> syncRows(rows, mapping));
		return layout;
	}

	@Nonnull
	private String defaultLanguage() {
		return languages.isEmpty() ? "" : languages.getFirst();
	}

	private static void syncRows(@Nonnull ObservableList<ExtensionRow> rows,
	                             @Nonnull Map<String, String> mapping) {
		List<ExtensionRow> blankRows = rows.stream()
				.filter(row -> row.getExtension().isBlank())
				.map(row -> new ExtensionRow("", row.getLanguage()))
				.toList();
		rows.setAll(toRows(mapping));
		rows.addAll(blankRows);
		FXCollections.sort(rows);
	}

	@Nonnull
	private static List<ExtensionRow> toRows(@Nonnull Map<String, String> mapping) {
		List<ExtensionRow> rows = new ArrayList<>(mapping.size());
		mapping.forEach((extension, language) -> rows.add(new ExtensionRow(extension, language)));
		rows.sort(ExtensionRow::compareTo);
		return rows;
	}

	@Nonnull
	private static String normalizeExtension(@Nonnull String extension) {
		String normalized = extension.trim().toLowerCase();
		while (normalized.startsWith("."))
			normalized = normalized.substring(1);
		return normalized;
	}

	private static boolean isValidExtension(@Nonnull ObservableList<ExtensionRow> rows,
	                                        @Nonnull ExtensionRow targetRow,
	                                        @Nonnull String extension) {
		if (extension.isBlank())
			return false;
		return rows.stream()
				.filter(row -> row != targetRow)
				.noneMatch(row -> extension.equals(row.getExtension()));
	}

	@Nonnull
	private static String id(@Nonnull ConfigContainer container) {
		return container.getGroupAndId() + ConfigGroups.PACKAGE_SPLIT + VALUE_ID;
	}

	/**
	 * Cell for extension-language pairs. Provides a dropdown of supported languages for selection.
	 */
	private class LanguageTableCell extends TableCell<ExtensionRow, String> {
		private final ComboBox<String> comboBox = new ComboBox<>();
		private boolean updating;

		private LanguageTableCell(@Nonnull FileTypeSyntaxAssociationServiceConfig.ExtensionMapping mapping) {
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			comboBox.setMaxWidth(Double.MAX_VALUE);
			comboBox.valueProperty().addListener((ob, old, cur) -> {
				if (updating || cur == null)
					return;

				ExtensionRow row = getTableRow() == null ? null : getTableRow().getItem();
				if (row == null || Objects.equals(cur, row.getLanguage()))
					return;

				row.setLanguage(cur);
				if (!row.getExtension().isBlank())
					mapping.put(row.getExtension(), cur);
			});
		}

		@Override
		protected void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);

			if (empty) {
				setGraphic(null);
				return;
			}

			ExtensionRow row = getTableRow() == null ? null : getTableRow().getItem();
			updating = true;
			comboBox.getItems().setAll(languageChoices(item));
			comboBox.setValue(item);
			updating = false;
			setGraphic(row == null ? null : comboBox);
		}

		@Nonnull
		private List<String> languageChoices(String currentValue) {
			TreeSet<String> items = new TreeSet<>(languages);
			if (currentValue != null && !currentValue.isBlank())
				items.add(currentValue);
			return new ArrayList<>(items);
		}
	}

	/**
	 * Row model for extension-language pairs.
	 */
	private static class ExtensionRow implements Comparable<ExtensionRow> {
		private final StringProperty extension = new SimpleStringProperty();
		private final StringProperty language = new SimpleStringProperty();

		private ExtensionRow(@Nonnull String extension, @Nonnull String language) {
			setExtension(extension);
			setLanguage(language);
		}

		@Nonnull
		private StringProperty extensionProperty() {
			return extension;
		}

		@Nonnull
		private StringProperty languageProperty() {
			return language;
		}

		@Nonnull
		private String getExtension() {
			return Objects.requireNonNullElse(extension.get(), "");
		}

		private void setExtension(@Nonnull String value) {
			extension.set(value);
		}

		@Nonnull
		private String getLanguage() {
			return Objects.requireNonNullElse(language.get(), "");
		}

		private void setLanguage(@Nonnull String value) {
			language.set(value);
		}

		@Override
		public int compareTo(@Nonnull ExtensionRow other) {
			Comparator<String> blankAware = Comparator.comparing(String::isBlank)
					.thenComparing(Comparator.naturalOrder());
			return blankAware.compare(getExtension(), other.getExtension());
		}
	}
}
