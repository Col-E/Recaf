package me.coley.recaf.config.container;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;
import me.coley.recaf.config.Ignore;
import me.coley.recaf.config.bounds.IntBounds;
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
	 * Left bound (minimum) for {@link #fontSize font size}.
	 */
	@Ignore
	public static final int FONT_SIZE_BOUND_LEFT = 8;

	/**
	 * Right bound (maximum) for {@link #fontSize font size}.
	 */
	@Ignore
	public static final int FONT_SIZE_BOUND_RIGHT = 20;

	/**
	 * Maximum depth of a directory structure to display before it gets truncated.
	 */
	@Group("general")
	@ConfigID("flashopentabs")
	public boolean flashOpentabs;

	/**
	 * Active translations.
	 */
	@Group("general")
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
	public IntegerProperty maxTreeTextLength = new SimpleIntegerProperty(100);

	/**
	 * Show the file filter buttons in workspace tree. Disabling frees up some space.
	 */
	@Group("workspace")
	@ConfigID("showfilterbuttons")
	public boolean showFilterButtons = true;

	/**
	 * Show the file filter buttons in workspace tree. Disabling frees up some space.
	 */
	@Group("workspace")
	@ConfigID("showselectionnavbar")
	public boolean showSelectionNavbar = true;

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

	/**
	 * Font size for the decompiler view.
	 */
	@IntBounds(min = FONT_SIZE_BOUND_LEFT, max = FONT_SIZE_BOUND_RIGHT)
	@Group("text")
	@ConfigID("fontsize")
	public IntegerProperty fontSize = new SimpleIntegerProperty(12);

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
