package me.coley.recaf.ui.menu.component;

import jakarta.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Menu;
import me.coley.recaf.cdi.RecafContainer;
import me.coley.recaf.graph.InheritanceGraph;
import me.coley.recaf.mapping.*;
import me.coley.recaf.ui.menu.MainMenu;
import me.coley.recaf.ui.pane.MappingGenPane;
import me.coley.recaf.ui.prompt.WorkspaceIOPrompts;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.WorkspaceManager;
import me.coley.recaf.workspace.resource.Resource;
import org.slf4j.Logger;

public class MappingsMenuComponent extends MenuComponent {
	private static final Logger logger = Logging.get(MappingsMenuComponent.class);
	private final WorkspaceManager workspaceManager;
	private final MappingsManager mappingsManager;

	@Inject
	public MappingsMenuComponent(WorkspaceManager workspaceManager,
								 MappingsManager mappingsManager) {
		this.workspaceManager = workspaceManager;
		this.mappingsManager = mappingsManager;
	}

	@Override
	protected Menu create(MainMenu mainMenu) {
		Menu menuMappings = menu("menu.mappings", Icons.DOCUMENTATION);
		menuMappings.disableProperty().bind(
				mainMenu.noWorkspaceProperty()
						.or(mainMenu.remappingProperty())
						.or(mainMenu.agentWorkspaceProperty()));
		Menu menuApply = menu("menu.mappings.apply");
		Menu menuExport = menu("menu.mappings.export");
		menuMappings.getItems().addAll(menuApply, menuExport);
		for (MappingsTool mappingsTool : mappingsManager.getRegisteredImpls()) {
			String name = mappingsTool.getName();
			menuApply.getItems().add(actionLiteral(name, null, () -> openMappings(mainMenu.remappingProperty(), mappingsTool)));
			if (mappingsTool.supportsTextExport())
				menuExport.getItems().add(actionLiteral(name, null, () -> exportMappings(mappingsTool)));
		}
		menuMappings.getItems().add(action("menu.mappings.view", Icons.EYE, this::openMappingViewer));
		menuMappings.getItems().add(action("menu.mappings.generate", Icons.CONFIG, this::openMappingGenerator));
		return menuMappings;
	}


	private void openMappings(BooleanProperty remapping, MappingsTool mappingsTool) {
		String mappingsText = WorkspaceIOPrompts.promptMappingInput();
		if (mappingsText == null) {
			return;
		}
		remapping.set(true);
		try {
			Mappings mappings = mappingsTool.create();
			mappings.parse(mappingsText);
			Resource resource = workspaceManager.getCurrent().getResources().getPrimary();
			InheritanceGraph graph = RecafContainer.get(InheritanceGraph.class);
			AggregateMappingManager aggregate = RecafContainer.get(AggregateMappingManager.class);
			MappingUtils.applyMappings(resource, mappings, graph, aggregate);
		} finally {
			remapping.set(false);
		}
	}

	private void exportMappings(MappingsTool mappingsTool) {
		AggregateMappingManager aggregateMappingManager = RecafContainer.get(AggregateMappingManager.class);
		Mappings currentAggregate = aggregateMappingManager.getAggregatedMappings();
		if (!currentAggregate.supportsExportIntermediate()) {
			logger.error("Cannot export aggregated mappings, intermediate export not supported!");
			return;
		}
		Mappings targetMappings = mappingsTool.create();
		targetMappings.importIntermediate(currentAggregate.exportIntermediate());
		WorkspaceIOPrompts.promptMappingExport(targetMappings);
	}

	private void openMappingViewer() {
		GenericWindow window = windows.getMappingViewWindow();
		window.titleProperty().bind(Lang.getBinding("menu.mappings.view"));
		window.show();
	}

	private void openMappingGenerator() {
		GenericWindow window = new GenericWindow(new MappingGenPane());
		window.titleProperty().bind(Lang.getBinding("menu.mappings.generate"));
		window.show();
	}
}
