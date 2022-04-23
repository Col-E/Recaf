package me.coley.recaf.ui;

import javafx.scene.control.Label;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.BasicClassRepresentation;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.pane.DexDecompilePane;
import me.coley.recaf.ui.pane.SmaliAssemblerPane;

/**
 * Display for a {@link ClassInfo}.
 *
 * @author Matt Coley
 */
public class AndroidClassView extends CommonClassView {
	private AndroidClassViewMode mode;

	/**
	 * @param info
	 * 		Initial state of the class to display.
	 */
	public AndroidClassView(DexClassInfo info) {
		super(info);
	}

	@Override
	protected void setInitialMode() {
		mode = Configs.editor().defaultAndroidClassMode;
	}

	@Override
	protected ClassRepresentation createViewForClass(CommonClassInfo info) {
		if (mode == AndroidClassViewMode.DECOMPILE) {
			if (info instanceof DexClassInfo) {
				return new DexDecompilePane();
			} else {
				return new BasicClassRepresentation(new Label("Unknown class info type!"), i -> {
				});
			}
		} else {
			return new SmaliAssemblerPane();
		}
	}

	/**
	 * @return Current view mode, dictating what is shown in {@link #getMainView() the main view}.
	 */
	public AndroidClassViewMode getMode() {
		return mode;
	}

	/**
	 * Set the view mode and trigger a refresh.
	 *
	 * @param mode
	 * 		New view mode.
	 */
	public void setMode(AndroidClassViewMode mode) {
		// Skip if the same
		if (this.mode == mode)
			return;
		this.mode = mode;
		// Cleanup old view if present
		if (mainView instanceof Cleanable) {
			((Cleanable) mainView).cleanup();
		}
		// Trigger refresh
		refreshView();
		onUpdate(getCurrentClassInfo());
	}
}
