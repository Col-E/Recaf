package me.coley.recaf.ui.prompt;

import javafx.scene.Parent;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import me.coley.recaf.ui.dialog.WizardDialog;
import me.coley.recaf.ui.control.ResourceSelectionList;
import me.coley.recaf.ui.dialog.Wizard;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.Threads;
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
	 * @param files
	 * 		Files to create a workspace from.
	 *
	 * @return Created workspace, or {@code null} if some failure or cancellation occured.
	 */
	public static Workspace createWorkspace(List<Path> files) {
		if (files.size() == 1) {
			try {
				return new Workspace(new Resources(ResourceIO.fromPath(files.get(0), true)));
			} catch (IOException ex) {
				logger.error("Failed to transform files collection into workspace resource collection", ex);
				return null;
			}
		}
		List<Resource> resources = readResources(files);
		if (resources == null) {
			Toolkit.getDefaultToolkit().beep();
			return null;
		}
		WizardInputSelection selection = new WizardInputSelection(resources);
		Wizard wizard = new Wizard(selection);
		WizardDialog<Workspace> workspaceWizardDialog =
				new WizardDialog<>(Lang.get("dialog.title.create-workspace"), wizard);
		wizard.setOnFinish(() -> workspaceWizardDialog.setResult(selection.createFromSelection()));
		Optional<Workspace> workspace = workspaceWizardDialog.showAndWait();
		return workspace.orElse(null);
	}

	/**
	 * Display a prompt to the user on how to handle loading the given files.
	 * <br>
	 * <b>Must be called on UI thread!</b>
	 *
	 * @param files
	 * 		Files to load.
	 *
	 * @return Result defining how to handle the files.
	 */
	public static WorkspaceDropResult prompt(List<Path> files) {
		try {
			List<Resource> resources = readResources(files);
			if (resources == null) {
				Toolkit.getDefaultToolkit().beep();
				return cancel();
			}
			WizardChooseAction action = new WizardChooseAction(resources);
			WizardInputSelection selection = new WizardInputSelection(resources);
			Wizard wizard = new Wizard(action, selection);
			WizardDialog<WorkspaceDropResult> workspaceWizardDialog =
					new WizardDialog<>(Lang.get("dialog.title.update-workspace"), wizard);
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
			Optional<WorkspaceDropResult> result = workspaceWizardDialog.showAndWait();
			return result.orElse(cancel());
		} catch (Throwable t) {
			logger.error("Failed to create drop prompt", t);
			return cancel();
		}
	}

	private static WorkspaceDropResult workspace(Workspace workspace) {
		return new WorkspaceDropResult(WorkspaceDropAction.CREATE_NEW_WORKSPACE, workspace, null);
	}

	private static WorkspaceDropResult add(List<Resource> library) {
		return new WorkspaceDropResult(WorkspaceDropAction.ADD_TO_WORKSPACE, null, library);
	}

	private static WorkspaceDropResult cancel() {
		return new WorkspaceDropResult(WorkspaceDropAction.CANCEL, null, null);
	}

	private static List<Resource> readResources(List<Path> files) {
		try {
			List<Resource> resources = new ArrayList<>();
			for (Path file : files) {
				resources.add(ResourceIO.fromPath(file, true));
			}
			return resources;
		} catch (IOException ex) {
			logger.error("Failed to transform files collection into workspace resource collection", ex);
			return null;
		}
	}

	/**
	 * Drop result container.
	 */
	public static class WorkspaceDropResult {
		private final WorkspaceDropAction action;
		private final Workspace workspace;
		private final List<Resource> libraries;

		private WorkspaceDropResult(WorkspaceDropAction action, Workspace workspace, List<Resource> libraries) {
			this.action = action;
			this.workspace = workspace;
			this.libraries = libraries;
		}

		/**
		 * @return Loaded library resources.
		 * Will be {@code null} if {@link #action} is not {@link WorkspaceDropAction#ADD_TO_WORKSPACE}
		 */
		public List<Resource> getLibraries() {
			return libraries;
		}

		/**
		 * @return Loaded workspace.
		 * Will be {@code null} if {@link #action} is not {@link WorkspaceDropAction#CREATE_NEW_WORKSPACE}
		 */
		public Workspace getWorkspace() {
			return workspace;
		}

		/**
		 * @return Action of the result.
		 */
		public WorkspaceDropAction getAction() {
			return action;
		}
	}

	/**
	 * Type of action to run as a result of a file drop action.
	 */
	public enum WorkspaceDropAction {
		ADD_TO_WORKSPACE,
		CREATE_NEW_WORKSPACE,
		CANCEL;
	}

	/**
	 * Wizard page for selecting a primary resource.
	 */
	private static class WizardChooseAction extends Wizard.WizardPage {
		private ResourceSelectionList inputList;
		private WorkspaceDropAction action;

		private WizardChooseAction(List<Resource> resources) {
			super(Lang.get("wizard.chooseaction"), false);
			// Its initialized below.
			inputList.addResources(resources);
			inputList.selectFirst();
		}

		@Override
		protected Parent getContent() {
			// Handle type
			ToggleGroup group = new ToggleGroup();
			RadioButton btnCreate = new RadioButton(Lang.get("dialog.option.create-workspace"));
			RadioButton btnAdd = new RadioButton(Lang.get("dialog.option.update-workspace"));
			btnCreate.selectedProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue) {
					action = WorkspaceDropAction.CREATE_NEW_WORKSPACE;
					setIsFinal(inputList != null && inputList.isSingleResource());
				}
			});
			btnAdd.selectedProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue) {
					action = WorkspaceDropAction.ADD_TO_WORKSPACE;
					setIsFinal(true);
				}
			});
			btnAdd.setToggleGroup(group);
			btnCreate.setToggleGroup(group);
			// Select the initial button with a delay so the input list can be populated before the listener fires.
			Threads.runFx(() -> btnCreate.setSelected(true));
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
		public WorkspaceDropAction getAction() {
			return action;
		}
	}

	/**
	 * Wizard page for selecting a primary resource.
	 */
	private static class WizardInputSelection extends Wizard.WizardPage {
		private ResourceSelectionList inputList;

		private WizardInputSelection(List<Resource> resources) {
			super(Lang.get("wizard.selectprimary"), true);
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
