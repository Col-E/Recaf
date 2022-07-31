package me.coley.recaf.ui.control.tree;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableStringValue;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.threading.FxThreadUtil;

import static me.coley.recaf.ui.pane.OutlinePane.showSynthetics;

public class OutlineTreeWrapper extends OutlineTree{

	private final SimpleBooleanProperty caseSensitivity = new SimpleBooleanProperty();

	private final OutlineTree tree;

	private final ObservableStringValue filter;

	public OutlineTreeWrapper(ClassRepresentation parent, ObservableStringValue filter) {
		super(parent);
		this.tree = new OutlineTree(parent);
		this.filter = filter;
		rootProperty().bindBidirectional(tree.rootProperty());
		cellFactoryProperty().bindBidirectional(tree.cellFactoryProperty());
	}

	@Override
	public void onUpdate(CommonClassInfo info) {
		boolean caseSensitive = caseSensitivity.get();
		String filterStr = caseSensitive ? filter.getValue() : filter.getValue().toLowerCase();
		OutlineItem outlineRoot = new OutlineItem(null);
		for (FieldInfo fieldInfo : info.getFields()) {
			extracted(fieldInfo.getAccess(), caseSensitive, fieldInfo.getName(), filterStr, outlineRoot, fieldInfo);
		}
		for (MethodInfo methodInfo : info.getMethods()) {
			extracted(methodInfo.getAccess(), caseSensitive, methodInfo.getName(), filterStr, outlineRoot, methodInfo);
		}
		outlineRoot.setExpanded(true);
		// Set factory to null while we update the root. This allows existing cells to be aware that they should
		// not attempt to put effort into redrawing since they are being replaced anyways.
		tree.setCellFactory(null);
		FxThreadUtil.run(() -> {
			tree.setRoot(outlineRoot);
			// Now that the root is set we can reinstate the intended cell factory. Cells for the root and its children
			// will use this factory when the FX thread requests them.
			tree.setCellFactory(param -> new OutlineCell(info));
		});
	}

	private static void extracted(int access, boolean caseSensitive, String name, String filterStr, OutlineItem outlineRoot, MemberInfo memberInfo) {
		if (!showSynthetics.get() && AccessFlag.isSynthetic(access))
			return;
		if(caseSensitive) {
			if (name.contains(filterStr))
				outlineRoot.getChildren().add(new OutlineItem(memberInfo));
		} else if (name.toLowerCase().contains(filterStr))
			outlineRoot.getChildren().add(new OutlineItem(memberInfo));
	}
}
