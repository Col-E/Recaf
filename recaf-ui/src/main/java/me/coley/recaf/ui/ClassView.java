package me.coley.recaf.ui;

import javafx.beans.property.IntegerProperty;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.*;
import me.coley.recaf.ui.control.CollapsibleTabPane;
import me.coley.recaf.ui.control.hex.HexClassView;
import me.coley.recaf.ui.pane.DecompilePane;
import me.coley.recaf.ui.pane.HierarchyPane;
import me.coley.recaf.ui.pane.outline.OutlinePane;
import me.coley.recaf.ui.pane.SmaliAssemblerPane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

import java.util.function.Consumer;

/**
 * Display for a {@link CommonClassInfo}.
 *
 * @author Matt Coley
 */
public class ClassView extends BorderPane implements ClassRepresentation, ToolSideTabbed, Cleanable, Undoable, FontSizeChangeable {
	private final OutlinePane outline;
	private final HierarchyPane hierarchy;
	private final BorderPane mainViewWrapper = new BorderPane();
	private final CollapsibleTabPane sideTabs = new CollapsibleTabPane();
	private final SplitPane contentSplit = new SplitPane();
	private ClassViewMode mode = Configs.editor().defaultClassMode;
	private ClassRepresentation mainView;
	private CommonClassInfo info;

	/**
	 * @param info
	 * 		Initial state of the class to display.
	 */
	public ClassView(CommonClassInfo info) {
		this.info = info;
		outline = new OutlinePane(this);
		hierarchy = new HierarchyPane();
		// Setup main view
		mainView = createViewForClass(info);
		mainViewWrapper.setCenter(mainView.getNodeRepresentation());
		contentSplit.getItems().add(mainViewWrapper);
		contentSplit.getStyleClass().add("view-split-pane");
		// Setup side tabs with class visualization tools
		sideTabs.setSide(Side.RIGHT);
		sideTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		populateSideTabs(sideTabs);
		installSideTabs(sideTabs);
		setCenter(contentSplit);
		onUpdate(info);
		Configs.keybinds().installEditorKeys(this);
	}

	private void applyEventsForFontSizeChange(ClassRepresentation view) {
		if (!(view instanceof FontSizeChangeable)) return;
		FontSizeChangeable fsc = (FontSizeChangeable) view;
		fsc.bindFontSize(Configs.display().fontSize);
		fsc.applyEventsForFontSizeChange(FontSizeChangeable.DEFAULT_APPLIER);
	}

	@Override
	public void bindFontSize(IntegerProperty property) {
		if (!(mainView instanceof FontSizeChangeable)) return;
		FontSizeChangeable fsc = (FontSizeChangeable) mainView;
		fsc.bindFontSize(property);
	}

	@Override
	public void applyEventsForFontSizeChange(Consumer<Node> consumer) {
		if (!(mainView instanceof FontSizeChangeable)) return;
		FontSizeChangeable fsc = (FontSizeChangeable) mainView;
		fsc.applyEventsForFontSizeChange(consumer);
	}

	private ClassRepresentation createViewForClass(CommonClassInfo info) {
		if (mode == ClassViewMode.DECOMPILE) {
			if (info instanceof ClassInfo) {
				return new DecompilePane();
			} else if (info instanceof DexClassInfo) {
				return new SmaliAssemblerPane();
			} else {
				return new BasicClassRepresentation(new Label("Unknown class info type!"), i -> {});
			}
		} else {
			return new HexClassView();
		}
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		info = newValue;
		outline.onUpdate(newValue);
		hierarchy.onUpdate(newValue);
		if (mainView != null) {
			mainView.onUpdate(newValue);
		}
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return info;
	}

	@Override
	public boolean supportsMemberSelection() {
		// Delegate to main view
		if (mainView != null) {
			return mainView.supportsMemberSelection();
		}
		return false;
	}

	@Override
	public boolean isMemberSelectionReady() {
		// Delegate to main view
		if (mainView != null) {
			return mainView.isMemberSelectionReady();
		}
		return false;
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		if (supportsMemberSelection() && mainView != null) {
			mainView.selectMember(memberInfo);
		}
	}

	@Override
	public void cleanup() {
		if (mainView instanceof Cleanable) {
			((Cleanable) mainView).cleanup();
		}
	}

	@Override
	public void undo() {
		if (supportsEditing()) {
			Resource primary = getPrimary();
			String name = info.getName();
			if (primary != null && primary.getClasses().hasHistory(name))
				primary.getClasses().decrementHistory(name);
		}
	}

	@Override
	public SaveResult save() {
		if (supportsEditing())
			return mainView.save();
		return SaveResult.IGNORED;
	}

	@Override
	public boolean supportsEditing() {
		// Only allow editing if the wrapped info belongs to the primary resource
		Resource primary = getPrimary();
		if (primary == null)
			return false;
		// Resource must contain the path
		if (info instanceof DexClassInfo && !primary.getDexClasses().containsKey(info.getName()))
			return false;
		else if (info instanceof ClassInfo && !primary.getClasses().containsKey(info.getName()))
			return false;
		// Then delegate to main view
		if (mainView != null)
			return mainView.supportsEditing();
		return false;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	@Override
	public void installSideTabs(CollapsibleTabPane tabPane) {
		if (!contentSplit.getItems().contains(tabPane)) {
			contentSplit.getItems().add(tabPane);
			tabPane.setup();
		}
	}

	@Override
	public void populateSideTabs(CollapsibleTabPane tabPane) {
		tabPane.getTabs().addAll(
				createOutlineTab(),
				createHierarchyTab()
		);
		if (mainView instanceof ToolSideTabbed) {
			((ToolSideTabbed) mainView).populateSideTabs(tabPane);
		}
	}

	/**
	 * @return Wrapped view.
	 */
	public ClassRepresentation getMainView() {
		return mainView;
	}

	/**
	 * @return Current view mode, dictating what is shown in {@link #getMainView() the main view}.
	 */
	public ClassViewMode getMode() {
		return mode;
	}

	/**
	 * Set the view mode and trigger a refresh.
	 *
	 * @param mode
	 * 		New view mode.
	 */
	public void setMode(ClassViewMode mode) {
		// Skip if the same
		if (this.mode == mode)
			return;
		this.mode = mode;
		// Cleanup old view if present
		if (mainView instanceof Cleanable) {
			((Cleanable) mainView).cleanup();
		}
		// Trigger refresh
		refreshView();
		onUpdate(info);
	}

	/**
	 * Regenerates the main view component from the {@link #getCurrentClassInfo() current class info}.
	 */
	public void refreshView() {
		mainView = createViewForClass(info);
		applyEventsForFontSizeChange(mainView);
		mainViewWrapper.setCenter(mainView.getNodeRepresentation());
		sideTabs.getTabs().clear();
		populateSideTabs(sideTabs);
		sideTabs.setup();
	}

	private Tab createOutlineTab() {
		Tab tab = new Tab();
		tab.textProperty().bind(Lang.getBinding("outline.title"));
		tab.setGraphic(Icons.getIconView(Icons.T_STRUCTURE));
		tab.setContent(outline);
		return tab;
	}

	private Tab createHierarchyTab() {
		Tab tab = new Tab();
		tab.textProperty().bind(Lang.getBinding("hierarchy.title"));
		tab.setGraphic(Icons.getIconView(Icons.T_TREE));
		tab.setContent(hierarchy);
		return tab;
	}

	private static Resource getPrimary() {
		Workspace workspace = RecafUI.getController().getWorkspace();
		if (workspace != null)
			return workspace.getResources().getPrimary();
		return null;
	}
}
