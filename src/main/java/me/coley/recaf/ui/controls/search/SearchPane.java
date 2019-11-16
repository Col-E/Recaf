package me.coley.recaf.ui.controls.search;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import me.coley.recaf.control.Controller;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.search.*;
import me.coley.recaf.ui.controls.*;
import me.coley.recaf.ui.controls.tree.*;
import me.coley.recaf.workspace.Workspace;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static me.coley.recaf.util.LangUtil.translate;

/**
 * Pane for displaying search query inputs &amp; results.
 *
 * @author Matt
 */
@SuppressWarnings("unchecked")
public class SearchPane extends SplitPane {
	private final Map<String, Input> inputMap = new HashMap<>();
	// TODO: Different tree cell (but mostly same code) based on QueryType
	//  - references can do the DnSpy-like expansion for example, others cant
	//  - lots recycled from FileItem and the like
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
		tree.setCellFactory(e -> new ResourceCell());
		// TODO: Support skipping certain packages (also add support for command version)
		// TODO: Different parameter pane based on QueryType
		ColumnPane params = new ColumnPane();
		Button btn = new Button("Search", new IconView("icons/find.png"));
		switch(type) {
			case CLASS_NAME:
				break;
			case CLASS_INHERITANCE:
				break;
			case MEMBER_DEFINITION:
				break;
			case USAGE:
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
				btn.setOnAction(e -> search(controller, () -> SearchBuilder.in(controller.getWorkspace())
						.skipDebug()
						.query(new StringQuery(input("ui.search.string"), input("ui.search.matchmode")))
						.build()));
				break;
			case VALUE:
				addInput(new Input<>(params, "ui.search.value", "ui.search.value.sub",
						TextField::new, in -> {
					// TODO: Hide this in an auto-typing extension of TextField
					//  - Also add validation support (use custom instead of relying on ControlsFx)
					String text = in.getText();
					if (text.matches("\\d+"))
						return Integer.parseInt(text);
					else if(text.matches("\\d+\\.?\\d*[dD]?")) {
						if(text.toLowerCase().contains("d"))
							return Double.parseDouble(text.substring(0, text.length() - 1));
						else
							return Double.parseDouble(text);
					} else if (text.matches("\\d+\\.?\\d*[fF]"))
						return Float.parseFloat(text.substring(0, text.length() - 1));
					else if (text.matches("\\d+\\.?\\d*[lL]"))
						return Long.parseLong(text.substring(0, text.length() - 1));
					return null;
				}));
				btn.setOnAction(e -> {
					Object value = input("ui.search.value");
					if(value == null)
						return;
					search(controller, () -> SearchBuilder.in(controller.getWorkspace())
							.skipDebug()
							.query(new ValueQuery(value)).build());
				});
				break;
			case INSTRUCTION_TEXT:
				break;
			default:
				break;
		}
		params.add(null, btn);
		// TODO: Only add results after "Search" button is pressed.
		//  - Do it once, updating it after subsequent presses
		//  - Maybe a nice/snappy expand animation so it doesn't just <POP> into existence
		getItems().addAll(params, tree);
		SplitPane.setResizableWithParent(params, Boolean.FALSE);
	}

	private void search(Controller controller, Supplier<SearchCollector> collectorSupplier) {
		Workspace workspace = controller.getWorkspace();
		List<SearchResult> results = collectorSupplier.get().getAllResults();
		tree.setRoot(new SearchRootItem(workspace.getPrimary(), results));
		tree.getRoot().setExpanded(true);
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
	}
}
