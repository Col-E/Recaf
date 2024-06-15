package software.coley.recaf.ui.pane;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.*;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.comment.*;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pane for listing comments made on classes and their members in the current workspace.
 *
 * @author Matt Coley
 */
@Dependent
public class CommentListPane extends BorderPane implements Navigable, DocumentationPane, CommentUpdateListener, CommentContainerListener {
	private static final Logger logger = Logging.get(CommentListPane.class);
	private final Map<String, ClassCommentPane> classToPane = new ConcurrentHashMap<>();
	private final CommentSearchPane searchPane = new CommentSearchPane();
	private final VBox commentsList = new VBox();
	private final WorkspacePathNode workspacePath;
	private final CellConfigurationService cellConfigurationService;
	private final CommentManager commentManager;
	private final Actions actions;

	@Inject
	public CommentListPane(@Nonnull CommentManager commentManager,
	                       @Nonnull CellConfigurationService cellConfigurationService,
	                       @Nonnull WorkspaceManager workspaceManager,
	                       @Nonnull Actions actions) {
		this.cellConfigurationService = cellConfigurationService;
		this.commentManager = commentManager;
		this.actions = actions;

		workspacePath = PathNodes.workspacePath(Objects.requireNonNull(workspaceManager.getCurrent(), "Cannot open comment list if no workspace is open"));

		// Some basic UI layout.
		commentsList.setSpacing(10);
		commentsList.setPadding(new Insets(10));
		ScrollPane scroll = new ScrollPane(commentsList);
		scroll.getStyleClass().add("dark-scroll-pane");
		scroll.setFitToWidth(true);
		setCenter(scroll);
		setBottom(searchPane);

		// Register self as a listener so that when comment updates are made we can change the display.
		commentManager.addCommentListener(this);
		commentManager.addCommentContainerListener(this);

		// Listener won't cover items that already exist.
		populateInitialComments();
	}

	@Override
	public boolean isTrackable() {
		// We want this type to be navigable to benefit from automatic close support.
		return false;
	}

	@Override
	public void onClassCommentUpdated(@Nonnull ClassPathNode path, @Nullable String comment) {
		ClassCommentPane pane = getOrCreateCommentPane(path);
		if (pane != null)
			pane.onClassCommentUpdated(path, comment);
	}

	@Override
	public void onFieldCommentUpdated(@Nonnull ClassMemberPathNode path, @Nullable String comment) {
		ClassPathNode classPath = Objects.requireNonNull(path.getParent());
		ClassCommentPane pane = getOrCreateCommentPane(classPath);
		if (pane != null)
			pane.onFieldCommentUpdated(path, comment);
	}

	@Override
	public void onMethodCommentUpdated(@Nonnull ClassMemberPathNode path, @Nullable String comment) {
		ClassPathNode classPath = Objects.requireNonNull(path.getParent());
		ClassCommentPane pane = getOrCreateCommentPane(classPath);
		if (pane != null)
			pane.onMethodCommentUpdated(path, comment);
	}

	@Override
	public void onClassContainerRemoved(@Nonnull ClassPathNode path, @Nullable ClassComments comments) {
		ClassCommentPane pane = classToPane.get(path.getValue().getName());
		if (pane != null) {
			FxThreadUtil.run(() -> commentsList.getChildren().remove(pane));
		}
	}

	/**
	 * Create comment panes for all class comments in the current workspace.
	 */
	private void populateInitialComments() {
		WorkspaceComments workspaceComments = commentManager.getCurrentWorkspaceComments();
		if (workspaceComments == null) return;
		for (ClassComments classComments : workspaceComments)
			commentsList.getChildren().add(new ClassCommentPane(classComments));
	}

	@Nullable
	private ClassCommentPane getOrCreateCommentPane(@Nonnull ClassPathNode path) {
		// Skip if workspace container is null.
		WorkspaceComments workspaceComments = commentManager.getCurrentWorkspaceComments();
		if (workspaceComments == null)
			return null;

		// Skip if class container is null.
		String name = path.getValue().getName();
		ClassComments classComments = workspaceComments.getClassComments(path);
		if (classComments == null)
			return null;

		// Get or create the comment pane.
		ClassCommentPane pane = classToPane.get(name);
		if (pane == null) {
			ClassCommentPane newPane = new ClassCommentPane(classComments);
			FxThreadUtil.run(() -> commentsList.getChildren().add(newPane));
			pane = newPane;
		}

		return pane;
	}

