package me.coley.recaf.config.container;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;
import me.coley.recaf.config.bounds.IntBounds;
import me.coley.recaf.ui.ClassViewMode;
import me.coley.recaf.ui.DiffViewMode;
import me.coley.recaf.ui.FileViewMode;
import me.coley.recaf.ui.pane.outline.MemberType;
import me.coley.recaf.ui.pane.outline.OutlinePane;
import me.coley.recaf.ui.pane.outline.Visibility;
import me.coley.recaf.ui.util.Icons;

import java.util.Map;
import java.util.TreeMap;

/**
 * Config container for editor values.
 *
 * @author Matt Coley
 */
public class EditorConfig implements ConfigContainer {
	/**
	 * Determines {@link me.coley.recaf.ui.behavior.ClassRepresentation} for {@link me.coley.recaf.ui.ClassView}.
	 */
	@Group("general")
	@ConfigID("classmode")
	public ClassViewMode defaultClassMode = ClassViewMode.DECOMPILE;

	/**
	 * Determines {@link me.coley.recaf.ui.behavior.FileRepresentation} for {@link me.coley.recaf.ui.FileView}.
	 */
	@Group("general")
	@ConfigID("filemode")
	public FileViewMode defaultFileView = FileViewMode.AUTO;

	/**
	 * Position of {@link me.coley.recaf.ui.control.ErrorDisplay} when problems occur.
	 */
	@Group("general")
	@ConfigID("errorindicatorpos")
	public Pos errorIndicatorPos = Pos.TOP_RIGHT;

	/**
	 * Associations between file extensions and {@link me.coley.recaf.ui.control.code.Language languages}.
	 */
	@Group("assoc")
	@ConfigID("fileextassociations")
	public Map<String, String> fileExtensionAssociations = new TreeMap<>();

	/**
	 * Show bracket folds in {@link me.coley.recaf.ui.control.code.SyntaxArea}.
	 */
	@Group("text")
	@ConfigID("showbracketfolds")
	public boolean showBracketFolds = true;

	/**
	 * Show types of fields and methods in the {@link OutlinePane}.
	 */
	@Group("outline")
	@ConfigID("showoutlinedtypes")
	public BooleanProperty showOutlinedTypes = new SimpleBooleanProperty();

	/**
	 * Show synthetic fields and methods in the {@link OutlinePane}.
	 */
	@Group("outline")
	@ConfigID("showoutlinedsynths")
	public BooleanProperty showOutlinedSynthetics = new SimpleBooleanProperty();

	/**
	 * Filter out members by type (method/field).
	 */
	@Group("outline")
	@ConfigID("showoutlinedmembertype")
	public ObjectProperty<MemberType> showOutlinedMemberType = new SimpleObjectProperty<>(MemberType.ALL);

	/**
	 * Filter out members by visibility (public/protected/package/private). Showing at which state something can be accessed
	 * Ex.: with protected selected, only protected and public members are shown.
	 */
	@Group("outline")
	@ConfigID("showoutlinedvisibility")
	public ObjectProperty<Visibility> showOutlinedVisibility = new SimpleObjectProperty<>(Visibility.ALL);

	/**
	 * Position of the icon for items in the outline.
	 */
	@Group("outline")
	@ConfigID("outlinedvisibilityiconposition")
	public ObjectProperty<Visibility.IconPosition> outlineVisibilityIconPosition = new SimpleObjectProperty<>(Visibility.IconPosition.RIGHT);

	@Group("outline")
	@ConfigID("sortalphabetically")
	public BooleanProperty sortOutlinedAlphabetically = new SimpleBooleanProperty();

	@Group("outline")
	@ConfigID("sortbyvisibility")
	public BooleanProperty sortOutlinedByVisibility = new SimpleBooleanProperty();

	@Group("outline")
	@ConfigID("filter.casesensitive")
	public BooleanProperty caseSensitiveOutlinedFilter = new SimpleBooleanProperty();
	/**
	 * Highlight the current hovered item in a {@link me.coley.recaf.ui.control.hex.HexView}.
	 */
	@Group("hex")
	@ConfigID("highlightcurrent")
	public boolean highlightCurrent = true;

	/**
	 * Highlight the current hovered item in a {@link me.coley.recaf.ui.control.hex.HexView}.
	 */
	@IntBounds(min = 8, max = 32)
	@Group("hex")
	@ConfigID("hexcolumns")
	public int hexColumns = 16;

	/**
	 * Show class format hints in the {@link me.coley.recaf.ui.control.hex.clazz.HexClassInfo} display.
	 */
	@Group("hex")
	@ConfigID("classhints")
	public boolean showClassHints = true;

	/**
	 * Diff-viewer class representation.
	 */
	@Group("diff")
	@ConfigID("diff-view-mode")
	public DiffViewMode diffViewMode = DiffViewMode.DECOMPILE;

	@Override
	public String iconPath() {
		return Icons.ACTION_EDIT;
	}

	@Override
	public String internalName() {
		return "conf.editor";
	}
}
