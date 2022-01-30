package me.coley.recaf.config.container;

import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;
import me.coley.recaf.config.IntBounds;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.Translatable;

/**
 * Config container for display values.
 *
 * @author Matt Coley
 */
public class DisplayConfig implements ConfigContainer {
	/**
	 * Maximum depth of a directory structure to display before it gets truncated.
	 */
	@Group("base")
	@ConfigID("flashopentabs")
	public boolean flashOpentabs;

	/**
	 * Active language.
	 */
	@Group("base")
	@ConfigID("language")
	public String lang = Lang.getSystemLanguage();

	/**
	 * Maximum depth of a directory structure to display before it gets truncated.
	 */
	@IntBounds(min = 3, max = 100)
	@Group("tree")
	@ConfigID("maxtreedirectorydepth")
	public int maxTreeDirectoryDepth = 35;

	/**
	 * Maximum length of a tree item's text before it gets truncated.
	 */
	@IntBounds(min = 50, max = 500)
	@Group("tree")
	@ConfigID("maxtreetextlength")
	public int maxTreeTextLength = 100;

	/**
	 * Show the file filter buttons in workspace tree. Disabling frees up some space.
	 */
	@Group("workspace")
	@ConfigID("showfilterbuttons")
	public boolean showFilterButtons = true;

	/**
	 * Behavior to use when dropping a file into the workspace tree.
	 */
	@Group("workspace")
	@ConfigID("onfiledrop")
	public WorkspaceAction onFileDrop = WorkspaceAction.CHOOSE;

	/**
	 * Prompt the user to double check if they want to close the workspace.
	 */
	@Group("workspace")
	@ConfigID("promptcloseworkspace")
	public boolean promptCloseWorkspace = true;

	/**
	 * Prompt the user to double check if they want to delete something from the workspace.
	 */
	@Group("workspace")
	@ConfigID("promptdeleteitem")
	public boolean promptDeleteItem = true;

	@Override
	public String iconPath() {
		return Icons.EYE;
	}

	@Override
	public String internalName() {
		return "conf.display";
	}

	/**
	 * Drop behavior.
	 */
	public enum WorkspaceAction implements Translatable {
		CHOOSE, CREATE_NEW, ADD_LIBRARY;

		@Override
		public String getTranslationKey() {
			switch (this) {
				default:
				case CHOOSE:
					return "conf.display.workspace.onfiledrop.choose";
				case CREATE_NEW:
					return "conf.display.workspace.onfiledrop.createnew";
				case ADD_LIBRARY:
					return "conf.display.workspace.onfiledrop.addlibrary";
			}
		}

		@Override
		public String toString() {
			return Lang.get(getTranslationKey());
		}
	}
}
