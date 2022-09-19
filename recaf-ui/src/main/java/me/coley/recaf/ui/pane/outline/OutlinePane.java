package me.coley.recaf.ui.pane.outline;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
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
import me.coley.recaf.ui.control.BoundLabel;
import me.coley.recaf.ui.control.NavigationBar;
import me.coley.recaf.ui.control.tree.OutlineTree;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.NodeEvents;
import me.coley.recaf.util.StringUtil;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Visualization of the fields and methods of a {@link ClassInfo}.
 *
 * @author Matt Coley
 * @author Amejonah
 */
public class OutlinePane extends BorderPane implements ClassRepresentation {
	public final SimpleBooleanProperty showTypes = new SimpleBooleanProperty();
	public final SimpleBooleanProperty showSynthetics = new SimpleBooleanProperty(true);
	public final SimpleObjectProperty<MemberType> memberType = new SimpleObjectProperty<>(MemberType.ALL);
	public final SimpleObjectProperty<Visibility> visibility = new SimpleObjectProperty<>(Visibility.ALL);
	public final SimpleBooleanProperty sortAlphabetically = new SimpleBooleanProperty();
	public final SimpleBooleanProperty sortByVisibility = new SimpleBooleanProperty();
	public final SimpleBooleanProperty caseSensitive = new SimpleBooleanProperty();
	private final OutlineTree tree;
	private CommonClassInfo classInfo;

