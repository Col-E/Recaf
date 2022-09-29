package me.coley.recaf.ui;

import me.coley.recaf.RecafUI;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.mapping.Mappings;
import me.coley.recaf.mapping.data.ClassMapping;
import me.coley.recaf.mapping.format.IntermediateMappings;
import me.coley.recaf.ui.behavior.ScrollSnapshot;
import me.coley.recaf.ui.behavior.Scrollable;
import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.ui.docking.RecafDockingManager;
import me.coley.recaf.ui.docking.impl.ClassTab;
import me.coley.recaf.util.TextDisplayUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.workspace.Workspace;
import org.slf4j.Logger;

import java.util.ArrayList;
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
	private static final Logger logger = Logging.get(MappingUX.class);

	/**
	 * @param openedClassTabs
	 * 		Snapshot of prior open class tabs.
	 * @param mappings
	 * 		Mappings applied.
	 */
	public static void handleClassRemapping(List<ClassTab> openedClassTabs, Mappings mappings) {
		// Skip if mapping doesn't support intermediate representation
		if (!mappings.supportsExportIntermediate())
			return;
		IntermediateMappings intermediate = mappings.exportIntermediate();
		Map<String, ClassMapping> mappedClasses = intermediate.getClasses();
		// Skip if no classes were mapped.
		if (mappedClasses.isEmpty())
			return;
		// Re-open the mapped classes as their new name, attempt to reset scroll position and such.
		Map<String, DockTab> classToOpenedTab = openedClassTabs.stream()
				.collect(Collectors.toMap(
						tab -> {
							// TODO: Sometimes the content/info is null
							//  - I think its a thread fighting issue
							ClassView view = (ClassView) tab.getContent();
							if (view == null) {
								logger.error("Cannot restore tab[{}], view was null", tab.getText());
								return null;
							}
							CommonClassInfo info = view.getCurrentClassInfo();
							if (info == null) {
								logger.error("Cannot restore tab[{}], view was null", tab.getText());
								return null;
							}
							return info.getName();
						},
						Function.identity())
				);
		mappedClasses.forEach((oldName, classMapping) -> {
			DockTab tab = classToOpenedTab.get(oldName);
			if (tab == null)
				return;
			// Check if the class got renamed. If the name is the same, the tabs should automatically get refreshed.
			// But if the name is different, we'll need to re-open the content.
			String newName = classMapping.getNewName();
			if (!oldName.equals(newName)) {
				// Get old content of tab
				ClassView oldView = (ClassView) tab.getContent();
				ScrollSnapshot scrollSnapshot = null;
				if (oldView.getMainView() instanceof Scrollable) {
					scrollSnapshot = ((Scrollable) oldView.getMainView()).makeScrollSnapshot();
				}
				tab.setContent(null);
				tab.close();
				// Open it in a new tab and update it
				Workspace workspace = RecafUI.getController().getWorkspace();
				CommonClassInfo newClassInfo = workspace.getResources().getClass(newName);
				RecafDockingManager docking = RecafDockingManager.getInstance();
				String title = TextDisplayUtil.shortenEscapeLimit(newName);
				ClassTab newTab = (ClassTab) docking.createTab(() -> new ClassTab(title, oldView));
				newTab.select();
				oldView.refreshView();
				oldView.onUpdate(newClassInfo);
				if (scrollSnapshot != null)
					FxThreadUtil.delayedRun(100, scrollSnapshot::restore);
				// Update tab cache to point to updated tab instance
				Map<String, ClassTab> classTabs = RecafDockingManager.getInstance().getClassTabs();
				classTabs.remove(oldName);
				classTabs.put(newName, newTab);
			}
		});
	}

	/**
	 * @return Snapshot of the UI's tabs representing mappable classes.
	 */
	public static List<ClassTab> snapshotTabState() {
		return new ArrayList<>(RecafDockingManager.getInstance().getClassTabs().values());
	}
}
