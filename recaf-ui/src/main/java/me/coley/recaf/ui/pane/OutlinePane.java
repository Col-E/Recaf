package me.coley.recaf.ui.pane;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.tree.OutlineTree;
import me.coley.recaf.ui.control.tree.OutlineTreeWrapper;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.NodeEvents;

import static me.coley.recaf.ui.util.Icons.getIconView;

/**
 * Visualization of the fields and methods of a {@link ClassInfo}.
 *
 * @author Matt Coley
 */
public class OutlinePane extends BorderPane implements ClassRepresentation {
	public static final SimpleBooleanProperty showTypes = new SimpleBooleanProperty();
	public static final SimpleBooleanProperty showSynthetics = new SimpleBooleanProperty(true);
	private final OutlineTreeWrapper tree;
	private CommonClassInfo classInfo;

	/**
	 * New outline panel.
	 *
	 * @param parent
	 * 		The parent panel the outline belongs to.
	 */
	public OutlinePane(ClassRepresentation parent) {
		SimpleStringProperty filterValue = new SimpleStringProperty();
		this.tree = new OutlineTreeWrapper(parent, filterValue);
		TextField filter = createFilterBar();
		filterValue.bind(filter.textProperty());
		setCenter(tree);
		setBottom(new VBox(createButtonBar(), filter));
	}

	private TextField createFilterBar() {
		TextField filter = new TextField();
		filter.setPromptText("Filter: Field/Method name...");
		filter.getStyleClass().add("filter-field");
		NodeEvents.addKeyPressHandler(filter, e -> {
			if (e.getCode() == KeyCode.ESCAPE) {
				filter.clear();
			}
		});
		NodeEvents.addKeyPressHandler(tree, e -> {
			String text = e.getText();
			if (text != null && !text.isEmpty()) {
				filter.requestFocus();
			} else if (e.getCode() == KeyCode.ESCAPE) {
				filter.clear();
			}
		});
		filter.textProperty().addListener((observable, oldValue, newValue) -> {
			tree.onUpdate(classInfo);
		});
		return filter;
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		classInfo = newValue;
		tree.onUpdate(newValue);
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return classInfo;
	}

	@Override
	public boolean supportsMemberSelection() {
		return true;
	}

	@Override
	public boolean isMemberSelectionReady() {
		// Should be always ready
		return tree.getRoot() != null;
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		// Cancel if selection already matches
		if (memberInfo.equals(tree.getSelectionModel().getSelectedItem().getValue()))
			return;
		// Find the member in the tree and select it
		OutlineTree.OutlineItem root = (OutlineTree.OutlineItem) tree.getRoot();
		for (TreeItem<?> child : root.getChildren()) {
			if (child instanceof OutlineTree.OutlineItem) {
				OutlineTree.OutlineItem memberItem = (OutlineTree.OutlineItem) child;
				if (memberInfo.equals(memberItem.getValue())) {
					tree.getSelectionModel().select(memberItem);
				}
			}
		}
	}

	@Override
	public SaveResult save() {
		throw new UnsupportedOperationException("Outline pane does not support modification");
	}

	@Override
	public boolean supportsEditing() {
		// Outline is just for show. Save keybind does nothing here.
		return false;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	/**
	 * @return Box containing tree display options.
	 */
	private Node createButtonBar() {
		HBox box = new HBox();
		// Show synthetics
		Button btnShowSynthetics = new Button();
		Tooltip tipShowSynthetics = new Tooltip();
		tipShowSynthetics.textProperty().bind(Lang.getBinding("conf.editor.outline.showoutlinedsynths"));
		btnShowSynthetics.setTooltip(tipShowSynthetics);
		btnShowSynthetics.setGraphic(getIconView(Icons.SYNTHETIC));
		btnShowSynthetics.setOnAction(e -> {
			boolean old = showSynthetics.get();
			showSynthetics.set(!old);
			Configs.editor().showOutlinedSynthetics = !old;
			onUpdate(classInfo);
			updateButton(btnShowSynthetics, !old);
		});
		updateButton(btnShowSynthetics, showSynthetics.get());
		// Show types
		Button btnShowTypes = new Button();
		Tooltip tipShowTypes = new Tooltip();
		tipShowTypes.textProperty().bind(Lang.getBinding("conf.editor.outline.showoutlinedtypes"));
		btnShowTypes.setTooltip(tipShowTypes);
		btnShowTypes.setGraphic(getIconView(Icons.CODE));
		btnShowTypes.setOnAction(e -> {
			boolean old = showTypes.get();
			showTypes.set(!old);
			Configs.editor().showOutlinedTypes = !old;
			onUpdate(classInfo);
			updateButton(btnShowTypes, !old);
		});
		updateButton(btnShowTypes, showTypes.get());
		box.getChildren().addAll(btnShowTypes, btnShowSynthetics);
		return box;
	}

	private static void updateButton(Button button, boolean active) {
		if (active) {
			button.setOpacity(1.0);
		} else {
			button.setOpacity(0.4);
		}
	}

	static {
		showTypes.set(Configs.editor().showOutlinedTypes);
		showSynthetics.set(Configs.editor().showOutlinedSynthetics);
	}
}
