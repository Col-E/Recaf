package me.coley.recaf.ui;

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.control.CollapsibleTabPane;
import me.coley.recaf.ui.pane.DecompilePane;
import me.coley.recaf.ui.pane.HierarchyPane;
import me.coley.recaf.ui.pane.OutlinePane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;

/**
 * Display for a {@link CommonClassInfo}.
 *
 * @author Matt Coley
 */
public class ClassView extends BorderPane implements ClassRepresentation, Cleanable {
	private final OutlinePane outline;
	private final HierarchyPane hierarchy;
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
		BorderPane mainViewWrapper = new BorderPane();
		if (info instanceof ClassInfo) {
			DecompilePane decompilePane = new DecompilePane();
			mainViewWrapper.setCenter(decompilePane);
			mainView = decompilePane;
		} else if (info instanceof DexClassInfo) {
			// TODO: Android display
			mainViewWrapper.setCenter(new Label("Android is not yet supported"));
		}
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
		setCenter(split);
		onUpdate(info);
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
	public boolean supportsMemberSelection() {
		// Delegate to main view
		if (mainView != null) {
			return mainView.supportsMemberSelection();
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
	public Node getNodeRepresentation() {
		return this;
	}

	private Tab createOutlineTab() {
		Tab tab = new Tab(Lang.get("outline.title"));
		tab.setGraphic(Icons.getIconView(Icons.T_STRUCTURE));
		tab.setContent(outline);
		return tab;
	}

	private Tab createHierarchyTab() {
		Tab tab = new Tab(Lang.get("hierarchy.title"));
		tab.setGraphic(Icons.getIconView(Icons.T_TREE));
		tab.setContent(hierarchy);
		return tab;
	}
}
