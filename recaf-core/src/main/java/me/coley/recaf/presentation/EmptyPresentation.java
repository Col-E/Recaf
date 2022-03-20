package me.coley.recaf.presentation;

import me.coley.recaf.Controller;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.*;

/**
 * A presentation that has no display at all. Ideal for using Recaf as a library.
 *
 * @author Matt Coley
 */
public class EmptyPresentation implements Presentation {
	@Override
	public void initialize(Controller controller) {
		// no-op
	}

	@Override
	public WorkspacePresentation workspaceLayer() {
		return new EmptyWorkspacePresentation();
	}

	/**
	 * No-operation implementation for workspace actions.
	 */
	public static class EmptyWorkspacePresentation implements WorkspacePresentation {
		@Override
		public boolean closeWorkspace(Workspace workspace) {
			return true;
		}

		@Override
		public void openWorkspace(Workspace workspace) {
			// no-op
		}

		@Override
		public void onNewClass(Resource resource, ClassInfo newValue) {
			// no-op
		}

		@Override
		public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
			// no-op
		}

		@Override
		public void onRemoveClass(Resource resource, ClassInfo oldValue) {
			// no-op
		}

		@Override
		public void onNewDexClass(Resource resource, String dexName, DexClassInfo newValue) {
			// no-op
		}

		@Override
		public void onRemoveDexClass(Resource resource, String dexName, DexClassInfo oldValue) {
			// no-op
		}

		@Override
		public void onUpdateDexClass(Resource resource, String dexName, DexClassInfo oldValue, DexClassInfo newValue) {
			// no-op
		}

		@Override
		public void onNewFile(Resource resource, FileInfo newValue) {
			// no-op
		}

		@Override
		public void onUpdateFile(Resource resource, FileInfo oldValue, FileInfo newValue) {
			// no-op
		}

		@Override
		public void onRemoveFile(Resource resource, FileInfo oldValue) {
			// no-op
		}
	}
}
