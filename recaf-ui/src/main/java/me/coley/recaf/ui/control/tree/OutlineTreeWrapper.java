package me.coley.recaf.ui.control.tree;

import javafx.beans.value.ObservableStringValue;
import me.coley.recaf.code.*;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.pane.OutlinePane;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.threading.FxThreadUtil;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutlineTreeWrapper extends OutlineTree {

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
		boolean caseSensitive = outlinePane.caseSensitive.get();
		String filterStr = caseSensitive ? filter.getValue() : filter.getValue().toLowerCase();
		OutlineItem outlineRoot = new OutlineItem(null);
		Comparator<ItemInfo> comparator = (a, b) -> {
			int result = 0;
			if (outlinePane.sortByVisibility.get()) {
				if (a instanceof MemberInfo && b instanceof MemberInfo) {
					result = OutlinePane.Visibility.ofMember((MemberInfo) a).compareTo(OutlinePane.Visibility.ofMember((MemberInfo) b));
				} else if (a instanceof InnerClassInfo && b instanceof InnerClassInfo) {
					result = OutlinePane.Visibility.ofClass((InnerClassInfo) a).compareTo(OutlinePane.Visibility.ofClass((InnerClassInfo) b));
				} else if (a instanceof CommonClassInfo && b instanceof CommonClassInfo) {
					result = OutlinePane.Visibility.ofClass((CommonClassInfo) a).compareTo(OutlinePane.Visibility.ofClass((CommonClassInfo) b));
				}
			}
			if (result == 0 && outlinePane.sortAlphabetically.get())
				result = a.getName().compareTo(b.getName());
			return result;
		};
		outlineRoot.getChildren().addAll(getItems(OutlinePane.MemberType.INNER_CLASS,
			info.getInnerClasses(), caseSensitive, filterStr, InnerClassInfo::getAccess, comparator));
		outlineRoot.getChildren().addAll(getItems(OutlinePane.MemberType.FIELD,
			info.getFields(), caseSensitive, filterStr, FieldInfo::getAccess, comparator));
		outlineRoot.getChildren().addAll(getItems(OutlinePane.MemberType.METHOD,
			info.getMethods(), caseSensitive, filterStr, MemberInfo::getAccess, comparator));
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

	private <T extends ItemInfo> List<OutlineItem> getItems(OutlinePane.MemberType memberType, List<T> items, boolean caseSensitive, String filterStr, Function<T, Integer> accessGetter, Comparator<ItemInfo> comparator) {
		return outlinePane.memberType.get().shouldDisplay(memberType) ? items.stream()
			.filter(item -> filter(accessGetter.apply(item), caseSensitive, item instanceof InnerClassInfo ? ((InnerClassInfo) item).getInnerName() : item.getName(), filterStr))
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
}
