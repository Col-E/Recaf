package me.coley.recaf.ui.pane;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.*;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.context.ContextBuilder;
import me.coley.recaf.ui.control.NavigationBar;
import me.coley.recaf.ui.control.tree.OutlineTree;
import me.coley.recaf.ui.control.tree.OutlineTreeWrapper;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.NodeEvents;
import me.coley.recaf.util.Translatable;

import java.util.List;
import java.util.function.Function;

import static me.coley.recaf.ui.util.Icons.getClassIcon;
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
	public final SimpleBooleanProperty caseSensitive = new SimpleBooleanProperty();
	private final OutlineTreeWrapper tree;
	private CommonClassInfo classInfo;

	public enum MemberType implements Translatable {
		ALL(Icons.CLASS_N_FIELD_N_METHOD, "misc.all"),
		FIELD(Icons.FIELD, "misc.member.field"),
		METHOD(Icons.METHOD, "misc.member.method"),
		FIELD_AND_METHOD(Icons.FIELD_N_METHOD, "misc.member.field_n_method"),
		INNER_CLASS(Icons.CLASS, "misc.member.inner_class");
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
		List<String> outers = parent.getCurrentClassInfo().getOuterClassBreadcrumbs();
		HBox breadcrumbs = new HBox();
		breadcrumbs.setSpacing(5);
		String previousOuterName = "";
		String firstOuterName = !outers.isEmpty() ? outers.get(0) : parent.getCurrentClassInfo().getName();
		int indexOfPathEnd = firstOuterName.lastIndexOf('/');
		if (indexOfPathEnd != -1) {
			previousOuterName = firstOuterName.substring(0, indexOfPathEnd + 1);
			HBox outerNode = new HBox(
				getIconView(Icons.FOLDER_PACKAGE),
				new Label(firstOuterName.substring(0, indexOfPathEnd))
			);
			outerNode.setSpacing(5);
			breadcrumbs.getChildren().addAll(outerNode);
			if (!outers.isEmpty()) breadcrumbs.getChildren().add(new NavigationBar.NavigationSeparator());
		}
		if (!outers.isEmpty()) {
			for (int i = 0; i < outers.size(); i++) {
				String outer = outers.get(i);
				ClassInfo classInfo = RecafUI.getController().getWorkspace().getResources().getClass(outer);
				HBox outerNode = new HBox(
					new StackPane(
						classInfo != null ? getClassIcon(classInfo) : getIconView(Icons.CLASS),
						getIconView(Icons.UP_FOR_ICON)
					),
					new Label(outer.startsWith(previousOuterName) ? outer.substring(previousOuterName.length()) : outer)
				);
				previousOuterName = outer + "$";
				outerNode.setSpacing(5);
				if (classInfo != null) {
					outerNode.setOnMouseClicked(e -> {
						if (e.getButton() == MouseButton.PRIMARY) CommonUX.openClass(classInfo);
					});
					ContextMenu menu = ContextBuilder.forClass(classInfo).setDeclaration(false).build();
					outerNode.setOnContextMenuRequested(e -> {
						menu.hide();
						menu.show(outerNode, e.getScreenX(), e.getScreenY());
					});
				}
				breadcrumbs.getChildren().add(outerNode);
				if (i < outers.size() - 1) {
					breadcrumbs.getChildren().add(new NavigationBar.NavigationSeparator());
				}
			}
		}
		if (!breadcrumbs.getChildren().isEmpty()) setTop(breadcrumbs);
		setCenter(tree);
		HBox.setHgrow(filter, Priority.ALWAYS);

		showTypes.bindBidirectional(Configs.editor().showOutlinedTypes);
		showSynthetics.bindBidirectional(Configs.editor().showOutlinedSynthetics);
		memberType.bindBidirectional(Configs.editor().showOutlinedMemberType);
		visibility.bindBidirectional(Configs.editor().showOutlinedVisibility);
		sortAlphabetically.bindBidirectional(Configs.editor().sortOutlinedAlphabetically);
		sortByVisibility.bindBidirectional(Configs.editor().sortOutlinedByVisibility);
		caseSensitive.bindBidirectional(Configs.editor().caseSensitiveOutlinedFilter);

		setBottom(new VBox(createButtonBar(), new HBox(filter, createBooleanButton("conf.editor.outline.filter.casesensitive", Icons.CASE_SENSITIVITY, caseSensitive))));
	}

	private TextField createFilterBar() {
		TextField filter = new TextField();
		filter.promptTextProperty().bind(Lang.getBinding("conf.editor.outline.filter.prompt"));
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
		return new HBox(
			// Show synthetics
			createBooleanButton("conf.editor.outline.showoutlinedsynths", Icons.SYNTHETIC, showSynthetics),
			// Show types
			createBooleanButton("conf.editor.outline.showoutlinedtypes", Icons.CODE, showTypes),
			// Sort alphabetically
			createBooleanButton("conf.editor.outline.sortalphabetically", Icons.SORT_ALPHABETICAL, sortAlphabetically),
			// sort by visibility
			createBooleanButton("conf.editor.outline.sortbyvisibility", Icons.SORT_VISIBILITY, sortByVisibility),
			// Member type
			createMultiChoiceButton(MemberType.class, "conf.editor.outline.showoutlinedmembertype", memberType, m -> m.icon),
			// Visibility
			createMultiChoiceButton(Visibility.class, "conf.editor.outline.showoutlinedvisibility", visibility, v -> v.icon)
		);
	}

	private <E extends Enum<E>> Button createMultiChoiceButton(Class<E> enumClass, String translationKey, Property<E> enumProperty, Function<E, String> iconProvider) {
		Button button = createButtonBase(translationKey);
		button.graphicProperty().bind(Bindings.createObjectBinding(() -> getIconView(iconProvider.apply(enumProperty.getValue())), enumProperty));
		button.setOnAction(e -> {
			enumProperty.setValue(enumClass.getEnumConstants()[(enumProperty.getValue().ordinal() + 1) % enumClass.getEnumConstants().length]);
		});
		enumProperty.addListener((observable, oldValue, newValue) -> onUpdate(classInfo));
		return button;
	}

	private static Button createButtonBase(String translationKey) {
		Button button = new Button();
		Tooltip tooltip = new Tooltip();
		tooltip.textProperty().bind(Lang.getBinding(translationKey));
		button.setTooltip(tooltip);
		return button;
	}

	private Button createBooleanButton(String tooltipKey, String graphic, SimpleBooleanProperty buttonOptionProperty) {
		Button button = createButtonBase(tooltipKey);
		button.setGraphic(getIconView(graphic));
		button.setOnAction(e -> buttonOptionProperty.set(!buttonOptionProperty.get()));
		buttonOptionProperty.addListener((observable, oldValue, newValue) -> {
			updateButton(button, newValue);
			onUpdate(classInfo);
		});
		updateButton(button, buttonOptionProperty.get());
		return button;
	}

	private static void updateButton(Button button, boolean active) {
		if (active) {
			button.setOpacity(1.0);
		} else {
			button.setOpacity(0.4);
		}
	}
}
