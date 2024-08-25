package software.coley.recaf.ui.pane;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.layout.BorderPane;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.properties.builtin.CachedDecompileProperty;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.comment.ClassComments;
import software.coley.recaf.services.comment.CommentManager;
import software.coley.recaf.services.comment.WorkspaceComments;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.NavigationManager;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.bracket.SelectedBracketTracking;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.pane.editing.AbstractDecompilePane;
import software.coley.recaf.ui.pane.editing.ClassPane;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.threading.ThreadUtil;

import java.util.Collection;
import java.util.Collections;

/**
 * Pane for editing the comments on a class, field, or method.
 *
 * @author Matt Coley
 */
@Dependent
public class CommentEditPane extends BorderPane implements UpdatableNavigable, DocumentationPane {
	private final CommentManager commentManager;
	private final NavigationManager navigationManager;
	private final Editor editor;
	private PathNode<?> path;

	@Inject
	public CommentEditPane(@Nonnull CommentManager commentManager,
						   @Nonnull NavigationManager navigationManager,
						   @Nonnull KeybindingConfig keys,
						   @Nonnull SearchBar searchBar) {
		this.commentManager = commentManager;
		this.navigationManager = navigationManager;

		// Configure the editor
		editor = new Editor();
		editor.setSelectedBracketTracking(new SelectedBracketTracking());
		editor.getRootLineGraphicFactory().addDefaultCodeGraphicFactories();
		searchBar.install(editor);

		// Setup keybindings
		setOnKeyPressed(e -> {
			if (keys.getSave().match(e))
				ThreadUtil.run(this::save);
		});

		// Layout
		setCenter(editor);
	}

	@Override
	public boolean isTrackable() {
		// Disabling tracking allows other panels with the same path-node to be opened.
		return false;
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
	public void disable() {
		setDisable(true);
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		this.path = path;

		WorkspaceComments workspaceComments = commentManager.getCurrentWorkspaceComments();
		if (workspaceComments == null)
			return;

		String comment = null;
		if (path instanceof ClassMemberPathNode memberPath) {
			ClassPathNode classPath = memberPath.getParent();
			ClassComments classComments = workspaceComments.getClassComments(classPath);
			if (classComments != null) {
				ClassMember member = memberPath.getValue();
				if (member.isMethod()) {
					comment = classComments.getMethodComment(member.getName(), member.getDescriptor());
				} else {
					comment = classComments.getFieldComment(member.getName(), member.getDescriptor());
				}
			}
		} else if (path instanceof ClassPathNode classPath) {
			ClassComments classComments = workspaceComments.getClassComments(classPath);
			if (classComments != null)
				comment = classComments.getClassComment();
		}

		if (comment != null)
			editor.setText(comment);
	}

	/**
	 * Called when {@link KeybindingConfig#getSave()} is pressed.
	 * <br>
	 * Updates the comment in the {@link ClassComments} associated with the {@link #path}.
	 */
	private void save() {
		if (path == null)
			return;

		WorkspaceComments workspaceComments = commentManager.getCurrentWorkspaceComments();
		if (workspaceComments == null)
			return;

		String comment = editor.getText();
		if (comment.isBlank())
			comment = null; // Will remove the comment
		if (path instanceof ClassMemberPathNode memberPath) {
			ClassPathNode classPath = memberPath.getParent();
			ClassComments classComments = workspaceComments.getOrCreateClassComments(classPath);
			ClassMember member = memberPath.getValue();
			if (member.isMethod()) {
				classComments.setMethodComment(member.getName(), member.getDescriptor(), comment);
			} else {
				classComments.setFieldComment(member.getName(), member.getDescriptor(), comment);
			}
			invalidateDecompile(classPath);
			FxThreadUtil.run(() -> Animations.animateSuccess(editor, 1000));
		} else if (path instanceof ClassPathNode classPath) {
			ClassComments classComments = workspaceComments.getOrCreateClassComments(classPath);
			classComments.setClassComment(comment);
			invalidateDecompile(classPath);
			FxThreadUtil.run(() -> Animations.animateSuccess(editor, 1000));
		}
	}

	private void invalidateDecompile(@Nonnull ClassPathNode classPath) {
		// Void the cached decompilation.
		ClassInfo classInfo = classPath.getValue();
		CachedDecompileProperty.remove(classInfo);

		// Trigger a re-decompilation.
		navigationManager.getNavigableChildrenByPath(classPath).forEach(child -> {
			if (child instanceof ClassPane classPane && classPath.equals(classPane.getPath()))
				classPane.getNavigableChildren().forEach(subchild -> {
					if (subchild instanceof AbstractDecompilePane decompilePane)
						decompilePane.decompile();
				});
		});
	}
}
