package software.coley.recaf.ui.menubar;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.control.MenuBar;

/**
 * Main menu component, bundling sub-menu components.
 *
 * @author Matt Coley
 * @see FileMenu
 */
@Dependent
public class MainMenu extends MenuBar {
	@Inject
	public MainMenu(@Nonnull FileMenu fileMenu,
					@Nonnull ConfigMenu configMenu,
					@Nonnull MappingMenu mappingMenu,
					@Nonnull ScriptMenu scriptMenu,
					@Nonnull HelpMenu helpMenu) {
		getMenus().addAll(fileMenu, configMenu, mappingMenu, scriptMenu, helpMenu);
		setPadding(new Insets(0, 0, 2, 0));
	}
}
