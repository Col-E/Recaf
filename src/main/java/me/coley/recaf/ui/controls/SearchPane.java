package me.coley.recaf.ui.controls;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import me.coley.recaf.control.Controller;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.search.*;
import me.coley.recaf.ui.controls.tree.*;
import me.coley.recaf.util.Log;
import me.coley.recaf.workspace.Workspace;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static me.coley.recaf.util.LangUtil.translate;

/**
 * Pane for displaying search query inputs &amp; results.
 *
 * @author Matt
 */
@SuppressWarnings("unchecked")
public class SearchPane extends SplitPane {
	private final Map<String, Input> inputMap = new HashMap<>();
	private final TreeView tree = new TreeView();


	/**
	 * @param controller
	 * 		Controller to act on.
	 * @param type
	 * 		Type of query.
	 */
	public SearchPane(GuiController controller, QueryType type) {
		setOrientation(Orientation.VERTICAL);
		setDividerPositions(0.5);
		tree.setCellFactory(e -> new JavaResourceCell());
		// TODO: Context menu
		//  - Jump to definition
		//    - decompile vs editor
		//  - Search references on item
		//    - Update tree, nothing else
		ColumnPane params = new ColumnPane();
		Button btn = new Button("Search");
		btn.getStyleClass().add("search-button");
		switch(type) {
			case MEMBER_DEFINITION:
				addInput(new Input<>(params, "ui.search.declaration.owner", "ui.search.declaration.owner.sub",
						NullableText::new, NullableText::get));
				addInput(new Input<>(params, "ui.search.declaration.name", "ui.search.declaration.name.sub",
						NullableText::new, NullableText::get));
				addInput(new Input<>(params, "ui.search.declaration.desc", "ui.search.declaration.desc.sub",
						NullableText::new, NullableText::get));
				addInput(new Input<>(params, "ui.search.matchmode", "ui.search.matchmode.sub", () -> {
					ComboBox<StringMatchMode> comboMode = new ComboBox<>();
					comboMode.getItems().setAll(StringMatchMode.values());
					comboMode.setValue(StringMatchMode.CONTAINS);
					return comboMode;
				}, ComboBoxBase::getValue));
				btn.setOnAction(e -> search(controller, () -> buildDefinitionSearch(controller.getWorkspace())));
				break;
			case REFERENCE:
				addInput(new Input<>(params, "ui.search.reference.owner", "ui.search.reference.owner.sub",
						NullableText::new, NullableText::get));
				addInput(new Input<>(params, "ui.search.reference.name", "ui.search.reference.name.sub",
						NullableText::new, NullableText::get));
				addInput(new Input<>(params, "ui.search.reference.desc", "ui.search.reference.desc.sub",
						NullableText::new, NullableText::get));
				addInput(new Input<>(params, "ui.search.matchmode", "ui.search.matchmode.sub", () -> {
					ComboBox<StringMatchMode> comboMode = new ComboBox<>();
					comboMode.getItems().setAll(StringMatchMode.values());
					comboMode.setValue(StringMatchMode.CONTAINS);
					return comboMode;
				}, ComboBoxBase::getValue));
				btn.setOnAction(e -> search(controller, () -> buildReferenceSearch(controller.getWorkspace())));
				break;
			case STRING:
				addInput(new Input<>(params, "ui.search.string", "ui.search.string.sub",
						TextField::new, TextInputControl::getText));
				addInput(new Input<>(params, "ui.search.matchmode", "ui.search.matchmode.sub", () -> {
					ComboBox<StringMatchMode> comboMode = new ComboBox<>();
					comboMode.getItems().setAll(StringMatchMode.values());
					comboMode.setValue(StringMatchMode.CONTAINS);
					return comboMode;
				}, ComboBoxBase::getValue));
				btn.setOnAction(e -> search(controller, () -> buildStringSearch(controller.getWorkspace())));
				break;
			case VALUE:
				addInput(new Input<>(params, "ui.search.value", "ui.search.value.sub",
						NumericText::new, NumericText::get));
				btn.setOnAction(e -> {
					if(input("ui.search.value") == null)
						return;
					search(controller, () -> buildValueSearch(controller.getWorkspace()));
				});
				break;
			case INSTRUCTION_TEXT:
				addInput(new Input<>(params, "ui.search.insn.lines", "ui.search.insn.lines.sub",
						TextArea::new, t -> Arrays.asList(t.getText().split("[\n\r]"))));
				addInput(new Input<>(params, "ui.search.matchmode", "ui.search.matchmode.sub", () -> {
					ComboBox<StringMatchMode> comboMode = new ComboBox<>();
					comboMode.getItems().setAll(StringMatchMode.values());
					comboMode.setValue(StringMatchMode.CONTAINS);
					return comboMode;
				}, ComboBoxBase::getValue));
				btn.setOnAction(e -> search(controller, () -> buildInsnSearch(controller.getWorkspace())));
				break;
			default:
				break;
		}
		PackageSelector selector = new PackageSelector(controller.windows());
		addInput(new Input<>(params, "ui.search.skippackages", "ui.search.skippackages.sub",
				() -> selector, PackageSelector::get));
		params.add(null, btn);
		getItems().addAll(params, tree);
		SplitPane.setResizableWithParent(params, Boolean.FALSE);
	}

