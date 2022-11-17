package me.coley.recaf.ui.control.tree;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.*;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.Updatable;
import me.coley.recaf.ui.context.ContextBuilder;
import me.coley.recaf.ui.control.IconView;
import me.coley.recaf.ui.pane.outline.MemberType;
import me.coley.recaf.ui.pane.outline.OutlinePane;
import me.coley.recaf.ui.pane.outline.Visibility;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.Types;
import me.coley.recaf.util.threading.FxThreadUtil;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tree that represents the {@link MemberInfo} of a {@link CommonClassInfo}.
 *
 * @author Matt Coley
 * @author Amejonah
 */
public class OutlineTree extends TreeView<ItemInfo> implements Updatable<CommonClassInfo> {
	private final ClassRepresentation parent;
	private final OutlinePane outlinePane;
	private final ObservableStringValue filter;

	public OutlineTree(ClassRepresentation parent, ObservableStringValue filter, OutlinePane outlinePane) {
		this.parent = parent;
		getStyleClass().add("transparent-tree");
		this.outlinePane = outlinePane;
		this.filter = filter;
	}

	@Override
	public void onUpdate(CommonClassInfo info) {
		boolean caseSensitive = outlinePane.caseSensitive.get();
		String filterStr = caseSensitive ? filter.getValue() : filter.getValue().toLowerCase();
		OutlineItem outlineRoot = new OutlineItem(null);
		Comparator<ItemInfo> comparator = (a, b) -> {
			int result = 0;
			if (outlinePane.sortByVisibility.get()) {
				if (a instanceof MemberInfo && b instanceof MemberInfo) {
					result = Visibility.ofMember((MemberInfo) a).compareTo(Visibility.ofMember((MemberInfo) b));
				} else if (a instanceof InnerClassInfo && b instanceof InnerClassInfo) {
					result = Visibility.ofClass((InnerClassInfo) a).compareTo(Visibility.ofClass((InnerClassInfo) b));
				} else if (a instanceof CommonClassInfo && b instanceof CommonClassInfo) {
					result = Visibility.ofClass((CommonClassInfo) a).compareTo(Visibility.ofClass((CommonClassInfo) b));
				}
			}
			if (result == 0 && outlinePane.sortAlphabetically.get())
				result = a.getName().compareTo(b.getName());
			return result;
		};
		outlineRoot.getChildren().addAll(getItems(MemberType.INNER_CLASS,
				info.getInnerClasses(), caseSensitive, filterStr, InnerClassInfo::getAccess, comparator));
		outlineRoot.getChildren().addAll(getItems(MemberType.FIELD,
				info.getFields(), caseSensitive, filterStr, FieldInfo::getAccess, comparator));
		outlineRoot.getChildren().addAll(getItems(MemberType.METHOD,
				info.getMethods(), caseSensitive, filterStr, MemberInfo::getAccess, comparator));
		outlineRoot.setExpanded(true);
		// Set factory to null while we update the root. This allows existing cells to be aware that they should
		// not attempt to put effort into redrawing since they are being replaced anyways.
		setCellFactory(null);
		FxThreadUtil.run(() -> {
			setRoot(outlineRoot);
			// Now that the root is set we can reinstate the intended cell factory. Cells for the root and its children
			// will use this factory when the FX thread requests them.
			setCellFactory(param -> new OutlineCell(info));
		});
	}

	private <T extends ItemInfo> List<OutlineItem> getItems(
			MemberType memberType,
			List<T> items,
			boolean caseSensitive,
			String filterStr,
			Function<T, Integer> accessGetter,
			Comparator<ItemInfo> comparator) {
		return outlinePane.memberType.get().shouldDisplay(memberType) ? items.stream()
				.filter(item ->
						filter(
								accessGetter.apply(item), caseSensitive,
								item instanceof InnerClassInfo ? ((InnerClassInfo) item).getSimpleName() : item.getName(),
								filterStr
						)
				)
				.sorted(comparator).map(OutlineItem::new)
				.collect(Collectors.toList()) : List.of();
	}

	private boolean filter(int access, boolean caseSensitive, String name, String filterStr) {
		if (!outlinePane.visibility.get().isAccess(access))
			return false;
		if (!outlinePane.showSynthetics.get() && AccessFlag.isSynthetic(access))
			return false;
		return caseSensitive ? name.contains(filterStr) : name.toLowerCase().contains(filterStr);
	}

	/**
	 * Item of a {@link MemberInfo}.
	 */
	public static class OutlineItem extends TreeItem<ItemInfo> {
		OutlineItem(ItemInfo member) {
			super(member);
		}
	}

	/**
	 * Cell of a {@link MemberInfo}.
	 */
	public class OutlineCell extends TreeCell<ItemInfo> {
		private final CommonClassInfo classInfo;

		OutlineCell(CommonClassInfo classInfo) {
			this.classInfo = classInfo;
			getStyleClass().add("transparent-cell");
			getStyleClass().add("monospace");
		}

