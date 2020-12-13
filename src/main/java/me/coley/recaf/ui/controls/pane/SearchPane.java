package me.coley.recaf.ui.controls.pane;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import me.coley.recaf.control.Controller;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.search.*;
import me.coley.recaf.ui.controls.NullableText;
import me.coley.recaf.ui.controls.NumericText;
import me.coley.recaf.ui.controls.PackageSelector;
import me.coley.recaf.ui.controls.SubLabeled;
import me.coley.recaf.ui.controls.tree.*;
import me.coley.recaf.util.LangUtil;
import me.coley.recaf.util.Log;
import me.coley.recaf.workspace.Workspace;

import java.util.*;
import java.util.function.BiConsumer;
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
	private final Runnable searchAction;


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
		ColumnPane params = new ColumnPane();
		Button btn = new Button(LangUtil.translate("ui.search"));
		btn.getStyleClass().add("search-button");
		switch(type) {
			case MEMBER_DEFINITION:
				addInput(new Input<>(params, "ui.search.declaration.owner", "ui.search.declaration.owner.sub",
						NullableText::new, NullableText::get, NullableText::setText));
				addInput(new Input<>(params, "ui.search.declaration.name", "ui.search.declaration.name.sub",
						NullableText::new, NullableText::get, NullableText::setText));
				addInput(new Input<>(params, "ui.search.declaration.desc", "ui.search.declaration.desc.sub",
						NullableText::new, NullableText::get, NullableText::setText));
				addInput(new Input<>(params, "ui.search.matchmode", "ui.search.matchmode.sub", () -> {
					ComboBox<StringMatchMode> comboMode = new ComboBox<>();
					comboMode.getItems().setAll(StringMatchMode.values());
					comboMode.setValue(StringMatchMode.CONTAINS);
					return comboMode;
				}, ComboBoxBase::getValue, ComboBoxBase::setValue));
				searchAction = () -> search(controller, () -> buildDefinitionSearch(controller.getWorkspace()));
				btn.setOnAction(e -> search());
				break;
			case CLASS_REFERENCE:
				addInput(new Input<>(params, "ui.search.cls_reference.name", "ui.search.cls_reference.name.sub",
						NullableText::new, NullableText::get, NullableText::setText));
				addInput(new Input<>(params, "ui.search.matchmode", "ui.search.matchmode.sub", () -> {
					ComboBox<StringMatchMode> comboMode = new ComboBox<>();
					comboMode.getItems().setAll(StringMatchMode.values());
					comboMode.setValue(StringMatchMode.CONTAINS);
					return comboMode;
				}, ComboBoxBase::getValue, ComboBoxBase::setValue));
				searchAction = () -> search(controller, () -> buildClassReferenceSearch(controller.getWorkspace()));
				btn.setOnAction(e -> search());
				break;
			case MEMBER_REFERENCE:
				addInput(new Input<>(params, "ui.search.mem_reference.owner", "ui.search.mem_reference.owner.sub",
						NullableText::new, NullableText::get, NullableText::setText));
				addInput(new Input<>(params, "ui.search.mem_reference.name", "ui.search.mem_reference.name.sub",
						NullableText::new, NullableText::get, NullableText::setText));
				addInput(new Input<>(params, "ui.search.mem_reference.desc", "ui.search.mem_reference.desc.sub",
						NullableText::new, NullableText::get, NullableText::setText));
				addInput(new Input<>(params, "ui.search.matchmode", "ui.search.matchmode.sub", () -> {
					ComboBox<StringMatchMode> comboMode = new ComboBox<>();
					comboMode.getItems().setAll(StringMatchMode.values());
					comboMode.setValue(StringMatchMode.CONTAINS);
					return comboMode;
				}, ComboBoxBase::getValue, ComboBoxBase::setValue));
				searchAction = () -> search(controller, () -> buildMemberReferenceSearch(controller.getWorkspace()));
				btn.setOnAction(e -> search());
				break;
			case STRING:
				addInput(new Input<>(params, "ui.search.string", "ui.search.string.sub",
						TextField::new, TextField::getText, TextField::setText));
				addInput(new Input<>(params, "ui.search.matchmode", "ui.search.matchmode.sub", () -> {
					ComboBox<StringMatchMode> comboMode = new ComboBox<>();
					comboMode.getItems().setAll(StringMatchMode.values());
					comboMode.setValue(StringMatchMode.CONTAINS);
					return comboMode;
				}, ComboBoxBase::getValue, ComboBoxBase::setValue));
				searchAction = () -> search(controller, () -> buildStringSearch(controller.getWorkspace()));
				btn.setOnAction(e -> search());
				break;
			case VALUE:
				addInput(new Input<>(params, "ui.search.value", "ui.search.value.sub",
						NumericText::new, NumericText::get, (e, t) -> e.setText(t.toString())));
				searchAction = () -> {
					if(input("ui.search.value") == null)
						return;
					search(controller, () -> buildValueSearch(controller.getWorkspace()));
				};
				btn.setOnAction(e -> search());
				break;
			case INSTRUCTION_TEXT:
				addInput(new Input<>(params, "ui.search.insn.lines", "ui.search.insn.lines.sub",
						TextArea::new, t -> Arrays.asList(t.getText().split("[\n\r]")),
						(e, t) -> e.setText(String.join("\n", t))));
				addInput(new Input<>(params, "ui.search.matchmode", "ui.search.matchmode.sub", () -> {
					ComboBox<StringMatchMode> comboMode = new ComboBox<>();
					comboMode.getItems().setAll(StringMatchMode.values());
					comboMode.setValue(StringMatchMode.CONTAINS);
					return comboMode;
				}, ComboBoxBase::getValue, ComboBoxBase::setValue));
				searchAction = () -> search(controller, () -> buildInsnSearch(controller.getWorkspace()));
				btn.setOnAction(e -> search());
				break;
			default:
				searchAction = null;
				break;
		}
		PackageSelector selector = new PackageSelector(controller.windows());
		addInput(new Input<>(params, "ui.search.skippackages", "ui.search.skippackages.sub",
				() -> selector, PackageSelector::get, PackageSelector::set));
		params.add(null, btn);
		getItems().addAll(params, tree);
		SplitPane.setResizableWithParent(params, Boolean.FALSE);
	}

	/**
	 * Run search and display results.
	 */
	public void search() {
		searchAction.run();
		tree.requestFocus();
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

	private SearchCollector buildClassReferenceSearch(Workspace workspace) {
		return SearchBuilder.in(workspace)
				.query(new ClassReferenceQuery(
						input("ui.search.cls_reference.name"), input("ui.search.matchmode")))
				.skipPackages(input("ui.search.skippackages"))
				.build();
	}

	private SearchCollector buildMemberReferenceSearch(Workspace workspace) {
		return SearchBuilder.in(workspace)
				.query(new MemberReferenceQuery(
						input("ui.search.mem_reference.owner"), input("ui.search.mem_reference.name"),
						input("ui.search.mem_reference.desc"), input("ui.search.matchmode")))
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
	 * @param key
	 * 		Input key.
	 * @param value
	 * 		Input value to set.
	 * @param <R>
	 * 		Input content type.
	 */
	public <R> void setInput(String key, R value) {
		Input<?, R> obj = inputMap.get(key);
		if (obj == null)
			throw new IllegalStateException("No input by key: " + key);
		obj.set(value);
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
		private final BiConsumer<E, R> setter;
		private final String key;

		private Input(ColumnPane root, String key, String desc, Supplier<E> create,
					  Function<E, R> mapper, BiConsumer<E, R> setter) {
			this.editor = create.get();
			this.mapper = mapper;
			this.setter = setter;
			this.key = key;
			SubLabeled labeled = new SubLabeled(translate(key), translate(desc));
			root.add(labeled, editor);
		}

		/**
		 * @param value
		 * 		Value of editor to set.
		 */
		public void set(R value) {
			setter.accept(editor, value);
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
