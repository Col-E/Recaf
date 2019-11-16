package me.coley.recaf.ui.controls.search;

import javafx.geometry.Orientation;
import javafx.scene.control.*;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.search.*;
import me.coley.recaf.ui.controls.*;
import me.coley.recaf.ui.controls.tree.*;
import me.coley.recaf.workspace.Workspace;

import java.util.List;

/**
 * Pane for displaying search query inputs &amp; results.
 *
 * @author Matt
 */
public class SearchPane extends SplitPane {
	private final GuiController controller;
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
	@SuppressWarnings("unchecked")
	public SearchPane(GuiController controller, QueryType type) {
		this.controller = controller;
		setOrientation(Orientation.VERTICAL);
		setDividerPositions(0.5);
		tree.setCellFactory(e -> new ResourceCell());
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
				SubLabeled lblText = new SubLabeled("String", "Content of the string");
				TextField text = new TextField();
				params.add(lblText, text);
				//
				SubLabeled lblMode = new SubLabeled("Match mode", "Method of string matching");
				ComboBox<StringMatchMode> comboMode = new ComboBox<>();
				comboMode.getItems().setAll(StringMatchMode.values());
				comboMode.setValue(StringMatchMode.CONTAINS);
				params.add(lblMode, comboMode);
				//
				btn.setOnAction(e -> {
					Workspace workspace = controller.getWorkspace();
					SearchCollector collector = SearchBuilder.in(workspace).skipDebug()
							.query(new StringQuery(text.getText(), comboMode.getValue())).build();
					List<SearchResult> results = collector.getAllResults();
					tree.setRoot(new SearchRootItem(workspace.getPrimary(), results));
					tree.getRoot().setExpanded(true);
				});
				params.add(null, btn);
				break;
			case VALUE:
				break;
			case INSTRUCTION_TEXT:
				break;
			default:
				break;
		}
		// TODO: Only add results after "Search" button is pressed.
		//  - Do it once, updating it after subsequent presses
		//  - Maybe a nice/snappy expand animation so it doesn't just <POP> into existence
		getItems().addAll(params, tree);
		SplitPane.setResizableWithParent(params, Boolean.FALSE);
	}
}