		@Override
		protected void updateItem(ItemInfo item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || isRootBeingUpdated()) {
				textProperty().unbind();
				setText(null);
				setGraphic(null);
				setOnMouseClicked(null);
				setContextMenu(null);
			} else if (item == null) {
				// Null is the edge case for the root
				setGraphic(Icons.getClassIcon(classInfo));
				String name = classInfo.getName();
				int separatorIndex = classInfo.getName().lastIndexOf('$');
				if (separatorIndex == -1)
					separatorIndex = classInfo.getName().lastIndexOf('/');
				if (separatorIndex > 0)
					name = name.substring(separatorIndex + 1);
				textProperty().unbind();
				setText(EscapeUtil.escape(name));
				setOnMouseClicked(null);
				if (classInfo instanceof ClassInfo) {
					setContextMenu(ContextBuilder.forClass((ClassInfo) classInfo)
							.setDeclaration(true)
							.build());
				} else {
					setContextMenu(null);
				}
			} else if (item instanceof MemberInfo) {
				MemberInfo member = (MemberInfo) item;
				String name = member.getName();
				String desc = member.getDescriptor();
				if (member.isField()) {
					textProperty().bind(Bindings.createStringBinding(() -> {
						String text = name;
						if (outlinePane.showTypes.get()) {
							String type;
							if (Types.isValidDesc(desc)) {
								type = Types.pretty(Type.getType(desc));
							} else type = "<INVALID>";
							text = type + " " + text;
						}
						return StringUtil.limit(EscapeUtil.escape(text), "...", Configs.display().maxTreeTextLength.get());
					}, outlinePane.showTypes, Configs.display().maxTreeTextLength));
					setGraphic(getMemberIcon(member));
					setContextMenu(ContextBuilder.forField(classInfo, (FieldInfo) item)
							.setDeclaration(true)
							.build());
				} else {
					textProperty().bind(Bindings.createStringBinding(() -> {
						String text = name;
						if (outlinePane.showTypes.get()) {
							text += "(" + Arrays.stream(Type.getArgumentTypes(desc))
									.map(Types::pretty)
									.collect(Collectors.joining(", ")) +
									")" + Types.pretty(Type.getReturnType(desc));
						}
						return StringUtil.limit(EscapeUtil.escape(text), "...", Configs.display().maxTreeTextLength.get());
					}, outlinePane.showTypes, Configs.display().maxTreeTextLength));
					setGraphic(getMemberIcon(member));
					setContextMenu(ContextBuilder.forMethod(classInfo, (MethodInfo) member)
							.setDeclaration(true)
							.build());
				}
				// Clicking the outline member selects it in the parent view
				setOnMouseClicked(e -> parent.selectMember(member));
			} else if (item instanceof InnerClassInfo) {
				InnerClassInfo innerClass = (InnerClassInfo) item;
				textProperty().bind(Bindings.createStringBinding(() -> StringUtil.limit(
								EscapeUtil.escape(innerClass.getSimpleName()),
								"...",
								Configs.display().maxTreeTextLength.get())
						, Configs.display().maxTreeTextLength));
				ClassInfo classInfo = RecafUI.getController().getWorkspace().getResources().getClass(innerClass.getName());
				if (classInfo == null) {
					setGraphic(getMemberIcon(innerClass));
					return;
				}
				setGraphic(getMemberIcon(classInfo));
				setContextMenu(ContextBuilder.forClass(classInfo).setDeclaration(false).build());
				setOnMouseClicked(e -> {
					if (e.getButton() == MouseButton.PRIMARY) CommonUX.openClass(classInfo);
				});
			} else {
				throw new IllegalArgumentException("Unknown item type: " + item.getClass().getName());
			}
		}

		private boolean isRootBeingUpdated() {
			return getTreeView().getCellFactory() == null;
		}
	}

	private static Node getMemberIcon(ItemInfo info) {
		Node node = null;
		if (info instanceof MemberInfo) {
			MemberInfo member = (MemberInfo) info;
			if (member.isField()) {
				node = Icons.getFieldIcon((FieldInfo) info);
			} else if (member.isMethod()) {
				node = Icons.getMethodIcon((MethodInfo) info);
			}
		} else if (info instanceof InnerClassInfo) {
			node = Icons.getClassIcon((InnerClassInfo) info);
		} else if (info instanceof CommonClassInfo) {
			node = Icons.getClassIcon((CommonClassInfo) info);
		}
		if (node == null)
			return null;
		HBox hbox = new HBox();
		Node finalNode = node;
		ChangeListener<Visibility.IconPosition> listener = (observable, oldV, newV) -> {
			if (newV != Visibility.IconPosition.NONE) {
				IconView visIcon = Icons.getIconView(Visibility.ofItem(info).icon);
				if (newV == Visibility.IconPosition.LEFT) {
					hbox.getChildren().setAll(visIcon, finalNode);
				} else {
					hbox.getChildren().setAll(finalNode, visIcon);
				}
			} else {
				hbox.getChildren().setAll(finalNode);
			}
		};
		listener.changed(null, null, Configs.editor().outlineVisibilityIconPosition.get());
		Configs.editor().outlineVisibilityIconPosition.addListener(new WeakChangeListener<>(listener));
		hbox.setUserData(listener);
		return hbox;
	}
}
