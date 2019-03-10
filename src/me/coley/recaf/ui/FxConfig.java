package me.coley.recaf.ui;

import org.controlsfx.control.PropertySheet;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import me.coley.recaf.config.impl.ConfASM;
import me.coley.recaf.config.impl.ConfCFR;
import me.coley.recaf.config.impl.ConfDisplay;
import me.coley.recaf.config.impl.ConfKeybinds;
import me.coley.recaf.config.impl.ConfOther;
import me.coley.recaf.config.impl.ConfUpdate;
import me.coley.recaf.ui.component.ReflectiveConfigSheet;
import me.coley.recaf.util.Icons;
import me.coley.recaf.util.JavaFX;
import me.coley.recaf.util.Lang;

/**
 * Window for handling config options.
 * 
 * @author Matt
 */
public class FxConfig extends Stage {
	private final static FxConfig INSTANCE = new FxConfig();

	private FxConfig() {
		setTitle(Lang.get("ui.config"));
		getIcons().add(Icons.CONFIG);
		// Prevent close, instead hide stage.
		setOnCloseRequest(event -> {
			event.consume();
			hide();
		});
		// Populate properties dynamically from config instances.
		PropertySheet propertySheet = new ReflectiveConfigSheet(
				ConfDisplay.instance(), 
				ConfKeybinds.instance(), 
				ConfASM.instance(), 
				ConfCFR.instance(),
				ConfUpdate.instance(),
				ConfOther.instance());
		propertySheet.setMode(PropertySheet.Mode.CATEGORY);
		VBox.setVgrow(propertySheet, Priority.ALWAYS);
		setScene(JavaFX.scene(propertySheet, 600, 500));
	}

	/**
	 * Display config window.
	 */
	public static void open() {
		if (INSTANCE.isShowing()) {
			INSTANCE.toFront();
		} else {
			INSTANCE.show();
		}
	}
}
