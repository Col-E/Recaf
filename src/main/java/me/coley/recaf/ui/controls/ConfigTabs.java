package me.coley.recaf.ui.controls;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import me.coley.recaf.control.gui.GuiController;

import static me.coley.recaf.util.LangUtil.translate;

/**
 * TabPane to hold a {@link ConfigPane} for each {@link me.coley.recaf.config.Config}
 *
 * @author Matt
 */
public class ConfigTabs extends TabPane {
	/**
	 * @param controller
	 * 		Gui controller.
	 */
	public ConfigTabs(GuiController controller) {
		Tab tabDisplay = new Tab(translate("display"), new ConfigPane(controller, controller.config().display()));
		Tab tabBinding = new Tab(translate("binding"), new ConfigPane(controller, controller.config().keys()));
		Tab tabDecompile = new Tab(translate("decompile"), new ConfigPane(controller, controller.config().decompile()));
		Tab tabAssembler = new Tab(translate("assembler"), new ConfigPane(controller, controller.config().assembler()));
		Tab tabUpdates = new Tab(translate("update"), new ConfigPane(controller, controller.config().update()));
		getTabs().addAll(tabDisplay, tabBinding, tabDecompile, tabAssembler, tabUpdates);
		setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
	}
}
