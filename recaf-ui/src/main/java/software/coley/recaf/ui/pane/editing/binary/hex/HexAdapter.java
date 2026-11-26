package software.coley.recaf.ui.pane.editing.binary.hex;

import jakarta.annotation.Nonnull;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.BundlePathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.workspace.WorkspaceSaveService;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;

import java.util.Collection;
import java.util.Collections;

/**
 * Adapts the navigable system with paths to the generic system used by {@link HexEditor}.
 *
 * @author Matt Coley
 */
public class HexAdapter extends BorderPane implements UpdatableNavigable {
	private static final Logger logger = Logging.get(HexAdapter.class);
	private final WorkspaceSaveService saveService;
	private final KeybindingConfig keys;
	private final HexEditor editor;
	private final EventHandler<KeyEvent> keyFilter = this::handleKeys;
	private PathNode<?> path;

	public HexAdapter(@Nonnull HexConfig config, @Nonnull WorkspaceSaveService saveService, @Nonnull KeybindingConfig keys) {
		this.saveService = saveService;
		this.keys = keys;

		setCenter(editor = new HexEditor(config));
		addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
	}

	/**
	 * Delegates to the hex editor commit action.
	 * <br>
	 * Updates the {@link ClassInfo} or {@link FileInfo} represented by the {@link #path} in the workspace.
	 */
	public void save() {
		if (editor.commit()) {
			// TODO: Animating on the hex editor is rather jarring
			//  vs on other controls like a text editor...
			Animations.animateSuccess(editor, 1000);
		} else {
			Animations.animateFailure(editor, 1000);
		}
	}

	/**
	 * @param event
	 * 		Key event to handle.
	 */
	private void handleKeys(@Nonnull KeyEvent event) {
		if (keys.getFind().match(event)) {
			editor.openFindDialog();
			event.consume();
		}
	}

	@Nonnull
	@Override
	public PathNode<?> getPath() {
		return path;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void requestFocus() {
		editor.requestFocus();
	}

	@Override
	public void disable() {
		removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
		editor.dispose();
		setCenter(null);
		setDisable(true);
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		if (path instanceof FilePathNode filePath) {
			FileInfo fileInfo = filePath.getValue();
			byte[] raw = fileInfo.getRawContent();
			if (editor.hasData()) {
				editor.updateData(raw);
			} else {
				editor.setInitialData(raw);
				editor.setCommitAction(data -> {
					BundlePathNode parent = filePath.getParent().getParent();
					FileBundle bundle = (FileBundle) parent.getValue();
					Workspace workspace = filePath.getValueOfType(Workspace.class);
					if (workspace == null)
						throw new IllegalStateException("Workspace missing from file path node");
					FileInfo newFileInfo = fileInfo.toFileBuilder().withRawContent(data).build();
					saveService.commit(workspace, bundle, newFileInfo);
				});
			}
		} else if (path instanceof ClassPathNode classPath) {
			ClassInfo classInfo = classPath.getValue();
			if (classInfo.isJvmClass()) {
				byte[] bytecode = classInfo.asJvmClass().getBytecode();
				if (editor.hasData()) {
					editor.updateData(bytecode);
				} else {
					editor.setInitialData(bytecode);
					editor.setCommitAction(data -> {
						try {
							BundlePathNode parent = classPath.getParent().getParent();
							JvmClassBundle bundle = (JvmClassBundle) parent.getValue();
							JvmClassInfo newClass = classInfo.asJvmClass().toJvmClassBuilder().adaptFrom(data).build();
							Workspace workspace = classPath.getValueOfType(Workspace.class);
							if (workspace == null)
								throw new IllegalStateException("Workspace missing from class path node");
							saveService.commit(workspace, bundle, newClass);
							FxThreadUtil.run(() -> Animations.animateSuccess(this, 1000));
						} catch (Throwable t) {
							// Skill issue
							logger.error("Hex editor edits to class resulted in an invalid class", t);
							FxThreadUtil.run(() -> Animations.animateFailure(this, 1000));
						}
					});
				}
			}
		}
	}
}
