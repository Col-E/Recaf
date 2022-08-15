package me.coley.recaf.ui.pane;

import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;
import me.coley.recaf.RecafUI;
import me.coley.recaf.mapping.*;
import me.coley.recaf.plugin.tools.Tool;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Displays contents of {@link me.coley.recaf.mapping.AggregatedMappings}.
 *
 * @author Matt Coley
 */
public class AggregateMappingPane extends BorderPane implements AggregatedMappingsListener {
	private static AggregateMappingPane instance;
	private final TextArea text = new TextArea();
	private final ComboBox<MappingsTool> combo = new ComboBox<>();
	private AggregatedMappings lastMappings;

	private AggregateMappingPane() {
		// TODO: More integrated display with class and member icons?
		//  - Text export already a feature from the menu dropdown, which this is basically a live preview of atm
		MappingsManager manager = RecafUI.getController().getServices().getMappingsManager();
		manager.addAggregatedMappingsListener(this);
		text.getStyleClass().add("monospaced");
		combo.getItems().addAll(manager.getRegisteredImpls().stream()
				.filter(MappingsTool::supportsTextExport)
				.collect(Collectors.toList()));
		combo.getItems().sort(Comparator.comparing(Tool::getName));
		combo.getSelectionModel().select(0);
		combo.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> onAggregatedMappingsUpdated(lastMappings));
		combo.setConverter(new StringConverter<>() {
			@Override
			public String toString(MappingsTool tool) {
				return tool.getName();
			}

			@Override
			public MappingsTool fromString(String name) {
				return manager.get(name);
			}
		});
		// Layout
		setPadding(new Insets(10));
		setCenter(text);
		setBottom(combo);
		// Setup initial state
		onAggregatedMappingsUpdated(manager.getAggregatedMappings());
	}

	@Override
	public void onAggregatedMappingsUpdated(AggregatedMappings mappings) {
		lastMappings = mappings;
		MappingsTool tool = combo.getSelectionModel().getSelectedItem();
		Mappings output = tool.create();
		output.importIntermediate(mappings.exportIntermediate());
		text.setText(output.exportText());
	}

	/**
	 * @return Aggregate mapping viewer pane instance.
	 */
	public static AggregateMappingPane get() {
		if (instance == null)
			instance = new AggregateMappingPane();
		return instance;
	}
}
