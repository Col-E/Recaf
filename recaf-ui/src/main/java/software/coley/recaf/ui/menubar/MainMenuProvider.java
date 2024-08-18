package software.coley.recaf.ui.menubar;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Facilitate the injection of {@link MainMenu} as a singleton bean.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class MainMenuProvider {
	private final MainMenu mainMenu;

	@Inject
	public MainMenuProvider(@Nonnull FileMenu fileMenu,
	                        @Nonnull ConfigMenu configMenu,
	                        @Nonnull SearchMenu searchMenu,
	                        @Nonnull MappingMenu mappingMenu,
	                        @Nonnull AnalysisMenu analysisMenu,
	                        @Nonnull ScriptMenu scriptMenu,
	                        @Nonnull HelpMenu helpMenu) {
		this.mainMenu = new MainMenu(fileMenu, configMenu, searchMenu, mappingMenu, analysisMenu, scriptMenu, helpMenu);
	}

	/**
	 * @return Shared main menu instance.
	 */
	@Nonnull
	@Produces
	public MainMenu getMainMenu() {
		return mainMenu;
	}
}
