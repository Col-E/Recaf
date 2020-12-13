package me.coley.recaf.ui.controls.popup;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import me.coley.recaf.config.ConfDecompile;
import me.coley.recaf.config.ConfDisplay;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.decompile.DecompileImpl;
import me.coley.recaf.ui.controls.ActionButton;
import me.coley.recaf.ui.controls.pane.ConfigPane;
import me.coley.recaf.ui.controls.pane.ConfigTabs;
import me.coley.recaf.ui.controls.view.ClassViewport;

import static me.coley.recaf.util.LangUtil.translate;

/**
 * Draggable suggestion window.
 *
 * @author Matt
 */
public class SuggestionWindow extends DragPopup {
	private SuggestionWindow(Node content, Control handle) {
		super(content, handle);
	}

	/**
	 * @param controller
	 * 		Controller to access config of.
	 * @param view
	 * 		View that has crashed decompiler.
	 *
	 * @return Suggestion window.
	 */
	public static SuggestionWindow suggestFailedDecompile(GuiController controller,
														  ClassViewport view) {
		return ofDecompilerChange(translate("suggest.decompile.failure"),
				translate("suggest.decompile.failure.title"), controller, view);
	}

	/**
	 * @param controller
	 * 		Controller to access config of.
	 * @param view
	 * 		View that has the errors.
	 *
	 * @return Suggestion window.
	 */
	public static SuggestionWindow suggestAltDecompile(GuiController controller,
													   ClassViewport view) {
		return ofDecompilerChange(translate("suggest.decompile.warn"),
				translate("suggest.decompile.warn.title"), controller, view);
	}

	/**
	 * @param controller
	 * 		Controller to access config of.
	 * @param view
	 * 		View that has the errors.
	 *
	 * @return Suggestion window.
	 */
	public static SuggestionWindow suggestTimeoutDecompile(GuiController controller,
													   ClassViewport view) {
		return ofDecompilerChange(translate("suggest.decompile.timeout"),
				translate("suggest.decompile.timeout.title"), controller, view);
	}

	/**
	 * @param switchMessage
	 * 		Message shown when offering switching decompilers.
	 * @param title
	 * 		Message for the window title.
	 * @param controller
	 * 		Controller to access config of.
	 * @param view
	 * 		View that has the errors.
	 *
	 * @return Suggestion window.
	 */
	public static SuggestionWindow ofDecompilerChange(String switchMessage, String title, GuiController controller,
													   ClassViewport view) {
		GridPane grid = new GridPane();
		grid.setPadding(new Insets(15));
		grid.setVgap(4);
		grid.setAlignment(Pos.CENTER);
		int col = 0;
		for(col = 0; col < 3; col++) {
			ColumnConstraints cc = new ColumnConstraints();
			cc.setHgrow(Priority.ALWAYS);
			cc.setFillWidth(true);
			grid.getColumnConstraints().add(cc);
		}
		grid.add(new Label(switchMessage), 0, 0, 3, 1);
		SuggestionWindow window = of(title, grid);
		// Add decompiler switch buttons
		col = 0;
		ConfDecompile confDecompile = controller.config().decompile();
		ConfDisplay confDisplay = controller.config().display();
		for (DecompileImpl impl : DecompileImpl.values()) {
			String text = impl.name();
			ActionButton btn = new ActionButton(text, () -> {
				confDecompile.decompiler = impl;
				refreshConfigWindow(controller);
				view.setOverrideDecompiler(null);
				window.close();
			});
			btn.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			// Disable current
			if (impl == view.getDecompiler())
				btn.setDisable(true);
			grid.add(btn, col, 2);
			col++;
		}
		grid.add(new Label(translate("suggest.switchmodes")), 0, 3, 3, 1);
		// Add class mode switch button
		col = 0;
		for (ClassViewport.ClassMode mode : ClassViewport.ClassMode.values()) {
			String text = mode.name();
			ActionButton btn = new ActionButton(text, () -> {
				confDisplay.classEditorMode = mode;
				refreshConfigWindow(controller);
				view.setOverrideMode(confDisplay.classEditorMode);
				window.close();
			});
			btn.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			// Disable current
			if (mode == view.getClassMode())
				btn.setDisable(true);
			grid.add(btn, col, 4);
			col++;
		}
		return window;
	}

	/**
	 * Update the config UI to reflect modified values.
	 *
	 * @param controller
	 * 		Controller to pull from.
	 */
	private static void refreshConfigWindow(GuiController controller) {
		// Do nothing if config window not populated
		if (controller.windows().getConfigWindow() == null)
			return;
		// Update the config ui
		ConfigTabs tabs = (ConfigTabs) controller.windows().getConfigWindow().getScene().getRoot();
		tabs.getTabs().forEach(t -> {
			ConfigPane pane = ((ConfigPane) t.getContent());
			if (pane.getConfig() == controller.config().display()) {
				pane.refresh();
			}
		});
	}

	/**
	 * @param title
	 * 		Window title.
	 * @param content
	 * 		Window content.
	 *
	 * @return Suggestion window instance.
	 */
	private static SuggestionWindow of(String title, Node content) {
		// Window title
		Label lblTitle = new Label(title);
		lblTitle.getStyleClass().add("h1");
		return new SuggestionWindow(content, lblTitle);
	}
}
