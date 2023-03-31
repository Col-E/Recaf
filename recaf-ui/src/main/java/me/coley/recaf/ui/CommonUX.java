package me.coley.recaf.ui;

import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.docking.DockingRegion;
import me.coley.recaf.ui.docking.RecafDockingManager;
import me.coley.recaf.ui.docking.impl.ClassTab;
import me.coley.recaf.ui.docking.impl.FileTab;
import me.coley.recaf.ui.util.Animations;
import me.coley.recaf.util.TextDisplayUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

/**
 * Utility calls for common UX behavior such as opening a class or file.
 *
 * @author Matt Coley
 */
public class CommonUX {
	private static final Logger logger = Logging.get(CommonUX.class);

	/**
	 * Open the class info type.
	 *
	 * @param info
	 * 		Class info.
	 *
	 * @return Tab containing the opened class representation.
	 */
	public static ClassTab openClass(CommonClassInfo info) {
		RecafDockingManager docking = RecafDockingManager.getInstance();
		ClassTab tab = docking.getClassTabs().get(info.getName());
		if (tab != null) {
			// Show little flash to bring attention to the open item
			if (Configs.display().flashOpentabs)
				Animations.animateNotice(tab.getContent(), 1000);
		} else {
			// Create the tab
			String title = TextDisplayUtil.shortenEscapeLimit(info.getName());
			tab = (ClassTab) RecafDockingManager.getInstance()
					.createTab(CommonUX::anyRegion, CommonUX::byPopulatedClasses,
							() -> new ClassTab(title, new ClassView(info)));
		}
		tab.select();
		bringToFront(tab);
		return tab;
	}


	/**
	 * @param owner
	 * 		Class info that defined the member.
	 * @param info
	 * 		Member info to open.
	 *
	 * @return Tab containing the opened class representation.
	 */
	public static ClassTab openMember(CommonClassInfo owner, MemberInfo info) {
		ClassTab tab = openClass(owner);
		if (tab.getContent() instanceof ClassRepresentation) {
			ClassRepresentation representation = (ClassRepresentation) tab.getContent();
			if (representation.supportsMemberSelection()) {
				representation.selectMember(info);
			} else {
				logger.warn("The current view for class '{}' does not support member selection!", owner.getName());
			}
		}
		bringToFront(tab);
		return tab;
	}

	/**
	 * Open the file info type.
	 *
	 * @param info
	 * 		File info.
	 *
	 * @return Tab containing the opened class representation.
	 */
	public static FileTab openFile(FileInfo info) {
		RecafDockingManager docking = RecafDockingManager.getInstance();
		FileTab tab = docking.getFileTabs().get(info.getName());
		if (tab != null) {
			// Show little flash to bring attention to the open item
			if (Configs.display().flashOpentabs)
				Animations.animateNotice(tab.getContent(), 1000);
		} else {
			// Create the tab
			String title = TextDisplayUtil.shortenEscapeLimit(info.getName());
			tab = (FileTab) RecafDockingManager.getInstance()
					.createTab(CommonUX::anyRegion, CommonUX::byPopulatedFiles,
							() -> new FileTab(title, new FileView(info)));
		}
		tab.select();
		bringToFront(tab);
		return tab;
	}

	/**
	 * Used as {@link me.coley.recaf.ui.docking.RegionFilter}.
	 *
	 * @param region
	 * 		Region input.
	 *
	 * @return Always {@code true}.
	 */
	public static boolean anyRegion(DockingRegion region) {
		return true;
	}

	/**
	 * Used as {@link me.coley.recaf.ui.docking.RegionPreference}.
	 *
	 * @param r1
	 * 		One region.
	 * @param r2
	 * 		Another region.
	 *
	 * @return Preference of the region with more {@link ClassTab} items.
	 */
	public static int byPopulatedClasses(DockingRegion r1, DockingRegion r2) {
		return -Long.compare(
				r1.getDockTabs().stream().filter(t -> t instanceof ClassTab).count(),
				r2.getDockTabs().stream().filter(t -> t instanceof ClassTab).count());
	}

	/**
	 * Used as {@link me.coley.recaf.ui.docking.RegionPreference}.
	 *
	 * @param r1
	 * 		One region.
	 * @param r2
	 * 		Another region.
	 *
	 * @return Preference of the region with more {@link FileTab} items.
	 */
	public static int byPopulatedFiles(DockingRegion r1, DockingRegion r2) {
		return -Long.compare(
				r1.getDockTabs().stream().filter(t -> t instanceof FileTab).count(),
				r2.getDockTabs().stream().filter(t -> t instanceof FileTab).count());
	}

	private static void bringToFront(Tab tab) {
		TabPane tabPane = tab.getTabPane();
		if (tabPane == null) return;
		Scene scene = tabPane.getScene();
		if (scene == null) return;
		scene.getWindow().requestFocus();
		tab.getContent().requestFocus();
	}
}