	@Nonnull
	@Override
	public PathNode<?> getPath() {
		return workspacePath;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		// Important that we unregister ourselves to prevent leakage.
		commentManager.removeCommentListener(this);
		commentManager.removeCommentContainerListener(this);

		// Clear out the UI.
		for (ClassCommentPane value : classToPane.values())
			value.clear();
		classToPane.clear();
		commentsList.getChildren().clear();
	}

	/**
	 * Displays content from a {@link ClassComments} container.
	 */
	private class ClassCommentPane extends BorderPane implements CommentUpdateListener {
		private final Label classComment = new Label();
		private final Map<String, Label> memberComments = new ConcurrentHashMap<>();
		private final DelegatingClassComments comments;
		private final ClassPathNode classPath;
		private String fullTextCached;

		private ClassCommentPane(@Nonnull ClassComments comments) {
			if (comments instanceof DelegatingClassComments delegatingComments) {
				this.comments = delegatingComments;

				// Common class level display setup.
				classPath = delegatingComments.getPath();
				classToPane.put(classPath.getValue().getName(), this);
				classComment.setGraphicTextGap(10);
				classComment.getStyleClass().add(Styles.TEXT_SUBTLE);
				classComment.setStyle("""
						-fx-border-color: -color-border-default;
						-fx-border-width: 0 0 1 0;
						""");
				setTop(classComment);
				getStyleClass().add("tooltip");

				// For searching, hide this pane if the search text is not in any of our comments.
				visibleProperty().bind(searchPane.searchTextProperty()
						.map(search -> search.isBlank() || buildFullText().toLowerCase().contains(search.toLowerCase())));
				managedProperty().bind(visibleProperty());

				// Initial layout.
				refresh();
			} else {
				throw new IllegalStateException("Expected enriched delegate comment model, but got persist model");
			}
		}

		/**
		 * Full re-population of comment data.
		 */
		private void refresh() {
			// Clear out old comment mappings
			memberComments.clear();

			// Create labels for class + class-comment
			Label classPreview = new Label();
			Node graphic = cellConfigurationService.graphicOf(classPath);
			classPreview.setCursor(Cursor.HAND);
			classPreview.setText(cellConfigurationService.textOf(classPath));
			classPreview.setGraphic(graphic);
			classPreview.setOnMousePressed(e -> {
				try {
					actions.gotoDeclaration(classPath);
				} catch (IncompletePathException ex) {
					logger.error("Cannot open comment target: {}", classPath, ex);
				}
			});
			classComment.setGraphic(classPreview);
			classComment.setText(filterComment(comments.getClassComment()));

			// Create labels for all class members + their comments.
			GridPane body = new GridPane();
			body.setHgap(10);
			body.setVgap(10);
			body.setPadding(new Insets(10));
			ClassInfo classInfo = classPath.getValue();
			for (FieldMember field : classInfo.getFields()) {
				String comment = comments.getFieldComment(field);
				addMemberComment(comment, classPath.child(field), body);
			}
			for (MethodMember method : classInfo.getMethods()) {
				String comment = comments.getMethodComment(method);
				addMemberComment(comment, classPath.child(method), body);
			}
			setCenter(body);
		}

		private void addMemberComment(@Nullable String comment, @Nonnull ClassMemberPathNode path, @Nonnull GridPane grid) {
			if (comment == null) return;

			// Label to show the member name + graphic.
			ClassMember member = path.getValue();
			Label memberPreview = new Label();
			memberPreview.setCursor(Cursor.HAND);
			memberPreview.setGraphic(cellConfigurationService.graphicOf(path));
			memberPreview.setText(cellConfigurationService.textOf(path));
			memberPreview.setOnMousePressed(e -> {
				try {
					ClassPathNode classPath = Objects.requireNonNull(path.getParent());
					actions.gotoDeclaration(classPath).requestFocus(member);
				} catch (IncompletePathException ex) {
					logger.error("Cannot open comment target: {}", path, ex);
				}
			});

			// Label for the actual comment.
			Label commentPreview = new Label(filterComment(comment));
			commentPreview.getStyleClass().add(Styles.TEXT_SUBTLE);

			// Layout in the grid.
			int row = grid.getRowCount();
			grid.add(memberPreview, 0, row);
			grid.add(commentPreview, 1, row);
			memberComments.put(memberKey(member), commentPreview);
		}

