package me.coley.recaf.ui;

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
import me.coley.recaf.ui.control.NavigationBar;
import me.coley.recaf.ui.control.hex.HexClassView;
import me.coley.recaf.ui.pane.DecompilePane;
import me.coley.recaf.ui.pane.HierarchyPane;
import me.coley.recaf.ui.pane.OutlinePane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

/**
 * Display for a {@link CommonClassInfo}.
 *
 * @author Matt Coley
 */
public class ClassView extends BorderPane implements ClassRepresentation, Cleanable, Undoable {
	private final OutlinePane outline;
	private final HierarchyPane hierarchy;
	private final BorderPane mainViewWrapper = new BorderPane();
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

		// Setup side tabs with class visualization tools
		CollapsibleTabPane sideTabs = new CollapsibleTabPane();
		sideTabs.setSide(Side.RIGHT);
		sideTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		sideTabs.getTabs().addAll(
				createOutlineTab(),
				createHierarchyTab()
		);
		sideTabs.setup();
		// Put it all together
		SplitPane split = new SplitPane();
		split.getItems().addAll(mainViewWrapper, sideTabs);
		split.setDividerPositions(0.75);
		split.getStyleClass().add("view-split-pane");

		setCenter(split);
		onUpdate(info);
		Configs.keybinds().installEditorKeys(this);

		NavigationBar.getInstance().update(info, null);
	}

	private ClassRepresentation createViewForClass(CommonClassInfo info) {
		if (mode == ClassViewMode.DECOMPILE) {
			if (info instanceof ClassInfo) {
				DecompilePane decompilePane = new DecompilePane();
				return decompilePane;
			} else if (info instanceof DexClassInfo) {
				// TODO: Android display
				return new BasicClassRepresentation(new Label("Android is not yet supported"), i -> {
				});
			} else {
				return new BasicClassRepresentation(new Label("Unknown class info type!"), i -> {
				});
			}
		} else {
			HexClassView view = new HexClassView();
			return view;
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
		if (primary == null || !primary.getClasses().containsKey(info.getName()))
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
		mainView = createViewForClass(info);
		mainViewWrapper.setCenter(mainView.getNodeRepresentation());
		onUpdate(info);
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
