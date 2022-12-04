package me.coley.recaf.ui.pane;

import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;
import me.coley.recaf.cdi.WorkspaceScoped;
import me.coley.recaf.mapping.*;
import me.coley.recaf.plugin.tools.Tool;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.WorkspaceCloseListener;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Displays contents of {@link me.coley.recaf.mapping.AggregatedMappings}.
 *
 * @author Matt Coley
 */
@WorkspaceScoped
public class AggregateMappingPane extends BorderPane implements AggregatedMappingsListener, WorkspaceCloseListener {
	private final TextArea text = new TextArea();
	private final ComboBox<MappingsTool> combo = new ComboBox<>();
	private AggregatedMappings lastMappings;

	@Inject
	public AggregateMappingPane(AggregateMappingManager aggregateMappingManager, MappingsManager mappingsManager) {
		// TODO: More integrated display with class and member icons?
		//  - Text export already a feature from the menu dropdown, which this is basically a live preview of atm
		aggregateMappingManager.addAggregatedMappingsListener(this);
		text.getStyleClass().add("monospaced");
		combo.getItems().addAll(mappingsManager.getRegisteredImpls().stream()
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
				return mappingsManager.get(name);
			}
		});
		// Layout
		setPadding(new Insets(10));
		setCenter(text);
		setBottom(combo);
		// Setup initial state
		onAggregatedMappingsUpdated(aggregateMappingManager.getAggregatedMappings());
	}

	@Override
	public void onAggregatedMappingsUpdated(AggregatedMappings mappings) {
		lastMappings = mappings;
		MappingsTool tool = combo.getSelectionModel().getSelectedItem();
		Mappings output = tool.create();
		output.importIntermediate(mappings.exportIntermediate());
		text.setText(output.exportText());
	}

	@Override
	public void onWorkspaceClosed(Workspace workspace) {
		// Workspace closed
		text.setDisable(true);
	}
}
