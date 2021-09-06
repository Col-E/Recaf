package me.coley.recaf.ui.control;

import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import me.coley.recaf.ui.control.tree.WorkspaceTreeWrapper;
import me.coley.recaf.ui.control.tree.item.BaseTreeValue;
import me.coley.recaf.util.Threads;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Text field that updates a {@link WorkspaceTreeWrapper} to filter what items are shown.
 *
 * @author Matt Coley
 */
public class WorkspaceFilterField extends TextField {
	private static final char TAG_INCLUDE_PREFIX = '+';
	private static final char TAG_EXCLUDE_PREFIX = '-';
	private final Supplier<Boolean> isCaseSensitive;

	/**
	 * @param tree
	 * 		Tree to update the filter of.
	 */
	public WorkspaceFilterField(WorkspaceTreeWrapper tree) {
		setPromptText("Filter: FileName..."); // TODO: Add "+tag -tag" when metadata system is set up
		isCaseSensitive = tree::isCaseSensitive;
		setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ESCAPE) {
				setText("");
			} else if (e.getCode() == KeyCode.UP || e.getCode() == KeyCode.DOWN) {
				Threads.runFx(() -> {
					getParent().requestFocus();
					tree.requestFocus();
				});
				e.consume();
			}
		});
		textProperty().addListener((observable, old, current) -> updateSearch(tree, current));
		getStyleClass().add("filter-field");
	}

	private void updateSearch(WorkspaceTreeWrapper tree, String value) {
		// Populate search arguments
		String[] args = value.split("\\s+");
		List<String> names = new ArrayList<>();
		List<String> tagIncludes = new ArrayList<>();
		List<String> tagExcludes = new ArrayList<>();
		for (String arg : args) {
			if (arg.isEmpty()) {
				continue;
			} else if (arg.charAt(0) == TAG_INCLUDE_PREFIX) {
				tagIncludes.add(arg.substring(1));
			} else if (arg.charAt(0) == TAG_EXCLUDE_PREFIX) {
				tagExcludes.add(arg.substring(1));
			} else {
				names.add(arg);
			}
		}
		// Apply search
		tree.getRootItem().predicateProperty().setValue(item -> filter(item, names, tagIncludes, tagExcludes));
	}

	private boolean filter(TreeItem<BaseTreeValue> item,
						   List<String> names,
						   List<String> tagIncludes,
						   List<String> tagExcludes) {
		boolean filterByTag = tagIncludes.size() > 0 || tagExcludes.size() > 0;
		boolean filterByName = names.size() > 0;
		boolean hasMatchingInclude = filterByTag && checkMatchInclude(item, tagIncludes);
		boolean hasMatchingExclude = filterByTag && checkMatchExclude(item, tagExcludes);
		boolean tagPassFilter = !filterByTag || (hasMatchingInclude && !hasMatchingExclude);
		boolean namePassFilter = !filterByName || checkMatchNames(item, names);
		return tagPassFilter && namePassFilter;
	}

	private boolean checkMatchNames(TreeItem<BaseTreeValue> item, List<String> names) {
		String itemName = item.getValue().getFullPath();
		if (itemName == null) {
			return false;
		}
		for (String name : names) {
			if (isCaseSensitive.get()) {
				if (itemName.contains(name)) {
					return true;
				}
			} else {
				if (itemName.toLowerCase().contains(name.toLowerCase())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkMatchInclude(TreeItem<BaseTreeValue> item, List<String> tagIncludes) {
		// TODO: Metadata system
		//  - Users and plugins and potentially core recaf will be able to tag files via their path
		//  - Example tags: "GUI", "FileIO", "Text", "Code", "Reflection"
		return true;
	}

	private boolean checkMatchExclude(TreeItem<BaseTreeValue> item, List<String> tagExcludes) {
		// TODO: Metadata system
		return false;
	}
}
