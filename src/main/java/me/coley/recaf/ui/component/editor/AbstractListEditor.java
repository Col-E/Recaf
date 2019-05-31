package me.coley.recaf.ui.component.editor;

import java.util.List;

import org.controlsfx.control.PropertySheet.Item;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import me.coley.recaf.ui.component.ActionButton;
import me.coley.recaf.ui.component.ActionMenuItem;
import me.coley.recaf.util.Lang;

/**
 * Base abstraction of an editor for a list of values.
 * 
 * @author Matt
 *
 * @param <V>
 *            Value type of list.
 * @param <C>
 *            Control type.
 * @param <L>
 *            List type of V.
 */
public abstract class AbstractListEditor<V, C extends Region, L extends List<V>> extends StagedCustomEditor<L> {
	private final String titleKey;
	private final int initWidth, initHeight;
	
	public AbstractListEditor(Item item, String titleKey, int initWidth, int initHeight) {
		super(item);
		this.titleKey = titleKey;
		this.initWidth = initWidth;
		this.initHeight = initHeight;
	}

	protected abstract C create(ListView<V> view);

	protected abstract V getValue(C control);

	protected abstract void reset(C control);

	protected void setupView(ListView<V> view) {}

	protected String getButtonTranslationKey() {
		return "misc.edit";
	}

	protected String getStageTranslationKey() {
		return titleKey;
	}

	@Override
	public Node getEditor() {
		return new ActionButton(Lang.get(getButtonTranslationKey()), () -> open(this));
	}

	/**
	 * Open another window to handle editing of the value.
	 * 
	 * @param editor
	 *            CustomEditor instance to for value get/set callbacks.
	 */
	private void open(AbstractListEditor<V, C, L> editor) {
		if (staged()) {
			return;
		}
		BorderPane listPane = new BorderPane();
		BorderPane menuPane = new BorderPane();
		ListView<V> view = new ListView<>();

		ObservableList<V> list;
		L value = editor.getValue();
		if (value != null) {
			list = FXCollections.observableArrayList(value);
		} else {
			list = FXCollections.observableArrayList();
		}
		list.addListener(new ListChangeListener<V>() {
			@SuppressWarnings("unchecked")
			@Override
			public void onChanged(Change<? extends V> c) {
				setValue((L) list);
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
		view.setItems(list);
		view.setEditable(true);
		setupView(view);
		ContextMenu contextMenu = new ContextMenu();
		contextMenu.getItems().add(new ActionMenuItem(Lang.get("misc.remove"), () -> delete(view)));
		view.setContextMenu(contextMenu);
		listPane.setCenter(view);
		listPane.setBottom(menuPane);
		C newValue = create(view);
		Button addItem = new ActionButton(Lang.get("misc.add"), () -> add(newValue, view));
		menuPane.setCenter(newValue);
		menuPane.setRight(addItem);
		setStage(getStageTranslationKey(), listPane, initWidth, initHeight);
	}

	/**
	 * Add member from the control to ListView.
	 * 
	 * @param control
	 * @param view
	 */
	protected void add(C control, ListView<V> view) {
		V v = getValue(control);
		if (v != null) {
			view.itemsProperty().get().add(v);
			reset(control);
		} else {
			// TODO: Alert user of failure?
		}
	}

	/**
	 * Remove selected items from ListView.
	 * 
	 * @param view
	 */
	protected void delete(ListView<V> view) {
		MultipleSelectionModel<V> selection = view.selectionModelProperty().getValue();
		for (int index : selection.getSelectedIndices()) {
			view.getItems().remove(index);
		}
	}
}