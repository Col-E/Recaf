package software.coley.recaf.ui.pane.editing.binary;

import jakarta.annotation.Nonnull;
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
import software.coley.recaf.ui.pane.editing.hex.HexConfig;
import software.coley.recaf.ui.pane.editing.hex.HexEditor;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.FxThreadUtil;
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
	private final HexEditor editor;
	private PathNode<?> path;

	public HexAdapter(@Nonnull HexConfig config) {
		setCenter(editor = new HexEditor(config));
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
					bundle.put(fileInfo.toFileBuilder().withRawContent(data).build());
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
							bundle.put(newClass);
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
