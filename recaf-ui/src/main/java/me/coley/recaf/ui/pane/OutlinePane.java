package me.coley.recaf.ui.pane;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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

import java.util.function.Consumer;

import static me.coley.recaf.ui.util.Icons.getIconView;

/**
 * Visualization of the fields and methods of a {@link ClassInfo}.
 *
 * @author Matt Coley
 */
public class OutlinePane extends BorderPane implements ClassRepresentation {
	public final SimpleBooleanProperty showTypes = new SimpleBooleanProperty();
	public final SimpleBooleanProperty showSynthetics = new SimpleBooleanProperty(true);
	public final SimpleObjectProperty<MemberType> memberType = new SimpleObjectProperty<>(MemberType.ALL);

	private final OutlineTreeWrapper tree;
	private CommonClassInfo classInfo;

	public enum MemberType {
		ALL(Icons.FIELD_N_METHOD),
		FIELD(Icons.FIELD),
		METHOD(Icons.METHOD);
		final String icon;

		MemberType(String icon) {
			this.icon = icon;
		}
	}

	/**
	 * New outline panel.
	 *
	 * @param parent The parent panel the outline belongs to.
	 */
	public OutlinePane(ClassRepresentation parent) {
		SimpleStringProperty filterValue = new SimpleStringProperty();
		this.tree = new OutlineTreeWrapper(parent, filterValue, this);
		TextField filter = createFilterBar();
		filterValue.bind(filter.textProperty());
		setCenter(tree);
		setBottom(new VBox(createButtonBar(), filter));

		showTypes.set(Configs.editor().showOutlinedTypes);
		showSynthetics.set(Configs.editor().showOutlinedSynthetics);
		memberType.set(Configs.editor().showOutlinedMemberType);
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
		Tooltip tipShowSynthetics = new Tooltip();
		tipShowSynthetics.textProperty().bind(Lang.getBinding("conf.editor.outline.showoutlinedsynths"));
		addButton(box, tipShowSynthetics, Icons.SYNTHETIC, showSynthetics, (newVal) -> Configs.editor().showOutlinedSynthetics = newVal);
		// Show types
		Tooltip tipShowTypes = new Tooltip();
		tipShowTypes.textProperty().bind(Lang.getBinding("conf.editor.outline.showoutlinedtypes"));
		addButton(box, tipShowTypes, Icons.CODE, showTypes, (newVal) -> Configs.editor().showOutlinedTypes = newVal);
		// Member type
		Tooltip tipMemberType = new Tooltip();
		tipMemberType.textProperty().bind(Lang.getBinding("conf.editor.outline.showoutlinedmembertype"));
		Button button = new Button();
		button.setTooltip(tipMemberType);
		button.graphicProperty().bind(Bindings.createObjectBinding(() -> getIconView(memberType.get().icon), memberType));
		button.setOnAction(e -> {
			MemberType newType = MemberType.values()[(memberType.get().ordinal() + 1) % MemberType.values().length];
			memberType.set(newType);
			Configs.editor().showOutlinedMemberType = newType;
			onUpdate(classInfo);
		});
		box.getChildren().add(button);
		return box;
	}

	private void addButton(Pane parent, Tooltip tooltip, String graphic, SimpleBooleanProperty buttonOptionProperty, Consumer<Boolean> editOption) {
		Button button = new Button();
		button.setTooltip(tooltip);
		button.setGraphic(getIconView(graphic));
		button.setOnAction(e -> {
			boolean old = buttonOptionProperty.get();
			buttonOptionProperty.set(!old);
			editOption.accept(!old);
			onUpdate(classInfo);
			updateButton(button, !old);
		});
		updateButton(button, buttonOptionProperty.get());
		parent.getChildren().add(button);
	}

	private static void updateButton(Button button, boolean active) {
		if (active) {
			button.setOpacity(1.0);
		} else {
			button.setOpacity(0.4);
		}
	}
}
