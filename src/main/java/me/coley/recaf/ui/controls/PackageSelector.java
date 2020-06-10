package me.coley.recaf.ui.controls;

import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import me.coley.recaf.control.gui.WindowManager;

import java.util.*;

import static me.coley.recaf.util.LangUtil.translate;

/**
 * Utility for selecting packages.
 *
 * @author Matt
 */
public class PackageSelector extends Button {
	private List<String> selected = Collections.emptyList();

	/**
	 * @param wm
	 * 		Window manager to use to spawn the package selector popup.
	 */
	public PackageSelector(WindowManager wm) {
		setText(translate("ui.search.skippackages.empty"));
		setOnAction(e -> {
			// Editable list
			BorderPane rootPane = new BorderPane();
			ListView<String> list = new ListView<>();
			list.itemsProperty().get().addAll(selected);
			list.setOnKeyPressed(ee -> {
				if(ee.getCode() == KeyCode.DELETE) {
					int index = list.getSelectionModel().getSelectedIndex();
					list.getItems().remove(index);
					update(list.getItems());
				}
			});
			list.setEditable(true);
			// List add
			BorderPane addPane = new BorderPane();
			TextField text = new TextField();
			Button btnAdd = new Button(translate("misc.add"));
			Runnable action = () -> {
				String pack = text.getText();
				if(pack == null || pack.isEmpty())
					return;
				list.getItems().add(pack);
				text.setText("");
				update(list.getItems());
			};
			text.setOnAction(ee -> action.run());
			btnAdd.setOnAction(ee -> action.run());
			addPane.setCenter(text);
			addPane.setRight(btnAdd);
			rootPane.setCenter(list);
			rootPane.setBottom(addPane);
			// Setup popup stage
			Stage dialog = wm.window(translate("ui.search.skippackages"), rootPane);
			dialog.initOwner(wm.getConfigWindow());
			dialog.show();
			// Close on focus lost
			dialog.focusedProperty().addListener((n, o, focused) -> {
				if(!focused)
					dialog.close();
			});
		});
	}

	private void update(Collection<String> items) {
		selected = new ArrayList<>(items);
		int size = selected.size();
		if(size == 0) {
			setText(translate("ui.search.skippackages.empty"));
			setGraphic(null);
		} else if(size == 1) {
			setText(selected.get(0));
			setGraphic(new IconView("icons/folder-package.png"));
		} else {
			setText(size + " packages");
			setGraphic(new IconView("icons/folder-package.png"));
		}
	}

	/**
	 * @return Selected packages.
	 */
	public List<String> get() {
		return selected;
	}

	/**
	 * @param values
	 * 		Selected packages to set.
	 */
	public void set(List<String> values) {
		update(values);
	}
}