	/**
	 * Run search and display results.
	 *
	 * @param controller
	 * 		Controller for the workspace.
	 * @param collectorSupplier
	 * 		Search generator.
	 */
	private void search(Controller controller, Supplier<SearchCollector> collectorSupplier) {
		Workspace workspace = controller.getWorkspace();
		List<SearchResult> results = null;
		SearchCollector collector = null;
		try {
			collector = collectorSupplier.get();
			results = collector.getAllResults();
		} catch(IllegalArgumentException ex) {
			// Some search argument requirements were not met
			results = Collections.emptyList();
			// TODO: visual warning
			Log.warn("Failed search due to illegal arguments: {}", ex.getMessage());
		}
		// Create parameter map so the root item can show the parameters of the search
		Map<String, Object> params = new TreeMap<>(inputMap.entrySet().stream()
				.collect(Collectors.toMap(
						e -> e.getKey().substring(e.getKey().lastIndexOf(".") + 1),
						e -> e.getValue().getOr("")
				)));
		tree.setRoot(new SearchRootItem(workspace.getPrimary(), results, params));
		JavaResourceTree.recurseOpen(tree.getRoot());
	}

	private SearchCollector buildDefinitionSearch(Workspace workspace) {
		return SearchBuilder.in(workspace)
				.skipDebug()
				.skipCode()
				.query(new MemberDefinitionQuery(
						input("ui.search.declaration.owner"), input("ui.search.declaration.name"),
						input("ui.search.declaration.desc"), input("ui.search.matchmode")))
				.skipPackages(input("ui.search.skippackages"))
				.build();
	}

	private SearchCollector buildReferenceSearch(Workspace workspace) {
		return SearchBuilder.in(workspace)
				.skipDebug()
				.query(new MemberReferenceQuery(
						input("ui.search.reference.owner"), input("ui.search.reference.name"),
						input("ui.search.reference.desc"), input("ui.search.matchmode")))
				.skipPackages(input("ui.search.skippackages"))
				.build();
	}

	private SearchCollector buildStringSearch(Workspace workspace) {
		return SearchBuilder.in(workspace)
				.skipDebug()
				.query(new StringQuery(input("ui.search.string"), input("ui.search.matchmode")))
				.skipPackages(input("ui.search.skippackages"))
				.build();
	}

	private SearchCollector buildValueSearch(Workspace workspace) {
		return SearchBuilder.in(workspace)
				.skipDebug()
				.skipPackages(input("ui.search.skippackages"))
				.query(new ValueQuery(input("ui.search.value"))).build();
	}

	private SearchCollector buildInsnSearch(Workspace workspace) {
		return SearchBuilder.in(workspace)
				.skipPackages(input("ui.search.skippackages"))
				.query(new InsnTextQuery(input("ui.search.insn.lines"), input("ui.search.matchmode"))).build();
	}

	/**
	 * @param input
	 * 		Input instance to register.
	 * @param <E>
	 * 		Editor type.
	 * @param <R>
	 * 		Editor content type.
	 */
	private <E extends Node, R> void addInput(Input<E, R> input) {
		inputMap.put(input.key, input);
	}

	/**
	 * @param key
	 * 		Input key.
	 * @param <R>
	 * 		Input content type.
	 *
	 * @return Input value.
	 */
	private <R> R input(String key) {
		Input<?, R> obj = inputMap.get(key);
		if (obj == null)
			throw new IllegalStateException("No input by key: " + key);
		return obj.get();
	}

	/**
	 * Wrapper for inputs.
	 *
	 * @param <E>
	 * 		Editor type.
	 * @param <R>
	 * 		Editor content type.
	 */
	private static class Input<E extends Node, R> {
		private final E editor;
		private final Function<E, R> mapper;
		private final String key;

		private Input(ColumnPane root, String key, String desc, Supplier<E> create,
					 Function<E, R> mapper) {
			this.editor = create.get();
			this.mapper = mapper;
			this.key = key;
			SubLabeled labeled = new SubLabeled(translate(key), translate(desc));
			root.add(labeled, editor);
		}

		/**
		 * @return Content of editor.
		 */
		public R get() {
			return mapper.apply(editor);
		}

		/**
		 * @param fallback
		 * 		Value to return if the editor's value is {@code null}.
		 *
		 * @return Content of editor.
		 */
		public R getOr(R fallback) {
			R ret = get();
			if(ret == null)
				return fallback;
			return ret;
		}
	}
}
