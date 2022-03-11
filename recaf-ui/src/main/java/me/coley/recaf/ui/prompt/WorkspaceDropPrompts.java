package me.coley.recaf.ui.prompt;

import javafx.scene.Parent;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import me.coley.recaf.ui.control.ResourceSelectionList;
import me.coley.recaf.ui.dialog.Wizard;
import me.coley.recaf.ui.dialog.WizardDialog;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceIO;
import me.coley.recaf.workspace.resource.Resources;
import org.slf4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Utility for spawning file drop prompts that will assist users in updating their workspaces.
 *
 * @author Matt Coley
 */
public class WorkspaceDropPrompts {
	private static final Logger logger = Logging.get(WorkspaceDropPrompts.class);

	/**
	 * Show prompt to create a workspace.
	 * <br>
	 * <b>Must be called on UI thread!</b>
	 *
	 * @param resources
	 * 		Resources to create a workspace from.
	 *
	 * @return Created workspace, or {@code null} if the passed resource list is {@code null} or empty..
	 */
	public static Workspace createWorkspace(List<Resource> resources) {
		if (resources == null || resources.isEmpty())
			return null;
		if (resources.size() == 1) {
			return new Workspace(new Resources(resources.get(0)));
		}
		WizardInputSelection selection = new WizardInputSelection(resources);
		Wizard wizard = new Wizard(selection);
		WizardDialog<Workspace> workspaceWizardDialog =
				new WizardDialog<>(Lang.getBinding("dialog.title.create-workspace"), wizard);
		wizard.setOnFinish(() -> workspaceWizardDialog.setResult(selection.createFromSelection()));
		Optional<Workspace> workspace = workspaceWizardDialog.showAndWait();
		return workspace.orElse(null);
	}

	/**
	 * Display a prompt to the user on how to handle the given resources.
	 * <br>
	 * <b>Must be called on UI thread!</b>
	 *
	 * @param resources
	 * 		Resources to operate on.
	 *
	 * @return Result defining how to handle the resources.
	 */
	public static WorkspaceAction prompt(List<Resource> resources) {
		try {
			if (resources == null) {
				Toolkit.getDefaultToolkit().beep();
				return cancel();
			}
			WizardChooseAction action = new WizardChooseAction(resources);
			WizardInputSelection selection = new WizardInputSelection(resources);
			Wizard wizard = new Wizard(action, selection);
			WizardDialog<WorkspaceAction> workspaceWizardDialog =
					new WizardDialog<>(Lang.getBinding("dialog.title.update-workspace"), wizard);
			wizard.setOnFinish(() -> {
				switch (action.getAction()) {
					case ADD_TO_WORKSPACE:
						workspaceWizardDialog.setResult(add(resources));
						break;
					case CREATE_NEW_WORKSPACE:
						workspaceWizardDialog.setResult(workspace(selection.createFromSelection()));
						break;
					case CANCEL:
					default:
						workspaceWizardDialog.setResult(cancel());
						break;
				}
			});
			Optional<WorkspaceAction> result = workspaceWizardDialog.showAndWait();
			return result.orElse(cancel());
		} catch (Throwable t) {
			logger.error("Failed to create drop prompt", t);
			return cancel();
		}
	}

	/**
	 * @param workspace
	 * 		Workspace to create.
	 *
	 * @return Created result for creating a new workspace.
	 */
	public static WorkspaceAction workspace(Workspace workspace) {
		return new WorkspaceAction(WorkspaceActionType.CREATE_NEW_WORKSPACE, workspace, null);
	}

	/**
	 * @param library
	 * 		Libraries to add.
	 *
	 * @return Created result for adding to the current workspace.
	 */
	public static WorkspaceAction add(List<Resource> library) {
		return new WorkspaceAction(WorkspaceActionType.ADD_TO_WORKSPACE, null, library);
	}

	/**
	 * @return Created result for canceling the action.
	 */
	public static WorkspaceAction cancel() {
		return new WorkspaceAction(WorkspaceActionType.CANCEL, null, null);
	}

	/**
	 * Map file paths to resources.
	 *
	 * @param files
	 * 		Files to load.
	 *
	 * @return Loaded files as resources.
	 */
	public static List<Resource> readResources(List<Path> files) {
		try {
			List<Resource> resources = new ArrayList<>();
			for (Path file : files) {
				resources.add(ResourceIO.fromPath(file, true));
			}
			return resources;
		} catch (IOException ex) {
			logger.error("Failed to transform files collection into workspace resource collection", ex);
			Toolkit.getDefaultToolkit().beep();
			return null;
		}
	}

	/**
	 * Wizard page for selecting a primary resource.
	 */
	private static class WizardChooseAction extends Wizard.WizardPage {
		private ResourceSelectionList inputList;
		private WorkspaceActionType action;

		private WizardChooseAction(List<Resource> resources) {
			super(Lang.getBinding("wizard.chooseaction"), false);
			// Its initialized below.
			inputList.addResources(resources);
			inputList.selectFirst();
		}

		@Override
		protected Parent getContent() {
			// Handle type
			ToggleGroup group = new ToggleGroup();
			RadioButton btnCreate = new RadioButton();
			btnCreate.textProperty().bind(Lang.getBinding("dialog.option.create-workspace"));
			RadioButton btnAdd = new RadioButton();
			btnAdd.textProperty().bind(Lang.getBinding("dialog.option.update-workspace"));
			btnCreate.selectedProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue) {
					action = WorkspaceActionType.CREATE_NEW_WORKSPACE;
					setIsFinal(inputList != null && inputList.isSingleResource());
				}
			});
			btnAdd.selectedProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue) {
					action = WorkspaceActionType.ADD_TO_WORKSPACE;
					setIsFinal(true);
				}
			});
			btnAdd.setToggleGroup(group);
			btnCreate.setToggleGroup(group);
			// Select the initial button with a delay so the input list can be populated before the listener fires.
			FxThreadUtil.run(() -> btnCreate.setSelected(true));
			// Layout
			ColumnConstraints fillWidth = new ColumnConstraints();
			fillWidth.setPercentWidth(100);
			GridPane grid = new GridPane();
			grid.getColumnConstraints().add(fillWidth);
			grid.add(inputList = new ResourceSelectionList(), 0, 0);
			grid.add(btnCreate, 0, 1);
			grid.add(btnAdd, 0, 2);
			return grid;
		}

		/**
		 * @return Action type from selection.
		 */
		public WorkspaceActionType getAction() {
			return action;
		}
	}

	/**
	 * Wizard page for selecting a primary resource.
	 */
	private static class WizardInputSelection extends Wizard.WizardPage {
		private ResourceSelectionList inputList;

		private WizardInputSelection(List<Resource> resources) {
			super(Lang.getBinding("wizard.selectprimary"), true);
			// Its initialized below.
			inputList.addResources(resources);
			inputList.selectFirst();
		}

		@Override
		protected Parent getContent() {
			return inputList = new ResourceSelectionList();
		}

		private Workspace createFromSelection() {
			if (inputList == null) {
				return null;
			}
			return inputList.createFromSelection();
		}
	}
}
