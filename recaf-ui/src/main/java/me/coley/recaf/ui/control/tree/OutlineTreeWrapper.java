package me.coley.recaf.ui.control.tree;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableStringValue;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.pane.OutlinePane;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.threading.FxThreadUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class OutlineTreeWrapper extends OutlineTree {

	private final SimpleBooleanProperty caseSensitivity = new SimpleBooleanProperty();

	private final OutlineTree tree;

	private final ObservableStringValue filter;

	public OutlineTreeWrapper(ClassRepresentation parent, ObservableStringValue filter, OutlinePane outlinePane) {
		super(parent, outlinePane);
		this.tree = new OutlineTree(parent, outlinePane);
		this.filter = filter;
		rootProperty().bindBidirectional(tree.rootProperty());
		cellFactoryProperty().bindBidirectional(tree.cellFactoryProperty());
	}

	@Override
	public void onUpdate(CommonClassInfo info) {
		boolean caseSensitive = caseSensitivity.get();
		String filterStr = caseSensitive ? filter.getValue() : filter.getValue().toLowerCase();
		OutlineItem outlineRoot = new OutlineItem(null);
		List<FieldInfo> fields = new ArrayList<>();
		if (outlinePane.memberType.get() != OutlinePane.MemberType.METHOD) {
			for (FieldInfo fieldInfo : info.getFields()) {
				filter(fieldInfo.getAccess(), caseSensitive, fieldInfo.getName(), filterStr, fields, fieldInfo);
			}
		}
		List<MethodInfo> methods = new ArrayList<>();
		if (outlinePane.memberType.get() != OutlinePane.MemberType.FIELD) {
			for (MethodInfo methodInfo : info.getMethods()) {
				filter(methodInfo.getAccess(), caseSensitive, methodInfo.getName(), filterStr, methods, methodInfo);
			}
		}
		Comparator<MemberInfo> comparator = (a, b) -> {
			int result = 0;
			if (outlinePane.sortByVisibility.get())
				result = OutlinePane.Visibility.ofMember(a).compareTo(OutlinePane.Visibility.ofMember(b));
			if (result == 0 && outlinePane.sortAlphabetically.get())
				result = a.getName().compareTo(b.getName());
			return result;
		};

		outlineRoot.getChildren().addAll(fields.stream().sorted(comparator).map(OutlineItem::new).collect(Collectors.toList()));
		outlineRoot.getChildren().addAll(methods.stream().sorted(comparator).map(OutlineItem::new).collect(Collectors.toList()));
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

	private <T extends MemberInfo> void filter(int access, boolean caseSensitive, String name, String filterStr, List<T> members, T memberInfo) {
		if (!outlinePane.visibility.get().isAccess(access))
			return;
		if (!outlinePane.showSynthetics.get() && AccessFlag.isSynthetic(access))
			return;
		if (caseSensitive) {
			if (name.contains(filterStr))
				members.add(memberInfo);
		} else if (name.toLowerCase().contains(filterStr))
			members.add(memberInfo);
	}
}