		@Override
		public void onClassCommentUpdated(@Nonnull ClassPathNode path, @Nullable String comment) {
			if (isApplicableClass(path)) {
				fullTextCached = null;
				FxThreadUtil.run(() -> classComment.setText(comment));
			}
		}

		@Override
		public void onFieldCommentUpdated(@Nonnull ClassMemberPathNode path, @Nullable String comment) {
			onMemberCommentUpdated(path, comment);
		}

		@Override
		public void onMethodCommentUpdated(@Nonnull ClassMemberPathNode path, @Nullable String comment) {
			onMemberCommentUpdated(path, comment);
		}

		private void onMemberCommentUpdated(@Nonnull ClassMemberPathNode path, @Nullable String comment) {
			if (isApplicableClass(path.getParent())) {
				fullTextCached = null;
				ClassMember member = path.getValue();
				Label memberComment = memberComments.get(memberKey(member));
				if (comment != null && memberComment != null)
					FxThreadUtil.run(() -> memberComment.setText(comment));
				else
					// Newly commented members and removed comments, trigger a refresh so things are in order.
					FxThreadUtil.run(this::refresh);
			}
		}

		/**
		 * @param path
		 * 		Path to check.
		 *
		 * @return {@code true} when the given path matches our {@link #classPath}.
		 */
		private boolean isApplicableClass(@Nullable ClassPathNode path) {
			if (path == null || classPath == null) return false;
			return path.getValue().getName().equals(classPath.getValue().getName());
		}

		/**
		 * @param member
		 * 		Some member.
		 *
		 * @return Lookup key for {@link #memberComments}.
		 */
		@Nonnull
		private String memberKey(@Nonnull ClassMember member) {
			return member.getName() + '.' + member.getDescriptor();
		}

		/**
		 * @param comment
		 * 		Input comment.
		 *
		 * @return Filtered comment with no newlines, length limited.
		 */
		@Nonnull
		private String filterComment(@Nullable String comment) {
			if (comment == null)
				return "";
			comment = comment.replace('\n', ' ');
			if (comment.length() > 60)
				comment = comment.substring(0, 60) + "...";
			return comment;
		}

		/**
		 * @return All comments combined.
		 */
		@Nonnull
		private String buildFullText() {
			if (fullTextCached == null) {
				StringBuilder sb = new StringBuilder();
				String comment = comments.getClassComment();
				if (comment != null) sb.append(comment);

				ClassInfo classInfo = classPath.getValue();
				for (FieldMember field : classInfo.getFields()) {
					comment = comments.getFieldComment(field);
					if (comment != null) sb.append(comment);
				}
				for (MethodMember method : classInfo.getMethods()) {
					comment = comments.getMethodComment(method);
					if (comment != null) sb.append(comment);
				}
				fullTextCached = sb.toString();
			}
			return fullTextCached;
		}

		/**
		 * Clear out UI.
		 */
		private void clear() {
			if (visibleProperty().isBound())
				visibleProperty().unbind();
			setCenter(null);
		}
	}

	/**
	 * Basic search pane.
	 */
	private static class CommentSearchPane extends HBox {
		private final TextField searchText = new TextField();

		public CommentSearchPane() {
			searchText.promptTextProperty().bind(Lang.getBinding("comments.search.prompt"));
			HBox.setHgrow(searchText, Priority.ALWAYS);
			setStyle("""
					-fx-background-color: -color-bg-default;
					-fx-border-color: -color-border-default;
					-fx-border-width: 1 0 0 0;
					""");
			setPadding(new Insets(10));
			setSpacing(10);
			getChildren().add(searchText);
		}

		/**
		 * @return Search text.
		 */
		@Nonnull
		public StringProperty searchTextProperty() {
			return searchText.textProperty();
		}
	}
}
