package me.coley.recaf.command.impl;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import me.coley.recaf.Recaf;
import me.coley.recaf.control.Controller;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.control.headless.HeadlessController;
import me.coley.recaf.util.LangUtil;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.ThreadUtil;
import me.coley.recaf.util.UiUtil;
import me.coley.recaf.util.self.SelfUpdater;
import me.coley.recaf.workspace.InstrumentationResource;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Scanner;

/**
 * Command line initializer for Recaf, invoked from the main method.
 * Sets up the controller to use then starts it.
 *
 * @author Matt
 */
@Command(
		name = "Recaf",
		version = Recaf.VERSION,
		description = "Recaf: A modern java bytecode editor.",
		mixinStandardHelpOptions = true)
public class Initializer implements Runnable {
	@Option(names = {"--input" }, description = "The input file to load. " +
			"Supported types are: class, jar, json")
	public Path input;
	@Option(names = {"--script" }, description = "Script file to load for cli usage")
	public Path script;
	@Option(names = { "--cli" }, description = "Run Recaf via CLI")
	public boolean cli;
	@Option(names = { "--instrument" }, description = "Indicates Recaf has been invoked as an agent")
	public boolean instrument;
	@Option(names = { "--noupdate" }, description = "Disable update checking entirely")
	public boolean noUpdates;
	//
	private Controller controller;

	@Override
	public void run() {
		// Disable update check
		if (noUpdates)
			SelfUpdater.disable();
		// Setup controller
		boolean headless = isHeadless();
		if (headless)
			controller = new HeadlessController(input, script);
		else
			controller = new GuiController(input);
		controller.setup();
		Recaf.setController(controller);
		if (instrument)
			InstrumentationResource.setup(controller);
		else if (controller.config().backend().firstTime && script == null)
			promptFirstTime();
	}

	/**
	 * Start the controller.
	 */
	public void startController() {
		controller.run();
	}

	/**
	 * Show documentation nag.
	 */
	private void promptFirstTime() {
		// Unmark value
		controller.config().backend().firstTime = false;
		// Determine how to show prompt
		if (isHeadless()) {
			Log.info(LangUtil.translate("misc.firsttime.cli"));
			String line = new Scanner(System.in).nextLine();
			if (line != null) {
				line = line.trim().toLowerCase();
				if (line.contains("ok") | line.contains("y"))
					openDoc();
			}
		} else {
			// Wait a bit so the platform gets loaded, then show an alert dialog
			ThreadUtil.runJfxDelayed(2000, () -> {
				ButtonType btnYes = new ButtonType(LangUtil.translate("misc.yes"), ButtonBar.ButtonData.YES);
				ButtonType btnNo = new ButtonType(LangUtil.translate("misc.no"), ButtonBar.ButtonData.NO);
				Alert alert = new Alert(Alert.AlertType.INFORMATION,
						LangUtil.translate("misc.firsttime.gui"), btnYes, btnNo);
				alert.setTitle(LangUtil.translate("misc.firsttime.gui.title"));
				Optional<ButtonType> result = alert.showAndWait();
				if (result.isPresent() && result.get().equals(btnYes))
					openDoc();
			});
		}
	}

	private void openDoc() {
		try {
			UiUtil.showDocument(URI.create(Recaf.DOC_URL));
		} catch(Exception ex) {
			Log.error(ex, "Failed to open documentation url");
		}
	}

	/**
	 * @return {@code true} when Recaf should not allocate a UI.
	 */
	private boolean isHeadless() {
		return cli || script != null;
	}

	/**
	 * @return The controller to execute Recaf with.
	 */
	public Controller getController() {
		return controller;
	}
}
