package me.coley.recaf.ui;

import javafx.scene.control.Tab;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.*;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.pane.DockingRootPane;

/**
 * Utility calls for common UX behavior such as opening a class or file.
 *
 * @author Matt Coley
 */
public class CommonUX {
	/**
	 * Open the generic class info type.
	 *
	 * @param info
	 * 		Generic class info.
	 *
	 * @return Tab containing the opened class representation.
	 */
	public static Tab openClass(CommonClassInfo info) {
		if (info instanceof ClassInfo) {
			return openClass((ClassInfo) info);
		} else {
			return openDexClass((DexClassInfo) info);
		}
	}

	/**
	 * Open the class info type.
	 *
	 * @param info
	 * 		Class info.
	 *
	 * @return Tab containing the opened class representation.
	 */
	public static Tab openClass(ClassInfo info) {
		DockingRootPane docking = RecafUI.getWindows().getMainWindow().getDockingRootPane();
		return docking.openInfoTab(info, () -> new ClassView(info));
	}

	/**
	 * Open the android dex class info type.
	 *
	 * @param info
	 * 		Android dex class info.
	 *
	 * @return Tab containing the opened class representation.
	 */
	public static Tab openDexClass(DexClassInfo info) {
		DockingRootPane docking = RecafUI.getWindows().getMainWindow().getDockingRootPane();
		return docking.openInfoTab(info, () -> new ClassView(info));
	}

	/**
	 * @param owner
	 * 		Class info that defined the member.
	 * @param info
	 * 		Member info to open.
	 *
	 * @return Tab containing the opened class representation.
	 */
	public static Tab openMember(CommonClassInfo owner, MemberInfo info) {
		Tab tab = openClass(owner);
		if (tab.getContent() instanceof ClassRepresentation) {
			ClassRepresentation representation = (ClassRepresentation) tab.getContent();
			representation.selectMember(info);
		}
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
	public static Tab openFile(FileInfo info) {
		DockingRootPane docking = RecafUI.getWindows().getMainWindow().getDockingRootPane();
		return docking.openInfoTab(info, () -> new FileView(info));
	}
}
