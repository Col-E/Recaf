package me.coley.recaf.ui.control.tree;

import javafx.beans.value.ChangeListener;
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
import me.coley.recaf.ui.pane.OutlinePane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.Types;
import me.coley.recaf.util.threading.FxThreadUtil;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Tree that represents the {@link MemberInfo} of a {@link CommonClassInfo}.
 *
 * @author Matt Coley
 */
public class OutlineTree extends TreeView<ItemInfo> implements Updatable<CommonClassInfo> {
	private final ClassRepresentation parent;
	protected final OutlinePane outlinePane;

	public OutlineTree(ClassRepresentation parent, OutlinePane outlinePane) {
		this.parent = parent;
		getStyleClass().add("transparent-tree");
		this.outlinePane = outlinePane;
	}

	@Override
	public void onUpdate(CommonClassInfo info) {
		OutlineItem outlineRoot = new OutlineItem(null);
		if (outlinePane.memberType.get() != OutlinePane.MemberType.METHOD) {
			for (FieldInfo fieldInfo : info.getFields()) {
				if (!outlinePane.showSynthetics.get() && AccessFlag.isSynthetic(fieldInfo.getAccess()))
					continue;
				outlineRoot.getChildren().add(new OutlineItem(fieldInfo));
			}
		}
		if (outlinePane.memberType.get() != OutlinePane.MemberType.FIELD) {
			for (MethodInfo methodInfo : info.getMethods()) {
				if (!outlinePane.showSynthetics.get() && AccessFlag.isSynthetic(methodInfo.getAccess()))
					continue;
				outlineRoot.getChildren().add(new OutlineItem(methodInfo));
			}
		}
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
					String text = name;
					if (outlinePane.showTypes.get()) {
						String type;
						if (Types.isValidDesc(desc))
							type = StringUtil.shortenPath(Type.getType(desc).getInternalName());
						else type = "<INVALID>";
						text = type + " " + text;
					}
					int maxLen = Configs.display().maxTreeTextLength;
					setText(StringUtil.limit(EscapeUtil.escape(text), "...", maxLen));
					setGraphic(getMemberIcon(member));
					setContextMenu(ContextBuilder.forField(classInfo, (FieldInfo) item)
						.setDeclaration(true)
						.build());
				} else {
					String text = name;
					if (outlinePane.showTypes.get()) {
						text += "(" + Arrays.stream(Type.getArgumentTypes(desc))
							.map(argType -> StringUtil.shortenPath(argType.getInternalName()))
							.collect(Collectors.joining(", ")) +
							")" + StringUtil.shortenPath(Type.getReturnType(desc).getInternalName());
					}
					int maxLen = Configs.display().maxTreeTextLength;
					setText(StringUtil.limit(EscapeUtil.escape(text), "...", maxLen));
					setGraphic(getMemberIcon(member));
					setContextMenu(ContextBuilder.forMethod(classInfo, (MethodInfo) member)
						.setDeclaration(true)
						.build());
				}
				// Clicking the outline member selects it in the parent view
				setOnMouseClicked(e -> parent.selectMember(member));
			} else if (item instanceof InnerClassInfo) {
				InnerClassInfo innerClass = (InnerClassInfo) item;
				setText(StringUtil.limit(
					EscapeUtil.escape(innerClass.getInnerName()),
					"...",
					Configs.display().maxTreeTextLength));
				ClassInfo classInfo = RecafUI.getController().getWorkspace().getResources().getClass(innerClass.getName());
				if (classInfo == null) {
					setGraphic(getMemberIcon(innerClass));
					return;
				}
				setGraphic(getMemberIcon(classInfo));
				setContextMenu(ContextBuilder.forClass(classInfo).setDeclaration(false).build());
				setOnMouseClicked(e -> {if (e.getButton() == MouseButton.PRIMARY) CommonUX.openClass(classInfo);});
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
		if (node == null) {return null;}
		var hbox = new HBox();

		Node finalNode = node;
		ChangeListener<OutlinePane.Visibility.IconPosition> listener = (observable, oldV, newV) -> {
			if (newV != OutlinePane.Visibility.IconPosition.NONE) {
				var visIcon = Icons.getIconView(OutlinePane.Visibility.ofItem(info).icon);
				if (newV == OutlinePane.Visibility.IconPosition.LEFT) {
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