	/**
	 * New outline panel.
	 *
	 * @param parent
	 * 		The parent panel the outline belongs to.
	 */
	public OutlinePane(ClassRepresentation parent) {
		SimpleStringProperty filterValue = new SimpleStringProperty();
		this.tree = new OutlineTree(parent, filterValue, this);
		TextField filter = createFilterBar();
		filterValue.bind(filter.textProperty());
		HBox breadcrumbs = createBreadcrumbs(parent);
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

	private HBox createBreadcrumbs(ClassRepresentation parent) {
		List<String> outers = new ArrayList<>(parent.getCurrentClassInfo().getOuterClassBreadcrumbs());
		HBox breadcrumbs = new HBox();
		breadcrumbs.setSpacing(5);
		String previousOuterName = "";
		String firstOuterName = !outers.isEmpty() ? outers.get(0) : parent.getCurrentClassInfo().getName();
		int indexOfPathEnd = firstOuterName.lastIndexOf('/');
		final ObservableList<Node> breadcrumbsChildren = breadcrumbs.getChildren();
		if (indexOfPathEnd != -1) {
			previousOuterName = firstOuterName.substring(0, indexOfPathEnd + 1);
			HBox outerNode = new HBox(
					Icons.getIconView(Icons.FOLDER_PACKAGE),
					new Label(firstOuterName.substring(0, indexOfPathEnd))
			);
			outerNode.setSpacing(5);
			breadcrumbsChildren.addAll(outerNode);
			if (!outers.isEmpty()) breadcrumbsChildren.add(new NavigationBar.NavigationSeparator());
		}
		final OuterMethodInfo outerMethod = parent.getCurrentClassInfo().getOuterMethod();
		if (outerMethod != null) {
			outers.add(outerMethod.getOwner());
		}
		if (!outers.isEmpty()) {
			for (int i = 0; i < outers.size(); i++) {
				String outer = outers.get(i);
				ClassInfo classInfo = RecafUI.getController().getWorkspace().getResources().getClass(outer);
				HBox outerNode = new HBox(
						new StackPane(
								classInfo != null ? Icons.getClassIcon(classInfo) : Icons.getIconView(Icons.CLASS),
								Icons.getIconView(Icons.UP_FOR_ICON)
						),
						new Label(outer.startsWith(previousOuterName) ? outer.substring(previousOuterName.length()) : outer)
				);
				previousOuterName = outer + "$";
				outerNode.setSpacing(5);
				if (classInfo != null) {
					contextMenuAndOpenForClass(outerNode, classInfo);
				}
				breadcrumbsChildren.add(outerNode);
				if (i < outers.size() - 1) {
					breadcrumbsChildren.add(new NavigationBar.NavigationSeparator());
				}
			}
		}
		if (outerMethod != null) {
			if (!breadcrumbsChildren.isEmpty()) breadcrumbsChildren.add(new NavigationBar.NavigationSeparator());
			HBox outerMethodNode = new HBox(
					new StackPane(Icons.getIconView(Icons.METHOD), Icons.getIconView(Icons.UP_FOR_ICON)),
					new BoundLabel(Bindings.createStringBinding(() -> {
						String text = outerMethod.getName();
						if (text == null) return null;
						String desc = outerMethod.getDescriptor();
						if (showTypes.get() && desc != null) {
							text += "(" + Arrays.stream(Type.getArgumentTypes(desc))
									.map(argType -> StringUtil.shortenPath(argType.getInternalName()))
									.collect(Collectors.joining(", ")) +
									")" + StringUtil.shortenPath(Type.getReturnType(desc).getInternalName());
						}
						return StringUtil.limit(EscapeUtil.escape(text), "...", Configs.display().maxTreeTextLength.get());
					}, showTypes, Configs.display().maxTreeTextLength))
			);
			ClassInfo classInfo = RecafUI.getController().getWorkspace().getResources().getClass(outerMethod.getOwner());
			outerMethodNode.setSpacing(5);
			if (classInfo != null && outerMethod.getName() != null) {
				MethodInfo methodInfo = classInfo.findMethod(outerMethod.getName(), outerMethod.getDescriptor());
				if (methodInfo != null) {
					outerMethodNode.setOnMouseClicked(e -> {
						if (e.getButton() == MouseButton.PRIMARY) CommonUX.openMember(classInfo, methodInfo);
					});
					ContextMenu menu = ContextBuilder.forMethod(classInfo, methodInfo).setDeclaration(false).build();
					outerMethodNode.setOnContextMenuRequested(e -> {
						menu.hide();
						menu.show(outerMethodNode, e.getScreenX(), e.getScreenY());
					});
				} else {
					contextMenuAndOpenForClass(outerMethodNode, classInfo);
				}
			}
			breadcrumbsChildren.add(outerMethodNode);
		}
		return breadcrumbs;
	}

	private static void contextMenuAndOpenForClass(HBox outerNode, ClassInfo classInfo) {
		outerNode.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.PRIMARY) CommonUX.openClass(classInfo);
		});
		ContextMenu menu = ContextBuilder.forClass(classInfo).setDeclaration(false).build();
		outerNode.setOnContextMenuRequested(e -> {
			menu.hide();
			menu.show(outerNode, e.getScreenX(), e.getScreenY());
		});
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
				createBooleanButtonWithUpdate("conf.editor.outline.showoutlinedsynths", Icons.SYNTHETIC, showSynthetics),
				// Show types
				createBooleanButton("conf.editor.outline.showoutlinedtypes", Icons.CODE, showTypes),
				// Sort alphabetically
				createBooleanButtonWithUpdate("conf.editor.outline.sortalphabetically", Icons.SORT_ALPHABETICAL, sortAlphabetically),
				// sort by visibility
				createBooleanButtonWithUpdate("conf.editor.outline.sortbyvisibility", Icons.SORT_VISIBILITY, sortByVisibility),
				// Member type
				createMultiChoiceButton(MemberType.class, "conf.editor.outline.showoutlinedmembertype", memberType, m -> m.icon),
				// Visibility
				createMultiChoiceButton(Visibility.class, "conf.editor.outline.showoutlinedvisibility", visibility, v -> v.icon)
		);
	}

	private <E extends Enum<E>> Button createMultiChoiceButton(Class<E> enumClass, String translationKey, Property<E> enumProperty, Function<E, String> iconProvider) {
		Button button = createButtonBase(translationKey);
		button.graphicProperty().bind(Bindings.createObjectBinding(() -> Icons.getIconView(iconProvider.apply(enumProperty.getValue())), enumProperty));
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
		button.setGraphic(Icons.getIconView(graphic));
		button.setOnAction(e -> {
			boolean newV = !buttonOptionProperty.get();
			buttonOptionProperty.set(newV);
		});
		button.opacityProperty().bind(
				Bindings.when(buttonOptionProperty)
						.then(1.0)
						.otherwise(0.4)
		);
		return button;
	}

	private Button createBooleanButtonWithUpdate(String tooltipKey, String graphic, SimpleBooleanProperty buttonOptionProperty) {
		Button button = createBooleanButton(tooltipKey, graphic, buttonOptionProperty);
		buttonOptionProperty.addListener((observable, oldValue, newValue) -> onUpdate(classInfo));
		return button;
	}
}
