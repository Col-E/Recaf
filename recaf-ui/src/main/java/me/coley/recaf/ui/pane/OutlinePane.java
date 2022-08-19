package me.coley.recaf.ui.pane;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
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
import me.coley.recaf.code.*;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.tree.OutlineTree;
import me.coley.recaf.ui.control.tree.OutlineTreeWrapper;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.NodeEvents;
import me.coley.recaf.util.Translatable;

import java.util.function.Consumer;
import java.util.function.Function;

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
	public final SimpleObjectProperty<Visibility> visibility = new SimpleObjectProperty<>(Visibility.ALL);

	public final SimpleBooleanProperty sortAlphabetically = new SimpleBooleanProperty();
	public final SimpleBooleanProperty sortByVisibility = new SimpleBooleanProperty();
	private final OutlineTreeWrapper tree;
	private CommonClassInfo classInfo;
	private final ChangeListener<?> listenerToUpdate = (observable, oldValue, newValue) -> onUpdate(classInfo);

	public enum MemberType implements Translatable {
		ALL(Icons.CLASS_N_FIELD_N_METHOD, "misc.all"),
		FIELD(Icons.FIELD, "misc.member.field"),
		METHOD(Icons.METHOD, "misc.member.method"),
		FIELD_AND_METHOD(Icons.FIELD_N_METHOD, "misc.member.field_n_method"),
		INNER_CLASS(Icons.CLASS, "misc.member.innerClass");
		final String icon;
		final String key;

		MemberType(String icon, String key) {
			this.icon = icon;
			this.key = key;
		}

		@Override
		public String getTranslationKey() {return key;}

		public boolean shouldDisplay(MemberType filter) {
			return this == ALL || this == filter || (this == FIELD_AND_METHOD && (filter == FIELD || filter == METHOD));
		}
	}

	public enum Visibility implements Translatable {
		ALL(Icons.ACCESS_ALL_VISIBILITY, (flags) -> true),
		PUBLIC(Icons.ACCESS_PUBLIC, (flags) -> AccessFlag.isPublic(flags)),
		PROTECTED(Icons.ACCESS_PROTECTED, (flags) -> AccessFlag.isProtected(flags)),
		PACKAGE(Icons.ACCESS_PACKAGE, (flags) -> AccessFlag.isPackage(flags)),
		PRIVATE(Icons.ACCESS_PRIVATE, (flags) -> AccessFlag.isPrivate(flags));
		public final String icon;
		private final Function<Integer, Boolean> isAccess;

		Visibility(String icon, Function<Integer, Boolean> isAccess) {
			this.icon = icon;
			this.isAccess = isAccess;
		}

		public static Visibility ofItem(ItemInfo info) {
			if (info instanceof MemberInfo) {
				return ofMember((MemberInfo) info);
			} else if (info instanceof CommonClassInfo) {
				return ofClass((CommonClassInfo) info);
			} else if (info instanceof InnerClassInfo) {
				return ofClass((InnerClassInfo) info);
			} else {
				throw new IllegalArgumentException("Unknown item type: " + info.getClass().getSimpleName());
			}
		}

		public boolean isAccess(int flags) {
			return isAccess.apply(flags);
		}

		public static Visibility ofMember(MemberInfo memberInfo) {
			return ofAccess(memberInfo.getAccess());
		}

		public static Visibility ofClass(CommonClassInfo info) {
			return ofAccess(info.getAccess());
		}

		public static Visibility ofClass(InnerClassInfo info) {
			return ofAccess(info.getAccess());
		}

		private static Visibility ofAccess(int access) {
			if (AccessFlag.isPublic(access))
				return PUBLIC;
			if (AccessFlag.isProtected(access))
				return PROTECTED;
			if (AccessFlag.isPackage(access))
				return PACKAGE;
			if (AccessFlag.isPrivate(access))
				return PRIVATE;
			return ALL;
		}

		@Override
		public String getTranslationKey() {return this == ALL ? "misc.all" : "misc.accessflag.visibility." + name().toLowerCase();}

		public enum IconPosition implements Translatable {
			NONE, LEFT, RIGHT;

			@Override
			public String getTranslationKey() {
				return this == NONE ? "misc.none" : "misc.position." + name().toLowerCase();
			}
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
		showTypes.addListener(new WeakChangeListener(listenerToUpdate));
		showSynthetics.set(Configs.editor().showOutlinedSynthetics);
		showSynthetics.addListener(new WeakChangeListener(listenerToUpdate));
		memberType.bindBidirectional(Configs.editor().showOutlinedMemberType);
		memberType.addListener(new WeakChangeListener(listenerToUpdate));
		visibility.bindBidirectional(Configs.editor().showOutlinedVisibility);
		visibility.addListener(new WeakChangeListener(listenerToUpdate));
		sortAlphabetically.bindBidirectional(Configs.editor().sortOutlinedAlphabetically);
		sortAlphabetically.addListener(new WeakChangeListener(listenerToUpdate));
		sortByVisibility.bindBidirectional(Configs.editor().sortOutlinedByVisibility);
		sortByVisibility.addListener(new WeakChangeListener(listenerToUpdate));
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
		addButton(box, "conf.editor.outline.showoutlinedsynths", Icons.SYNTHETIC, showSynthetics,
			(newVal) -> Configs.editor().showOutlinedSynthetics = newVal);
		// Show types
		addButton(box, "conf.editor.outline.showoutlinedtypes", Icons.CODE, showTypes,
			(newVal) -> Configs.editor().showOutlinedTypes = newVal);
		// Sort alphabetically
		addButton(box, "conf.editor.outline.sortalphabetically", Icons.SORT_ALPHABETICAL, sortAlphabetically,
			(newVal) -> Configs.editor().sortOutlinedAlphabetically.set(newVal));
		// sort by visibility
		addButton(box, "conf.editor.outline.sortbyvisibility", Icons.SORT_VISIBILITY, sortByVisibility,
			(newVal) -> Configs.editor().sortOutlinedByVisibility.set(newVal));
		// Member type
		Class<? extends Enum<?>> enumChoice = MemberType.class;
		Tooltip tipMemberType = new Tooltip();
		tipMemberType.textProperty().bind(Lang.getBinding("conf.editor.outline.showoutlinedmembertype"));
		Button memberTypeButton = new Button();
		memberTypeButton.setTooltip(tipMemberType);
		memberTypeButton.graphicProperty().bind(Bindings.createObjectBinding(() -> getIconView(memberType.get().icon), memberType));
		memberTypeButton.setOnAction(e -> {
			MemberType newType = MemberType.values()[(memberType.get().ordinal() + 1) % MemberType.values().length];
			memberType.set(newType);
		});
		box.getChildren().add(memberTypeButton);
		// Visibility
		Tooltip tipVisibility = new Tooltip();
		tipVisibility.textProperty().bind(Lang.getBinding("conf.editor.outline.showoutlinedvisibility"));
		Button visibilityButton = new Button();
		visibilityButton.setTooltip(tipVisibility);
		visibilityButton.graphicProperty().bind(Bindings.createObjectBinding(() -> getIconView(visibility.get().icon), visibility));
		visibilityButton.setOnAction(e -> {
			Visibility newVisibility = Visibility.values()[(visibility.get().ordinal() + 1) % Visibility.values().length];
			visibility.set(newVisibility);
		});
		box.getChildren().add(visibilityButton);
		return box;
	}

	private void addButton(Pane parent, String tooltipKey, String graphic, SimpleBooleanProperty buttonOptionProperty, Consumer<Boolean> editOption) {
		Button button = new Button();
		var tooltip = new Tooltip();
		tooltip.textProperty().bind(Lang.getBinding(tooltipKey));
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
