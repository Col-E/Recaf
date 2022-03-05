package me.coley.recaf.ui.prompt;

import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import me.coley.recaf.ui.control.ResourceSelectionList;
import me.coley.recaf.ui.dialog.Wizard;
import me.coley.recaf.ui.dialog.WizardDialog;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for spawning prompts that confirm if users want to close workspaces.
 *
 * @author Matt Coley
 */
public class WorkspaceClosePrompt {
	/**
	 * Display the confirmation prompt.
	 *
	 * @param workspace
	 * 		Workspace to close.
	 *
	 * @return {@code true} to close. {@code false} to keep open.
	 */
	public static boolean prompt(Workspace workspace) {
		List<Resource> resources = new ArrayList<>();
		resources.add(workspace.getResources().getPrimary());
		resources.addAll(workspace.getResources().getLibraries());
		WizardCloseDisplay selection = new WizardCloseDisplay(resources);
		Wizard wizard = new Wizard(selection);
		WizardDialog<Boolean> closeWizardDialog =
				new WizardDialog<>(Lang.getBinding("dialog.title.close-workspace"), wizard);
		closeWizardDialog.setResultConverter(v -> v != ButtonType.CANCEL);
		wizard.setOnCancel(() -> closeWizardDialog.setResult(false));
		wizard.setOnFinish(() -> closeWizardDialog.setResult(true));
		return closeWizardDialog.showAndWait().orElse(true);
	}

	/**
	 * Wizard page for displaying the workspace to be closed.
	 */
	private static class WizardCloseDisplay extends Wizard.WizardPage {
		private ResourceSelectionList inputList;

		private WizardCloseDisplay(List<Resource> resources) {
			super(Lang.getBinding("wizard.currentworkspace"), true);
			// Its initialized below.
			inputList.addResources(resources);
			inputList.selectFirst();
		}

		@Override
		protected Parent getContent() {
			return inputList = new ResourceSelectionList();
		}
	}
}
