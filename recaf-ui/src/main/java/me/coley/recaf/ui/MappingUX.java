package me.coley.recaf.ui;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.mapping.Mappings;
import me.coley.recaf.mapping.data.ClassMapping;
import me.coley.recaf.mapping.impl.IntermediateMappings;
import me.coley.recaf.ui.behavior.ScrollSnapshot;
import me.coley.recaf.ui.behavior.Scrollable;
import me.coley.recaf.ui.pane.DockingRootPane;
import me.coley.recaf.util.Threads;
import me.coley.recaf.workspace.Workspace;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility calls for mapping UX behavior such as re-opening classes that have been renamed in mapping operations.
 *
 * @author Matt Coley
 */
public class MappingUX {
	/**
	 * @param snapshot
	 * 		Old snapshot.
	 * @param mappings
	 * 		Mappings applied.
	 */
	private static void handleClassRemapping(Snapshot snapshot, Mappings mappings) {
		List<Tab> openedClassTabs = snapshot.openedClassTabs;
		// Skip if mapping doesn't support intermediate representation
		if (!mappings.supportsExportIntermediate())
			return;
		IntermediateMappings intermediate = mappings.exportIntermediate();
		Map<String, ClassMapping> mappedClasses = intermediate.getClasses();
		// Skip if no classes were mapped.
		if (mappedClasses.isEmpty())
			return;
		// Re-open the mapped classes as their new name, attempt to reset scroll position and such.
		Map<String, Tab> classToOpenedTab = openedClassTabs.stream()
				.collect(Collectors.toMap(
						tab -> ((ClassView) tab.getContent()).getCurrentClassInfo().getName(),
						Function.identity())
				);
		mappedClasses.forEach((oldName, classMapping) -> {
			Tab tab = classToOpenedTab.get(oldName);
			if (tab == null)
				return;
			// Get old content of tab
			ClassView oldView = (ClassView) tab.getContent();
			ScrollSnapshot scrollSnapshot = null;
			if (oldView.getMainView() instanceof Scrollable) {
				scrollSnapshot = ((Scrollable) oldView.getMainView()).makeScrollSnapshot();
			}
			tab.setContent(null);
			// Open it in a new tab
			Workspace workspace = RecafUI.getController().getWorkspace();
			CommonClassInfo newClassInfo = workspace.getResources().getClass(classMapping.getNewName());
			DockingRootPane docking = RecafUI.getWindows().getMainWindow().getDockingRootPane();
			docking.openInfoTab(newClassInfo, () -> oldView);
			oldView.onUpdate(newClassInfo);
			if (scrollSnapshot != null) {
				Threads.runFxDelayed(100, scrollSnapshot::restore);
			}
		});
	}

	/**
	 * @return Snapshot of the UI's tabs representing mappable classes.
	 */
	public static Snapshot snapshotTabState() {
		List<Tab> openedClassTabs = RecafUI.getWindows().getMainWindow().getDockingRootPane().getAllTabs().stream()
				.filter(tab -> tab.getContent() instanceof ClassView)
				.collect(Collectors.toList());
		Map<Tab, TabPane> tabToContainer = openedClassTabs.stream()
				.collect(Collectors.toMap(Function.identity(), Tab::getTabPane));
		return new Snapshot(openedClassTabs, tabToContainer);
	}

	/**
	 * UI state snapshot.
	 */
	public static class Snapshot {
		private final List<Tab> openedClassTabs;
		private final Map<Tab, TabPane> tabToContainer;

		/**
		 * @param openedClassTabs
		 * 		Tabs that were open prior to the mapping application.
		 * 		These all have {@link ClassView} as their content.
		 * @param tabToContainer
		 * 		Mapping of tabs to their containers.
		 */
		public Snapshot(List<Tab> openedClassTabs, Map<Tab, TabPane> tabToContainer) {
			this.openedClassTabs = openedClassTabs;
			this.tabToContainer = tabToContainer;
		}

		/**
		 * Re-open any closed tabs from a mapping operation.
		 *
		 * @param mappings
		 * 		Mappings to use in state restoration.
		 */
		public void restoreState(Mappings mappings) {
			handleClassRemapping(this, mappings);
		}
	}
}
