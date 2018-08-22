package me.coley.recaf.ui.component;

import java.util.List;

import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.tree.AnnotationNode;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.TypeUtil;
import me.coley.recaf.ui.FormatFactory;
import me.coley.recaf.util.Lang;

// TODO: Support adding to the "values" list in AnnotationNode.
public class AnnotationListEditor<T extends List<AnnotationNode>> extends StagedCustomEditor<T> {
	private final ObservableList<AnnotationNode> annotations;

	@SuppressWarnings("unchecked")
	public AnnotationListEditor(Item item) {
		super(item);
		T list = (T) item.getValue();
		if (list == null) {
			annotations = FXCollections.observableArrayList();
		} else {
			annotations = FXCollections.observableArrayList(list);
		}
	}

	@Override
	public Node getEditor() {
		return new ActionButton(Lang.get("misc.edit"), () -> open(this));
	}

	private void open(AnnotationListEditor<T> annotationListEditor) {
		if (staged()) {
			return;
		}
		BorderPane content = new BorderPane();
		ListView<AnnotationNode> view = new ListView<>(annotations);
		annotations.addListener(new ListChangeListener<AnnotationNode>() {
			@SuppressWarnings("unchecked")
			@Override
			public void onChanged(Change<? extends AnnotationNode> c) {
				setValue((T) annotations);
			}
		});
		view.setCellFactory(param -> new ListCell<AnnotationNode>() {
			@Override
			public void updateItem(AnnotationNode item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					// Reset 'hidden' items
					setGraphic(null);
					setText(null);
				} else {
					setGraphic(FormatFactory.annotation(item));
				}
			}
		});
		view.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode().equals(KeyCode.DELETE)) {
					delete(view);
				}
			}
		});
		view.setItems(annotations);
		view.setEditable(true);
		// textfield / button to add new value
		BorderPane menuPane = new BorderPane();
		TextField annoDesc = new TextField();
		annoDesc.setTooltip(new Tooltip(Lang.get("ui.bean.class.annotations.desc.tooltip")));
		
		annoDesc.setOnAction((e) -> add(annoDesc, view));
		Button addAnno = new ActionButton(Lang.get("misc.add"), () -> add(annoDesc, view));
		menuPane.setCenter(annoDesc);
		menuPane.setRight(addAnno);
		content.setCenter(view);
		content.setBottom(menuPane);
		setStage("ui.bean.class.annotations.title", content, 400, 500);
	}

	/**
	 * Add member in TextField to ListView.
	 * 
	 * @param text
	 * @param view
	 */
	private void add(TextField text, ListView<AnnotationNode> view) {
		AnnotationNode node = construct(text);
		if (node != null) {
			view.itemsProperty().get().add(node);
			text.textProperty().setValue("");
		}
	}

	/**
	 * Remove selected items from ListView.
	 * 
	 * @param view
	 */
	private void delete(ListView<AnnotationNode> view) {
		MultipleSelectionModel<AnnotationNode> selection = view.selectionModelProperty().getValue();
		for (int index : selection.getSelectedIndices()) {
			view.getItems().remove(index);
		}
	}

	private AnnotationNode construct(TextField text) {
		String desc = text.textProperty().get();
		if (desc == null || desc.isEmpty() || !TypeUtil.isStandard(desc)) {
			Logging.error(Lang.get("misc.invalidtype.standard"), true);
			return null;
		}
		return new AnnotationNode(desc);
	}
}
