package me.coley.recaf.ui.control.tree;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import me.coley.recaf.code.*;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.Updatable;
import me.coley.recaf.ui.context.ContextBuilder;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.Types;
import me.coley.recaf.util.threading.FxThreadUtil;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.stream.Collectors;

import static me.coley.recaf.ui.pane.OutlinePane.showSynthetics;
import static me.coley.recaf.ui.pane.OutlinePane.showTypes;

/**
 * Tree that represents the {@link MemberInfo} of a {@link CommonClassInfo}.
 */
public class OutlineTree extends TreeView<MemberInfo> implements Updatable<CommonClassInfo> {
	private final ClassRepresentation parent;
	public OutlineTree(ClassRepresentation parent) {
		this.parent = parent;
		getStyleClass().add("transparent-tree");
	}

	@Override
	public void onUpdate(CommonClassInfo info) {
		OutlineItem outlineRoot = new OutlineItem(null);
		for (FieldInfo fieldInfo : info.getFields()) {
			if (!showSynthetics.get() && AccessFlag.isSynthetic(fieldInfo.getAccess()))
				continue;
			outlineRoot.getChildren().add(new OutlineItem(fieldInfo));
		}
		for (MethodInfo methodInfo : info.getMethods()) {
			if (!showSynthetics.get() && AccessFlag.isSynthetic(methodInfo.getAccess()))
				continue;
			outlineRoot.getChildren().add(new OutlineItem(methodInfo));
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
	public static class OutlineItem extends TreeItem<MemberInfo> {
		OutlineItem(MemberInfo member) {
			super(member);
		}
	}

	/**
	 * Cell of a {@link MemberInfo}.
	 */
	public class OutlineCell extends TreeCell<MemberInfo> {
		private final CommonClassInfo classInfo;

		OutlineCell(CommonClassInfo classInfo) {
			this.classInfo = classInfo;
			getStyleClass().add("transparent-cell");
			getStyleClass().add("monospace");
		}

		@Override
		protected void updateItem(MemberInfo item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || isRootBeingUpdated()) {
				setText(null);
				setGraphic(null);
				setOnMouseClicked(null);
				setContextMenu(null);
			} else if (item == null) {
				// Null is the edge case for the root
				setGraphic(Icons.getClassIcon(classInfo));
				setText(EscapeUtil.escape(StringUtil.shortenPath(classInfo.getName())));
				setOnMouseClicked(null);
				if (classInfo instanceof ClassInfo) {
					setContextMenu(ContextBuilder.forClass((ClassInfo) classInfo)
						.setDeclaration(true)
						.build());
				} else {
					setContextMenu(null);
				}
			} else {
				String name = item.getName();
				String desc = item.getDescriptor();
				if (item.isField()) {
					String text = name;
					if (showTypes.get()) {
						String type;
						if (Types.isValidDesc(desc))
							type = StringUtil.shortenPath(Type.getType(desc).getInternalName());
						else type = "<INVALID>";
						text = type + " " + text;
					}
					int maxLen = Configs.display().maxTreeTextLength;
					setText(StringUtil.limit(EscapeUtil.escape(text), "...", maxLen));
					setGraphic(Icons.getFieldIcon((FieldInfo) item));
					setContextMenu(ContextBuilder.forField(classInfo, (FieldInfo) item)
						.setDeclaration(true)
						.build());
				} else {
					MethodInfo methodInfo = (MethodInfo) item;
					String text = name;
					if (showTypes.get()) {
						text += "(" + Arrays.stream(Type.getArgumentTypes(desc))
							.map(argType -> StringUtil.shortenPath(argType.getInternalName()))
							.collect(Collectors.joining(", ")) +
							")" + StringUtil.shortenPath(Type.getReturnType(desc).getInternalName());
					}
					int maxLen = Configs.display().maxTreeTextLength;
					setText(StringUtil.limit(EscapeUtil.escape(text), "...", maxLen));
					setGraphic(Icons.getMethodIcon(methodInfo));
					setContextMenu(ContextBuilder.forMethod(classInfo, (MethodInfo) item)
						.setDeclaration(true)
						.build());
				}
				// Clicking the outline member selects it in the parent view
				setOnMouseClicked(e -> parent.selectMember(item));
			}
		}

		private boolean isRootBeingUpdated() {
			return getTreeView().getCellFactory() == null;
		}
	}
}
