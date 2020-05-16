package me.coley.recaf.ui;

import javafx.stage.Stage;
import me.coley.recaf.Recaf;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.text.CssThemeEditorPane;
import me.coley.recaf.util.Resource;
import me.coley.recaf.util.self.SelfReferenceUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static me.coley.recaf.util.LangUtil.translate;

/**
 * Theme manager.
 *
 * @author Matt
 */
public class Themes {
	/**
	 * Open the custom theme editor.
	 *
	 * @param controller
	 * 		Current controller.
	 */
	public static void showThemeEditor(GuiController controller) {
		Stage stage = controller.windows().getThemeEditorWindow();
		if(stage == null) {
			stage = controller.windows().window(translate("ui.menubar.themeeditor"),
					new CssThemeEditorPane(controller));
			controller.windows().setThemeEditorWindow(stage);
		}
		stage.show();
		stage.toFront();
	}

	/**
	 * @return List of application-wide styles.
	 */
	public static List<Resource> getStyles() {
		List<Resource> resources =  SelfReferenceUtil.get().getStyles();
		resources.addAll(get("ui-", ".css"));
		return resources;
	}

	/**
	 * @return List of text-editor styles.
	 */
	public static List<Resource> getTextThemes() {
		List<Resource> resources =  SelfReferenceUtil.get().getTextThemes();
		resources.addAll(get("text-", ".css"));
		return resources;
	}

	private static Collection<Resource> get(String prefix, String suffix) {
		List<Resource> resources = new ArrayList<>();
		File styleDirectory = Recaf.getDirectory("style").toFile();
		if (!styleDirectory.exists())
			styleDirectory.mkdir();
		for (File file : styleDirectory.listFiles()) {
			String name = file.getName();
			if (name.startsWith(prefix) && name.endsWith(suffix))
				resources.add(Resource.external(file.getPath().replace('\\', '/')));
		}
		return resources;
	}
}
