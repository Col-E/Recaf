package me.coley.recaf.ui;

import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.ui.panel.DockingRootPane;

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
	 */
	public static void openClass(CommonClassInfo info) {
		if (info instanceof ClassInfo) {
			openClass((ClassInfo) info);
		} else {
			openDexClass((DexClassInfo) info);
		}
	}

	/**
	 * Open the class info type.
	 *
	 * @param info
	 * 		Class info.
	 */
	public static void openClass(ClassInfo info) {
		DockingRootPane docking = RecafUI.getWindows().getMainWindow().getDockingRootPane();
		docking.openInfoTab(info, () -> new ClassView(info));
	}

	/**
	 * Open the android dex class info type.
	 *
	 * @param info
	 * 		Android dex class info.
	 */
	public static void openDexClass(DexClassInfo info) {
		DockingRootPane docking = RecafUI.getWindows().getMainWindow().getDockingRootPane();
		docking.openInfoTab(info, () -> new ClassView(info));
	}

	/**
	 * Open the file info type.
	 *
	 * @param info
	 * 		File info.
	 */
	public static void openFile(FileInfo info) {
	}
}
