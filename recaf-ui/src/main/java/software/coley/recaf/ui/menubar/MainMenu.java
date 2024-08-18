package software.coley.recaf.ui.menubar;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import javafx.geometry.Insets;
import javafx.scene.control.MenuBar;

/**
 * Main menu component, bundling sub-menu components.
 *
 * @author Matt Coley
 * @see MainMenuProvider A producer bean allows you to @Inject this type.
 */
public class MainMenu extends MenuBar {
	private final FileMenu fileMenu;
	private final ConfigMenu configMenu;
	private final SearchMenu searchMenu;
	private final MappingMenu mappingMenu;
	private final AnalysisMenu analysisMenu;
	private final ScriptMenu scriptMenu;
	private final HelpMenu helpMenu;

	/**
	 * Called from {@link MainMenuProvider} which facilitates exposing
	 * this class as an effective {@link ApplicationScoped} bean.
	 *
	 * @param fileMenu
	 * 		File menu instance.
	 * @param configMenu
	 * 		Config menu instance.
	 * @param searchMenu
	 * 		Search menu instance.
	 * @param mappingMenu
	 * 		Mapping menu instance.
	 * @param analysisMenu
	 * 		Analysis menu instance.
	 * @param scriptMenu
	 * 		Script menu instance.
	 * @param helpMenu
	 * 		Help menu instance.
	 */
	MainMenu(@Nonnull FileMenu fileMenu,
	         @Nonnull ConfigMenu configMenu,
	         @Nonnull SearchMenu searchMenu,
	         @Nonnull MappingMenu mappingMenu,
	         @Nonnull AnalysisMenu analysisMenu,
	         @Nonnull ScriptMenu scriptMenu,
	         @Nonnull HelpMenu helpMenu) {
		this.fileMenu = fileMenu;
		this.configMenu = configMenu;
		this.searchMenu = searchMenu;
		this.mappingMenu = mappingMenu;
		this.analysisMenu = analysisMenu;
		this.scriptMenu = scriptMenu;
		this.helpMenu = helpMenu;

		getMenus().addAll(fileMenu, configMenu, searchMenu, mappingMenu, analysisMenu, scriptMenu, helpMenu);
		setPadding(new Insets(0, 0, 2, 0));
	}

	/** @return File menu instance. */
	@Nonnull
	public FileMenu getFileMenu() {
		return fileMenu;
	}

	/** @return Config menu instance. */
	@Nonnull
	public ConfigMenu getConfigMenu() {
		return configMenu;
	}

	/** @return Search menu instance. */
	@Nonnull
	public SearchMenu getSearchMenu() {
		return searchMenu;
	}

	/** @return Mapping menu instance. */
	@Nonnull
	public MappingMenu getMappingMenu() {
		return mappingMenu;
	}

	/** @return Analysis menu instance. */
	@Nonnull
	public AnalysisMenu getAnalysisMenu() {
		return analysisMenu;
	}

	/** @return Script menu instance. */
	@Nonnull
	public ScriptMenu getScriptMenu() {
		return scriptMenu;
	}

	/** @return Help menu instance. */
	@Nonnull
	public HelpMenu getHelpMenu() {
		return helpMenu;
	}
}
